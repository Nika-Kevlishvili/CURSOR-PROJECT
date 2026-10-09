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