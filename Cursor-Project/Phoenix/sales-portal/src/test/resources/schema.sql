CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;
CREATE EXTENSION IF NOT EXISTS postgis_raster;

CREATE SCHEMA IF NOT EXISTS nomenclature
    AUTHORIZATION postgres;

DROP TABLE IF EXISTS nomenclature.nomenclatures;
DROP TABLE IF EXISTS customer.unwanted_customer;
DROP TYPE IF EXISTS nomenclature_status;
create schema if not exists receivable;
create schema if not exists sysconfig;
create schema if not exists crm;
create type receivable.customer_deposit_payment_deadline_exclude as enum ('WEEKENDS', 'HOLIDAYS');
create type receivable.customer_deposit_calendar_type as enum ('WORKING_DAYS', 'CALENDAR_DAYS', 'CERTAIN_DAYS');
create type receivable.customer_deposit_payment_deadline_change_to as enum ('PREVIOUS_WORKING_DAY', 'NEXT_WORKING_DAY');
CREATE TYPE nomenclature_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'DELETED'
    );

CREATE TYPE status_enum AS ENUM (
    'ACTIVE',
    'DELETED'
    );

CREATE TYPE pc_xenergie_application AS ENUM (
    'CONSUMER',
    'GENERATOR'
    );

CREATE TYPE residential_type AS ENUM (
    'QUARTER',
    'RESIDENTIAL_AREA'
    );

CREATE TYPE street_types AS ENUM (
    'STREET',
    'BOULEVARD'
    );

CREATE TYPE system_messages_enum AS ENUM (
    'WARNING','ERROR','CONFIRMATION','INFORMATIONAL','DELETE'
    );

CREATE TYPE customer_comm_contact_types as ENUM (
    'MOBILE_NUMBER',
    'LANDLINE_PHONE',
    'CALL_CENTER',
    'FAX',
    'EMAIL',
    'WEBSITE',
    'OTHER_PLATFORM'
    );

CREATE TYPE account_period_status AS ENUM (
    'OPEN',
    'CLOSED'
    );

CREATE TABLE IF NOT EXISTS nomenclature.nomenclatures
(
    id         integer                                             NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 2147483647 CACHE 1 ),
    name       character varying(255) COLLATE pg_catalog."default" NOT NULL,
    table_name character varying(50) COLLATE pg_catalog."default"  NOT NULL,
    view_name  character varying(50) COLLATE pg_catalog."default"  NOT NULL,
    CONSTRAINT nomenclatures_pk PRIMARY KEY (id),
    CONSTRAINT nomenclatures_uk1 UNIQUE (name, table_name)
);

DROP TABLE IF EXISTS nomenclature.countries;
create sequence nomenclature.countries_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.countries
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.countries_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    name_transliterated   text                                                 NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50),
    CONSTRAINT countries_pk PRIMARY KEY (id)
);
alter sequence nomenclature.countries_id_seq owned by nomenclature.countries.id;

DROP TABLE IF EXISTS nomenclature.regions;
create sequence nomenclature.regions_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.regions
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.regions_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    name_transliterated   text                                                 NULL,
    country_id            integer                                              NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50),
    CONSTRAINT regions_pk PRIMARY KEY (id)
--     CONSTRAINT regions_fk1 FOREIGN KEY (country_id)
--         REFERENCES nomenclature.countries (id) MATCH SIMPLE
);
alter sequence nomenclature.regions_id_seq owned by nomenclature.regions.id;

DROP TABLE IF EXISTS nomenclature.municipalities;
create sequence nomenclature.municipalities_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.municipalities
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.municipalities_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    name_transliterated   text                                                 NULL,
    region_id             integer                                              NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50),
    CONSTRAINT municipalities_pk PRIMARY KEY (id)
        INCLUDE (name, region_id)
--     CONSTRAINT municipalities_fk1 FOREIGN KEY (region_id)
--         REFERENCES nomenclature.regions (id) MATCH SIMPLE
);
alter sequence nomenclature.municipalities_id_seq owned by nomenclature.municipalities.id;

DROP TABLE IF EXISTS nomenclature.populated_places;
create sequence nomenclature.populated_places_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.populated_places
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.populated_places_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    name_transliterated   text                                                 NULL,
    municipality_id       integer                                              NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50),
    CONSTRAINT populated_places_pk PRIMARY KEY (id)
        INCLUDE (name, municipality_id)
--     CONSTRAINT populated_places_fk1 FOREIGN KEY (municipality_id)
--         REFERENCES nomenclature.municipalities (id) MATCH SIMPLE
);
alter sequence nomenclature.populated_places_id_seq owned by nomenclature.populated_places.id;

DROP TABLE IF EXISTS nomenclature.districts;
create sequence nomenclature.districts_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.districts
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.districts_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    name_transliterated   text                                                 NULL,
    populated_place_id    integer                                              NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50),
    CONSTRAINT districts_pk PRIMARY KEY (id)
        INCLUDE (name, populated_place_id)
--     CONSTRAINT districts_fk1 FOREIGN KEY (populated_place_id)
--         REFERENCES nomenclature.populated_places (id) MATCH SIMPLE
);
alter sequence nomenclature.districts_id_seq owned by nomenclature.districts.id;

DROP TABLE IF EXISTS nomenclature.titles;
create sequence nomenclature.titles_id_seq increment by 1;
create table if not exists nomenclature.titles
(
    id                    integer                                            NOT NULL primary key DEFAULT nextval('nomenclature.titles_id_seq'),
    name                  varchar(2048)                                      not null,
    name_transliterated   text                                               null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.titles_id_seq owned by nomenclature.titles.id;

DROP TABLE IF EXISTS nomenclature.segments;
create sequence nomenclature.segments_id_seq increment by 1;
create table if not exists nomenclature.segments
(
    id                    integer primary key                                not null DEFAULT nextval('nomenclature.segments_id_seq'),
    name                  varchar(512)                                       not null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.segments_id_seq owned by nomenclature.segments.id;

DROP TABLE IF EXISTS nomenclature.banks;
create sequence nomenclature.banks_id_seq increment by 1;
create table if not exists nomenclature.banks
(
    id                    integer                                            not null primary key DEFAULT nextval('nomenclature.banks_id_seq'),
    name                  varchar(255)                                       not null,
    name_transliterated   text                                               null,
    bic                   varchar(255)                                       not null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.banks_id_seq owned by nomenclature.banks.id;

DROP TABLE IF EXISTS nomenclature.account_manager_types;
create sequence nomenclature.account_manager_types_id_seq increment by 1;
create table if not exists nomenclature.account_manager_types
(
    id                    integer                                            not null primary key DEFAULT nextval('nomenclature.account_manager_types_id_seq'),
    name                  varchar(2048)                                      not null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.account_manager_types_id_seq owned by nomenclature.account_manager_types.id;

DROP TABLE IF EXISTS nomenclature.belonging_capital_owners;
create sequence nomenclature.belonging_capital_owners_id_seq increment by 1;
create table if not exists nomenclature.belonging_capital_owners
(
    id                    integer                                            not null primary key DEFAULT nextval('nomenclature.belonging_capital_owners_id_seq'),
    name                  varchar(2048)                                      not null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.belonging_capital_owners_id_seq owned by nomenclature.belonging_capital_owners.id;

create type nomenclature.timezone as enum ('CET', 'EET');

create sequence nomenclature.profiles_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.profiles
(
    id                    integer primary key NOT NULL DEFAULT nextval('nomenclature.profiles_id_seq'),
    name                  varchar(512)        not null,
    is_default            boolean             not null,
    is_hard_coded         boolean             not null,
    system_user_id        varchar(50)         not null,
    status                nomenclature_status not null,
    ordering_id           integer             not null
        constraint profiles_ordering_uk
            unique
                deferrable initially deferred,
    create_date           timestamp with time zone     default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    timezone              nomenclature.timezone
);
alter sequence nomenclature.profiles_id_seq owned by nomenclature.profiles.id;

DROP TABLE IF EXISTS nomenclature.legal_forms;
create sequence nomenclature.legal_forms_id_seq increment by 1;
DROP TABLE IF EXISTS nomenclature.legal_forms_transl;
create sequence nomenclature.legal_forms_transl_id_seq increment by 1;
create table if not exists nomenclature.legal_forms
(
    id                          bigint                                             not null primary key DEFAULT nextval('nomenclature.legal_forms_id_seq'),
    name                        varchar(2048)                                      not null,
    name_transliterated         text                                               null,
    full_descr                  varchar(2048)                                      not null,
    description_transliterated  text                                               null,
    ordering_id                 integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null,
    is_hard_coded         boolean
);
alter sequence nomenclature.legal_forms_id_seq owned by nomenclature.legal_forms.id;

create table if not exists nomenclature.legal_forms_transl
(
    id                    bigint primary key                                 not null DEFAULT nextval('nomenclature.legal_forms_transl_id_seq'),
    name                  varchar(2048)                                      not null,
    full_descr            varchar(2048)                                      not null,
    legal_form_id         bigint                                             not null,
--         constraint legal_forms_transl_fk2
--             references nomenclature.legal_forms,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.legal_forms_transl_id_seq owned by nomenclature.legal_forms_transl.id;

DROP TABLE IF EXISTS nomenclature.economic_branch_ci;
create sequence nomenclature.economic_branch_ci_id_seq increment by 1;
create table if not exists nomenclature.economic_branch_ci
(
    id                    integer                                            not null primary key DEFAULT nextval('nomenclature.economic_branch_ci_id_seq'),
    name                  varchar(2048)                                      not null,
    name_transliterated   text                                               null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.economic_branch_ci_id_seq owned by nomenclature.economic_branch_ci.id;

DROP TABLE IF EXISTS nomenclature.platforms;
create sequence nomenclature.platforms_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.platforms
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.platforms_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50),
    CONSTRAINT platforms_pk PRIMARY KEY (id)
);
alter sequence nomenclature.platforms_id_seq owned by nomenclature.platforms.id;

DROP TABLE IF EXISTS nomenclature.representation_methods;
create sequence nomenclature.representation_methods_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.representation_methods
(
    id                    integer                                              NOT NULL primary key DEFAULT nextval('nomenclature.representation_methods_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    name_transliterated   text                                                 NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                                  default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);
alter sequence nomenclature.representation_methods_id_seq owned by nomenclature.representation_methods.id;

DROP TABLE IF EXISTS nomenclature.connection_types_rci;
create sequence nomenclature.connection_types_rci_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.connection_types_rci
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.connection_types_rci_id_seq') primary key,
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);
alter sequence nomenclature.connection_types_rci_id_seq owned by nomenclature.connection_types_rci.id;

DROP TABLE IF EXISTS nomenclature.connection_types_gcc;
create sequence nomenclature.connection_types_gcc_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.connection_types_gcc
(
    id                    integer                                              NOT NULL primary key DEFAULT nextval('nomenclature.connection_types_gcc_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                                  default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);
alter sequence nomenclature.connection_types_gcc_id_seq owned by nomenclature.connection_types_gcc.id;

DROP TABLE IF EXISTS nomenclature.contact_purposes;
create sequence nomenclature.contact_purposes_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.contact_purposes
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.contact_purposes_id_seq') primary key,
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    is_hard_coded         boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);
alter sequence nomenclature.contact_purposes_id_seq owned by nomenclature.contact_purposes.id;

DROP TABLE IF EXISTS nomenclature.unwanted_customers_reasons;
create sequence nomenclature.unwanted_customers_reasons_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.unwanted_customers_reasons
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.unwanted_customers_reasons_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    name_transliterated   text                                                 NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);
alter sequence nomenclature.unwanted_customers_reasons_id_seq owned by nomenclature.unwanted_customers_reasons.id;

DROP TABLE IF EXISTS nomenclature.ownership_forms;
create sequence nomenclature.ownership_forms_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.ownership_forms
(
    id                    integer primary key                                  NOT NULL DEFAULT nextval('nomenclature.ownership_forms_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    name_transliterated   text                                                 NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);
alter sequence nomenclature.ownership_forms_id_seq owned by nomenclature.ownership_forms.id;

DROP TABLE IF EXISTS nomenclature.economic_branch_ncea;
create sequence nomenclature.economic_branch_ncea_id_seq increment by 1;
create table if not exists nomenclature.economic_branch_ncea
(
    id                    bigint primary key                                 not null DEFAULT nextval('nomenclature.economic_branch_ncea_id_seq'),
    name                  varchar(2048)                                      not null,
    name_transliterated   text                                               null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.economic_branch_ncea_id_seq owned by nomenclature.economic_branch_ncea.id;

DROP TABLE IF EXISTS nomenclature.zip_codes;
create sequence nomenclature.zip_codes_id_seq increment by 1;
create table if not exists nomenclature.zip_codes
(
    id                    integer                                            not null primary key DEFAULT nextval('nomenclature.zip_codes_id_seq'),
    zip_code              varchar(32)                                        not null,
    name_transliterated   text                                               null,
    populated_place_id    integer                                            not null,
--         constraint zip_codes_fk2
--             references nomenclature.populated_places,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.zip_codes_id_seq owned by nomenclature.zip_codes.id;

DROP TABLE IF EXISTS nomenclature.residential_areas;
create sequence if not exists nomenclature.residential_areas_id_seq increment by 1;
create table if not exists nomenclature.residential_areas
(
    id                    bigint primary key                                 not null DEFAULT nextval('nomenclature.residential_areas_id_seq'),
    name                  varchar(2048)                                      not null,
    name_transliterated   text                                               null,
    populated_place_id    integer                                            not null,
--         constraint residential_areas_fk1
--             references nomenclature.populated_places,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null,
    type                  residential_type                                   not null,
    ordering_id           integer                                            not null
        constraint residential_areas_uk1
            unique
                deferrable initially deferred
);
alter sequence nomenclature.residential_areas_id_seq owned by nomenclature.residential_areas.id;

DROP TABLE IF EXISTS nomenclature.streets;
create sequence nomenclature.streets_id_seq increment by 1;
create table if not exists nomenclature.streets
(
    id                    bigint primary key                                 not null DEFAULT nextval('nomenclature.streets_id_seq'),
    name                  varchar(2048)                                      not null,
    name_transliterated   text                                               null,
    populated_place_id    integer                                            not null,
--         constraint streets_fk1
--             references nomenclature.populated_places,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null,
    type                  street_types                                       not null,
    ordering_id           integer                                            not null
        constraint streets_uk1
            unique
                deferrable initially deferred
);
alter sequence nomenclature.streets_id_seq owned by nomenclature.streets.id;

DROP TABLE IF EXISTS nomenclature.credit_ratings;
create sequence nomenclature.credit_ratings_id_seq increment by 1;
create table if not exists nomenclature.credit_ratings
(
    id                    bigint primary key                                 not null DEFAULT nextval('nomenclature.credit_ratings_id_seq'),
    name                  varchar(2048)                                      not null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.credit_ratings_id_seq owned by nomenclature.credit_ratings.id;

DROP TABLE IF EXISTS nomenclature.preferences;
create sequence nomenclature.preferences_id_seq increment by 1;
create table if not exists nomenclature.preferences
(
    id                    bigint                                             not null primary key DEFAULT nextval('nomenclature.preferences_id_seq'),
    name                  varchar(2048)                                      not null,
    ordering_id           float4                                             not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.preferences_id_seq owned by nomenclature.preferences.id;
DROP TABLE IF EXISTS nomenclature.system_messages;
create sequence if not exists nomenclature.system_messages_id_seq increment by 1;
CREATE TABLE if not exists nomenclature.system_messages
(
    id                            bigint                                             not null primary key,
    title                         varchar(256)                                       not null,
    message_text                  varchar(2048)                                      not null,
    ok_button_text                varchar(32),
    no_button_text                varchar(32),
    cancel_button_text            varchar(32),
    system_user_id                varchar(50)                                        not null,
    create_date                   timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date                   timestamp with time zone,
    modify_system_user_id         varchar(50),
    message_type                  system_messages_enum                               not null,
    name                          varchar(2048)                                      not null,
    key_name                      varchar(2048)                                      not null
        constraint system_messages_key_name_uk
            unique,
    ok_enabled                    boolean,
    no_enabled                    boolean,
    cancel_enabled                boolean,
    title_translated              varchar(256)                                       not null,
    message_text_translated       varchar(2048)                                      not null,
    ok_button_translated_text     varchar(32),
    no_button_translated_text     varchar(32),
    cancel_button_translated_text varchar(32)
);
create schema customer;
create type customer_type_enum as enum ('LEGAL_ENTITY', 'PRIVATE_CUSTOMER', 'PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY');
create type customer.customer_type_enum as enum ('LEGAL_ENTITY', 'PRIVATE_CUSTOMER');
create type customer.customer_status_enum as enum ('ACTIVE', 'DELETED');
create type customer_delete_status_enum as enum ('POTENTIAL', 'NEW', 'ACTIVE', 'LOST','ENDED');
create type customer.system_source as enum ('SELF_SERVICE_PORTAL', 'PHOENIX', 'SALES_PORTAL', 'VCOK');

create sequence customer.customers_id_seq increment by 1;
create sequence customer.customer_owners_id_seq increment by 1;
create table if not exists customer.customers
(
    id                      integer generated by default as identity
        constraint customers_pk
            primary key,
    customer_number         bigint
        constraint customers_customer_number_uk
            unique,
    identifier              varchar(256)                                       not null,
    create_date             timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id          varchar(50)                                        not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50),
    last_customer_detail_id bigint,
    status                  customer.customer_status_enum                      not null,
    customer_type           customer.customer_type_enum                        not null,
    is_hard_coded           boolean,
    additional_info         varchar(4096),
    birth_date              date,
    kyc_passed              boolean,
    kyc_expiration_date     date,
    system_source_id        customer.system_source default 'PHOENIX'::customer.system_source
);

create type customer.customer_detail_status_enum as enum ('POTENTIAL', 'NEW', 'ACTIVE', 'LOST', 'ENDED');

create table if not exists customer.customer_details
(
    id                              bigint generated by default as identity
        constraint customer_details_pk
            primary key,
    old_customer_numbers            varchar(2048),
    vat_number                      varchar(15),
    name                            varchar(255)                                       not null,
    name_transl                     varchar(255)                                       not null,
    legal_form_id                   integer,
--         constraint customer_details_fk3
--             references nomenclature.legal_forms,
    legal_form_transl_id            bigint,
--         constraint customer_details_fk4
--             references nomenclature.legal_forms_transl,
    ownership_form_id               bigint,
--         constraint customer_details_fk5
--             references nomenclature.ownership_forms,
    economic_branch_ci_id           bigint,
--         constraint customer_details_fk6
--             references nomenclature.economic_branch_ci,
    economic_branch_ncea_id         bigint,
--         constraint customer_details_fk7
--             references nomenclature.economic_branch_ncea,
    main_activity_subject           varchar(2048),
    customer_declared_consumption   varchar(11),
    credit_rating_id                integer,
--         constraint customer_details_fk8
--             references nomenclature.credit_ratings,
    bank_id                         integer,
--         constraint customer_details_fk9
--             references nomenclature.banks,
    iban                            varchar(22),
    zip_code_id                     integer,
--         constraint customer_details_fk10
--             references nomenclature.zip_codes,
    street_number                   varchar(32),
    address_additional_info         varchar(512),
    block                           varchar(128),
    entrance                        varchar(32),
    floor                           varchar(16),
    apartment                       varchar(32),
    mailbox                         varchar(32),
    street_id                       integer,
--         constraint customer_details_fk11
--             references nomenclature.streets,
    residential_area_id             integer,
--         constraint customer_details_fk12
--             references nomenclature.residential_areas,
    district_id                     integer,
--         constraint customer_details_fk13
--             references nomenclature.districts,
    region_foreign                  varchar(512),
    municipality_foreign            varchar(512),
    populated_place_foreign         varchar(512),
    zip_code_foreign                varchar(32),
    district_foreign                varchar(512),
    customer_id                     bigint                                             not null,
--         constraint customer_details_fk2
--             references customer.customers,
    version_id                      integer                                            not null,
    public_procurement_law          boolean                  default false,
    marketing_comm_consent          boolean,
    foreign_entity_person           boolean                                            not null,
    direct_debit                    boolean,
    foreign_address                 boolean                                            not null,
    prefer_communication_in_english boolean,
    populated_place_id              integer,
--         constraint customer_details_fk14
--             references nomenclature.populated_places,
    system_user_id                  character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date                     timestamp with time zone default CURRENT_TIMESTAMP not null,
    country_id                      integer,
--         constraint customer_details_fk15
--             references nomenclature.countries,
    status                          customer.customer_detail_status_enum               not null,
    modify_date                     timestamp with time zone,
    modify_system_user_id           varchar,
    middle_name                     varchar(512),
    middle_name_transl              varchar(512),
    last_name                       varchar(512),
    last_name_transl                varchar(512),
    business_activity_name          varchar(2048),
    business_activity_name_transl   varchar(2048),
    business_activity               boolean,
    gdpr_regulation_consent         boolean,
    residential_area_foreign        varchar(2048),
    street_foreign                  varchar(2048),
    foreign_street_type             street_types,
    foreign_residential_area_type   residential_type,
    street_type                     street_types,
    residential_area_type           residential_type,
    additional_info                 varchar(4096),
    search_column                   text,
    additional_info_del             text,
    -- Transliterated address fields
    country_transl                  varchar(512),
    region_transl                   varchar(512),
    municipality_transl             varchar(512),
    populated_place_transl          varchar(512),
    zip_code_transl                 varchar(32),
    district_transl                 varchar(512),
    residential_area_transl         varchar(1024),
    street_transl                   varchar(1024),
    street_number_transl            varchar(512),
    address_additional_info_transl  varchar(512),
    block_transl                    varchar(128),
    entrance_transl                 varchar(32),
    floor_transl                    varchar(16),
    apartment_transl                varchar(32),
    mailbox_transl                  varchar(32),
    constraint customer_details_uk1
        unique (customer_id, version_id)
);

create table if not exists customer.customer_owners
(
    id                         bigint generated by default as identity
        constraint customer_owners_pk
            primary key,
    customer_id                bigint                                             not null,
--         constraint customer_owners_fk1
--             references customer.customers,
    owner_customer_id          bigint                                             not null,
--         constraint customer_owners_fk2
--             references customer.customers,
    belonging_capital_owner_id integer                                            not null,
--         constraint customer_owners_fk3
--             references nomenclature.belonging_capital_owners,
    additional_info            varchar(2048),
    status                     status_enum                                        not null,
    system_user_id             character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date                timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date                timestamptz                                        NULL,
    modify_system_user_id      varchar(50)
);

create sequence customer.unwanted_customers_id_seq increment by 1;
create table if not exists customer.unwanted_customers
(
    id                           bigint                                             not null primary key,
    identifier                   varchar(13)                                        not null,
    name                         varchar(2048),
    unwanted_customers_reason_id integer,
    additional_info              varchar(2048),
    create_contract_restrict     boolean,
    create_order_restrict        boolean,
    system_user_id               character varying(50) COLLATE pg_catalog."default" NOT NULL,
    status                       status_enum,
    create_date                  timestamp with time zone,
    modify_date                  timestamp with time zone,
    modify_system_user_id        varchar(50)
);

DROP TABLE IF EXISTS customer.customer_managers;
create sequence customer.customer_managers_id_seq increment by 1;
create table if not exists customer.customer_managers
(
    id                       bigint                                             not null default nextval('customer.customer_managers_id_seq'),
    name                     varchar(512)                                       not null,
    middle_name              varchar(512),
    surname                  varchar(512)                                       not null,
    personal_number          varchar(12),
    job_position             varchar(512)                                       not null,
    position_held_from       date,
    position_held_to         date,
    birth_date               date,
    representation_method_id integer                                            not null,
--         constraint customer_manager_fk2
--             references nomenclature.representation_methods,
    title_id                 integer                                            not null,
--         constraint customer_manager_fk1
--             references nomenclature.titles,
    additional_info          varchar(2048),
    status                   status_enum                                        not null,
    system_user_id           character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date              timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50),
    customer_detail_id       bigint                                             not null,
    search_column            text,
--         constraint customer_manager_fk3
--             references customer.customer_details,
    unrecognized_identifier  boolean                  default false             not null,
    kyc_passed               boolean,
    kyc_expiration_date      date,
    constraint customer_managers_chk1
        check (position_held_from <= position_held_to)
);
alter sequence customer.customer_managers_id_seq owned by customer.customer_managers.id;

DROP TABLE IF EXISTS customer.customer_communications;
create sequence customer.customer_communications_id_seq increment by 1;
create table if not exists customer.customer_communications
(
    id                            bigint                                             not null default nextval('customer.customer_communications_id_seq') primary key,
    contact_type_name             varchar(512)                                       not null,
    country_id                    integer,
--         constraint customer_communications_fk1
--             references nomenclature.countries,
    populated_place_id            integer,
--         constraint customer_communications_fk4
--             references nomenclature.populated_places,
    street_id                     integer,
--         constraint customer_communications_fk5
--             references nomenclature.streets,
    residential_area_id           integer,
--         constraint customer_communications_fk6
--             references nomenclature.residential_areas,
    district_id                   integer,
--         constraint customer_communications_fk7
--             references nomenclature.districts,
    zip_code_id                   integer,
--         constraint customer_communications_fk8
--             references nomenclature.zip_codes,
    region_foreign                varchar(512),
    municipality_foreign          varchar(512),
    populated_place_foreign       varchar(512),
    zip_code_foreign              varchar(32),
    district_foreign              varchar(512),
    street_number                 varchar(32),
    block                         varchar(128),
    entrance                      varchar(32),
    floor                         varchar(16),
    apartment                     varchar(32),
    mailbox                       varchar(32),
    address_additional_info       varchar(512),
    customer_detail_id            bigint                                             not null,
--         constraint customer_communications_fk9
--             references customer.customer_details,
    status                        status_enum                                        not null,
    system_user_id                character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date                   timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date                   timestamp with time zone,
    modify_system_user_id         varchar(50),
    street_foreign                varchar(2048),
    residential_area_foreign      varchar(2048),
    foreign_street_type           street_types,
    foreign_residential_area_type residential_type,
    street_type                   street_types,
    residential_area_type         residential_type,
    foreign_address               boolean                                            not null,
    -- Transliterated fields
    country_transl                varchar(512),
    region_transl                 varchar(512),
    municipality_transl           varchar(512),
    populated_place_transl        varchar(512),
    zip_code_transl               varchar(32),
    district_transl               varchar(512),
    residential_area_transl       varchar(1024),
    street_transl                 varchar(1024),
    street_number_transl          varchar(32),
    address_additional_info_transl varchar(512),
    block_transl                  varchar(128),
    entrance_transl               varchar(32),
    floor_transl                  varchar(16),
    apartment_transl              varchar(32),
    mailbox_transl                varchar(32),
    location                      geography(Point,4326),
    system_source_id              customer.system_source                                  default 'PHOENIX'::customer.system_source
);
alter sequence customer.customer_communications_id_seq owned by customer.customer_communications.id;

DROP TABLE IF EXISTS customer.customer_comm_contact_persons;
create sequence customer.customer_comm_contact_persons_id_seq increment by 1;
create table if not exists customer.customer_comm_contact_persons
(
    id                        bigint                                             not null default nextval('customer.customer_comm_contact_persons_id_seq'),
    name                      varchar(512)                                       not null,
    middle_name               varchar(512),
    surname                   varchar(512)                                       not null,
    job_position              varchar(512),
    position_held_from        date,
    position_held_to          date,
    birth_date                date,
    additional_info           varchar(2048),
    title_id                  integer                                            not null,
--         constraint customer_comm_contact_pers_fk1
--             references nomenclature.titles,
    status                    status_enum                                        not null,
    system_user_id            character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date               timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date               timestamp with time zone,
    modify_system_user_id     varchar(50),
    customer_communication_id bigint
--         constraint customer_comm_contact_pers_fk2
--             references customer.customer_communications
);
alter sequence customer.customer_comm_contact_persons_id_seq owned by customer.customer_comm_contact_persons.id;

DROP TABLE IF EXISTS customer.customer_comm_contact_purposes;
create sequence customer.customer_comm_contact_purposes_id_seq increment by 1;
create table if not exists customer.customer_comm_contact_purposes
(
    id                        bigint                                             not null default nextval('customer.customer_comm_contact_purposes_id_seq') primary key,
    customer_communication_id bigint                                             not null,
--         constraint customer_comm_contact_purposes_fk1
--             references customer.customer_communications,
    contact_purpose_id        integer                                            not null,
    status                    status_enum                                        not null,
    system_user_id            character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date               timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date               timestamp with time zone,
    modify_system_user_id     varchar(50)
);
alter sequence customer.customer_comm_contact_purposes_id_seq owned by customer.customer_comm_contact_purposes.id;

DROP TABLE IF EXISTS customer.customer_communication_contacts;
create sequence customer.customer_communication_contacts_id_seq increment by 1;
create table if not exists customer.customer_communication_contacts
(
    id                        bigint                                             not null default nextval('customer.customer_communication_contacts_id_seq'),
    send_sms                  boolean                  default false             not null,
    platform_id               integer,
--         constraint customer_communication_contacts_fk1
--             references nomenclature.platforms,
    system_user_id            character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date               timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date               timestamp with time zone,
    modify_system_user_id     varchar(50),
    status                    status_enum                                        not null,
    contact_type              customer_comm_contact_types                        not null,
    customer_communication_id bigint,
--         constraint customer_communication_contacts_fk2
--             references customer.customer_communications,
    contact_value             varchar(512)
);
alter sequence customer.customer_communication_contacts_id_seq owned by customer.customer_communication_contacts.id;

DROP TABLE IF EXISTS customer.related_customers;
create sequence customer.related_customers_id_seq increment by 1;
create table if not exists customer.related_customers
(
    id                    bigint                                             not null default nextval('customer.related_customers_id_seq')
        primary key,
    customer_id           bigint                                             not null,
--         constraint related_customers_fk1
--             references customer.customers,
    related_customer_id   bigint                                             not null,
--         constraint related_customers_fk2
--             references customer.customers,
    status                status_enum                                        not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    connection_types_rci  integer                                            not null
--         constraint related_customers_fk3
--             references nomenclature.connection_types_rci
);
alter sequence customer.related_customers_id_seq owned by customer.related_customers.id;
create sequence customer.customer_segments_id_seq;
create table if not exists customer.customer_segments
(
    id                    bigint                                             not null primary key default nextval('customer.customer_segments_id_seq'),
    customer_detail_id    integer                                            not null,
    segment_id            integer                                            not null,
--         constraint customer_segments_fk1
--             references nomenclature.segments,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    status                customer.customer_status_enum                      not null,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence customer.customer_segments_id_seq owned by customer.customer_segments.id;

DROP TABLE IF EXISTS customer.customer_account_managers;
create sequence customer.customer_account_managers_id_seq increment by 1;
create table if not exists customer.customer_account_managers
(
    id                      bigint      not null primary key default nextval('customer.customer_account_managers_id_seq'),
    customer_detail_id      bigint      not null,
--         constraint customer_account_managers_fk2
--             references customer.customer_details,
    account_manager_id      bigint      not null,
    account_manager_type_id integer     not null,
--         constraint customer_account_managers_fk1
--             references nomenclature.account_manager_types,
    status                  status_enum not null,
    system_user_id          varchar(10) not null,
    create_date             timestamp                        default CURRENT_TIMESTAMP not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(10),
    name_temp               varchar(1000),
    middle_name_temp        varchar(1000),
    surname_temp            varchar(1000)
);
alter sequence customer.customer_account_managers_id_seq owned by customer.customer_account_managers.id;

DROP TABLE IF EXISTS customer.connected_groups;
create sequence customer.connected_groups_id_seq increment by 1;
create table if not exists customer.connected_groups
(
    id                      bigint                                             not null default nextval('customer.connected_groups_id_seq') primary key,
    name                    varchar(2048)                                      not null,
    connection_types_gcc_id integer                                            not null,
--         constraint connected_groups_fk1
--             references nomenclature.connection_types_gcc,
    additional_info         varchar(2048),
    status                  status_enum                                        not null,
    system_user_id          character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date             timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50)
);
alter sequence customer.connected_groups_id_seq owned by customer.connected_groups.id;

DROP TABLE IF EXISTS customer.customer_preferences;
create sequence customer.customer_preferences_id_seq increment by 1;
create table if not exists customer.customer_preferences
(
    id                    bigint                                             not null primary key default nextval('customer.customer_preferences_id_seq'),
    customer_detail_id    bigint                                             not null,
--         constraint customer_preferences_fk1
--             references customer.customer_details,
    preferences_id        integer                                            not null,
--         constraint customer_preferences_fk2
--             references nomenclature.preferences,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    status                status_enum                                        not null,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    constraint customer_preferences_uk1
        unique (customer_detail_id, preferences_id)
);
alter sequence customer.customer_preferences_id_seq owned by customer.customer_preferences.id;

DROP TABLE IF EXISTS customer.customer_connected_groups;
create sequence customer.customer_connected_groups_id_seq increment by 1;
create table customer.customer_connected_groups
(
    id                    bigint                                             not null primary key default nextval('customer.customer_connected_groups_id_seq'),
    customer_id           bigint                                             not null,
--         constraint customer_connencted_groups_fk1
--             references customer.customers,
    status                status_enum                                        not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    connected_group_id    bigint                                             not null,
--         constraint customer_connencted_groups_fk2
--             references customer.connected_groups,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence customer.customer_connected_groups_id_seq owned by customer.customer_connected_groups.id;

create schema process_management;

CREATE TYPE process_management.process_status AS ENUM (
    'IN_PROGRESS',
    'PAUSED',
    'NOT_STARTED',
    'AWAITING',
    'COMPLETED',
    'CANCELED'
    );
CREATE TYPE process_management.process_type AS ENUM (
    'PROCESS_CUSTOMER_MASS_IMPORT'
    );

DROP TABLE IF EXISTS process_management.process;
create sequence process_management.process_id_seq increment by 1;
create table if not exists process_management.process
(
    id                               bigint                                             not null primary key default nextval('process_management.process_id_seq'),
    create_date                      timestamp                                          not null,
    modify_date                      timestamp,
    modify_system_user_id            varchar(50),
    system_user_id                   character varying(50) COLLATE pg_catalog."default" NOT NULL,
    file_url                         varchar(255),
    name                             varchar(510),
    process_complete_date            timestamp,
    process_start_date               timestamp,
    status                           varchar(255),
    user_permissions                 varchar(1024),
    type                             varchar(255),
    date_field                       date,
    collection_channel_id            bigint,
    payment_package_id               bigint,
    reminder_id                      bigint,
    currency_from_collection_channel boolean
);
alter sequence process_management.process_id_seq owned by process_management.process.id;

DROP TABLE IF EXISTS process_management.processed_record_info;
create sequence process_management.processed_record_id_seq increment by 1;
create table if not exists process_management.processed_record_info
(
    id                        bigint    not null primary key default nextval('process_management.processed_record_id_seq'),
    create_date               timestamp not null,
    error_message             varchar(255),
    process_id                bigint,
    record_id                 bigint,
    record_identifier         varchar(20),
    record_identifier_version varchar(50),
    success                   boolean   not null
);
alter sequence process_management.processed_record_id_seq owned by process_management.processed_record_info.id;

create table if not exists process_management.template
(
    template_name varchar(255),
    file_url      varchar(510)
);

alter table process_management.template
    add constraint template_pk
        primary key (template_name);

create sequence if not exists customer.account_managers_id_seq increment by 1;

create table if not exists customer.account_managers
(
    id                    bigint                                             not null primary key default nextval('customer.account_managers_id_seq'),
    user_name             varchar(2048)                                      not null
        constraint account_managers_uk1
            unique,
    first_name            varchar(2048)                                      not null,
    last_name             varchar(2048)                                      not null,
    display_name          varchar(2048)                                      not null,
    email                 varchar(2048)                                      not null,
    organizational_unit   varchar(2048)                                      not null,
    status                customer.customer_status_enum                      not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    business_unit         varchar(2048),
    search_column         text
);

create table if not exists customer.portal_tags
(
    id                    bigint generated by default as identity
        constraint portal_tags_pk
            primary key,
    portal_id             varchar(2048)                                      not null
        constraint portal_tags_uk
            unique,
    description           varchar(2048)                                      not null,
    name                  varchar(2048)                                      not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    system_user_id        varchar(50)                                        not null,
    modify_system_user_id varchar(50),
    description_bg        varchar(2048)                                      not null,
    name_bg               varchar(2048)                                      not null,
    status                customer.customer_status_enum
);
create table if not exists customer.account_manager_tags
(
    id                    bigint generated by default as identity
        constraint account_manager_tags_pk
            primary key,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    system_user_id        varchar(50)                                        not null,
    modify_system_user_id varchar(50),
    account_manager_id    bigint,
    portal_tag_id         bigint
);
--create type customer.customer_status_enum as enum ('ACTIVE', 'DELETED');

DROP TABLE IF EXISTS customer.customer_account_managers;
create sequence customer.customer_account_managers_id_seq increment by 1;

DROP TABLE if exists customer.customer_account_managers;
create table if not exists customer.customer_account_managers
(
    id                      bigint                                             not null primary key default nextval('customer.customer_account_managers_id_seq'),

    customer_detail_id      bigint                                             not null,
--         constraint customer_account_managers_fk2
--             references customer.customer_details,
    account_manager_type_id integer                                            not null,
--         constraint customer_account_managers_fk1
--             references nomenclature.account_manager_types,
    status                  status_enum                                        not null,
    system_user_id          character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date             timestamp with time zone                                                default CURRENT_TIMESTAMP not null,
    modify_date             timestamptz                                        NULL,
    modify_system_user_id   varchar(50),
    account_manager_id      bigint
--         constraint customer_account_managers_fk3
--             references customer.account_managers
);
alter sequence customer.customer_account_managers_id_seq owned by customer.customer_account_managers.id;

create view customer.vw_customer_account_managers(customer_detail_id, display_name, display_name_desc) as
SELECT cam.customer_detail_id,
       string_agg(am.display_name::text, ','::text)
       OVER (PARTITION BY cam.customer_detail_id ORDER BY am.display_name)      AS display_name,
       string_agg(am.display_name::text, ','::text)
       OVER (PARTITION BY cam.customer_detail_id ORDER BY am.display_name DESC) AS display_name_desc
FROM customer.customer_account_managers cam,
     customer.account_managers am,
     customer.customer_details cd
WHERE cam.customer_detail_id = cd.id
  AND cam.account_manager_id = am.id;

drop sequence if exists nomenclature.goods_suppliers_id_seq;
create sequence if not exists nomenclature.goods_suppliers_id_seq increment by 1;

DROP TABLE if exists nomenclature.goods_suppliers;
create table if not exists nomenclature.goods_suppliers
(
    id                    bigint                   not null primary key default nextval('nomenclature.goods_suppliers_id_seq'),
    name                  varchar(512)             not null,
    identifier            varchar(512),
    is_default            boolean                  not null,
    create_date           timestamp with time zone not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status      not null,
    ordering_id           integer                  not null
        constraint goods_suppliers_uk1
            unique
                deferrable initially deferred
);

alter sequence nomenclature.goods_suppliers_id_seq owned by nomenclature.goods_suppliers.id;

drop sequence if exists nomenclature.goods_groups_id_seq;
create sequence if not exists nomenclature.goods_groups_id_seq;

create table if not exists nomenclature.goods_groups
(
    id                    bigint                   not null primary key default nextval('nomenclature.goods_groups_id_seq'),
    name                  varchar(512)             not null,
    name_transl           varchar(512)             not null,
    is_default            boolean                  not null,
    create_date           timestamp with time zone not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status      not null,
    ordering_id           integer                  not null
        constraint goods_groups_uk1
            unique
                deferrable initially deferred
);

alter sequence nomenclature.goods_groups_id_seq owned by nomenclature.goods_groups.id;

DROP TABLE IF EXISTS nomenclature.sales_areas;
create sequence nomenclature.sales_areas_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.sales_areas
(
    id                    integer                  NOT NULL primary key DEFAULT nextval('nomenclature.sales_areas_id_seq'),
    name                  varchar(512)             not null,
    login_portal_tag      varchar(512),
    is_default            boolean                  not null,
    create_date           timestamp with time zone not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status      not null,
    ordering_id           integer                  not null
        constraint sales_areas_uk1
            unique
                deferrable initially deferred
);
alter sequence nomenclature.sales_areas_id_seq owned by nomenclature.sales_areas.id;

DROP TABLE IF EXISTS nomenclature.sales_channels;
create sequence nomenclature.sales_channels_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.sales_channels
(
    id                     integer                  NOT NULL primary key DEFAULT nextval('nomenclature.sales_channels_id_seq'),
    name                   varchar(512)             not null,
    portal_tag_id          bigint,
    off_premises_contracts boolean,
    is_default             boolean                  not null,
    create_date            timestamp with time zone not null,
    system_user_id         varchar(50)              not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50),
    is_hard_coded boolean,
    status                 nomenclature_status      not null,
    ordering_id            integer                  not null
        constraint sales_channels_uk1
            unique
                deferrable initially deferred
);
alter sequence nomenclature.sales_channels_id_seq owned by nomenclature.sales_channels.id;

DROP TABLE IF EXISTS nomenclature.currencies;
create sequence nomenclature.currencies_id_seq increment by 1;
create table if not exists nomenclature.currencies
(
    id                    integer                  NOT NULL primary key DEFAULT nextval('nomenclature.currencies_id_seq'),
    name                  varchar(512)             not null,
    print_name            varchar(512)             not null,
    abbreviation          varchar(128)             not null,
    full_name             varchar(512)             not null,
    alt_currency_id       integer,
--         constraint currencies_fk1 references nomenclature.currencies,
    alt_ccy_exchange_rate numeric,
    main_ccy_start_date   date,
    main_ccy              boolean                  not null,
    is_default            boolean                  not null,
    create_date           timestamp with time zone not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status      not null,
    ordering_id           integer                  not null
        constraint currencies_uk1
            unique
                deferrable initially deferred
);
alter sequence nomenclature.currencies_id_seq owned by nomenclature.currencies.id;

DROP TABLE IF EXISTS nomenclature.vat_rates;
create sequence nomenclature.vat_rates_id_seq increment by 1;
create table if not exists nomenclature.vat_rates
(
    id                    integer                  NOT NULL primary key DEFAULT nextval('nomenclature.vat_rates_id_seq'),
    name                  varchar(512)             not null,
    value_in_percent      numeric                  not null,
    start_date            date,
    global_vat_rate       boolean                  not null,
    create_date           timestamp with time zone not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status      not null,
    ordering_id           integer                  not null
        constraint vat_rates_uk1
            unique
                deferrable initially deferred
);
alter sequence nomenclature.vat_rates_id_seq owned by nomenclature.vat_rates.id;


DROP TABLE IF EXISTS nomenclature.goods_units;
create sequence nomenclature.goods_units_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.goods_units
(
    id                    integer                                              NOT NULL primary key DEFAULT nextval('nomenclature.goods_units_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                                  default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);
DROP TABLE IF EXISTS nomenclature.product_types;
create sequence nomenclature.product_types_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.product_types
(
    id                    integer                                              NOT NULL primary key DEFAULT nextval('nomenclature.product_types_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                                  default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);
DROP TABLE IF EXISTS nomenclature.price_component_price_types;
create sequence nomenclature.price_component_price_types_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.price_component_price_types
(
    id                    integer                                              NOT NULL primary key DEFAULT nextval('nomenclature.price_component_price_types'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                                  default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50),
    is_hardcoded          boolean                                                                   default false
);
alter sequence nomenclature.price_component_price_types_id_seq owned by nomenclature.price_component_price_types.id;

DROP TABLE IF EXISTS nomenclature.service_units;
create sequence nomenclature.service_units_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.service_units
(
    id                    integer                  NOT NULL primary key DEFAULT nextval('nomenclature.service_units_id_seq'),
    name                  varchar(512)             not null,
    is_default            boolean                  not null,
    create_date           timestamp with time zone not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status      not null,
    ordering_id           integer                  not null
        constraint service_units_uk1
            unique
                deferrable initially deferred
);
alter sequence nomenclature.service_units_id_seq owned by nomenclature.service_units.id;

create sequence nomenclature.service_types_id_seq increment by 1;
create table nomenclature.service_types
(
    id                    integer                                              NOT NULL primary key DEFAULT nextval('nomenclature.service_types_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                                  default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);

create sequence nomenclature.electricity_price_types_id_seq increment by 1;
create table nomenclature.electricity_price_types
(
    id                    integer                                              NOT NULL primary key DEFAULT nextval('nomenclature.electricity_price_types_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                                  default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);

drop sequence if exists nomenclature.product_groups_id_seq;
drop table if exists nomenclature.product_groups;
create sequence if not exists nomenclature.product_groups_id_seq increment by 1;
create table if not exists nomenclature.product_groups
(
    id                    integer                  NOT NULL primary key DEFAULT nextval('nomenclature.product_groups_id_seq'),
    name                  varchar(512)             not null,
    is_default            boolean                  not null,
    create_date           timestamp with time zone not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status      not null,
    ordering_id           integer                  not null,
    name_transl           varchar(512)             not null
);
alter sequence nomenclature.product_groups_id_seq owned by nomenclature.product_groups.id;

drop sequence if exists nomenclature.service_groups_id_seq;
drop table if exists nomenclature.service_groups;
create sequence if not exists nomenclature.service_groups_id_seq increment by 1;
create table if not exists nomenclature.service_groups
(
    id                    integer                  NOT NULL primary key DEFAULT nextval('nomenclature.service_groups_id_seq'),
    name                  varchar(512)             not null,
    is_default            boolean                  not null,
    create_date           timestamp with time zone not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status      not null,
    ordering_id           integer                  not null,
    name_transl           varchar(512)             not null
);
alter sequence nomenclature.service_groups_id_seq owned by nomenclature.service_groups.id;

create sequence nomenclature.price_component_value_types_id_seq increment by 1;
create table nomenclature.price_component_value_types
(
    id                    integer                                              NOT NULL primary key DEFAULT nextval('nomenclature.price_component_value_types_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    name_transliterated   text                                                 NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                                  default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);

-- GOODS schema

CREATE SCHEMA goods;

CREATE TYPE goods_status AS ENUM (
    'ACTIVE',
    'DELETED'
    );

CREATE TYPE goods_subobject_status AS ENUM (
    'ACTIVE',
    'DELETED'
    );

CREATE TYPE goods_details_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'DELETED'
    );

DROP TABLE IF EXISTS goods.goods;
create sequence goods.goods_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS goods.goods
(
    id                    integer      NOT NULL primary key DEFAULT nextval('goods.goods_id_seq'),
    create_date           timestamp with time zone          default CURRENT_TIMESTAMP,
    system_user_id        varchar(50)  not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                goods_status not null,
    last_goods_details_id bigint
);

DROP TABLE IF EXISTS goods.goods_details;
create sequence goods.goods_details_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS goods.goods_details
(
    id                           integer              NOT NULL primary key DEFAULT nextval('goods.goods_details_id_seq'),
    name                         varchar(1024)        not null,
    name_transl                  varchar(1024),
    printing_name                varchar(1024)        not null,
    printing_name_transl         varchar(1024),
    goods_groups_id              integer              not null,
    other_system_connection_code varchar(256),
    goods_suppliers_id           integer              not null,
    manufacturer_code_number     varchar(512),
    price                        numeric              not null,
    currency_id                  integer              not null,
    goods_units_id               integer              not null,
    vat_rate_id                  integer              not null,
    goods_id                     bigint               not null,
    system_user_id               varchar(50)          not null,
    create_date                  timestamp with time zone                  default CURRENT_TIMESTAMP not null,
    modify_system_user_id        varchar(50),
    modify_date                  timestamp with time zone,
    income_account_numbers       varchar(512),
    controlling_orders           varchar(512),
    version_id                   integer,
    status                       goods_details_status not null,
    global_vat_rate              boolean              not null,
    global_sales_area            boolean              not null,
    global_sales_channel         boolean              not null,
    global_segment               boolean              not null,
    constraint goods_details_goods_id_version_id_uk
        unique (goods_id, version_id)
);

DROP TABLE IF EXISTS goods.goods_sales_channels;
create sequence goods.goods_sales_channels_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS goods.goods_sales_channels
(
    id                    integer     NOT NULL primary key DEFAULT nextval('goods.goods_sales_channels_id_seq'),
    goods_details_id      bigint      not null,
    sales_channels_id     integer     not null,
    create_date           timestamp with time zone         default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50) not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                goods_subobject_status
);

create view goods.vw_goods_sales_channels(goods_details_id, sales_channels_name, sales_channels_name_desc) as
SELECT DISTINCT gsc.goods_details_id,
                string_agg(sc.name::text, ', '::text)
                OVER (PARTITION BY gsc.goods_details_id ORDER BY sc.name ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS sales_channels_name,
                string_agg(sc.name::text, ', '::text)
                OVER (PARTITION BY gsc.goods_details_id ORDER BY sc.name DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS sales_channels_name_desc
FROM goods.goods_sales_channels gsc,
     nomenclature.sales_channels sc
WHERE gsc.sales_channels_id = sc.id
  AND gsc.status = 'ACTIVE'::goods_subobject_status;

DROP TABLE IF EXISTS goods.goods_sales_areas;
create sequence goods.goods_sales_areas_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS goods.goods_sales_areas
(
    id                    integer     NOT NULL primary key DEFAULT nextval('goods.goods_sales_areas_id_seq'),
    goods_details_id      bigint      not null,
    sales_areas_id        integer     not null,
    create_date           timestamp with time zone         default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50) not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                goods_subobject_status
);

DROP TABLE IF EXISTS goods.goods_segments;
create sequence goods.goods_segments_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS goods.goods_segments
(
    id                    integer     NOT NULL primary key DEFAULT nextval('goods.goods_segments_id_seq'),
    goods_details_id      bigint      not null,
    segment_id            integer     not null,
    create_date           timestamp with time zone         default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50) not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                goods_subobject_status
);

CREATE SCHEMA prices;

CREATE TYPE price_parameter_status AS ENUM (
    'ACTIVE',
    'DELETED'
    );

CREATE TYPE time_zones AS ENUM (
    'CET',
    'EET'
    );

CREATE TYPE period_types AS ENUM (
    'FIFTEEN_MINUTES',
    'ONE_HOUR',
    'ONE_DAY',
    'ONE_MONTH'
    );

drop sequence if exists prices.price_parameters_id_seq;
drop table if exists prices.price_parameters;
create sequence if not exists prices.price_parameters_id_seq increment by 1;
create table if not exists prices.price_parameters
(
    id                             integer                  NOT NULL primary key DEFAULT nextval('prices.price_parameters_id_seq'),
    time_zone                      time_zones               not null,
    period_type                    period_types             not null,
    create_date                    timestamp with time zone not null,
    system_user_id                 varchar(50)              not null,
    modify_date                    timestamp with time zone,
    modify_system_user_id          varchar,
    status                         price_parameter_status   not null,
    last_price_parameter_detail_id bigint
);
alter sequence prices.price_parameters_id_seq owned by prices.price_parameters.id;

drop sequence if exists prices.price_parameter_details_id_seq;
drop table if exists prices.price_parameter_details;
create sequence if not exists prices.price_parameter_details_id_seq increment by 1;
create table if not exists prices.price_parameter_details
(
    id                    integer       NOT NULL primary key DEFAULT nextval('prices.price_parameter_details_id_seq'),
    name                  varchar(1024) not null,
    price_parameter_id    bigint        not null,
    version_id            integer       not null,
    create_date           timestamp with time zone           default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)   not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence prices.price_parameter_details_id_seq owned by prices.price_parameter_details.id;

drop sequence if exists prices.price_parameter_detail_info_id_seq;
drop table if exists prices.price_parameter_detail_info;
create sequence if not exists prices.price_parameter_detail_info_id_seq increment by 1;
create table if not exists prices.price_parameter_detail_info
(
    id                        integer                  NOT NULL primary key DEFAULT nextval('prices.price_parameter_detail_info_id_seq'),
    period_from               timestamp with time zone not null,
    period_to                 timestamp with time zone not null,
    price                     numeric                  not null,
    is_shifted_hour           boolean                  not null,
    price_parameter_detail_id bigint                   not null,
    create_date               timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id            varchar(50)              not null,
    modify_date               timestamp with time zone,
    modify_system_user_id     varchar(50)
);
alter sequence prices.price_parameter_detail_info_id_seq owned by prices.price_parameter_detail_info.id;

drop table if exists nomenclature.calendars;
create sequence if not exists nomenclature.calendars_id_seq;
create table if not exists nomenclature.calendars
(
    id                    integer             NOT NULL primary key DEFAULT nextval('nomenclature.calendars_id_seq'),
    name                  varchar(512)        not null,
    weekends              varchar(100),
    is_default            boolean             not null,
    status                nomenclature_status not null,
    system_user_id        varchar             not null,
    create_date           timestamp with time zone                 default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    ordering_id           integer
        constraint calendars_ordering_uk
            unique
                deferrable initially deferred
);
alter sequence nomenclature.calendars_id_seq owned by nomenclature.calendars.id;

drop table if exists nomenclature.holidays;
create type nomenclature.holiday_status as enum ('ACTIVE', 'DELETED');
create sequence if not exists nomenclature.holidays_id_seq increment by 1;
create table if not exists nomenclature.holidays
(
    id                    integer                     NOT NULL primary key DEFAULT nextval('nomenclature.holidays_id_seq'),
    calendar_id           integer                     not null,
--         constraint holidays_calendar_fk
--             references nomenclature.calendars,
    holiday               date                        not null,
    system_user_id        varchar(50)                 not null,
    status                nomenclature.holiday_status not null,
    create_date           timestamp with time zone                         default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence nomenclature.holidays_id_seq owned by nomenclature.holidays.id;

create schema if not exists terms;
CREATE TYPE terms.activation_contract_delivery_types AS ENUM (
    'DAY',
    'WEEK',
    'MONTH'
    );
CREATE TYPE terms.resigning_deadline_types AS ENUM (
    'DAY',
    'WEEK',
    'MONTH'
    );
CREATE TYPE terms.general_notice_period_types AS ENUM (
    'DAY',
    'WORKING_DAY',
    'WEEK',
    'MONTH'
    );
CREATE TYPE terms.notice_term_period_types AS ENUM (
    'DAY',
    'WORKING_DAY',
    'WEEK',
    'MONTH'
    );
CREATE TYPE terms.notice_term_disconnection_period_types AS ENUM (
    'DAY',
    'WORKING_DAY',
    'WEEK',
    'MONTH'
    );
CREATE TYPE terms.term_status AS ENUM (
    'ACTIVE',
    'DELETED'
    );

create type terms.term_start_initial_term_of_contract as enum (
    'SIGNING',
    'EXACT_DATE',
    'DATE_OF_CHANGE_OF_CBG',
    'FIRST_DELIVERY',
    'MANUAL'
    );
create type terms.terms_contract_entry_into_force as enum (
    'SIGNING',
    'EXACT_DAY',
    'DATE_CHANGE_OF_CBG',
    'FIRST_DELIVERY',
    'MANUAL'
    );
create type terms.terms_supply_activation as enum (
    'FIRST_DAY_OF_MONTH',
    'EXACT_DATE',
    'MANUAL'
    );
create type terms.terms_wait_for_old_contract_term_to_expire as enum (
    'YES',
    'NO'
    );

drop sequence if exists terms.terms_id_seq;
create sequence if not exists terms.terms_id_seq increment by 1;
create table if not exists terms.terms
(
    id                                                     bigint            NOT NULL primary key DEFAULT nextval('terms.terms_id_seq'),
    name                                                   varchar(1024)     not null,
    contract_delivery_activation_value                     numeric,
    contract_delivery_activation_auto_termination          boolean           not null,
    resigning_deadline_value                               numeric,
    supply_activation_first_day_of_month                   boolean,
    supply_activation_first_day_after_exp_contract         boolean,
    supply_activation_exact_date                           boolean,
    contract_delivery_activation_type                      terms.activation_contract_delivery_types,
    resigning_deadline_type                                terms.resigning_deadline_types,
    supply_activation_manual                               boolean,
    supply_activation_exact_date_start_day                 integer,
    general_notice_period_value                            smallint,
    general_notice_period_type                             terms.general_notice_period_types,
    notice_term_period_value                               smallint,
    notice_term_period_type                                terms.notice_term_period_types,
    notice_term_disconnection_period_value                 smallint,
    notice_term_disconnection_period_type                  terms.notice_term_disconnection_period_types,
    contract_entry_into_force_signing                      boolean,
    contract_entry_into_force_exact_day_of_month           boolean,
    contract_entry_into_force_date_change_of_cbg           boolean,
    contract_entry_into_force_first_delivery               boolean,
    contract_entry_into_force_manual                       boolean,
    no_interest_on_overdue_debts                           boolean           not null,
    create_date                                            timestamp with time zone               default CURRENT_TIMESTAMP not null,
    system_user_id                                         varchar(50)       not null,
    modify_date                                            timestamp with time zone,
    modify_system_user_id                                  varchar(50),
    contract_entry_into_force_exact_day_of_month_start_day smallint,
    status                                                 terms.term_status not null,
    group_detail_id                                        bigint,
    start_initial_term_of_contract                         terms.term_start_initial_term_of_contract[],
    start_initial_term_of_contract_day                     smallint,
    contract_entry_into_force                              terms.terms_contract_entry_into_force[],
    supply_activation                                      terms.terms_supply_activation[],
    wait_for_old_contract_term_to_expire                   terms.terms_wait_for_old_contract_term_to_expire[],
    first_day_of_month_initial_contract_term               integer
);

CREATE TYPE calendar_types AS ENUM (
    'WORKING_DAYS',
    'CALENDAR_DAYS',
    'CERTAIN_DAYS'
    );
CREATE TYPE payment_term_status AS ENUM (
    'ACTIVE',
    'DELETED'
    );
CREATE TYPE due_date_change AS ENUM (
    'PREVIOUS_WORKING_DAY',
    'NEXT_WORKING_DAY'
    );


drop sequence if exists terms.invoice_payment_terms_id_seq;
create sequence if not exists terms.invoice_payment_terms_id_seq increment by 1;
create table if not exists terms.invoice_payment_terms
(
    id                    bigint              NOT NULL primary key DEFAULT nextval('terms.invoice_payment_terms_id_seq'),
    type                  calendar_types      not null,
    value                 smallint,
    value_from            integer,
    value_to              integer,
    calendar_id           integer             not null,
    exclude_weekends      boolean             not null,
    term_id               bigint              not null,
    create_date           timestamp with time zone                 default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)         not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                payment_term_status not null,
    exclude_holidays      boolean             not null,
    due_date_change       due_date_change,
    name                  varchar(1024)       not null
);

DROP TABLE IF EXISTS nomenclature.grid_operators;
create sequence nomenclature.grid_operators_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.grid_operators
(
    id                                      integer             NOT NULL primary key DEFAULT nextval('nomenclature.grid_operators_id_seq'),
    name                                    varchar(512)        not null,
    full_name                               varchar(512)        not null,
    power_supply_termination_request_email  varchar(512),
    power_supply_reconnection_request_email varchar(512),
    objection_to_change_cbg_email           varchar(512),
    code                                    varchar(32),
    code_for_xenergy                        varchar(32),
    ordering_id                             integer             not null
        constraint "grid operators_ordering_uk"
            unique
                deferrable initially deferred,
    create_date                             timestamp with time zone                 default CURRENT_TIMESTAMP not null,
    system_user_id                          varchar(50)         not null,
    modify_date                             timestamp with time zone,
    modify_system_user_id                   varchar(50),
    status                                  nomenclature_status not null,
    is_default                              boolean             not null,
    is_owned_by_energo_pro                  boolean,
    is_hard_coded                           boolean
);
alter sequence nomenclature.sales_areas_id_seq owned by nomenclature.grid_operators.id;

create schema if not exists product;
create type product.auto_termination_from as enum ('FIRST_DAY_OF_MONTH', 'EVENT_DATE', 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE');
create type product.event as enum ('EXPIRATION_OF_THE_CONTRACT_TERM','DEACTIVATION_OF_POINTS_OF_DELIVERY','EXPIRATION_OF_THE_NOTICE');
create type product.notice_due_type as enum ('DAY', 'WEEK', 'MONTH');
create type product.termination_status as enum ('ACTIVE', 'DELETED');
create type product.termination_calculate_from as enum ('CONTRACT_END_DATE', 'REQUIRED_TERMINATION_DATE');

DROP TABLE IF EXISTS product.terminations;
create sequence product.terminations_id_seq increment by 1;
create table if not exists product.terminations
(
    id                          bigint                     not null primary key default nextval('product.terminations_id_seq'),
    name                        varchar(2048)              not null,
    contract_clause_number      varchar(2048),
    auto_termination            boolean                    not null,
    auto_termination_from       product.auto_termination_from,
    event                       product.event,
    notice_due                  boolean                    not null,
    notice_due_value_min        integer,
    notice_due_type             product.notice_due_type,
    auto_email_notification     boolean,
    additional_info             varchar(4096),
    status                      product.termination_status not null,
    system_user_id              varchar(50)                not null,
    create_date                 timestamp with time zone                        default CURRENT_TIMESTAMP not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50),
    termination_group_detail_id bigint,
    notice_due_value_max        integer,
    contract_template_id        bigint,
    calculate_from              product.termination_calculate_from,
    penalties_with_valid_notice    bigint,
    penalties_without_valid_notice bigint,
    penalties_without_notice       bigint
);
alter sequence product.terminations_id_seq owned by product.terminations.id;


create type product.termination_notification_channel as enum ('EMAIL', 'SMS', 'PHONE', 'PAPER', 'OTHER');

DROP TABLE IF EXISTS product.termination_notification_channels;
create sequence product.termination_notification_channels_id_seq increment by 1;
create table if not exists product.termination_notification_channels
(
    termination_id        bigint                                   not null,
--         constraint termination_notification_channels_termination_fk
--             references product.terminations,
    notification_channel  product.termination_notification_channel not null,
    system_user_id        varchar(50)                              not null,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    id                    bigint                                   not null primary key default nextval('product.termination_notification_channels_id_seq'),
    modify_system_user_id varchar(50),
    modify_date           timestamp with time zone
);
alter sequence product.termination_notification_channels_id_seq owned by product.termination_notification_channels.id;

DROP TABLE IF EXISTS terms.term_groups;
create sequence terms.groups_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS terms.term_groups
(
    id                    integer           NOT NULL primary key DEFAULT nextval('terms.groups_id_seq'),
    create_date           timestamp with time zone               default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)       not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                terms.term_status not null,
    last_group_detail_id  bigint
);

DROP TABLE IF EXISTS terms.term_group_details;
create sequence terms.group_details_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS terms.term_group_details
(
    id                    integer       NOT NULL primary key DEFAULT nextval('terms.group_details_id_seq'),
    name                  varchar(1024) not null,
    group_id              bigint,
    create_date           timestamp with time zone           default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)   not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    version_id            integer       not null,
    start_date            date,
    constraint group_details_group_version_uk
        unique (group_id, version_id)
);
create type terms.penalty_party_receiver as enum ('ENERGO_PRO', 'CUSTOMER');
create type terms.penalty_payment_term_exclude as enum ('WEEKENDS', 'HOLIDAYS');
create type terms.applicability as enum ('CONTRACT', 'POD', 'EVENT');
create type terms.penalty_status as enum ('ACTIVE', 'DELETED');
create table terms.penalties
(
    id                           bigint generated by default as identity (maxvalue 2147483647)
        primary key,
    name                         varchar(1024)                                      not null,
    contract_clause_number       varchar(2048)            default 2048,
    process_id                   bigint,
    process_start_code           varchar                  default 1024,
    applicability                terms.applicability                                not null,
    amount_calculation_formula   text,
    min_amount                   numeric,
    max_amount                   numeric,
    currency_id                  integer,
    automatic_submission         boolean                                            not null,
    additional_info              varchar(4096),
    term_id                      bigint,
    penalty_group_detail_id      bigint,
    create_date                  timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id               varchar(50)                                        not null,
    modify_date                  timestamp with time zone,
    modify_system_user_id        varchar(50),
    penalty_party_receiver       terms.penalty_party_receiver[],
    status                       terms.penalty_status                               not null,
    template_id                  bigint,
    email_template_id            bigint,
    no_interest_on_overdue_debts boolean
);

create table terms.penalty_payment_terms
(
    id                    bigint generated by default as identity
        constraint penalty_payment_terms_pk
            primary key,
    type                  calendar_types                                     not null,
    value                 smallint,
    value_from            integer,
    value_to              integer,
    calendar_id           integer                                            not null,

    penalty_id            bigint                                             not null,

    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                payment_term_status                                not null,
    due_date_change       due_date_change,
    name                  varchar(1024)                                      not null,
    excludes              terms.penalty_payment_term_exclude[]               not null
);

create table terms.penalty_group_details
(
    id                    bigint generated by default as identity
        primary key,
    name                  varchar(1024)                                      not null,
    penalty_group_id      bigint                                             not null,
    version_id            integer                                            not null,
    start_date            date,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table terms.penalty_group_penalties
(
    id                      bigint generated by default as identity
        primary key,
    penalty_group_detail_id bigint                   not null,
    penalty_id              bigint                   not null,
    status                  terms.penalty_status     not null,
    create_date             timestamp with time zone not null,
    system_user_id          varchar(50)              not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50)
);

CREATE TYPE termination_group_status AS ENUM (
    'ACTIVE',
    'DELETED'
    );

CREATE TYPE termination_group_termination_status AS ENUM (
    'ACTIVE',
    'DELETED'
    );

DROP TABLE IF EXISTS product.termination_groups;
create sequence product.termination_groups_id_seq increment by 1;
create table if not exists product.termination_groups
(
    id                    bigint                   not null primary key default nextval('product.termination_groups_id_seq'),
    status                termination_group_status not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence product.termination_groups_id_seq owned by product.termination_groups.id;

DROP TABLE IF EXISTS product.termination_groups;
create sequence product.termination_groups_id_seq increment by 1;
create table if not exists product.termination_groups
(
    id                    bigint                   not null primary key default nextval('product.termination_groups_id_seq'),
    status                termination_group_status not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence product.termination_groups_id_seq owned by product.termination_groups.id;

DROP TABLE IF EXISTS product.termination_group_details;
create sequence product.termination_group_details_id_seq increment by 1;
create table if not exists product.termination_group_details
(
    id                    bigint        not null primary key default nextval('product.termination_group_details_id_seq'),
    name                  varchar(1024) not null,
    start_date            date          not null,
    create_date           timestamp                          default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)   not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    termination_group_id  bigint        not null,
    version_id            integer       not null,
    constraint termination_group_details_group_start_date
        unique (start_date, termination_group_id)
);
alter sequence product.termination_group_details_id_seq owned by product.termination_group_details.id;

DROP TABLE IF EXISTS product.termination_group_terminations;
create sequence product.termination_group_terminations_id_seq increment by 1;
create table if not exists product.termination_group_terminations
(
    id                          bigint                               not null primary key default nextval('product.termination_group_terminations_id_seq'),
    termination_id              bigint                               not null,
    termination_group_detail_id bigint,
    status                      termination_group_termination_status not null,
    create_date                 timestamp with time zone                                  default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                          not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50)
);
alter sequence product.termination_group_terminations_id_seq owned by product.termination_group_terminations.id;
create table terms.penalty_groups
(
    id                    bigint generated by default as identity
        primary key,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP,
    system_user_id        varchar(50)              default 50,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                terms.penalty_status not null
);

create schema if not exists product;

create type product.product_detail_status as enum ('ACTIVE', 'INACTIVE');
create type product.provided_capacity_limit_type as enum ('FROM', 'TO');

drop sequence if exists product.product_details_id_seq;
create sequence if not exists product.product_details_id_seq increment by 1;

create type product.product_contract_type as enum ('COMBINED', 'SUPPLY_ONLY', 'WITHOUT_SUPPLY', 'SUPPLY_BALANCING');
create type product.product_consumption_purpose as enum ('HOUSEHOLD', 'NON_HOUSEHOLD');
create type product.product_forecasting as enum ('ENERGO_PRO', 'CUSTOMER');
create type product.product_payment_guarantee as enum ('NO', 'CASH_DEPOSIT', 'BANK', 'CASH_DEPOSIT_AND_BANK');
create type product.product_pod_metering_type as enum ('SETTLEMENT_PERIOD', 'SLP');
create type product.product_schedule_registration as enum ('ENERGO_PRO', 'CUSTOMER', 'OTHER');
create type product.product_taking_over_balancing_cost as enum ('ENERGO_PRO', 'CUSTOMER');
create type product.product_voltage_level as enum ('LOW', 'MEDIUM', 'MEDIUM_DIRECT_CONNECTED', 'HIGH');
create type product.product_pod_type as enum ('CONSUMER', 'GENERATOR');
create type product.payment_channels as enum ('CSC', 'CASH_DESK_IN_BANK', 'ONLINE');

drop table if exists product.product_details;
create table if not exists product.product_details
(
    id                                    bigint                        NOT NULL primary key DEFAULT nextval('product.product_details_id_seq'),
    name                                  varchar(1024)                 not null,
    name_transl                           varchar(1024)                 not null,
    available_for_sale                    boolean,
    available_from                        timestamp with time zone,
    available_to                          timestamp with time zone,
    printing_name                         varchar                       not null,
    printing_name_transl                  varchar(1024)                 not null,
    short_description                     varchar(8192),
    full_description                      varchar(32768),
    invoice_and_templates_text            varchar(2048),
    invoice_and_templates_text_transl     varchar(2048),
    product_group_id                      integer,
    other_system_connection_code          varchar(256),
    product_type_id                       integer                       not null,
    income_account_number                 varchar(32),
    cost_center_controlling_order         varchar(32),
    vat_rate_id                           integer,
    electricity_price_type_id             integer,
    equal_monthly_installments_activation boolean,
    installment_number                    smallint,
    installment_number_from               smallint,
    installment_number_to                 smallint,
    amount                                numeric,
    amount_from                           numeric,
    amount_to                             numeric,
    currency_id                           integer,
    status                                product.product_detail_status not null,
    product_id                            bigint,
    create_date                           timestamp with time zone                           default CURRENT_TIMESTAMP not null,
    system_user_id                        varchar(50)                   not null,
    modify_system_user_id                 varchar(50),
    provided_capacity_limit_type          product.provided_capacity_limit_type,
    provided_capacity_limit_amount        smallint,
    version_id                            integer                       not null,
    global_vat_rate                       boolean,
    global_sales_area                     boolean,
    global_sales_channel                  boolean,
    global_segment                        boolean,
    consumption_purpose                   product.product_consumption_purpose[],
    forecasting                           product.product_forecasting[],
    payment_guarantee                     product.product_payment_guarantee[],
    pod_metering_type                     product.product_pod_metering_type[],
    schedule_registration                 product.product_schedule_registration[],
    voltage_level                         product.product_voltage_level[],
    ineligible_payment_channel            product.payment_channels[],
    taking_over_balancing_cost            product.product_taking_over_balancing_cost[],
    modify_date                           timestamp with time zone,
    term_id                               bigint,
    term_group_id                         bigint,
    additional_info1                      varchar(1024),
    additional_info2                      varchar(1024),
    additional_info3                      varchar(1024),
    additional_info4                      varchar(1024),
    additional_info5                      varchar(1024),
    additional_info6                      varchar(1024),
    additional_info7                      varchar(1024),
    additional_info8                      varchar(1024),
    additional_info9                      varchar(1024),
    additional_info10                     varchar(1024),
    cash_deposit_amount                   numeric,
    cash_deposit_currency_id              bigint,
    bank_guarantee_amount                 numeric,
    bank_guarantee_currency_id            bigint,
    product_balancing_id_for_consumer     bigint,
    global_grid_operator                  boolean,
    pod_type                              product.product_pod_type[],
    product_balancing_id_for_generator    bigint,
    contract_type_text                    varchar(22222),
    contract_type                         product.product_contract_type[],
    invoice_template_id                   bigint,
    is_promotional                        boolean,
    apply_to_specific_customers           boolean,
    is_resignable                         boolean,
    constraint product_details_product_version_uk
        unique (product_id, version_id)
);

create type product.product_status as enum ('ACTIVE', 'DELETED');
create type product.product_file_status as enum ('DRAFT', 'SIGNED');
drop sequence if exists product.product_id_seq;
create sequence if not exists product.product_id_seq increment by 1;

drop table if exists product.products;
create table if not exists product.products
(
    id                     bigint                 NOT NULL primary key DEFAULT nextval('product.product_id_seq'),
    status                 product.product_status not null,
    create_date            timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)            not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50),
    last_product_detail_id bigint,
    customer_identifier    varchar(2048)
);

drop sequence if exists product.product_consumption_purposes_id_seq;
create sequence if not exists product.product_consumption_purposes_id_seq increment by 1;

drop table if exists product.product_consumption_purposes;
create table if not exists product.product_consumption_purposes
(
    id                    bigint                              NOT NULL primary key DEFAULT nextval('product.product_consumption_purposes_id_seq'),
    consumption_purpose   product.product_consumption_purpose not null,
    product_detail_id     bigint                              not null,
    create_date           timestamp with time zone                                 default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                         not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

drop sequence if exists product.product_detail_contract_types_id_seq;
create sequence if not exists product.product_detail_contract_types_id_seq increment by 1;

drop table if exists product.product_contract_types;
create table if not exists product.product_contract_types
(
    id                    bigint                        NOT NULL primary key DEFAULT nextval('product.product_detail_contract_types_id_seq'),
    contract_type         product.product_contract_type not null,
    product_detail_id     bigint                        not null,
    create_date           timestamp with time zone                           default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                   not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

drop sequence if exists product.product_forecastings_id_seq;
create sequence if not exists product.product_forecastings_id_seq increment by 1;

drop table if exists product.product_forecastings;
create table if not exists product.product_forecastings
(
    id                    bigint                      NOT NULL primary key DEFAULT nextval('product.product_forecastings_id_seq'),
    forecasting           product.product_forecasting not null,
    product_detail_id     bigint                      not null,
    create_date           timestamp with time zone                         default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                 not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);


drop sequence if exists product.product_pod_metering_types_id_seq;
create sequence if not exists product.product_pod_metering_types_id_seq increment by 1;

drop table if exists product.product_pod_metering_types;
create table if not exists product.product_pod_metering_types
(
    id                    bigint                            NOT NULL primary key DEFAULT nextval('product.product_pod_metering_types_id_seq'),
    pod_metering_types    product.product_pod_metering_type not null,
    product_detail_id     bigint                            not null,
    create_date           timestamp with time zone                               default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                       not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);


drop sequence if exists product.product_payment_guarantees_id_seq;
create sequence if not exists product.product_payment_guarantees_id_seq increment by 1;

drop table if exists product.product_payment_guarantees;
create table if not exists product.product_payment_guarantees
(
    id                    bigint                            NOT NULL primary key DEFAULT nextval('product.product_payment_guarantees_id_seq'),
    payment_guarantee     product.product_payment_guarantee not null,
    product_detail_id     bigint                            not null,
    create_date           timestamp with time zone                               default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                       not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

drop sequence if exists product.product_detail_voltage_levels_id_seq;
create sequence if not exists product.product_detail_voltage_levels_id_seq;

drop table if exists product.product_voltage_levels;
create table if not exists product.product_voltage_levels
(
    id                    bigint                        NOT NULL primary key DEFAULT nextval('product.product_detail_voltage_levels_id_seq'),
    voltage_level         product.product_voltage_level not null,
    product_detail_id     bigint                        not null,
    create_date           timestamp with time zone                           default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                   not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

drop sequence if exists product.product_schedule_registrations_id_seq;
create sequence if not exists product.product_schedule_registrations_id_seq increment by 1;


drop table if exists product.product_schedule_registrations;
create table if not exists product.product_schedule_registrations
(
    id                    bigint                                NOT NULL primary key DEFAULT nextval('product.product_schedule_registrations_id_seq'),
    schedule_registration product.product_schedule_registration not null,
    product_detail_id     bigint                                not null,
    create_date           timestamp with time zone                                   default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                           not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

drop sequence if exists product.product_taking_over_balancing_costs_id_seq;
create sequence if not exists product.product_taking_over_balancing_costs_id_seq increment by 1;

drop table if exists product.product_taking_over_balancing_costs;
create table if not exists product.product_taking_over_balancing_costs
(
    id                         bigint                                     NOT NULL primary key DEFAULT nextval('product.product_taking_over_balancing_costs_id_seq'),
    taking_over_balancing_cost product.product_taking_over_balancing_cost not null,
    product_detail_id          bigint                                     not null,
    create_date                timestamp with time zone                                        default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50)
);

drop sequence if exists product.product_grid_operators_id_seq;
create sequence if not exists product.product_grid_operators_id_seq increment by 1;

create type product.product_subobject_status as enum ('ACTIVE', 'DELETED');

drop table if exists product.product_grid_operators;
create table if not exists product.product_grid_operators
(
    id                    bigint                           NOT NULL primary key DEFAULT nextval('product.product_grid_operators_id_seq'),
    product_detail_id     bigint                           not null,
    status                product.product_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    grid_operator_id      integer                          not null
);


drop sequence if exists product.product_spec_offer_contracts_id_seq;
create sequence if not exists product.product_spec_offer_contracts_id_seq increment by 1;

drop table if exists product.product_spec_offer_contracts;
create table if not exists product.product_spec_offer_contracts
(
    id                    bigint                           NOT NULL primary key DEFAULT nextval('product.product_spec_offer_contracts_id_seq'),
    product_detail_id     bigint                           not null,
    contract_id           bigint                           not null,
    contract_detail_id    bigint,
    status                product.product_subobject_status not null,
    system_user_id        varchar(50)                      not null default '',
    create_date           timestamp                        not null default now(),
    modify_date           timestamp,
    modify_system_user_id varchar(50)
);

drop sequence if exists product.product_spec_offer_customers_id_seq;
create sequence if not exists product.product_spec_offer_customers_id_seq increment by 1;

drop table if exists product.product_spec_offer_customers;
create table if not exists product.product_spec_offer_customers
(
    id                    bigint                           NOT NULL primary key DEFAULT nextval('product.product_spec_offer_customers_id_seq'),
    product_detail_id     bigint                           not null,
    customer_id           bigint                           not null,
    customer_detail_id    bigint,
    status                product.product_subobject_status not null,
    system_user_id        varchar(50)                      not null default '',
    create_date           timestamp                        not null default now(),
    modify_date           timestamp,
    modify_system_user_id varchar(50)
);


drop sequence if exists product.product_resign_products_id_seq;
create sequence if not exists product.product_resign_products_id_seq increment by 1;

drop table if exists product.product_resign_products;
create table if not exists product.product_resign_products
(
    id                       bigint                           NOT NULL primary key DEFAULT nextval('product.product_resign_products_id_seq'),
    product_detail_id        bigint                           not null,
    target_product_id        bigint                           not null,
    target_product_detail_id bigint,
    status                   product.product_subobject_status not null,
    system_user_id           varchar(50)                      not null,
    create_date              timestamp                        not null,
    modify_date              timestamp,
    modify_system_user_id    varchar(50)
);


drop sequence if exists product.product_payment_method_limitations_id_seq;
create sequence if not exists product.product_payment_method_limitations_id_seq increment by 1;

drop table if exists product.product_payment_method_limitations;
create table if not exists product.product_payment_method_limitations
(
    id                         bigint                           NOT NULL primary key DEFAULT nextval('product.product_payment_method_limitations_id_seq'),
    product_detail_id          bigint                           not null,
    ineligible_payment_channel product.payment_channels         not null,
    status                     product.product_subobject_status not null,
    create_date                timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                      not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50)
);

drop sequence if exists product.product_contract_terms_id_seq;
create sequence if not exists product.product_contract_terms_id_seq increment by 1;

create type product.product_contract_term_period_type as enum ('PERIOD', 'CERTAIN_DATE', 'WITHOUT_TERM', 'OTHER');
create type product.product_contract_term_type as enum ('DAY_DAYS', 'MONTH_MONTHS', 'YEAR_YEARS');
create type product.product_contract_term_renewal_period_type as enum ('DAY_DAYS', 'MONTH_MONTHS', 'YEAR_YEARS');

drop table if exists product.product_contract_terms;
create table if not exists product.product_contract_terms
(
    id                        bigint                           NOT NULL primary key DEFAULT nextval('product.product_contract_terms_id_seq'),
    name                      varchar(512)                     not null,
    contract_term_period_type product.product_contract_term_period_type,
    contract_term_type        product.product_contract_term_type,
    value                     integer,
    perpetuity_cause          boolean,
    auto_termination          boolean,
    create_date               timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id            varchar(50)                      not null,
    modify_date               timestamp with time zone,
    modify_system_user_id     varchar(50),
    status                    product.product_subobject_status not null,
    product_details_id        bigint,
    automatic_renewal         boolean,
    number_of_renewals        integer,
    renewal_period_value      integer,
    renewal_period_type       product.product_contract_term_renewal_period_type
);

drop sequence if exists product.product_sales_channels_id_seq;
create sequence if not exists product.product_sales_channels_id_seq;

drop table if exists product.product_sales_channels;
create table if not exists product.product_sales_channels
(
    id                    bigint                           NOT NULL primary key DEFAULT nextval('product.product_sales_channels_id_seq'),
    product_detail_id     bigint                           not null,
    status                product.product_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    sales_channel_id      integer                          not null
);

drop sequence if exists product.product_sales_areas_id_seq;
create sequence if not exists product.product_sales_areas_id_seq;

drop table if exists product.product_sales_areas;
create table if not exists product.product_sales_areas
(
    id                    bigint                           NOT NULL primary key DEFAULT nextval('product.product_sales_areas_id_seq'),
    product_detail_id     bigint                           not null,
    status                product.product_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    sales_area_id         integer                          not null
);

drop sequence if exists product.product_segments_id_seq;
create sequence if not exists product.product_segments_id_seq;

drop table if exists product.product_segments;
create table if not exists product.product_segments
(
    id                    bigint                           NOT NULL primary key DEFAULT nextval('product.product_segments_id_seq'),
    product_detail_id     bigint                           not null,
    status                product.product_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    segment_id            integer                          not null
);

CREATE TYPE terms.term_group_term_status AS ENUM (
    'ACTIVE', 'DELETED'
    );
DROP TABLE IF EXISTS terms.term_group_terms;
create sequence terms.term_group_terms_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS terms.term_group_terms
(
    id                    integer                      NOT NULL primary key DEFAULT nextval('terms.term_group_terms_id_seq'),
    term_group_detail_id  bigint                       not null,
    term_id               bigint                       not null,
    status                terms.term_group_term_status not null,
    create_date           timestamp with time zone                          default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                  not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists nomenclature.scales_id_seq increment by 1;

drop table if exists nomenclature.scales;
create table if not exists nomenclature.scales
(
    id                             integer             NOT NULL primary key DEFAULT nextval('nomenclature.scales_id_seq'),
    name                           varchar(512)        not null,
    grid_operator_id               integer             not null,
    scale_type                     varchar(512)        not null,
    scale_code                     varchar(512),
    tariff_scale                   varchar(512),
    is_default                     boolean             not null,
    calculation_for_number_of_days boolean,
    scale_for_active_electricity   boolean,
    ordering_id                    integer             not null,
    create_date                    timestamp with time zone                 default CURRENT_TIMESTAMP not null,
    system_user_id                 varchar(50)         not null,
    modify_date                    timestamp with time zone,
    modify_system_user_id          varchar(50),
    status                         nomenclature_status not null
);

-- price component

create schema price_component;

CREATE TYPE price_component_group_status AS ENUM ('ACTIVE','DELETED');
CREATE TYPE price_component_group_price_component_status AS ENUM ('ACTIVE','DELETED');
create type pc_number_type as enum ('POSITIVE', 'NEGATIVE');
create type pc_status as enum ('ACTIVE', 'DELETED');
create type pc_issued_separate_invoice as enum ('INVOICE_ONE', 'INVOICE_TWO', 'INVOICE_THREE', 'INVOICE_FOUR');

DROP TABLE IF EXISTS price_component.price_components;
create sequence price_component.price_components_id_seq increment by 1;
create table if not exists price_component.price_components
(
    id                                       bigint                     not null primary key default nextval('price_component.price_components_id_seq'),
    name                                     varchar(1024)              not null,
    invoice_and_template_text                varchar(1024),
    price_component_price_type_id            integer                    not null,
--         constraint price_components_price_component_price_type_fk
--             references nomenclature.price_component_price_types,
    price_component_value_type_id            integer                    not null,
--         constraint price_components_price_component_value_type_fk
--             references nomenclature.price_component_value_types,
    currency_id                              integer                    not null,
--         constraint price_components_currency_fk
--             references nomenclature.currencies,
    vat_rate_id                              integer,
--         constraint price_components_vat_rate_fk
--             references nomenclature.vat_rates,
    number_type                              pc_number_type             not null,
    global_vat_rate                          boolean,
    discount                                 boolean,
    income_account_number                    varchar(32),
    cost_center_controlling_order            varchar(32),
    contract_template_tag                    varchar(512),
    price_in_words                           varchar(2048),
    price_formula                            varchar(4096)              not null,
    issued_separate_invoice                  pc_issued_separate_invoice not null,
    conditions                               varchar(4096),
    status                                   pc_status                  not null,
    create_date                              timestamp with time zone                        default CURRENT_TIMESTAMP not null,
    system_user_id                           varchar(50)                not null,
    modify_date                              timestamp with time zone,
    modify_system_user_id                    varchar(50),
    price_component_group_detail_id          bigint,
    xenergie_application                     pc_xenergie_application,
    don_not_include_in_the_vat_base          boolean,
    alt_invoice_recipient_customer_detail_id bigint
);
alter sequence price_component.price_components_id_seq owned by price_component.price_components.id;

DROP TABLE IF EXISTS price_component.price_component_groups;
create sequence price_component.price_component_groups_id_seq increment by 1;
create table if not exists price_component.price_component_groups
(
    id                    bigint                       not null primary key default nextval('price_component.price_component_groups_id_seq'),
    status                price_component_group_status not null,
    create_date           timestamp with time zone                          default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                  not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence price_component.price_component_groups_id_seq owned by price_component.price_component_groups.id;

DROP TABLE IF EXISTS price_component.price_component_group_details;
create sequence price_component.price_component_group_details_id_seq increment by 1;
create table if not exists price_component.price_component_group_details
(
    id                       bigint        not null primary key default nextval('price_component.price_component_group_details_id_seq'),
    name                     varchar(1024) not null,
    start_date               date          not null,
    end_date                 date,
    create_date              timestamp                          default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)   not null,
    modify_date              timestamp,
    modify_system_user_id    varchar(50),
    price_component_group_id bigint        not null,
    version_id               integer       not null,
    constraint price_component_group_details_group_start_date
        unique (start_date, price_component_group_id)
);
alter sequence price_component.price_component_group_details_id_seq owned by price_component.price_component_group_details.id;

DROP TABLE IF EXISTS price_component.pc_group_pcs;
create sequence price_component.pc_group_pcs_id_seq increment by 1;
create table if not exists price_component.pc_group_pcs
(
    id                              bigint                                       not null primary key default nextval('price_component.pc_group_pcs_id_seq'),
    price_component_id              bigint                                       not null,
    price_component_group_detail_id bigint,
    status                          price_component_group_price_component_status not null,
    create_date                     timestamp with time zone                                          default CURRENT_TIMESTAMP not null,
    system_user_id                  varchar(50)                                  not null,
    modify_date                     timestamp with time zone,
    modify_system_user_id           varchar(50)
);
alter sequence price_component.pc_group_pcs_id_seq owned by price_component.pc_group_pcs.id;


create schema interim_advance_payment;

create type interim_advance_payment.iap_value_type as enum
    ('PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT', 'EXACT_AMOUNT', 'PRICE_COMPONENT');

create type interim_advance_payment.iap_date_of_issue_type as enum
    ('MATCH_THE_INVOICE_DATE', 'DATE_OF_THE_MONTH', 'WORKING_DAYS_AFTER_INVOICE_DATE', 'PERIODICAL');

create type interim_advance_payment.iap_payment_type as enum ('OBLIGATORY', 'AT_LEAST_ONE');

create type interim_advance_payment.iap_period_type as enum ('DAY_OF_WEEK_AND_PERIOD_OF_YEAR', 'DAY_OF_MONTH');

create type interim_advance_payment.iap_issuing_for_the_month_to_current as enum
    ('MINUS_ONE', 'ZERO', 'PLUS_ONE', 'PLUS_TWO', 'PLUS_TWELVE');

create type interim_advance_payment.iap_deduction_from as enum
    ('FIRST_INVOICE_FOR_SAME_PERIOD', 'FIRST_INVOICE_WITH_LONGER_PAYMENT_TERM');

create type interim_advance_payment.iap_status as enum ('ACTIVE', 'DELETED');

DROP TABLE IF EXISTS interim_advance_payment.interim_advance_payments;
create sequence interim_advance_payment.interim_advance_payments_id_seq increment by 1;
create table if not exists interim_advance_payment.interim_advance_payments
(
    id                               bigint                                         not null primary key default nextval('interim_advance_payment.interim_advance_payments_id_seq'),
    name                             varchar(1024)                                  not null,
    value_type                       interim_advance_payment.iap_value_type         not null,
    value                            smallint,
    value_from                       smallint,
    value_to                         smallint,
    currency_id                      integer,
--         constraint interim_advance_payments_currency_fk
--             references nomenclature.currencies,
    date_of_issue_type               interim_advance_payment.iap_date_of_issue_type not null,
    date_of_issue_value              smallint,
    date_of_issue_value_from         smallint,
    date_of_issue_value_to           smallint,
    price_component_id               bigint,
    payment_type                     interim_advance_payment.iap_payment_type,
    period_type                      interim_advance_payment.iap_period_type,
    match_term_of_standard_invoice   boolean                                        not null,
    no_interest_on_overdue_debts     boolean                                        not null,
    year_round                       boolean                                        not null,
    issuing_for_the_month_to_current interim_advance_payment.iap_issuing_for_the_month_to_current,
    deduction_from                   interim_advance_payment.iap_deduction_from     not null,
    status                           interim_advance_payment.iap_status             not null,
    system_user_id                   varchar(50)                                    not null,
    create_date                      timestamp with time zone                                            default CURRENT_TIMESTAMP not null,
    modify_date                      timestamp with time zone,
    modify_system_user_id            varchar(50),
    iap_group_detail_id              bigint,
    has_missing_invoice              boolean
);
alter sequence interim_advance_payment.interim_advance_payments_id_seq owned by interim_advance_payment.interim_advance_payments.id;

create type interim_advance_payment.iapt_calendar_type
as enum ('WORKING_DAYS', 'CALENDAR_DAYS', 'CERTAIN_DAYS');

create type interim_advance_payment.iapt_due_date_change
as enum ('PREVIOUS_WORKING_DAY', 'NEXT_WORKING_DAY');

create type interim_advance_payment.iap_subobject_status
as enum ('ACTIVE', 'DELETED');

DROP TABLE IF EXISTS interim_advance_payment.interim_advance_payment_terms;
create sequence interim_advance_payment.interim_advance_payment_terms_id_seq increment by 1;
create table if not exists interim_advance_payment.interim_advance_payment_terms
(
    id                         bigint                                       not null primary key default nextval('interim_advance_payment.interim_advance_payment_terms_id_seq'),
    type                       interim_advance_payment.iapt_calendar_type   not null,
    value                      smallint,
    value_from                 integer,
    value_to                   integer,
    calendar_id                integer                                      not null,
--         constraint interim_advance_payment_terms_calendar_fk
--             references nomenclature.calendars,
    exclude_weekends           boolean                                      not null,
    interim_advance_payment_id bigint                                       not null,
--         constraint interim_advance_payment_terms_interim_advance_payment_fk
--             references interim_advance_payment.interim_advance_payments,
    create_date                timestamp with time zone                                          default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                  not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    status                     interim_advance_payment.iap_subobject_status not null,
    exclude_holidays           boolean                                      not null,
    due_date_change            interim_advance_payment.iapt_due_date_change,
    name                       varchar(1024)                                not null
);
alter sequence interim_advance_payment.interim_advance_payment_terms_id_seq owned by interim_advance_payment.interim_advance_payment_terms.id;


DROP TABLE IF EXISTS interim_advance_payment.interim_advance_payment_issuing_periods;
create sequence interim_advance_payment.interim_advance_payment_issuing_periods_id_seq increment by 1;
create table if not exists interim_advance_payment.interim_advance_payment_issuing_periods
(
    id                         bigint                                       not null primary key default nextval('interim_advance_payment.interim_advance_payment_issuing_periods_id_seq'),
    period_from                varchar(5)                                   not null,
    period_to                  varchar(5)                                   not null,
    interim_advance_payment_id bigint                                       not null,
--         constraint interim_advance_payment_issuing_periods_interim_advance_payment
--             references interim_advance_payment.interim_advance_payments,
    status                     interim_advance_payment.iap_subobject_status not null,
    create_date                timestamp with time zone                                          default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                  not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50)
);
alter sequence interim_advance_payment.interim_advance_payment_issuing_periods_id_seq owned by interim_advance_payment.interim_advance_payment_issuing_periods.id;

create type interim_advance_payment.iap_week
as enum ('FIRST_WEEK', 'SECOND_WEEK', 'THIRD_WEEK', 'FOURTH_WEEK', 'FIFTH_WEEK', 'LAST_WEEK');


create type interim_advance_payment.iap_day
as enum ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY', 'ALL_DAYS');


DROP TABLE IF EXISTS interim_advance_payment.interim_advance_payment_day_week_period_year;
create sequence interim_advance_payment.interim_advance_payment_day_week_period_year_id_seq increment by 1;
create table if not exists interim_advance_payment.interim_advance_payment_day_week_period_year
(
    id                         bigint                                       not null primary key default nextval('interim_advance_payment.interim_advance_payment_day_week_period_year_id_seq'),
    interim_advance_payment_id bigint                                       not null,
--         constraint interim_advance_payment_day_week_period_year_interim_advance_pa
--             references interim_advance_payment.interim_advance_payments,
    week                       interim_advance_payment.iap_week             not null,
    status                     interim_advance_payment.iap_subobject_status not null,
    create_date                timestamp with time zone                                          default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                  not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    day                        interim_advance_payment.iap_day[]
);
alter sequence interim_advance_payment.interim_advance_payment_day_week_period_year_id_seq owned by interim_advance_payment.interim_advance_payment_day_week_period_year.id;

create type interim_advance_payment.iap_month_number
as enum ('ONE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEN', 'ELEVEN', 'TWELVE', 'THIRTEEN', 'FOURTEEN', 'FIFTEEN', 'SIXTEEN', 'SEVENTEEN', 'EIGHTEEN', 'NINETEEN', 'TWENTY', 'TWENTYONE', 'TWENTYTWO', 'TWENTYTHREE', 'TWENTYFOUR', 'TWENTYFIVE', 'TWENTYSIX', 'TWENTYSEVEN', 'TWENTYEIGHT', 'TWENTYNINE', 'THIRTY', 'THIRTYONE', 'ALL_DAYS');


create type interim_advance_payment.iap_month
as enum ('JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER');


DROP TABLE IF EXISTS interim_advance_payment.interim_advance_payment_date_of_months;
create sequence interim_advance_payment.interim_advance_payment_date_of_months_id_seq;
create table if not exists interim_advance_payment.interim_advance_payment_date_of_months
(
    id                         bigint                                       not null primary key default nextval('interim_advance_payment.interim_advance_payment_date_of_months_id_seq'),
    interim_advance_payment_id bigint                                       not null,
--         constraint interim_advance_payment_date_of_months_interim_advance_payment_
--             references interim_advance_payment.interim_advance_payments,
    month_number               interim_advance_payment.iap_month_number[]   not null,
    status                     interim_advance_payment.iap_subobject_status not null,
    create_date                timestamp with time zone                                          default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                  not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    month                      interim_advance_payment.iap_month
);
alter sequence interim_advance_payment.interim_advance_payment_date_of_months_id_seq owned by interim_advance_payment.interim_advance_payment_date_of_months.id;

create sequence if not exists product.product_terminations_id_seq increment by 1;
DROP TABLE if EXISTS product.product_terminations;
create table if not exists product.product_terminations
(
    id                    bigint                           not null primary key default nextval('product.product_terminations_id_seq'),
    product_detail_id     bigint                           not null,
    termination_id        bigint                           not null,
    status                product.product_subobject_status not null,
    create_date           timestamp with time zone         not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence product.product_termination_groups_id_seq increment by 1;
DROP TABLE if EXISTS product.product_termination_groups;
create table if not exists product.product_termination_groups
(
    id                    bigint                           not null primary key default nextval('product.product_termination_groups_id_seq'),
    product_detail_id     bigint                           not null,
    termination_group_id  bigint                           not null,
    status                product.product_subobject_status not null,
    create_date           timestamp with time zone         not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence product.product_penalties_id_seq increment by 1;
DROP TABLE if exists product.product_penalties;
create table if not exists product.product_penalties
(
    id                    bigint                           not null primary key default nextval('product.product_penalties_id_seq'),
    product_detail_id     bigint                           not null,
    penalty_id            bigint                           not null,
    status                product.product_subobject_status not null,
    create_date           timestamp with time zone         not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence product.product_penalty_groups_id_seq increment by 1;
DROP table if exists product.product_penalty_groups;
create table if not exists product.product_penalty_groups
(
    id                    bigint                           not null primary key default nextval('product.product_penalty_groups_id_seq'),
    product_detail_id     bigint                           not null,
    penalty_group_id      bigint                           not null,
    status                product.product_subobject_status not null,
    create_date           timestamp with time zone         not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
CREATE TYPE interim_advance_payment.iap_group_iap_status AS ENUM ('ACTIVE','DELETED');
DROP TABLE IF EXISTS interim_advance_payment.iap_group_iaps;
create sequence interim_advance_payment.iap_group_iaps_id_seq increment by 1;
create table if not exists interim_advance_payment.iap_group_iaps
(
    id                                      bigint                                       not null primary key default nextval('interim_advance_payment.iap_group_iaps_id_seq'),
    interim_advance_payment_id              bigint                                       not null,
    interim_advance_payment_group_detail_id bigint,
    status                                  interim_advance_payment.iap_group_iap_status not null,
    create_date                             timestamp with time zone                                          default CURRENT_TIMESTAMP not null,
    system_user_id                          varchar(50)                                  not null,
    modify_date                             timestamp with time zone,
    modify_system_user_id                   varchar(50)
);
alter sequence interim_advance_payment.iap_group_iaps_id_seq owned by interim_advance_payment.iap_group_iaps.id;
CREATE TYPE interim_advance_payment.iap_group_status AS ENUM ('ACTIVE','DELETED');
DROP TABLE IF EXISTS interim_advance_payment.interim_advance_payment_groups;
create sequence interim_advance_payment.interim_advance_payment_groups_id_seq increment by 1;
create table if not exists interim_advance_payment.interim_advance_payment_groups
(
    id                    bigint                                   not null primary key default nextval('interim_advance_payment.interim_advance_payment_groups_id_seq'),
    status                interim_advance_payment.iap_group_status not null,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence interim_advance_payment.interim_advance_payment_groups_id_seq owned by interim_advance_payment.interim_advance_payment_groups.id;
DROP TABLE IF EXISTS interim_advance_payment.interim_advance_payment_group_details;
create sequence interim_advance_payment.interim_advance_payment_group_details_id_seq increment by 1;
create table if not exists interim_advance_payment.interim_advance_payment_group_details
(
    id                               bigint                                             not null primary key default nextval('interim_advance_payment.interim_advance_payment_group_details_id_seq'),
    name                             varchar                                            not null,
    start_date                       timestamp with time zone default CURRENT_TIMESTAMP not null,
    end_date                         date,
    interim_advance_payment_group_id bigint                                             not null,
    version_id                       integer                                            not null,
    create_date                      timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                   varchar(50)                                        not null,
    modify_date                      timestamp with time zone,
    modify_system_user_id            varchar(50)
);
alter sequence interim_advance_payment.interim_advance_payment_group_details_id_seq owned by interim_advance_payment.interim_advance_payment_group_details.id;

create sequence product.product_interim_advance_payments_id_seq increment by 1;
DROP table if exists product.product_interim_advance_payments;
create table if not exists product.product_interim_advance_payments
(
    id                         bigint                           not null primary key default nextval('product.product_interim_advance_payments_id_seq'),
    product_detail_id          bigint                           not null,
    interim_advance_payment_id bigint                           not null,
    status                     product.product_subobject_status not null,
    create_date                timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                      not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50)
);

create sequence product.product_interim_advance_payment_groups_id_seq increment by 1;
DROP table if exists product.product_interim_advance_payment_groups;
create table if not exists product.product_interim_advance_payment_groups
(
    id                               bigint                           not null primary key default nextval('product.product_interim_advance_payment_groups_id_seq'),
    product_detail_id                bigint                           not null,
    interim_advance_payment_group_id bigint                           not null,
    status                           product.product_subobject_status not null,
    create_date                      timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id                   varchar(50)                      not null,
    modify_date                      timestamp with time zone,
    modify_system_user_id            varchar(50)
);

create sequence product.product_price_components_id_seq increment by 1;
DROP TABLE if exists product.product_price_components;
create table if not exists product.product_price_components
(
    id                    bigint                           not null primary key default nextval('product.product_price_components_id_seq'),
    product_detail_id     bigint                           not null,
    price_component_id    bigint                           not null,
    status                product.product_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence product.product_price_component_groups_id_seq increment by 1;
drop table if exists product.product_price_component_groups;
create table if not exists product.product_price_component_groups
(
    id                       bigint                           not null primary key default nextval('product.product_price_component_groups_id_seq'),
    product_detail_id        bigint                           not null,
    price_component_group_id bigint                           not null,
    status                   product.product_subobject_status not null,
    create_date              timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                      not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50)
);

create type product.product_allows_sales_under as enum ('CONCLUDED_CONTRACT', 'ACTIVATED_CONTRACT');
create type product.product_obligatory as enum ('OBLIGATORY_CONDITION', 'AT_LEAST_ONE_CONDITION');

create sequence if not exists product.product_linked_products_id_seq increment by 1;

drop table if exists product.product_linked_products;
create table if not exists product.product_linked_products
(
    id                    bigint                             not null primary key default nextval('product.product_linked_products_id_seq'),
    obligatory            product.product_obligatory         not null,
    product_detail_id     bigint                             not null,
    linked_product_id     bigint                             not null,
    allows_sales_under    product.product_allows_sales_under not null,
    status                product.product_subobject_status   not null,
    create_date           timestamp with time zone                                default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists product.product_linked_services_id_seq increment by 1;

drop table if exists product.product_linked_services;
create table if not exists product.product_linked_services
(
    id                    bigint                             not null primary key default nextval('product.product_linked_services_id_seq'),
    obligatory            product.product_obligatory         not null,
    product_detail_id     bigint                             not null,
    linked_service_id     bigint                             not null,
    allows_sales_under    product.product_allows_sales_under not null,
    status                product.product_subobject_status   not null,
    create_date           timestamp with time zone                                default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create schema if not exists service;
create type service.service_status as enum ('ACTIVE', 'DELETED');
create type service.service_file_status as enum ('DRAFT', 'SIGNED');

DROP TABLE IF EXISTS service.services;
create sequence service.services_id_seq increment by 1;
create table if not exists service.services
(
    id                     bigint                 not null primary key default nextval('service.services_id_seq'),
    status                 service.service_status not null,
    create_date            timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)            not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50),
    last_service_detail_id bigint,
    customer_identifier    varchar(2048)
);
alter sequence service.services_id_seq owned by service.services.id;

create type price_component.pc_formula_variable as enum ('X1', 'X2', 'X3', 'X4', 'X5', 'X6', 'X7', 'X8');
create sequence price_component.price_component_formula_variables_id_seq;
create table price_component.price_component_formula_variables
(
    id                       bigint generated by default as identity
        constraint price_component_formula_variables_pk
            primary key,
    description              varchar(512),
    value                    numeric,
    value_from               numeric,
    value_to                 numeric,
    formula_variable         price_component.pc_formula_variable                not null,
    price_component_id       bigint                                             not null,
--         constraint price_component_formula_variables_price_component_fk
--             references price_component.price_components,
    create_date              timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                                        not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50),
    profile_for_balancing_id bigint
);

create table if not exists product.product_price_components
(
    id                    bigint generated by default as identity
        constraint product_price_components_pk
            primary key,
    product_detail_id     bigint                           not null,
--         constraint product_price_components_product_detail_fk
--             references product.product_details,
    price_component_id    bigint                           not null,
--         constraint product_price_components_price_component_fk
--             references price_component.price_components,
    status                product.product_subobject_status not null,
    create_date           timestamp with time zone         not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create table if not exists product.product_price_component_groups
(
    id                       bigint generated by default as identity
        constraint product_price_component_groups_pk
            primary key,
    product_detail_id        bigint                           not null,
--         constraint product_price_component_groups_product_fk
--             references product.product_details,
    price_component_group_id bigint                           not null,
--         constraint product_price_component_groups_price_component_group_fk
--             references price_component.price_component_groups,
    status                   product.product_subobject_status not null,
    create_date              timestamp with time zone         not null,
    system_user_id           varchar(50)                      not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50)
);

-- SERVICE
create schema if not exists service;

create type service.service_voltage_level as enum ('LOW', 'MEDIUM', 'MEDIUM_DIRECT_CONNECTED', 'HIGH');
create type service.service_pod_type as enum ('CONSUMER', 'GENERATOR');
create type service.service_subobject_status as enum ('ACTIVE', 'DELETED');
create type service.service_sale_method as enum ('CONTRACT', 'ORDER');
create type service.service_provided_capacity_limit_type as enum ('FROM', 'TO');
create type service.service_pod_metering_type as enum ('SETTLEMENT_PERIOD', 'SLP');
create type service.service_periodicity as enum ('ONE_TIME', 'SUBSCRIPTION');
create type service.service_payment_method as enum ('FREE', 'PERIODICAL', 'ONE_TIME');
create type service.service_payment_guarantee as enum ('NO', 'CASH_DEPOSIT', 'BANK', 'CASH_DEPOSIT_AND_BANK');
create type service.service_obligatory as enum ('OBLIGATORY_CONDITION', 'AT_LEAST_ONE_CONDITION');
create type service.service_ineligible_payment_channels as enum ('CSC', 'CASH_DESK_IN_BANK', 'ONLINE');
create type service.service_execution_level as enum ('CUSTOMER', 'CONTRACT', 'POINT_OF_DELIVERY');
create type service.service_detail_status as enum ('ACTIVE', 'INACTIVE');
create type service.service_contract_term_type as enum ('DAY_DAYS', 'MONTH_MONTHS', 'YEAR_YEARS');
create type service.service_contract_renewal_period_type as enum ('DAY_DAYS', 'MONTH_MONTHS', 'YEAR_YEARS');
create type service.service_contract_term_period_type as enum ('PERIOD', 'CERTAIN_DATE', 'WITHOUT_TERM', 'OTHER');
create type service.service_consumption_purpose as enum ('HOUSEHOLD', 'NON_HOUSEHOLD');
create type service.service_allows_sales_under as enum ('CONCLUDED_CONTRACT', 'ACTIVATED_CONTRACT');

DROP TABLE IF EXISTS service.service_details;
create sequence service.service_details_id_seq increment by 1;
create table if not exists service.service_details
(
    id                                    bigint                          not null primary key default nextval('service.service_details_id_seq'),
    name                                  varchar(1024)                   not null,
    name_transl                           varchar(1024)                   not null,
    status                                service.service_detail_status   not null,
    available_for_sale                    boolean                         not null,
    available_from                        timestamp with time zone,
    available_to                          timestamp with time zone,
    printing_name                         varchar                         not null,
    printing_name_transl                  varchar(1024)                   not null,
    short_description                     varchar(8192)                   not null,
    full_description                      varchar(32768),
    invoice_and_templates_text            varchar(2048),
    invoice_and_templates_text_transl     varchar(2048),
    service_group_id                      integer,
    other_system_connection_code          varchar(256),
    service_type_id                       integer                         not null,
    income_account_number                 varchar(32),
    sale_method                           service.service_sale_method[]   not null,
    payment_guarantee                     service.service_payment_guarantee[],
    cash_deposit_amount                   numeric,
    cash_deposit_currency_id              bigint,
    bank_guarantee_amount                 numeric,
    bank_guarantee_currency_id            bigint,
    consumption_purpose                   service.service_consumption_purpose[],
    pod_metering_type                     service.service_pod_metering_type[],
    voltage_level                         service.service_voltage_level[],
    cost_center_controlling_order         varchar(32),
    vat_rate_id                           integer,
    service_unit_id                       integer,
    equal_monthly_installments_activation boolean                         not null,
    installment_number                    smallint,
    installment_number_from               smallint,
    installment_number_to                 smallint,
    amount                                numeric,
    amount_from                           numeric,
    amount_to                             numeric,
    currency_id                           integer,
    periodicity                           service.service_periodicity     not null,
    auto_subscription_renewal             boolean,
    payment_method                        service.service_payment_method  not null,
    payment_before_execution              boolean                         not null,
    execution_level                       service.service_execution_level not null,
    provided_capacity_limit_type          service.service_provided_capacity_limit_type,
    provided_capacity_limit_amount        smallint,
    ineligible_payment_channel            service.service_ineligible_payment_channels[],
    create_date                           timestamp with time zone                             default CURRENT_TIMESTAMP not null,
    system_user_id                        varchar(50)                     not null,
    modify_date                           timestamp with time zone,
    modify_system_user_id                 varchar(50),
    version_id                            integer                         not null,
    service_id                            bigint                          not null,
    global_vat_rate                       boolean,
    term_id                               bigint,
    term_group_id                         bigint,
    global_sales_area                     boolean,
    global_sales_channel                  boolean,
    global_segment                        boolean,
    additional_info1                      varchar(1024),
    additional_info2                      varchar(1024),
    additional_info3                      varchar(1024),
    additional_info4                      varchar(1024),
    additional_info5                      varchar(1024),
    additional_info6                      varchar(1024),
    additional_info7                      varchar(1024),
    additional_info8                      varchar(1024),
    additional_info9                      varchar(1024),
    additional_info10                     varchar(1024),
    global_grid_operator                  boolean,
    pod_type                              service.service_pod_type[]
);
alter sequence service.service_details_id_seq owned by service.service_details.id;

DROP TABLE IF EXISTS service.service_contract_terms;
create sequence service.service_contract_terms_id_seq increment by 1;
create table if not exists service.service_contract_terms
(
    id                        bigint                           not null primary key default nextval('service.service_contract_terms_id_seq'),
    name                      varchar(512)                     not null,
    contract_term_period_type service.service_contract_term_period_type,
    contract_term_type        service.service_contract_term_type,
    value                     integer,
    perpetuity_clause         boolean,
    auto_termination          boolean,
    create_date               timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id            varchar(50)                      not null,
    modify_date               timestamp with time zone,
    modify_system_user_id     varchar(50),
    service_details_id        bigint                           not null,
    status                    service.service_subobject_status not null,
    automatic_renewal         boolean,
    number_of_renewals        integer,
    renewal_period_value      integer,
    renewal_period_type       service.service_contract_renewal_period_type
);
alter sequence service.service_contract_terms_id_seq owned by service.service_contract_terms.id;

DROP TABLE IF EXISTS service.service_grid_operators;
create sequence service.service_grid_operators_id_seq increment by 1;
create table if not exists service.service_grid_operators
(
    id                    bigint                           not null primary key default nextval('service.service_grid_operators_id_seq'),
    service_detail_id     bigint                           not null,
    status                service.service_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    grid_operator_id      integer                          not null
);
alter sequence service.service_grid_operators_id_seq owned by service.service_grid_operators.id;

DROP TABLE IF EXISTS service.service_interim_advance_payment_groups;
create sequence service.service_interim_advance_payment_groups_id_seq increment by 1;
create table if not exists service.service_interim_advance_payment_groups
(
    id                               bigint                           not null primary key default nextval('service.service_interim_advance_payment_groups_id_seq'),
    service_detail_id                bigint                           not null,
    interim_advance_payment_group_id bigint                           not null,
    status                           service.service_subobject_status not null,
    create_date                      timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id                   varchar(50)                      not null,
    modify_date                      timestamp with time zone,
    modify_system_user_id            varchar(50)
);
alter sequence service.service_interim_advance_payment_groups_id_seq owned by service.service_interim_advance_payment_groups.id;

DROP TABLE IF EXISTS service.service_interim_advance_payments;
create sequence service.service_interim_advance_payments_id_seq increment by 1;
create table if not exists service.service_interim_advance_payments
(
    id                         bigint                           not null primary key default nextval('service.service_interim_advance_payments_id_seq'),
    service_detail_id          bigint                           not null,
    interim_advance_payment_id bigint                           not null,
    status                     service.service_subobject_status not null,
    create_date                timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                      not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50)
);
alter sequence service.service_interim_advance_payments_id_seq owned by service.service_interim_advance_payments.id;

DROP TABLE IF EXISTS service.service_linked_products;
create sequence service.service_linked_products_id_seq increment by 1;
create table if not exists service.service_linked_products
(
    id                    bigint                             not null primary key default nextval('service.service_linked_products_id_seq'),
    obligatory            service.service_obligatory         not null,
    service_detail_id     bigint                             not null,
    linked_product_id     bigint                             not null,
    allows_sales_under    service.service_allows_sales_under not null,
    status                service.service_subobject_status   not null,
    create_date           timestamp with time zone                                default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence service.service_linked_products_id_seq owned by service.service_linked_products.id;

DROP TABLE IF EXISTS service.service_linked_services;
create sequence service.service_linked_services_id_seq increment by 1;
create table if not exists service.service_linked_services
(
    id                    bigint                             not null primary key default nextval('service.service_linked_services_id_seq'),
    obligatory            service.service_obligatory         not null,
    service_detail_id     bigint                             not null,
    linked_service_id     bigint                             not null,
    allows_sales_under    service.service_allows_sales_under not null,
    status                service.service_subobject_status   not null,
    create_date           timestamp with time zone                                default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence service.service_linked_services_id_seq owned by service.service_linked_services.id;

DROP TABLE IF EXISTS service.service_penalties;
create sequence service.service_penalties_id_seq increment by 1;
create table if not exists service.service_penalties
(
    id                    bigint                           not null primary key default nextval('service.service_penalties_id_seq'),
    service_detail_id     bigint                           not null,
    penalty_id            bigint                           not null,
    status                service.service_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence service.service_penalties_id_seq owned by service.service_penalties.id;

DROP TABLE IF EXISTS service.service_penalty_groups;
create sequence service.service_penalty_groups_id_seq increment by 1;
create table if not exists service.service_penalty_groups
(
    id                    bigint                           not null primary key default nextval('service.service_penalty_groups_id_seq'),
    service_detail_id     bigint                           not null,
    penalty_group_id      bigint                           not null,
    status                service.service_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence service.service_penalty_groups_id_seq owned by service.service_penalty_groups.id;

DROP TABLE IF EXISTS service.service_price_component_groups;
create sequence service.service_price_component_groups_id_seq increment by 1;
create table if not exists service.service_price_component_groups
(
    id                       bigint                           not null primary key default nextval('service.service_price_component_groups_id_seq'),
    service_detail_id        bigint                           not null,
    price_component_group_id bigint                           not null,
    status                   service.service_subobject_status not null,
    create_date              timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                      not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50)
);
alter sequence service.service_price_component_groups_id_seq owned by service.service_price_component_groups.id;

DROP TABLE IF EXISTS service.service_price_components;
create sequence service.service_price_components_id_seq increment by 1;
create table if not exists service.service_price_components
(
    id                    bigint                           not null primary key default nextval('service.service_price_components_id_seq'),
    service_detail_id     bigint                           not null,
    price_component_id    bigint                           not null,
    status                service.service_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence service.service_price_components_id_seq owned by service.service_price_components.id;

DROP TABLE IF EXISTS service.service_sales_areas;
create sequence service.service_sales_areas_id_seq increment by 1;
create table if not exists service.service_sales_areas
(
    id                    bigint                           not null primary key default nextval('service.service_sales_areas_id_seq'),
    service_detail_id     bigint                           not null,
    status                service.service_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    sales_area_id         integer                          not null
);
alter sequence service.service_sales_areas_id_seq owned by service.service_sales_areas.id;

DROP TABLE IF EXISTS service.service_sales_channels;
create sequence service.service_sales_channels_id_seq increment by 1;
create table if not exists service.service_sales_channels
(
    id                    bigint                           not null primary key default nextval('service.service_sales_channels_id_seq'),
    service_detail_id     bigint                           not null,
    status                service.service_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    sales_channel_id      integer                          not null
);
alter sequence service.service_sales_channels_id_seq owned by service.service_sales_channels.id;

DROP TABLE IF EXISTS service.service_segments;
create sequence service.service_segments_id_seq increment by 1;
create table if not exists service.service_segments
(
    id                    bigint                           not null primary key default nextval('service.service_segments_id_seq'),
    service_detail_id     bigint                           not null,
    status                service.service_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    segment_id            integer                          not null
);
alter sequence service.service_segments_id_seq owned by service.service_segments.id;

DROP TABLE IF EXISTS service.service_termination_groups;
create sequence service.service_termination_groups_id_seq increment by 1;
create table if not exists service.service_termination_groups
(
    id                    bigint                           not null primary key default nextval('service.service_termination_groups_id_seq'),
    service_detail_id     bigint                           not null,
    termination_group_id  bigint                           not null,
    status                service.service_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence service.service_termination_groups_id_seq owned by service.service_termination_groups.id;

DROP TABLE IF EXISTS service.service_terminations;
create sequence service.service_terminations_id_seq increment by 1;
create table if not exists service.service_terminations
(
    id                    bigint                           not null primary key default nextval('service.service_terminations_id_seq'),
    service_detail_id     bigint                           not null,
    termination_id        bigint                           not null,
    status                service.service_subobject_status not null,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence service.service_terminations_id_seq owned by service.service_terminations.id;
create type price_component.am_model_type as enum ('PRICE_AM_FOR_VOLUMES', 'PRICE_AM_OVERTIME', 'PRICE_AM_PER_PIECE');


create type price_component.am_application_type as enum ('BY_SETTLEMENT_PERIODS', 'BY_SCALES', 'PERIODICALLY', 'ONE_TIME');


create type price_component.am_application_level as enum ('POD', 'CONTRACT');


create type price_component.am_over_time_one_time_type as enum ('UPON_SIGNING_CONTRACT', 'TOGETHER_WITH_FIRST_INVOICE', 'ON_TERMINATION_OF_CONTRACT');


create type price_component.am_periodicity as enum ('DAY_OF_WEEK_AND_PERIOD_OF_YEAR', 'DAY_OF_MONTH', 'RRULE_FORMULA');


create type price_component.am_month as enum ('JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER');


create type price_component.am_day as enum ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY', 'ALL_DAYS');


create type price_component.am_week as enum ('FIRST_WEEK', 'SECOND_WEEK', 'THIRD_WEEK', 'FOURTH_WEEK', 'LAST_WEEK');


create type price_component.am_month_number as enum ('ONE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEEN', 'ELEVEN', 'TWELVE', 'THIRTEEN', 'FOURTEEN', 'FIFTEEN', 'SIXTEEN', 'SEVENTEEN', 'EIGHTTEEN', 'NINETEEN', 'TWENTY', 'TWENTYONE', 'TWENTYTWO', 'TWENTYTHREE', 'TWENTYFOUR', 'TWENTYFIVE', 'TWENTYSIX', 'TWENTYSEVEN', 'TWENTYEIGHT', 'TWENTYNINE', 'THIRTY', 'THIRTYONE', 'ALL_DAYS');


create type price_component.am_status as enum ('ACTIVE', 'DELETED');


create type price_component.am_for_volumes_by_settlement_period_profile as enum ('METERED_VOLUMES', 'CONTRACTED_VOLUMES', 'REQUESTED_VOLUMES', 'EXCESS_VOLUMES', 'SHORTAGE_VOLUMES', 'IMBALANCES_VOLUMES', 'EXTERNAL_VOLUMES');


create type price_component.am_settlement_period_minute_range as enum ('ZERO_FIFTEEN', 'SIXTEEN_THIRTY', 'THIRTYONE_FORTYFIVE', 'FORTYSIX_SIXTY');


create type price_component.am_settlement_period_hours as enum ('ALL_HOURS', 'ONE', 'TWO', 'THREE', 'THREE_ADDITIONAL', 'FOUR', 'FOUR_ADDITIONAL', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEEN', 'ELEVEN', 'TWELVE', 'THIRTEEN', 'FOURTEEN', 'FIFTEEN', 'SIXTEEN', 'SEVENTEEN', 'EIGHTEEN', 'NINETEEN', 'TWENTY');
create type price_component.time_zones as enum ('CET', 'EET');

create type price_component.am_subobject_status as enum ('ACTIVE', 'DELETED');


create sequence price_component.application_models_id_seq;

create sequence price_component.application_model_per_piece_ranges_id_seq;

create sequence price_component.application_model_over_time_one_time_id_seq;

create sequence price_component.am_over_time_periodically_id_seq;

create sequence price_component.am_over_time_periodically_day_week_period_year_id_seq;

create sequence price_component.am_over_time_periodically_date_of_months_id_seq;

create sequence price_component.am_over_time_periodically_issuing_periods_id_seq;

create sequence price_component.am_for_volumes_by_scales_id_seq;

create sequence price_component.am_for_volumes_by_scale_scales_id_seq;

create sequence price_component.am_for_volumes_by_scale_issuing_periods_id_seq;

create sequence price_component.am_for_volumes_by_settlement_periods_id_seq;

create sequence price_component.am_for_volumes_by_settlement_period_issuing_periods_id_seq;

create sequence price_component.am_for_volumes_by_settlement_periods_day_week_period_yea_id_seq;

create sequence price_component.am_for_volumes_by_settlement_period_date_of_months_id_seq;

create sequence price_component.am_for_volumes_by_settlement_period_profiles_id_seq;

create sequence price_component.am_settlement_periods_id_seq;

create sequence price_component.am_for_volume_by_scale_kwh_restriction_ranges_id_seq;

create sequence price_component.am_for_volumes_by_scale_ccy_restriction_ranges_id_seq;

create sequence price_component.am_for_volumes_by_settlement_period_ccy_restriction_rang_id_seq;

create sequence price_component.am_for_volumes_by_settlement_period_kwh_restriction_rang_id_seq;


create table if not exists price_component.application_models
(
    id                     bigint generated by default as identity
        constraint application_models_pk
            primary key,
    application_model_type price_component.am_model_type                      not null,
    application_type       price_component.am_application_type,
    application_level      price_component.am_application_level,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                                        not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50),
    status                 price_component.am_status                          not null,
    price_component_id     bigint
--         constraint application_models_price_components_id_fk
--             references price_component.price_components
);

create table if not exists price_component.am_per_piece_ranges
(
    id                    bigint generated by default as identity
        constraint application_model_per_piece_ranges_pk
            primary key,
    value_from            integer                                            not null,
    value_to              integer                                            not null,
    application_model_id  bigint                                             not null,
--         constraint am_per_piece_ranges_application_model_fk
--             references price_component.application_models,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                price_component.am_subobject_status                not null
);

create table if not exists price_component.am_over_time_one_times
(
    id                    bigint generated by default as identity
        constraint application_model_over_time_one_time_pk
            primary key,
    type                  price_component.am_over_time_one_time_type         not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    application_model_id  bigint                                             not null,
--         constraint am_over_time_one_times_application_model_fk
--             references price_component.application_models,
    status                price_component.am_subobject_status                not null
);

create table if not exists price_component.am_over_time_periodically
(
    id                    bigint generated by default as identity
        constraint am_over_time_periodically_pk
            primary key,
    periodicity           price_component.am_periodicity                     not null,
    year_round            boolean,
    rrule_formula         varchar(2048),
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    application_model_id  bigint                                             not null,
--         constraint am_over_time_periodically_application_model_fk
--             references price_component.application_models,
    status                price_component.am_subobject_status                not null
);


create table if not exists price_component.am_over_time_periodically_day_week_period_year
(
    id                           bigint generated by default as identity
        constraint price_component_day_week_period_year_pk
            primary key,
    am_over_time_periodically_id bigint                                             not null,
--         constraint am_over_time_periodically_day_week_period_year_over_time_period
--             references price_component.am_over_time_periodically,
    week                         price_component.am_week                            not null,
    create_date                  timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id               varchar(50)                                        not null,
    modify_date                  timestamp with time zone,
    modify_system_user_id        varchar(50),
    day                          price_component.am_day[],
    status                       price_component.am_subobject_status                not null
);

create table if not exists price_component.am_over_time_periodically_date_of_months
(
    id                           bigint generated by default as identity
        constraint am_over_time_periodically_date_of_months_pk
            primary key,
    am_over_time_periodically_id bigint                                             not null,
--         constraint am_over_time_periodically_date_of_months_over_time_periodically
--             references price_component.am_over_time_periodically,
    month_number                 price_component.am_month_number[]                  not null,
    create_date                  timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id               varchar(50)                                        not null,
    modify_date                  timestamp with time zone,
    modify_system_user_id        varchar(50),
    month                        price_component.am_month,
    status                       price_component.am_subobject_status                not null
);



create table if not exists price_component.am_over_time_periodically_issuing_periods
(
    id                           bigint generated by default as identity
        constraint am_over_time_periodically_issuing_periods_pk
            primary key,
    period_from                  varchar(5)                                         not null,
    period_to                    varchar(5)                                         not null,
    am_over_time_periodically_id bigint                                             not null,
--         constraint am_over_time_periodically_issuing_periods_over_time_periodicall
--             references price_component.am_over_time_periodically,
    create_date                  timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id               varchar(50)                                        not null,
    modify_date                  timestamp with time zone,
    modify_system_user_id        varchar(50),
    status                       price_component.am_subobject_status                not null
);

create table if not exists price_component.am_for_volumes_by_scales
(
    id                                                 bigint generated by default as identity
        constraint am_for_volumes_by_scales_pk
            primary key,
    restriction_of_application_based_on_volume         boolean                                            not null,
    restriction_of_application_based_on_values         boolean                                            not null,
    year_round                                         boolean                                            not null,
    create_date                                        timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                                     varchar(50)                                        not null,
    modify_date                                        timestamp with time zone,
    modify_system_user_id                              varchar(50),
    application_model_id                               bigint                                             not null,
--         constraint am_for_volumes_by_scales_application_model_fk
--             references price_component.application_models,
    status                                             price_component.am_subobject_status                not null,
    restriction_of_application_based_on_volume_percent integer
);

create table if not exists price_component.am_for_volumes_by_scale_scales
(
    id                         bigint generated by default as identity
        constraint am_for_volumes_by_scale_scales_pk
            primary key,
    scale_id                   integer                                            not null,
--         constraint am_for_volumes_by_scale_scales_scale_id_fk
--             references nomenclature.scales,
    am_for_volumes_by_scale_id bigint                                             not null,
--         constraint am_for_volumes_by_scale_scales_am_for_volumes_by_scale_fk
--             references price_component.am_for_volumes_by_scales,
    create_date                timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                        not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    status                     price_component.am_subobject_status                not null
);

create table if not exists price_component.am_for_volumes_by_scale_issuing_periods
(
    id                         bigint generated by default as identity
        constraint am_for_volumes_by_scale_issuing_periods_pk
            primary key,
    period_from                varchar(5)                                         not null,
    period_to                  varchar(5)                                         not null,
    am_for_volumes_by_scale_id bigint                                             not null,
--         constraint am_for_volumes_by_scale_issuing_periods_scale_id_fk
--             references price_component.am_for_volumes_by_scales,
    create_date                timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                        not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    status                     price_component.am_subobject_status                not null
);

create table if not exists price_component.am_for_volumes_by_settlement_periods
(
    id                                                 bigint generated by default as identity
        constraint am_for_volumes_by_settlement_period_pk
            primary key,
    periodicity                                        price_component.am_periodicity                     not null,
    time_zone                                          price_component.time_zones                         not null,
    rrule_formula                                      varchar(2048),
    year_round                                         boolean                                            not null,
    create_date                                        timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                                     varchar(50)                                        not null,
    modify_date                                        timestamp with time zone,
    modify_system_user_id                              varchar(50),
    application_model_id                               bigint                                             not null,
--         constraint am_for_volumes_by_settlement_periods_application_model_fk
--             references price_component.application_models,
    restriction_of_application_based_on_volume         boolean                                            not null,
    restriction_of_application_based_on_values         boolean,
    status                                             price_component.am_subobject_status                not null,
    restriction_of_application_based_on_volume_percent integer
);

create table if not exists price_component.am_for_volumes_by_settlement_period_issuing_periods
(
    id                                     bigint generated by default as identity
        constraint am_for_volumes_by_settlement_period_issuing_period_pk
            primary key,
    period_from                            varchar(5)                                         not null,
    period_to                              varchar(5)                                         not null,
    am_for_volumes_by_settlement_period_id bigint                                             not null,
--         constraint am_for_volumes_by_settlement_period_issuing_period_settlement_p
--             references price_component.am_for_volumes_by_settlement_periods,
    create_date                            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                                        not null,
    modify_date                            timestamp with time zone,
    modify_system_user_id                  varchar(50),
    status                                 price_component.am_subobject_status                not null
);

create table if not exists price_component.am_for_volumes_by_settlement_periods_day_week_period_year
(
    id                                     bigint generated by default as identity
        constraint am_for_volumes_by_settlement_periods_day_week_period_year_pk
            primary key,
    am_for_volumes_by_settlement_period_id bigint                                             not null,
--         constraint am_for_volumes_by_settlement_periods_day_week_period_year_settl
--             references price_component.am_for_volumes_by_settlement_periods,
    week                                   price_component.am_week                            not null,
    create_date                            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                                        not null,
    modify_date                            timestamp with time zone,
    modify_system_user_id                  varchar(50),
    day                                    price_component.am_day[],
    status                                 price_component.am_subobject_status                not null
);

create table if not exists price_component.am_for_volumes_by_settlement_period_date_of_months
(
    id                                     bigint generated by default as identity
        constraint am_for_volumes_by_settlement_period_date_of_months_pk
            primary key,
    am_for_volumes_by_settlement_period_id bigint                                             not null,
--         constraint am_for_volumes_by_settlement_period_date_of_months_settlement_p
--             references price_component.am_for_volumes_by_settlement_periods,
    month_number                           price_component.am_month_number[]                  not null,
    create_date                            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                                        not null,
    modify_date                            timestamp with time zone,
    modify_system_user_id                  varchar(50),
    month                                  price_component.am_month,
    status                                 price_component.am_subobject_status                not null
);

create table if not exists price_component.am_for_volumes_by_settlement_period_profiles
(
    id                                     bigint generated by default as identity
        constraint am_for_volumes_by_settlement_period_profiles_pk
            primary key,
    percentage                             numeric                                            not null,
    am_for_volumes_by_settlement_period_id bigint                                             not null,
--         constraint am_for_volumes_by_settlement_period_profiles_settlement_period_
--             references price_component.am_for_volumes_by_settlement_periods,
    create_date                            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                                        not null,
    modify_date                            timestamp with time zone,
    modify_system_user_id                  varchar(50),
    status                                 price_component.am_subobject_status                not null,
    profile_id                             integer
--         constraint am_for_volumes_by_settlement_period_profile_fk
--             references nomenclature.profiles
);

create table if not exists price_component.am_settlement_periods
(
    id                                     bigint generated by default as identity
        constraint am_settlement_periods_pk
            primary key,
    am_for_volumes_by_settlement_period_id bigint,
--         constraint am_settlement_periods_am_by_settlement_period_fk
--             references price_component.am_for_volumes_by_settlement_periods,
    minute_range                           price_component.am_settlement_period_minute_range  not null,
    hours                                  price_component.am_settlement_period_hours[]       not null,
    create_date                            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                                        not null,
    modify_date                            varchar,
    modify_system_user_id                  varchar(50),
    status                                 price_component.am_subobject_status                not null
);

create table if not exists price_component.am_for_volumes_by_scale_kwh_restriction_ranges
(
    id                         bigint generated by default as identity
        constraint am_for_volume_by_scale_kwh_restriction_ranges_pk
            primary key,
    value_from                 integer,
    value_to                   integer                                            not null,
    am_for_volumes_by_scale_id bigint                                             not null,
--         constraint am_for_volumes_by_scale_kwh_restriction_ranges_scale_id_fk
--             references price_component.am_for_volumes_by_scales,
    create_date                timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                        not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    status                     price_component.am_subobject_status                not null
);

create table if not exists price_component.am_for_volumes_by_scale_ccy_restriction_ranges
(
    id                         bigint generated by default as identity
        constraint am_for_volumes_by_scale_ccy_restriction_ranges_pk
            primary key,
    value_from                 integer,
    value_to                   integer                                            not null,
    am_for_volumes_by_scale_id bigint                                             not null,
--         constraint am_for_volumes_by_scale_ccy_restriction_scale_fk
--             references price_component.am_for_volumes_by_scales,
    create_date                timestamp with time zone default CURRENT_TIMESTAMP not null,
    currency_id                integer                                            not null,
    system_user_id             varchar(50)                                        not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    status                     price_component.am_subobject_status                not null
);

create table if not exists price_component.am_for_volumes_by_settlement_period_ccy_restriction_ranges
(
    id                                     bigint generated by default as identity
        constraint am_for_volumes_by_settlement_period_ccy_restriction_ranges_pk
            primary key,
    value_from                             integer                                            not null,
    value_to                               integer                                            not null,
    am_for_volumes_by_settlement_period_id bigint                                             not null,
--         constraint am_for_volumes_by_settlement_period_ccy_restriction_ranges_sett
--             references price_component.am_for_volumes_by_settlement_periods,
    status                                 price_component.am_subobject_status                not null,
    currency_id                            integer                                            not null,
    create_date                            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                                        not null,
    modify_date                            timestamp with time zone,
    modify_system_user_id                  varchar(50)
);

create table if not exists price_component.am_for_volumes_by_settlement_period_kwh_restriction_ranges
(
    id                                     bigint generated by default as identity
        constraint am_for_volumes_by_settlement_period_kwh_restriction_ranges_pk
            primary key,
    value_from                             integer,
    value_to                               integer                                            not null,
    am_for_volumes_by_settlement_period_id bigint                                             not null,
--         constraint am_for_volumes_by_settlement_period_kwh_restriction_ranges_sett
--             references price_component.am_for_volumes_by_settlement_periods,
    status                                 price_component.am_subobject_status                not null,
    create_date                            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                                        not null,
    modify_date                            timestamp with time zone,
    modify_system_user_id                  varchar(50)
);

create view customer.vw_customer_group_account_managers(display_name, display_name_desc, connected_group_id) as
SELECT DISTINCT string_agg(cg.display_name::text, ';'::text)
                OVER (PARTITION BY cg.connected_group_id ORDER BY cg.display_name ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS display_name,
                string_agg(cg.display_name::text, ';'::text)
                OVER (PARTITION BY cg.connected_group_id ORDER BY cg.display_name DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS display_name_desc,
                cg.connected_group_id
FROM (SELECT DISTINCT am.display_name,
                      ccg.connected_group_id
      FROM customer.customer_account_managers cam,
           customer.account_managers am,
           customer.customer_details cd,
           customer.customers c,
           customer.customer_connected_groups ccg
      WHERE cam.customer_detail_id = cd.id
        AND cam.account_manager_id = am.id
        AND ccg.customer_id = c.id
        AND c.last_customer_detail_id = cd.id
        AND cam.status = 'ACTIVE'::status_enum
        AND ccg.status = 'ACTIVE'::status_enum) cg;
create view product.vw_product_contract_terms(product_details_id, name, name_desc) as
SELECT DISTINCT pct.product_details_id,
                string_agg(pct.name::text, ','::text)
                OVER (PARTITION BY pct.product_details_id ORDER BY pct.name ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS name,
                string_agg(pct.name::text, ','::text)
                OVER (PARTITION BY pct.product_details_id ORDER BY pct.name DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS name_desc
FROM product.product_contract_terms pct
WHERE pct.status = 'ACTIVE'::product.product_subobject_status;
create table if not exists product.product_files
(
    id                    bigint generated by default as identity
        constraint product_files_pk primary key,
    name                  varchar(50)                                        not null,
    file_url              varchar(255)                                       not null,
    product_detail_id     bigint,
    status                product.product_subobject_status                   not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    file_type             varchar(50),
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    archived_file_type    varchar(50),
    file_statuses         product.product_file_status[]
);


create view product.vw_product_sales_channels(product_detail_id, name, name_desc) as
SELECT DISTINCT psc.product_detail_id,
                string_agg(sc.name::text, ','::text)
                OVER (PARTITION BY psc.product_detail_id ORDER BY sc.name ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS name,
                string_agg(sc.name::text, ','::text)
                OVER (PARTITION BY psc.product_detail_id ORDER BY sc.name DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS name_desc
FROM product.product_sales_channels psc
         JOIN nomenclature.sales_channels sc ON psc.sales_channel_id = sc.id;
create view service.vw_service_sales_channels(service_detail_id, name, name_desc) as
SELECT DISTINCT ssc.service_detail_id,
                string_agg(sc.name::text, ','::text)
                OVER (PARTITION BY ssc.service_detail_id ORDER BY sc.name ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS name,
                string_agg(sc.name::text, ','::text)
                OVER (PARTITION BY ssc.service_detail_id ORDER BY sc.name DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS name_desc
FROM service.service_sales_channels ssc
         JOIN nomenclature.sales_channels sc ON ssc.sales_channel_id = sc.id
WHERE ssc.status = 'ACTIVE'::service.service_subobject_status;

create view service.vw_service_contract_terms(service_details_id, name, name_desc) as
SELECT DISTINCT sct.service_details_id,
                string_agg(sct.name::text, ','::text)
                OVER (PARTITION BY sct.service_details_id ORDER BY sct.name ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS name,
                string_agg(sct.name::text, ','::text)
                OVER (PARTITION BY sct.service_details_id ORDER BY sct.name DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS name_desc
FROM service.service_contract_terms sct
WHERE sct.status = 'ACTIVE'::service.service_subobject_status;

create view service.vw_available_services(displayname, name, id, type) as
SELECT 'Service - '::text || sd.name::text AS displayname,
       sd.name,
       s.id,
       'SERVICE'::text                     AS type
FROM service.service_details sd
         JOIN service.services s ON s.last_service_detail_id = sd.id
WHERE s.status = 'ACTIVE'::service.service_status;

create view product.vw_available_products(displayname, name, id, type) as
SELECT 'Product - '::text || pd.name::text AS displayname,
       pd.name,
       pd.id,
       'PRODUCT'::text                     AS type
FROM product.product_details pd
         JOIN product.products p ON p.last_product_detail_id = pd.id
WHERE p.status = 'ACTIVE'::product.product_status;

create view product.vw_available_product_and_services(displayname, name, id, type) as
SELECT vw_available_products.displayname,
       vw_available_products.name,
       vw_available_products.id,
       vw_available_products.type
FROM product.vw_available_products
UNION
SELECT vw_available_services.displayname,
       vw_available_services.name,
       vw_available_services.id,
       vw_available_services.type
FROM service.vw_available_services;

create sequence if not exists service.service_files_id_seq increment by 1;

create table if not exists service.service_files
(
    id                    bigint                           NOT NULL primary key DEFAULT nextval('service.service_files_id_seq'),
    name                  varchar(50)                      not null,
    file_url              varchar(300)                     not null,
    service_detail_id     bigint,
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    file_type             varchar(50),
    file_statuses         service.service_file_status[],
    status                service.service_subobject_status not null,
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    archived_file_type    varchar(50)
);


-- BUNDLE 3

DROP TABLE IF EXISTS nomenclature.user_types;
create sequence nomenclature.user_types_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.user_types
(
    id                    integer             NOT NULL primary key DEFAULT nextval('nomenclature.user_types_id_seq'),
    name                  varchar(512)        not null,
    ordering_id           integer             not null,
    create_date           timestamp with time zone                 default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)         not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status not null,
    is_default            boolean             not null
);
alter sequence nomenclature.user_types_id_seq owned by nomenclature.user_types.id;
create sequence nomenclature.contract_version_types_id_seq;

create table if not exists nomenclature.contract_version_types
(
    id                    integer             NOT NULL DEFAULT nextval('nomenclature.contract_version_types_id_seq')
        constraint contract_version_types_pk
            primary key,
    name                  varchar(512)        not null,
    is_default            boolean             not null,
    system_user_id        varchar(50)         not null,
    status                nomenclature_status not null,
    ordering_id           integer             not null
        constraint contract_version_types_ordering_uk
            unique
                deferrable initially deferred,
    create_date           timestamp with time zone     default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    is_hard_coded         boolean
);


create sequence nomenclature.balancing_group_coordinators_id_seq increment by 1;
DROP TABLE IF EXISTS nomenclature.balancing_group_coordinators;
create table if not exists nomenclature.balancing_group_coordinators
(
    id                       integer      NOT NULL    DEFAULT nextval('nomenclature.balancing_group_coordinators_id_seq'),
    name                     varchar(512) not null,
    name_transliterated      text         null,
    full_name                varchar(512) not null,
    full_name_transliterated text         null,
    is_default               boolean      not null,
    ordering_id           integer      not null
        constraint balancing_group_coordinators_ordering_uk
            unique
                deferrable initially deferred,
    system_user_id        varchar(50)  not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    status                nomenclature_status
);
alter sequence nomenclature.balancing_group_coordinators_id_seq owned by nomenclature.balancing_group_coordinators.id;

create schema pod;
create type pod.system_source as enum ('SELF_SERVICE_PORTAL', 'PHOENIX', 'SALES_PORTAL', 'VCOK');
create type pod.pod_status as enum ('ACTIVE', 'DELETED');
create type pod.pod_consumption_purpose as enum ('HOUSEHOLD', 'NON_HOUSEHOLD');
create type pod.pod_type as enum ('CONSUMER', 'GENERATOR');
create type pod.pod_voltage_level as enum ('LOW', 'MEDIUM', 'MEDIUM_DIRECT_CONNECTED', 'HIGH');
create type pod.pod_measurement_type as enum ('SETTLEMENT_PERIOD', 'SLP');


DROP TABLE IF EXISTS pod.pod;
create sequence pod.pod_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS pod.pod
(
    id                                        integer        NOT NULL primary key DEFAULT nextval('pod.pod_id_seq'),
    identifier                                varchar(33)    not null,
    impossibility_disconnection               boolean        not null,
    blocked_for_disconnection                 boolean        not null,
    blocked_for_disconnection_date_from       date,
    blocked_for_disconnection_date_to         date,
    blocked_for_disconnection_reason          varchar(2048),
    blocked_for_disconnection_additional_info varchar(2048),
    blocked_for_billing                       boolean        not null,
    blocked_for_billing_date_from             date,
    blocked_for_billing_date_to               date,
    blocked_for_billing_reason                varchar(2048),
    blocked_for_billing_additional_info       varchar(2048),
    status                                    pod.pod_status not null,
    create_date                               timestamp with time zone            default CURRENT_TIMESTAMP not null,
    system_user_id                            varchar(50)    not null,
    modify_date                               timestamp with time zone,
    modify_system_user_id                     varchar(50),
    last_pod_detail_id                        bigint,
    grid_operator_id                          integer        not null,
    disconnected                              boolean,
    system_source_id                          pod.system_source        default 'PHOENIX'::pod.system_source
);
alter sequence pod.pod_id_seq owned by pod.pod.id;

DROP TABLE IF EXISTS pod.pod_details;
create sequence pod.pod_details_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS pod.pod_details
(
    id                                   integer                     NOT NULL primary key DEFAULT nextval('pod.pod_details_id_seq'),
    name                                 varchar(1024)               not null,
    balancing_group_coordinator_id       integer,
    type                                 pod.pod_type,
    estimated_monthly_avg_consumption    integer                     not null,
    consumption_purpose                  pod.pod_consumption_purpose not null,
    user_type_id                         integer,
    voltage_level                        pod.pod_voltage_level       not null,
    customer_identifier_by_grid_operator varchar(13),
    customer_number_by_grid_operator     varchar(16),
    measurement_type                     pod.pod_measurement_type    not null,
    provided_power                       integer,
    multiplier                           numeric,
    zip_code_id                          integer,
    street_number                        varchar(32),
    address_additional_info              varchar(512),
    block                                varchar(128),
    entrance                             varchar(32),
    floor                                varchar(16),
    apartment                            varchar(32),
    mailbox                              varchar(32),
    street_id                            integer,
    residential_area_id                  integer,
    district_id                          integer,
    region_foreign                       varchar(512),
    municipality_foreign                 varchar(512),
    populated_place_foreign              varchar(512),
    zip_code_foreign                     varchar(32),
    district_foreign                     varchar(512),
    foreign_address                      boolean                     not null,
    populated_place_id                   integer,
    country_id                           integer,
    street_foreign                       varchar(2048),
    residential_area_foreign             varchar(2048),
    foreign_street_type                  street_types,
    foreign_residential_area_type        residential_type,
    pod_id                               bigint                      not null,
    version_id                           integer                     not null,
    create_date                          timestamp with time zone                         default CURRENT_TIMESTAMP not null,
    system_user_id                       varchar(50)                 not null,
    modify_date                          timestamp with time zone,
    modify_system_user_id                varchar(50),
    additional_identifier                varchar(33),
    customer_id                          bigint,
    pod_measurement_types_id             integer,
    customer_described_name              text,
    country_trsl                         text,
    region_trsl                          text,
    municipality_trsl                    text,
    populated_place_trsl                 text,
    zip_code_trsl                        text,
    district_trsl                        text,
    residential_area_trsl                text,
    street_trsl                          text,
    street_number_trsl                   text,
    address_additional_info_trsl         text,
    block_trsl                           text,
    entrance_trsl                        text,
    floor_trsl                           text,
    apartment_trsl                       text,
    mailbox_trsl                         text,
    residental_area_type_trsl                       text,
    location                             text,
    street_type_trsl                         text,
    constraint pod_details_pod_version_uk
        unique (pod_id, version_id)
);
alter sequence pod.pod_details_id_seq owned by pod.pod_details.id;
create sequence pod.discounts_id_seq increment by 1;

create type pod.pod_discount_status as enum ('ACTIVE', 'DELETED');

create table pod.discounts
(
    id                             integer                 NOT NULL DEFAULT nextval('pod.discounts_id_seq')
        constraint discounts_pk
            primary key,
    amount_in_percent              numeric                 not null,
    amount_in_money_per_kwh        numeric                 not null,
    date_from                      date                    not null,
    date_to                        date                    not null,
    order_number                   varchar(512)            not null,
    certificate_number             varchar(512)            not null,
    currency_id                    integer                 not null,
    volume_without_discount_in_kwh integer,
    customer_id                    bigint                  not null,
    invoiced                       boolean,
    status                         pod.pod_discount_status not null,
    create_date                    timestamp with time zone         default CURRENT_TIMESTAMP not null,
    system_user_id                 varchar(50)             not null,
    modify_date                    timestamp with time zone,
    modify_system_user_id          varchar(50),
    constraint discounts_date_chk
        check (date_from <= date_to)
);

create type pod.pod_subobject_status as enum ('ACTIVE', 'DELETED');

create table pod.discount_pods
(
    id                    bigint generated by default as identity
        constraint discount_pods_pk
            primary key,
    discount_id           bigint                                             not null,
    pod_id                bigint                                             not null,
    status                pod.pod_subobject_status                           not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create type pod.pod_meter_status as enum ('ACTIVE', 'DELETED');

create sequence pod.meters_id_seq increment by 1;
DROP TABLE IF EXISTS pod.meters;
create table if not exists pod.meters
(
    id                    integer              NOT NULL DEFAULT nextval('pod.meters_id_seq'),
    grid_operator_id      integer              not null,
    installment_date      date                 not null,
    remove_date           date,
    status                pod.pod_meter_status not null,
    create_date           timestamp with time zone      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    pod_id                bigint               not null,
    number                varchar(32)
);
alter sequence pod.meters_id_seq owned by pod.meters.id;

create sequence pod.meter_scales_id_seq increment by 1;
DROP TABLE IF EXISTS pod.meter_scales;
create table if not exists pod.meter_scales
(
    id                    integer                  NOT NULL DEFAULT nextval('pod.meter_scales_id_seq'),
    meter_id              bigint                   not null,
    scale_id              integer                  not null,
    status                pod.pod_subobject_status not null,
    create_date           timestamp with time zone          default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50)
);
alter sequence pod.meter_scales_id_seq owned by pod.meter_scales.id;

create view pod.vw_discount_pods(discount_id, name, name_desc, pod_identifier) as
SELECT DISTINCT dp.discount_id,
                string_agg(((pd.name::text || '('::text) || p.id) || ')'::text, ', '::text)
                OVER (PARTITION BY dp.discount_id ORDER BY pd.name ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS name,
                string_agg(((pd.name::text || '('::text) || p.id) || ')'::text, ', '::text)
                OVER (PARTITION BY dp.discount_id ORDER BY pd.name DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS name_desc,
                string_agg(lower(p.identifier::text), ','::text)
                OVER (PARTITION BY dp.discount_id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)                       AS pod_identifier
FROM pod.discount_pods dp
         JOIN pod.pod p ON dp.pod_id = p.id
         JOIN pod.pod_details pd ON p.last_pod_detail_id = pd.id
WHERE dp.status = 'ACTIVE'::pod.pod_subobject_status
  AND p.status = 'ACTIVE'::pod.pod_status;

create type pod.billing_by_profile_period_type as enum ('FIFTEEN_MINUTES', 'ONE_HOUR', 'ONE_DAY', 'ONE_MONTH');
create type pod.billing_by_profile_status as enum ('ACTIVE', 'DELETED');
create type pod.billing_by_profile_time_zone as enum ('EET', 'CET');

create sequence pod.billing_by_profile_id_seq increment by 1;
DROP TABLE IF EXISTS pod.billing_by_profile;
create table if not exists pod.billing_by_profile
(
    id                    integer                            NOT NULL DEFAULT nextval('pod.billing_by_profile_id_seq'),
    pod_id                bigint                             not null,
    time_zone             pod.billing_by_profile_time_zone   not null,
    profile_id            integer                            not null,
    period_from           date                               not null,
    period_to             date                               not null,
    status                pod.billing_by_profile_status,
    create_date           timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    period_type           pod.billing_by_profile_period_type not null,
    invoiced              boolean
);
alter sequence pod.billing_by_profile_id_seq owned by pod.billing_by_profile.id;


create sequence pod.billing_data_by_profile_id_seq increment by 1;
DROP TABLE IF EXISTS pod.billing_data_by_profile;
create table if not exists pod.billing_data_by_profile
(
    id                    integer                                            NOT NULL DEFAULT nextval('pod.billing_data_by_profile_id_seq'),
    period_from           timestamp with time zone                           not null,
    period_to             timestamp with time zone                           not null,
    value                 numeric                                            not null,
    is_shifted_hour       boolean                                            not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_system_user_id varchar(50)                                        not null,
    billing_by_profile_id bigint                                             not null,
    invoiced              boolean                  default false,
    CONSTRAINT billing_data_by_profile_unique UNIQUE (period_from, is_shifted_hour, billing_by_profile_id)
);
alter sequence pod.billing_data_by_profile_id_seq owned by pod.billing_data_by_profile.id;


create sequence nomenclature.deactivation_purposes_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.deactivation_purposes
(
    id                    integer primary key NOT NULL DEFAULT nextval('nomenclature.deactivation_purposes_id_seq'),
    name                  varchar(512)        not null,
    is_default            boolean             not null,
    is_hard_coded         boolean             not null,
    system_user_id        varchar(50)         not null,
    status                nomenclature_status not null,
    ordering_id           integer             not null
        constraint deactivation_purposes_ordering_uk
            unique
                deferrable initially deferred,
    create_date           timestamp with time zone     default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence nomenclature.deactivation_purposes_id_seq owned by nomenclature.deactivation_purposes.id;

create sequence nomenclature.bnb_base_interest_rates_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.bnb_base_interest_rates
(
    id                    integer primary key NOT NULL DEFAULT nextval('nomenclature.bnb_base_interest_rates_id_seq'),
    name                  varchar(50)         not null,
    is_default            boolean             not null,
    system_user_id        varchar(50)         not null,
    status                nomenclature_status not null,
    percentage_rate       numeric             not null
        constraint bnb_base_interest_rates_percentage_rate_chk
            check (percentage_rate <= (100)::numeric),
    date_from             date                not null,
    ordering_id           integer             not null
        constraint bnb_base_interest_rates_ordering_uk
            unique
                deferrable initially deferred,
    create_date           timestamp with time zone     default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence nomenclature.bnb_base_interest_rates_id_seq owned by nomenclature.bnb_base_interest_rates.id;
create type nomenclature.performer_type as enum ('TAG', 'MANAGER');
create sequence nomenclature.task_types_id_seq increment by 1;
create type nomenclature.status_enum as enum ('ACTIVE', 'INACTIVE', 'DELETED');

create table if not exists nomenclature.task_types
(
    id                    integer primary key      NOT NULL DEFAULT nextval('nomenclature.task_types_id_seq'),
    name                  varchar(512)             not null,
    calendar_id           integer                  not null,
    is_default            boolean                  not null,

    system_user_id        varchar(50)              not null,
    status                nomenclature.status_enum not null,

    ordering_id           integer                  not null
        constraint task_types_ordering_uk
            unique
                deferrable initially deferred,
    allow_additional_stages boolean DEFAULT FALSE NOT NULL,
    create_date           timestamp with time zone          default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create type nomenclature.task_type_term_type as enum ('CALENDAR_DAYS', 'WORKING_DAYS');
create sequence nomenclature.task_type_stages_id_seq increment by 1;

create table if not exists nomenclature.task_type_stages
(
    id                    integer primary key              NOT NULL DEFAULT nextval('nomenclature.task_type_stages_id_seq'),
    performer             varchar(2048),
    term                  integer,
    term_type             nomenclature.task_type_term_type not null,
    system_user_id        varchar(50)                      not null,
    performer_group       bigint,
    performer_type        nomenclature.performer_type,
    status                nomenclature.status_enum         not null,
    create_date           timestamp with time zone                  default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    task_type_id          integer                          not null,
    stage                 integer,
    is_mandatory          boolean DEFAULT FALSE NOT NULL,
    constraint task_type_stages_task_type_stage_uk
        unique (task_type_id, stage)
);

create type pod.billing_by_scale_status as enum ('ACTIVE', 'DELETED');

create sequence pod.billing_by_scale_id_seq increment by 1;
DROP TABLE IF EXISTS pod.billing_by_scale;
create table if not exists pod.billing_by_scale
(
    id                        bigint generated by default as identity
        constraint billing_by_scale_pk
            primary key,
    pod_id                    bigint                                             not null,
    date_from                 date                                               not null,
    date_to                   date                                               not null,
    billing_power_in_kw       bigint                                             not null,
    invoice_number            varchar(32)                                        not null,
    invoice_date              date                                               not null,
    invoice_correction        varchar(32),
    correction                boolean                                            not null,
    override                  boolean                                            not null,
    basis_for_issuing_invoice varchar(512),
    status                    pod.billing_by_scale_status,

    create_date               timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id            varchar(50)                                        not null,
    modify_date               timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_system_user_id     varchar(50)                                        not null,
    invoiced                  boolean
);


create sequence pod.billing_data_by_scale_id_seq increment by 1;
DROP TABLE IF EXISTS pod.billing_data_by_scale;
create table if not exists pod.billing_data_by_scale
(
    id                    bigint generated by default as identity
        constraint billing_data_by_scale_pk
            primary key,
    period_from           date                                               not null,
    period_to             date                                               not null,
    meter_id              bigint,
    scale_id              integer,
    time_zone             varchar(256),
    new_meter_reading     numeric,
    old_meter_reading     numeric,
    difference_kwh        numeric,
    multiplier            numeric,
    correction_kwh        numeric,
    deducted_kwh          numeric,
    total_volumes_kwh     numeric,
    volumes               numeric,
    unit_price            numeric,
    total_value           numeric,
    billing_by_scale_id   bigint                                             not null
        constraint billing_data_by_scale_billing_fk
            references pod.billing_by_scale,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    scale_number          varchar(9),
    index                 integer,
    system_source_id      pod.system_source,
    constraint billing_data_by_scale_billing_index_uk
        unique (billing_by_scale_id, index),
    constraint billing_data_by_scale_date_chk
        check (period_from <= period_to)
);

create schema interest_rate;

create type interest_rate.interest_rate_type as enum ('YEARLY', 'DAILY');
create type interest_rate.interest_rate_charging as enum ('OVERDUE_LIABILITY', 'TOTAL_LIABILITY_ACCORDING_CONTRACT_ORDER');
create type interest_rate.interest_rate_periodicity as enum ('DAYS', 'MONTHS');
create type interest_rate.interest_rate_status as enum ('ACTIVE', 'DELETED');

create sequence interest_rate.interest_ratess_id_seq increment by 1;
DROP TABLE IF EXISTS interest_rate.interest_rates;
create table if not exists interest_rate.interest_rates
(
    id                                                 integer                              NOT NULL DEFAULT nextval('interest_rate.interest_ratess_id_seq')
        constraint interest_rate_pk
            primary key,
    name                                               varchar(512)                         not null,
    is_default                                         boolean                              not null,
    type                                               interest_rate.interest_rate_type     not null,
    interest_charging                                  interest_rate.interest_rate_charging not null,
    min_amount_for_interest_charging                   numeric,
    min_amount_of_an_interest                          numeric,
    max_amount_of_an_interest                          numeric,
    currency_id                                        integer                              not null,
    min_amount_of_interest_in_percent_of_the_liability numeric,
    max_amount_of_interest_in_percent_of_the_liability numeric,
    grace_period                                       bigint,
    periodicity                                        interest_rate.interest_rate_periodicity,
    grouping_of_the_interest                           boolean                              not null,
    income_account_number                              varchar(32)                          not null,
    cost_center_controlling_order                      varchar(32)                          not null,
    status                                             interest_rate.interest_rate_status   not null,
    create_date                                        timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id                                     varchar(50)                          not null,
    modify_date                                        timestamp with time zone,
    modify_system_user_id                              varchar(50)
);

create type interest_rate.interest_rate_calendar_type as enum ('WORKING_DAYS', 'CALENDAR_DAYS', 'CERTAIN_DAYS');
create type interest_rate.interest_rate_due_date_change as enum ('PREVIOUS_WORKING_DAY', 'NEXT_WORKING_DAY');
create type interest_rate.interest_rate_subobject_status as enum ('ACTIVE', 'DELETED');
create type interest_rate.interest_rate_exclude as enum (
    'WEEKENDS',
    'HOLIDAYS'
    );

create sequence interest_rate.interest_rate_payment_terms_id_seq increment by 1;
DROP TABLE IF EXISTS interest_rate.interest_rate_payment_terms;
create table if not exists interest_rate.interest_rate_payment_terms
(
    id                    integer                                      NOT NULL DEFAULT nextval('interest_rate.interest_rate_payment_terms_id_seq'),
    type                  interest_rate.interest_rate_calendar_type    not null,
    value                 integer,
    value_from            integer,
    value_to              integer,
    calendar_id           integer                                      not null,
    due_date_change       interest_rate.interest_rate_due_date_change,
    name                  varchar(1024)                                not null,
    interest_rate_id      bigint                                       not null,
    status                interest_rate.interest_rate_subobject_status not null,
    excludes              interest_rate.interest_rate_exclude[],
    create_date           timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                  not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create type interest_rate.interest_rate_and_period_status as enum ('ACTIVE', 'DELETED');

create sequence interest_rate.interest_rate_rates_and_periods_id_seq increment by 1;
DROP TABLE IF EXISTS interest_rate.interest_rates_and_periods;
create table if not exists interest_rate.interest_rates_and_periods
(
    id                       integer                                       NOT NULL DEFAULT nextval('interest_rate.interest_rate_rates_and_periods_id_seq'),
    amount_in_percent        numeric                                       not null,
    bir                      boolean,
    fee                      integer,
    currency_id              integer                                       not null,
    valid_from               date                                          not null,
    valid_to                 date,
    status                   interest_rate.interest_rate_and_period_status not null,
    create_date              timestamp with time zone                               default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                                   not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50),
    interest_rate_id         bigint                                        not null,
    base_interest_rate       numeric,
    applicable_interest_rate numeric
);
create schema if not exists task;

create type task.task_status as enum ('ACTIVE', 'DELETED');
create type task.task_current_status as enum ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'TERMINATED', 'OVERDUE');
create type task.task_connection_type as enum ('CUSTOMER', 'CONTRACT_ORDER', 'COMMUNICATION', 'INTERNAL', 'BILLING', 'RECEIVABLE_BLOCKING', 'RECEIVABLES');
create sequence if not exists task.tasks_id_seq increment by 1;
create table if not exists task.tasks
(
    id                      bigint primary key        NOT NULL DEFAULT nextval('task.tasks_id_seq'),
    number                  bigint                    not null,
    task_type_id            integer                   not null,
    current_status          task.task_current_status  not null,
    connection_type         task.task_connection_type not null,
    description             varchar(4096),
    create_date             timestamp with time zone           default CURRENT_TIMESTAMP not null,
    system_user_id          varchar(50)               not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50),
    status                  task.task_status          not null,
    first_stage_start_date  date,
    last_stage_end_date     date,
    current_stage_performer bigint,
    last_completion_date    timestamp with time zone,
    termination_date        timestamp with time zone);

create type task.task_stage_status as enum ('OPEN', 'IN_PROGRESS', 'OVERDUE', 'COMPLETED');
create type task.task_stage_term_type as enum ('CALENDAR_DAYS', 'WORKING_DAYS');
create sequence if not exists task.task_stages_id_seq increment by 1;
create table if not exists task.task_stages
(
    id                    bigint                    NOT NULL DEFAULT nextval('task.task_stages_id_seq'),
    performer             bigint                    not null,
    term                  integer,
    term_type             task.task_stage_term_type not null,
    stage                 integer                   not null,
    task_type_stage_id    integer,
    status                task.task_stage_status    not null,
    tag_performer         bigint,
    current_performer_id  bigint,
    performer_type        nomenclature.performer_type,
    create_date           timestamp with time zone           default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)               not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    start_date            date,
    end_date              date,
    completion_date       timestamp with time zone,
    is_additional_stage   boolean DEFAULT FALSE NOT NULL,
    task_id               bigint                    not null,
    constraint task_stages_task_type_stage_id_check
        check (
            (is_additional_stage = TRUE AND task_type_stage_id IS NULL)
            OR
            (is_additional_stage = FALSE AND task_type_stage_id IS NOT NULL)
        )
);

create sequence if not exists task.task_comments_id_seq increment by 1;
create table if not exists task.task_comments
(
    id                    bigint      NOT NULL     DEFAULT nextval('task.task_stages_id_seq'),
    comment               varchar(2048),
    task_id               bigint      not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50) not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

CREATE TYPE task.file_status AS ENUM ('DRAFT', 'SIGNED');

CREATE SEQUENCE IF NOT EXISTS task.task_files_id_seq INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS task.task_files
(
    id                    bigint primary key        NOT NULL DEFAULT nextval('task.task_files_id_seq'),
    name                  varchar(255)              not null,
    file_url             varchar(2048)             not null,
    task_id              bigint                    not null,
    file_statuses        task.file_status[]        not null,
    is_manually_added    boolean                   not null default false,
    file_size            bigint,
    create_date          timestamp with time zone  default CURRENT_TIMESTAMP not null,
    system_user_id       varchar(50)               not null,
    modify_date          timestamp with time zone,
    modify_system_user_id varchar(50),
    status               task.task_status          not null
);

ALTER TABLE task.task_files
    ADD CONSTRAINT fk_task_files_task_id
        FOREIGN KEY (task_id) REFERENCES task.tasks(id);

CREATE INDEX idx_task_files_task_id ON task.task_files(task_id);

create type customer.status_enum as enum ('ACTIVE', 'DELETED');
create sequence if not exists customer.customer_tasks_id_seq;
create table if not exists customer.customer_tasks
(
    id                    bigint               NOT NULL DEFAULT nextval('customer.customer_tasks_id_seq'),

    customer_id           bigint               not null,
    task_id               bigint               not null,
    status                customer.status_enum not null,
    create_date           timestamp with time zone      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create schema if not exists product_contract;
create sequence product_contract.contract_number_seq
    maxvalue 999999
    cycle;
create sequence product_contract.contract_internal_intermediaries_id_seq;
create sequence product_contract.contract_external_intermediaries_id_seq;
create sequence product_contract.contract_assisting_employees_id_seq;
create sequence product_contract.related_contracts_id_seq;
create sequence product_contract.contract_pods_id_seq;

create type product_contract.contract_status as enum ('ACTIVE', 'DELETED');
create type product_contract.contract_file_status as enum ('DRAFT', 'SIGNED');
create type product_contract.contract_sub_status as enum ('DRAFT', 'READY', 'SIGNED_BY_CUSTOMER', 'SIGNED_BY_EPRES', 'IN_TERMINATION_BY_CUSTOMER', 'IN_TERMINATION_BY_EPRES', 'IN_TERMINATION_BY_GO_DATA', 'DELIVERY', 'FROM_CUSTOMER_WITH_NOTICE', 'FROM_CUSTOMER_WITHOUT_NOTICE', 'FROM_EPRES_WITH_NOTICE', 'FROM_EPRES_WITHOUT_NOTICE', 'BY_MUTUAL_AGREEMENT', 'NEW_CONTRACT_SIGNED', 'EXPIRED', 'DELETED_ID_DECEASED_PERSON', 'FORCE_MAJEURE', 'UNSENT_TO_CUSTOMER', 'INVALID_DATA', 'REFUSAL_TO_SIGN_BY_CUSTOMER', 'REFUSAL_TO_SIGN_BY_EPRES', 'TEST', 'CHANGED_WITH_AGREEMENT', 'SIGNED_BY_BOTH_SIDES', 'SPECIAL_PROCESSES', 'AWAITING_ACTIVATION', 'CONTRACT_IS_NOT_ACTIVE', 'IN_PROCESS');
create type product_contract.contract_contract_status as enum ('DRAFT', 'READY', 'SIGNED', 'ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY', 'TERMINATED', 'CANCELLED', 'CHANGED_WITH_AGREEMENT');
create type product_contract.contract_status_csp_delivery_status as enum ('CANCELLED', 'FAILED', 'SUCCESS');
create sequence product_contract.contracts_id_seq increment by 1;
create table if not exists product_contract.contracts
(
    id                          bigint primary key               NOT NULL DEFAULT nextval('product_contract.contracts_id_seq'),
    status                      product_contract.contract_status not null,
    create_date                 timestamp with time zone                  default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                      not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50),
    contract_number             varchar(15)                      not null,
    contract_status             product_contract.contract_contract_status,
    contract_sub_status         product_contract.contract_sub_status,
    contract_status_modify_date timestamp with time zone,
    termination_date            date,
    contract_term_end_date      date,
    activation_date             date,
    perpetuity_date             date,
    supply_activation_date      date,
    entry_into_force_date       date,
    initial_term_start_date     date,
    signing_date                date,
    is_locked                   boolean,
    resign_to_contract_id       bigint,
    contract_term_end_date_modify_date timestamp with time zone,
    system_source_id            pod.system_source                  default 'PHOENIX'::pod.system_source ,
    additional_information      varchar(4096)
--         constraint contracts_resign_to_contract_fk
--             references product_contract.contracts
);

create type product_contract.contract_subobject_status as enum ('ACTIVE', 'DELETED');
create sequence if not exists product_contract.contract_tasks_id_seq increment by 1;
create table if not exists product_contract.contract_tasks
(
    id                    bigint                                     NOT NULL DEFAULT nextval('product_contract.contract_tasks_id_seq'),
    contract_id           bigint                                     not null,
    task_id               bigint                                     not null,
    status                product_contract.contract_subobject_status not null,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create or replace view task.vw_task_stage_performers(task_id, performer, performer_desc) as
SELECT DISTINCT ts.task_id,
                string_agg(am.display_name::text, ', '::text)
                OVER (PARTITION BY ts.task_id ORDER BY am.display_name ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS performer,
                string_agg(am.display_name::text, ', '::text)
                OVER (PARTITION BY ts.task_id ORDER BY am.display_name DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS performer_desc
FROM task.task_stages ts
         LEFT JOIN customer.account_managers am ON ts.performer = am.id;
-- activity
create schema if not exists activity;
create type activity.activity_connection_type as enum ('CUSTOMER', 'PRODUCT_CONTRACT', 'SERVICE_CONTRACT', 'ORDER', 'COMMUNICATION', 'TASK');
create type activity.activity_status as enum ('ACTIVE', 'DELETED');
create type activity.file_status as enum ('DRAFT', 'SIGNED');

create sequence activity.activity_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS activity.activity
(
    id                    integer primary key      NOT NULL DEFAULT nextval('activity.activity_id_seq'),
    activity_number       bigint,
    activity_id           integer                  not null,
    sub_activity_id       integer                  not null,
    fields                jsonb,
    status                activity.activity_status not null,
    create_date           timestamp with time zone          default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    connection_type       activity.activity_connection_type
);
alter sequence activity.activity_id_seq owned by activity.activity.id;

create sequence product_contract.contract_activity_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS product_contract.contract_activity
(
    id                    integer primary key                        NOT NULL DEFAULT nextval('product_contract.contract_activity_id_seq'),
    contract_id           bigint                                     not null,
--         constraint contract_activity_contract_fk
--             references product_contract.contracts,
    activity_id           integer                                    not null,
--         constraint contract_activity_fk
--             references activity.activity,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                product_contract.contract_subobject_status not null
);
alter sequence product_contract.contract_activity_id_seq owned by product_contract.contract_activity.id;

create sequence customer.customer_activity_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS customer.customer_activity
(
    id                    integer primary key NOT NULL DEFAULT nextval('customer.customer_activity_id_seq'),
    customer_id           bigint              not null,
--         constraint customer_activity_customer_fk
--             references customer.customers,
    activity_id           integer             not null,
--         constraint customer_activity_fk
--             references activity.activity,
    create_date           timestamp with time zone     default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)         not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                status_enum         not null
);
alter sequence customer.customer_activity_id_seq owned by customer.customer_activity.id;

create type task.task_subobject_status as enum ('ACTIVE', 'DELETED');
create sequence task.task_activity_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS task.task_activity
(
    id                    integer primary key        NOT NULL DEFAULT nextval('task.task_activity_id_seq'),
    task_id               bigint                     not null,
--         constraint task_activity_task_fk
--             references task.tasks,
    activity_id           bigint                     not null,
--         constraint task_activity_fk
--             references activity.activity,
    status                task.task_subobject_status not null,
    create_date           timestamp with time zone            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence task.task_activity_id_seq owned by task.task_activity.id;

create schema if not exists product_contract;
create sequence product_contract.contract_files_id_seq increment by 1;
create table if not exists product_contract.contract_files
(
    id                    integer primary key                        NOT NULL DEFAULT nextval('product_contract.contract_files_id_seq'),
    name                  varchar(100)                               not null,
    file_url              varchar(256)                               not null,
    contract_detail_id    bigint,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                product_contract.contract_subobject_status not null,
    file_statuses         product_contract.contract_file_status[],
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    archived_file_type    varchar(50)
);

create sequence product_contract.contract_additional_docs_id_seq increment by 1;
create table if not exists product_contract.contract_additional_docs
(
    id                    integer primary key                        NOT NULL DEFAULT nextval('product_contract.contract_additional_docs_id_seq'),
    name                  varchar(100)                               not null,
    file_url              varchar(256)                               not null,
    contract_detail_id    bigint,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                product_contract.contract_subobject_status not null,
    file_statuses         product_contract.contract_file_status[],
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    archived_file_type    varchar(50)
);

create schema service_contract;

create sequence service_contract.contracts_id_seq increment by 1;
create sequence service_contract.contract_number_seq increment by 1;
create type service_contract.contract_status as enum ('ACTIVE', 'DELETED');
create type service_contract.contract_file_status as enum ('DRAFT', 'SIGNED');
create type service_contract.contract_contract_status as enum ('DRAFT', 'READY', 'SIGNED', 'ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY', 'TERMINATED', 'CANCELLED', 'CHANGED_WITH_AGREEMENT');
create type service_contract.contract_sub_status as enum ('DRAFT', 'READY', 'SIGNED_BY_CUSTOMER', 'SIGNED_BY_EPRES', 'IN_TERMINATION_BY_CUSTOMER', 'IN_TERMINATION_BY_EPRES', 'IN_TERMINATION_BY_GO_DATA', 'DELIVERY', 'FROM_CUSTOMER_WITH_NOTICE', 'FROM_CUSTOMER_WITHOUT_NOTICE', 'FROM_EPRES_WITH_NOTICE', 'FROM_EPRES_WITHOUT_NOTICE', 'BY_MUTUAL_AGREEMENT', 'NEW_CONTRACT_SIGNED', 'EXPIRED', 'DELETED_ID_DECEASED_PERSON', 'FORCE_MAJEURE', 'UNSENT_TO_CUSTOMER', 'INVALID_DATA', 'REFUSAL_TO_SIGN_BY_CUSTOMER', 'REFUSAL_TO_SIGN_BY_EPRES', 'TEST', 'CHANGED_WITH_AGREEMENT', 'SIGNED_BY_BOTH_SIDES', 'SPECIAL_PROCESSES', 'AWAITING_ACTIVATION', 'IN_PROCESS');
create table service_contract.contracts
(
    id                          integer primary key              NOT NULL DEFAULT nextval('service_contract.contracts_id_seq'),
    status                      service_contract.contract_status not null,
    create_date                 timestamp with time zone                  default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                      not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50),
    contract_number             varchar(15)                      not null,
    contract_status             service_contract.contract_contract_status,
    termination_date            date,
    signing_date                date,
    entry_into_force_date       date,
    contract_term_end_date      date,
    perpetuity_date             date,
    initial_term_start_date     date,
    contract_sub_status         service_contract.contract_sub_status,
    contract_status_modify_date timestamp with time zone,
    activity_id                 bigint,
    is_locked                   boolean,
    additional_information      text
);

create type service_contract.contract_detail_status as enum ('DRAFT', 'READY', 'SIGNED', 'ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY', 'TERMINATED', 'CANCELLED', 'CHANGED_WITH_AGREEMENT');
create type service_contract.contract_types as enum ('CONTRACT', 'ADDITIONAL_AGREEMENT', 'EX_OFFICIO_AGREEMENT');
create type service_contract.contract_detail_sub_status as enum ('DRAFT', 'READY', 'SIGNED_BY_CUSTOMER', 'SIGNED_BY_EPRES', 'IN_TERMINATION_BY_CUSTOMER', 'IN_TERMINATION_BY_EPRES', 'IN_TERMINATION_BY_GO_DATA', 'DELIVERY', 'FROM_CUSTOMER_WITH_NOTICE', 'FROM_CUSTOMER_WITHOUT_NOTICE', 'FROM_EPRES_WITH_NOTICE', 'FROM_EPRES_WITHOUT_NOTICE', 'BY_MUTUAL_AGREEMENT', 'NEW_CONTRACT_SIGNED', 'EXPIRED', 'DELETED_ID_DECEASED_PERSON', 'FORCE_MAJEURE', 'UNSENT_TO_CUSTOMER', 'INVALID_DATA', 'REFUSAL_TO_SIGN_BY_CUSTOMER', 'REFUSAL_TO_SIGN_BY_EPRES', 'TEST', 'CHANGED_WITH_AGREEMENT');
create type service_contract.contract_payment_guarantee as enum ('NO', 'CASH_DEPOSIT', 'BANK', 'CASH_DEPOSIT_AND_BANK');
create type service_contract.contractVersionStatus as enum ('DRAFT', 'READY', 'SIGNED', 'CANCELLED');
create type service_contract.contractEntryIntoForce as enum ('SIGNING', 'EXACT_DAY', 'DATE_CHANGE_OF_CBG', 'FIRST_DELIVERY','MANUAL');
create type service_contract.status as enum ('DRAFT', 'READY', 'SIGNED', 'CANCELLED');
create type service_contract.contract_start_initial_term as enum ('SIGNING', 'EXACT_DATE', 'DATE_OF_CHANGE_OF_CBG', 'FIRST_DELIVERY', 'MANUAL');

create sequence service_contract.contract_details_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS service_contract.contract_details
(
    id                                     integer primary key             NOT NULL DEFAULT nextval('service_contract.contract_details_id_seq'),
    type                                   service_contract.contract_types not null,
    contract_term_until_the_amount         boolean,
    contract_term_until_the_amount_value   numeric,
    contract_id                            bigint                          not null,
    create_date                            timestamp with time zone                 default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                     not null,
    modify_date                            timestamp with time zone,
    modify_system_user_id                  varchar(50),
    currency_id                            integer,
    cash_deposit_currency_id               integer,
    service_detail_id                      bigint,
    customer_communication_id_for_billing  bigint                          not null,
    customer_communication_id_for_contract bigint                          not null,
    version_id                             integer                         not null,
    customer_detail_id                     bigint                          not null,
    direct_debit                           boolean                         not null,
    bank_id                                integer,
    iban                                   varchar(22),
    applicable_interest_rate               integer                         not null,
    campaign_id                            integer,
    service_contract_term_id               bigint,
    invoice_payment_term_id                bigint,
    payment_guarantee                      service_contract.contract_payment_guarantee,
    invoice_payment_term_value             integer,
    cash_deposit_amount                    numeric,
    equal_monthly_installment_number       smallint,
    equal_monthly_installment_amount       numeric,
    bank_guarantee_amount                  numeric,
    bank_guarantee_currency_id             integer,
    guarantee_contract                     boolean                         not null,
    guarantee_contract_info                varchar(1024),
    start_date                             date                                     default CURRENT_DATE,
    end_date                               date,
    contract_term_end_date                 date                                     default CURRENT_DATE,
    status                                 service_contract.contractVersionStatus,
    entry_into_force                       service_contract.contractEntryIntoForce,
    entry_into_force_value                 date                                     default CURRENT_DATE,
    initial_term_start_value               date                                     default CURRENT_DATE,
    employee_id                            numeric,
    quantity                               numeric,
    additional_agreement_suffix            numeric,
    start_initial_term                     service_contract.contract_start_initial_term,
    constraint contract_details_contract_version_uk
        unique (contract_id, version_id)
);
alter sequence service_contract.contract_details_id_seq owned by service_contract.contract_details.id;


DROP TABLE IF EXISTS nomenclature.activity;
create sequence nomenclature.activity_id_seq increment by 1;
create table if not exists nomenclature.activity
(
    id                    integer primary key                                not null DEFAULT nextval('nomenclature.activity_id_seq'),
    name                  varchar(512)                                       not null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);
alter sequence nomenclature.activity_id_seq owned by nomenclature.activity.id;

DROP TABLE IF EXISTS nomenclature.sub_activity;
create sequence nomenclature.sub_activity_id_seq increment by 1;
create table if not exists nomenclature.sub_activity
(
    id                    integer primary key                                not null DEFAULT nextval('nomenclature.sub_activity_id_seq'),
    name                  varchar(512)                                       not null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null,
    fields                jsonb,
    activity_id           integer                                            not null
);
alter sequence nomenclature.sub_activity_id_seq owned by nomenclature.sub_activity.id;

create type product_contract.contract_detail_status as enum ('DRAFT', 'READY', 'SIGNED', 'ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY', 'TERMINATED', 'CANCELLED', 'CHANGED_WITH_AGREEMENT');
create type product_contract.contract_types as enum ('CONTRACT', 'ADDITIONAL_AGREEMENT', 'EX_OFFICIO_AGREEMENT');
create type product_contract.contract_type as enum ('COMBINED', 'SUPPLY_BALANCING', 'SUPPLY_ONLY', 'WITHOUT_SUPPLY');
create type product_contract.contract_detail_sub_status as enum ('DRAFT', 'READY', 'SIGNED_BY_CUSTOMER', 'SIGNED_BY_EPRES', 'IN_TERMINATION_BY_CUSTOMER', 'IN_TERMINATION_BY_EPRES', 'IN_TERMINATION_BY_GO_DATA', 'DELIVERY', 'FROM_CUSTOMER_WITH_NOTICE', 'FROM_CUSTOMER_WITHOUT_NOTICE', 'FROM_EPRES_WITH_NOTICE', 'FROM_EPRES_WITHOUT_NOTICE', 'BY_MUTUAL_AGREEMENT', 'NEW_CONTRACT_SIGNED', 'EXPIRED', 'DELETED_ID_DECEASED_PERSON', 'FORCE_MAJEURE', 'UNSENT_TO_CUSTOMER', 'INVALID_DATA', 'REFUSAL_TO_SIGN_BY_CUSTOMER', 'REFUSAL_TO_SIGN_BY_EPRES', 'TEST', 'CHANGED_WITH_AGREEMENT');
create type product_contract.contract_payment_guarantee as enum ('NO', 'CASH_DEPOSIT', 'BANK', 'CASH_DEPOSIT_AND_BANK');
create type product_contract.contract_entry_into_force as enum ('SIGNING', 'EXACT_DAY_OF_MONTH', 'DATE_CHANGE_OF_CBG', 'FIRST_DELIVERY', 'MANUAL');
create type product_contract.contract_supply_activation as enum ('FIRST_DAY_OF_MONTH', 'FIRST_DAY_AFTER_EXP_CONTRACT', 'EXACT_DATE', 'MANUAL');
create type product_contract.contract_start_initial_term as enum ('SIGNING', 'EXACT_DATE', 'DATE_OF_CHANGE_OF_CBG', 'FIRST_DELIVERY', 'MANUAL');
create type product_contract.contract_wait_for_old_contract_term_to_expire as enum ('YES', 'NO');

DROP TABLE IF EXISTS product_contract.contract_details;
create sequence product_contract.contract_details_id_seq increment by 1;
create table if not exists product_contract.contract_details
(
    id                                             integer primary key                                not null DEFAULT nextval('product_contract.contract_details_id_seq'),
    type                                           product_contract.contract_types,
    contract_term_until_the_amount                 boolean,
    contract_term_until_the_amount_value           numeric,
    contract_term_until_the_volume                 boolean,
    contract_term_until_the_volume_value           numeric,
    public_procurement_law                         boolean,
    contract_id                                    bigint                                             not null,
    create_date                                    timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                                 varchar(50)                                        not null,
    modify_date                                    timestamp with time zone,
    modify_system_user_id                          varchar(50),
    currency_id                                    integer,
    product_detail_id                              bigint,
    customer_communication_id_for_billing          bigint,
    customer_communication_id_for_contract         bigint,
    version_id                                     integer                                            not null,
    customer_detail_id                             bigint,
    deal_number                                    varchar(50),
    estimated_total_consumption_under_contract_kwh numeric,
    direct_debit                                   boolean,
    bank_id                                        integer,
    iban                                           varchar(22),
    risk_assessment                                varchar(256),
    risk_assessment_additional_condition           varchar(256),
    applicable_interest_rate                       integer,
    campaign_id                                    integer,
    contract_type                                  product_contract.contract_type,
    product_contract_term_id                       bigint,
    invoice_payment_term_id                        bigint,
    payment_guarantee                              product_contract.contract_payment_guarantee,
    invoice_payment_term_value                     integer,
    entry_into_force                               product_contract.contract_entry_into_force,
    supply_activation_after_contract_resigning     product_contract.contract_supply_activation,
    start_initial_term                             product_contract.contract_start_initial_term,
    equal_monthly_installment_number               smallint,
    equal_monthly_installment_amount               numeric,
    cash_deposit_currency_id                       integer,
    cash_deposit_amount                            numeric,
    bank_guarantee_currency_id                     integer,
    bank_guarantee_amount                          numeric,
    guarantee_contract_info                        varchar(1024),
    guarantee_contract                             boolean,
    marginal_price                                 numeric,
    marginal_price_validity                        varchar(14),
    avg_hourly_load_profiles                       numeric,
    procurement_price                              numeric,
    cost_price_increase_from_imbalances            numeric,
    contract_term_end_date                         date,
    initial_term_start_value                       date,
    entry_into_force_value                         date,
    supply_activation_value                        date,
    set_margin                                     numeric,
    constraint contract_details_contract_version_uk
        unique (contract_id, version_id),
    start_date                                     date                     default CURRENT_DATE      not null,
    end_date                                       date,
    status                                         product_contract.contract_detail_status,
    employee_id                                    bigint,
    additional_agreement_suffix                    integer,
    wait_for_old_contract_term_to_expire           product_contract.contract_wait_for_old_contract_term_to_expire
);

create table if not exists product_contract.contract_status_csp_delivery
(
    id                  bigint generated by default as identity
        constraint contract_status_csp_delivery_pk
            primary key,
    contract_id         bigint                                             not null
        constraint contract_status_csp_delivery_contract_fk
            references product_contract.contracts,
    contract_detail_id  bigint                                             not null
        constraint contract_status_csp_delivery_contract_det_fk
            references product_contract.contract_details,
    system_source_id    customer.system_source                             not null,
    contract_status     product_contract.contract_contract_status          not null,
    contract_sub_status product_contract.contract_sub_status               not null,
    activation_date     timestamp,
    create_date         timestamp with time zone default CURRENT_TIMESTAMP not null,
    response_date       timestamp,
    delivery_status     product_contract.contract_status_csp_delivery_status
);

create type service_contract.contract_subobject_status as enum ('ACTIVE', 'DELETED');

create sequence if not exists service_contract.contract_tasks_id_seq increment by 1;
create table if not exists service_contract.contract_tasks
(
    id                    bigint                                     NOT NULL DEFAULT nextval('service_contract.contract_tasks_id_seq'),
    contract_id           bigint                                     not null,
    task_id               bigint                                     not null,
    status                service_contract.contract_subobject_status not null,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence service_contract.contract_activity_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS service_contract.contract_activity
(
    id                    integer primary key                        NOT NULL DEFAULT nextval('service_contract.contract_activity_id_seq'),
    contract_id           bigint                                     not null,
--         constraint contract_activity_contract_fk
--             references service_contract.contracts,
    activity_id           integer                                    not null,
--         constraint contract_activity_fk
--             references activity.activity,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                service_contract.contract_subobject_status not null
);
alter sequence service_contract.contract_activity_id_seq owned by service_contract.contract_activity.id;

create type activity.activity_subobject_status as enum ('ACTIVE', 'DELETED');
create sequence activity.activity_files_id_seq increment by 1;
create table if not exists activity.activity_files
(
    id                    integer primary key                NOT NULL DEFAULT nextval('activity.activity_files_id_seq'),
    name                  varchar(100)                       not null,
    file_url              varchar(256)                       not null,
    activity_id           bigint,
    create_date           timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                activity.activity_subobject_status not null,
    file_statuses         activity.file_status[],
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    archived_file_type    varchar(50)
);

create sequence nomenclature.campaigns_id_seq increment by 1;
create table if not exists nomenclature.campaigns
(
    id                    integer primary key                                not null DEFAULT nextval('nomenclature.campaigns_id_seq'),
    name                  varchar(512)                                       not null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);

create sequence nomenclature.external_intermediaries_id_seq increment by 1;
create table if not exists nomenclature.external_intermediaries
(
    id                    integer primary key                                not null DEFAULT nextval('nomenclature.external_intermediaries_id_seq'),
    name                  varchar(512)                                       not null,
    identifier            varchar(512)                                       not null,
    ordering_id           integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        character varying(50) COLLATE pg_catalog."default" NOT NULL,
    create_date           timestamp with time zone                                    default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                        NULL,
    modify_system_user_id varchar(50),
    status                nomenclature_status                                not null
);

create table if not exists product_contract.contract_internal_intermediaries
(
    id                    bigint generated by default as identity
        constraint contract_internal_intermediaries_pk
            primary key,
    account_manager_id    integer                                            not null,
--         constraint contract_internal_intermediarie_manager_fk
--             references customer.account_managers,
    contract_detail_id    bigint                                             not null,
--         constraint contract_internal_intermediaries_contract_fk
--             references product_contract.contract_details,
    status                product_contract.contract_subobject_status         not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create table if not exists product_contract.contract_external_intermediaries
(
    id                       bigint generated by default as identity
        constraint contract_external_intermediaries_pk
            primary key,
    external_intermediary_id integer                                            not null,
--         constraint contract_external_intermediarie_fk
--             references nomenclature.external_intermediaries,
    contract_detail_id       bigint                                             not null,
--         constraint contract_external_intermediaries_contract_fk
--             references product_contract.contract_details,
    status                   product_contract.contract_subobject_status         not null,
    create_date              timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                                        not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50)
);
create type product_contract.contract_billing_group_sending_invoice as enum ('EMAIL', 'ON_PAPER', 'CUSTOMER_PORTAL');
create table if not exists product_contract.contract_billing_groups
(
    id                                       bigint generated by default as identity
        constraint contract_billing_groups_pk
            primary key,
    group_number                             varchar(4)                                         not null,
    sending_invoice                          product_contract.contract_billing_group_sending_invoice[],
    separate_invoice_for_each_pod            boolean                                            not null,
    direct_debit                             boolean                                            not null,
    bank_id                                  integer,
--         constraint contract_billing_groups_bank_fk
--             references nomenclature.banks,
    iban                                     varchar(22),
    alt_invoice_recipient_customer_detail_id bigint,
--         constraint contract_billing_groups_customer_fk
--             references customer.customer_details,
    customer_communication_id_for_billing    bigint,
--         constraint contract_billing_groups_customer_communication_fk
--             references customer.customer_communications,
    contract_id                              bigint                                             not null,
--         constraint contract_billing_groups_contract_fk
--             references product_contract.contracts,
    create_date                              timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                           varchar(50)                                        not null,
    modify_date                              timestamp with time zone,
    modify_system_user_id                    varchar(50),
    status                                   product_contract.contract_subobject_status         not null,
    single_select                            boolean
);

create table if not exists product_contract.contract_assisting_employees
(
    id                    bigint generated by default as identity
        constraint contract_assisting_employees_pk
            primary key,
    account_manager_id    integer                                            not null,
--         constraint contract_assisting_employees_manager_fk
--             references customer.account_managers,
    contract_detail_id    bigint                                             not null,
--         constraint contract_assisting_employees_contract_fk
--             references product_contract.contract_details,
    status                product_contract.contract_subobject_status         not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table if not exists product_contract.contract_related_product_contracts
(
    id                    bigint generated by default as identity
        constraint related_contracts_pk
            primary key,
    contract_id           bigint                                     not null,
--         constraint related_contracts_fk
--             references product_contract.contracts,
    related_contract_id   bigint                                     not null,
--         constraint contract_related_contracts_fk
--             references product_contract.contracts,
    status                product_contract.contract_subobject_status not null,
    create_date           timestamp with time zone                   not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    constraint related_contracts_chk
        check (contract_id <> related_contract_id)
);

create sequence product_contract.contract_related_service_orders_id_seq increment by 1;
create table if not exists product_contract.contract_related_service_orders
(
    id                    integer primary key                        not null DEFAULT nextval('product_contract.contract_related_service_orders_id_seq'),
    contract_id           bigint                                     not null,
    status                product_contract.contract_subobject_status not null,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    service_order_id      bigint                                     not null
);

create sequence product_contract.contract_related_service_contracts_id_seq increment by 1;
create table if not exists product_contract.contract_related_service_contracts
(
    id                          integer primary key                        not null DEFAULT nextval('product_contract.contract_related_service_contracts_id_seq'),
    contract_id                 bigint                                     not null,
    status                      product_contract.contract_subobject_status not null,
    create_date                 timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                                not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50),
    related_service_contract_id bigint                                     not null
);

create sequence product_contract.contract_related_goods_orders_id_seq increment by 1;
create table if not exists product_contract.contract_related_goods_orders
(
    id                    integer primary key                        not null DEFAULT nextval('product_contract.contract_related_goods_orders_id_seq'),
    contract_id           bigint                                     not null,
    status                product_contract.contract_subobject_status not null,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    goods_order_id        bigint                                     not null
);

create table if not exists product_contract.contract_pods
(
    id                        bigint generated by default as identity
        constraint contract_pods_pk
            primary key,
    status                    product_contract.contract_subobject_status         not null,
    create_date               timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id            varchar(50)                                        not null,
    modify_date               timestamp with time zone,
    modify_system_user_id     varchar(50),
    custom_modify_date        timestamp with time zone,
    split_id                  bigint,
    contract_detail_id        bigint                                             not null,
--         constraint contract_pods_contract_detail_fk
--             references product_contract.contract_details,
    activation_date           date,
    deactivation_date         date,
    deactivation_purpose_id   integer,
--         constraint contract_pods_deactivation_reason_fk
--             references nomenclature.deactivation_purposes,
    contract_billing_group_id bigint,
--         constraint contract_pods_billing_group_fk
--             references product_contract.contract_billing_groups,
    pod_detail_id             bigint                                             not null,
--         constraint contract_pods_pod_fk
--             references pod.pod_details,
    deal_number               varchar(50)
);

create type sysconfig.rfd_channel_option_type as enum ('EMAIL_SMS_ON_PAPER','REST');
create type sysconfig.bg_mdw_inv_send_type as enum ('MULTIPLE_CHOICE', 'SINGLE_CHOICE');
create type sysconfig.bg_mdw_inv_pre_option as enum ('CUSTOMER_PORTAL', 'EMAIL', 'ON_PAPER');
create type sysconfig.communication_data_validation as enum ('EMAIL_MANDATORY', 'SMS_MANDATORY');
create type sysconfig.sms_integration as enum ('NEW_SERVICE', 'OLD_SERVICE');
create type sysconfig.virtual_pos_identification as enum ('MERCHANT_ID', 'SECRET_KEY');
create type sysconfig.contract_number_prefix as enum ('EPES', 'K', 'D');
create sequence sysconfig.runtime_settings_id_seq increment by 1;
create table if not exists sysconfig.runtime_settings
(
    id                            bigint generated by default as identity,
    rfd_communication_channel     sysconfig.rfd_channel_option_type,
    bg_mdw_inv_send_type          sysconfig.bg_mdw_inv_send_type[],
    bg_mdw_inv_pre_option         sysconfig.bg_mdw_inv_pre_option[],
    communication_data_validation sysconfig.communication_data_validation[],
    sms_integration               sysconfig.sms_integration[],
    version_id                    bigint                                             not null,
    create_date                   timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                text,
    modify_date                   timestamp with time zone,
    modify_system_user_id         text,
    easy_pay_merchant_id          text,
    easy_pay_secret_key           text,
    virtual_pos_merchant_id       text,
    virtual_pos_secret_key        text,
    lawsuit_counter               text,
    company_id                    integer,
    contract_number_prefix        sysconfig.contract_number_prefix
);


-- service order

create schema if not exists service_order;
create type service_order.order_order_status as enum ('REQUESTED', 'CONFIRMED', 'AWAITING_PAYMENT', 'PAID', 'IN_EXECUTION', 'COMPLETED', 'REFUSED');
create type service_order.order_status as enum ('ACTIVE', 'DELETED');
create type service_order.order_subobject_status as enum ('ACTIVE', 'DELETED');
create type service_order.order_invoice_status as enum ('NOT_GENERATED', 'DRAFT_PROFORMA_GENERATED', 'PDF_GENERATED','REAL_PROFORMA_GENERATED', 'REAL_INVOICE_GENERATED');

create sequence service_order.orders_id_seq increment by 1;
create table if not exists service_order.orders
(
    id                                    integer primary key              not null DEFAULT nextval('service_order.orders_id_seq'),
    order_number                          varchar(14)                      not null,
    status_modify_date                    timestamp with time zone         not null,
    create_date                           timestamp with time zone                  default CURRENT_TIMESTAMP not null,
    system_user_id                        varchar(50)                      not null,
    modify_date                           timestamp with time zone,
    modify_system_user_id                 varchar(50),
    service_detail_id                     bigint,
    direct_debit                          boolean                          not null,
    bank_id                               integer,
    iban                                  varchar(22),
    applicable_interest_rate_id           bigint,
    campaign_id                           integer,
    prepayment_term_in_calendar_days      integer,
    customer_detail_id                    bigint                           not null,
    customer_communication_id_for_billing bigint                           not null,
    invoice_payment_term_id               bigint,
    order_status                          service_order.order_order_status not null,
    invoice_payment_term_value            integer,
    quantity                              integer,
    status                                service_order.order_status       not null,
    employee_id                           bigint                           not null,
    service_contract_term_id              bigint,
    contract_term_certain_date            timestamp without time zone,
    invoice_template_id                   bigint,
    email_template_id                     bigint,
    order_invoice_status                  service_order.order_invoice_status
);

create sequence service_order.order_activity_id_seq increment by 1;
create table if not exists service_order.order_activity
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_activity_id_seq'),
    order_id              bigint                               not null,
    activity_id           integer                              not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                service_order.order_subobject_status not null
);

create sequence service_order.order_assisting_employees_id_seq increment by 1;
create table if not exists service_order.order_assisting_employees
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_assisting_employees_id_seq'),
    account_manager_id    integer                              not null,
    order_id              bigint                               not null,
    status                service_order.order_subobject_status not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence service_order.order_external_intermediaries_id_seq increment by 1;
create table if not exists service_order.order_external_intermediaries
(
    id                       integer primary key                  not null DEFAULT nextval('service_order.order_external_intermediaries_id_seq'),
    external_intermediary_id integer                              not null,
    order_id                 bigint                               not null,
    status                   service_order.order_subobject_status not null,
    create_date              timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                          not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50)
);

create sequence service_order.order_internal_intermediaries_id_seq increment by 1;
create table if not exists service_order.order_internal_intermediaries
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_internal_intermediaries_id_seq'),
    account_manager_id    integer                              not null,
    order_id              bigint                               not null,
    status                service_order.order_subobject_status not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence service_order.order_pods_id_seq increment by 1;
create table if not exists service_order.order_pods
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_pods_id_seq'),
    status                service_order.order_subobject_status not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    order_id              bigint                               not null,
    pod_id                bigint                               not null
);

create sequence service_order.order_price_components_id_seq increment by 1;
create table if not exists service_order.order_price_components
(
    id                                  integer primary key                  not null DEFAULT nextval('service_order.order_price_components_id_seq'),
    value                               numeric,
    price_component_formula_variable_id bigint                               not null,
    status                              service_order.order_subobject_status not null,
    create_date                         timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id                      varchar(50)                          not null,
    modify_date                         timestamp with time zone,
    modify_system_user_id               varchar(50),
    order_id                            bigint                               not null
);

create sequence service_order.order_proxies_id_seq increment by 1;
create table if not exists service_order.order_proxies
(
    id                                   integer primary key                  not null DEFAULT nextval('service_order.order_proxies_id_seq'),
    proxy_name                           varchar(512)                         not null,
    proxy_foreign_entity_person          boolean                              not null,
    proxy_personal_identifier            varchar(32)                          not null,
    proxy_email                          varchar(512),
    proxy_mobile_phone                   varchar(32),
    proxy_attorney_power_number          varchar(32)                          not null,
    proxy_date                           date                                 not null,
    proxy_valid_till                     date,
    proxy_notary_public                  varchar(512)                         not null,
    proxy_registration_number            varchar(32),
    proxy_operation_area                 varchar(512)                         not null,
    proxy_by_proxy_foreign_entity_person boolean                              not null,
    proxy_by_proxy_name                  varchar(512),
    proxy_by_proxy_personal_identifier   varchar(12),
    proxy_by_proxy_email                 varchar(512),
    proxy_by_proxy_mobile_phone          varchar(32),
    proxy_by_proxy_attorney_power_number varchar(32),
    proxy_by_proxy_date                  date,
    proxy_by_proxy_valid_till            date,
    proxy_by_proxy_notary_public         varchar(512),
    proxy_by_proxy_registration_number   varchar(32),
    proxy_by_proxy_operation_area        varchar(512),
    status                               service_order.order_subobject_status not null,
    create_date                          timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id                       varchar(50)                          not null,
    modify_date                          timestamp with time zone,
    modify_system_user_id                varchar(50),
    order_id                             bigint                               not null
);

create sequence service_order.order_proxy_files_id_seq increment by 1;
create table if not exists service_order.order_proxy_files
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_proxy_files_id_seq'),
    name                  varchar(100)                         not null,
    file_url              varchar(256)                         not null,
    order_proxy_id        bigint,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                service_order.order_subobject_status not null
);

create sequence service_order.order_proxy_managers_id_seq increment by 1;
create table if not exists service_order.order_proxy_managers
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_proxy_managers_id_seq'),
    order_proxy_id        bigint                               not null,
    customer_manager_id   bigint                               not null,
    status                service_order.order_subobject_status not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence service_order.order_related_goods_orders_id_seq increment by 1;
create table if not exists service_order.order_related_goods_orders
(
    id                     integer primary key                  not null DEFAULT nextval('service_order.order_related_goods_orders_id_seq'),
    order_id               bigint                               not null,
    related_goods_order_id bigint                               not null,
    status                 service_order.order_subobject_status not null,
    create_date            timestamp with time zone             not null,
    system_user_id         varchar(50)                          not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50)
);

create sequence service_order.order_related_product_contracts_id_seq increment by 1;
create table if not exists service_order.order_related_product_contracts
(
    id                          integer primary key                  not null DEFAULT nextval('service_order.order_related_product_contracts_id_seq'),
    order_id                    bigint                               not null,
    related_product_contract_id bigint                               not null,
    status                      service_order.order_subobject_status not null,
    create_date                 timestamp with time zone             not null,
    system_user_id              varchar(50)                          not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50)
);

create sequence service_order.order_related_service_contracts_id_seq increment by 1;
create table if not exists service_order.order_related_service_contracts
(
    id                          integer primary key                  not null DEFAULT nextval('service_order.order_related_service_contracts_id_seq'),
    order_id                    bigint                               not null,
    related_service_contract_id bigint                               not null,
    status                      service_order.order_subobject_status not null,
    create_date                 timestamp with time zone             not null,
    system_user_id              varchar(50)                          not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50)
);

create sequence service_order.order_related_service_orders_id_seq increment by 1;
create table if not exists service_order.order_related_service_orders
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_related_service_orders_id_seq'),
    order_id              bigint                               not null,
    related_order_id      bigint                               not null,
    status                service_order.order_subobject_status not null,
    create_date           timestamp with time zone             not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence service_order.order_tasks_id_seq increment by 1;
create table if not exists service_order.order_tasks
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_tasks_id_seq'),
    order_id              bigint                               not null,
    task_id               bigint                               not null,
    status                service_order.order_subobject_status not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence service_order.order_unrecognized_pods_id_seq increment by 1;
create table if not exists service_order.order_unrecognized_pods
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_unrecognized_pods_id_seq'),
    pod_identifier        varchar(50)                          not null,
    status                service_order.order_subobject_status not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    order_id              bigint                               not null
);

create sequence service_order.order_linked_service_contracts_id_seq increment by 1;
create table if not exists service_order.order_linked_service_contracts
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_linked_service_contracts_id_seq'),
    contract_id           bigint                               not null,
    status                service_order.order_subobject_status not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    order_id              bigint                               not null
);

create sequence service_order.order_linked_product_contracts_id_seq increment by 1;
create table if not exists service_order.order_linked_product_contracts
(
    id                    integer primary key                  not null DEFAULT nextval('service_order.order_linked_product_contracts_id_seq'),
    contract_id           bigint                               not null,
    status                service_order.order_subobject_status not null,
    create_date           timestamp with time zone                      default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                          not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    order_id              bigint                               not null
);



create sequence product_contract.contract_proxies_id_seq increment by 1;
create table if not exists product_contract.contract_proxies
(
    id                                   integer primary key                        not null DEFAULT nextval('product_contract.contract_proxies_id_seq'),
    proxy_name                           varchar(512),
    proxy_foreign_entity_person          boolean,
    proxy_personal_identifier            varchar(32),
    proxy_email                          varchar(512),
    proxy_mobile_phone                   varchar(32),
    proxy_attorney_power_number          varchar(32),
    proxy_date                           date,
    proxy_valid_till                     date,
    proxy_notary_public                  varchar(512),
    proxy_registration_number            varchar(32),
    proxy_operation_area                 varchar(512),
    proxy_by_proxy_foreign_entity_person boolean,
    proxy_by_proxy_name                  varchar(512),
    proxy_by_proxy_personal_identifier   varchar(12),
    proxy_by_proxy_email                 varchar(512),
    proxy_by_proxy_mobile_phone          varchar(32),
    proxy_by_proxy_attorney_power_number varchar(32),
    proxy_by_proxy_date                  date,
    proxy_by_proxy_valid_till            date,
    proxy_by_proxy_notary_public         varchar(512),
    proxy_by_proxy_registration_number   varchar(32),
    proxy_by_proxy_operation_area        varchar(512),
    status                               product_contract.contract_subobject_status not null,
    create_date                          timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id                       varchar(50)                                not null,
    modify_date                          timestamp with time zone,
    modify_system_user_id                varchar(50),
    contract_detail_id                   bigint                                     not null
);

create sequence product_contract.contract_proxy_files_id_seq increment by 1;
create table if not exists product_contract.contract_proxy_files
(
    id                    integer primary key                        not null DEFAULT nextval('product_contract.contract_proxy_files_id_seq'),
    name                  varchar(100)                               not null,
    file_url              varchar(256)                               not null,
    contract_proxy_id     bigint,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                product_contract.contract_subobject_status not null
);

create sequence product_contract.contract_proxy_managers_id_seq increment by 1;
create table if not exists product_contract.contract_proxy_managers
(
    id                    integer primary key                        not null DEFAULT nextval('product_contract.contract_proxy_managers_id_seq'),
    contract_proxy_id     bigint                                     not null,
    customer_manager_id   bigint                                     not null,
    status                product_contract.contract_subobject_status not null,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create schema if not exists goods_order;
create type goods_order.order_order_status as enum ('REQUESTED', 'CONFIRMED', 'AWAITING_PAYMENT', 'PAID', 'IN_EXECUTION', 'COMPLETED', 'REFUSED');
create type goods_order.order_status as enum ('ACTIVE', 'DELETED');
create type goods_order.order_invoice_status as enum ('NOT_GENERATED', 'DRAFT_PROFORMA_GENERATED', 'PDF_GENERATED','REAL_PROFORMA_GENERATED', 'REAL_INVOICE_GENERATED');
create sequence goods_order.orders_id_seq increment by 1;
create table if not exists goods_order.orders
(
    id                                    integer primary key            not null DEFAULT nextval('goods_order.orders_id_seq'),
    order_number                          varchar(14)                    not null,
    status                                goods_order.order_status       not null,
    create_date                           timestamp with time zone                default CURRENT_TIMESTAMP not null,
    system_user_id                        varchar(50)                    not null,
    modify_date                           timestamp with time zone,
    modify_system_user_id                 varchar(50),
    direct_debit                          boolean                        not null,
    bank_id                               integer,
    iban                                  varchar(22),
    applicable_interest_rate_id           bigint,
    payment_term_in_calendar_days         integer,
    customer_detail_id                    bigint                         not null,
    customer_communication_id_for_billing bigint                         not null,
    no_interest_on_overdue_debts          boolean,
    activity_id                           bigint,
    income_account_number                 varchar(32),
    cost_center_controlling_order         varchar(32),
    vat_rate_id                           integer,
    global_vat_rate                       boolean                        not null,
    order_status                          goods_order.order_order_status not null,
    campaign_id                           integer,
    status_modify_date                    timestamp with time zone,
    employee_id                           bigint,
    invoice_template_id                   bigint,
    email_template_id                     bigint,
    order_invoice_status                  goods_order.order_invoice_status
);

create type goods_order.order_subobject_status as enum ('ACTIVE', 'DELETED');

create sequence if not exists goods_order.order_proxies_id_seq increment by 1;
create table if not exists goods_order.order_proxies
(
    id                                   integer primary key                not null DEFAULT nextval('goods_order.order_proxies_id_seq'),
    proxy_name                           varchar(512)                       not null,
    proxy_foreign_entity_person          boolean                            not null,
    proxy_personal_identifier            varchar(32)                        not null,
    proxy_email                          varchar(512),
    proxy_mobile_phone                   varchar(32),
    proxy_attorney_power_number          varchar(32)                        not null,
    proxy_date                           date                               not null,
    proxy_valid_till                     date,
    proxy_notary_public                  varchar(512)                       not null,
    proxy_registration_number            varchar(32),
    proxy_operation_area                 varchar(512)                       not null,
    proxy_by_proxy_foreign_entity_person boolean                            not null,
    proxy_by_proxy_name                  varchar(512),
    proxy_by_proxy_personal_identifier   varchar(12),
    proxy_by_proxy_email                 varchar(512),
    proxy_by_proxy_mobile_phone          varchar(32),
    proxy_by_proxy_attorney_power_number varchar(32),
    proxy_by_proxy_date                  date,
    proxy_by_proxy_valid_till            date,
    proxy_by_proxy_notary_public         varchar(512),
    proxy_by_proxy_registration_number   varchar(32),
    proxy_by_proxy_operation_area        varchar(512),
    status                               goods_order.order_subobject_status not null,
    create_date                          timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id                       varchar(50)                        not null,
    modify_date                          timestamp with time zone,
    modify_system_user_id                varchar(50),
    order_id                             bigint                             not null
);

create sequence if not exists goods_order.order_internal_intermediaries_id_seq increment by 1;
create table if not exists goods_order.order_internal_intermediaries
(
    id                    integer primary key                not null DEFAULT nextval('goods_order.order_internal_intermediaries_id_seq'),
    account_manager_id    integer                            not null,
    order_id              bigint                             not null,
    status                goods_order.order_subobject_status not null,
    create_date           timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists goods_order.order_assisting_employees_id_seq increment by 1;
create table if not exists goods_order.order_assisting_employees
(
    id                    integer primary key                not null DEFAULT nextval('goods_order.order_assisting_employees_id_seq'),
    account_manager_id    integer                            not null,
    order_id              bigint                             not null,
    status                goods_order.order_subobject_status not null,
    create_date           timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists goods_order.order_activity_id_seq increment by 1;
create table if not exists goods_order.order_activity
(
    id                    integer primary key                not null DEFAULT nextval('goods_order.order_activity_id_seq'),
    order_id              bigint                             not null,
    activity_id           integer                            not null,
    create_date           timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                goods_order.order_subobject_status not null
);

create sequence if not exists goods_order.order_internal_intermediaries_id_seq increment by 1;
create table if not exists goods_order.order_internal_intermediaries
(
    id                    integer primary key                not null DEFAULT nextval('goods_order.order_internal_intermediaries_id_seq'),
    account_manager_id    integer                            not null,
    order_id              bigint                             not null,
    status                goods_order.order_subobject_status not null,
    create_date           timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists goods_order.order_related_goods_orders_id_seq increment by 1;
create table if not exists goods_order.order_related_goods_orders
(
    id                    integer primary key                not null DEFAULT nextval('goods_order.order_related_goods_orders_id_seq'),
    order_id              bigint                             not null,
    related_order_id      bigint                             not null,
    status                goods_order.order_subobject_status not null,
    create_date           timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    constraint related_orders_chk
        check (order_id <> related_order_id)
);

create sequence if not exists goods_order.order_tasks_id_seq increment by 1;
create table if not exists goods_order.order_tasks
(
    id                    integer primary key                not null DEFAULT nextval('goods_order.order_tasks_id_seq'),
    order_id              bigint                             not null,
    task_id               bigint                             not null,
    status                goods_order.order_subobject_status not null,
    create_date           timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence service_order.order_number_seq increment by 1;

create sequence goods_order.order_external_intermediaries_id_seq increment by 1;
create table if not exists goods_order.order_external_intermediaries
(
    id                       integer primary key                not null DEFAULT nextval('goods_order.order_external_intermediaries_id_seq'),
    external_intermediary_id integer                            not null,
    order_id                 bigint                             not null,
    status                   goods_order.order_subobject_status not null,
    create_date              timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                        not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50)
);

create type goods_order.payment_term_exclude as enum ('WEEKENDS', 'HOLIDAYS');
create type goods_order.payment_term_due_date_change as enum ('PREVIOUS_WORKING_DAY', 'NEXT_WORKING_DAY');
create type goods_order.payment_term_calendar_types as enum ('WORKING_DAYS', 'CALENDAR_DAYS', 'CERTAIN_DAYS');
create sequence if not exists goods_order.order_payment_terms_id_seq increment by 1;
create table if not exists goods_order.order_payment_terms
(
    id                    integer primary key                     not null DEFAULT nextval('goods_order.order_payment_terms_id_seq'),
    type                  goods_order.payment_term_calendar_types not null,
    value                 integer,
    calendar_id           integer                                 not null,
    order_id              bigint                                  not null,
    create_date           timestamp with time zone                         default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                             not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                goods_order.order_subobject_status      not null,
    due_date_change       goods_order.payment_term_due_date_change,
    name                  varchar(1024)                           not null,
    excludes              goods_order.payment_term_exclude[]
);

create sequence if not exists goods_order.order_goods_id_seq increment by 1;
create table if not exists goods_order.order_goods
(
    id                            integer primary key not null DEFAULT nextval('goods_order.order_goods_id_seq'),
    name                          varchar(1024),
    other_system_connection_code  varchar(256),
    goods_units_id                integer,
    quantity                      numeric             not null,
    price                         numeric,
    currency_id                   integer,
    income_account_numbers        varchar(512),
    cost_center_controlling_order varchar(512),
    goods_details_id              bigint,
    create_date                   timestamp with time zone     default CURRENT_TIMESTAMP not null,
    system_user_id                varchar(50)         not null,
    modify_date                   timestamp with time zone,
    modify_system_user_id         varchar(50),
    order_id                      bigint              not null
);

create sequence service_contract.contract_external_intermediaries_id_seq increment by 1;
create table if not exists service_contract.contract_external_intermediaries
(
    id                       integer primary key                        not null DEFAULT nextval('service_contract.contract_external_intermediaries_id_seq'),
    external_intermediary_id integer                                    not null,
    contract_detail_id       bigint                                     not null,
    status                   service_contract.contract_subobject_status not null,
    create_date              timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                                not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50)
);

create sequence service_contract.contract_related_service_orders_id_seq increment by 1;
create table if not exists service_contract.contract_related_service_orders
(
    id                    integer primary key                        not null DEFAULT nextval('service_contract.contract_related_service_orders_id_seq'),
    contract_id           bigint                                     not null,
    status                service_contract.contract_subobject_status not null,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    service_order_id      bigint                                     not null
);

create sequence service_contract.contract_related_service_contracts_id_seq increment by 1;
create table if not exists service_contract.contract_related_service_contracts
(
    id                          integer primary key                        not null DEFAULT nextval('service_contract.contract_related_service_contracts_id_seq'),
    contract_id                 bigint                                     not null,
    related_contract_id         bigint                                     not null,
    status                      service_contract.contract_subobject_status not null,
    create_date                 timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                                not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50),
    related_service_contract_id bigint                                     not null
);

create sequence service_contract.contract_related_goods_orders_id_seq increment by 1;
create table if not exists service_contract.contract_related_goods_orders
(
    id                    integer primary key                        not null DEFAULT nextval('service_contract.contract_related_goods_orders_id_seq'),
    contract_id           bigint                                     not null,
    status                service_contract.contract_subobject_status not null,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    goods_order_id        bigint                                     not null
);


create sequence nomenclature.action_types_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.action_types
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.action_types_id_seq') primary key,
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    is_hard_coded         boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50)
);


create schema if not exists action;
create type action.action_penalty_payer as enum ('CUSTOMER', 'EPRES');
create type action.action_status as enum ('ACTIVE', 'DELETED');
create type action.action_action_status as enum ('AWAITING', 'EXECUTED');
create type action.action_subobject_status as enum ('ACTIVE', 'DELETED');
create type action.action_file_status as enum ('DRAFT', 'SIGNED');

create sequence action.actions_id_seq increment by 1;
create table if not exists action.actions
(
    id                             integer                     NOT NULL DEFAULT nextval('action.actions_id_seq') primary key,
    action_type_id                 integer                     not null,
    notice_receiving_date          date                        not null,
    execution_date                 date                        not null,
    penalty_claim_amount           numeric,
    penalty_claim_currency_id      integer,
    penalty_payer                  action.action_penalty_payer not null,
    dont_allow_auto_penalty_claim  boolean                     not null,
    penalty_id                     bigint,
    termination_id                 bigint,
    customer_id                    bigint                      not null,
    product_contract_id            bigint,
    additional_info                varchar(1024),
    create_date                    timestamp with time zone             default CURRENT_TIMESTAMP not null,
    system_user_id                 varchar(50)                 not null,
    modify_date                    timestamp with time zone,
    modify_system_user_id          varchar(50),
    status                         action.action_status        not null,
    action_status                  action.action_action_status,
    service_contract_id            bigint,
    email_template_id              bigint,
    document_template_id           bigint,
    calculated_penalty_amount      numeric,
    calculated_penalty_currency_id integer,
    without_penalty                boolean                     not null,
    without_auto_termination       boolean,
    claim_amount_manually_entered  boolean,
    prefix                         varchar(50),
    action_number                  varchar(12)
);

create table if not exists service_contract.contract_internal_intermediaries
(
    id                    bigint generated by default as identity
        constraint contract_internal_intermediaries_pk
            primary key,
    account_manager_id    integer                                            not null,
    contract_detail_id    bigint                                             not null,
    status                service_contract.contract_subobject_status         not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table if not exists service_contract.contract_assisting_employees
(
    id                    bigint generated by default as identity,
    account_manager_id    integer                                            not null,
    contract_detail_id    bigint                                             not null,
    status                service_contract.contract_subobject_status         not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);


create table service_contract.contract_linked_service_contracts
(
    id                         bigint generated by default as identity,
    status                     service_contract.contract_subobject_status         not null,
    create_date                timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                        not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    contract_id                bigint                                             not null,
    linked_service_contract_id bigint                                             not null
);

create table service_contract.contract_linked_product_contracts
(
    id                         bigint generated by default as identity,
    status                     service_contract.contract_subobject_status         not null,
    create_date                timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                        not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    contract_id                bigint                                             not null,
    linked_product_contract_id bigint                                             not null
);

create table service_contract.contract_pods
(
    id                    bigint generated by default as identity,
    status                service_contract.contract_subobject_status         not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    contract_detail_id    bigint                                             not null,
    pod_id                bigint                                             not null
);

create table service_contract.contract_unrecognized_pods
(
    id                    bigint generated by default as identity,
    pod_identifier        varchar(50)                                        not null,
    status                service_contract.contract_subobject_status         not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    contract_detail_id    bigint                                             not null
);

create sequence service_contract.contract_proxies_id_seq increment by 1;
create table if not exists service_contract.contract_proxies
(
    id                                   integer primary key                        not null DEFAULT nextval('service_contract.contract_proxies_id_seq'),
    proxy_name                           varchar(512)                               not null,
    proxy_foreign_entity_person          boolean                                    not null,
    proxy_personal_identifier            varchar(32)                                not null,
    proxy_email                          varchar(512),
    proxy_mobile_phone                   varchar(32),
    proxy_attorney_power_number          varchar(32)                                not null,
    proxy_date                           date                                       not null,
    proxy_valid_till                     date,
    proxy_notary_public                  varchar(512)                               not null,
    proxy_registration_number            varchar(32),
    proxy_operation_area                 varchar(512)                               not null,
    proxy_by_proxy_foreign_entity_person boolean                                    not null,
    proxy_by_proxy_name                  varchar(512),
    proxy_by_proxy_personal_identifier   varchar(12),
    proxy_by_proxy_email                 varchar(512),
    proxy_by_proxy_mobile_phone          varchar(32),
    proxy_by_proxy_attorney_power_number varchar(32),
    proxy_by_proxy_date                  date,
    proxy_by_proxy_valid_till            date,
    proxy_by_proxy_notary_public         varchar(512),
    proxy_by_proxy_registration_number   varchar(32),
    proxy_by_proxy_operation_area        varchar(512),
    status                               service_contract.contract_subobject_status not null,
    create_date                          timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id                       varchar(50)                                not null,
    modify_date                          timestamp with time zone,
    modify_system_user_id                varchar(50),
    contract_detail_id                   bigint                                     not null
);

create sequence service_contract.contract_proxy_files_id_seq increment by 1;
create table if not exists service_contract.contract_proxy_files
(
    id                    integer primary key                        not null DEFAULT nextval('service_contract.contract_proxy_files_id_seq'),
    name                  varchar(100)                               not null,
    file_url              varchar(256)                               not null,
    contract_proxy_id     bigint,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                service_contract.contract_subobject_status not null
);

create sequence service_contract.contract_proxy_managers_id_seq increment by 1;
create table if not exists service_contract.contract_proxy_managers
(
    id                    integer primary key                        not null DEFAULT nextval('service_contract.contract_proxy_managers_id_seq'),
    contract_proxy_id     bigint                                     not null,
    customer_manager_id   bigint                                     not null,
    status                service_contract.contract_subobject_status not null,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);


create sequence service_contract.contract_files_id_seq increment by 1;
create table if not exists service_contract.contract_files
(
    id                    integer primary key                        not null DEFAULT nextval('service_contract.contract_files_id_seq'),
    name                  varchar(100)                               not null,
    file_url              varchar(256)                               not null,
    contract_detail_id    bigint,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                service_contract.contract_subobject_status not null,
    file_statuses         service_contract.contract_file_status[],
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    archived_file_type    varchar(50)
);

create sequence service_contract.contract_additional_docs_id_seq increment by 1;
create table if not exists service_contract.contract_additional_docs
(
    id                    integer primary key                        not null DEFAULT nextval('service_contract.contract_additional_docs_id_seq'),
    name                  varchar(100)                               not null,
    file_url              varchar(256)                               not null,
    contract_detail_id    bigint,
    create_date           timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                service_contract.contract_subobject_status not null,
    file_statuses         service_contract.contract_file_status[],
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    archived_file_type    varchar(50)
);

create sequence action.action_pods_id_seq increment by 1;
create table if not exists action.action_pods
(
    id                    integer                        NOT NULL DEFAULT nextval('action.action_pods_id_seq') primary key,
    pod_id                bigint                         not null,
    create_date           timestamp with time zone                default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                    not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                action.action_subobject_status not null,
    action_id             bigint                         not null
);

create sequence action.action_files_id_seq increment by 1;
create table if not exists action.action_files
(
    id                    integer                        NOT NULL DEFAULT nextval('action.action_files_id_seq') primary key,
    name                  varchar(100)                   not null,
    file_url              varchar(256)                   not null,
    action_id             bigint,
    create_date           timestamp with time zone                default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                    not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                action.action_subobject_status not null,
    file_statuses         action.action_file_status[],
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    archived_file_type    varchar(50)
);

create view action.vw_action_pods(action_id, name, name_desc) as
SELECT DISTINCT ap.action_id,
                string_agg(p.identifier::text, ', '::text)
                OVER (PARTITION BY ap.action_id ORDER BY p.identifier ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS name,
                string_agg(p.identifier::text, ', '::text)
                OVER (PARTITION BY ap.action_id ORDER BY p.identifier DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS name_desc
FROM action.action_pods ap
         JOIN pod.pod p ON ap.pod_id = p.id
WHERE ap.status = 'ACTIVE'::action.action_subobject_status
  AND p.status = 'ACTIVE'::pod.pod_status;

create sequence product_contract.contract_version_types_id_seq;

create table if not exists product_contract.contract_version_types
(
    id                       bigint                                     NOT NULL DEFAULT nextval('product_contract.contract_version_types_id_seq')
        constraint contract_version_types_pk
            primary key,
    contract_detail_id       bigint                                     not null,
--         constraint contract_version_types_contract_fk
--             references product_contract.contract_details,
    contract_version_type_id bigint                                     not null,
--         constraint contract_version_types_fk
--             references nomenclature.contract_version_types,
    status                   product_contract.contract_subobject_status not null,
    create_date              timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                                not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50)
);

create sequence if not exists service_contract.contract_version_types_id_seq;
create table if not exists service_contract.contract_version_types
(
    id                       bigint                                     NOT NULL DEFAULT nextval('service_contract.contract_version_types_id_seq')
        constraint contract_version_types_pk
            primary key,
    contract_detail_id       bigint                                     not null,
--         constraint contract_version_types_contract_fk
--             references service_contract.contract_details,
    contract_version_type_id bigint                                     not null,
--         constraint contract_version_types_fk
--             references nomenclature.contract_version_types,
    status                   service_contract.contract_subobject_status not null,
    create_date              timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                                not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50)
);

create sequence service_contract.contract_interim_advance_payments_id_seq;
create table service_contract.contract_interim_advance_payments
(
    id                         bigint      NOT NULL     DEFAULT nextval('service_contract.contract_interim_advance_payments_id_seq'),
    issue_day                  int,
    value                      smallint,
    term_value                 int,
    interim_advance_payment_id bigint      not null,
    create_date                timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50) not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    contract_detail_id         bigint      not null,
    status                     service_contract.contract_subobject_status
);

create sequence service_contract.contract_price_components_id_seq;
create table service_contract.contract_price_components
(

    id                                  bigint                                     NOT NULL DEFAULT nextval('service_contract.contract_price_components_id_seq'),
    value                               numeric,
    price_component_formula_variable_id bigint                                     not null,
    status                              service_contract.contract_subobject_status not null,
    create_date                         timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id                      varchar(50)                                not null,
    modify_date                         timestamp with time zone,
    modify_system_user_id               varchar(50),
    contract_detail_id                  bigint                                     not null
);


create sequence service_contract.contract_iap_price_components_id_seq;
create table service_contract.contract_iap_price_components
(

    id                                  bigint                                     NOT NULL DEFAULT nextval('service_contract.contract_iap_price_components_id_seq'),
    price_component_formula_variable_id bigint                                     not null,
    status                              service_contract.contract_subobject_status not null,
    value                               numeric,
    create_date                         timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id                      varchar(50)                                not null,
    modify_date                         timestamp with time zone,
    modify_system_user_id               varchar(50),
    contract_interim_advance_payment_id bigint                                     not null
);

create sequence if not exists goods_order.order_related_product_contracts_id_seq;
create table if not exists goods_order.order_related_product_contracts
(
    id                          bigint                             NOT NULL DEFAULT nextval('goods_order.order_related_product_contracts_id_seq'),
    order_id                    bigint                             not null,
--         constraint related_contracts_fk
--             references goods_order.orders,
    related_product_contract_id bigint                             not null,
--         constraint order_related_product_contracts_fk
--             references product_contract.contracts,
    status                      goods_order.order_subobject_status not null,
    create_date                 timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                        not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50)
);

create sequence if not exists goods_order.order_related_service_contracts_id_seq;
create table if not exists goods_order.order_related_service_contracts
(
    id                          bigint                             NOT NULL DEFAULT nextval('goods_order.order_related_service_contracts_id_seq'),
    order_id                    bigint                             not null,
--         constraint related_contracts_fk
--             references goods_order.orders,
    related_service_contract_id bigint                             not null,
--         constraint order_related_service_contracts_fk
--             references service_contract.contracts,
    status                      goods_order.order_subobject_status not null,
    create_date                 timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                        not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50)
);

create sequence if not exists goods_order.order_related_service_orders_id_seq;
create table if not exists goods_order.order_related_service_orders
(
    id                       bigint                             NOT NULL DEFAULT nextval('goods_order.order_related_service_orders_id_seq'),
    order_id                 bigint                             not null,
--         constraint related_orders_fk
--             references goods_order.orders,
    related_service_order_id bigint                             not null,
--         constraint order_related_service_orders_fk
--             references service_order.orders,
    status                   goods_order.order_subobject_status not null,
    create_date              timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                        not null,
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50)
);

create sequence if not exists goods_order.order_related_goods_orders_id_seq;
create table if not exists goods_order.order_related_goods_orders
(
    id                    bigint                             NOT NULL DEFAULT nextval('goods_order.order_related_goods_orders_id_seq'),
    order_id              bigint                             not null,
--         constraint related_orders_fk
--             references goods_order.orders,
    related_order_id      bigint                             not null,
--         constraint order_related_goods_orders_fk
--             references goods_order.orders,
    status                goods_order.order_subobject_status not null,
    create_date           timestamp with time zone                    default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    constraint related_orders_chk
        check (order_id <> related_order_id)
);

create view service_order.vw_order_customer_account_managers(customer_detail_id, display_name, display_name_desc) as
SELECT DISTINCT cam.customer_detail_id,
                string_agg(((am.display_name::text || '('::text) || am.user_name::text) || ')'::text, '/'::text)
                OVER (PARTITION BY cam.customer_detail_id ORDER BY am.display_name ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS display_name,
                string_agg(((am.display_name::text || '('::text) || am.user_name::text) || ')'::text, '/'::text)
                OVER (PARTITION BY cam.customer_detail_id ORDER BY am.display_name DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS display_name_desc
FROM customer.customer_account_managers cam,
     customer.account_managers am,
     customer.customer_details cd,
     customer.customers c
WHERE cam.customer_detail_id = cd.id
  AND cam.account_manager_id = am.id
  AND cd.customer_id = c.id
  AND cam.status = 'ACTIVE'::status_enum;

create sequence if not exists action.action_liabilities_id_seq increment by 1;
create table if not exists action.action_liabilities
(
    id                    bigint                         NOT NULL DEFAULT nextval('action.action_liabilities_id_seq'),
    action_id             bigint                         not null,
    status                action.action_subobject_status not null,
    create_date           timestamp with time zone                default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                    not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists product.product_for_balancing_id_seq increment by 1;
create table if not exists product.product_for_balancing
(
    id                    bigint                 NOT NULL DEFAULT nextval('product.product_for_balancing_id_seq'),
    name                  varchar(50)            not null,
    status                product.product_status not null,
    create_date           timestamp with time zone        default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)            not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

DROP TABLE IF EXISTS nomenclature.prefixes;
create sequence if not exists nomenclature.prefixes_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.prefixes
(
    id                    integer                                              NOT NULL DEFAULT nextval('nomenclature.prefixes_id_seq'),
    name                  character varying(2048) COLLATE pg_catalog."default" NOT NULL,
    prefix_type           varchar(50)                                          NOT NULL,
    status                nomenclature_status                                  NOT NULL,
    ordering_id           integer                                              NOT NULL,
    is_default            boolean                                              NOT NULL,
    system_user_id        character varying(50) COLLATE pg_catalog."default"   NOT NULL,
    create_date           timestamp with time zone                                      default CURRENT_TIMESTAMP not null,
    modify_date           timestamptz                                          NULL,
    modify_system_user_id varchar(50),
    CONSTRAINT prefixes_pk PRIMARY KEY (id),
    is_hard_coded         boolean
);
alter sequence nomenclature.prefixes_id_seq owned by nomenclature.prefixes.id;

create schema if not exists company;
create sequence if not exists company.companies_id_seq increment by 1;
create sequence if not exists company.company_number_seq increment by 1;
create table company.companies
(
    id                    bigint generated by default as identity
        constraint companies_pk
            primary key,
    company_number        bigint                                             not null
        constraint companies_company_number_uk
            unique,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create type company.company_subobject_status as enum ('ACTIVE', 'DELETED');
create sequence if not exists company.company_details_id_seq increment by 1;
create table company.company_details
(
    id                                    bigint generated by default as identity
        constraint company_details_pk
            primary key,
    identifier                            varchar(32)                                        not null,
    vat_number                            varchar(32),
    number_under_excise_duties_tax_wh_act varchar(512),
    name                                  varchar(2048)                                      not null,
    name_transl                           varchar(2048)                                      not null,
    management_address                    varchar(2048)                                      not null,
    management_address_transl             varchar(2048)                                      not null,
    version_id                            integer                                            not null,
    company_id                            bigint                                             not null,
    create_date                           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                        varchar(50)                                        not null,
    modify_date                           timestamp with time zone,
    modify_system_user_id                 varchar(50),
    start_date                            date                                               not null,
    logo_name                             varchar(200),
    logo_file_url                         varchar(300),
    company_title                         varchar(512),
    company_title_transl                  varchar(512),
    color_scheme                          varchar(20),
    unique (company_id, version_id)
);
create sequence if not exists company.company_logos_id_seq increment by 1;
create table company.company_logos
(
    id                    bigint generated by default as identity
        constraint company_logos_pk
            primary key,
    name                  varchar(200)                                       not null,
    file_url              varchar(256)                                       not null,
    company_detail_id     bigint,
--         constraint company_logos_company_detail_fk
--             references company.company_details,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                company.company_subobject_status                   not null
);
create sequence if not exists company.company_banks_id_seq increment by 1;
create table company.company_banks
(
    id                    bigint generated by default as identity
        constraint company_banks_pk
            primary key,
    company_detail_id     bigint                                             not null,
--         constraint company_banks_company_fk
--             references company.company_details,
    bank_id               integer                                            not null,
--         constraint company_banks_fk
--             references nomenclature.banks,
    iban                  varchar(22)                                        not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                company.company_subobject_status                   not null
);
create sequence if not exists company.company_communication_addresses_id_seq increment by 1;
create table company.company_communication_addresses
(
    id                    bigint generated by default as identity
        constraint company_communication_addresses_pk
            primary key,
    company_detail_id     bigint                                             not null,
--         constraint company_communication_addresses_company_fk
--             references company.company_details,
    address               varchar(2048)                                      not null,
    address_transl        varchar(2048)                                      not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                company.company_subobject_status                   not null
);
create sequence if not exists company.company_emails_id_seq increment by 1;
create table company.company_emails
(
    id                    bigint generated by default as identity
        constraint company_emails_pk
            primary key,
    company_detail_id     bigint                                             not null,
--         constraint company_emails_company_fk
--             references company.company_details,
    email                 varchar(2048)                                      not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                company.company_subobject_status                   not null
);
create sequence if not exists company.company_invoice_compilers_id_seq increment by 1;
create table company.company_invoice_compilers
(
    id                      bigint generated by default as identity
        constraint company_invoice_compilers_pk
            primary key,
    company_detail_id       bigint                                             not null,
--         constraint company_invoice_compilers_company_fk
--             references company.company_details,
    invoice_compiler        varchar(512)                                       not null,
    invoice_compiler_transl varchar(512)                                       not null,
    create_date             timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id          varchar(50)                                        not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50),
    status                  company.company_subobject_status                   not null
);
create sequence if not exists company.company_invoice_issue_places_id_seq increment by 1;
create table company.company_invoice_issue_places
(
    id                         bigint generated by default as identity
        constraint company_invoice_issue_places_pk
            primary key,
    company_detail_id          bigint                                             not null,
--         constraint company_invoice_issue_places_company_fk
--             references company.company_details,
    invoice_issue_place        varchar(512)                                       not null,
    invoice_issue_place_transl varchar(512)                                       not null,
    create_date                timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                        not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    status                     company.company_subobject_status                   not null
);
create sequence if not exists company.company_managers_id_seq increment by 1;
create table company.company_managers
(
    id                    bigint generated by default as identity
        constraint company_managers_pk
            primary key,
    company_detail_id     bigint                                             not null,
--         constraint company_managers_company_fk
--             references company.company_details,
    manager               varchar(2048)                                      not null,
    manager_transl        varchar(2048)                                      not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                company.company_subobject_status                   not null
);
create sequence if not exists company.company_telephones_id_seq increment by 1;
create table company.company_telephones
(
    id                    bigint generated by default as identity
        constraint company_telephones_pk
            primary key,
    company_detail_id     bigint                                             not null,
--         constraint company_telephones_company_fk
--             references company.company_details,
    telephone             varchar(2048)                                      not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                company.company_subobject_status                   not null
);

create schema billing;
create type billing.billing_run_periodicity as enum ('STANDARD', 'PERIODIC');
create type billing.billing_type as enum ('STANDARD_BILLING', 'MANUAL_INVOICE', 'MANUAL_CREDIT_OR_DEBIT_NOTE', 'INVOICE_CORRECTION', 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT', 'INVOICE_REVERSAL');
create type billing.billing_status as enum ('INITIAL', 'IN_PROGRESS_DRAFT', 'DRAFT', 'IN_PROGRESS_GENERATION', 'GENERATED', 'IN_PROGRESS_ACCOUNTING', 'COMPLETED', 'DELETED', 'PAUSED', 'CANCELLED', 'IN_PROGRESS_TERMINATION');
create type billing.billing_invoice_due_date_type as enum ('ACCORDING_TO_THE_CONTRACT', 'DATE');
create type billing.billing_application_model_type as enum ('FOR_VOLUMES', 'OVER_TIME_PERIODICAL', 'OVER_TIME_ONE_TIME', 'PER_PIECE', 'INTERIM_AND_ADVANCE_PAYMENT');
create type billing.billing_sending_an_invoice as enum ('ACCORDING_TO_THE_CONTRACT', 'EMAIL', 'PAPER');
create type billing.billing_execution_type as enum ('MANUAL', 'IMMEDIATELY', 'EXACT_DATE');
create type billing.billing_criteria as enum ('ALL_CUSTOMERS', 'CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS', 'LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS');
create type billing.billing_application_level as enum ('CUSTOMER', 'CONTRACT', 'POD');
create type billing.billing_run_stage as enum ('GENERATE_AND_SIGN', 'AUTOMATICALLY_ACCOUNTING');
create type billing.billing_document_type as enum ('DEBIT_NOTE', 'CREDIT_NOTE');
create type billing.billing_deduction_from as enum ('FIRST_INVOICE_FOR_SAME_PERIOD', 'FIRST_INVOICE_WITH_LONGER_PAYMENT_TERM');
create type billing.billing_issuing_for_month_to_current as enum ('ONE', 'MINUS_ONE', 'ZERO', 'PLUS_ONE', 'PLUS_TWO', 'PLUS_TWELVE');
create type billing.billing_invoice_type as enum ('INVOICE_ONE', 'INVOICE_TWO', 'INVOICE_THREE', 'INVOICE_FOUR');
create type billing.billing_manual_invoice_type as enum ('STANDARD_INVOICE', 'DETAILED_INVOICE');
create type billing.ap_file_generation_status as enum ('INITIAL', 'IN_PROGRESS', 'FAILED', 'COMPLETED');


DROP TABLE IF EXISTS billing.account_periods;
create sequence if not exists billing.account_periods_id_seq increment by 1;
create table billing.account_periods
(
    id                     integer                  not null default nextval('billing.account_periods_id_seq')
        constraint account_periods_pk
            primary key,
    name                   varchar(16)              not null,
    start_date             timestamp with time zone not null,
    end_date               timestamp with time zone not null,
    status                 account_period_status    not null,
    create_date            timestamp with time zone          default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)              not null,
    modify_date            timestamp with time zone,
    file_generation_status billing.ap_file_generation_status,
    modify_system_user_id  varchar(50)
);

DROP TABLE IF EXISTS billing.account_period_status_change_hist;
create sequence if not exists billing.account_period_status_change_hist_id_seq increment by 1;
create table billing.account_period_status_change_hist
(
    id                bigint generated by default as identity
        constraint account_period_status_change_hist_pk
            primary key,
    account_period_id bigint                                             not null,
    status            account_period_status                              not null,
    create_date       timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id    varchar(50)
);

create type billing.billing_subobject_status as enum ('ACTIVE', 'DELETED');

create table billing.account_period_sap_report
(
    id                    bigint generated by default as identity
        primary key,
    name                  varchar(200)                                       not null,
    file_url              varchar(256)                                       not null,
    account_period_id     bigint                                             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                billing.billing_subobject_status                   not null
);

create table billing.account_period_vat_dairy_report
(
    id                    bigint generated by default as identity
        primary key,
    name                  varchar(200)                                       not null,
    file_url              varchar(256)                                       not null,
    account_period_id     bigint                                             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                billing.billing_subobject_status                   not null
);

create type billing.process_periodicity_billing_process_start as enum ('INSTANT', 'MANUAL', 'AFTER_PROCESS', 'DATE_AND_TIME');
create type billing.process_periodicity_change_to as enum ('PREVIOUS_WORKING_DAY', 'NEXT_WORKING_DAY');
create type billing.process_periodicity_day as enum ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY', 'ALL_DAYS');
create type billing.process_periodicity_exclude as enum ('WEEKENDS', 'HOLIDAYS');
create type billing.process_periodicity_ignore_at_runtime as enum ('ERRORS', 'WARNINGS');
create type billing.process_periodicity_month as enum ('JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER');
create type billing.process_periodicity_period_type as enum ('PERIOD_OF_YEAR', 'DAY_OF_MONTH', 'FORMULA');
create type billing.process_periodicity_status as enum ('ACTIVE', 'DELETED');
create type billing.process_periodicity_type as enum ('PERIODICAL', 'ONE_TIME');
create type billing.process_periodicity_week as enum ('FIRST_WEEK', 'SECOND_WEEK', 'THIRD_WEEK', 'FOURTH_WEEK', 'FIFTH_WEEK','LAST_WEEK');
create type billing.billing_month_number as enum ('ONE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEN', 'ELEVEN', 'TWELVE', 'THIRTEEN', 'FOURTEEN', 'FIFTEEN', 'SIXTEEN', 'SEVENTEEN', 'EIGHTEEN', 'NINETEEN', 'TWENTY', 'TWENTYONE', 'TWENTYTWO', 'TWENTYTHREE', 'TWENTYFOUR', 'TWENTYFIVE', 'TWENTYSIX', 'TWENTYSEVEN', 'TWENTYEIGHT', 'TWENTYNINE', 'THIRTY', 'THIRTYONE', 'ALL_DAYS');
create type billing.billing_run_process_stage as enum ('DRAFT', 'DRAFT_DOCUMENT', 'ACCOUNTING');
create type billing.prefix_type as enum ('PRODUCT', 'SERVICE', 'GOODS');
create type billing.billing_max_end_date as enum ('CURRENT_MONTH', 'PREVIOUS_MONTH');
create sequence if not exists billing.billings_id_seq increment by 1;
create table if not exists billing.billings
(
    id                                    integer                            not null primary key default nextval('billing.billings_id_seq'),
    billing_number                        varchar(19)                        not null,
    run_periodicity                       billing.billing_run_periodicity,
    additional_info                       varchar(2048),
    type                                  billing.billing_type               not null,
    status                                billing.billing_status,
    tax_event_date                        date,
    invoice_date                          date,
    account_period_id                     bigint,
--         constraint billings_account_period_fk
--             references billing.account_periods,
    invoice_due_date_type                 billing.billing_invoice_due_date_type,
    invoice_due_date                      date,
    application_model_type                billing.billing_application_model_type[],
    sending_an_invoice                    billing.billing_sending_an_invoice not null,
    execution_date                        timestamp with time zone,
    criteria                              billing.billing_criteria,
    application_level                     billing.billing_application_level,
    customer_contract_or_pod_conditions   text,
    customer_contract_or_pod_list         text,
    create_date                           timestamp with time zone                                default CURRENT_TIMESTAMP not null,
    system_user_id                        varchar(50)                        not null,
    modify_date                           timestamp with time zone,
    modify_system_user_id                 varchar(50),
    run_stage                             billing.billing_run_stage[],
    income_account_number                 varchar(32),
    cost_center_controlling_order         varchar(32),
    vat_rate_id                           integer,
--         constraint billings_vat_rate_fk
--             references nomenclature.vat_rates,
    applicable_interest_rate_id           bigint,
    direct_debit                          boolean,
    bank_id                               integer,
    iban                                  varchar(22),
    basic_for_issuing                     varchar(1024),
    customer_detail_id                    bigint,
--         constraint billings_customer_fk
--             references customer.customer_details,
    goods_order_id                        bigint,
--         constraint billings_goods_order_fk
--             references goods_order.orders,
    service_order_id                      bigint,
--         constraint billings_service_order_fk
--             references service_order.orders,
    contract_billing_group_id_del         bigint,
--         constraint billings_contract_billing_group_fk
--             references product_contract.contract_billing_groups,
    customer_communication_id             bigint,
--         constraint billings_customer_communication_fk
--             references customer.customer_communications,
    product_contract_id                   bigint,
--         constraint billings_product_contract_fk
--             references product_contract.contracts,
    service_contract_id                   bigint,
--         constraint billings_service_contract_fk
--             references service_contract.contracts,
    document_type                         billing.billing_document_type,
    billing_run_process_stage             billing.billing_run_process_stage,
    amount_excluding_vat                  numeric,
    amount_excluding_vat_currency_id      integer,
--         constraint billings_amount_excluding_vat_currency_fk
--             references nomenclature.currencies,
    issuing_for_month_to_current          billing.billing_issuing_for_month_to_current,
    deduction_from                        billing.billing_deduction_from,
    execution_type                        billing.billing_execution_type,
    manual_invoice_type                   billing.billing_manual_invoice_type,
    invoice_type                          billing.billing_invoice_type[],
    global_vat_rate                       boolean,
    invoice_list                          text,
    price_change                          boolean,
    periodicity_created_from_id           bigint,
    periodic_billing_created_from         bigint,
    employee_id                           bigint,
--         constraint billings_account_manager_employee_fk
--             references customer.account_managers,
    prefix_type                           billing.prefix_type,
    max_end_date                          date,
    periodic_max_end_date                 billing.billing_max_end_date,
    periodic_max_end_date_value           integer,
    template_id                           bigint,
    email_template_id                     bigint,
    run_main_data_preparation_status      varchar,
    run_interim_data_preparation_status   varchar,
    run_interim_invoice_generation_status varchar,
    run_main_inovice_generation_status    varchar,
    volume_change                         boolean,
    print_file_name                       text,
    payment_text                       varchar(4097)
);

create sequence if not exists billing.process_periodicity_id_seq increment by 1;
create table billing.process_periodicity
(
    id                             bigint generated by default as identity
        constraint process_periodicity_pk
            primary key,
    name                           varchar(33)                                        not null,
    type                           billing.process_periodicity_type,
    start_after_process_billing_id bigint,
--         constraint process_periodicity_billing_fk
--             references billing.billings,
    ignore_at_runtime              billing.process_periodicity_ignore_at_runtime[],
    excludes                       billing.process_periodicity_exclude[],
    calendar_id                    integer,
--         constraint process_periodicity_calendar_fk
--             references nomenclature.calendars,
    change_to                      billing.process_periodicity_change_to,
    period_type                    billing.process_periodicity_period_type,
    rrule_formula                  varchar(2048),
    year_round                     boolean                                            not null,
    billing_process_start          billing.process_periodicity_billing_process_start,
    billing_process_start_date     timestamp with time zone,
    create_date                    timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                 varchar(50)                                        not null,
    modify_date                    timestamp with time zone,
    modify_system_user_id          varchar(50),
    status                         billing.process_periodicity_status                 not null
);

-- alter table billing.billings
--     add constraint billings_periodicity_created_from_id foreign key (periodicity_created_from_id) references billing.process_periodicity (id);
create sequence if not exists billing.process_periodicity_day_of_months_id_seq increment by 1;
create table billing.process_periodicity_day_of_months
(
    id                     bigint generated by default as identity
        constraint process_periodicity_day_of_months_pk
            primary key,
    process_periodicity_id bigint                                             not null,
--         constraint process_periodicity_day_of_months_fk
--             references billing.process_periodicity,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                                        not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50),
    month                  billing.process_periodicity_month,
    status                 billing.billing_subobject_status                   not null,
    month_number           billing.billing_month_number[]                     not null
);
create sequence if not exists billing.process_periodicity_incompatible_billings_id_seq increment by 1;
create table billing.process_periodicity_incompatible_billings
(
    id                      bigint generated by default as identity
        constraint process_periodicity_incompatible_billings_pk
            primary key,
    process_periodicity_id  bigint                                             not null,
--         constraint process_periodicity_inc_bill_periodicity_fk
--             references billing.process_periodicity,
    incompatible_billing_id bigint                                             not null,
--         constraint process_periodicity_inc_bill_billing_fk
--             references billing.billings,
    create_date             timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id          varchar(50)                                        not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50),
    status                  billing.billing_subobject_status                   not null
);
create sequence if not exists billing.process_periodicity_issuing_periods_id_seq increment by 1;
create table billing.process_periodicity_issuing_periods
(
    id                     bigint generated by default as identity
        constraint process_periodicity_issuing_periods_pk
            primary key,
    period_from            varchar(5)                                         not null,
    period_to              varchar(5)                                         not null,
    process_periodicity_id bigint                                             not null,
--         constraint process_periodicity_issuing_periods_process_periodicity_fk
--             references billing.process_periodicity,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                                        not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50),
    status                 billing.billing_subobject_status                   not null
);
create sequence if not exists billing.process_periodicity_period_of_year_id_seq increment by 1;
create table billing.process_periodicity_period_of_year
(
    id                     bigint generated by default as identity
        constraint process_periodicity_period_of_year_pk
            primary key,
    process_periodicity_id bigint                                             not null,
--         constraint process_periodicity_period_of_year_process_periodicity_fk
--             references billing.process_periodicity,
    week                   billing.process_periodicity_week                   not null,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                                        not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50),
    day                    billing.process_periodicity_day[],
    status                 billing.billing_subobject_status                   not null
);
create sequence if not exists billing.process_periodicity_start_time_intervals_id_seq increment by 1;
create table billing.process_periodicity_time_intervals
(
    id                     bigint generated by default as identity
        constraint process_periodicity_time_intervals_pk
            primary key,
    start_time             time with time zone                                not null,
    end_time               time with time zone,
    process_periodicity_id bigint                                             not null,
--         constraint process_periodicity_time_intervals_process_periodicity_fk
--             references billing.process_periodicity,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                                        not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50),
    status                 billing.billing_subobject_status                   not null
);

create sequence if not exists billing.billing_process_periodicity_id_seq increment by 1;
create table if not exists billing.billing_process_periodicity
(
    id                     bigint generated by default as identity
        constraint billing_process_periodicity_pk
            primary key,
    billing_id             bigint                                             not null,
--         constraint billing_process_periodicity_billing_fk
--             references billing.billings,
    process_periodicity_id bigint                                             not null,
--         constraint billing_process_periodicity_fk
--             references billing.process_periodicity,
    status                 billing.billing_subobject_status                   not null,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                                        not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50)
);

DROP TABLE IF EXISTS nomenclature.pod_measurement_types;
create sequence if not exists nomenclature.pod_measurement_types_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.pod_measurement_types
(
    id                    integer generated by default as identity
        constraint pod_measurement_types_pk
            primary key,
    name                  varchar(512)                                       not null,
    grid_operator_id      integer                                            not null,
    is_default            boolean                                            not null,
    system_user_id        varchar(50)                                        not null,
    status                nomenclature.status_enum                           not null,
    ordering_id           integer                                            not null
        constraint pod_measurement_types_ordering_uk
            unique
                deferrable initially deferred,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    measurement_type_xenergie_export text
);

create sequence product_contract.contract_interim_advance_payments_id_seq;
create table product_contract.contract_interim_advance_payments
(
    id                         bigint                                     NOT NULL DEFAULT nextval('product_contract.contract_interim_advance_payments_id_seq'),

    value                      numeric,
    interim_advance_payment_id bigint                                     not null,

    create_date                timestamp with time zone                            default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                                not null,
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    contract_detail_id         bigint                                     not null,
    status                     product_contract.contract_subobject_status not null,
    issue_date                 varchar(32),
    term_value                 integer
);

create table if not exists process_management.process_contract_pods
(
    id                       bigint generated by default as identity
        constraint process_contract_pods_pk
            primary key,
    contract_pod_id          bigint                                             not null,
--         constraint process_contract_pods_fk
--             references product_contract.contract_pods,
    create_date              timestamp with time zone default CURRENT_TIMESTAMP not null,
    processed_record_info_id bigint                                             not null
--         constraint process_contract_pods_record_info_fk
--             references process_management.processed_record_info
);

create schema if not exists invoice;
create sequence if not exists invoice.invoices_id_seq increment by 1;
create type invoice.invoice_document_type as enum ('INVOICE', 'DEBIT_NOTE', 'CREDIT_NOTE', 'PROFORMA_INVOICE');
create type invoice.invoice_status as enum ('DRAFT', 'REAL', 'CANCELLED','DRAFT_GENERATED');
create type invoice.invoice_type as enum ('MANUAL', 'STANDARD', 'INTERIM_AND_ADVANCE_PAYMENT', 'CORRECTION', 'RECONNECTION', 'REVERSAL');
create table if not exists invoice.invoices
(
    id                                           bigint                        NOT NULL PRIMARY KEY DEFAULT nextval('invoice.invoices_id_seq'),
    invoice_number                               varchar(24),
    invoice_date                                 date                          not null,
    status                                       invoice.invoice_status        not null,
    tax_event_date                               date,
    payment_deadline                             date,
    type                                         invoice.invoice_type          not null,
    meter_reading_period_from                    date,
    meter_reading_period_to                      date,
    basis_for_issuing                            varchar(1024)                 not null,
    income_account_number                        varchar(32),
    cost_center_controlling_order                varchar(32),
    applicable_interest_rate_id                  bigint,
    bank_id                                      integer,
    iban                                         varchar(22),
    direct_debit                                 boolean,
    customer_detail_id                           bigint,
    goods_order_id                               bigint,
    service_order_id                             bigint,
    contract_billing_group_id                    bigint,
    product_contract_id                          bigint,
    service_contract_id                          bigint,
    billing_id                                   bigint,
    create_date                                  timestamp with time zone                           default CURRENT_TIMESTAMP not null,
    system_user_id                               varchar(50)                   not null,
    modify_date                                  timestamp with time zone,
    modify_system_user_id                        varchar(50),
    customer_communication_id                    bigint,
    invoice_cancelation_id                       bigint,
    currency_id                                  bigint,
    document_type                                invoice.invoice_document_type not null,
    currency_exchange_rate_on_invoice_creation   numeric,
    product_detail_id                            bigint,
    service_detail_id                            bigint,
    service_contract_detail_id                   bigint,
    product_contract_detail_id                   bigint,
    alternative_recipient_customer_detail_id     bigint,
    account_period_id                            bigint,
    total_amount_excluding_vat                   numeric,
    total_amount_of_vat                          numeric,
    total_amount_including_vat                   numeric,
    total_amount_including_vat_in_other_currency numeric,
    no_interest_on_overdue_debts                 boolean,
    standard_invoice_id                          bigint,
    reversal_created_from_id                     bigint,
    customer_id                                  bigint,
    product_id                                   bigint,
    service_id                                   bigint,
    pod_id                                       bigint,
    is_deducted                                  boolean,
    is_interim_match_invoice                     boolean,
    interim_calculated_from_invoice_id           bigint,
    deducted_for_invoice_id                      bigint,
    issuing_for_the_month                        date,
    issuing_for_payment_term_date                date,
    invoice_slot                                 varchar,
    deduction_from_type                          interim_advance_payment.iap_deduction_from,
    contract_type                                varchar,
    currency_id_in_other_currency                bigint,
    total_amount_excluding_vat_in_other_currency numeric,
    total_amount_of_vat_in_other_currency        numeric,
    contract_communication_id                    bigint,
    processor_row_id                             bigint,
    template_detail_id                           bigint,
    invoice_cancellation_number                  varchar(24),
    invoice_document_id                          bigint,
    has_with_electricity_invoice_detail          boolean                                            default false,
    compensation_index                           integer,
    total_actual_consumption                     numeric,
    has_one_time_on_time_invoice_detail          boolean,
    price_was_changed                            boolean,
    current_status_change_date                   timestamp,
    parent_invoice_id                            bigint,
    invoice_number_modify_date                   timestamp with time zone,
    total_actual_consumption_amount              numeric
);

create sequence if not exists invoice.invoice_detailed_data_id_seq increment by 1;
create table if not exists invoice.invoice_detailed_data
(
    id                                bigint NOT NULL          DEFAULT nextval('invoice.invoice_detailed_data_id_seq'),
    price_component_id                bigint,
    pod_id                            bigint,
    unrecognized_pod                  varchar,
    period_from                       date,
    period_to                         date,
    meter_number                      varchar(32),
    new_meter_reading                 numeric,
    old_meter_reading                 numeric,
    difference                        numeric,
    multiplier                        numeric,
    correction                        numeric,
    deducted                          numeric,
    total_volumes                     numeric,
    measures_unit_for_total_volumes   integer,
    unit_price                        numeric,
    measure_unit_for_unit_price       integer,
    value                             numeric,
    measure_unit_for_value            integer,
    income_account_number             varchar(32),
    cost_center_controlling_order     varchar(32),
    vat_rate_percent                  numeric,
    create_date                       timestamp with time zone default CURRENT_TIMESTAMP not null,
    invoice_id                        bigint not null,
    vat_rate_id                       integer,
    good_name                         varchar(32),
    measure_unit_for_total_volumes_go bigint,
    measure_unit_for_unit_price_go    bigint,
    measure_unit_for_value_go         bigint,
    measure_unit_for_total_volumes_so bigint,
    pc_group_detail_id                bigint,
    currency_exchange_rate            numeric
);
create sequence if not exists invoice.invoice_vat_rate_values_id_seq increment by 1;
create table if not exists invoice.invoice_vat_rate_values
(
    id                    bigint      not null     default nextval('invoice.invoice_vat_rate_values_id_seq'),
    vat_rate_percent      numeric     not null,
    amount_excluding_vat  numeric     not null,
    value_of_vat          numeric     not null,
    invoice_id            bigint      not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50) not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create type invoice.invoice_standard_detail_type as enum ('PER_PIECE', 'OVER_TIME_ONE_TIME', 'OVER_TIME_PERIODICAL', 'WITH_ELECTRICITY', 'DISCOUNT', 'SCALE', 'SETTLEMENT', 'INTERIM_DEDUCTION', 'INTERIM_EXACT_AMOUNT', 'INTERIM_PRICE_COMPONENT', 'INTERIM_PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT');
create table if not exists invoice.invoice_standard_detailed_data_vat_base
(
    id                                         bigint generated always as identity,
    invoice_id                                 bigint                               not null,
    detail_type                                invoice.invoice_standard_detail_type not null,
    alt_invoice_recipient_customer_detail_id   bigint,
    pc_id                                      bigint,
    pod_id                                     bigint,
    date_from                                  date,
    date_to                                    date,
    customer_detail_id                         bigint,
    product_contract_detail_id                 bigint,
    service_contract_detail_id                 bigint,
    service_detail_id                          bigint,
    product_detail_id                          bigint,
    total_volumes                              numeric,
    unit_price                                 numeric,
    main_currency_total_amount_without_vat     numeric,
    main_currency_total_amount_with_vat        numeric,
    main_currency_total_amount_vat             numeric,
    main_currency_id                           bigint,
    alt_currency_total_amount_without_vat      numeric,
    alt_currency_total_amount_with_vat         numeric,
    alt_currency_total_amount_vat              numeric,
    alt_currency_id                            bigint,
    original_currency_total_amount_without_vat numeric,
    original_currency_total_amount_with_vat    numeric,
    original_currency_total_amount_vat         numeric,
    original_currency_id                       numeric,
    vat_rate_id                                bigint,
    vat_rate_percent                           numeric,
    new_meter_reading                          numeric,
    old_meter_reading                          numeric,
    difference                                 numeric,
    multiplier                                 numeric,
    correction                                 numeric,
    deducted                                   numeric,
    measures_unit_for_total_volumes            integer,
    measure_unit_for_unit_price                integer,
    income_account_number                      varchar(32),
    cost_center_controlling_order              varchar(32),
    create_date                                timestamp with time zone default CURRENT_TIMESTAMP,
    modify_system_user_id                      varchar(50),
    modify_date                                timestamp with time zone,
    system_user_id                             varchar(50),
    tariff                                     boolean,
    pc_group_detail_id                         bigint,
    meter_id                                   bigint,
    unrecognized_pod                           varchar(500),
    scale_id                                   integer
);

create table if not exists invoice.invoice_total_actual_consumption
(
    id                       bigint generated always as identity,
    invoice_id               bigint,
    pod_id                   bigint,
    total_actual_consumption numeric
);

DROP TABLE IF EXISTS nomenclature.risk_assessments;
create sequence nomenclature.risk_assessments_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.risk_assessments
(
    id                    integer                  NOT NULL primary key DEFAULT nextval('nomenclature.risk_assessments_id_seq'),
    name                  varchar(512)             not null,
    is_default            boolean                  not null,
    create_date           timestamp with time zone not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status      not null,
    ordering_id           integer                  not null
        constraint risk_assessments_uk1
            unique
                deferrable initially deferred
);
alter sequence nomenclature.risk_assessments_id_seq owned by nomenclature.risk_assessments.id;

create sequence if not exists billing.billing_billing_groups_id_seq increment by 1;
create table if not exists billing.billing_contract_billing_groups
(
    id                        integer                          not null primary key default nextval('billing.billing_billing_groups_id_seq'),
    contract_billing_group_id bigint                           not null,
--         constraint billing_contract_billing_groups_fk
--             references product_contract.contract_billing_groups,
    billing_id                bigint                           not null,
--         constraint billing_contract_billing_groups_billing_fk
--             references billing.billings,
    status                    billing.billing_subobject_status not null,
    create_date               timestamp with time zone                              default CURRENT_TIMESTAMP not null,
    system_user_id            varchar(50)                      not null,
    modify_date               timestamp with time zone,
    modify_system_user_id     varchar(50)
);

create sequence if not exists billing.billing_summary_data_id_seq increment by 1;
create table if not exists billing.billing_summary_data
(
    id                                        integer     not null primary key default nextval('billing.billing_summary_data_id_seq'),
    price_component_or_price_component_groups varchar(1024),
    total_volumes                             numeric,
    measures_unit_for_total_volumes           varchar(512),
    unit_price                                numeric,
    measure_unit_for_unit_price               varchar(512),
    value                                     numeric,
    value_currency_id                         integer,
--         constraint billing_summary_data_currency_fk
--             references nomenclature.currencies,
    income_account                            varchar(32),
    cost_center                               varchar(32),
    vat_rate_id                               integer,
--         constraint billing_summary_data_vat_rate_fk
--             references nomenclature.vat_rates,
    create_date                               timestamp with time zone         default CURRENT_TIMESTAMP not null,
    system_user_id                            varchar(50) not null,
    modify_date                               timestamp with time zone,
    modify_system_user_id                     varchar(50),
    global_vat_rate                           boolean,
    billing_id                                bigint      not null
--         constraint billing_summary_data_billing_fk
--             references billing.billings
);

create sequence if not exists billing.billing_detailed_data_id_seq increment by 1;
create table if not exists billing.billing_detailed_data
(
    id                              integer     not null primary key default nextval('billing.billing_detailed_data_id_seq'),
    price_component                 varchar(1024),
    pod                             varchar(33),
    period_from                     date,
    period_to                       date,
    meter                           varchar(32),
    new_meter_reading               numeric,
    old_meter_reading               numeric,
    differences                     numeric,
    multiplier                      numeric,
    correction                      numeric,
    deducted                        numeric,
    total_volumes                   numeric,
    measures_unit_for_total_volumes varchar(512),
    unit_price                      numeric,
    measure_unit_for_unit_price     varchar(512),
    value                           numeric,
    value_currency_id               integer,
--         constraint billing_detailed_data_currency_fk
--             references nomenclature.currencies,
    income_account                  varchar(32),
    cost_center                     varchar(32),
    vat_rate_id                     integer,
--         constraint billing_detailed_data_vat_rate_fk
--             references nomenclature.vat_rates,
    create_date                     timestamp with time zone         default CURRENT_TIMESTAMP not null,
    system_user_id                  varchar(50) not null,
    modify_date                     timestamp with time zone,
    modify_system_user_id           varchar(50),
    billing_id                      bigint      not null,
--         constraint billing_detailed_data_billing_fk
--             references billing.billings,
    global_vat_rate                 boolean,
    constraint billing_detailed_data_period_chk
        check (period_from <= period_to)
);

CREATE TYPE price_component.pc_status as enum ('ACTIVE', 'DELETED');
CREATE TABLE IF NOT EXISTS price_component.profile_for_balancing
(
    id                    bigint generated by default as identity
        constraint profile_for_balancing_pk
            primary key,
    name                  varchar(50)                                        not null,
    status                price_component.pc_status                          not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists billing.billing_tasks_id_seq increment by 1;
create table if not exists billing.billing_tasks
(
    id                    integer primary key              not null DEFAULT nextval('billing.billing_tasks_id_seq'),
    billing_id            bigint                           not null,
    task_id               bigint                           not null,
    status                billing.billing_subobject_status not null,
    create_date           timestamp with time zone                  default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists billing.billing_invoices_id_seq increment by 1;
create table billing.billing_invoices
(
    id                    bigint generated by default as identity
        constraint billing_invoices_pk
            primary key,
    billing_id            bigint                                             not null,
--         constraint billing_invoices_billing_fk
--             references billing.billings,
    invoice_id            bigint                                             not null,
--         constraint billing_invoices_fk
--             references invoice.invoices,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                billing.billing_subobject_status                   not null
);


CREATE TABLE IF NOT EXISTS product.product_additional_params
(
    id                    bigint generated by default as identity (maxvalue 2147483647)
        constraint product_additional_params_pk
            primary key,
    label                 varchar(1024),
    value                 varchar(1024),
    product_detail_id     bigint                                             not null,
--         constraint product_additional_params_product_detail_fk
--             references product.product_details,
    ordering_id           bigint                                             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    constraint product_additional_params_product_order_uk
        unique (product_detail_id, ordering_id)
            deferrable initially deferred
);

CREATE TABLE IF NOT EXISTS service.service_additional_params
(
    id                    bigint generated by default as identity (maxvalue 2147483647)
        constraint service_additional_params_pk
            primary key,
    label                 varchar(1024),
    value                 varchar(1024),
    service_detail_id     bigint                                             not null,
--         constraint service_additional_params_service_detail_fk
--             references service.service_details,
    ordering_id           bigint                                             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    constraint service_additional_params_service_order_uk
        unique (service_detail_id, ordering_id)
            deferrable initially deferred
);

create sequence if not exists billing.billing_invoice_files_id_seq increment by 1;
create table if not exists billing.billing_invoice_files
(
    id                    integer primary key              not null DEFAULT nextval('billing.billing_invoice_files_id_seq'),
    name                  varchar(200)                     not null,
    file_url              varchar(256)                     not null,
    billing_id            bigint,
--         constraint billing_invoice_files_billing_fk
--             references billing.billings,
    create_date           timestamp with time zone                  default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                      not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                billing.billing_subobject_status not null
);
create type terms.penalty_subobject_status as enum ('ACTIVE', 'DELETED');
create table terms.penalty_action_types
(
    id                    bigint generated by default as identity (maxvalue 2147483647)
        constraint penalty_action_types_pk
            primary key,
    penalty_id            bigint                                             not null,
--         constraint penalty_action_types_penalty_id_fk
--             references terms.penalties,
    action_type_id        integer                                            not null,
--         constraint penalty_action_types_fk
--             references nomenclature.action_types,
    status                terms.penalty_subobject_status                     not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create schema if not exists receivable;

DROP TABLE IF EXISTS nomenclature.blocking_reasons;
create sequence nomenclature.blocking_reasons_id_seq increment by 1;
create type nomenclature.blocking_reason_type as enum ('ALL', 'BLOCKED_FOR_PAYMENT', 'BLOCKED_FOR_REMINDER_LETTERS', 'BLOCKED_FOR_CALC_LATE_PAYMENT_FINES_INTERESTS', 'BLOCKED_FOR_LIABILITIES_OFFSETTING', 'BLOCKED_FOR_SUPPLY_TERMINATION');
CREATE TABLE IF NOT EXISTS nomenclature.blocking_reasons
(
    id                    integer generated by default as identity
        constraint blocking_reasons_pk
            primary key,
    name                  varchar(2048)                                      not null,
    ordering_id           integer                                            not null
        constraint blocking_reasons_ordering_uk
            unique
                deferrable initially deferred,
    is_default            boolean                                            not null,
    system_user_id        varchar(50)                                        not null,
    status                nomenclature.status_enum                           not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    reason_type           nomenclature.blocking_reason_type[]                not null,
    is_hard_coded         boolean
);
alter sequence nomenclature.blocking_reasons_id_seq owned by nomenclature.blocking_reasons.id;

DROP TABLE IF EXISTS receivable.mass_operation_for_blocking;
create sequence receivable.mass_operation_for_blocking_id_seq increment by 1;
create type receivable.mass_operation_for_blocking_type as enum ('CUSTOMER_LIABILITY', 'CUSTOMER_RECEIVABLE', 'PAYMENT');
create type receivable.mass_operation_for_blocking_status as enum ('DRAFT', 'EXECUTED');
create type receivable.general_status as enum ('ACTIVE', 'DELETED');
create type receivable.mass_operation_for_blocking_customer_condition_type as enum ('ALL_CUSTOMERS', 'CUSTOMERS_UNDER_CONDITIONS', 'LIST_OF_CUSTOMERS');
CREATE TABLE IF NOT EXISTS receivable.mass_operation_for_blocking
(
    id                                                         bigint generated by default as identity
        constraint mass_operation_for_blocking_pk
            primary key,
    name                                                       varchar(512)                                                   not null,
    type                                                       receivable.mass_operation_for_blocking_type[]                  not null,
    mass_operation_blocking_status                             receivable.mass_operation_for_blocking_status                  not null,
    customer_condition_type                                    receivable.mass_operation_for_blocking_customer_condition_type not null,
    customer_conditions                                        varchar(4096),
    list_of_customers                                          varchar(8192),
    exclusion_by_amount_less_than                              integer,
    exclusion_by_amount_greater_than                           integer,
    currency_id                                                integer,
--         constraint mass_operation_for_blocking_currencies_fk
--             references nomenclature.currencies,
    blocked_for_payment                                        boolean,
    blocked_for_payment_from_date                              date,
    blocked_for_payment_to_date                                date,
    blocked_for_payment_blocking_reason_id                     integer,
--         constraint mass_op_blck_blocked_for_payment_blocking_reason_id_fk
--             references nomenclature.blocking_reasons,
    blocked_for_payment_additional_info                        varchar(2048),
    blocked_for_reminder_letters                               boolean,
    blocked_for_reminder_letters_from_date                     date,
    blocked_for_reminder_letters_to_date                       date,
    blocked_for_reminder_letters_blocking_reason_id            integer,
--         constraint mass_op_blck_blocked_for_reminder_letters_blocking_reasons_fk
--             references nomenclature.blocking_reasons,
    blocked_for_reminder_letters_additional_info               varchar(2048),
    blocked_for_calculation_of_late_payment                    boolean,
    blocked_for_calculation_of_late_payment_from_date          date,
    blocked_for_calculation_of_late_payment_to_date            date,
    blocked_for_calculation_of_late_payment_blocking_reason_id integer,
--         constraint mass_op_blck_blocked_for_calc_of_late_payment_blocking_reason_
--             references nomenclature.blocking_reasons,
    blocked_for_calculation_of_late_payment_additional_info    varchar(2048),
    blocked_for_liabilities_offsetting                         boolean,
    blocked_for_liabilities_offsetting_from_date               date,
    blocked_for_liabilities_offsetting_to_date                 date,
    blocked_for_liabilities_offsetting_blocking_reason_id      integer,
--         constraint mass_op_blck_blocked_for_liab_offsetting_blocking_reasons_fk
--             references nomenclature.blocking_reasons,
    blocked_for_liabilities_offsetting_additional_info         varchar(2048),
    blocked_for_supply_termination                             boolean,
    blocked_for_supply_termination_from_date                   date,
    blocked_for_supply_termination_to_date                     date,
    blocked_for_supply_termination_blocking_reason_id          integer,
--         constraint mass_op_blck_blocked_for_supply_termination_blocking_reasons_f
--             references nomenclature.blocking_reasons,
    blocked_for_supply_termination_additional_info             varchar(2048),
    status                                                     receivable.general_status                                      not null,
    create_date                                                timestamp with time zone default CURRENT_TIMESTAMP             not null,
    system_user_id                                             varchar(50)                                                    not null,
    modify_date                                                timestamp with time zone,
    modify_system_user_id                                      varchar(50)
);
alter sequence receivable.mass_operation_for_blocking_id_seq owned by receivable.mass_operation_for_blocking.id;

DROP TABLE IF EXISTS receivable.mass_operation_for_blocking_exclution_prefixes;
create type receivable.receivable_subobject_status as enum ('ACTIVE', 'DELETED');
create sequence receivable.mass_operation_for_blocking_exclution_prefixes_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS receivable.mass_operation_for_blocking_exclution_prefixes
(
    id                             bigint generated by default as identity
        constraint mass_operation_for_blocking_exclution_prefixes_pk
            primary key,
    mass_operation_for_blocking_id bigint                                             not null,
--     constraint mass_operation_for_blocking_exclution_fk
--     references mass_operation_for_blocking,
    prefix_id                      bigint                                             not null,
--         constraint mass_operation_for_blocking_exclution_prefixes_fk
--             references nomenclature.prefixes,
    status                         receivable.receivable_subobject_status             not null,
    create_date                    timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                 varchar(50)                                        not null,
    modify_date                    timestamp with time zone,
    modify_system_user_id          varchar(50)
);
alter sequence receivable.mass_operation_for_blocking_exclution_prefixes_id_seq owned by receivable.mass_operation_for_blocking_exclution_prefixes.id;

create schema if not exists receivable;
create type receivable.customer_payment_outgoing_doc_type as enum ('INVOICE', 'LATE_PAYMENT_FINE', 'DEPOSIT', 'PENALTY');
create table receivable.customer_payments
(
    id                                        bigint generated by default as identity
        constraint customer_payments_pk
            primary key,
    payment_number                            varchar(24)                                        not null,
    payment_date                              date                                               not null,
    full_offset_date                          date                                               not null,
    initial_amount                            numeric                                            not null,
    current_amount                            numeric                                            not null,
    currency_id                               integer                                            not null,
--         constraint customer_payments_curreny_fk
--             references nomenclature.currencies,
    collection_channel_id                     bigint                                             not null,
    payment_package_id                        bigint                                             not null,
    account_period_id                         bigint                                             not null,
    payment_purpose                           varchar(2048),
    payment_info                              varchar(2048),
    blocked_for_offsetting                    boolean,
    blocked_for_offsetting_from_date          date,
    blocked_for_offsetting_to_date            date,
    blocked_for_offsetting_blocking_reason_id integer,
    blocked_for_offsetting_additional_info    varchar(2048),
    customer_id                               bigint                                             not null,
--         constraint customer_payments_customer_fk
--             references customer.customers,
    contract_billing_group_id                 bigint,
--         constraint customer_payments_contract_billing_group_fk
--             references product_contract.contract_billing_groups,
    outgoing_document_type                    receivable.customer_payment_outgoing_doc_type,
    invoice_id                                bigint,
--         constraint customer_payments_invoice_fk
--             references invoice.invoices,
    late_payment_fine_id                      bigint,
    customer_deposit_id                       bigint,
    penalty_id                                bigint,
--         constraint customer_payments_penalty_fk
--             references terms.penalties,
    status                                    receivable.general_status                          not null,
    create_date                               timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                            varchar(50)                                        not null,
    modify_date                               timestamp with time zone,
    modify_system_user_id                     varchar(50)
);

DROP TABLE IF EXISTS nomenclature.collection_partners;
create sequence nomenclature.collection_partners_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.collection_partners
(
    id                    integer                  NOT NULL primary key DEFAULT nextval('nomenclature.collection_partners_id_seq'),
    name                  varchar(1024)            not null,
    is_default            boolean                  not null,
    create_date           timestamp with time zone not null,
    system_user_id        varchar(50)              not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                nomenclature_status      not null,
    ordering_id           integer                  not null
        constraint collection_partners_uk1
            unique
                deferrable initially deferred
);
alter sequence nomenclature.collection_partners_id_seq owned by nomenclature.collection_partners.id;
create table if not exists service_contract.contract_service_additional_params
(
    id                          bigint generated by default as identity (maxvalue 2147483647)
        constraint contract_additional_params_pk
            primary key,
    label                       varchar(1024),
    value                       varchar(1024),
    service_additional_param_id bigint                                             not null,
--         constraint contract_service_additional_params_fk
--             references service.service_additional_params,
    contract_detail_id          bigint                                             not null,
--         constraint contract_service_additional_params_contract_fk
--             references service_contract.contract_details,
    create_date                 timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                                        not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50)
);
create table if not exists product_contract.contract_product_additional_params
(
    id                          bigint generated by default as identity (maxvalue 2147483647)
        constraint contract_product_additional_params_pk
            primary key,
    label                       varchar(1024),
    value                       varchar(1024),
    product_additional_param_id bigint                                             not null,
--         constraint contract_product_additional_params_fk
--             references product.product_additional_params,
    contract_detail_id          bigint                                             not null,
--         constraint contract_product_additional_params_contract_fk
--             references product_contract.contract_details,
    create_date                 timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                                        not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50)
);
create sequence nomenclature.balancing_group_coordinator_grounds_id_seq;
create table if not exists nomenclature.balancing_group_coordinator_grounds
(
    id                    integer generated by default as identity
        constraint balancing_group_coordinator_grounds_pk
            primary key,
    name                  varchar(1024)                                      not null,
    is_default            boolean                                            not null,
    ordering_id           integer                                            not null
        constraint balancing_group_coordinator_grounds_ordering_uk
            unique
                deferrable initially deferred,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    status                nomenclature.status_enum
);
create sequence nomenclature.grounds_for_objection_withdrawal_to_change_of_cbg_id_seq;
create table nomenclature.grounds_for_objection_withdrawal_to_change_of_cbg
(
    id                    integer generated by default as identity
        constraint grounds_for_objection_withdrawal_to_change_of_cbg_pk
            primary key,
    name                  varchar(1024)                                      not null,
    is_default            boolean                                            not null,
    ordering_id           integer                                            not null
        constraint grounds_for_objection_withdrawal_to_change_of_cbg_ordering_uk
            unique
                deferrable initially deferred,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    status                nomenclature.status_enum
);
create sequence nomenclature.cancelation_reasons_id_seq;
create table nomenclature.cancelation_reasons
(
    id                    integer generated by default as identity
        constraint cancelation_reasons_pk
            primary key,
    name                  varchar(1024)                       not null,
    ordering_id           integer                             not null
        constraint cancelation_reasons_ordering_uk
            unique
                deferrable initially deferred,
    is_default            boolean                             not null,
    system_user_id        varchar(50)                         not null,
    status                nomenclature.status_enum            not null,
    create_date           timestamp default CURRENT_TIMESTAMP not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50)
);
create type receivable.collection_channel_type as enum ('OFFLINE', 'ONLINE');
create type receivable.collection_channels_customer_condition_type as enum ('ALL_CUSTOMERS', 'CUSTOMERS_UNDER_CONDITIONS', 'LIST_OF_CUSTOMERS');
create type receivable.collection_channel_file_type as enum ('PAYMENT_PARTNER', 'BANK_PARTNER');
create sequence if not exists receivable.collection_channels_id_seq;
create type billing.performer_type as enum ('TAG', 'MANAGER');

create table if not exists receivable.collection_channels
(
    id                                         bigint generated by default as identity
        constraint collection_channels_pk
            primary key,
    name                                       varchar(512)                                           not null,
    type                                       receivable.collection_channel_type                     not null,
    employee_group_for_error_notification      bigint,
    collection_partner_id                      integer                                                not null,
--         constraint collection_channels_collection_partner_fk
--             references nomenclature.collection_partners,
    income_account_number                      varchar(512)                                           not null,
    currency_id                                integer                                                not null,
--         constraint collection_channels_currency_fk
--             references nomenclature.currencies,
    customer_condition_type                    receivable.collection_channels_customer_condition_type not null,
    customer_conditions                        varchar(4096),
    list_of_customers                          varchar(8192),
    exclude_liabilities_by_amount_less_than    integer,
    exclude_liabilities_by_amount_greater_than integer,
    file_type                                  receivable.collection_channel_file_type,
    data_sending_schedule                      varchar(2048),
    data_receiving_schedule                    varchar(2048),
    number_of_working_days_for_waiting_payment integer,
    global_bank                                boolean,
    calendar_id                                integer,
--         constraint collection_channels_calendar_fk
--             references nomenclature.calendars,
    waiting_period_tolerance_in_hours          integer,
    folder_for_file_receiving                  varchar(2048),
    folder_for_file_sending                    varchar(2048),
    email_for_file_sending                     varchar(2048),
    status                                     receivable.general_status                              not null,
    create_date                                timestamp with time zone default CURRENT_TIMESTAMP     not null,
    system_user_id                             varchar(50)                                            not null,
    modify_date                                timestamp with time zone,
    modify_system_user_id                      varchar(50),
    waiting_period_time                        timestamp,
    notification_employee_id                   bigint,
    notification_tag_id                        bigint,
    performer_type                             billing.performer_type,
    combine_liabilities                        boolean
);

create sequence if not exists receivable.collection_channel_exclude_liab_prefixes_id_seq;
create table if not exists receivable.collection_channel_exclude_liab_prefixes
(
    id                    bigint generated by default as identity
        constraint collection_channel_exclude_liab_by_prefix_pk
            primary key,
    collection_channel_id bigint                                             not null,
--         constraint collection_channel_exclude
--             references receivable.collection_channels,
    prefix_id             bigint                                             not null,
--         constraint collection_channel_exclude_liab_by_prefix_fk
--             references nomenclature.prefixes,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists receivable.collection_channel_priority_liab_prefixes_id_seq;
create table if not exists receivable.collection_channel_priority_liab_prefixes
(
    id                    bigint generated by default as identity
        constraint collection_channel_priority_liab_by_prefix_pk
            primary key,
    collection_channel_id bigint                                             not null,
--         constraint collection_channel_priority
--             references receivable.collection_channels,
    prefix_id             bigint                                             not null,
--         constraint collection_channel_priority_liab_by_prefix_fk
--             references nomenclature.prefixes,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists receivable.collection_channel_banks_id_seq;
create table if not exists receivable.collection_channel_banks
(
    id                    bigint generated by default as identity
        constraint collection_channel_banks_pk
            primary key,
    collection_channel_id bigint                                             not null,
--         constraint collection_channel_fk
--             references receivable.collection_channels,
    bank_id               bigint                                             not null,
--         constraint collection_channel_banks_fk
--             references nomenclature.banks,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create type receivable.objection_withdrawal_to_change_of_cbg_status as enum ('DRAFT', 'IN_PROGRESS', 'SEND');
create sequence if not exists receivable.objection_withdrawal_to_change_of_cbg_id_seq;
create table if not exists receivable.objection_withdrawal_to_change_of_cbg
(
    id                              bigint generated by default as identity
        constraint objection_withdrawal_to_change_of_cbg_pk
            primary key,
    withdrawal_change_of_cbg_number varchar(24)                                             not null,
    change_of_cbg_id                bigint                                                  not null,
    email_template_id               bigint                                                  not null,
    withdrawal_change_of_cbg_status receivable.objection_withdrawal_to_change_of_cbg_status not null,
    create_date                     timestamp default CURRENT_TIMESTAMP                     not null,
    system_user_id                  varchar(50)                                             not null,
    modify_date                     timestamp,
    modify_system_user_id           varchar(50),
    status                          receivable.general_status                               not null
);
create sequence if not exists receivable.objection_withdrawal_to_change_of_cbg_doc_templates_id_seq;
create table if not exists receivable.objection_withdrawal_to_change_of_cbg_doc_templates
(
    id                          bigint generated by default as identity
        constraint objection_withdrawal_to_change_of_cbg_doc_templates_pk
            primary key,
    withdrawal_change_of_cbg_id bigint                                             not null,
    template_id                 bigint                                             not null,
    status                      receivable.receivable_subobject_status             not null,
    create_date                 timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                                        not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50)
);
create sequence if not exists receivable.objection_withdrawal_to_change_of_cbg_email_templates_id_seq;
create table if not exists receivable.objection_withdrawal_to_change_of_cbg_email_templates
(
    id                          bigint generated by default as identity
        constraint objection_withdrawal_to_change_of_cbg_email_templates_pk
            primary key,
    withdrawal_change_of_cbg_id bigint                                             not null,
    template_id                 bigint                                             not null,
    status                      receivable.receivable_subobject_status             not null,
    create_date                 timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                                        not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50)
);
create sequence if not exists receivable.objection_withdrawal_to_change_of_cbg_tasks_id_seq;
create table if not exists receivable.objection_withdrawal_to_change_of_cbg_tasks
(
    id                          bigint generated by default as identity
        constraint objection_withdrawal_to_change_of_cbg_tasks_pk
            primary key,
    withdrawal_change_of_cbg_id bigint                                             not null,
    task_id                     bigint                                             not null,
    status                      receivable.receivable_subobject_status             not null,
    create_date                 timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id              varchar(50)                                        not null,
    modify_date                 timestamp with time zone,
    modify_system_user_id       varchar(50)
);
create sequence if not exists receivable.objection_withdrawal_to_change_of_cbg_process_results_id_seq;
create table if not exists receivable.objection_withdrawal_to_change_of_cbg_process_results
(
    id                                             bigint generated by default as identity
        constraint objection_withdrawal_to_change_of_cbg_process_results_pk
            primary key,
    customer_id                                    bigint                              not null,
    pod_id                                         bigint                              not null,
    grounds_for_obj_withdrawal_to_change_of_cbg_id integer                             not null,
    overdue_amount_for_contract                    numeric                             not null,
    overdue_amount_for_billing_group               numeric                             not null,
    overdue_amount_for_pod                         numeric                             not null,
    balancing_group_coordinator_ground_id          integer                             not null,
    is_checked                                     boolean                             not null,
    create_date                                    timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                                 varchar(50)                         not null,
    modify_date                                    timestamp,
    modify_system_user_id                          varchar(50),
    change_withdrawal_of_cbg_id                    bigint                              not null
);
create sequence if not exists nomenclature_customer_assessment_types_id_seq;
create table if not exists nomenclature.customer_assessment_types
(
    id                    integer generated by default as identity
        constraint customer_assessment_types_pk
            primary key,
    name                  varchar(256)                        not null,
    is_default            boolean                             not null,
    system_user_id        varchar(50)                         not null,
    status                nomenclature.status_enum            not null,
    ordering_id           integer                             not null
        constraint customer_assessment_types_uk
            unique
                deferrable initially deferred,
    create_date           timestamp default CURRENT_TIMESTAMP not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    is_hard_coded         boolean
);
create sequence if not exists nomenclature_additional_conditions_id_seq;
create table if not exists nomenclature.additional_conditions
(
    id                          integer generated by default as identity
        constraint additional_conditions_pk
            primary key,
    name                        varchar(1024)                       not null,
    customer_assessment_type_id integer                             not null,
    is_default                  boolean                             not null,
    system_user_id              varchar(50)                         not null,
    status                      nomenclature.status_enum            not null,
    ordering_id                 integer                             not null
        constraint additional_conditions_uk
            unique
                deferrable initially deferred,
    create_date                 timestamp default CURRENT_TIMESTAMP not null,
    modify_date                 timestamp,
    modify_system_user_id       varchar(50)
);
create type receivable.payment_package_status as enum ('LOCKED', 'UNLOCKED');
create type receivable.payment_package_type as enum ('ONLINE', 'OFFLINE');
create sequence if not exists receivable.payment_packages_id_seq increment by 1;
create table if not exists receivable.payment_packages
(
    id                     bigint generated by default as identity
        constraint payment_packages_pk
            primary key,
    payment_package_status receivable.payment_package_status                  not null,
    collection_channel_id  bigint                                             not null,
--         constraint payment_packages_collection_channel_fk
--             references receivable.collection_channels,
    account_period_id      bigint                                             not null,
    payment_date           date                                               not null,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                                        not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50),
    status                 receivable.general_status,
    payment_package_type   receivable.payment_package_type
);
create sequence if not exists receivable.manual_liabilitie_offsettings_id_seq increment by 1;
create table if not exists receivable.manual_liabilitie_offsettings
(
    id                                    bigint generated by default as identity
        constraint manual_liabilitie_offsettings_pk
            primary key,
    manual_liabilitie_date                date                                not null,
    customer_id                           bigint                              not null,
    customer_communication_id_for_billing bigint                              not null,
    reversed                              boolean                             not null,
    create_date                           timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                        varchar(50)                         not null,
    modify_date                           timestamp,
    modify_system_user_id                 varchar(50),
    customer_detail_id                    bigint
);
create sequence if not exists receivable.customer_deposits_id_seq increment by 1;
create table receivable.customer_deposits
(
    id                            bigint generated by default as identity
        constraint customer_deposits_pk
            primary key,
    deposit_number                varchar(24)                                        not null,
    payment_deadline              date                                               not null,
    refund_date                   date,
    initial_amount                numeric                                            not null,
    current_amount                numeric                                            not null,
    currency_id                   integer                                            not null,
    income_account_number         varchar(512)                                       not null,
    cost_center_controlling_order varchar(512),
    customer_id                   bigint                                             not null,
    status                        receivable.general_status                          not null,
    create_date                   timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                varchar(50)                                        not null,
    modify_date                   timestamp with time zone,
    modify_system_user_id         varchar(50),
    constraint customer_deposits_payment_deadline_refund_chk
        check (refund_date >= payment_deadline)
);
create sequence if not exists receivable.mlo_customer_deposits_id_seq increment by 1;
create table if not exists receivable.mlo_customer_deposits
(
    id                              bigint generated by default as identity
        constraint mlo_customer_deposits_pk
            primary key,
    manual_liabilitie_offsetting_id bigint,
    customer_deposit_id             bigint                              not null,
    create_date                     timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                  varchar(50)                         not null,
    modify_date                     timestamp,
    modify_system_user_id           varchar(50),
    reversal_receivable_id          bigint
);
create type receivable.customer_liabilitie_creation_type as enum ('AUTOMATIC', 'MANUAL');
create type receivable.customer_liabilitie_outgoing_doc_type as enum ('INVOICE', 'LATE_PAYMENT_FINE', 'DEPOSIT', 'RESCHEDULING','ACTION','DEBIT_NOTE');
create sequence if not exists receivable.customer_liabilities_id_seq increment by 1;
create table receivable.customer_liabilities
(
    id                                                         bigint generated by default as identity
        constraint customer_liabilities_pk
            primary key,
    liability_number                                           varchar(24),
    account_period_id                                          bigint,
    due_date                                                   date,
    full_offset_date                                           date,
    applicable_interest_rate_id                                bigint,
    applicable_interest_rate_date_from                         date,
    applicable_interest_rate_date_to                           date,
    initial_amount                                             numeric,
    initial_amount_in_other_ccy                                numeric,
    current_amount                                             numeric,
    current_amount_in_other_ccy                                numeric,
    currency_id                                                integer,
    outgoing_document_from_external_system                     varchar(512),
    basis_for_issuing                                          varchar(512),
    income_account_number                                      varchar(512),
    cost_center_controlling_order                              varchar(512),
    direct_debit                                               boolean,
    bank_id                                                    integer,
    iban                                                       varchar(22),
    blocked_for_payment                                        boolean,
    blocked_for_payment_from_date                              date,
    blocked_for_payment_to_date                                date,
    blocked_for_payment_blocking_reason_id                     integer,
    blocked_for_payment_additional_info                        varchar(2048),
    blocked_for_reminder_letters                               boolean,
    blocked_for_reminder_letters_from_date                     date,
    blocked_for_reminder_letters_to_date                       date,
    blocked_for_reminder_letters_blocking_reason_id            integer,
    blocked_for_reminder_letters_additional_info               varchar(2048),
    blocked_for_calculation_of_late_payment                    boolean,
    blocked_for_calculation_of_late_payment_from_date          date,
    blocked_for_calculation_of_late_payment_to_date            date,
    blocked_for_calculation_of_late_payment_blocking_reason_id integer,
    blocked_for_calculation_of_late_payment_additional_info    varchar(2048),
    blocked_for_liabilities_offsetting                         boolean,
    blocked_for_liabilities_offsetting_from_date               date,
    blocked_for_liabilities_offsetting_to_date                 date,
    blocked_for_liabilities_offsetting_blocking_reason_id      integer,
    blocked_for_liabilities_offsetting_additional_info         varchar(2048),
    customer_id                                                bigint,
    invoice_id                                                 bigint,
    action_id                                                  bigint,
    claimed_penalty_id                                         bigint,
    contract_billing_group_id                                  bigint,
    alt_invoice_recipient_customer_id                          bigint,
    outgoing_document_type                                     receivable.customer_liabilitie_outgoing_doc_type,
    status                                                     receivable.general_status,
    create_date                                                timestamp with time zone default CURRENT_TIMESTAMP,
    system_user_id                                             varchar(50),
    modify_date                                                timestamp with time zone,
    modify_system_user_id                                      varchar(50),
    creation_type                                              receivable.customer_liabilitie_creation_type,
    additional_info                                            varchar(2048),
    blocked_for_supply_termination                             boolean,
    blocked_for_supply_termination_from_date                   date,
    blocked_for_supply_termination_to_date                     date,
    blocked_for_supply_termination_blocking_reason_id          integer,
    blocked_for_supply_termination_additional_info             varchar(2048),
    deposit_id                                                 bigint,
    late_payment_fine_id                                       bigint,
    rescheduling_id                                            bigint,
    end_date_of_waiting_payment                                timestamp,
    child_late_payment_fine_id                                 bigint,
    amount_without_interest                                    numeric,
    occurrence_date                                            date,
    amount_without_interest_in_other_ccy                       numeric,
    added_to_deposit                                           boolean,
    manual_liability_offsetting_id                             bigint,
    liability_lawsuit                                          text,
    is_government_liability boolean,
    exclude_from_payment_offsetting boolean,
    constraint customer_liabilities_interest_date_check
        check (applicable_interest_rate_date_from <= applicable_interest_rate_date_to)
);
create sequence if not exists receivable.mlo_customer_liabilities_id_seq increment by 1;
create table if not exists receivable.mlo_customer_liabilities
(
    id                              bigint generated by default as identity
        constraint mlo_customer_liabilities_pk
            primary key,
    manual_liabilitie_offsetting_id bigint,
    customer_liabilitie_id          bigint                              not null,
    create_date                     timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                  varchar(50)                         not null,
    modify_date                     timestamp,
    modify_system_user_id           varchar(50)
);
create type receivable.customer_receivable_creation_type as enum ('AUTOMATIC', 'MANUAL');
create type receivable.customer_receivable_outgoing_doc_type as enum ('CREDIT_NOTE', 'LATE_PAYMENT_FINE');
create sequence if not exists receivable.customer_receivables_id_seq increment by 1;
create table if not exists receivable.customer_receivables
(
    id                                     bigint generated by default as identity
        constraint customer_receivables_pk
            primary key,
    receivable_number                      varchar(24)                                        not null,
    account_period_id                      bigint                                             not null,
    due_date                               date,
    full_offset_date                       date,
    initial_amount                         numeric                                            not null,
    initial_amount_in_other_ccy            numeric,
    current_amount                         numeric                                            not null,
    current_amount_in_other_ccy            numeric,
    currency_id                            integer                                            not null,
    outgoing_document_from_external_system varchar(512),
    basis_for_issuing                      varchar(512),
    income_account_number                  varchar(512),
    cost_center_controlling_order          varchar(512),
    direct_debit                           boolean,
    bank_id                                integer,
    iban                                   varchar(22),
    blocked_for_payment                    boolean,
    blocked_for_payment_from_date          date,
    blocked_for_payment_to_date            date,
    blocked_for_payment_blocking_reason_id integer,
    blocked_for_payment_additional_info    varchar(2048),
    customer_id
                                           bigint                                             not null,
    invoice_id                             bigint,
    action_id                              bigint,
    contract_billing_group_id              bigint,
    alt_invoice_recipient_customer_id      bigint,
    outgoing_document_type                 receivable.customer_receivable_outgoing_doc_type,
    status                                 receivable.general_status                          not null,
    create_date                            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                                        not null,
    modify_date                            timestamp with time zone,
    modify_system_user_id                  varchar(50),
    creation_type                          receivable.customer_receivable_creation_type       not null,
    additional_info                        varchar(2048),
    occurrence_date                        date,
    late_payment_fine_id                   bigint
);
create sequence if not exists receivable.mlo_customer_receivables_id_seq increment by 1;
create table if not exists receivable.mlo_customer_receivables
(
    id                              bigint generated by default as identity
        constraint mlo_customer_receivables_pk
            primary key,
    manual_liabilitie_offsetting_id bigint,
    customer_receivable_id          bigint                              not null,
    create_date                     timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                  varchar(50)                         not null,
    modify_date                     timestamp,
    modify_system_user_id           varchar(50)
);
create type receivable.receivable_late_payment_fine_type as enum ('LATE_PAYMENT_FINE', 'REVERSAL_OF_LATE_PAYMENT_FINE');
create type receivable.late_payment_fine_outgoing_doc_type as enum ('RESCHEDULING', 'LATE_PAYMENT_FINE_JOB','ONLINE_PAYMENT');
create sequence if not exists receivable.late_payment_fines_id_seq increment by 1;
create sequence if not exists receivable.late_payment_fine_number_seq increment by 1;
create table if not exists receivable.late_payment_fines
(
    id                            bigint generated by default as identity
        constraint late_payment_fines_pk
            primary key,
    late_payment_number           varchar(10)                                  not null,
    type                          receivable.receivable_late_payment_fine_type not null,
    amount                        numeric                                      not null,
    amount_in_other_ccy           numeric                                      not null,
    due_date                      date                                         not null,
    currency_id                   integer                                      not null,
    income_account_number         varchar(512),
    cost_center_controlling_order varchar(512),
    customer_id                   bigint                                       not null,
    contract_billing_group_id     bigint,
    issuer                        varchar(50),
    template_id                   bigint,
    file_url                      varchar(256),
    reversed                      boolean   default false                      not null,
    create_date                   timestamp default CURRENT_TIMESTAMP          not null,
    system_user_id                varchar(50)                                  not null,
    modify_date                   timestamp,
    modify_system_user_id         varchar(50),
    reversal_late_payment_fine_id bigint,
    communication_id              bigint,
    outgoing_doc_type             receivable.late_payment_fine_outgoing_doc_type,
    logical_date                  date,
    parent_liability_id           bigint,
    rescheduling_id               bigint,
    parent_lpf_id                 bigint,
    document_template_id          bigint
);

create sequence if not exists receivable.late_payment_fine_ftp_files_id_seq increment by 1;
create table if not exists receivable.late_payment_fine_ftp_files
(
    id                    bigint generated by default as identity
        constraint late_payment_fine_ftp_files_pk
            primary key,
    name                  varchar(200)                                       not null,
    file_url              varchar(256)                                       not null,
    late_payment_fine_id  bigint                                             not null,
--         constraint late_payment_fine_ftp_files
--             references receivable.late_payment_fines,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                receivable.receivable_subobject_status             not null,
    document_id           bigint,
    page_count bigint,
    execution_date           timestamp
);

create sequence if not exists receivable.late_payment_fine_communications_id_seq increment by 1;
create table if not exists receivable.late_payment_fine_communications
(
    id                     bigint generated by default as identity
        constraint late_payment_fine_communications_pk
            primary key,
    email_communication_id bigint,
    create_date            timestamp default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                         not null,
    modify_date            timestamp,
    modify_system_user_id  varchar(50),
    late_payment_fine_id   bigint                              not null
);

create sequence if not exists customer_deposits_id_seq;
create sequence if not exists receivable.deposit_number_seq;
create table if not exists receivable.customer_deposits
(
    id                            bigint generated by default as identity
        constraint customer_deposits_pk
            primary key,
    deposit_number                varchar(24)                                        not null,
    payment_deadline              date                                               not null,
    refund_date                   date,
    initial_amount                numeric                                            not null,
    current_amount                numeric,
    currency_id                   integer                                            not null,
--         constraint customer_deposits_currency_fk
--             references nomenclature.currencies,
    income_account_number         varchar(512)                                       not null,
    cost_center_controlling_order varchar(512),
    customer_id                   bigint                                             not null,
--         constraint customer_deposits_customer_fk
--             references customer.customers,
    status                        receivable.general_status                          not null,
    create_date                   timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                varchar(50)                                        not null,
    modify_date                   timestamp with time zone,
    modify_system_user_id         varchar(50),
    constraint customer_deposits_payment_deadline_refund_chk
        check (refund_date >= payment_deadline)
);
create type receivable.file_status as enum ('DRAFT', 'SIGNED');
create sequence if not exists receivable.customer_deposit_ftp_files_id_seq;
create table if not exists receivable.customer_deposit_ftp_files
(
    id                    bigint generated by default as identity
        constraint customer_deposit_ftp_files_pk
            primary key,
    name                  varchar(200)                                       not null,
    file_url              varchar(256)                                       not null,
    deposit_id            bigint                                             not null,
--         constraint customer_deposit_ftp_files_deposit_fk
--             references receivable.customer_deposits,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                receivable.receivable_subobject_status             not null,
    file_statuses         receivable.file_status[],
    document_id           bigint
);
create sequence if not exists customer_liabilities_id_seq;
create type receivable.power_supply_disconnection_reminder_status as enum ('DRAFT', 'EXECUTED','IN_PROGRESS','SMS_IN_PROGRESS','EMAIL_IN_PROGRESS');
create type receivable.reminder_communication_channel as enum ('EMAIL', 'SMS', 'ON_PAPER', 'EMAIL_SMS_ON_PAPER');
create type receivable.customer_filter_type_enum as enum ('NONE', 'INCLUDED', 'EXCLUDED');
create sequence if not exists power_supply_disconnection_reminders_id_seq;
create table if not exists receivable.power_supply_disconnection_reminders
(
    id                       bigint generated by default as identity
        constraint power_supply_disconnection_reminders_pk
            primary key,
    reminder_number          varchar(24)                                                    not null,
    reminder_status          receivable.power_supply_disconnection_reminder_status          not null,
    customer_send_date       timestamp                                                      not null,
    liability_amount_from    numeric,
    liability_amount_to      numeric,
    currency_id              integer,
--         constraint power_supply_disconnection_reminders_ccy_fk
--             references nomenclature.currencies,
    liabilities_max_due_date date                                                           not null,
    status                   receivable.general_status                                      not null,
    create_date              timestamp                            default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                                                    not null,
    modify_date              timestamp,
    modify_system_user_id    varchar(50),
    document_template_id     bigint,

    email_template_id        bigint,
    sms_template_id          bigint,
    communication_channel    receivable.reminder_communication_channel[],
    disconnection_date       date,
    customer_filter_type     receivable.customer_filter_type_enum default 'NONE'::receivable.customer_filter_type_enum,
    customer_list            text,
    printed_file_name        varchar(65),
    status_assigned_at       timestamp with time zone,
    employee_id              bigint
);
create sequence if not exists power_supply_disconnection_reminder_tasks_id_seq;
create table if not exists receivable.power_supply_disconnection_reminder_tasks
(
    id                                     bigint generated by default as identity
        constraint power_supply_disconnection_reminder_tasks_pk
            primary key,
    power_supply_disconnection_reminder_id bigint                                             not null,
--         constraint power_supply_disconnection_tasks_reminder_fk
--             references receivable.power_supply_disconnection_reminders,
    task_id                                bigint                                             not null,
--         constraint power_supply_disconnection_reminder_task_fk
--             references task.tasks,
    status                                 receivable.receivable_subobject_status             not null,
    create_date                            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                                        not null,
    modify_date                            timestamp with time zone,
    modify_system_user_id                  varchar(50)
);
create sequence if not exists power_supply_disconnection_reminder_customers_id_seq;
create table if not exists receivable.power_supply_disconnection_reminder_customers
(
    id                                     bigint generated by default as identity
        constraint power_supply_disconnection_reminder_customers_pk
            primary key,
    customer_id                            bigint                              not null,
--         constraint power_supply_disconnection_reminder_customer_fk
--             references customer.customers,
    customer_liability_id                  bigint                              not null,
--         constraint power_supply_disconnection_reminder_customer_liability_fk
--             references receivable.customer_liabilities,
    liability_amount                       numeric                             not null,
    create_date                            timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                         not null,
    modify_date                            timestamp,
    modify_system_user_id                  varchar,
    power_supply_disconnection_reminder_id bigint                              not null,
    sms_id bigint,
    email_id bigint
--         constraint power_supply_disconnection_reminder_fk
--             references receivable.power_supply_disconnection_reminders
);
create type receivable.customer_assessment_status as enum ('DRAFT', 'FINAL');
create sequence if not exists customer_assessments_id_seq;
create table if not exists receivable.customer_assessments
(
    id                          bigint generated by default as identity
        constraint customer_assessments_pk
            primary key,
    assessment_number           varchar(24)                           not null,
    assessment_status           receivable.customer_assessment_status not null,
    customer_id                 bigint                                not null,
--         constraint customer_assessments_customer_fk
--             references customer.customers,
    status                      receivable.general_status             not null,
    create_date                 timestamp default CURRENT_TIMESTAMP   not null,
    system_user_id              varchar(50)                           not null,
    modify_date                 timestamp,
    modify_system_user_id       varchar(50),
    final_assessment            boolean                               not null,
    customer_assessment_type_id integer                               not null
--         constraint customer_assessment_type_fk
--             references nomenclature.customer_assessment_types
);

create table if not exists receivable.customer_assessment_add_conditions
(
    id                      bigint generated by default as identity
        constraint customer_assessment_add_conditions_pk
            primary key,
    customer_assessment_id  bigint,
    additional_condition_id bigint,
    status                  receivable.receivable_subobject_status             not null,
    create_date             timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id          varchar(50)                                        not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50)
);
create sequence if not exists customer_assessment_tasks_id_seq;
create table if not exists receivable.customer_assessment_tasks
(
    id                     bigint generated by default as identity
        constraint customer_assessment_tasks_pk
            primary key,
    customer_assessment_id bigint                                             not null,
--         constraint customer_assessment_tasks_assessment_fk
--             references receivable.customer_assessments,
    task_id                bigint                                             not null,
--         constraint customer_assessment_tasks_fk
--             references task.tasks,
    status                 receivable.receivable_subobject_status             not null,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                                        not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50)
);
DROP TABLE IF EXISTS nomenclature.sms_sending_numbers;
create sequence nomenclature.sms_sending_numbers_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.sms_sending_numbers
(
    id                    integer generated by default as identity
        constraint sms_sending_numbers_pk
            primary key,
    name                  varchar(1024)                                      not null,
    sms_number            varchar(32)                                        not null,
    is_default            boolean                                            not null,
    system_user_id        varchar(50)                                        not null,
    status                nomenclature.status_enum                           not null,
    ordering_id           integer                                            not null
        constraint sms_sending_numbers_ordering_uk
            unique
                deferrable initially deferred,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    is_hard_coded         boolean
);
DROP TABLE IF EXISTS nomenclature.rps_numbers;
create sequence nomenclature.rps_numbers_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.rps_numbers
(
    id                    integer generated by default as identity
        constraint rps_numbers_pk
            primary key,
    rps_number            varchar(512)                                       not null,
    day_of_month          integer                                            not null
        constraint rps_numbers_day_of_month_check
            check (day_of_month >= 1 and day_of_month <= 31),
    is_default            boolean                                            not null,
    system_user_id        varchar(50)                                        not null,
    status                nomenclature.status_enum                           not null,
    ordering_id           integer                                            not null
        constraint rps_numbers_ordering_uk
            unique
                deferrable initially deferred,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
alter sequence nomenclature.rps_numbers_id_seq owned by nomenclature.rps_numbers.id;

create sequence nomenclature.communication_topics_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS nomenclature.communication_topics
(
    id                    integer generated by default as identity
        constraint communication_topics_pk
            primary key,
    name                  varchar(1024)                                      not null,
    is_default            boolean                                            not null,
    system_user_id        varchar(50)                                        not null,
    status                nomenclature.status_enum                           not null,
    ordering_id           integer                                            not null
        constraint communication_topics_ordering_uk
            unique
                deferrable initially deferred,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    is_hard_coded         boolean
);
alter sequence nomenclature.communication_topics_id_seq owned by nomenclature.communication_topics.id;

create type crm.onpaper_communication_subobject_status as enum ('ACTIVE', 'DELETED');
CREATE TABLE IF NOT EXISTS crm.onpaper_communication_files
(
    id                       bigint generated by default as identity
        constraint onpaper_communication_files_pk
            primary key,
    name                     varchar(200)                                       not null,
    file_url                 varchar(256),
    onpaper_communication_id bigint,
    create_date              timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id           varchar(50)                                        not null,
    modify_date              timestamp,
    modify_system_user_id    varchar(50),
    status                   crm.onpaper_communication_subobject_status         not null,
    document_id              bigint
);

create type crm.onpaper_communication_type as enum ('OUTGOING');
create type crm.general_status as enum ('ACTIVE', 'DELETED');
create type crm.onpaper_communication_creation_type as enum ('AUTOMATIC');
create type crm.onpaper_communication_sending_status as enum ('NOT_SENT', 'SENT_MANUALLY');
CREATE TABLE IF NOT EXISTS crm.onpaper_communications
(
    id                                     bigint generated by default as identity
        constraint onpaper_communications_pk
            primary key,
    customer_detail_id                     bigint                                   not null,
    customer_communication_id              bigint                                   not null,
    communication_topic_id                 integer                                  not null,
    archimed_number                        text,
    creation_type                          crm.onpaper_communication_creation_type  not null,
    communication_type                     crm.onpaper_communication_type           not null,
    sending_status                         crm.onpaper_communication_sending_status not null,
    status                                 crm.general_status                       not null,
    create_date                            timestamp default CURRENT_TIMESTAMP      not null,
    system_user_id                         varchar(50)                              not null,
    modify_date                            timestamp,
    modify_system_user_id                  varchar(50),
    sent_date                              timestamp with time zone,
    power_supply_disconnection_reminder_id bigint,
    sender_employee_id                     bigint
);

create type crm.email_communication_type as enum ('INCOMING', 'OUTGOING');
create type crm.email_communication_channels as enum ('EMAIL', 'MASS_EMAIL');
create type crm.email_communication_status as enum ('DRAFT', 'RECEIVED', 'SENT', 'IN_PROGRESS', 'SENT_FAILED', 'SENT_SUCCESSFULLY');
create type crm.email_communication_customers_status as enum ('DRAFT', 'IN_PROGRESS', 'SENT', 'SENT_FAILED', 'SENT_SUCCESSFULLY', 'NOT_SENT', 'SENT_MANUALLY');
create sequence crm.email_communications_id_seq increment by 1;
CREATE TABLE IF NOT EXISTS crm.email_communications
(
    id                              bigint generated by default as identity
        constraint email_communications_pk
            primary key,
    communication_as_an_institution boolean,
    dms_number                      varchar(128),
    communication_topic_id          integer                             not null,
--         constraint email_communications_topic_fk
--             references nomenclature.communication_topics,
    communication_type              crm.email_communication_type        not null,
    sent_date                       timestamp,
    email_mailbox_id                integer                             not null,
    email_subject                   varchar(255)                        not null,
    email_body                      text                                not null,
    sender_employee_id              bigint,
    email_template_id              bigint,
--         constraint email_communications_sender_employee_fk
--             references customer.account_managers,
    create_date                     timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                  varchar(50)                         not null,
    modify_date                     timestamp,
    modify_system_user_id           varchar(50),
    communication_channel           crm.email_communication_channels    not null,
    communication_status            crm.email_communication_status,
    status                          crm.general_status
);
alter sequence crm.email_communications_id_seq owned by crm.email_communications.id;


create table crm.email_communication_customers
(
    id                         bigint generated by default as identity
        constraint email_communication_customers_pk
            primary key,
    customer_detail_id         bigint,
    customer_communication_id  bigint,
    email_communication_id     bigint,
    create_date                timestamp default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                         not null,
    modify_date                timestamp,
    modify_system_user_id      varchar(50),
    status                     crm.email_communication_customers_status,
    contact_purpose_id         bigint,
    email_body                 text,
    product_contract_detail_id bigint,
    service_contract_detail_id bigint
);

create type crm.email_communication_subobject_status as enum ('ACTIVE', 'DELETED');

create table crm.email_communication_tasks
(
    id                     bigint generated by default as identity
        constraint email_communication_tasks_pk
            primary key,
    email_communication_id bigint,
    task_id                bigint,
    status                 crm.email_communication_subobject_status,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP,
    system_user_id         varchar(50),
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50)
);

-- alter table process_management.process
--     add constraint fk_collection_channel foreign key (collection_channel_id) references receivable.collection_channels (id);
-- alter table process_management.process
--     add constraint fk_payment_package foreign key (payment_package_id) references receivable.payment_packages (id);
create type invoice.reversal_status as enum ('NOT_STARTED', 'REVERSED', 'FAILED');

create sequence process_management.process_files_id_seq increment by 1;
create table if not exists process_management.process_files
(
    id                    bigint generated by default as identity
        constraint process_files_pk
            primary key,
    name                  varchar(200)                                       not null,
    file_url              varchar(256)                                       not null,
    process_id            bigint,
--         constraint process_fk
--             references process_management.process,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50)
);

create table if not exists invoice.invoice_reversal_invoice
(
    id                    bigint                                             not null
        constraint invoice_reversal_invoice_pk
            primary key,
    reversal_id           bigint                                             not null,
--         constraint invoice_reversal_invoice_billings_id_fk
--             references billing.billings,
    invoice_id            bigint                                             not null,
--         constraint invoice_reversal_invoice_invoice_id_fk
--             references invoice.invoices,
    status                invoice.reversal_status                            not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create table if not exists billing.billing_error_data
(
    id                    bigint                                             not null
        constraint billing_error_data_pk
            primary key,
    billing_id            bigint                                             not null,
--         constraint billing_error_data_billing_fk
--             references billing.billings,
    error_message         varchar(512),
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create sequence billing.billing_error_data_id_seq;

create sequence invoice.invoice_reversal_invoice_id_seq;

create table receivable.liabilities_condition_replacemets
(
    id               integer not null
        primary key,
    condition_text   varchar,
    replacement_text varchar,
    is_key           boolean
);
create sequence if not exists product.product_details_collection_channels_id_seq increment by 1;
create table if not exists product.product_details_collection_channels
(
    id                    bigint generated by default as identity
        constraint collection_channel_details_pk
            primary key,
    collection_channel_id bigint                                             not null,
    product_details_id    bigint                                             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    status                product.product_status                             not null
);
create sequence if not exists service.service_details_collection_channels_id_seq increment by 1;
create table if not exists service.service_details_collection_channels
(
    id                    bigint generated by default as identity
        constraint collection_channel_details_pk
            primary key,
    collection_channel_id bigint                                             not null,
    service_details_id    bigint                                             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    status                service.service_subobject_status                   not null
);
create schema template;
create type invoice.invoice_subobject_status as enum ('ACTIVE', 'DELETED');
create type template.template_status as enum ('ACTIVE', 'INACTIVE', 'DELETED');
create type template.template_purpose as enum ('CANCEL_DISCONNECTION_POWER', 'DEPOSIT', 'EMAIL', 'INVOICE_CANCEL', 'LATE_PAYMENT_FINE', 'MANUAL_LIABILITY_OFFSET', 'OBJECTION_CHANGE_COORD', 'OBJECTION_WITHDRAW_CHANGE_COORD', 'PENALTY', 'PRODUCT', 'RECONNECTION_POWER', 'REMINDER', 'REMINDER_DISCONNECT_POWER', 'REQUEST_DISCONNECT_POWER', 'RESCHEDULING', 'SERVICE', 'SMS', 'TERMINATION', 'INVOICE', 'ADDITIONAL_AGREEMENT');
create type template.template_output_file_format as enum ('DOCX', 'PDF', 'XLSX');
create type template.template_file_signing as enum ('NO', 'SIGNING_WITH_SYSTEM_CERTIFICATE', 'SIGNING_WITH_TABLET', 'SIGNING_WITH_QUALIFIED_SIGNATURE');
create type template.template_file as enum ('NONE', 'SYSTEM_CERTIFICATE', 'QES', 'QES_AND_SYSTEM_CERTIFICATE', 'QES_OR_SYSTEM_CERTIFICATE');
create type template.template_language as enum ('BILINGUAL', 'BULGARIAN');
create type template.template_customer_type as enum ('LEGAL_ENTITY', 'PRIVATE_CUSTOMER', 'PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY');
create type template.template_purpose_of_consumption as enum ('NON_HOUSEHOLD', 'HOUSEHOLD');
create type template.template_file_name_sufix as enum ('DATE_AND_TIME_DDMMYYYYHHMM', 'DATE_AND_TIME_DDMMYYYYHHMMSS', 'DATE_AND_TIME_DDMMYYYY_HHMM', 'DATE_AND_TIME_DDMMYYYY_HHMMSS', 'DATE_AND_TIME_DDMMYYHHMM', 'DATE_DDMMYYYYDATE_DDMMYY', 'TIME_HHMM', 'TIME_HHMMSS', 'TIMESTAMP', 'DATE_AND_TIME_DDMMYY_HHMM', 'DATE_DDMMYYYY', 'DATE_DDMMYY');
create type template.template_file_name as enum ('CUSTOMER_IDENTIFIER', 'CUSTOMER_NAME', 'CUSTOMER_NUMBER', 'DOCUMENT_NUMBER', 'FILE_ID', 'TIMESTAMP');
create type template.template_subobject_status as enum ('ACTIVE', 'DELETED');
create type template.template_type as enum ('DOCUMENT', 'EMAIL', 'SMS');
create sequence template.template_details_id_seq;
create sequence template.templates_id_seq;
create sequence service.service_template_id_seq;
create sequence product.product_templates_id_seq;
create sequence billing.billing_templates_id_seq;
create sequence template.template_files_id_seq;
create table if not exists template.templates
(
    id                                     bigint generated by default as identity
        constraint templates_pk
            primary key,
    create_date                            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                         varchar(50)                                        not null,
    template_purpose                       template.template_purpose                          not null,
    modify_date                            timestamp with time zone,
    modify_system_user_id                  varchar(50),
    last_template_detail_id                bigint,
    default_for_goods_order_document       boolean,
    default_for_goods_order_email          boolean,
    default_for_late_payment_fine_document boolean,
    default_for_late_payment_fine_email    boolean,
    default_claimed_penalty_email      boolean,
    default_claimed_penalty_document   boolean,
    default_product_contract_conclusion    boolean,
    default_service_contract_conclusion    boolean,
    status                                 template.template_status                           not null
);

create table if not exists template.template_files
(
    id                    bigint generated by default as identity
        constraint template_files_pk
            primary key,
    name                  varchar(200)                                       not null,
    file_url              varchar(256)                                       not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                template.template_subobject_status                 not null
);

create table if not exists template.template_details
(
    id                      bigint generated by default as identity
        constraint template_details_pk
            primary key,
    template_id             bigint,
--         constraint template_details_template_id_fk
--             references template.templates,
    create_date             timestamp with time zone default CURRENT_TIMESTAMP not null,
    start_date              date                     default CURRENT_DATE      not null,
    end_date                date,
    name                    varchar(1024)                                      not null,
    language                template.template_language,
    customer_type           template.template_customer_type[],
    purpose_of_consumption  template.template_purpose_of_consumption[],
    type                    template.template_type                             not null,
    output_file_format      template.template_output_file_format[],
    file_name_sufix         template.template_file_name_sufix,
    file_name               template.template_file_name[],
    file_signing            template.template_file_signing[],
    file                    template.template_file,
    system_user_id          varchar(50)                                        not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50),
    last_template_detail_id bigint,
    file_name_prefix        varchar(1024),
    version                 integer,
    template_file_id        bigint,
--         constraint template_details_template_files_id_fk
--             references template.template_files,
    subject                 varchar(2048),
    quantity                integer
);
create type product.product_template_types as enum ('CONTRACT_TEMPLATE', 'BI_CONTRACT_TEMPLATE', 'ADDITIONAL_INVOICE_TEMPLATE', 'INVOICE_TEMPLATE', 'EMAIL_TEMPLATE');
create table if not exists product.product_templates
(
    id                    bigint                                             not null
        constraint product_templates_pk
            primary key,
    template_id           bigint                                             not null,
--         constraint product_templates_template_fk
--             references template.templates,
    product_detail_id     bigint                                             not null,
--         constraint product_templates_product_detail_fk
--             references product.product_details,
    status                product.product_subobject_status                   not null,
    product_template_type product.product_template_types,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create type service.service_template_types as enum ('CONTRACT_TEMPLATE', 'BI_CONTRACT_TEMPLATE', 'ADDITIONAL_INVOICE_TEMPLATE', 'EMAIL_TEMPLATE', 'INVOICE_TEMPLATE');
create table if not exists service.service_templates
(

    id                    bigint                                             not null
        constraint service_templates_pk
            primary key,
    template_id           bigint                                             not null,
--         constraint service_templates_template_fk
--             references template.templates,
    service_detail_id     bigint                                             not null,
--         constraint service_templates_service_detail_fk
--             references service.service_details,
    status                service.service_subobject_status                   not null,
    service_template_type service.service_template_types                     not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create sequence template.document_id_seq;
create sequence template.document_file_id_seq;

create table if not exists template.document_file
(
    id                    bigint generated by default as identity
        constraint template_document_file_id_pk
            primary key,
    unsigned_file_url     varchar(256),
    unsigned_file_name    varchar(256),
    signed_file_url       varchar(256),
    signed_file_name      varchar(256),
    document_id           bigint,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create type template.document_status as enum ('UNSIGNED', 'SIGNED');
create type template.document_base_status as enum ('ACTIVE', 'DELETED');
create type template.document_signers as enum ('NO', 'SYSTEM_CERTIFICATE', 'SIGNATUS', 'QES');

CREATE SEQUENCE template.document_file_generator_id_seq;
create table if not exists template.document
(
    id                    bigint generated by default as identity
        constraint template_document_id_pk
            primary key,
    name                  varchar(255),
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    document_status       template.document_status,
    status                template.document_base_status,
    unsigned_file_url     varchar(512),
    signed_file_url       varchar(512),
    signers               template.document_signers[],
    signed_by             template.document_signers[],
    file_format           template.template_output_file_format,
    template_id           bigint,
    archived_file_type    varchar(50),
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    status_modify_date    timestamp with time zone,
    is_unsigned_archived  boolean,
    unsigned_document_id  bigint,
    unsigned_file_id      bigint,
    file_name_file_id_tag_value       bigint
);
create sequence product.termination_templates_id_seq;

create table if not exists billing.standard_vat_base_details
(
    id                                         bigint generated always as identity,
    invoice_id                                 bigint  not null,
    type                                       varchar not null,
    pc_id                                      bigint,
    pod_id                                     bigint,
    date_from                                  date,
    date_to                                    date,
    customer_detail_id                         bigint,
    contract_detail_id                         bigint,
    total_amount_without_vat_main_currency     numeric,
    total_amount_with_vat_main_currency        numeric,
    total_amount_vat_main_currency             numeric,
    main_currency_id                           bigint,
    total_amount_without_vat_alt_currency      numeric,
    total_amount_with_vat_alt_currency         numeric,
    total_amount_vat_alt_currency              numeric,
    alt_currency_id                            bigint,
    total_amount_without_vat_original_currency numeric,
    total_amount_with_vat_original_currency    numeric,
    total_amount_vat_original_currency         numeric,
    original_currency_id                       numeric,
    vat_rate_id                                bigint,
    vat_rate_percent                           numeric,
    service_detail_id                          bigint,
    product_detail_id                          bigint,
    alt_invoice_recipient_customer_detail_id   bigint
);

create table if not exists product.termination_templates
(
    id                    bigint                                             not null
        constraint termination_template_pk
            primary key,
    template_id           bigint                                             not null,
--         constraint termination_template_template_fk
--             references template.templates,
    termination_id        bigint                                             not null,
--         constraint termination_template_termination_fk
--             references product.terminations,
    status                product.product_subobject_status                   not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create sequence terms.penalty_templates_id_seq;

create table if not exists terms.penalty_templates
(
    id                    bigint                                             not null
        constraint termination_template_pk
            primary key,
    template_id           bigint                                             not null,
--         constraint penalty_templates_template_fk
--             references template.templates,
    penalty_id            bigint                                             not null,
--         constraint penalty_templates_termination_fk
--             references terms.penalties,
    status                terms.penalty_subobject_status                     not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create table if not exists billing.billing_templates
(
    id                    bigint                                             not null
        constraint termination_template_pk
            primary key,
    template_id           bigint                                             not null,
--         constraint billing_templates_template_fk
--             references template.templates,
    billing_id            bigint                                             not null,
--         constraint billing_templates_billing_fk
--             references billing.billings,
    status                billing.billing_subobject_status                   not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create sequence invoice.invoice_cancelation_templates_id_seq;
create table if not exists invoice.invoice_cancelation_templates
(
    id                     bigint generated by default as identity
        constraint invoice_cancelation_templates_pk
            primary key,
    invoice_cancelation_id bigint                                             not null,
    template_id            bigint                                             not null,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                                        not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50),
    status                 invoice.invoice_subobject_status                   not null
);


create table receivable.objection_to_change_of_cbg_templates
(
    id                            bigint                                             not null
        constraint objection_to_change_of_cbg_templates_pk
            primary key,
    template_id                   bigint                                             not null,
--         constraint objection_to_change_of_cbg_templates_template_fk
--             references template.templates,
    objection_to_change_of_cbg_id bigint                                             not null,
    status                        billing.billing_subobject_status                   not null,
    create_date                   timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                varchar(50)                                        not null,
    modify_date                   timestamp with time zone,
    modify_system_user_id         varchar(50)
);


create table receivable.power_supply_disconnection_request_templates
(
    id                                    bigint generated by default as identity
        constraint power_supply_disconnection_request_templates_pk
            primary key,
    power_supply_disconnection_request_id bigint                                 not null,
    template_id                           bigint                                 not null,
    status                                receivable.receivable_subobject_status not null,
    create_date                           timestamp default CURRENT_TIMESTAMP    not null,
    system_user_id                        varchar(50)                            not null,
    modify_date                           timestamp,
    modify_system_user_id                 varchar(50)
);

create type receivable.power_supply_dcn_cancellation_status as enum ('DRAFT', 'EXECUTED');

create table receivable.power_supply_dcn_cancellations
(
    id                                    bigint generated by default as identity
        constraint power_supply_dcn_cancellations_pk
            primary key,
    cancellation_number                   varchar(24)                                     not null,
    cancellation_status                   receivable.power_supply_dcn_cancellation_status not null,
    power_supply_disconnection_request_id bigint,
    status                                receivable.general_status                       not null,
    create_date                           timestamp default CURRENT_TIMESTAMP             not null,
    system_user_id                        varchar(50)                                     not null,
    modify_date                           timestamp,
    modify_system_user_id                 varchar(50)
);

create table receivable.objection_withdrawal_to_change_of_cbg_templates
(
    id                                       bigint                                             not null
        constraint objection_withdrawal_to_change_of_cbg_templates_pk
            primary key,
    template_id                              bigint                                             not null,
--         constraint objection_withdrawal_to_change_of_cbg_templates_template_fk
--             references template.templates,
    objection_withdrawal_to_change_of_cbg_id bigint,
    status                                   billing.billing_subobject_status                   not null,
    create_date                              timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                           varchar(50)                                        not null,
    modify_date                              timestamp with time zone,
    modify_system_user_id                    varchar(50)
);

create table receivable.power_supply_dcn_cancellation_templates
(
    id                               bigint generated by default as identity
        constraint power_supply_dcn_cancellation_templates_pk
            primary key,
    power_supply_dcn_cancellation_id bigint,
    template_id                      bigint                                             not null,
    status                           receivable.receivable_subobject_status             not null,
    create_date                      timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                   varchar(50)                                        not null,
    modify_date                      timestamp with time zone,
    modify_system_user_id            varchar(50)
);

create type receivable.power_supply_disconnection_status as enum ('DRAFT', 'EXECUTED');
create table receivable.power_supply_disconnections
(
    id                                    bigint generated by default as identity
        constraint power_supply_disconnections_pk
            primary key,
    disconnection_number                  varchar(24)                                  not null,
    disconnection_status                  receivable.power_supply_disconnection_status not null,
    power_supply_disconnection_request_id bigint                                       not null,
    status                                receivable.general_status                    not null,
    create_date                           timestamp default CURRENT_TIMESTAMP          not null,
    system_user_id                        varchar(50)                                  not null,
    modify_date                           timestamp,
    modify_system_user_id                 varchar(50)
);



create type nomenclature.expiration_period_status as enum ('ACTIVE', 'DELETED');
create sequence nomenclature.document_expiration_period_id_seq;
create table if not exists nomenclature.document_expiration_period
(
    id                    bigint
        constraint expiration_period_pk
            primary key,
    number_of_months      integer                                            not null,
    system_user_id        varchar(50)                                        not null,
    status                nomenclature.expiration_period_status              not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table invoice.invoice_documents
(
    id                    bigint                              not null
        constraint invoice_documents_pk
            primary key,
    file_url              varchar(128)                        not null,
    create_date           timestamp default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                         not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    original_template_id  bigint                              not null,
--         constraint invoice_documents_original_template_id_fk
--             references template.templates,
    invoice_id            bigint,
--         constraint invoice_documents_invoice_id_fk
--             references invoice.invoices,
    name                  varchar(50)
);
create sequence receivable.power_supply_disconnection_reminder_templates_id_seq;
create table if not exists receivable.power_supply_disconnection_reminder_templates
(
    id                                     bigint generated by default as identity
        constraint power_supply_disconnection_reminder_templates_pk
            primary key,
    power_supply_disconnection_reminder_id bigint                                 not null,
--         constraint power_supply_dcn_reminder_template_reminder_fk
--             references receivable.power_supply_disconnection_reminders,
    template_id                            bigint                                 not null,
    status                                 receivable.receivable_subobject_status not null,
    create_date                            timestamp default CURRENT_TIMESTAMP    not null,
    system_user_id                         varchar(50)                            not null,
    modify_date                            timestamp,
    modify_system_user_id                  varchar(50)
);

create table if not exists receivable.customer_deposit_payment_ddl_aft_withdrawal
(
    id                    bigint generated by default as identity (maxvalue 2147483647)
        constraint customer_deposit_payment_ddl_aft_withdrawal_pk
            primary key,
    type                  receivable.customer_deposit_calendar_type          not null,
    value                 smallint,
    value_from            integer,
    value_to              integer,
    calendar_id           integer,
    excludes              receivable.customer_deposit_payment_deadline_exclude[],
    change_to             receivable.customer_deposit_payment_deadline_change_to[],
    customer_deposit_id   bigint,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                receivable.receivable_subobject_status             not null,
    name                  varchar(1024)                                      not null
);
create table if not exists action.action_templates
(
    id                    bigint                                             not null
        constraint action_templates_fk
            primary key,
    template_id           bigint                                             not null,
--         constraint action_templates_templates_fk
--             references template.templates,
    action_id             bigint                                             not null,
--         constraint action_templates_action_fk
--             references action.actions,
    status                action.action_subobject_status                     not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create type process_management.notification_type as enum ('COMPLETION', 'ERROR');

create table if not exists process_management.process_notification
(
    id                    bigint                                             not null
        constraint process_notification_pk
            primary key,
    process_id            bigint,
    performer_tag_id      bigint,
--         constraint process_notification_portal_tags_id_fk
--             references customer.portal_tags,
    performer_id          bigint,
--         constraint process_notification_account_managers_id_fk
--             references customer.account_managers,
    notification_type     process_management.notification_type,
    system_user_id        varchar(50),
    modify_system_user_id varchar(50),
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone
);
create type billing.notification_type as enum ('STARTUP', 'ERROR', 'WARNING', 'COMPLETE');
create table if not exists billing.billing_notifications
(
    id                    bigint generated by default as identity
        constraint billing_notifications_pk
            primary key,
    billing_id            bigint                                             not null,
--         constraint billing_notifications_billing_fk
--             references billing.billings,
    employee_id           bigint,
--         constraint billing_notifications_employee_fk
--             references customer.account_managers,
    tag_id                bigint,
--         constraint billing_notifications_tag_id
--             references customer.portal_tags,
    performer_type        billing.performer_type,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    notification_type     billing.notification_type
);
create schema notification;
create sequence notification.user_notification_id_seq;

create table if not exists notification.user_notifications
(
    account_manager_id    bigint      not null,
    id                    bigint      not null
        constraint user_notifications_pk
            primary key,
    create_date           timestamp   not null,
    system_user_id        varchar(50),
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    notification_type     varchar(50) not null,
    is_readen             boolean,
    entity_id             bigint      not null,
    read_date             timestamp,
    attributes jsonb
);
create type receivable.reminder_trigger_for_liabilities as enum ('WHEN_OVERDUE', 'ON_DUE_DATE', 'WHEN_INVOICES');
create type receivable.reminder_customer_condition_type as enum ('ALL_CUSTOMERS', 'CUSTOMERS_UNDER_CONDITIONS', 'LIST_OF_CUSTOMERS');

create table if not exists receivable.reminders
(
    id                        bigint generated by default as identity
        constraint reminders_pk
            primary key,
    reminder_number           varchar(24)                                 not null,
    trigger_for_liabilities   receivable.reminder_trigger_for_liabilities not null,
    postponement_in_days      integer,
    due_amount_from           numeric,
    due_amount_to             numeric,
    currency_id               integer,
--         constraint reminders_currency_fk
--             references nomenclature.currencies,
    customer_condition        receivable.reminder_customer_condition_type not null,
    customer_under_conditions varchar(4096),
    list_of_customers         text,
    communication_channel     receivable.reminder_communication_channel[] not null,
    contact_purpose_id        integer                                     not null,
--         constraint reminders_contact_purpose_fk
--             references nomenclature.contact_purposes,
    status                    receivable.general_status                   not null,
    create_date               timestamp default CURRENT_TIMESTAMP         not null,
    system_user_id            varchar(50)                                 not null,
    modify_date               timestamp,
    modify_system_user_id     varchar(50),
    template_id               bigint,
    email_template_id         bigint,
    sms_template_id           bigint,
--         constraint reminder_templates_id
--             references template.templates,
    constraint reminders_amount_chk
        check (due_amount_from <= due_amount_to)
);
create type crm.sms_communication_type as enum ('INCOMING', 'OUTGOING');

create type crm.sms_comm_status as enum ('IN_PROGRESS', 'SENT_FAILED', 'SENT_SUCCESSFULLY', 'DRAFT', 'RECEIVED', 'SENT', 'NOT_SENT', 'SENT_MANUALLY');
create type crm.sms_communication_channels as enum ('SMS', 'MASS_SMS');



create table if not exists crm.email_communication_contact_purposes
(
    id                     bigint generated by default as identity
        constraint email_communication_contact_purposes_pk
            primary key,
    email_communication_id bigint                              not null,
--         constraint email_comm_contact_purposes_email_comm_fk
--             references crm.email_communications,
    contact_purpose_id     integer                             not null,
--         constraint email_comm_contact_purposes_fk
--             references nomenclature.contact_purposes,
    create_date            timestamp default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                         not null,
    modify_date            timestamp,
    modify_system_user_id  varchar(50),
    status                 crm.email_communication_subobject_status
);
create sequence process_management.process_notification_id_seq;
create procedure receivable.automatic_liability_offsetting(IN p_receivable_id bigint,
                                                           IN p_liability_id bigint,
                                                           IN p_system_user_id character varying,
                                                           IN p_modify_system_user_id character varying)
    language plpgsql
as
'
    begin
    end;
';

create table if not exists nomenclature.disconnection_reasons
(
    id                    integer generated by default as identity
        constraint disconnection_reasons_pk
            primary key,
    name                  varchar(1024)                       not null,
    ordering_id           integer                             not null
        constraint disconnection_reasons_ordering_uk
            unique
                deferrable initially deferred,
    is_default            boolean                             not null,
    system_user_id        varchar(50)                         not null,
    status                nomenclature.status_enum            not null,
    create_date           timestamp default CURRENT_TIMESTAMP not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50)
);

create type receivable.power_supply_disconnection_request_status as enum ('DRAFT', 'EXECUTED', 'FEE_CHARGED');
create type receivable.power_supply_disconnection_requests_supplier_type as enum ('CURRENT', 'PREVIOUS');
create type receivable.power_supply_dscn_req_customer_cond_type as enum ('ALL_CUSTOMERS', 'CUSTOMERS_UNDER_CONDITIONS', 'LIST_OF_CUSTOMERS');
create table if not exists receivable.power_supply_disconnection_requests
(
    id                                       bigint generated by default as identity
        constraint power_supply_disconnection_requests_pk
            primary key,
    request_number                           varchar(24)                                                  not null,
    supplier_type                            receivable.power_supply_disconnection_requests_supplier_type not null,
    grid_operator_id                         integer                                                      not null,
    disconnection_reason_id                  integer                                                      not null,
    grid_operator_request_registration_date  date                                                         not null,
    customer_reminder_letter_sent_date       timestamp                                                    not null,
    grid_operator_disconnection_fee_pay_date date,
    power_supply_disconnection_date          date,
    liability_amount_from                    numeric,
    liability_amount_to                      numeric,
    currency_id                              integer,
    customer_condition_type                  receivable.power_supply_dscn_req_customer_cond_type          not null,
    customer_conditions                      varchar(4096),
    list_of_customers                        varchar(1024),
    status                                   receivable.general_status                                    not null,
    create_date                              timestamp default CURRENT_TIMESTAMP                          not null,
    system_user_id                           varchar(50)                                                  not null,
    modify_date                              timestamp,
    modify_system_user_id                    varchar(50),
    power_supply_disconnection_reminder_id   bigint,
    disconnection_request_status             receivable.power_supply_disconnection_request_status,
    all_selected                             boolean,
    pods_with_highest_consumption            boolean,
    exclude_pod_ids                          varchar(4096),
    tax_calculated                           boolean,
    execution_date                           timestamp
);
-- create table if not exists receivable.power_supply_dcn_cancellations_doc_files
-- (
--     id                    bigint generated by default as identity
--         constraint power_supply_dcn_cancellations_doc_files_pk
--             primary key,
--     document_id           bigint
--         constraint power_supply_dcn_cancellations_doc_files_document_id_pk
--             references template.document,
--     cancellation_id       bigint
--         constraint power_supply_dcn_cancellations_id_pk
--             references power_supply_dcn_cancellations,
--     create_date           timestamp with time zone,
--     system_user_id        varchar(50),
--     modify_date           timestamp with time zone,
--     modify_system_user_id varchar(50),
--     status                receivable.general_status
-- );
create sequence receivable.power_supply_disconnection_request_pods_id_seq increment by 1;
create table if not exists receivable.power_supply_disconnection_request_pods
(
    id                                    bigint generated by default as identity
        constraint power_supply_disconnection_request_pods_pk
            primary key,
    customer_id                           bigint                              not null,
    pod_id                                bigint                              not null,
    product_contract_id                   bigint,
    alt_invoice_recipient_customer_id     bigint,
    create_date                           timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                        varchar(50)                         not null,
    modify_date                           timestamp,
    modify_system_user_id                 varchar(50),
    is_checked                            boolean                             not null,
    power_supply_disconnection_request_id bigint                              not null,
    pod_with_highest_consumption          boolean
);

create sequence receivable.power_supply_disconnection_request_pod_liabilities_id_seq increment by 1;
create table if not exists receivable.power_supply_disconnection_request_pod_liabilities
(
    id                                        bigint generated by default as identity
        constraint power_supply_disconnection_request_pod_liab_pk
            primary key,
    power_supply_disconnection_request_pod_id bigint                                not null,
--         constraint power_supply_dcn_request_pod_fk
--             references receivable.power_supply_disconnection_request_pods,
    customer_liability_id                     bigint                                not null,
--         constraint power_supply_dcn_request_pod_liability_fk
--             references receivable.customer_liabilities,
    liability_amount                          numeric                               not null,
    create_date                               timestamp   default CURRENT_TIMESTAMP not null,
    system_user_id                            varchar(50)                           not null,
    modify_date                               timestamp,
    modify_system_user_id                     varchar(50) default 50
);


create type receivable.rescheduling_interest_type as enum ('INTEREST_WITH_EVERY_INSTALLMENT', 'INTEREST_WITH_THE_FIRST_INSTALLMENT', 'INTEREST_WITH_LAST_INSTALLMENT', 'FIRST_INSTALLMENT_INTEREST_ONLY');
create type receivable.rescheduling_status as enum ('DRAFT', 'EXECUTED');
create table if not exists receivable.reschedulings
(
    id                                     bigint generated by default as identity
        constraint rescheduling_pk
            primary key,
    rescheduling_number                    varchar(24)                           not null,
    customer_assessment_id                 bigint                                not null,
    rescheduling_status                    receivable.rescheduling_status        not null,
    status                                 receivable.general_status             not null,
    customer_id                            bigint                                not null,
    number_of_installment                  integer,
    amount_of_the_installment              numeric,
    currency_id                            integer                               not null,
    interest_rate_id                       bigint,
    interest_rate_id_for_installments      bigint                                not null,
    installment_due_day                    smallint
        constraint rescheduling_installment_due_day_chk
            check ((installment_due_day >= 1) AND (installment_due_day <= 31)),
    interest_type                          receivable.rescheduling_interest_type not null,
    create_date                            timestamp default CURRENT_TIMESTAMP   not null,
    system_user_id                         char(50)                              not null,
    modify_date                            timestamp,
    modify_system_user_id                  varchar(50),
    customer_communication_id              bigint,
    customer_detail_id                     bigint,
    customer_communication_id_for_contract bigint,
    initial_amount                         numeric,
    current_amount                         numeric,
    execution_date                         date,
    reversal_date                          varchar,
    reversed                               boolean
);
create table receivable.rescheduling_agreement_templates
(
    id                    bigint generated by default as identity
        constraint rescheduling_agreement_templates_pk
            primary key,
    rescheduling_id       bigint                                             not null,
    template_id           bigint                                             not null,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table receivable.rescheduling_files
(
    id                    bigint generated by default as identity
        constraint rescheduling_files_pk
            primary key,
    name                  varchar(200)                                       not null,
    file_url              varchar(256),
    rescheduling_id       bigint,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    status                receivable.receivable_subobject_status             not null,
    file_statuses         receivable.file_status[],
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    archived_file_type    varchar(50)
);
create table receivable.rescheduling_liabilities
(
    id                     bigint generated by default as identity
        constraint rescheduling_liabilities_pk
            primary key,
    customer_liabilitie_id bigint                              not null,
    current_amount         numeric                             not null,
    interests_from_date    date,
    create_date            timestamp default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)                         not null,
    modify_date            timestamp,
    modify_system_user_id  varchar(50),
    rescheduling_id        bigint                              not null
);
create table receivable.rescheduling_plans
(
    id                    bigint generated by default as identity
        constraint rescheduling_plans_pk
            primary key,
    rescheduling_id       bigint                              not null,
    installment_number    varchar(24)                         not null,
    amount                numeric                             not null,
    principal_amount      numeric                             not null,
    interest_amount       numeric                             not null,
    fee                   numeric                             not null,
    due_date              date                                not null,
    create_date           timestamp default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                         not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    amount_without_interests numeric
);
create table receivable.rescheduling_tasks
(
    id                    bigint generated by default as identity
        constraint rescheduling_tasks_pk
            primary key,
    rescheduling_id       bigint                                             not null,
    task_id               bigint                                             not null,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create table receivable.rescheduling_signable_documents
(
    id                    bigint generated by default as identity
        constraint rescheduling_signable_documents_pk
            primary key,
    document_id           bigint,
    rescheduling_id       bigint,
    create_date           timestamp with time zone,
    system_user_id        varchar(50),
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                receivable.general_status
);

create type receivable.objection_to_change_of_cbg_status as enum ('DRAFT', 'IN_PROGRESS', 'SEND');
create sequence if not exists receivable.objection_to_change_of_cbg_id_seq increment by 1;
create table if not exists receivable.objection_to_change_of_cbg
(
    id                    bigint generated by default as identity
        constraint objection_to_change_of_cbg_pk
            primary key,
    change_of_cbg_number  varchar(24)                                  not null,
    grid_operator_id      integer                                      not null,
--         constraint objection_to_change_of_cbg_grid_operator_fk
--             references nomenclature.grid_operators,
    change_date           date                                         not null,
    change_of_cbg_status  receivable.objection_to_change_of_cbg_status not null,
    create_date           timestamp default CURRENT_TIMESTAMP          not null,
    system_user_id        varchar(50)                                  not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    status                receivable.general_status                    not null,
    email_template_id     bigint
);

create sequence if not exists receivable.objection_to_change_of_cbg_templates_id_seq increment by 1;
create table if not exists receivable.objection_to_change_of_cbg_email_templates
(
    id                    bigint generated by default as identity
        constraint objection_to_change_of_cbg_email_templates_pk
            primary key,
    change_of_cbg_id      bigint                                             not null,
--         constraint objection_to_change_of_cbg_email_templates_cbg_fk
--             references receivable.objection_to_change_of_cbg,
    template_id           bigint                                             not null,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence if not exists receivable.objection_to_change_of_cbg_templates_id_seq increment by 1;
create table if not exists receivable.objection_to_change_of_cbg_templates
(
    id                            bigint                                             not null
        constraint objection_to_change_of_cbg_templates_pk
            primary key,
    template_id                   bigint                                             not null,

    objection_to_change_of_cbg_id bigint                                             not null,

    status                        billing.billing_subobject_status                   not null,
    create_date                   timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                varchar(50)                                        not null,
    modify_date                   timestamp with time zone,
    modify_system_user_id         varchar(50)
);

create sequence if not exists receivable.objection_to_change_of_cbg_sub_files_id_seq increment by 1;
create table receivable.objection_to_change_of_cbg_sub_files
(
    id                      bigint generated by default as identity
        constraint obj_to_change_of_cbg_sub_files_pk
            primary key,
    name                    varchar(200)                                       not null,
    file_url                varchar(256)                                       not null,
    obj_to_change_of_cbg_id bigint,
    create_date             timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id          varchar(50)                                        not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50),
    status                  receivable.receivable_subobject_status             not null,
    file_statuses           receivable.file_status[]
);

create sequence if not exists receivable.objection_to_change_of_cbg_pods_id_seq increment by 1;
create table if not exists receivable.objection_to_change_of_cbg_pods
(
    id                    bigint generated by default as identity
        constraint objection_to_change_of_cbg_pods_pk
            primary key,
    change_of_cbg_id      bigint                              not null,
--         constraint objection_to_change_of_cbg_fk
--             references receivable.objection_to_change_of_cbg,
    pod_id                bigint                              not null,
--         constraint objection_to_change_of_cbg_pods_fk
--             references pod.pod,
    create_date           timestamp default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                         not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    constraint objection_to_change_of_cbg_pod_cbg_uk
        unique (change_of_cbg_id, pod_id)
);

create table if not exists receivable.objection_to_change_of_cbg_tasks
(
    id                    bigint generated by default as identity
        constraint objection_to_change_of_cbg_tasks_pk
            primary key,
    change_of_cbg_id      bigint                                             not null,
--         constraint objection_to_change_of_cbg_tasks_cbg_fk
--             references receivable.objection_to_change_of_cbg,
    task_id               bigint                                             not null,
--         constraint objection_to_change_of_cbg_tasks_fk
--             references task.tasks,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table if not exists receivable.objection_to_change_of_cbg_doc_templates
(
    id                    bigint generated by default as identity
        constraint objection_to_change_of_cbg_doc_templates_pk
            primary key,
    change_of_cbg_id      bigint                                             not null,
--         constraint objection_to_change_of_cbg_doc_templates_cbg_fk
--             references receivable.objection_to_change_of_cbg,
    template_id           bigint                                             not null,
--         constraint objection_to_change_of_cbg_doc_templates_fk
--             references template.templates,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    file_url              varchar(255)                                       not null,
    name                  varchar(255),
    document_id           bigint
);

create table if not exists receivable.objection_to_change_of_cbg_files
(
    id                    bigint generated by default as identity
        constraint objection_to_change_of_cbg_files_pk
            primary key,
    name                  varchar(200)                                       not null,
    file_url              varchar(256)                                       not null,
    change_of_cbg_id      bigint,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    status                receivable.receivable_subobject_status             not null
);

create table if not exists receivable.objection_to_change_of_cbg_process_results
(
    id                                             bigint generated by default as identity
        constraint objection_to_change_of_cbg_process_results_pk
            primary key,
    customer_id                                    bigint                              not null,
--         constraint obj_to_change_of_cbg_proc_result_customer_fk
--             references customer.customers,
    pod_id                                         bigint                              not null,
    grounds_for_obj_withdrawal_to_change_of_cbg_id integer,
--         constraint obj_to_change_of_cbg_proc_result_fk
--             references nomenclature.grounds_for_objection_withdrawal_to_change_of_cbg,
    overdue_amount_for_contract                    numeric                             not null,
    overdue_amount_for_billing_group               numeric                             not null,
    overdue_amount_for_pod                         numeric                             not null,
    balancing_group_coordinator_ground_id          integer,
--         constraint obj_to_change_of_cbg_proc_result_cbg_ground_fk
--             references nomenclature.balancing_group_coordinator_grounds,
    is_checked                                     boolean                             not null,
    create_date                                    timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                                 varchar(50)                         not null,
    modify_date                                    timestamp,
    modify_system_user_id                          varchar(50),
    change_of_cbg_id                               bigint                              not null
--         constraint obj_to_change_of_cbg_proc_result_cbg_fk
--             references receivable.objection_to_change_of_cbg
);

create table receivable.power_supply_dcn_cancellation_tasks
(
    id                               bigint generated by default as identity
        constraint power_supply_dcn_cancellation_tasks_pk
            primary key,
    power_supply_dcn_cancellation_id bigint                                             not null,
    task_id                          bigint                                             not null,
    status                           receivable.receivable_subobject_status             not null,
    create_date                      timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                   varchar(50)                                        not null,
    modify_date                      timestamp with time zone,
    modify_system_user_id            varchar(50)
);

create table receivable.power_supply_dcn_cancellation_pods
(
    id                               bigint generated by default as identity
        constraint power_supply_dcn_cancellation_pods_pk
            primary key,
    customer_id                      bigint                              not null,
    pod_id                           bigint                              not null,
    is_checked                       boolean                             not null,
    cancellation_reason_id           integer,
    create_date                      timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                   varchar(50)                         not null,
    modify_date                      timestamp,
    modify_system_user_id            varchar(50),
    power_supply_dcn_cancellation_id bigint                              not null
);

create table receivable.power_supply_dcn_cancellation_files
(
    id                               bigint generated by default as identity
        constraint power_supply_dcn_cancellation_files_pk
            primary key,
    name                             varchar(200)                                       not null,
    file_url                         varchar(256),
    power_supply_dcn_cancellation_id bigint,
    create_date                      timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                   varchar(50)                                        not null,
    modify_date                      timestamp,
    modify_system_user_id            varchar(50),
    status                           receivable.receivable_subobject_status             not null,
    file_statuses                    receivable.file_status[],
    is_archived                      boolean,
    document_id                      bigint,
    file_id                          bigint,
    archived_file_type               varchar(50)
);

create type nomenclature.grid_operator_taxes_supplier_type as enum ('CURRENT', 'PREVIOUS');
create table nomenclature.grid_operator_taxes
(
    id                                                         integer generated by default as identity
        constraint grid_operator_taxes_pk
            primary key,
    grid_operator_id                                           integer                                        not null,
    tax_for_reconnection                                       numeric                                        not null,
    tax_for_express_reconnection                               numeric                                        not null,
    currency_id                                                integer                                        not null,
    ordering_id                                                integer                                        not null
        constraint grid_operator_taxes_ordering_uk
            unique
                deferrable initially deferred,
    is_default                                                 boolean                                        not null,
    system_user_id                                             varchar(50)                                    not null,
    status                                                     nomenclature.status_enum                       not null,
    create_date                                                timestamp default CURRENT_TIMESTAMP            not null,
    modify_date                                                timestamp,
    modify_system_user_id                                      varchar(50),
    disconnection_type                                         char(1024)                                     not null,
    supplier_type                                              nomenclature.grid_operator_taxes_supplier_type not null,
    remove_tax_in_cancelation                                  boolean                                        not null,
    default_for_pod_with_measurement_type_slp                  boolean                                        not null,
    default_for_pod_with_measurement_type_by_settlement_period boolean                                        not null,
    number_of_income_account                                   varchar(32),
    basis_for_issuing                                          varchar(1024),
    cost_center_controlling_order                              varchar(32),
    price_component_or_price_component_group_or_item           varchar(1024),
    email_template_id                                          integer,
    document_template_id                                       integer
);

create sequence if not exists invoice.manual_invoice_summary_data_id_seq increment by 1 minvalue 0;
create table if not exists invoice.manual_invoice_summary_data
(
    id                                        bigint NOT NULL primary key DEFAULT nextval('invoice.manual_invoice_summary_data_id_seq'),
    price_component_or_price_component_groups varchar(1024),
    total_volumes                             numeric,
    measures_unit_for_total_volumes           varchar(512),
    unit_price                                numeric,
    measure_for_unit_price                    varchar(512),
    value                                     numeric,
    value_currency_id                         bigint,
    income_account_number                     varchar(32),
    cost_center                               varchar(32),
    vat_rate_id                               bigint,
    create_date                               timestamp with time zone    default CURRENT_TIMESTAMP not null,
    modify_date                               timestamp with time zone,
    system_user_id                            varchar(50),
    modify_system_user_id                     varchar(50),
    invoice_id                                bigint,
    vat_rate_percent                          numeric,
    value_currency_exchange_rate              numeric,
    value_currency_name                       varchar(1024),
    vat_rate_name                             varchar(1024)
);

create sequence if not exists invoice.manual_invoice_detailed_data_id_seq increment by 1 minvalue 0;
create table if not exists invoice.manual_invoice_detailed_data
(
    id                                        bigint NOT NULL primary key DEFAULT nextval('invoice.manual_invoice_detailed_data_id_seq'),
    price_component_or_price_component_groups varchar(1024),
    pod                                       varchar(33),
    period_from                               date,
    period_to                                 date,
    meter                                     varchar(32),
    new_meter_reading                         numeric,
    old_meter_reading                         numeric,
    differences                               numeric,
    multiplier                                numeric,
    correction                                numeric,
    deducted                                  numeric,
    total_volumes                             numeric,
    measures_unit_for_total_volumes           varchar(512),
    unit_price                                numeric,
    measure_for_unit_price                    varchar(512),
    value                                     numeric,
    value_currency_id                         bigint,
    income_account_number                     varchar(32),
    cost_center                               varchar(32),
    vat_rate_id                               bigint,
    create_date                               timestamp with time zone    default CURRENT_TIMESTAMP not null,
    modify_date                               timestamp with time zone,
    system_user_id                            varchar(50),
    modify_system_user_id                     varchar(50),
    invoice_id                                bigint,
    vat_rate_percent                          numeric,
    value_currency_exchange_rate              numeric,
    value_currency_name                       varchar(1024),
    vat_rate_name                             varchar(1024),
    constraint period_check
        check (period_from <= period_to)
);

create sequence billing.billing_run_draft_invoices_marks_id_seq increment by 1;
create table billing.billing_run_draft_invoices_marks
(
    id                    bigint      NOT NULL primary key DEFAULT nextval('billing.billing_run_draft_invoices_marks_id_seq'),
    billing_run_id        bigint      not null,
    invoice_id            bigint      not null,
    create_date           timestamp with time zone         default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50) not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create sequence billing.billing_run_draft_generated_invoices_marks_id_seq increment by 1;
create table billing.billing_run_draft_pdf_invoices_marks
(
    id                    bigint NOT NULL primary key DEFAULT nextval('billing.billing_run_draft_generated_invoices_marks_id_seq'),
    billing_run_id        bigint not null,
    invoice_id            bigint not null,
    create_date           timestamp with time zone    default CURRENT_DATE not null,
    modify_date           timestamp with time zone,
    system_user_id        varchar(50),
    modify_system_user_id varchar(50)
);

create table product_contract.contract_price_components
(
    id                                  bigint generated by default as identity
        constraint contract_price_components_pk
            primary key,
    value                               numeric,
    price_component_formula_variable_id bigint                                             not null,
--         constraint contract_price_components_fk
--             references price_component.price_component_formula_variables,
    status                              product_contract.contract_subobject_status         not null,
    create_date                         timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                      varchar(50)                                        not null,
    modify_date                         timestamp with time zone,
    modify_system_user_id               varchar(50),
    contract_detail_id                  bigint                                             not null
--         constraint contract_price_components_contract_fk
--             references product_contract.contract_details
);

create table invoice.invoice_standard_detailed_data
(
    id                                         bigint generated always as identity,
    invoice_id                                 bigint                               not null,
    detail_type                                invoice.invoice_standard_detail_type not null,
    pc_id                                      bigint,
    pod_id                                     bigint,
    pod_detail_id                              bigint,
    date_from                                  date,
    date_to                                    date,
    customer_detail_id                         bigint,
    product_contract_detail_id                 bigint,
    service_contract_detail_id                 bigint,
    service_detail_id                          bigint,
    product_detail_id                          bigint,
    total_volumes                              numeric,
    unit_price                                 numeric,
    main_currency_total_amount_without_vat     numeric,
    main_currency_total_amount_with_vat        numeric,
    main_currency_total_amount_vat             numeric,
    main_currency_id                           bigint,
    alt_currency_total_amount_without_vat      numeric,
    alt_currency_total_amount_with_vat         numeric,
    alt_currency_total_amount_vat              numeric,
    alt_currency_id                            bigint,
    original_currency_total_amount_without_vat numeric,
    original_currency_total_amount_with_vat    numeric,
    original_currency_total_amount_vat         numeric,
    original_currency_id                       numeric,
    vat_rate_id                                bigint,
    vat_rate_percent                           numeric,
    new_meter_reading                          numeric,
    old_meter_reading                          numeric,
    difference                                 numeric,
    multiplier                                 numeric,
    correction                                 numeric,
    deducted                                   numeric,
    measures_unit_for_total_volumes            integer,
    measure_unit_for_unit_price                integer,
    income_account_number                      varchar(32),
    cost_center_controlling_order              varchar(32),
    interim_deduction_invoice_id               bigint,
    create_date                                timestamp with time zone default CURRENT_TIMESTAMP,
    modify_system_user_id                      varchar(50),
    modify_date                                timestamp with time zone,
    system_user_id                             varchar(50),
    pc_group_detail_id                         bigint,
    tariff                                     boolean,
    meter_id                                   bigint,
    interim_id                                 bigint,
    discount_id                                bigint,
--         constraint detailed_data_discount_id_fk
--             references pod.discounts,

    price_component_price_type_id              bigint,
    billing_data_scale_ids                     bigint[],
    billing_data_profile_ids                   bigint[],
    restricted                                 boolean,
    discounted                                 boolean,
    unrecognized_pod                           varchar(500),
    scale_id                                   integer
);

create table invoice.manual_debit_or_credit_note_invoice_detailed_data
(
    id                                        bigint generated by default as identity
        constraint manual_debit_or_credit_note_invoice_detailed_data_pk
            primary key,
    price_component_or_price_component_groups varchar(1024),
    pod                                       varchar(33),
    period_from                               date,
    period_to                                 date,
    meter                                     varchar(32),
    new_meter_reading                         numeric,
    old_meter_reading                         numeric,
    differences                               numeric,
    multiplier                                numeric,
    correction                                numeric,
    deducted                                  numeric,
    total_volumes                             numeric,
    measures_unit_for_total_volumes           varchar(512),
    unit_price                                numeric,
    measure_for_unit_price                    varchar(512),
    value                                     numeric,
    value_currency_id                         bigint,
    income_account_number                     varchar(32),
    cost_center                               varchar(32),
    vat_rate_id                               bigint,
    create_date                               timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date                               timestamp with time zone,
    system_user_id                            varchar(50),
    modify_system_user_id                     varchar(50),
    invoice_id                                bigint                                             not null,
    vat_rate_percent                          numeric,
    value_currency_exchange_rate              numeric,
    value_currency_name                       varchar(1024),
    vat_rate_name                             varchar(1024),
    constraint period_check
        check (period_from <= period_to)
);

create sequence invoice.manual_debit_or_credit_note_invoice_summery_data_id_seq increment by 1;
create table invoice.manual_debit_or_credit_note_invoice_summary_data
(
    id                                        bigint NOT NULL primary key DEFAULT nextval('invoice.manual_debit_or_credit_note_invoice_summery_data_id_seq'),
    price_component_or_price_component_groups varchar(1024),
    total_volumes                             numeric,
    measures_unit_for_total_volumes           varchar(512),
    unit_price                                numeric,
    measure_for_unit_price                    varchar(512),
    value                                     numeric,
    value_currency_id                         bigint,
    income_account_number                     varchar(32),
    cost_center                               varchar(32),
    vat_rate_id                               bigint,
    create_date                               timestamp with time zone    default CURRENT_TIMESTAMP not null,
    modify_date                               timestamp with time zone,
    system_user_id                            varchar(50),
    modify_system_user_id                     varchar(50),
    invoice_id                                bigint not null,
    vat_rate_percent                          numeric,
    value_currency_exchange_rate              numeric,
    value_currency_name                       varchar(1024),
    vat_rate_name                             varchar(1024)
);

create table invoice.invoice_cancelations
(
    id                    bigint generated by default as identity
        constraint invoice_cancelations_pk
            primary key,
    process_id            bigint                                             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    tax_event_date        date,
    contract_template_id  bigint,
--         constraint invoice_cancelations_templates_id_fk
--             references template.templates,
    email_template_id     bigint
--         constraint invoice_cancelations_email_templates_id_fk
--             references template.templates
);

create type nomenclature.default_assignment_type as enum ('ALL', 'DEFAULT_FOR_LIABILITIES', 'DEFAULT_FOR_RECEIVABLES', 'DEFAULT_FOR_LPF_LIABILITIES', 'DEFAULT_FOR_LPF_RECEIVABLES', 'DEFAULT_FOR_DEPOSIT', 'DEFAULT_FOR_PRO_FORMA_INVOICE', 'DEFAULT_FOR_LPF', 'DEFAULT_FOR_ACTION', 'DEFAULT_FOR_ACTION_LIABILITY');
create table nomenclature.income_account
(
    id                      integer generated by default as identity
        constraint income_account_pk
            primary key,
    name                    varchar(2048)                                      not null,
    number                  varchar(32)                                        not null,
    is_default              boolean                                            not null,
    system_user_id          varchar(50)                                        not null,
    status                  nomenclature.status_enum                           not null,
    ordering_id             integer                                            not null
        constraint income_account_ordering_uk
            unique
                deferrable initially deferred,
    create_date             timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50),
    default_assignment_type nomenclature.default_assignment_type[]
);

create type receivable.power_supply_reconnection_status as enum ('DRAFT', 'EXECUTED');
create sequence receivable.power_supply_reconnections_id_seq increment by 1;
create table receivable.power_supply_reconnections
(
    id                    bigint generated by default as identity
        constraint power_supply_reconnections_pk
            primary key,
    reconnection_number   varchar(24)                                 not null,
    reconnection_status   receivable.power_supply_reconnection_status not null,
    grid_operator_id      integer                                     not null,
--         constraint power_supply_reconnection_grid_operator_fk
--             references nomenclature.grid_operators,
    status                receivable.general_status                   not null,
    create_date           timestamp default CURRENT_TIMESTAMP         not null,
    system_user_id        varchar(50)                                 not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    internal_template_id  bigint,
    -- PHN-3854: set when status becomes EXECUTED; Job N2 lookback uses this instead of create_date
    execution_date        timestamp with time zone
);
create sequence receivable.power_supply_reconnection_files_id_seq increment by 1;
create table receivable.power_supply_reconnection_files
(
    id                           bigint generated by default as identity
        constraint power_supply_reconnection_files_pk
            primary key,
    name                         varchar(200)                                       not null,
    file_url                     varchar(256),
    power_supply_reconnection_id bigint,
--         constraint power_supply_reconnection_files_reconnection_fk
--             references receivable.power_supply_reconnections,
    create_date                  timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id               varchar(50)                                        not null,
    modify_date                  timestamp,
    modify_system_user_id        varchar(50),
    status                       receivable.receivable_subobject_status             not null,
    file_statuses                receivable.file_status[],
    is_archived                  boolean,
    document_id                  bigint,
    file_id                      bigint,
    archived_file_type           varchar(50)
);

create sequence receivable.power_supply_reconnection_pods_id_seq increment by 1;
create table if not exists receivable.power_supply_reconnection_pods
(
    id                                    bigint generated by default as identity
        constraint power_supply_reconnection_pods_pk
            primary key,
    customer_id                           bigint                              not null,
    pod_id                                bigint                              not null,
    power_supply_disconnection_request_id bigint                              not null,
    cancelation_reason_id                 integer,
    create_date                           timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                        varchar(50)                         not null,
    power_supply_reconnection_id          bigint                              not null,
    modify_date                           timestamp,
    modify_system_user_id                 varchar(50),
    reconnection_date                     date,
    express_reconnection                  boolean default false not null
);

create table receivable.power_supply_reconnection_templates
(
    id                           bigint generated by default as identity
        constraint power_supply_reconnection_templates_pk
            primary key,
    power_supply_reconnection_id bigint                                             not null,
--         constraint power_supply_reconnection_fk
--             references receivable.power_supply_reconnections,
    template_id                  bigint                                             not null,
    status                       receivable.receivable_subobject_status             not null,
    create_date                  timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id               varchar(50)                                        not null,
    modify_date                  timestamp with time zone,
    modify_system_user_id        varchar(50)
);

create table receivable.power_supply_reconnection_tasks
(
    id                           bigint generated by default as identity
        constraint power_supply_reconnection_tasks_pk
            primary key,
    power_supply_reconnection_id bigint                                             not null,
--         constraint power_supply_reconnection_fk
--             references receivable.power_supply_reconnections,
    task_id                      bigint                                             not null,
--         constraint power_supply_reconnection_tasks_fk
--             references task.tasks,
    status                       receivable.receivable_subobject_status             not null,
    create_date                  timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id               varchar(50)                                        not null,
    modify_date                  timestamp with time zone,
    modify_system_user_id        varchar(50)
);

create view receivable.vw_power_supply_reconnection_dcn_requests
            (power_supply_reconnection_id, power_supply_disconnection_request_number,
             power_supply_disconnection_request_number_desc)
as
SELECT DISTINCT psrp.power_supply_reconnection_id,
                string_agg(psdr.request_number::text, '/'::text)
                OVER (PARTITION BY psrp.power_supply_reconnection_id ORDER BY psdr.request_number ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS power_supply_disconnection_request_number,
                string_agg(psdr.request_number::text, '/'::text)
                OVER (PARTITION BY psrp.power_supply_reconnection_id ORDER BY psdr.request_number DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS power_supply_disconnection_request_number_desc
FROM receivable.power_supply_reconnection_pods psrp
         JOIN receivable.power_supply_disconnection_requests psdr
              ON psrp.power_supply_disconnection_request_id = psdr.id;
create type crm.file_status as enum ('DRAFT', 'SIGNED');
create table crm.sms_communications
(
    id                                 bigint generated by default as identity
        constraint sms_communications_pk
            primary key,
    communication_as_an_institution    boolean                             not null,
    communication_topic_id             integer                             not null,
--         constraint sms_communications_topic_fk
--             references nomenclature.communication_topics,
    communication_type                 crm.sms_communication_type          not null,
    sent_date                          timestamp,
    sms_sending_number_id              integer                             not null,
--         constraint sms_communications_sms_sending_number_fk
--             references nomenclature.sms_sending_numbers,
    sms_body                           text                                not null,
    sender_employee_id                 bigint,
--         constraint sms_communications_sender_employee_fk
--             references customer.account_managers,
    create_date                        timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                     varchar(50)                         not null,
    modify_date                        timestamp,
    modify_system_user_id              varchar(50),
    communication_channel              crm.sms_communication_channels      not null,
    status                             crm.general_status,
    communication_status               crm.sms_comm_status,
    template_id                        bigint,
--         constraint sms_communication_template_fk
--             references template.templates,
    all_customers_with_active_contract boolean
);

create type crm.sms_communication_subobject_status as enum ('ACTIVE', 'DELETED');

create table crm.sms_communication_tasks
(
    id                    bigint generated by default as identity
        constraint sms_communication_tasks_pk
            primary key,
    sms_communication_id  bigint                                             not null,
    task_id               bigint                                             not null,
    status                crm.sms_communication_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table crm.sms_communication_sms_templates
(
    id                    bigint generated by default as identity
        constraint sms_communication_sms_templates_pk
            primary key,
    sms_communication_id  bigint                                 not null,
    template_id           bigint                                 not null,
    status                crm.sms_communication_subobject_status not null,
    create_date           timestamp default CURRENT_TIMESTAMP    not null,
    system_user_id        varchar(50)                            not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50)
);

create table crm.sms_communication_related_customers
(
    id                    bigint generated by default as identity
        constraint sms_communication_related_customers_pk
            primary key,
    sms_communication_id  bigint                                 not null,
    customer_id           bigint                                 not null,
    status                crm.sms_communication_subobject_status not null,
    create_date           timestamp default CURRENT_TIMESTAMP    not null,
    system_user_id        varchar(50)                            not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50)
);

create table crm.sms_communication_files
(
    id                    bigint generated by default as identity
        constraint sms_communication_files_pk
            primary key,
    name                  varchar(200)                                       not null,
    file_url              varchar(256),
    sms_communication_id  bigint,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    status                crm.sms_communication_subobject_status             not null,
    is_report             boolean,
    is_after_send_report  boolean,
    file_statuses         crm.file_status[],
    is_archived           boolean,
    document_id           bigint,
    file_id               bigint,
    archived_file_type    varchar(50)
);

create table crm.sms_communication_customers
(
    id                         bigint generated by default as identity
        constraint sms_communication_customers_pk
            primary key,
    customer_detail_id         bigint,
    customer_communication_id  bigint,
    sms_communication_id       bigint,
    create_date                timestamp default CURRENT_TIMESTAMP not null,
    system_user_id             varchar(50)                         not null,
    modify_date                timestamp,
    modify_system_user_id      varchar(50),
    sms_comm_status            crm.sms_comm_status,
    sms_body                   text,
    product_contract_detail_id bigint,
    service_contract_detail_id bigint,
    contract_number            text,
    contract_id                bigint
);

create sequence if not exists receivable.customer_payment_receivable_offsettings_id_seq increment by 1;
create table receivable.customer_payment_receivable_offsettings
(
    id                     bigint                    NOT NULL DEFAULT nextval('receivable.customer_payment_receivable_offsettings_id_seq'),
    customer_payment_id    bigint                    not null,
    customer_receivable_id bigint                    not null,
    amount                 numeric                   not null,
    currency_id            integer                   not null,
    status                 receivable.general_status not null,
    create_date            timestamp with time zone           default CURRENT_TIMESTAMP not null,
    system_user_id         varchar(50)               not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  varchar(50)
);
create table crm.sms_communication_customer_contacts
(
    id                                bigint generated by default as identity
        constraint sms_communication_customer_contacts_pk
            primary key,
    customer_communication_contact_id bigint,
    phone_number                      varchar(128),
    create_date                       timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                    varchar(50)                         not null,
    modify_date                       timestamp,
    modify_system_user_id             varchar(50),
    sms_communication_customer_id     bigint                              not null,
    constraint sms_communication_customer_contacts_chk
        check ((customer_communication_contact_id IS NOT NULL) OR (phone_number IS NOT NULL))
);

create table crm.sms_communication_contact_purposes
(
    id                    bigint generated by default as identity
        constraint sms_communication_contact_purposes_pk
            primary key,
    sms_communication_id  bigint                                 not null,
    contact_purpose_id    integer                                not null,
    create_date           timestamp default CURRENT_TIMESTAMP    not null,
    system_user_id        varchar(50)                            not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    status                crm.sms_communication_subobject_status not null
);

create table crm.sms_communication_activity
(
    id                    bigint generated by default as identity
        constraint sms_communication_activity_pk
            primary key,
    sms_communication_id  bigint                                             not null,
    activity_id           integer                                            not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                crm.sms_communication_subobject_status             not null
);

create sequence if not exists receivable.customer_deposit_payment_deadline_after_withdrawal_id_seq
    maxvalue 2147483647;


create table if not exists receivable.customer_deposit_service_orders
(
    id                    bigint generated by default as identity
        constraint customer_deposit_service_orders_pk
            primary key,
    customer_deposit_id   bigint                                             not null,
--         constraint customer_deposit_service_order_deposit_fk
--             references receivable.customer_deposits,
    order_id              bigint,
--         constraint customer_deposit_service_order_fk
--             references service_order.orders,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table if not exists receivable.customer_deposit_service_contracts
(
    id                    bigint generated by default as identity
        constraint customer_deposit_service_contracts_pk
            primary key,
    customer_deposit_id   bigint                                             not null,
    contract_id           bigint,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table if not exists receivable.customer_deposit_product_contracts
(
    id                    bigint generated by default as identity
        primary key,
    customer_deposit_id   bigint                                             not null,
    contract_id           bigint,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create type receivable.template_type as enum ('EMAIL', 'DOCUMENT');
create table if not exists receivable.customer_deposit_email_templates
(
    id                    bigint generated by default as identity
        primary key,
    customer_deposit_id   bigint                                             not null,
    template_id           bigint                                             not null,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    template_type         receivable.template_type
);

create table receivable.customer_deposit_goods_orders
(
    id                    bigint generated by default as identity
        constraint customer_deposit_goods_orders_pk
            primary key,
    customer_deposit_id   bigint                                             not null,
--         constraint customer_deposit_goods_order_deposit_fk
--             references receivable.customer_deposits,
    order_id              bigint,
--         constraint customer_deposit_goods_order_fk
--             references goods_order.orders,
    status                receivable.receivable_subobject_status             not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);


create view receivable.vw_customer_deposit_contract_orders
            (customer_deposit_id, contract_order_number, contract_order_number_desc) as
SELECT DISTINCT tbl.customer_deposit_id,
                string_agg(tbl.contract_order_number::text, ','::text)
                OVER (PARTITION BY tbl.customer_deposit_id ORDER BY tbl.contract_order_number ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)      AS contract_order_number,
                string_agg(tbl.contract_order_number::text, ','::text)
                OVER (PARTITION BY tbl.customer_deposit_id ORDER BY tbl.contract_order_number DESC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS contract_order_number_desc
FROM (SELECT cdpc.customer_deposit_id,
             (SELECT c.contract_number
              FROM product_contract.contracts c
              WHERE c.id = cdpc.contract_id) AS contract_order_number
      FROM receivable.customer_deposit_product_contracts cdpc
      WHERE cdpc.status = 'ACTIVE'::receivable.receivable_subobject_status
      UNION ALL
      SELECT cdsc.customer_deposit_id,
             (SELECT c.contract_number
              FROM service_contract.contracts c
              WHERE c.id = cdsc.contract_id) AS contract_number
      FROM receivable.customer_deposit_service_contracts cdsc
      WHERE cdsc.status = 'ACTIVE'::receivable.receivable_subobject_status
      UNION ALL
      SELECT cdso.customer_deposit_id,
             (SELECT o.order_number
              FROM service_order.orders o
              WHERE o.id = cdso.order_id) AS order_number
      FROM receivable.customer_deposit_service_orders cdso
      WHERE cdso.status = 'ACTIVE'::receivable.receivable_subobject_status
      UNION ALL
      SELECT cdgo.customer_deposit_id,
             (SELECT o.order_number
              FROM goods_order.orders o
              WHERE o.id = cdgo.order_id) AS order_number
      FROM receivable.customer_deposit_goods_orders cdgo
      WHERE cdgo.status = 'ACTIVE'::receivable.receivable_subobject_status) tbl;

create table receivable.late_payment_fine_invoices
(
    id                    bigint generated by default as identity
        constraint late_payment_fine_invoices_pk
            primary key,
    invoice_id            bigint,
--         constraint late_payment_fine_invoices_fk
--             references invoice.invoices,
    late_paid_amount      numeric                             not null,
    overdue_start_date    date                                not null,
    overdue_end_date      date,
    number_of_days        integer,
    percentage            numeric                             not null,
    total_amount          numeric                             not null,
    create_date           timestamp default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                         not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    late_payment_fine_id  bigint                              not null,
    fee                   numeric,
    currency_id           integer,
    invoice_number        varchar(50)
);

create table invoice.invoice_cancellation_documents
(
    id                      bigint generated by default as identity
        constraint invoice_cancellation_documents_pk
            primary key,
    name                    varchar(200)                                       not null,
    file_url                varchar(256)                                       not null,
    invoice_cancellation_id bigint,
--         constraint invoice_cancellation_files_fk
--             references invoice.invoice_cancelations,
    create_date             timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id          varchar(50)                                        not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50),
    status                  invoice.invoice_subobject_status                   not null
);

create table if not exists nomenclature.pod_additional_params
(
    id                    integer generated by default as identity
        constraint pod_additional_params_pk
            primary key,
    name                  varchar(1024)                                      not null,
    is_default            boolean                                            not null,
    system_user_id        varchar(50)                                        not null,
    status                nomenclature.status_enum                           not null,
    ordering_id           integer                                            not null
        constraint pod_additional_params_ordering_uk
            unique
                deferrable initially deferred,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table if not exists invoice.invoice_cancellation_numbers
(
    id                    bigint                              not null,
    number                integer                             not null,
    cancellation_date     timestamp,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    create_date           timestamp default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                         not null,
    invoice_id            bigint                              not null
);

create table if not exists invoice.invoice_document_files
(
    id                    bigint                              not null
        constraint invoice_document_files_pk
            primary key,
    create_date           timestamp default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                         not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50),
    document_id           bigint                              not null,
--         constraint invoice_document_files_documents_fk
--             references template.document,
    invoice_id            bigint
--         constraint invoice_document_file_invoice_fk
--             references invoice.invoices
);

create sequence invoice.invoice_document_files_id_seq;

create type receivable.poewr_supply_disconnection_reminder_doc_files_status as enum ('ACTIVE', 'DELETED');

create table receivable.power_supply_disconnection_reminder_doc_files
(
    id                            bigint generated by default as identity
        constraint power_supply_disconnection_reminder_doc_files_pk
            primary key,
    reminder_for_disconnection_id bigint                                                          not null,
--         constraint power_supply_disconnection_reminder_doc_files_fk
--             references receivable.power_supply_disconnection_reminders,
    template_id                   bigint                                                          not null,
--         constraint power_supply_disconnection_reminder_doc_files_template_fk
--             references template.templates,
    customer_id                   bigint                                                          not null,
--         constraint power_supply_disconnection_reminder_doc_files_customer_fk
--             references customer.customers,
    status                        receivable.poewr_supply_disconnection_reminder_doc_files_status not null,
    create_date                   timestamp with time zone default CURRENT_TIMESTAMP              not null,
    system_user_id                varchar(50)                                                     not null,
    modify_date                   timestamp with time zone,
    modify_system_user_id         varchar(50),
    file_url                      varchar(255)                                                    not null,
    name                          varchar(255)
);

create table if not exists billing.billing_invoice_sum_files
(
    id                    bigint
        constraint billing_invoice_sum_files_pk
            primary key,
    document_id           bigint,
--         constraint billing_invoice_sum_files_document_fk
--             references template.document,
    billing_id            bigint,
--         constraint billing_invoice_sum_files_billing_fk
--             references billing.billings,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    status                billing.billing_subobject_status,
    number_of_invoices       bigint,
    total_amount_of_invoices numeric,
    execution_date           timestamp
);

drop sequence if exists nomenclature.missing_customer_id_seq;
create sequence if not exists nomenclature.missing_customer_id_seq;
create table if not exists nomenclature.missing_customer
(
    id                        bigserial
        primary key,
    uic                       varchar(13)                                        not null,
    name                      varchar(2048)                                      not null,
    name_transliterated       varchar(2048)                                      not null,
    legal_form                varchar(128)                                       not null,
    legal_form_transliterated varchar(128)                                       not null,
    system_user_id            varchar(50)                                        not null,
    status                    nomenclature.status_enum                           not null,
    create_date               timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date               timestamp with time zone,
    modify_system_user_id     varchar(50),
    is_default                boolean                                            not null,
    ordering_id               integer                                            not null
        constraint missing_customer_ordering_uk
            unique
                deferrable initially deferred
);

create sequence billing.billing_invoice_sum_files_id_seq;

create table if not exists action.action_signable_documents
(
    id                         bigint generated by default as identity
        constraint action_signable_documents_pk
            primary key,
    document_id                bigint,
--         constraint action_signable_documents_document_id_fk
--             references template.document,
    service_contract_detail_id bigint,
--         constraint action_signable_documents_service_contract_details_id_fk
--             references service_contract.contract_details,
    product_contract_detail_id bigint,
--         constraint action_signable_documents_product_contract_details_id_fk
--             references product_contract.contract_details,
    action_id                  bigint,
--         constraint action_signable_documents_action_id_fk
--             references action.actions,
    create_date                timestamp with time zone,
    system_user_id             varchar(50),
    modify_date                timestamp with time zone,
    modify_system_user_id      varchar(50),
    status                     action.action_subobject_status
);

create schema if not exists lock;
CREATE TABLE IF NOT EXISTS lock.locks
(
    lock_key        varchar(255)                      NOT NULL,
    entity_type     varchar(255)                      NOT NULL,
    entity_id       int8                              NOT NULL,
    lock_owner      varchar(255)                      NOT NULL,
    created_at      timestamp                         NOT NULL,
    expires_at      timestamp                         NOT NULL,
    "version"       int4                              NOT NULL,
    version_id      int8                              NULL,
    has_super_owner bool                              NULL,
    id              int8 GENERATED ALWAYS AS IDENTITY NOT NULL,
    system_lock     bool DEFAULT false                NULL,
    billing_id      int8                              NULL
);

create type billing.compensation_status as enum ('INVOICED', 'NEW', 'UNINVOICED');

create type invoice.invoice_number_type as enum ('DRAFT_INVOICE', 'DRAFT_PROFORMA', 'REAL', 'REAL_PROFORMA');
create sequence invoice.invoice_number_generator_id_seq;

create table if not exists invoice.invoice_number_generator
(
    id                    bigint                                             not null
        constraint invoice_number_generator_pk
            primary key,
    invoice_number        varchar(256),
    type                  invoice.invoice_number_type                        not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

create table if not exists billing.compensations
(
    id                                bigserial
        primary key,
    compensation_document_number      varchar(255)                                       not null
        unique,
    compensation_document_date        date                                               not null,
    compensation_document_volumes     numeric(10, 2)                                     not null,
    compensation_document_price       numeric(10, 2)                                     not null,
    compensation_reason               text,
    compensation_document_period      date                                               not null,
    compensation_document_amount      numeric(10, 2)                                     not null,
    customer_id                       bigint                                             not null,
    pod_id                            bigint                                             not null,
    recipient_id                      bigint                                             not null,
    compensation_index                integer,
    invoice_usage_date                timestamp with time zone,
    create_date                       timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                    varchar(50)                                        not null,
    modify_date                       timestamp with time zone,
    modify_system_user_id             varchar(50),
    invoice_id                        bigint,
--         constraint fk_invoice
--             references invoice.invoices,
    liability_for_recipient_id        bigint,
--         constraint fk_recipient_liability
--             references receivable.customer_liabilities,
    receivable_for_customer_id        bigint,
--         constraint fk_customer_receivable
--             references receivable.customer_receivables,
    liability_for_customer_id         bigint,
--         constraint fk_customer_liability
--             references receivable.customer_liabilities,
    receivable_for_recipient_id       bigint,
--         constraint fk_recipient_receivable
--             references receivable.customer_receivables,
    compensation_status               billing.compensation_status                        not null,
    status                            billing.billing_subobject_status,
    compensation_document_currency_id bigint                                             not null,
    reversed                          boolean                  default false             not null
);
create sequence invoice.real_invoice_number_seq;
create sequence invoice.real_proforma_invoice_number_seq start with 3000000000;
create sequence invoice.draft_invoice_number_seq;
create sequence invoice.draft_proforma_invoice_number_seq;

CREATE FUNCTION receivable.convert_to_currency(
    p_amount NUMERIC,
    p_currency_id INTEGER,
    p_dest_currency_id INTEGER,
    p_dec_points INTEGER DEFAULT 2
) RETURNS NUMERIC
    STABLE
    PARALLEL SAFE
    LANGUAGE plpgsql
AS
'
    DECLARE
        def_cur RECORD;
        rec_cur RECORD;
    BEGIN
        -- Set default destination currency
        IF p_dest_currency_id = 0 THEN
            SELECT id
            INTO p_dest_currency_id
            FROM nomenclature.currencies
            WHERE main_ccy_start_date <= CURRENT_DATE
              AND main_ccy = TRUE
              AND status != ''DELETED''
            ORDER BY main_ccy_start_date DESC
            LIMIT 1;
        END IF;

        -- Fetch currency details
        IF p_dest_currency_id = -1 OR p_dest_currency_id = p_currency_id THEN
            SELECT *
            INTO rec_cur
            FROM nomenclature.currencies
            WHERE id = p_currency_id
              AND status != ''DELETED'';
        ELSE
            SELECT *
            INTO rec_cur
            FROM nomenclature.currencies
            WHERE id = p_currency_id
              AND alt_currency_id = p_dest_currency_id
              AND status != ''DELETED'';
        END IF;

        -- Handle NULL cases
        IF rec_cur.id IS NULL THEN
            RETURN NULL;
        ELSIF p_dest_currency_id = p_currency_id THEN
            RETURN ROUND(p_amount, p_dec_points);
        ELSE
            RETURN ROUND(p_amount * rec_cur.alt_ccy_exchange_rate, p_dec_points);
        END IF;
    END;
';

create type receivable.operation_context as enum ('ALO', 'MLO', 'DLO', 'APO', 'DPO', 'DDO', 'RSC', 'RSR', 'MLR', 'OPO');
create type receivable.transaction_object_type as enum ('LIABILITY', 'RECEIVABLE', 'PAYMENT', 'DEPOSIT', 'RESCHEDULING');
create type receivable.operation_type as enum ('OFFSETTING', 'REVERSAL');
create type receivable.transaction_status as enum ('ACTIVE', 'REVERSED', 'DELETED');

create table if not exists receivable.customer_receivable_transactions
(
    id                       bigint generated by default as identity
        constraint customer_receivable_transactions_pk
            primary key,
    operation_date           timestamp with time zone,
    source_object_type       receivable.transaction_object_type,
    source_object_id         bigint,
    dest_object_type         receivable.transaction_object_type,
    dest_object_id           bigint,
    operation_type           receivable.operation_type,
    amount                   numeric,
    currency_id              integer,
    status                   receivable.transaction_status,
    connected_transaction_id bigint,
    create_date              timestamp with time zone default CURRENT_TIMESTAMP,
    system_user_id           varchar(50),
    modify_date              timestamp with time zone,
    modify_system_user_id    varchar(50),
    old_id                   bigint,
    operation_context        receivable.operation_context
);

create view receivable.customer_liabilitie_paid_by_deposits
            (id, customer_liabilitie_id, customer_deposit_id, amount, currency_id, status, operation_date, create_date,
             operation_context, system_user_id, modify_date, modify_system_user_id)
as
SELECT crt.id,
       crt.dest_object_id   AS customer_liabilitie_id,
       crt.source_object_id AS customer_deposit_id,
       crt.amount,
       crt.currency_id,
       crt.status,
       crt.operation_date,
       crt.create_date,
       crt.operation_context,
       crt.system_user_id,
       crt.modify_date,
       crt.modify_system_user_id
FROM receivable.customer_receivable_transactions crt
WHERE crt.source_object_type = 'DEPOSIT'::receivable.transaction_object_type
  AND crt.dest_object_type = 'LIABILITY'::receivable.transaction_object_type;

create view receivable.customer_liabilitie_paid_by_payments
            (id, customer_liabilitie_id, customer_payment_id, amount, currency_id, status, operation_date, create_date,
             operation_context, system_user_id, modify_date, modify_system_user_id)
as
SELECT crt.id,
       crt.dest_object_id   AS customer_liabilitie_id,
       crt.source_object_id AS customer_payment_id,
       crt.amount,
       crt.currency_id,
       crt.status,
       crt.operation_date,
       crt.create_date,
       crt.operation_context,
       crt.system_user_id,
       crt.modify_date,
       crt.modify_system_user_id
FROM receivable.customer_receivable_transactions crt
WHERE crt.source_object_type = 'PAYMENT'::receivable.transaction_object_type
  AND crt.dest_object_type = 'LIABILITY'::receivable.transaction_object_type;

create view receivable.customer_liabilitie_paid_by_receivables
            (id, customer_liabilitie_id, customer_receivable_id, amount, currency_id, status, operation_date,
             create_date, operation_context, system_user_id, modify_date, modify_system_user_id)
as
SELECT crt.id,
       crt.dest_object_id   AS customer_liabilitie_id,
       crt.source_object_id AS customer_receivable_id,
       crt.amount,
       crt.currency_id,
       crt.status,
       crt.operation_date,
       crt.create_date,
       crt.operation_context,
       crt.system_user_id,
       crt.modify_date,
       crt.modify_system_user_id
FROM receivable.customer_receivable_transactions crt
WHERE crt.source_object_type = 'RECEIVABLE'::receivable.transaction_object_type
  AND crt.dest_object_type = 'LIABILITY'::receivable.transaction_object_type;

create view receivable.customer_liabilitie_paid_by_rescheduling
            (id, customer_liabilitie_id, customer_rescheduling_id, amount, currency_id, status, operation_date,
             create_date, operation_context, system_user_id, modify_date, modify_system_user_id)
as
SELECT crt.id,
       crt.dest_object_id   AS customer_liabilitie_id,
       crt.source_object_id AS customer_rescheduling_id,
       crt.amount,
       crt.currency_id,
       crt.status,
       crt.operation_date,
       crt.create_date,
       crt.operation_context,
       crt.system_user_id,
       crt.modify_date,
       crt.modify_system_user_id
FROM receivable.customer_receivable_transactions crt
WHERE crt.source_object_type = 'RESCHEDULING'::receivable.transaction_object_type
  AND crt.dest_object_type = 'LIABILITY'::receivable.transaction_object_type;

create table if not exists receivable.rescheduling_draft_liabilities
(
    id                    bigint generated by default as identity
        constraint rescheduling_assessment_draft_liabilities_pk
            primary key,
    rescheduling_id       bigint,
    customer_liability_id bigint,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp,
    modify_system_user_id varchar(50)
);

create sequence if not exists receivable.mlo_customer_negative_payments_id_seq increment by 1;
create table if not exists receivable.mlo_customer_negative_payments
(
    id                              bigint generated by default as identity
        constraint mlo_customer_negative_payments_pk
            primary key,
    manual_liabilitie_offsetting_id bigint,
    customer_payment_id             bigint                              not null,
    create_date                     timestamp default CURRENT_TIMESTAMP not null,
    system_user_id                  varchar(50)                         not null,
    modify_date                     timestamp,
    modify_system_user_id           varchar(50),
    before_current_amount           numeric,
    after_current_amount            numeric,
    currency_id                     integer
);

create schema if not exists reporting;

create table if not exists reporting.pod_consumption_cach
(
    id           bigint generated always as identity,
    pod_id       bigint,
    type         integer,
    period_from  date,
    period_to    date,
    total_volume numeric,
    hours        timestamp without time zone[]
);

create sequence if not exists receivable.late_payment_fine_tasks_id_seq;
create table if not exists receivable.late_payment_fine_tasks
(
    id                    bigint                    NOT NULL DEFAULT nextval('receivable.late_payment_fine_tasks_id_seq'),

    late_payment_fine_id  bigint                    not null,
    task_id               bigint                    not null,
    status                receivable.general_status not null,
    create_date           timestamp with time zone           default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)               not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);
create table if not exists receivable.mlo_email_templates
(
    id                              bigint generated by default as identity
        constraint mlo_email_templates_pk
            primary key,
    manual_liabilitie_offsetting_id bigint                                             not null
        constraint mlo_email_templates_offst_fk
            references receivable.manual_liabilitie_offsettings,
    template_id                     bigint                                             not null,
    status                          receivable.receivable_subobject_status             not null,
    create_date                     timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id                  varchar(50)                                        not null,
    modify_date                     timestamp with time zone,
    modify_system_user_id           varchar(50),
    template_type                   receivable.template_type
);



CREATE MATERIALIZED VIEW IF NOT EXISTS customer.mv_customer_address_info
    TABLESPACE pg_default
AS
SELECT cd.id                                                 AS customer_detail_id,
       cd.customer_id,
       cd.economic_branch_ci_id,
       cd.status,
       lower(cn.name::text)                                  AS country_name,
       lower(pp.name::text)                                  AS populated_place_name,
       lower(mu.name::text)                                  AS municipality_name,
       lower(rg.name::text)                                  AS region_name,
       lower(dis.name::text)                                 AS district_name,
       lower(s.name::text)                                   AS street_name,
       lower(ra.name::text)                                  AS residential_area_name,
       lower((((((((((((COALESCE(rg.name, ''::character varying)::text || ', '::text) ||
                       COALESCE(mu.name, ''::character varying)::text) || ', '::text) ||
                     COALESCE(pp.name, ''::character varying)::text) || ', '::text) ||
                   COALESCE(dis.name, ''::character varying)::text) || ', '::text) ||
                 COALESCE(s.name, ''::character varying)::text) || ', '::text) ||
               COALESCE(ra.name, ''::character varying)::text) || ', '::text) ||
             COALESCE(cn.name, ''::character varying)::text) AS full_address,
       c.last_customer_detail_id
FROM customer.customer_details cd
         JOIN customer.customers c ON cd.customer_id = c.id
         LEFT JOIN nomenclature.countries cn ON cd.country_id = cn.id
         LEFT JOIN nomenclature.populated_places pp ON cd.populated_place_id = pp.id
         LEFT JOIN nomenclature.municipalities mu ON pp.municipality_id = mu.id
         LEFT JOIN nomenclature.regions rg ON mu.region_id = rg.id
         LEFT JOIN nomenclature.districts dis ON cd.district_id = dis.id
         LEFT JOIN nomenclature.streets s ON cd.street_id = s.id
         LEFT JOIN nomenclature.residential_areas ra ON cd.residential_area_id = ra.id
WITH DATA;

create table product.product_contract_templates
(
    product_detail_id bigint not null
        primary key,
    asc_templates     text,
    desc_templates    text
);

create table if not exists crm.email_communication_activity
(
    id                     bigint generated by default as identity
        constraint email_communication_activity_pk
            primary key,
    email_communication_id bigint                                             not null
        constraint email_communication_activity_email_communication_fk
            references crm.email_communications,
    activity_id            integer                                            not null
        constraint email_communication_activity_fk
            references activity.activity,
    create_date            timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id         text                                               not null,
    modify_date            timestamp with time zone,
    modify_system_user_id  text,
    status                 crm.email_communication_subobject_status           not null
);
create table if not exists invoice.interim_reversal_credit_debit_notes
(
    id                      bigint                                             not null
        constraint interim_reversal_credit_debit_note_pk
            primary key,
    interim_invoice_id      bigint
        constraint interim_invoice_fk
            references invoice.invoices,
    credit_debit_invoice_id bigint
        constraint credit_debit_invoice_id_fk
            references invoice.invoices,
    create_date             timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id          varchar(50)                                        not null,
    modify_date             timestamp with time zone,
    modify_system_user_id   varchar(50)
);
create sequence invoice.interim_reversal_credit_debit_note_id_seq;

create sequence customer.customers_cust_num_priv_seq increment by 1;
create sequence customer.customers_cust_num_legal_seq increment by 1;

create sequence service.service_preferences_id_seq;
create sequence product.product_preferences_id_seq;
create table if not exists product.product_preferences
(
    id                    bigint
        constraint product_preferences_pk
            primary key,
    product_detail_id     bigint                                             not null
        constraint product_preferences_product_detail_fk
            references product.product_details,
    status                product.product_subobject_status                   not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    preference_id         integer                                            not null
        constraint product_preferences_preferences_fk
            references nomenclature.preferences
);

create table if not exists service.service_preferences
(
    id                    bigint
        constraint service_preferences_pk
            primary key,
    service_detail_id     bigint                                             not null
        constraint service_preferences_service_detail_fk
            references service.service_details,
    status                service.service_subobject_status                   not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    preference_id         integer                                            not null
        constraint service_preferences_preferences_fk
            references nomenclature.preferences
);
create table if not exists product_contract.forbidden_numbers
(
    contract_number bigint not null
        primary key
);


CREATE FUNCTION product_contract.get_contract_number(
) RETURNS bigint
    STABLE
    PARALLEL SAFE
    LANGUAGE plpgsql
AS
'
    DECLARE
        v_num bigint;
        tries int := 0;
    BEGIN

        LOOP
            v_num := nextval(''product_contract.contract_number_seq'');

            -- If v_num is not forbidden, we''re done
            IF NOT EXISTS (SELECT 1
                           FROM product_contract.forbidden_numbers f
                           WHERE f.contract_number = v_num) THEN
                RETURN v_num;
            END IF;

            -- OPTIONAL: small safety to avoid infinite loop in case of bad data
            tries := tries + 1;
            IF tries > 100000 THEN
                RAISE EXCEPTION ''Unable to find allowed number after % attempts starting near %'',
                    tries, v_num;
            END IF;
        END LOOP;
    END;
';

create schema if not exists translation;

create type translation.translation_language as enum ('BULGARIAN', 'ENGLISH');

create table translation.translations
(
    id                    bigint generated by default as identity
        constraint translations_pk
            primary key,
    value                 varchar(512),
    translated_value      varchar(512),
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        varchar(50)                                        not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50),
    dest_language         translation.translation_language
);

create function translation.translate_text(input_text text, dest_language_param text) returns text
    language plpgsql
as
'
DECLARE
    result_text       TEXT;
    translation_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO translation_count
    FROM translation.translations
    WHERE text(dest_language) = dest_language_param;

    WITH RECURSIVE
        translations_ordered AS (
            SELECT value,
                   translated_value,
                   regexp_replace(value, ''([\\^$*+?().|\\[\\]{}])'', ''\\\\$1'', ''g'') AS escaped_value,
                   row_number() OVER (ORDER BY length(value) DESC) AS id
            FROM translation.translations
            WHERE text(dest_language) = dest_language_param
        ),
        translation_process AS (
            SELECT input_text AS current_text,
                   1 AS iteration

            UNION ALL

            SELECT regexp_replace(
                           tp.current_text,
                           ''(?i)'' || t.escaped_value,
                           t.translated_value,
                           ''g''
                   ) AS current_text,
                   tp.iteration + 1 AS iteration
            FROM translation_process tp
                     JOIN translations_ordered t ON t.id = tp.iteration
            WHERE tp.iteration <= translation_count
        )
    SELECT current_text
    INTO result_text
    FROM translation_process
    WHERE iteration = translation_count + 1
    LIMIT 1;

    RETURN result_text;
END;
';
create table if not exists product_contract.contract_signable_documents
(
    id                    bigint not null
        constraint contract_signable_documents_pk
            primary key,
    document_id           bigint
        constraint contract_signable_documents_document_id_fk
            references template.document,
    contract_detail_id    bigint
        constraint contract_signable_documents_contract_details_id_fk
            references product_contract.contract_details,
    create_date           timestamp with time zone,
    system_user_id        text,
    modify_date           timestamp with time zone,
    modify_system_user_id text,
    status                product_contract.contract_status,
    email_sent            boolean
);
create table if not exists product_contract.contract_resigned_contracts
(
    id                    bigint generated by default as identity
        constraint contract_resigned_contracts_pk
            primary key,
    contract_id           bigint                                             not null
        constraint contract_fk
            references product_contract.contracts,
    resigned_contract_id  bigint                                             not null
        constraint resigned_contracts_fk
            references product_contract.contracts,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        text                                               not null,
    modify_date           timestamp with time zone,
    modify_system_user_id text,
    constraint contract_resigned_contracts_check
        check (contract_id <> resigned_contract_id)
);
create type pod.pod_details_additional_params_status as enum ('ACTIVE', 'DELETED');

create table if not exists pod.pod_details_additional_params
(
    id                    bigint generated by default as identity
        constraint pod_details_additional_params_pk
            primary key,
    pod_detail_id         bigint                                             not null
        constraint pod_details_additional_params_product_detail_fk
            references pod.pod_details,
    status                pod.pod_details_additional_params_status           not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    system_user_id        text                                               not null,
    modify_date           timestamp with time zone,
    modify_system_user_id text,
    pod_additional_param  bigint                                             not null
        constraint pod_additional_params_fk
            references nomenclature.pod_additional_params
);

create type customer.manager_contact_type as enum ('MOBILE_NUMBER', 'EMAIL');

create table if not exists customer.manager_contacts
(
    id                    bigint generated by default as identity
        constraint manager_contacts_pk
            primary key,
    manager_id            bigint                                             not null,
    contact_type          customer.manager_contact_type                      not null,
    contact_value         varchar(256)                                       not null,
    status                customer.status_enum                               not null,
    system_user_id        varchar(50)                                        not null,
    create_date           timestamp with time zone default CURRENT_TIMESTAMP not null,
    modify_date           timestamp with time zone,
    modify_system_user_id varchar(50)
);

alter table product_contract.contract_pods
    add is_resigned boolean;

alter table product_contract.contract_pods
    add resigned_by_id bigint;
-- PHN-3011: per-channel, per-month contract serial counter (mirrors the production migration
-- phoenix-core/database/migrations/2026-06-08_PHN-3011_contract_serial_counter.sql).
create table if not exists product_contract.contract_serial_counter (
    channel      smallint    not null,
    year_month   varchar(4)  not null,
    last_serial  integer     not null,
    constraint contract_serial_counter_pk primary key (channel, year_month),
    constraint contract_serial_counter_channel_chk check (channel in (1, 2, 3))
);

create or replace function product_contract.allocate_contract_serial(p_channel integer, p_year_month text)
returns integer
language plpgsql
as $func$
declare
    v_serial integer;
begin
    insert into product_contract.contract_serial_counter (channel, year_month, last_serial)
    values (p_channel::smallint, p_year_month, 1)
    on conflict (channel, year_month)
    do update set last_serial = product_contract.contract_serial_counter.last_serial + 1
    returning last_serial into v_serial;
    return v_serial;
end;
$func$;

create or replace function product_contract.reserve_contract_serial(p_channel integer, p_year_month text, p_serial integer)
returns integer
language plpgsql
as $func$
declare
    v_serial integer;
begin
    insert into product_contract.contract_serial_counter (channel, year_month, last_serial)
    values (p_channel::smallint, p_year_month, p_serial)
    on conflict (channel, year_month)
    do update set last_serial = greatest(product_contract.contract_serial_counter.last_serial, p_serial)
    returning last_serial into v_serial;
    return v_serial;
end;
$func$;
