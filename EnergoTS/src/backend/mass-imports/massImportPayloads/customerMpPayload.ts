export function customerMassPayload() {
    return {
        'customer_number': {
            'value': '', // Will be auto-generated
            'cellNumber': 'A2'
        },
        'customer_version': {
            'value': '', // Default version
            'cellNumber': 'B2'
        },
        'customer_type': {
            'value': '', // LEGAL_ENTITY or PRIVATE_CUSTOMER - must be set per customer
            'cellNumber': 'C2'
        },
        'customer_business_activity': {
            'value': '', // Default false for private customers
            'cellNumber': 'D2'
        },
        'customer_public_procurement_law': {
            'value': 'NO', // Default false (procurementLaw from legal entity)
            'cellNumber': 'E2'
        },
        'customer_marketing_comm_consent': {
            'value': 'NO', // Default false (shared field)
            'cellNumber': 'F2'
        },
        'unrecognized_uic_pn_turn_off_validation': {
            'value': 'YES', // Default validation enabled
            'cellNumber': 'G2'
        },
        'customer_gdpr_regulation_consent': {
            'value': '', // Default false (from private customer details)
            'cellNumber': 'H2'
        },
        'customer_identifier': {
            'value': '', // Will be generated with timestamp/random - leave empty for auto-generation
            'cellNumber': 'I2'
        },
        'customer_status': {
            'value': 'NEW', // Default status for new customers (shared field)
            'cellNumber': 'J2'
        },
        'customer_old_customer_numbers': {
            'value': '', // Usually null/empty for new customers
            'cellNumber': 'K2'
        },
        'customer_vat_number': {
            'value': '', // Usually null for private customers, required for legal entities
            'cellNumber': 'L2'
        },
        'customer_name': {
            'value': '',
            'cellNumber': 'M2'
        },
        'customer_name_transl': {
            'value': '',
            'cellNumber': 'N2'
        },
        'customer_middle_name': {
            'value': '',
            'cellNumber': 'O2'
        },
        'customer_middle_name_transl': {
            'value': '',
            'cellNumber': 'P2'
        },
        'customer_last_name': {
            'value': '',
            'cellNumber': 'Q2'
        },
        'customer_last_name_transl': {
            'value': '',
            'cellNumber': 'R2'
        },
        'customer_legal_form': {
            'value': '',
            'cellNumber': 'S2'
        },
        'customer_legal_form_transl': {
            'value': '',
            'cellNumber': 'T2'
        },
        'customer_business_activity_name': {
            'value': '',
            'cellNumber': 'U2'
        },
        'customer_business_activity_name_transl': {
            'value': '',
            'cellNumber': 'V2'
        },
        'customer_ownership_form': {
            'value': '',
            'cellNumber': 'W2'
        },
        'customer_economic_branch_ci': {
            'value': '',
            'cellNumber': 'X2'
        },
        'customer_economic_branch_ncea': {
            'value': '',
            'cellNumber': 'Y2'
        },
        'customer_segment_1': {
            'value': 'Неуточнен', //default for all types of customers
            'cellNumber': 'Z2'
        },
        'customer_segment_2': {
            'value': '',
            'cellNumber': 'AA2'
        },
        'customer_segment_3': {
            'value': '',
            'cellNumber': 'AB2'
        },
        'customer_main_activity_subject': {
            'value': '',
            'cellNumber': 'AC2'
        },
        'customer_unregistered_address': {
            'value': 'YES', // Default false (corresponds to foreign field in address)
            'cellNumber': 'AD2'
        },
        'customer_address_country': {
            'value': 'АНГОЛА', // Should be set from envVariables.countries
            'cellNumber': 'AE2'
        },
        'customer_address_region': {
            'value': '', // Should be set from envVariables.regions
            'cellNumber': 'AF2'
        },
        'customer_address_municipality': {
            'value': '', // Should be set from envVariables.municipalities
            'cellNumber': 'AG2'
        },
        'customer_address_populated_place': {
            'value': '', // Should be set from envVariables.population_places
            'cellNumber': 'AH2'
        },
        'customer_address_zip_code': {
            'value': '', // Should be set from envVariables.zip_codes
            'cellNumber': 'AI2'
        },
        'customer_address_district': {
            'value': '', // Should be set from envVariables.districts
            'cellNumber': 'AJ2'
        },
        'customer_address_residential_area': {
            'value': '', // Should be set from envVariables.residential_areas
            'cellNumber': 'AK2'
        },
        'customer_address_street': {
            'value': '', // Should be set from envVariables.streets
            'cellNumber': 'AL2'
        },
        'customer_address_street_number': {
            'value': '', // Default street number from both payloads
            'cellNumber': 'AM2'
        },
        'customer_address_additional_info': {
            'value': '', // Usually null/empty
            'cellNumber': 'AN2'
        },
        'customer_address_block': {
            'value': '', // Usually null/empty
            'cellNumber': 'AO2'
        },
        'customer_address_entrance': {
            'value': '', // Usually null/empty
            'cellNumber': 'AP2'
        },
        'customer_address_floor': {
            'value': '', // Usually null/empty
            'cellNumber': 'AQ2'
        },
        'customer_address_apartment': {
            'value': '', // Usually null/empty
            'cellNumber': 'AR2'
        },
        'customer_address_mailbox': {
            'value': '', // Usually null/empty
            'cellNumber': 'AS2'
        },
        'customer_address_region_foreign': {
            'value': 'MASS REGION',
            'cellNumber': 'AT2'
        },
        'customer_address_municipality_foreign': {
            'value': 'MASS MUNICIPALITY',
            'cellNumber': 'AU2'
        },
        'customer_address_populated_place_foreign': {
            'value': 'MASS POPULATED PLACE',
            'cellNumber': 'AV2'
        },
        'customer_address_zip_code_foreign': {
            'value': 'MASS ZIP',
            'cellNumber': 'AW2'
        },
        'customer_address_district_foreign': {
            'value': '',
            'cellNumber': 'AX2'
        },
        'customer_address_residential_area_type_foreign': {
            'value': '',
            'cellNumber': 'AY2'
        },
        'customer_address_residential_area_foreign': {
            'value': '',
            'cellNumber': 'AZ2'
        },
        'customer_address_street_type_foreign': {
            'value': '',
            'cellNumber': 'BA2'
        },
        'customer_address_street_foreign': {
            'value': '',
            'cellNumber': 'BB2'
        },
        'customer_direct_debit': {
            'value': 'NO', // Default false (from bankingDetails)
            'cellNumber': 'BC2'
        },
        'customer_bank': {
            'value': '', // Should be set from envVariables.banks
            'cellNumber': 'BD2'
        },
        'customer_iban': {
            'value': '', // Usually null/empty by default
            'cellNumber': 'BE2'
        },
        'customer_declared_consumption': {
            'value': '', // Usually null by default
            'cellNumber': 'BF2'
        },
        'customer_preferences_1': {
            'value': '', // Should be set from envVariables.preferences
            'cellNumber': 'BG2'
        },
        'customer_preferences_2': {
            'value': '', // Additional preferences if needed
            'cellNumber': 'BH2'
        },
        'customer_preferences_3': {
            'value': '', // Additional preferences if needed
            'cellNumber': 'BI2'
        },
        'customer_credit_rating': {
            'value': '', // Should be set from envVariables.credit_rating
            'cellNumber': 'BJ2'
        },
        'related_customer1_identifier': {
            'value': '',
            'cellNumber': 'BK2'
        },
        'related_customer1_connection_types': {
            'value': '',
            'cellNumber': 'BL2'
        },
        'related_customer2_identifier': {
            'value': '',
            'cellNumber': 'BM2'
        },
        'related_customer2_connection_types': {
            'value': '',
            'cellNumber': 'BN2'
        },
        'related_customer3_identifier': {
            'value': '',
            'cellNumber': 'BO2'
        },
        'related_customer3_connection_types': {
            'value': '',
            'cellNumber': 'BP2'
        },
        'owner_customer1_identifier': {
            'value': '',
            'cellNumber': 'BQ2'
        },
        'owner1_belonging_capital_owner': {
            'value': '',
            'cellNumber': 'BR2'
        },
        'owner1_additional_info': {
            'value': '',
            'cellNumber': 'BS2'
        },
        'owner2_customer_identifier': {
            'value': '',
            'cellNumber': 'BT2'
        },
        'owner2_belonging_capital_owner': {
            'value': '',
            'cellNumber': 'BU2'
        },
        'owner2_additional_info': {
            'value': '',
            'cellNumber': 'BV2'
        },
        'owner3_customer_identifier': {
            'value': '',
            'cellNumber': 'BW2'
        },
        'owner3_belonging_capital_owner': {
            'value': '',
            'cellNumber': 'BX2'
        },
        'owner3_additional_info': {
            'value': '',
            'cellNumber': 'BY2'
        },
        'manager_name_1': {
            'value': '',
            'cellNumber': 'BZ2'
        },
        'manager_middle_name_1': {
            'value': '',
            'cellNumber': 'CA2'
        },
        'manager_surname_1': {
            'value': '',
            'cellNumber': 'CB2'
        },
        'manager_personal_number_1': {
            'value': '',
            'cellNumber': 'CC2'
        },
        'manager1_job_position': {
            'value': '',
            'cellNumber': 'CD2'
        },
        'manager1_position_held_from': {
            'value': '',
            'cellNumber': 'CE2'
        },
        'manager1_position_held_to': {
            'value': '',
            'cellNumber': 'CF2'
        },
        'manager1_birth_date': {
            'value': '',
            'cellNumber': 'CG2'
        },
        'manager1_representation_method': {
            'value': '',
            'cellNumber': 'CH2'
        },
        'manager1_title': {
            'value': '',
            'cellNumber': 'CI2'
        },
        'manager1_additional': {
            'value': '',
            'cellNumber': 'CJ2'
        },
        'manager2_name': {
            'value': '',
            'cellNumber': 'CK2'
        },
        'manager2_middle_name': {
            'value': '',
            'cellNumber': 'CL2'
        },
        'manager2_surname': {
            'value': '',
            'cellNumber': 'CM2'
        },
        'manager2_personal_number': {
            'value': '',
            'cellNumber': 'CN2'
        },
        'manager2_job_position': {
            'value': '',
            'cellNumber': 'CO2'
        },
        'manager2_position_held_from': {
            'value': '',
            'cellNumber': 'CP2'
        },
        'manager2_position_held_to': {
            'value': '',
            'cellNumber': 'CQ2'
        },
        'manager2_birth_date': {
            'value': '',
            'cellNumber': 'CR2'
        },
        'manager2_representation_method': {
            'value': '',
            'cellNumber': 'CS2'
        },
        'manager2_title': {
            'value': '',
            'cellNumber': 'CT2'
        },
        'manager2_additional': {
            'value': '',
            'cellNumber': 'CU2'
        },
        'manager3_name': {
            'value': '',
            'cellNumber': 'CV2'
        },
        'manager3_middle_name': {
            'value': '',
            'cellNumber': 'CW2'
        },
        'manager3_surname': {
            'value': '',
            'cellNumber': 'CX2'
        },
        'manager3_personal_number': {
            'value': '',
            'cellNumber': 'CY2'
        },
        'manager3_job_position': {
            'value': '',
            'cellNumber': 'CZ2'
        },
        'manager3_position_held_from': {
            'value': '',
            'cellNumber': 'DA2'
        },
        'manager3_position_held_to': {
            'value': '',
            'cellNumber': 'DB2'
        },
        'manager3_birth_date': {
            'value': '',
            'cellNumber': 'DC2'
        },
        'manager3_representation_method': {
            'value': '',
            'cellNumber': 'DD2'
        },
        'manager3_title': {
            'value': '',
            'cellNumber': 'DE2'
        },
        'manager3_additional': {
            'value': '',
            'cellNumber': 'DF2'
        },
        'account_manager1_manager_identifier': {
            'value': '',
            'cellNumber': 'DG2'
        },
        'account_manager1_type': {
            'value': '',
            'cellNumber': 'DH2'
        },
        'account_manager2_manager_identifier': {
            'value': '',
            'cellNumber': 'DI2'
        },
        'account_manager2_type': {
            'value': '',
            'cellNumber': 'DJ2'
        },
        'account_manager3_manager_identifier': {
            'value': '',
            'cellNumber': 'DK2'
        },
        'account_manager3_type': {
            'value': '',
            'cellNumber': 'DL2'
        },
        'communication_data1_contact_type_name': {
            'value': 'BILLING&INVOICE', // Default contact type name from payloads
            'cellNumber': 'DM2'
        },
        'communication_data1_address_foreign_address': {
            'value': 'YES', // Default false (matches address.foreign)
            'cellNumber': 'DN2'
        },
        'communication_data1_address_country': {
            'value': 'АНГОЛА',
            'cellNumber': 'DO2'
        },
        'communication_data1_address_region': {
            'value': '',
            'cellNumber': 'DP2'
        },
        'communication_data1_address_municipality': {
            'value': '',
            'cellNumber': 'DQ2'
        },
        'communication_data1_address_populated_place': {
            'value': '',
            'cellNumber': 'DR2'
        },
        'communication_data1_address_zip_code': {
            'value': '',
            'cellNumber': 'DS2'
        },
        'communication_data1_address_district': {
            'value': '',
            'cellNumber': 'DT2'
        },
        'communication_data1_address_residential_area': {
            'value': '',
            'cellNumber': 'DU2'
        },
        'communication_data1_address_street': {
            'value': '',
            'cellNumber': 'DV2'
        },
        'communication_data1_address_street_number': {
            'value': '',
            'cellNumber': 'DW2'
        },
        'communication_data1_address_additional_info': {
            'value': '',
            'cellNumber': 'DX2'
        },
        'communication_data1_address_block': {
            'value': '',
            'cellNumber': 'DY2'
        },
        'communication_data1_address_entrance': {
            'value': '',
            'cellNumber': 'DZ2'
        },
        'communication_data1_address_floor': {
            'value': '',
            'cellNumber': 'EA2'
        },
        'communication_data1_address_apartment': {
            'value': '',
            'cellNumber': 'EB2'
        },
        'communication_data1_address_mailbox': {
            'value': '',
            'cellNumber': 'EC2'
        },
        'communication_data1_address_region_foreign': {
            'value': 1,
            'cellNumber': 'ED2'
        },
        'communication_data1_address_municipality_foreign': {
            'value': 1,
            'cellNumber': 'EE2'
        },
        'communication_data1_address_populated_place_foreign': {
            'value': 1,
            'cellNumber': 'EF2'
        },
        'communication_data1_address_zip_code_foreign': {
            'value': 1,
            'cellNumber': 'EG2'
        },
        'communication_data1_address_district_foreign': {
            'value': '',
            'cellNumber': 'EH2'
        },
        'communication_data1_address_residential_area_type_foreign': {
            'value': '',
            'cellNumber': 'EI2'
        },
        'communication_data1_address_residential_area_foreign': {
            'value': '',
            'cellNumber': 'EJ2'
        },
        'communication_data1_address_street_type_foreign': {
            'value': '',
            'cellNumber': 'EK2'
        },
        'communication_data1_address_street_foreign': {
            'value': '',
            'cellNumber': 'EL2'
        },
        'communication_data1_platform1_name': {
            'value': '',
            'cellNumber': 'EM2'
        },
        'communication_data1_platform1_type': {
            'value': '',
            'cellNumber': 'EN2'
        },
        'communication_data1_platform2_name': {
            'value': '',
            'cellNumber': 'EO2'
        },
        'communication_data1_platform2_type': {
            'value': '',
            'cellNumber': 'EP2'
        },
        'communication_data1_platform3_name': {
            'value': '',
            'cellNumber': 'EQ2'
        },
        'communication_data1_platform3_type': {
            'value': '',
            'cellNumber': 'ER2'
        },
        'communication_data1_mobile_number_1': {
            'value': 12345,
            'cellNumber': 'ES2'
        },
        'communication_data1_send_sms_1': {
            'value': 'YES',
            'cellNumber': 'ET2'
        },
        'communication_data1_mobile_number_2': {
            'value': '',
            'cellNumber': 'EU2'
        },
        'communication_data1_send_sms_2': {
            'value': '',
            'cellNumber': 'EV2'
        },
        'communication_data1_mobile_number_3': {
            'value': '',
            'cellNumber': 'EW2'
        },
        'communication_data1_send_sms_3': {
            'value': '',
            'cellNumber': 'EX2'
        },
        'communication_data1_landline_phone_1': {
            'value': '',
            'cellNumber': 'EY2'
        },
        'communication_data1_landline_phone_2': {
            'value': '',
            'cellNumber': 'EZ2'
        },
        'communication_data1_landline_phone_3': {
            'value': '',
            'cellNumber': 'FA2'
        },
        'communication_data1_call_center_1': {
            'value': '',
            'cellNumber': 'FB2'
        },
        'communication_data1_call_center_2': {
            'value': '',
            'cellNumber': 'FC2'
        },
        'communication_data1_call_center_3': {
            'value': '',
            'cellNumber': 'FD2'
        },
        'communication_data1_fax_1': {
            'value': '',
            'cellNumber': 'FE2'
        },
        'communication_data1_fax_2': {
            'value': '',
            'cellNumber': 'FF2'
        },
        'communication_data1_fax_3': {
            'value': '',
            'cellNumber': 'FG2'
        },
        'communication_data1_email_1': {
            'value': '1@1.COM',
            'cellNumber': 'FH2'
        },
        'communication_data1_email_2': {
            'value': '',
            'cellNumber': 'FI2'
        },
        'communication_data1_email_3': {
            'value': '',
            'cellNumber': 'FJ2'
        },
        'communication_data1_website_1': {
            'value': '',
            'cellNumber': 'FK2'
        },
        'communication_data1_website_2': {
            'value': '',
            'cellNumber': 'FL2'
        },
        'communication_data1_website_3': {
            'value': '',
            'cellNumber': 'FM2'
        },
        'communication_data1_contact_purpose_1': {
            'value': 'Комуникация по договор',
            'cellNumber': 'FN2'
        },
        'communication_data1_contact_purpose_2': {
            'value': 'Фактуриране и плащане',
            'cellNumber': 'FO2'
        },
        'communication_data1_contact_purpose_3': {
            'value': '',
            'cellNumber': 'FP2'
        },
        'communication_data1_contact_person1_name': {
            'value': '',
            'cellNumber': 'FQ2'
        },
        'communication_data1_contact_person1_middle_name': {
            'value': '',
            'cellNumber': 'FR2'
        },
        'communication_data1_contact_person1_surname': {
            'value': '',
            'cellNumber': 'FS2'
        },
        'communication_data1_contact_person1_job_position': {
            'value': '',
            'cellNumber': 'FT2'
        },
        'communication_data1_contact_person1_position_held_from': {
            'value': '',
            'cellNumber': 'FU2'
        },
        'communication_data1_contact_person1_position_held_to': {
            'value': '',
            'cellNumber': 'FV2'
        },
        'communication_data1_contact_person1_birth_date': {
            'value': '',
            'cellNumber': 'FW2'
        },
        'communication_data1_contact_person1_additional_info': {
            'value': '',
            'cellNumber': 'FX2'
        },
        'communication_data1_contact_person1_title': {
            'value': '',
            'cellNumber': 'FY2'
        },
        'communication_data1_contact_person2_name': {
            'value': '',
            'cellNumber': 'FZ2'
        },
        'communication_data1_contact_person2_middle_name': {
            'value': '',
            'cellNumber': 'GA2'
        },
        'communication_data1_contact_person2_surname': {
            'value': '',
            'cellNumber': 'GB2'
        },
        'communication_data1_contact_person2_job_position': {
            'value': '',
            'cellNumber': 'GC2'
        },
        'communication_data1_contact_person2_position_held_from': {
            'value': '',
            'cellNumber': 'GD2'
        },
        'communication_data1_contact_person2_position_held_to': {
            'value': '',
            'cellNumber': 'GE2'
        },
        'communication_data1_contact_person2_birth_date': {
            'value': '',
            'cellNumber': 'GF2'
        },
        'communication_data1_contact_person2_additional_info': {
            'value': '',
            'cellNumber': 'GG2'
        },
        'communication_data1_contact_person2_title': {
            'value': '',
            'cellNumber': 'GH2'
        },
        'communication_data1_contact_person3_name': {
            'value': '',
            'cellNumber': 'GI2'
        },
        'communication_data1_contact_person3_middle_name': {
            'value': '',
            'cellNumber': 'GJ2'
        },
        'communication_data1_contact_person3_surname': {
            'value': '',
            'cellNumber': 'GK2'
        },
        'communication_data1_contact_person3_job_position': {
            'value': '',
            'cellNumber': 'GL2'
        },
        'communication_data1_contact_person3_position_held_from': {
            'value': '',
            'cellNumber': 'GM2'
        },
        'communication_data1_contact_person3_position_held_to': {
            'value': '',
            'cellNumber': 'GN2'
        },
        'communication_data1_contact_person3_birth_date': {
            'value': '',
            'cellNumber': 'GO2'
        },
        'communication_data1_contact_person3_additional_info': {
            'value': '',
            'cellNumber': 'GP2'
        },
        'communication_data1_contact_person3_title': {
            'value': '',
            'cellNumber': 'GQ2'
        },
        'communication_data2_contact_type_name': {
            'value': '',
            'cellNumber': 'GR2'
        },
        'communication_data2_address_foreign_address': {
            'value': '',
            'cellNumber': 'GS2'
        },
        'communication_data2_address_country': {
            'value': '',
            'cellNumber': 'GT2'
        },
        'communication_data2_address_region': {
            'value': '',
            'cellNumber': 'GU2'
        },
        'communication_data2_address_municipality': {
            'value': '',
            'cellNumber': 'GV2'
        },
        'communication_data2_address_populated_place': {
            'value': '',
            'cellNumber': 'GW2'
        },
        'communication_data2_address_zip_code': {
            'value': '',
            'cellNumber': 'GX2'
        },
        'communication_data2_address_district': {
            'value': '',
            'cellNumber': 'GY2'
        },
        'communication_data2_address_residential_area': {
            'value': '',
            'cellNumber': 'GZ2'
        },
        'communication_data2_address_street': {
            'value': '',
            'cellNumber': 'HA2'
        },
        'communication_data2_address_street_number': {
            'value': '',
            'cellNumber': 'HB2'
        },
        'communication_data2_address_additional_info': {
            'value': '',
            'cellNumber': 'HC2'
        },
        'communication_data2_address_block': {
            'value': '',
            'cellNumber': 'HD2'
        },
        'communication_data2_address_entrance': {
            'value': '',
            'cellNumber': 'HE2'
        },
        'communication_data2_address_floor': {
            'value': '',
            'cellNumber': 'HF2'
        },
        'communication_data2_address_apartment': {
            'value': '',
            'cellNumber': 'HG2'
        },
        'communication_data2_address_mailbox': {
            'value': '',
            'cellNumber': 'HH2'
        },
        'communication_data2_address_region_foreign': {
            'value': '',
            'cellNumber': 'HI2'
        },
        'communication_data2_address_municipality_foreign': {
            'value': '',
            'cellNumber': 'HJ2'
        },
        'communication_data2_address_populated_place_foreign': {
            'value': '',
            'cellNumber': 'HK2'
        },
        'communication_data2_address_zip_code_foreign': {
            'value': '',
            'cellNumber': 'HL2'
        },
        'communication_data2_address_district_foreign': {
            'value': '',
            'cellNumber': 'HM2'
        },
        'communication_data2_address_residential_area_type_foreign': {
            'value': '',
            'cellNumber': 'HN2'
        },
        'communication_data2_address_residential_area_foreign': {
            'value': '',
            'cellNumber': 'HO2'
        },
        'communication_data2_address_street_type_foreign': {
            'value': '',
            'cellNumber': 'HP2'
        },
        'communication_data2_address_street_foreign': {
            'value': '',
            'cellNumber': 'HQ2'
        },
        'communication_data2_platform1_name': {
            'value': '',
            'cellNumber': 'HR2'
        },
        'communication_data2_platform1_type': {
            'value': '',
            'cellNumber': 'HS2'
        },
        'communication_data2_platform2_name': {
            'value': '',
            'cellNumber': 'HT2'
        },
        'communication_data2_platform2_type': {
            'value': '',
            'cellNumber': 'HU2'
        },
        'communication_data2_platform3_name': {
            'value': '',
            'cellNumber': 'HV2'
        },
        'communication_data2_platform3_type': {
            'value': '',
            'cellNumber': 'HW2'
        },
        'communication_data2_mobile_number_1': {
            'value': '',
            'cellNumber': 'HX2'
        },
        'communication_data2_send_sms_1': {
            'value': '',
            'cellNumber': 'HY2'
        },
        'communication_data2_mobile_number_2': {
            'value': '',
            'cellNumber': 'HZ2'
        },
        'communication_data2_send_sms_2': {
            'value': '',
            'cellNumber': 'IA2'
        },
        'communication_data2_mobile_number_3': {
            'value': '',
            'cellNumber': 'IB2'
        },
        'communication_data2_send_sms_3': {
            'value': '',
            'cellNumber': 'IC2'
        },
        'communication_data2_landline_phone_1': {
            'value': '',
            'cellNumber': 'ID2'
        },
        'communication_data2_landline_phone_2': {
            'value': '',
            'cellNumber': 'IE2'
        },
        'communication_data2_landline_phone_3': {
            'value': '',
            'cellNumber': 'IF2'
        },
        'communication_data2_call_center_1': {
            'value': '',
            'cellNumber': 'IG2'
        },
        'communication_data2_call_center_2': {
            'value': '',
            'cellNumber': 'IH2'
        },
        'communication_data2_call_center_3': {
            'value': '',
            'cellNumber': 'II2'
        },
        'communication_data2_fax_1': {
            'value': '',
            'cellNumber': 'IJ2'
        },
        'communication_data2_fax_2': {
            'value': '',
            'cellNumber': 'IK2'
        },
        'communication_data2_fax_3': {
            'value': '',
            'cellNumber': 'IL2'
        },
        'communication_data2_email_1': {
            'value': '',
            'cellNumber': 'IM2'
        },
        'communication_data2_email_2': {
            'value': '',
            'cellNumber': 'IN2'
        },
        'communication_data2_email_3': {
            'value': '',
            'cellNumber': 'IO2'
        },
        'communication_data2_website_1': {
            'value': '',
            'cellNumber': 'IP2'
        },
        'communication_data2_website_2': {
            'value': '',
            'cellNumber': 'IQ2'
        },
        'communication_data2_website_3': {
            'value': '',
            'cellNumber': 'IR2'
        },
        'communication_data2_contact_purpose_1': {
            'value': '',
            'cellNumber': 'IS2'
        },
        'communication_data2_contact_purpose_2': {
            'value': '',
            'cellNumber': 'IT2'
        },
        'communication_data2_contact_purpose_3': {
            'value': '',
            'cellNumber': 'IU2'
        },
        'communication_data2_contact_person1_name': {
            'value': '',
            'cellNumber': 'IV2'
        },
        'communication_data2_contact_person1_middle_name': {
            'value': '',
            'cellNumber': 'IW2'
        },
        'communication_data2_contact_person1_surname': {
            'value': '',
            'cellNumber': 'IX2'
        },
        'communication_data2_contact_person1_job_position': {
            'value': '',
            'cellNumber': 'IY2'
        },
        'communication_data2_contact_person1_position_held_from': {
            'value': '',
            'cellNumber': 'IZ2'
        },
        'communication_data2_contact_person1_position_held_to': {
            'value': '',
            'cellNumber': 'JA2'
        },
        'communication_data2_contact_person1_birth_date': {
            'value': '',
            'cellNumber': 'JB2'
        },
        'communication_data2_contact_person1_additional_info': {
            'value': '',
            'cellNumber': 'JC2'
        },
        'communication_data2_contact_person1_title': {
            'value': '',
            'cellNumber': 'JD2'
        },
        'communication_data2_contact_person2_name': {
            'value': '',
            'cellNumber': 'JE2'
        },
        'communication_data2_contact_person2_middle_name': {
            'value': '',
            'cellNumber': 'JF2'
        },
        'communication_data2_contact_person2_surname': {
            'value': '',
            'cellNumber': 'JG2'
        },
        'communication_data2_contact_person2_job_position': {
            'value': '',
            'cellNumber': 'JH2'
        },
        'communication_data2_contact_person2_position_held_from': {
            'value': '',
            'cellNumber': 'JI2'
        },
        'communication_data2_contact_person2_position_held_to': {
            'value': '',
            'cellNumber': 'JJ2'
        },
        'communication_data2_contact_person2_birth_date': {
            'value': '',
            'cellNumber': 'JK2'
        },
        'communication_data2_contact_person2_additional_info': {
            'value': '',
            'cellNumber': 'JL2'
        },
        'communication_data2_contact_person2_title': {
            'value': '',
            'cellNumber': 'JM2'
        },
        'communication_data2_contact_person3_name': {
            'value': '',
            'cellNumber': 'JN2'
        },
        'communication_data2_contact_person3_middle_name': {
            'value': '',
            'cellNumber': 'JO2'
        },
        'communication_data2_contact_person3_surname': {
            'value': '',
            'cellNumber': 'JP2'
        },
        'communication_data2_contact_person3_job_position': {
            'value': '',
            'cellNumber': 'JQ2'
        },
        'communication_data2_contact_person3_position_held_from': {
            'value': '',
            'cellNumber': 'JR2'
        },
        'communication_data2_contact_person3_position_held_to': {
            'value': '',
            'cellNumber': 'JS2'
        },
        'communication_data2_contact_person3_birth_date': {
            'value': '',
            'cellNumber': 'JT2'
        },
        'communication_data2_contact_person3_additional_info': {
            'value': '',
            'cellNumber': 'JU2'
        },
        'communication_data2_contact_person3_title': {
            'value': '',
            'cellNumber': 'JV2'
        },
        'communication_data3_contact_type_name': {
            'value': '',
            'cellNumber': 'JW2'
        },
        'communication_data3_address_foreign_address': {
            'value': '',
            'cellNumber': 'JX2'
        },
        'communication_data3_address_country': {
            'value': '',
            'cellNumber': 'JY2'
        },
        'communication_data3_address_region': {
            'value': '',
            'cellNumber': 'JZ2'
        },
        'communication_data3_address_municipality': {
            'value': '',
            'cellNumber': 'KA2'
        },
        'communication_data3_address_populated_place': {
            'value': '',
            'cellNumber': 'KB2'
        },
        'communication_data3_address_zip_code': {
            'value': '',
            'cellNumber': 'KC2'
        },
        'communication_data3_address_district': {
            'value': '',
            'cellNumber': 'KD2'
        },
        'communication_data3_address_residential_area': {
            'value': '',
            'cellNumber': 'KE2'
        },
        'communication_data3_address_street': {
            'value': '',
            'cellNumber': 'KF2'
        },
        'communication_data3_address_street_number': {
            'value': '',
            'cellNumber': 'KG2'
        },
        'communication_data3_address_additional_info': {
            'value': '',
            'cellNumber': 'KH2'
        },
        'communication_data3_address_block': {
            'value': '',
            'cellNumber': 'KI2'
        },
        'communication_data3_address_entrance': {
            'value': '',
            'cellNumber': 'KJ2'
        },
        'communication_data3_address_floor': {
            'value': '',
            'cellNumber': 'KK2'
        },
        'communication_data3_address_apartment': {
            'value': '',
            'cellNumber': 'KL2'
        },
        'communication_data3_address_mailbox': {
            'value': '',
            'cellNumber': 'KM2'
        },
        'communication_data3_address_region_foreign': {
            'value': '',
            'cellNumber': 'KN2'
        },
        'communication_data3_address_municipality_foreign': {
            'value': '',
            'cellNumber': 'KO2'
        },
        'communication_data3_address_populated_place_foreign': {
            'value': '',
            'cellNumber': 'KP2'
        },
        'communication_data3_address_zip_code_foreign': {
            'value': '',
            'cellNumber': 'KQ2'
        },
        'communication_data3_address_district_foreign': {
            'value': '',
            'cellNumber': 'KR2'
        },
        'communication_data3_address_residential_area_type_foreign': {
            'value': '',
            'cellNumber': 'KS2'
        },
        'communication_data3_address_residential_area_foreign': {
            'value': '',
            'cellNumber': 'KT2'
        },
        'communication_data3_address_street_type_foreign': {
            'value': '',
            'cellNumber': 'KU2'
        },
        'communication_data3_address_street_foreign': {
            'value': '',
            'cellNumber': 'KV2'
        },
        'communication_data3_platform1_name': {
            'value': '',
            'cellNumber': 'KW2'
        },
        'communication_data3_platform1_type': {
            'value': '',
            'cellNumber': 'KX2'
        },
        'communication_data3_platform2_name': {
            'value': '',
            'cellNumber': 'KY2'
        },
        'communication_data3_platform2_type': {
            'value': '',
            'cellNumber': 'KZ2'
        },
        'communication_data3_platform3_name': {
            'value': '',
            'cellNumber': 'LA2'
        },
        'communication_data3_platform3_type': {
            'value': '',
            'cellNumber': 'LB2'
        },
        'communication_data3_mobile_number_1': {
            'value': '',
            'cellNumber': 'LC2'
        },
        'communication_data3_send_sms_1': {
            'value': '',
            'cellNumber': 'LD2'
        },
        'communication_data3_mobile_number_2': {
            'value': '',
            'cellNumber': 'LE2'
        },
        'communication_data3_send_sms_2': {
            'value': '',
            'cellNumber': 'LF2'
        },
        'communication_data3_mobile_number_3': {
            'value': '',
            'cellNumber': 'LG2'
        },
        'communication_data3_send_sms_3': {
            'value': '',
            'cellNumber': 'LH2'
        },
        'communication_data3_landline_phone_1': {
            'value': '',
            'cellNumber': 'LI2'
        },
        'communication_data3_landline_phone_2': {
            'value': '',
            'cellNumber': 'LJ2'
        },
        'communication_data3_landline_phone_3': {
            'value': '',
            'cellNumber': 'LK2'
        },
        'communication_data3_call_center_1': {
            'value': '',
            'cellNumber': 'LL2'
        },
        'communication_data3_call_center_2': {
            'value': '',
            'cellNumber': 'LM2'
        },
        'communication_data3_call_center_3': {
            'value': '',
            'cellNumber': 'LN2'
        },
        'communication_data3_fax_1': {
            'value': '',
            'cellNumber': 'LO2'
        },
        'communication_data3_fax_2': {
            'value': '',
            'cellNumber': 'LP2'
        },
        'communication_data3_fax_3': {
            'value': '',
            'cellNumber': 'LQ2'
        },
        'communication_data3_email_1': {
            'value': '',
            'cellNumber': 'LR2'
        },
        'communication_data3_email_2': {
            'value': '',
            'cellNumber': 'LS2'
        },
        'communication_data3_email_3': {
            'value': '',
            'cellNumber': 'LT2'
        },
        'communication_data3_website_1': {
            'value': '',
            'cellNumber': 'LU2'
        },
        'communication_data3_website_2': {
            'value': '',
            'cellNumber': 'LV2'
        },
        'communication_data3_website_3': {
            'value': '',
            'cellNumber': 'LW2'
        },
        'communication_data3_contact_purpose_1': {
            'value': '',
            'cellNumber': 'LX2'
        },
        'communication_data3_contact_purpose_2': {
            'value': '',
            'cellNumber': 'LY2'
        },
        'communication_data3_contact_purpose_3': {
            'value': '',
            'cellNumber': 'LZ2'
        },
        'communication_data3_contact_person1_name': {
            'value': '',
            'cellNumber': 'MA2'
        },
        'communication_data3_contact_person1_middle_name': {
            'value': '',
            'cellNumber': 'MB2'
        },
        'communication_data3_contact_person1_surname': {
            'value': '',
            'cellNumber': 'MC2'
        },
        'communication_data3_contact_person1_job_position': {
            'value': '',
            'cellNumber': 'MD2'
        },
        'communication_data3_contact_person1_position_held_from': {
            'value': '',
            'cellNumber': 'ME2'
        },
        'communication_data3_contact_person1_position_held_to': {
            'value': '',
            'cellNumber': 'MF2'
        },
        'communication_data3_contact_person1_birth_date': {
            'value': '',
            'cellNumber': 'MG2'
        },
        'communication_data3_contact_person1_additional_info': {
            'value': '',
            'cellNumber': 'MH2'
        },
        'communication_data3_contact_person1_title': {
            'value': '',
            'cellNumber': 'MI2'
        },
        'communication_data3_contact_person2_name': {
            'value': '',
            'cellNumber': 'MJ2'
        },
        'communication_data3_contact_person2_middle_name': {
            'value': '',
            'cellNumber': 'MK2'
        },
        'communication_data3_contact_person2_surname': {
            'value': '',
            'cellNumber': 'ML2'
        },
        'communication_data3_contact_person2_job_position': {
            'value': '',
            'cellNumber': 'MM2'
        },
        'communication_data3_contact_person2_position_held_from': {
            'value': '',
            'cellNumber': 'MN2'
        },
        'communication_data3_contact_person2_position_held_to': {
            'value': '',
            'cellNumber': 'MO2'
        },
        'communication_data3_contact_person2_birth_date': {
            'value': '',
            'cellNumber': 'MP2'
        },
        'communication_data3_contact_person2_additional_info': {
            'value': '',
            'cellNumber': 'MQ2'
        },
        'communication_data3_contact_person2_title': {
            'value': '',
            'cellNumber': 'MR2'
        },
        'communication_data3_contact_person3_name': {
            'value': '',
            'cellNumber': 'MS2'
        },
        'communication_data3_contact_person3_middle_name': {
            'value': '',
            'cellNumber': 'MT2'
        },
        'communication_data3_contact_person3_surname': {
            'value': '',
            'cellNumber': 'MU2'
        },
        'communication_data3_contact_person3_job_position': {
            'value': '',
            'cellNumber': 'MV2'
        },
        'communication_data3_contact_person3_position_held_from': {
            'value': '',
            'cellNumber': 'MW2'
        },
        'communication_data3_contact_person3_position_held_to': {
            'value': '',
            'cellNumber': 'MX2'
        },
        'communication_data3_contact_person3_birth_date': {
            'value': '',
            'cellNumber': 'MY2'
        },
        'communication_data3_contact_person3_additional_info': {
            'value': '',
            'cellNumber': 'MZ2'
        },
        'communication_data3_contact_person3_title': {
            'value': '',
            'cellNumber': 'NA2'
        },
        'prefer_communication_in_english': {
            'value': 'NO', // Default false (shared field in both customer types)
            'cellNumber': 'NB2'
        }
    }
};