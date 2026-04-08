-- Count of liability rows returned by PowerSupplyDisconnectionReminderRepository.execute for reminder id 1976 (Dev)
WITH blocking_checks AS (SELECT
                                                     mofb.id AS mass_operation_id,
                                                     cl.customer_id,
                                                     cl.id AS customer_liability_id
                                                 FROM receivable.mass_operation_for_blocking mofb
                                                          JOIN receivable.vw_mass_operation_for_blocking_excl_customers mofbec
                                                               ON mofbec.mass_operation_for_blocking_id = mofb.id
                                                          JOIN receivable.customer_liabilities cl
                                                               ON cl.customer_id = mofbec.customer_id
                                                          LEFT JOIN invoice.invoices i ON i.id = cl.invoice_id
                                                          LEFT JOIN nomenclature.currencies cr1 ON cr1.id = cl.currency_id
                                                          JOIN receivable.power_supply_disconnection_reminders psdr
                                                               ON psdr.id IN (1976)
                                                 WHERE
                                                     mofb.mass_operation_blocking_status = 'EXECUTED'
                                                   AND mofb.status = 'ACTIVE'
                                                   AND mofb.type && '{CUSTOMER_LIABILITY}'
                                                   AND mofb.customer_condition_type = 'LIST_OF_CUSTOMERS'
                                                   AND (
                                                     i.invoice_number IS NULL
                                                         OR NOT EXISTS (
                                                         SELECT 1
                                                         FROM receivable.mass_operation_for_blocking_exclution_prefixes mofbp
                                                                  JOIN nomenclature.prefixes pref ON pref.id = mofbp.prefix_id
                                                         WHERE mofbp.mass_operation_for_blocking_id = mofb.id
                                                           AND mofbp.status = 'ACTIVE'
                                                           AND i.invoice_number LIKE pref.name || '-%'
                                                     )
                                                     )
                                                   AND (
                                                     mofb.currency_id IS NULL
                                                         OR (
                                                         cl.currency_id = mofb.currency_id
                                                             AND (mofb.exclusion_by_amount_less_than    IS NULL OR cl.current_amount                      >= mofb.exclusion_by_amount_less_than)
                                                             AND (mofb.exclusion_by_amount_greater_than IS NULL OR cl.current_amount                      <= mofb.exclusion_by_amount_greater_than)
                                                         )
                                                         OR (
                                                         cl.currency_id <> mofb.currency_id
                                                             AND (mofb.exclusion_by_amount_less_than    IS NULL OR cl.current_amount * cr1.alt_ccy_exchange_rate >= mofb.exclusion_by_amount_less_than)
                                                             AND (mofb.exclusion_by_amount_greater_than IS NULL OR cl.current_amount * cr1.alt_ccy_exchange_rate <= mofb.exclusion_by_amount_greater_than)
                                                         )
                                                     )
                                                   AND (
                                                     (COALESCE(mofb.blocked_for_reminder_letters, false) = TRUE
                                                         AND DATE(psdr.customer_send_date)
                                                          BETWEEN mofb.blocked_for_reminder_letters_from_date
                                                          AND COALESCE(mofb.blocked_for_reminder_letters_to_date, CURRENT_DATE))
                                                     )
                                                 GROUP BY mofb.id, cl.customer_id, cl.id
                                                 UNION ALL
                                                 SELECT
                                                     mofb.id AS mass_operation_id,
                                                     cl.customer_id,
                                                     cl.id AS customer_liability_id
                                                 FROM receivable.mass_operation_for_blocking mofb
                                                          JOIN receivable.customer_liabilities cl ON TRUE
                                                          LEFT JOIN invoice.invoices i ON i.id = cl.invoice_id
                                                          LEFT JOIN nomenclature.currencies cr1 ON cr1.id = cl.currency_id
                                                          JOIN receivable.power_supply_disconnection_reminders psdr
                                                               ON psdr.id IN (1976)
                                                 WHERE
                                                     mofb.mass_operation_blocking_status = 'EXECUTED'
                                                   AND mofb.status = 'ACTIVE'
                                                   AND mofb.type && '{CUSTOMER_LIABILITY}'
                                                   AND mofb.customer_condition_type = 'ALL_CUSTOMERS'
                                                   AND (
                                                     i.invoice_number IS NULL
                                                         OR NOT EXISTS (
                                                         SELECT 1
                                                         FROM receivable.mass_operation_for_blocking_exclution_prefixes mofbp
                                                                  JOIN nomenclature.prefixes pref ON pref.id = mofbp.prefix_id
                                                         WHERE mofbp.mass_operation_for_blocking_id = mofb.id
                                                           AND mofbp.status = 'ACTIVE'
                                                           AND i.invoice_number LIKE pref.name || '-%'
                                                     )
                                                     )
                                                   AND (
                                                     mofb.currency_id IS NULL
                                                         OR (
                                                         cl.currency_id = mofb.currency_id
                                                             AND (mofb.exclusion_by_amount_less_than    IS NULL OR cl.current_amount                      >= mofb.exclusion_by_amount_less_than)
                                                             AND (mofb.exclusion_by_amount_greater_than IS NULL OR cl.current_amount                      <= mofb.exclusion_by_amount_greater_than)
                                                         )
                                                         OR (
                                                         cl.currency_id <> mofb.currency_id
                                                             AND (mofb.exclusion_by_amount_less_than    IS NULL OR cl.current_amount * cr1.alt_ccy_exchange_rate >= mofb.exclusion_by_amount_less_than)
                                                             AND (mofb.exclusion_by_amount_greater_than IS NULL OR cl.current_amount * cr1.alt_ccy_exchange_rate <= mofb.exclusion_by_amount_greater_than)
                                                         )
                                                     )
                                                   AND (
                                                     (COALESCE(mofb.blocked_for_reminder_letters, false) = TRUE
                                                         AND DATE(psdr.customer_send_date)
                                                          BETWEEN mofb.blocked_for_reminder_letters_from_date
                                                          AND COALESCE(mofb.blocked_for_reminder_letters_to_date, CURRENT_DATE))

                                                     )
                                                 GROUP BY mofb.id, cl.customer_id, cl.id
                                                 UNION ALL
                                                 SELECT
                                                     mofb.id AS mass_operation_id,
                                                     cl.customer_id,
                                                     cl.id AS customer_liability_id
                                                 FROM receivable.mass_operation_for_blocking mofb
                                                          JOIN LATERAL receivable.liability_condition_eval_exact(mofb.customer_conditions) l ON TRUE
                                                          JOIN receivable.customer_liabilities cl ON cl.id = l.liability_id
                                                          LEFT JOIN invoice.invoices i ON i.id = cl.invoice_id
                                                          LEFT JOIN nomenclature.currencies cr1 ON cr1.id = cl.currency_id
                                                          JOIN receivable.power_supply_disconnection_reminders psdr
                                                               ON psdr.id IN (1976)
                                                 WHERE
                                                     mofb.mass_operation_blocking_status = 'EXECUTED'
                                                   AND mofb.status = 'ACTIVE'
                                                   AND mofb.type && '{CUSTOMER_LIABILITY}'
                                                   AND mofb.customer_condition_type = 'CUSTOMERS_UNDER_CONDITIONS'
                                                   AND (
                                                     i.invoice_number IS NULL
                                                         OR NOT EXISTS (
                                                         SELECT 1
                                                         FROM receivable.mass_operation_for_blocking_exclution_prefixes mofbp
                                                                  JOIN nomenclature.prefixes pref ON pref.id = mofbp.prefix_id
                                                         WHERE mofbp.mass_operation_for_blocking_id = mofb.id
                                                           AND mofbp.status = 'ACTIVE'
                                                           AND i.invoice_number LIKE pref.name || '-%'
                                                     )
                                                     )
                                                   AND (
                                                     mofb.currency_id IS NULL
                                                         OR (
                                                         cl.currency_id = mofb.currency_id
                                                             AND (mofb.exclusion_by_amount_less_than    IS NULL OR cl.current_amount >= mofb.exclusion_by_amount_less_than)
                                                             AND (mofb.exclusion_by_amount_greater_than IS NULL OR cl.current_amount <= mofb.exclusion_by_amount_greater_than)
                                                         )
                                                         OR (
                                                         cl.currency_id <> mofb.currency_id
                                                             AND (mofb.exclusion_by_amount_less_than    IS NULL OR cl.current_amount * cr1.alt_ccy_exchange_rate >= mofb.exclusion_by_amount_less_than)
                                                             AND (mofb.exclusion_by_amount_greater_than IS NULL OR cl.current_amount * cr1.alt_ccy_exchange_rate <= mofb.exclusion_by_amount_greater_than)
                                                         )
                                                     )
                                                   AND (
                                                     (COALESCE(mofb.blocked_for_reminder_letters, false)
                                                         AND DATE(psdr.customer_send_date) BETWEEN mofb.blocked_for_reminder_letters_from_date
                                                          AND COALESCE(mofb.blocked_for_reminder_letters_to_date, CURRENT_DATE))

                                                     )
                                                 GROUP BY mofb.id, cl.customer_id, cl.id
                        ),
                             liability_calculations AS (
                                 SELECT
                                     cl.customer_id,
                                     cl.current_amount,
                                     cl.id AS customer_liability_id,
                                     psdr.id AS psdrId,
                                     psdr.liability_amount_from AS amountfrom,
                                     psdr.liability_amount_to   AS amountto,
                                     cr1.id AS currencyId,
                                     cr1.alt_currency_id        AS alternativeCurrencyId,
                                     cr1.alt_ccy_exchange_rate  AS alternativeExchangeRate,
                                     cr2.id AS psdrCurrencyId,
                                     cr2.alt_ccy_exchange_rate  AS psdrCurrencyAlternativeExchangeRate,
                                     DATE(psdr.customer_send_date) AS customerSendDate,
                                     i.invoice_number AS invoiceNumber,
                                     CASE
                                         WHEN psdr.currency_id IS NOT NULL THEN
                                             CASE
                                                 WHEN cl.currency_id = psdr.currency_id THEN cl.current_amount
                                                 ELSE cl.current_amount * cr1.alt_ccy_exchange_rate
                                                 END
                                         ELSE
                                             CASE
                                                 WHEN cr1.id = (SELECT id FROM nomenclature.currencies cr3 WHERE cr3.is_default = TRUE)
                                                     THEN cl.current_amount
                                                 ELSE cl.current_amount * cr1.alt_ccy_exchange_rate
                                                 END
                                         END AS calculated_amount,
                                     SUM(
                                     CASE
                                         WHEN psdr.currency_id IS NOT NULL THEN
                                             CASE
                                                 WHEN cl.currency_id = psdr.currency_id THEN cl.current_amount
                                                 ELSE cl.current_amount * cr1.alt_ccy_exchange_rate
                                                 END
                                         ELSE
                                             CASE
                                                 WHEN cr1.id = (SELECT id FROM nomenclature.currencies cr3 WHERE cr3.is_default = TRUE)
                                                     THEN cl.current_amount
                                                 ELSE cl.current_amount * cr1.alt_ccy_exchange_rate
                                                 END
                                         END
                                        ) OVER (PARTITION BY cl.customer_id, psdr.id) AS sumCurrentAmount,
                                     CASE
                                         WHEN psdr.currency_id IS NOT NULL THEN
                                             CASE
                                                 WHEN cl.currency_id = psdr.currency_id THEN cr1.id
                                                 ELSE cr2.id
                                                 END
                                         ELSE
                                             CASE
                                                 WHEN cr1.id = (SELECT cr3.id FROM nomenclature.currencies cr3 WHERE cr3.is_default = TRUE) THEN cr1.id
                                                 ELSE (SELECT cr4.id FROM nomenclature.currencies cr4 WHERE cr4.is_default = TRUE)
                                                 END
                                         END AS currentCurrencyId
                                 FROM receivable.customer_liabilities cl
                                          LEFT JOIN invoice.invoices i ON i.id = cl.invoice_id
                                          JOIN nomenclature.currencies cr1 ON cr1.id = cl.currency_id
                                          LEFT JOIN receivable.power_supply_disconnection_reminders psdr
                                                    ON psdr.id IN (1976)
                                          LEFT JOIN nomenclature.currencies cr2 ON cr2.id = psdr.currency_id
                                 WHERE
                                     cl.status = 'ACTIVE'
                                   AND cl.due_date <= CURRENT_DATE
                                   AND cl.due_date <= psdr.liabilities_max_due_date
                                   AND cl.current_amount > 0
                                   AND (
                                     cl.contract_billing_group_id IS NOT NULL
                                         OR (
                                         cl.contract_billing_group_id IS NULL
                                             AND cl.outgoing_document_type = 'RESCHEDULING'
                                             AND EXISTS (
                                             SELECT 1
                                             FROM receivable.rescheduling_liabilities rl1
                                                      JOIN receivable.rescheduling_liabilities rl2
                                                           ON rl1.rescheduling_id = rl2.rescheduling_id
                                                      JOIN receivable.customer_liabilities cl2
                                                           ON rl2.customer_liabilitie_id = cl2.id
                                             WHERE rl1.rescheduling_id = (
                                                 SELECT r.id
                                                 FROM receivable.reschedulings r
                                                 WHERE r.id = cl.rescheduling_id
                                             )
                                               AND cl2.contract_billing_group_id IS NOT NULL
                                               AND cl2.status = 'ACTIVE'
                                         )
                                         )
                                     )
                                   AND CASE
                                           WHEN COALESCE(cl.blocked_for_reminder_letters, false) = TRUE THEN
                                               DATE(psdr.customer_send_date)
                                                   NOT BETWEEN cl.blocked_for_reminder_letters_from_date
                                                   AND COALESCE(blocked_for_reminder_letters_to_date, CURRENT_DATE)
                                           ELSE TRUE
                                     END
                                   AND CASE
                                           WHEN psdr.customer_filter_type = 'NONE' THEN TRUE
                                           WHEN psdr.customer_filter_type = 'EXCLUDED' THEN
                                               NOT EXISTS (
                                                   SELECT 1
                                                   FROM unnest(string_to_array(psdr.customer_list, ',')) AS customer_identifier
                                                            JOIN customer.customers c ON c.identifier = trim(customer_identifier)
                                                   WHERE c.id = cl.customer_id
                                               )
                                           WHEN psdr.customer_filter_type = 'INCLUDED' THEN
                                               EXISTS (
                                                   SELECT 1
                                                   FROM unnest(string_to_array(psdr.customer_list, ',')) AS customer_identifier
                                                            JOIN customer.customers c ON c.identifier = trim(customer_identifier)
                                                   WHERE c.id = cl.customer_id
                                               )
                                           ELSE TRUE
                                     END
                             ),
                             filtered_liabilities AS (
                                 SELECT lc.*
                                 FROM liability_calculations lc
                                 WHERE (lc.amountfrom IS NULL OR lc.sumCurrentAmount >= lc.amountfrom)
                                    AND (lc.amountto   IS NULL OR lc.sumCurrentAmount <= lc.amountto)
                             )
                        SELECT COUNT(*)::bigint AS phoenix_execute_row_count
                        FROM filtered_liabilities fl
                                 LEFT JOIN blocking_checks bc
                                           ON fl.customer_liability_id = bc.customer_liability_id
                        WHERE bc.mass_operation_id IS NULL;
