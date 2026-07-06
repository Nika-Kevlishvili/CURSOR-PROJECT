-- PDT-3035 / Prod process 2114 — Test mirror seed for TC-BE-1 (NPE until-amount)
-- Run once on Test DB before TC-BE-1. API PUT coerces null until-term → false; this SQL does not.
--
-- Target: EPES2606002385 — null until-term + product entry_into_force_value (basic entry_in_force_date stays null)

UPDATE product_contract.contract_details cd
SET
  contract_term_until_the_amount = NULL,
  contract_term_until_the_volume = NULL,
  entry_into_force_value = (CURRENT_DATE + INTERVAL '30 days')::timestamptz
FROM product_contract.contracts c
WHERE c.id = cd.contract_id
  AND cd.version_id = 1
  AND c.contract_number = 'EPES2606002385';

-- Verify:
-- SELECT c.contract_number, cd.contract_term_until_the_amount, cd.entry_into_force_value, cd.entry_into_force_date
-- FROM product_contract.contracts c
-- JOIN product_contract.contract_details cd ON cd.contract_id = c.id AND cd.version_id = 1
-- WHERE c.contract_number = 'EPES2606002385';
