create schema if not exists billing_run;

create table if not exists billing_run.per_piece_details
(
    id                 bigint  not null,
    contract_detail_id bigint  not null,
    run_contract_id    bigint  not null,
    price_component_id bigint  not null,
    bg_invoice_slot_id bigint  not null,
    quantity           integer not null,
    price_formula      varchar not null,
    run_total_price    numeric,
    status             varchar not null,
    billing_run_id     bigint  not null,
    product_detail_id  bigint,
    customer_detail_id bigint,
    product_id         bigint,
    customer_id        bigint,
    pc_group_id        bigint,
    service_unit_id    bigint
);

create table if not exists billing_run.bg_invoice_slots
(
    bg_invoice_slot_id            bigint       not null,
    run_id                        bigint       not null,
    contract_id                   bigint,
    contract_billing_group_id     bigint,
    billing_group_number          varchar(256),
    separate_invoice_for_each_pod boolean,
    pc_issued_separate_invoice    pc_issued_separate_invoice,
    pod_invoices_slot             bigint,
    status                        varchar(256) not null,
    error_message                 varchar,
    created_from                  varchar(255),
    contract_type                 varchar(255),
    customer_id                   bigint,
    product_id                    bigint,
    latest_contract_detail_id     bigint,
    latest_product_detail_id      bigint,
    latest_customer_detail_id     bigint,
    customer_identifier           varchar,
    customer_name                 varchar,
    payment_term_id               bigint,
    no_interest_on_overdue_debt   boolean,
    receipt_of_an_invoice_number  bigint,
    customer_communication_id     bigint,
    number_of_income_account      varchar,
    cost_center_controlling_order varchar,
    direct_debit                  boolean,
    bank                          varchar,
    bic                           varchar,
    bank_account                  varchar,
    contract_communication_id     bigint,
    interest_rate_id              bigint,
    bank_id                       bigint
);

create table if not exists billing_run.run_contracts
(
    id                      bigint  not null,
    run_id                  bigint  not null,
    contract_id             bigint  not null,
    processing_status       varchar not null,
    contract_type           varchar not null,
    created_from            varchar,
    last_contract_detail_id bigint
);

create table if not exists billing_run.over_time_one_time
(
    id                 bigint  not null,
    contract_detail_id bigint  not null,
    run_contract_id    bigint  not null,
    price_component_id bigint  not null,
    bg_invoice_slot_id bigint  not null,
    price_formula      varchar not null,
    run_total_price    numeric,
    status             varchar not null,
    billing_run_id     bigint  not null,
    pod_count          integer,
    customer_detail_id bigint,
    product_detail_id  bigint,
    customer_id        bigint,
    product_id         bigint,
    pc_group_id        bigint,
    pod_id  bigint
);

create table if not exists billing_run.over_time_periodical
(
    id                 bigint  not null,
    contract_detail_id bigint  not null,
    run_contract_id    bigint  not null,
    price_component_id bigint  not null,
    bg_invoice_slot_id bigint  not null,
    price_formula      varchar not null,
    run_total_price    numeric,
    status             varchar not null,
    billing_run_id     bigint  not null,
    pod_count          integer,
    customer_detail_id bigint,
    product_detail_id  bigint,
    customer_id        bigint,
    product_id         bigint,
    pc_group_id        bigint,
    pod_id  bigint
);

create table if not exists billing_run.over_time_with_electricity
(
    run_contract_id           bigint,
    price_component_id        bigint,
    pc_group_id               bigint,
    pod_count                 integer,
    price_formula             varchar,
    status                    varchar,
    run_id                    bigint,
    id                        bigint not null,
    bg_invoice_slot_id        bigint,
    contract_detail_id        bigint,
    customer_detail_id        bigint,
    product_detail_id         bigint,
    has_invalid_price_formula boolean,
    pod_id bigint
);

create table if not exists billing_run.run_interim_data
(
    id                               bigint  not null,
    is_valid_for_generation          boolean,
    run_id                           bigint  not null,
    run_interim_contract_id          bigint  not null,
    billing_group_id                 bigint,
    pod_id                           bigint,
    interim_id                       bigint  not null,
    interim_from_type                varchar not null,
    value_type                       varchar not null,
    valid_for_value_type             boolean,
    calculation_value                numeric,
    currency_id                      bigint,
    date_of_issue_type               varchar not null,
    valid_for_date_of_issue_type     boolean,
    date_of_issue_value              integer,
    has_valid_payment_term_value     boolean,
    payment_term_value               integer,
    payment_term_id                  bigint,
    price_component_id               bigint,
    issued_separate_invoice          varchar,
    valid_for_days_after_invoice     boolean,
    vat_rate_id                      bigint,
    prev_invoice_id                  bigint,
    status                           varchar,
    contract_id                      bigint,
    contract_detail_id               bigint,
    product_detail_id                bigint,
    contract_type                    varchar,
    error_message                    varchar,
    product_id                       bigint,
    issuing_for_the_month_to_current interim_advance_payment.iap_issuing_for_the_month_to_current,
    deduction_from                   interim_advance_payment.iap_deduction_from,
    global_vat_rate                  boolean,
    price_formula                    varchar,
    match_term_of_standard_invoice   boolean,
    customer_detail_id               bigint,
    cost_center_controlling_order    varchar,
    number_of_income_account         varchar,
    applicable_interest_rate         bigint,
    direct_debit                     boolean,
    bank_id                          bigint,
    bank_account                     varchar,
    receipt_of_an_invoice_number     bigint,
    customer_communication_id        bigint,
    contract_communication_id        bigint,
    no_interest_on_overdue_debt      boolean
);