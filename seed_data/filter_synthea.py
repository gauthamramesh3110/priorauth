#!/usr/bin/env python3
"""
Synthea pass 1: filter the raw export down to one payer's working set.

The raw Synthea export describes a whole regional healthcare system. This
system models a single payer organization and the provider organizations it
contracts with, so pass 1 reduces the export to the subset that payer touches,
closed under every foreign key that V2__curated_and_reference_tables.sql
declares.

Closure is the point, not size. If a coverage row survives but its patient does
not, the load in PA-06 fails on a constraint violation rather than warning.

Usage:
    python filter_synthea.py --stats
    python filter_synthea.py
    python filter_synthea.py --payer <uuid>
"""

from __future__ import annotations

import argparse
import csv
import sys
from collections import Counter
from pathlib import Path

# The payer this deployment represents. Change this and re-run; everything
# downstream follows from it.
PAYER_ID = "d47b3510-2895-3b70-9897-342d681c769d"

RAW_DIR = Path(__file__).parent / "raw"
OUT_DIR = Path(__file__).parent / "working-set"

# csv.field_size_limit guards against a pathological row; Synthea descriptions
# are long but bounded.
csv.field_size_limit(1_000_000)


def read_rows(name: str):
    """Stream one raw CSV. Files here reach 16MB, so never read them whole."""
    path = RAW_DIR / name
    if not path.exists():
        sys.exit(f"Missing {path}. Download a Synthea export into {RAW_DIR}/ first.")
    with path.open(newline="", encoding="utf-8") as fh:
        yield from csv.DictReader(fh)


def write_rows(name: str, fieldnames: list[str], rows: list[dict], key: str):
    """Write one filtered CSV, sorted so re-runs produce no spurious git diff."""
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    path = OUT_DIR / name
    with path.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames)
        writer.writeheader()
        for row in sorted(rows, key=lambda r: r[key]):
            writer.writerow(row)
    return len(rows)


def show_stats():
    """Patient counts per payer, so the payer choice is made with data."""
    names = {r["Id"]: r["NAME"] for r in read_rows("payers.csv")}
    patients_by_payer: dict[str, set[str]] = {}
    for row in read_rows("payer_transitions.csv"):
        patients_by_payer.setdefault(row["PAYER"], set()).add(row["PATIENT"])

    print(f"{'payer':40s} {'patients':>9s}  id")
    print("-" * 90)
    for payer_id, patients in sorted(
        patients_by_payer.items(), key=lambda kv: -len(kv[1])
    ):
        marker = " <- selected" if payer_id == PAYER_ID else ""
        print(f"{names.get(payer_id, '?'):40s} {len(patients):9d}  {payer_id}{marker}")


def filter_working_set(payer_id: str) -> int:
    # -- 1. patients covered by this payer, at any point in their history ----
    #
    # Expired spans are kept deliberately. A patient whose coverage lapsed is
    # the most realistic NOT_COVERED rejection available, and dropping them
    # would leave that intake path testable only with invented data.
    coverage_rows = [
        r for r in read_rows("payer_transitions.csv") if r["PAYER"] == payer_id
    ]
    patient_ids = {r["PATIENT"] for r in coverage_rows}
    if not patient_ids:
        sys.exit(f"No patients found for payer {payer_id}. Run --stats to pick one.")

    # -- 2. one streaming pass over encounters -------------------------------
    #
    # encounters.csv carries PATIENT, ORGANIZATION, PROVIDER and PAYER on one
    # row, which resolves the provider and organization sets without touching
    # any other file.
    #
    # Organizations are split by whether the encounter was covered by this
    # payer. Organizations that treated these patients but never under this
    # payer are real out-of-network candidates, which beats inventing a
    # holdout set in PA-07.
    provider_ids: set[str] = set()
    orgs_in_network: set[str] = set()
    orgs_seen_elsewhere: set[str] = set()

    for row in read_rows("encounters.csv"):
        if row["PATIENT"] not in patient_ids:
            continue
        if row["PROVIDER"]:
            provider_ids.add(row["PROVIDER"])
        org = row["ORGANIZATION"]
        if not org:
            continue
        if row["PAYER"] == payer_id:
            orgs_in_network.add(org)
        else:
            orgs_seen_elsewhere.add(org)

    orgs_out_of_network = orgs_seen_elsewhere - orgs_in_network

    # -- 3. close over providers --------------------------------------------
    #
    # A provider's home ORGANIZATION can differ from the organization on any
    # given encounter, and provider_ref has a foreign key to organization_ref.
    # Union those back in before writing, or PA-06 fails on load.
    provider_rows = [r for r in read_rows("providers.csv") if r["Id"] in provider_ids]
    org_ids = orgs_in_network | orgs_out_of_network
    org_ids |= {r["ORGANIZATION"] for r in provider_rows if r["ORGANIZATION"]}

    # -- 4. write the working set -------------------------------------------
    #
    # Note what is absent: encounters, procedures, medications, imaging.
    # Encounters are the spine filtered through, not a table anything loads.
    # The cost files are read directly from raw/ by PA-09 when choosing
    # eligible codes.
    counts: dict[str, int] = {}

    patient_rows = [r for r in read_rows("patients.csv") if r["Id"] in patient_ids]
    counts["patients.csv"] = write_rows(
        "patients.csv", list(patient_rows[0].keys()), patient_rows, "Id"
    )

    payer_rows = [r for r in read_rows("payers.csv") if r["Id"] == payer_id]
    counts["payers.csv"] = write_rows(
        "payers.csv", list(payer_rows[0].keys()), payer_rows, "Id"
    )

    counts["payer_transitions.csv"] = write_rows(
        "payer_transitions.csv",
        list(coverage_rows[0].keys()),
        coverage_rows,
        "PATIENT",
    )

    counts["providers.csv"] = write_rows(
        "providers.csv", list(provider_rows[0].keys()), provider_rows, "Id"
    )

    org_rows = [r for r in read_rows("organizations.csv") if r["Id"] in org_ids]
    counts["organizations.csv"] = write_rows(
        "organizations.csv", list(org_rows[0].keys()), org_rows, "Id"
    )

    condition_rows = [
        r for r in read_rows("conditions.csv") if r["PATIENT"] in patient_ids
    ]
    counts["conditions.csv"] = write_rows(
        "conditions.csv",
        list(condition_rows[0].keys()),
        condition_rows,
        "PATIENT",
    )

    # network_participation has no Synthea source. Emitted here so PA-07 seeds
    # from observed behaviour rather than an arbitrary holdout.
    network_rows = [
        {"PAYER": payer_id, "ORGANIZATION": org, "IN_NETWORK": "true"}
        for org in orgs_in_network
    ] + [
        {"PAYER": payer_id, "ORGANIZATION": org, "IN_NETWORK": "false"}
        for org in orgs_out_of_network
    ]
    counts["network_participation.csv"] = write_rows(
        "network_participation.csv",
        ["PAYER", "ORGANIZATION", "IN_NETWORK"],
        network_rows,
        "ORGANIZATION",
    )

    # -- 5. assert closure ---------------------------------------------------
    #
    # Every foreign key V2 declares must resolve inside the working set. Fail
    # here, loudly, rather than as a constraint violation during PA-06.
    failures: list[str] = []

    written_patients = {r["Id"] for r in patient_rows}
    written_orgs = {r["Id"] for r in org_rows}

    orphan_coverage = {r["PATIENT"] for r in coverage_rows} - written_patients
    if orphan_coverage:
        failures.append(f"{len(orphan_coverage)} coverage rows with no patient")

    orphan_providers = {
        r["ORGANIZATION"] for r in provider_rows if r["ORGANIZATION"]
    } - written_orgs
    if orphan_providers:
        failures.append(f"{len(orphan_providers)} providers with no organization")

    orphan_conditions = {r["PATIENT"] for r in condition_rows} - written_patients
    if orphan_conditions:
        failures.append(f"{len(orphan_conditions)} conditions with no patient")

    orphan_network = {r["ORGANIZATION"] for r in network_rows} - written_orgs
    if orphan_network:
        failures.append(f"{len(orphan_network)} network rows with no organization")

    if len(payer_rows) != 1:
        failures.append(f"expected exactly 1 payer row, wrote {len(payer_rows)}")

    # -- 6. report -----------------------------------------------------------
    print(f"payer:  {payer_rows[0]['NAME']}  ({payer_id})")
    print(f"output: {OUT_DIR}")
    print()
    for name, n in counts.items():
        print(f"  {name:28s} {n:7d}")
    print()
    print(f"  organizations in-network     {len(orgs_in_network):7d}")
    print(f"  organizations out-of-network {len(orgs_out_of_network):7d}")

    distinct_codes = Counter(r["CODE"] for r in condition_rows)
    print(f"  distinct condition codes     {len(distinct_codes):7d}")
    print()

    if failures:
        print("CLOSURE FAILED:")
        for f in failures:
            print(f"  - {f}")
        return 1

    print("closure verified: every foreign key in V2 resolves")
    if len(patient_rows) < 100:
        print(
            f"warning: {len(patient_rows)} patients is thin. PA-10 needs enough "
            "condition variety to hand-author criteria against."
        )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--stats", action="store_true", help="show patient counts per payer and exit"
    )
    parser.add_argument("--payer", default=PAYER_ID, help="override the payer id")
    args = parser.parse_args()

    if args.stats:
        show_stats()
        return 0
    return filter_working_set(args.payer)


if __name__ == "__main__":
    sys.exit(main())