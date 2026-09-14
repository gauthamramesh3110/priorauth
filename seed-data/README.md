# Seed data

Everything in this directory exists to turn a raw Synthea export into the
working set the two services are seeded from.

## Provenance

| | |
|---|---|
| Source | [Synthea, The MITRE Corporation](https://synthetichealth.github.io/synthea/) |
| Downloaded | `<2026-09-08>` |
| Format | CSV export, Massachusetts population |
| Selected payer | Humana, `d47b3510-2895-3b70-9897-342d681c769d` |

**Synthea generates randomly.** Re-downloading or regenerating produces a
different population with different patients and different condition codes. The
hand-authored criteria in `curated/criteria.csv` reference specific SNOMED codes
that must exist on real patients in the working set, so `working-set/` is
committed and treated as source rather than as reproducible output.

If something in the seed behaves unexpectedly later, this table is how you tell
whether the data changed or the code did.

## No real patient data

Synthea is a synthetic patient population simulator. It generates records from
aggregated public health statistics rather than from real records, so nothing
here describes a real person. The names, addresses, SSNs and identifiers in
`working-set/patients.csv` are fabricated. There is no HIPAA or privacy
consideration attached to this data.

MITRE states that Synthea-generated data is free from cost, privacy and
security restrictions. The Synthea generator itself is open source under the
Apache License 2.0.

## Layout

```
seed-data/
├── raw/                          # gitignored, download this yourself
├── filter_synthea.py             # pass 1: filter to the working set
├── working-set/                  # committed, output of pass 1
├── curated/                      # committed, hand-authored
│   ├── eligible_codes.csv
│   └── criteria.csv
└── load.py                       # pass 3: insert into both databases
```

`raw/` is not committed. The full export is around 79 MB, roughly half of it
`observations.csv`, which phase 1 does not use.

## Pass 1: filtering

```bash
python filter_synthea.py --stats      # patient counts per payer
python filter_synthea.py              # write the working set
```

The raw export describes a whole regional healthcare system: 10 payers, 1,171
patients, 1,119 organizations, 5,855 providers, 53,346 encounters. This system
models a single payer and the provider organizations it contracts with, so pass
1 reduces the export to that payer's world.

**Closure, not size, is the goal.** `V2__curated_and_reference_tables.sql`
declares seven foreign keys. If a coverage row survives filtering but its
patient does not, `load.py` fails on a constraint violation rather than warning.
The script asserts closure before exiting and returns non-zero on failure.

### Current working set

| File | Rows |
|---|---|
| `patients.csv` | 234 |
| `payers.csv` | 1 |
| `payer_transitions.csv` | 307 |
| `providers.csv` | 319 |
| `organizations.csv` | 319 |
| `conditions.csv` | 2,051 |
| `network_participation.csv` | 319 |

114 distinct condition codes, which is comfortable headroom for hand-authoring
criteria in PA-10.

### Why Humana

Mid-sized in this population at 234 patients. Medicaid covers 497, nearly half
the population, which would leave too few organizations outside the network to
make an out-of-network rejection realistic. Dual Eligible covers 22, far too
thin to author criteria against.

Change `PAYER_ID` at the top of `filter_synthea.py` and re-run to select a
different one.

## What is absent from the working set, and why

`encounters.csv` is the join spine that pass 1 filters *through*. It carries
`PATIENT`, `ORGANIZATION`, `PROVIDER` and `PAYER` on one row, which resolves the
patient, provider and organization sets in a single streaming pass. No table in
either schema loads it, so it is not written out.

`procedures.csv`, `medications.csv` and `imaging_studies.csv` are read directly
from `raw/` when choosing prior-auth eligible codes in PA-09. They are inputs to
a decision, not seed data.

## Derived rather than invented: network_participation

Synthea has no concept of a provider network, so the `OUT_OF_NETWORK` intake
rejection has no direct data source. Rather than marking an arbitrary holdout,
pass 1 derives it while scanning encounters:

- **In-network:** the organization appears on an encounter this payer covered
- **Out-of-network:** the organization treated one of these patients, but only
  under other coverage

On the current working set that splits 252 in-network against 67
out-of-network. Both buckets reflect observed behaviour, so `OUT_OF_NETWORK`
rejections rest on real relationships rather than fabricated ones.

## Hand-authored content

Two files in `curated/` have no Synthea equivalent and are written by hand:

- `eligible_codes.csv` — which codes require prior authorization. A payer policy
  decision, not clinical data.
- `criteria.csv` — one clinical criterion per eligible code.

Real payers license commercial criteria sets such as InterQual and MCG. Those
cannot be reproduced here and are not needed: a simplified criterion per code
gives the rule engine something real to branch on.

## Attribution

Synthetic patient data generated by [Synthea](https://github.com/synthetichealth/synthea),
an open-source synthetic patient population simulator from The MITRE
Corporation.

> Walonoski J, Kramer M, Nichols J, et al. Synthea: An approach, method, and
> software mechanism for generating synthetic patients and the synthetic
> electronic health care record. *Journal of the American Medical Informatics
> Association*, 25(3):230–238, 2018.
