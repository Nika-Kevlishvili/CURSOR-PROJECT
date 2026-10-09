PDT-2931 — Product contract mass import (Dev)
Generated: 2026-06-05T09:54:57.748Z
API: http://10.236.20.11:8091/

Files:
  1) PDT-2931-PRODUCT_CONTRACTS-template-Dev.xlsx — official empty template from Dev
  2) PDT-2931-mass-import-CREATE-row-sample.xlsx — row 2: CREATE (empty A/B/C)
  3) PDT-2931-mass-import-CREATE-E-C-three-rows-sample.xlsx — rows 2–4: CREATE + edit E + edit C

Replace <<...>> placeholders before upload.
Upload: POST mass-import/PRODUCT_CONTRACTS/files/upload (multipart file, expect 202).
Interest rate column AX prefilled: Interestrate1780600372762
