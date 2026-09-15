#!/usr/bin/env python3
"""
Seed pass 3: load the working set into payer_db.

Reads the filtered CSVs produced by filter_synthea.py and inserts them into the
Payer Service's reference tables. Nothing here writes to prior_auth_review or
the curated configuration tables: those are application state and hand-authored
content respectively.

This is deliberately a separate artifact from either service. Neither service
should own the other's seed data, and in PA-26 the same loader gains a second
target database.

Usage:
    python load.py --dry-run      # map and validate, no database needed
    python load.py                # truncate and load

Environment:
    PAYER_DB_URL   default postgresql://payer:payer@localhost:5434/payer_db
"""

from __future__ import annotations

import argparse
import csv
import os
import sys
from pathlib import Path

CURATED = Path(__file__).parent / "curated"

# Eligible codes deliberately left without a criterion, so the evaluator's
# NO_CRITERION_DEFINED branch is reachable. Declared here rather than merely
# tolerated: the loader asserts this set matches exactly, which catches both an
# accidental omission and an accidental addition.
CODES_WITHOUT_CRITERION = {("US", "IMAGING")}

WORKING_SET = Path(__file__).parent / "working-set"

DEFAULT_DB_URL = "postgresql://payer:payer@localhost:5434/payer_db"

# Truncated together, in one statement, so foreign keys never block the reset.
#
# Every table that references one of the loaded tables must appear here, or
# PostgreSQL refuses the TRUNCATE. patient_condition and network_participation
# are therefore included even though this story does not load them: they arrive
# in PA-12 and PA-07.
#
# prior_auth_review is deliberately absent. It holds no foreign keys into these
# tables (its patient_id and provider_id are plain UUIDs, because they point at
# caches of another organization's data) so reseeding never destroys
# application state.
TRUNCATE_TABLES = [
    "coverage",
    "patient_condition",
    "network_participation",
    "prior_auth_criteria",
    "prior_auth_eligible_code",
    "provider_ref",
    "organization_ref",
    "patient_ref",
    "payer_ref",
]


def nullable(value: str | None) -> str | None:
    """Synthea leaves optional fields as empty strings. The database wants NULL.

    Half the patients in the working set have no ZIP, so this is not a
    theoretical concern: without it those rows load as empty strings and every
    downstream "is the zip missing" check silently answers no.
    """
    if value is None:
        return None
    value = value.strip()
    return value or None

def earliest_coverage_year() -> int:
    """Network participation has no start date in the data, so one is derived.

    Using the earliest coverage year means every organization is in or out of
    network for the whole period any patient was covered, which is the only
    honest answer available: Synthea records no contract dates.
    """
    rows = read_csv("payer_transitions.csv")
    return min(int(r["START_YEAR"]) for r in rows)

def read_csv(name: str) -> list[dict]:
    path = WORKING_SET / name
    if not path.exists():
        sys.exit(f"Missing {path}. Run filter_synthea.py first.")
    with path.open(newline="", encoding="utf-8") as fh:
        return list(csv.DictReader(fh))

def read_curated(name: str) -> list[dict]:
    """Hand-authored configuration, which lives outside the generated working set."""
    path = CURATED / name
    if not path.exists():
        sys.exit(f"Missing {path}. This file is written by hand, not generated.")
    with path.open(newline="", encoding="utf-8") as fh:
        return list(csv.DictReader(fh))

# ---------------------------------------------------------------------------
# Loaders
#
# Insert order matters: a table's parents must already be present. This is the
# same ordering V2__curated_and_reference_tables.sql uses to create them.
# ---------------------------------------------------------------------------

def map_payer_ref(rows):
    return [
        (
            r["Id"],
            r["NAME"],
            nullable(r["ADDRESS"]),
            nullable(r["CITY"]),
            nullable(r["STATE_HEADQUARTERED"]),
            nullable(r["ZIP"]),
            nullable(r["PHONE"]),
        )
        for r in rows
    ]


def map_patient_ref(rows):
    return [
        (
            r["Id"],
            r["FIRST"],
            r["LAST"],
            r["BIRTHDATE"],
            nullable(r["GENDER"]),
            nullable(r["ADDRESS"]),
            nullable(r["CITY"]),
            nullable(r["STATE"]),
            nullable(r["ZIP"]),
        )
        for r in rows
    ]


def map_organization_ref(rows):
    return [
        (
            r["Id"],
            r["NAME"],
            nullable(r["ADDRESS"]),
            nullable(r["CITY"]),
            nullable(r["STATE"]),
            nullable(r["ZIP"]),
        )
        for r in rows
    ]


def map_provider_ref(rows):
    # Note SPECIALITY, not SPECIALTY. That is Synthea's spelling, and the
    # column it maps to is specialty. Getting this wrong raises a KeyError
    # rather than loading nulls, which is the failure mode you want.
    return [
        (r["Id"], r["ORGANIZATION"], r["NAME"], nullable(r["SPECIALITY"]))
        for r in rows
    ]


def map_coverage(rows):
    # id is BIGSERIAL, so it is not supplied here.
    return [
        (
            r["PATIENT"],
            r["PAYER"],
            int(r["START_YEAR"]),
            int(r["END_YEAR"]),
            nullable(r["OWNERSHIP"]),
        )
        for r in rows
    ]

def map_network_participation(rows):
    # Synthea has no provider network concept. These rows are derived in pass 1
    # from encounter behaviour: an organization is in-network if it appears on
    # an encounter this payer covered, out-of-network if it treated the same
    # patients only under other coverage.
    org_ids = {r["Id"] for r in read_csv("organizations.csv")}
    covered = {r["ORGANIZATION"] for r in rows}

    if len(covered) != len(rows):
        raise ValueError("an organization appears more than once")
    if covered != org_ids:
        missing = len(org_ids - covered)
        extra = len(covered - org_ids)
        raise ValueError(
            f"network rows do not cover the working set: "
            f"{missing} organizations missing, {extra} unknown"
        )

    in_network = {r["ORGANIZATION"] for r in rows if r["IN_NETWORK"] == "true"}
    out_network = {r["ORGANIZATION"] for r in rows if r["IN_NETWORK"] == "false"}
    if len(in_network) + len(out_network) != len(rows):
        raise ValueError("IN_NETWORK must be exactly 'true' or 'false'")
    if not in_network or not out_network:
        raise ValueError("both buckets must be non-empty, or a rejection path is dead")

    # The OUT_OF_NETWORK rejection is only reachable if a request can name a
    # provider at an out-of-network organization. Assert it here rather than
    # discovering it as an untestable path in PA-15.
    providers = read_csv("providers.csv")
    reachable = [p for p in providers if p["ORGANIZATION"] in out_network]
    if not reachable:
        raise ValueError(
            "no provider belongs to an out-of-network organization, "
            "so OUT_OF_NETWORK cannot be triggered by real seed data"
        )

    effective_from = f"{earliest_coverage_year()}-01-01"
    print(
        f"      {len(in_network)} in-network, {len(out_network)} out-of-network, "
        f"{len(reachable)} providers reachable for OUT_OF_NETWORK, "
        f"effective from {effective_from}"
    )

    return [
        (
            r["PAYER"],
            r["ORGANIZATION"],
            r["IN_NETWORK"] == "true",
            effective_from,
            None,  # effective_to: no contract end recorded
        )
        for r in rows
    ]

def map_eligible_code(rows):
    keys = {(r["code"], r["code_type"]) for r in rows}
    if len(keys) != len(rows):
        raise ValueError("duplicate (code, code_type)")
    if not {r["code_type"] for r in rows} <= {"PROCEDURE", "IMAGING", "MEDICATION"}:
        raise ValueError("code_type must be PROCEDURE, IMAGING or MEDICATION")
    return [
        (r["code"], r["code_type"], r["description"], r["typical_cost"] or None)
        for r in rows
    ]


def map_criteria(rows):
    eligible = {
        (r["code"], r["code_type"]) for r in read_curated("eligible_codes.csv")
    }
    keys = [(r["code"], r["code_type"]) for r in rows]

    orphans = set(keys) - eligible
    if orphans:
        raise ValueError(f"criteria for codes not on the eligible list: {sorted(orphans)}")
    if len(keys) != len(set(keys)):
        raise ValueError("more than one criterion for the same code")

    # The gap is the point, so it is asserted rather than allowed. A missing
    # criterion added later by someone who has forgotten why US has none would
    # silently kill the NO_CRITERION_DEFINED branch.
    gap = eligible - set(keys)
    if gap != CODES_WITHOUT_CRITERION:
        raise ValueError(
            f"codes without a criterion changed: expected "
            f"{sorted(CODES_WITHOUT_CRITERION)}, found {sorted(gap)}"
        )

    flags = {r["auto_approve_if_met"] for r in rows}
    if not flags <= {"true", "false"}:
        raise ValueError("auto_approve_if_met must be exactly 'true' or 'false'")

    auto = [r for r in rows if r["auto_approve_if_met"] == "true"]
    always = [r for r in rows if r["auto_approve_if_met"] == "false"]
    if not auto:
        raise ValueError("no auto-approve criteria, so AUTO_APPROVE is unreachable")
    if not always:
        raise ValueError("no always-review criteria, so ALWAYS_PHYSICIAN_REVIEW is unreachable")

    bad = [r["code"] for r in always if r["required_condition_code"]]
    if bad:
        raise ValueError(f"always-review criteria must have no condition: {bad}")

    # patient_condition is empty until PA-12, so this validates against the
    # working set rather than the database.
    patients_with = {}
    for c in read_csv("conditions.csv"):
        patients_with.setdefault(c["CODE"], set()).add(c["PATIENT"])

    for r in auto:
        code = r["required_condition_code"]
        if not code:
            raise ValueError(f"{r['code']}: auto-approve with no required condition")
        holders = patients_with.get(code)
        if not holders:
            raise ValueError(
                f"{r['code']}: required condition {code} is on no patient, "
                "so AUTO_APPROVE is unreachable for it"
            )
        if len(holders) < 3:
            print(f"      warning: {r['code']} condition {code} on only {len(holders)} patients")

    print(
        f"      {len(auto)} auto-approve, {len(always)} always-review, "
        f"{len(gap)} with no criterion"
    )

    return [
        (
            r["code"],
            r["code_type"],
            r["criterion_description"],
            nullable(r["required_condition_code"]),
            r["auto_approve_if_met"] == "true",
            int(r["default_validity_days"]),
        )
        for r in rows
    ]

LOADERS = [
    (
        "payer_ref",
        "payers.csv",
        "INSERT INTO payer_ref (id, name, address, city, state, zip, phone)"
        " VALUES (%s, %s, %s, %s, %s, %s, %s)",
        map_payer_ref,
        read_csv,
    ),
    (
        "patient_ref",
        "patients.csv",
        "INSERT INTO patient_ref"
        " (id, first_name, last_name, birthdate, gender, address, city, state, zip)"
        " VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)",
        map_patient_ref,
        read_csv,
    ),
    (
        "organization_ref",
        "organizations.csv",
        "INSERT INTO organization_ref (id, name, address, city, state, zip)"
        " VALUES (%s, %s, %s, %s, %s, %s)",
        map_organization_ref,
        read_csv,
    ),
    (
        "provider_ref",
        "providers.csv",
        "INSERT INTO provider_ref (id, organization_id, name, specialty)"
        " VALUES (%s, %s, %s, %s)",
        map_provider_ref,
        read_csv,
    ),
    (
        "coverage",
        "payer_transitions.csv",
        "INSERT INTO coverage (patient_id, payer_id, start_year, end_year, ownership)"
        " VALUES (%s, %s, %s, %s, %s)",
        map_coverage,
        read_csv,
    ),
    (
        "network_participation",
        "network_participation.csv",
        "INSERT INTO network_participation"
        " (payer_id, organization_id, in_network, effective_from, effective_to)"
        " VALUES (%s, %s, %s, %s, %s)",
        map_network_participation,
        read_csv,
    ),
    (
        "prior_auth_eligible_code",
        "eligible_codes.csv",
        "INSERT INTO prior_auth_eligible_code (code, code_type, description, typical_cost)"
        " VALUES (%s, %s, %s, %s)",
        map_eligible_code,
        read_curated,
    ),
    (
        "prior_auth_criteria",
        "criteria.csv",
        "INSERT INTO prior_auth_criteria"
        " (code, code_type, criterion_description, required_condition_code,"
        "  auto_approve_if_met, default_validity_days)"
        " VALUES (%s, %s, %s, %s, %s, %s)",
        map_criteria,
        read_curated,
    ),
]

# Loaded by later stories, listed so the gap is visible rather than forgotten:
#   patient_condition      <- conditions.csv              (PA-12)
#   prior_auth_eligible_code, prior_auth_criteria         (PA-11)


def prepare() -> list[tuple[str, str, list[tuple]]]:
    """Read and map every loader's source, before touching the database."""
    prepared = []
    for table, source, insert, mapper, reader in LOADERS:
        rows = reader(source)
        try:
            values = mapper(rows)
        except KeyError as exc:
            sys.exit(f"{source}: expected column {exc} is missing")
        except ValueError as exc:
            sys.exit(f"{source}: {exc}")
        prepared.append((table, insert, values))
        print(f"  {table:24s} <- {source:28s} {len(values):6d} rows")
    return prepared


def load(db_url: str, prepared) -> int:
    try:
        import psycopg
    except ImportError:
        sys.exit("psycopg is not installed. pip install 'psycopg[binary]'")

    # One transaction for the whole reseed. A failure part-way through leaves
    # the database as it was rather than half-loaded, which matters because the
    # services may be running against it.
    with psycopg.connect(db_url) as conn:
        with conn.cursor() as cur:
            cur.execute(
                f"TRUNCATE {', '.join(TRUNCATE_TABLES)} RESTART IDENTITY"
            )
            for table, insert, values in prepared:
                cur.executemany(insert, values)

            print()
            failures = []
            for table, _, values in prepared:
                cur.execute(f"SELECT count(*) FROM {table}")
                actual = cur.fetchone()[0]
                ok = actual == len(values)
                print(f"  {table:24s} {actual:6d} rows  {'ok' if ok else 'MISMATCH'}")
                if not ok:
                    failures.append(f"{table}: expected {len(values)}, found {actual}")

            if failures:
                conn.rollback()
                print("\nLOAD FAILED, rolled back:")
                for f in failures:
                    print(f"  - {f}")
                return 1

        conn.commit()
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="read and map the CSVs without connecting to a database",
    )
    parser.add_argument(
        "--db-url",
        default=os.environ.get("PAYER_DB_URL", DEFAULT_DB_URL),
        help="payer_db connection string",
    )
    args = parser.parse_args()

    print(f"source: {WORKING_SET}")
    print()
    prepared = prepare()

    if args.dry_run:
        print("\ndry run: nothing written")
        return 0

    print(f"\ntarget: {args.db_url.rsplit('@', 1)[-1]}")
    return load(args.db_url, prepared)


if __name__ == "__main__":
    sys.exit(main())
