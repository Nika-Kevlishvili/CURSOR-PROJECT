INSERT INTO nomenclature.currencies (id, name, print_name, abbreviation, full_name, alt_currency_id,
                                     alt_ccy_exchange_rate, main_ccy_start_date, main_ccy, is_default, create_date,
                                     system_user_id, modify_date, modify_system_user_id, status, ordering_id)
VALUES (26,
        'EUR',
        'EUR',
        'euro',
        'EURO',
        null,
        2,
        '2024-08-01',
        true,
        true,
        '2023-03-23 11:45:08.533576 +00:00',
        'm47500',
        '2024-10-17 12:46:08.360461 +00:00',
        'phoenix.testa',
        'ACTIVE',
        39);

INSERT INTO nomenclature.currencies (id, name, print_name, abbreviation, full_name, alt_currency_id,
                                     alt_ccy_exchange_rate, main_ccy_start_date, main_ccy, is_default, create_date,
                                     system_user_id, modify_date, modify_system_user_id, status, ordering_id)
VALUES (61,
        'лева',
        'лева',
        'лв.',
        'Български лев',
        26,
        0.5,
        null,
        false,
        false,
        '2023-03-27 11:04:09.382525 +00:00',
        'm47500',
        '2024-10-17 12:46:08.376111 +00:00',
        'phoenix.testa',
        'ACTIVE',
        99);

update nomenclature.currencies
set alt_currency_id = 61
where id = 26;

INSERT INTO nomenclature.price_component_price_types (id, name, is_default, create_date, system_user_id, modify_date,
                                                      modify_system_user_id, status, ordering_id)
VALUES (88,
        'LUKAS TYPE',
        false,
        '2024-03-18 09:14:47.690164 +00:00',
        'phoenix.testa',
        '2024-10-17 12:55:56.954624 +00:00',
        'phoenix.testa',
        'ACTIVE',
        53);

INSERT INTO nomenclature.price_component_value_types (id, name, is_default, create_date, system_user_id, modify_date,
                                                      modify_system_user_id, status, ordering_id)
VALUES (77,
        'LUKAS VALUE',
        false,
        '2024-03-18 09:15:21.352108 +00:00',
        'phoenix.testa',
        '2024-03-18 09:15:21.352110 +00:00',
        'phoenix.testa',
        'ACTIVE',
        71);

INSERT INTO nomenclature.vat_rates (id, name, value_in_percent, start_date, global_vat_rate, create_date,
                                    system_user_id, modify_date, modify_system_user_id, status, ordering_id)
VALUES (327,
        'LUKAS VAT RATE (10%)',
        10,
        null,
        false,
        '2024-03-18 09:16:50.784634 +00:00',
        'phoenix.testa',
        '2024-10-04 11:32:44.896894 +00:00',
        'l13943',
        'ACTIVE',
        151);

INSERT INTO price_component.price_components (id, name, invoice_and_template_text, price_component_price_type_id,
                                              price_component_value_type_id, currency_id, vat_rate_id, number_type,
                                              global_vat_rate, income_account_number, cost_center_controlling_order,
                                              contract_template_tag, price_in_words, price_formula,
                                              issued_separate_invoice, conditions, status, create_date, system_user_id,
                                              modify_date, modify_system_user_id, price_component_group_detail_id,
                                              discount, xenergie_application, don_not_include_in_the_vat_base,
                                              alt_invoice_recipient_customer_detail_id)
VALUES (59674,
        'Price Component for per piece',
        'per piece price',
        88,
        77,
        26,
        327,
        'POSITIVE',
        false,
        '10',
        '20',
        '30',
        null,
        'IF{($X1$<>$X2$)$X3$}ELSEIF{($X2$>$X1$)$X2$}ELSE{$X1$}+IF{($X7$/$X1$=7)$X7$}',
        'INVOICE_THREE',
        null,
        'ACTIVE',
        '2024-10-07 07:17:09.045540 +00:00',
        'l13943',
        '2024-10-07 11:48:58.734610 +00:00',
        'l13943',
        null,
        false,
        null,
        false,
        null);

INSERT INTO billing_run.run_contracts (id, run_id, contract_id, processing_status, contract_type, created_from,
                                       last_contract_detail_id)
VALUES (1, 8602, 5261, 'CREATED', 'PRODUCT_CONTRACT', 'PER_PIECE', null);

insert into billing_run.bg_invoice_slots(bg_invoice_slot_id, run_id, status)
values (63122, 8602, 'CREATED');

INSERT INTO billing_run.per_piece_details (id,
                                           contract_detail_id, run_contract_id, price_component_id,
                                           bg_invoice_slot_id,
                                           quantity, price_formula, run_total_price, status, billing_run_id,
                                           product_detail_id, customer_detail_id, product_id, customer_id, pc_group_id,
                                           service_unit_id)
VALUES (1,
        4345,
        1,
        59674,
        63122,
        10,
        'IF{($X1$<>$X2$)$X3$}ELSEIF{($X2$>$X1$)$X2$}ELSE{$X1$}+IF{($X7$/$X1$=7)$X7$}',
        null,
        'CREATED',
        8602,
        2703,
        174526321,
        2346,
        6004939,
        898,
        36);
