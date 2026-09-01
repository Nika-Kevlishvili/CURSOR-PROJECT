#!/usr/bin/env python3
"""Generate 40 POD mass-import xlsx files cloned from Dev2 POD id 34493.

Each file: sheet Sheet1, official template headers, 25_000 create rows.
Dev2 heap: 50k of this template OOM'd on upload; use 25k.
"""
from __future__ import annotations

import os
import time
from pathlib import Path

import xlsxwriter

OUT_DIR = Path(__file__).resolve().parent
ROWS_PER_FILE = 10_000
FILE_COUNT = 200
ALPHABET = b"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

HEADERS = [
    "POD_id",
    "POD_version",
    "POD_create_edit (C or E)",
    "POD_identifier_code_number",
    "POD_additional_identifier",
    "POD_name",
    "POD_grid_operator",
    "POD_coordinator_for_balancing_groups",
    "POD_estimated_monthly_average_consumption_production_in_kwh",
    "POD_type_of_user",
    "POD_customer_identifier_by_grid_operator",
    "POD_customer_number_by_grid_operator",
    "POD_type_of_point_of_delivery",
    "POD_purpose_of_consumption",
    "POD_voltage_level",
    "POD_measurement_type",
    "POD_measurement_type_nomenclature",
    "POD_provided_power",
    "POD_multiplier",
    "POD_address_unregistered_country",
    "POD_address_country",
    "POD_address_region",
    "POD_address_municipality",
    "POD_address_populated_place",
    "POD_address_zip_code",
    "POD_address_disctrict",
    "POD_address_residential_area",
    "POD_address_quarter",
    "POD_address_boulevard",
    "POD_address_street_boulevard",
    "POD_address_number",
    "POD_address_additional_information",
    "POD_address_block",
    "POD_address_entrance",
    "POD_address_floor",
    "POD_address_apartment",
    "POD_address_mailbox",
    "latitude",
    "longitude",
    "POD_impossibility_of_disconnection",
    "POD_blocked_for_disconnection",
    "POD_blocked_for_disconnection_from_date",
    "POD_blocked_for_disconnection_to_date",
    "POD_blocked_for_disconnection_reason",
    "POD_blocked_for_disconnection_additional_information",
    "POD_blocked_for_billing",
    "POD_blocked_for_billing_from_date",
    "POD_blocked_for_billing_to_date",
    "POD_blocked_for_billing_reason",
    "POD_blocked_for_billing_additional_information",
    "POD_customer",
    "POD_additional_param_1",
    "POD_additional_param_2",
    "POD_additional_param_3",
    "POD_energy_sharing_model",
    "POD_rps_number",
]

# Clone of Dev2 POD 34493 latest version (detail 34531). Empty cells stay None.
BASE_ROW = [
    None,  # 0 POD_id empty => create
    None,  # 1 version
    None,  # 2 create/edit
    None,  # 3 identifier filled per row
    None,  # 4 additional identifier
    "BIG DATA POD",
    "BIG DATA",
    None,
    1,
    None,
    None,
    None,
    "CONSUMER",
    "HOUSEHOLD",
    "LOW",
    "SLP",
    "BIG DATA MEASUREMENT TYPE",
    None,
    None,
    "NO",
    "STANDARD COUNTRY",
    "STANDARD REGION",
    "STANDARD MUNICIPALITY",
    "STANDARD POPULATED PLACE",
    "STANDARD ZIP CODE",
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    "NO",
    "NO",
    None,
    None,
    None,
    None,
    "NO",
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
    None,
]


def make_identifier(seq: int) -> str:
    """G2M + 7-digit unique sequence + 23 random A-Z0-9 = 33 chars, pattern ^[0-9A-Z]+$."""
    suffix = bytearray(23)
    rnd = os.urandom(23)
    for i, b in enumerate(rnd):
        suffix[i] = ALPHABET[b % 36]
    return f"G2M{seq:07d}{suffix.decode('ascii')}"


def write_file(part: int) -> Path:
    path = OUT_DIR / f"pod-mass-import-dev2-34493-part-{part:03d}-of-{FILE_COUNT:03d}.xlsx"
    workbook = xlsxwriter.Workbook(
        str(path),
        {
            "constant_memory": True,
            "strings_to_urls": False,
        },
    )
    sheet = workbook.add_worksheet("Sheet1")
    sheet.write_row(0, 0, HEADERS)
    start_seq = (part - 1) * ROWS_PER_FILE + 1
    row_values = list(BASE_ROW)
    for offset in range(ROWS_PER_FILE):
        seq = start_seq + offset
        row_values[3] = make_identifier(seq)
        sheet.write_row(offset + 1, 0, row_values)
    workbook.close()
    return path


def main() -> None:
    started = time.time()
    assert len(HEADERS) == 56, len(HEADERS)
    assert len(BASE_ROW) == 56, len(BASE_ROW)
    for part in range(1, FILE_COUNT + 1):
        t0 = time.time()
        path = write_file(part)
        size_mb = path.stat().st_size / (1024 * 1024)
        print(
            f"WROTE {path.name} rows={ROWS_PER_FILE} size_mb={size_mb:.1f} elapsed_s={time.time() - t0:.1f}",
            flush=True,
        )
    print(f"DONE files={FILE_COUNT} total_rows={FILE_COUNT * ROWS_PER_FILE} elapsed_s={time.time() - started:.1f}")


if __name__ == "__main__":
    main()
