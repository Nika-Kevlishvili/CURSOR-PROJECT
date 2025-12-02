package bg.energo.phoenix.repository.receivable.reminder;

import bg.energo.phoenix.model.documentModels.reminder.ReminderDocumentModelMiddleResponse;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.reminder.Reminder;
import bg.energo.phoenix.model.response.billing.billingRun.OneTimeCreationModel;
import bg.energo.phoenix.model.response.receivable.reminder.ReminderListingMiddleResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    @Query(value = "select nextval('receivable.reminders_id_seq')", nativeQuery = true)
    Long getNextSequenceValue();

    @Query(nativeQuery = true,
            value = """
                     select  
                             r.id as id,
                             r.reminder_number as number,
                             r.trigger_for_liabilities as triggerForLiabilities,
                             r.customer_condition as conditionType,
                             text(r.communication_channel) as communicationChannel,
                             date(r.create_date) as creationDate,
                             r.status as status
                     from receivable.reminders r
                     where
                         ((:triggerForLiabilities) is null or text(r.trigger_for_liabilities) in (:triggerForLiabilities))
                     and ((:customerCondition) is null or text(r.customer_condition) in (:customerCondition))
                     and ((:statuses) is null or text(r.status) in :statuses)
                     and ((:communicationChannel) is null or cast(r.communication_channel as text[])  && cast(:communicationChannel as text[]))
                     and (:prompt is null or (:searchBy = 'ALL' and (lower(r.reminder_number) like :prompt))
                                          or ((:searchBy = 'REMINDER_NUMBER' and lower(r.reminder_number) like :prompt)))
                    """,
            countQuery = """
                     select count(1)
                     from receivable.reminders r
                     where
                         ((:triggerForLiabilities) is null or text(r.trigger_for_liabilities) in (:triggerForLiabilities))
                     and ((:customerCondition) is null or text(r.customer_condition) in (:customerCondition))
                     and ((:statuses) is null or text(r.status) in :statuses)
                     and ((:communicationChannel) is null or cast(r.communication_channel as text[])  && cast(:communicationChannel as text[]))
                     and (:prompt is null or (:searchBy = 'ALL' and (lower(r.reminder_number) like :prompt))
                                          or ((:searchBy = 'REMINDER_NUMBER' and lower(r.reminder_number) like :prompt)))
                    """
    )
    Page<ReminderListingMiddleResponse> filter(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("triggerForLiabilities") List<String> triggerForLiabilities,
            @Param("communicationChannel") String communicationChannel,
            @Param("customerCondition") List<String> customerCondition,
            @Param("statuses") List<String> statuses,
            Pageable pageable
    );

    Optional<Reminder> findByIdAndStatus(Long id, EntityStatus status);

    List<Reminder> findAllByStatus(EntityStatus status);

    @Query(nativeQuery = true,
            value = """
                            select 
                                 b.id AS id,
                                 b.reminder_number AS reminderNumber,
                                 text(b.trigger_for_liabilities) AS triggerForLiabilities,
                                 b.postponement_in_days AS postponementInDays,
                                 b.due_amount_from AS dueAmountFrom,
                                 b.due_amount_to AS dueAmountTo,
                                 b.currency_id AS currenyId,
                                 b.customer_condition AS customerCondition,
                                 b.customer_under_conditions AS customerUnderConditions,
                                 b.list_of_customers AS listOfCustomers,
                                 b.communication_channel AS communicationChannel,
                                 b.contact_purpose_id AS contactPurposeId,
                                 b.status AS status,
                                 b.create_date AS createDate,
                               pp.id as periodicityId,
                               pp.billing_process_start as processExecutionType,
                               pp.billing_process_start_date as processxecutionDate
                               from receivable.reminders b
                        join receivable.reminder_periodicity bpp on bpp.reminder_id = b.id
                        join billing.process_periodicity pp on pp.id=bpp.periodicity_id
                        where bpp.status='ACTIVE'
                          and (pp.type='PERIODICAL' and not exists (select 1 from billing.billings b2 where b2.periodicity_created_from_id=pp.id 
                                                                                                        and b2.periodic_billing_created_from=b.id
                                                                                                        and (b2.run_periodicity is null or b2.run_periodicity='STANDARD')
                                                                                                        and date(b2.create_date)=date(current_date))or
                               (pp.type='ONE_TIME' and not exists (select 1 from billing.billings b2 where b2.periodicity_created_from_id=pp.id 
                                                                                                       and b2.periodic_billing_created_from=b.id
                                                                                                       and (b2.run_periodicity is null or b2.run_periodicity='STANDARD'))))
                        and ( (pp.type = 'PERIODICAL' and
                    
                               (
                                    (pp.calendar_id is null and current_date = (
                            WITH closest_future_date AS (
                                select s from billing.check_process_periodicity(pp.id,current_date,current_date + interval '365 day') s
                            )
                                (select s from closest_future_date cfd)
                            ))
                                or 
                                   (pp.change_to is null and current_date=(
                                       WITH closest_future_date AS (
                                           select s from billing.check_process_periodicity(pp.id,current_date,current_date + interval '365 day') s
                                       )
                                       (select s from closest_future_date cfd)
                                       ) and current_date in (select date from billing.get_working_days(pp.calendar_id,date(current_date), date(current_date)) as date) )
                                       or
                    
                                   (pp.change_to = 'PREVIOUS_WORKING_DAY' and (current_date = (
                                       WITH closest_future_date AS (
                                           select s from billing.check_process_periodicity(pp.id,current_date,current_date + interval '365 day') s
                                       )
                                            SELECT ( date)
                                       FROM (
                                                -- Your select query here
                                                select date from billing.get_working_days(pp.id,date(current_date), date((select s from closest_future_date cfd))) as date
                                            ) AS subquery
                                       ORDER BY ABS(date((select s from closest_future_date cfd)) - date)
                                       LIMIT 1
                                       )) ) or
                                   (pp.change_to = 'NEXT_WORKING_DAY'
                                       and current_date= (
                                           WITH closest_future_date AS (
                                               select s from billing.check_process_periodicity_inverted(pp.id,current_date- interval '365 day',current_date ) s
                                           )
                                           SELECT ( date)
                                           FROM (
                                                    -- Your select query here
                                                    select date from billing.get_working_days(pp.id, date((select s from closest_future_date cfd)),date(current_date)) as date
                                                ) AS subquery
                                           ORDER BY ABS(date((select s from closest_future_date cfd)) - date)
                                           LIMIT 1
                                           )
                                       )
                    
                                   )) or (pp.type='ONE_TIME' and pp.billing_process_start='DATE_AND_TIME' and date(pp.billing_process_start_date)=date(current_date)))
                        order by createDate desc
                    """)
    List<OneTimeCreationModel> findAllPeriodicForOneTimeCreation();

    @Query(
            nativeQuery = true,
            value = """
                    select reminder.id as reminderId
                    from receivable.reminders reminder
                             join receivable.reminder_periodicity reminderPeriodicity on reminderPeriodicity.reminder_id = reminder.id
                             join billing.process_periodicity processPeriodicity
                                  on processPeriodicity.id = reminderPeriodicity.periodicity_id
                    where reminder.status = 'ACTIVE' and reminderPeriodicity.status = 'ACTIVE'
                      and ((processPeriodicity.type = 'PERIODICAL' and
                            ((processPeriodicity.calendar_id is null and current_date = (WITH closest_future_date AS (
                            select s from billing.check_process_periodicity(
                                                                processPeriodicity.id,
                                                                current_date,
                                                                current_date +
                                                                interval '365 day') s)
                             (select s from closest_future_date cfd))) or
                             (processPeriodicity.change_to is null and current_date = (WITH closest_future_date AS (
                             select s from billing.check_process_periodicity(
                                                                processPeriodicity.id,
                                                                current_date,
                                                                current_date +
                                                                interval '365 day') s)
                              (select s from closest_future_date cfd)) and
                              current_date in (select date
                                               from billing.get_working_days(processPeriodicity.calendar_id, date(current_date),
                                                                date(current_date)) as date)) or
                             (processPeriodicity.change_to = 'PREVIOUS_WORKING_DAY' and
                              (current_date = (WITH closest_future_date AS (
                            select s from billing.check_process_periodicity(processPeriodicity.id,
                              current_date,
                              current_date +
                              interval '365 day') s)
                                               select (date)
                                               from (select date
                                                     from billing.get_working_days(processPeriodicity.id,
                                                                 date(current_date),
                                                                 date((select s from closest_future_date cfd))) as date) AS subquery
                                               order by ABS(date((select s from closest_future_date cfd)) - date)
                                               limit 1))) or
                             (processPeriodicity.change_to = 'NEXT_WORKING_DAY' and current_date = (with closest_future_date as (
                             select s from billing.check_process_periodicity_inverted(
                                                  processPeriodicity.id,
                                                  current_date -
                                                  interval '365 day',
                                                  current_date) s)
                            select (date)
                            from (select date
                                  from billing.get_working_days(
                                               processPeriodicity.id,
                                               date((select s from closest_future_date cfd)),
                                               date(current_date)) as date) AS subquery
                            order by ABS(date((select s from closest_future_date cfd)) - date)
                            limit 1)
                                 )
                                )) or
                           (processPeriodicity.type = 'ONE_TIME' and processPeriodicity.billing_process_start = 'DATE_AND_TIME' and
                            date(processPeriodicity.billing_process_start_date) = date(current_date)))
                    order by reminderId desc
                    """
    )
    Set<Long> findAllReminderWitchShouldBeRun();

    @Query(value = """
               select array_agg(pc.contract_number)
                        from customer.customers c
                        join customer.customer_details cd on cd.id=c.last_customer_detail_id
                        join product_contract.contract_details pcd on cd.id = pcd.customer_detail_id
                        JOIN product_contract.contracts pc ON pcd.contract_id = pc.id
                        where
                            pc.status= 'ACTIVE'
                            and pcd.start_date <= current_date
                            and c.id=:customerId;
            """, nativeQuery = true)
    String findContractNumbers(Long customerId);

    @Query("""
                   select br.billingNumber
                   from BillingRun br
                   where br.id=:billingId
            """)
    String findBillingNumberById(Long billingId);

    @Query("""
                   select br.groupNumber
                   from ContractBillingGroup br
                   where br.id=:billingId
            """)
    String findBillingNumberForLpfById(Long billingId);

    @Query(nativeQuery = true,
            value = """
                    SELECT DISTINCT CONCAT(
                                            cd.name,
                                            CASE
                                                WHEN text(c.customer_type) = 'PRIVATE_CUSTOMER'
                                                    THEN CONCAT(' ', cd.middle_name, ' ', cd.last_name)
                                                WHEN text(c.customer_type) = 'LEGAL_ENTITY' THEN CONCAT(' ', lf.name)
                                                ELSE ''
                                                END
                                    )                             AS CustomerNameComb,
                                    CONCAT(
                                            cd.name_transl,
                                            CASE
                                                WHEN text(c.customer_type) = 'PRIVATE_CUSTOMER'
                                                    THEN CONCAT(' ', cd.middle_name_transl, ' ', cd.last_name_transl)
                                                WHEN text(c.customer_type) = 'LEGAL_ENTITY' THEN CONCAT(' ', lf.name)
                                                ELSE ''
                                                END
                                    )                             AS CustomerNameCombTrsl,
                                    c.identifier                  as CustomerIdentifier,
                                    c.customer_number             as CustomerNumber,
                                    translation.translate_text(CONCAT(
                                                                       CASE
                                                                           WHEN cd.foreign_address
                                                                               THEN CONCAT(
                                                                                   cd.district_foreign,
                                                                                   CASE
                                                                                       WHEN cd.residential_area_foreign IS NULL THEN ''
                                                                                       ELSE CONCAT(', ', cd.foreign_residential_area_type, ', ', cd.residential_area_foreign)
                                                                                       END,
                                                                                   CASE
                                                                                       WHEN cd.street_foreign IS NULL THEN ''
                                                                                       ELSE CONCAT(', ', cd.foreign_street_type, ', ', cd.street_foreign)
                                                                                       END
                                                                                    )
                                                                           ELSE CONCAT(
                                                                                   dist.name,
                                                                                   CASE
                                                                                       WHEN resarea.name IS NULL THEN ''
                                                                                       ELSE CONCAT(', ', resarea.type, ', ', resarea.name)
                                                                                       END,
                                                                                   CASE
                                                                                       WHEN strt.name IS NULL THEN ''
                                                                                       ELSE CONCAT(', ', strt.type, ', ', strt.name)
                                                                                       END
                                                                                )
                                                                           END, ', ',
                                                                       cd.street_number, ', ',
                                                                       CASE WHEN cd.block IS NOT NULL THEN CONCAT('бл. ', cd.block, ', ') ELSE '' END,
                                                                       CASE WHEN cd.entrance IS NOT NULL THEN CONCAT('вх. ', cd.entrance, ', ') ELSE '' END,
                                                                       CASE WHEN cd.floor IS NOT NULL THEN CONCAT('ет. ', cd.floor, ', ') ELSE '' END,
                                                                       CASE WHEN cd.apartment IS NOT NULL THEN CONCAT('ап. ', cd.apartment, ', ') ELSE '' END,
                                                                       cd.address_additional_info
                                                               ) ,text('BULGARIAN'))    AS HeadquarterAddressComb,
                                    CASE
                                        WHEN cd.foreign_address THEN cd.populated_place_foreign
                                        ELSE popp.name
                                        END                       AS HeadquarterPopulatedPlace,
                                    CASE
                                        WHEN cd.foreign_address THEN cd.zip_code_foreign
                                        ELSE zipc.zip_code
                                        END                       AS HeadquarterZip,
                                    CASE
                                        WHEN cd.foreign_address THEN cd.district_foreign
                                        ELSE dist.name
                                        END                       AS HeadquarterDistrict,
                                    translation.translate_text(CASE
                                                                   WHEN cd.foreign_address THEN text(cd.foreign_residential_area_type)
                                                                   ELSE text(resarea.type)
                                                                   END ,text('BULGARIAN'))                      AS HeadquarterQuarterRaType,
                                    CASE
                                        WHEN cd.foreign_address THEN cd.residential_area_foreign
                                        ELSE resarea.name
                                        END                       AS HeadquarterQuarterRaName,
                                    translation.translate_text(CASE
                                                                   WHEN cd.foreign_address THEN text(cd.foreign_street_type)
                                                                   ELSE text(strt.type)
                                                                   END ,TEXT('BULGARIAN'))                   AS HeadquarterStrBlvdType,
                                    CASE
                                        WHEN cd.foreign_address THEN cd.street_foreign
                                        ELSE strt.name
                                        END                       AS HeadquarterStrBlvdName,
                                    cd.street_number              AS HeadquarterStrBlvdNumber,
                                    cd.block                      AS HeadquarterBlock,
                                    cd.entrance                   AS HeadquarterEntrance,
                                    cd.floor                      AS HeadquarterFloor,
                                    cd.apartment                  AS HeadquarterApartment,
                                    cd.address_additional_info    AS HeadquarterAdditionalInfo,
                                    ARRAY_AGG(DISTINCT segm.name) AS CustomerSegments,
                                    translation.translate_text( CONCAT(
                                                                        CASE
                                                                            WHEN cc.foreign_address
                                                                                THEN CONCAT(
                                                                                    cc.district_foreign,
                                                                                    CASE
                                                                                        WHEN cc.residential_area_foreign IS NULL THEN ''
                                                                                        ELSE CONCAT(', ', cc.foreign_residential_area_type, ', ', cc.residential_area_foreign)
                                                                                        END,
                                                                                    CASE
                                                                                        WHEN cc.street_foreign IS NULL THEN ''
                                                                                        ELSE CONCAT(', ', cc.foreign_street_type, ', ', cc.street_foreign)
                                                                                        END
                                                                                     )
                                                                            ELSE CONCAT(
                                                                                    cc.contact_type_name,
                                                                                    CASE
                                                                                        WHEN resarea2.name IS NULL THEN ''
                                                                                        ELSE CONCAT(', ', resarea2.type, ', ', resarea2.name)
                                                                                        END,
                                                                                    CASE
                                                                                        WHEN strt2.name IS NULL THEN ''
                                                                                        ELSE CONCAT(', ', strt2.type, ', ', strt2.name)
                                                                                        END
                                                                                 )
                                                                            END, ', ',
                                                                        cc.street_number, ', ',
                                                                        CASE WHEN cc.block IS NOT NULL THEN CONCAT('бл. ', cc.block, ', ') ELSE '' END,
                                                                        CASE WHEN cc.entrance IS NOT NULL THEN CONCAT('вх. ', cc.entrance, ', ') ELSE '' END,
                                                                        CASE WHEN cc.floor IS NOT NULL THEN CONCAT('ет. ', cc.floor, ', ') ELSE '' END,
                                                                        CASE WHEN cc.apartment IS NOT NULL THEN CONCAT('ап. ', cc.apartment, ', ') ELSE '' END,
                                                                        cc.address_additional_info
                                                                ) ,TEXT('BULGARIAN'))                            AS CommunicationAddressComb,
                                    CASE
                                        WHEN cc.foreign_address THEN cc.populated_place_foreign
                                        ELSE popp2.name
                                        END                       AS CommunicationPopulatedPlace,
                                    CASE
                                        WHEN cc.foreign_address THEN cc.zip_code_foreign
                                        ELSE zipc2.zip_code
                                        END                       AS CommunicationZip,
                                    CASE
                                        WHEN cc.foreign_address THEN cc.district_foreign
                                        ELSE dist2.name
                                        END                       AS CommunicationDistrict,
                                    translation.translate_text( CASE
                                                                    WHEN cc.foreign_address THEN text(cc.foreign_residential_area_type)
                                                                    ELSE text(resarea2.type)
                                                                    END   ,text('BULGARIAN'))                    AS CommunicationQuarterRaType,
                                    CASE
                                        WHEN cc.foreign_address THEN cc.residential_area_foreign
                                        ELSE resarea2.name
                                        END                       AS CommunicationQuarterRaName,
                                    translation.translate_text(CASE
                                                                   WHEN cc.foreign_address THEN text(cc.foreign_street_type)
                                                                   ELSE text(strt2.type)
                                                                   END ,TEXT('BULGARIAN'))                      AS CommunicationStrBlvdType,
                                    CASE
                                        WHEN cc.foreign_address THEN cc.street_foreign
                                        ELSE strt2.name
                                        END                       AS CommunicationStrBlvdName,
                                    cc.street_number              AS CommunicationStrBlvdNumber,
                                    cc.block                      AS CommunicationBlock,
                                    cc.entrance                   AS CommunicationEntrance,
                                    cc.floor                      AS CommunicationFloor,
                                    cc.apartment                  AS CommunicationApartment,
                                    cc.address_additional_info    AS CommunicationAdditionalInfo,
                                    curr.print_name               as Currency
                    FROM customer.customer_details cd
                             join customer.customers c on cd.customer_id = c.id
                             LEFT JOIN nomenclature.districts dist ON cd.district_id = dist.id
                             LEFT JOIN nomenclature.residential_areas resarea ON cd.residential_area_id = resarea.id
                             LEFT JOIN nomenclature.streets strt ON cd.street_id = strt.id
                             LEFT JOIN nomenclature.legal_forms lf ON cd.legal_form_id = lf.id
                             LEFT JOIN nomenclature.populated_places popp ON cd.populated_place_id = popp.id
                             LEFT JOIN nomenclature.zip_codes zipc ON cd.zip_code_id = zipc.id
                             LEFT JOIN customer.customer_segments cussegm ON cd.id = cussegm.customer_detail_id
                             LEFT JOIN nomenclature.segments segm ON cussegm.segment_id = segm.id
                             LEFT JOIN customer.customer_communications cc ON cc.id = :customerCommunicationId
                             LEFT JOIN nomenclature.residential_areas resarea2
                                       ON cc.residential_area_id = resarea2.id and resarea2.status = 'ACTIVE'
                             LEFT JOIN nomenclature.streets strt2 ON cc.street_id = strt2.id
                             LEFT JOIN nomenclature.populated_places popp2 ON cc.populated_place_id = popp2.id
                             LEFT JOIN nomenclature.zip_codes zipc2 ON cc.zip_code_id = zipc2.id
                             LEFT JOIN nomenclature.districts dist2 ON cc.district_id = dist2.id
                             left join nomenclature.currencies curr on curr.is_default = true
                    WHERE cd.id = :customerDetailId
                    GROUP BY CustomerNameComb, CustomerNameCombTrsl, CustomerIdentifier, CustomerNumber,
                             HeadquarterAddressComb, HeadquarterStrBlvdNumber, HeadquarterBlock, HeadquarterEntrance, HeadquarterFloor,
                             HeadquarterApartment, HeadquarterAdditionalInfo, CommunicationAddressComb, CommunicationStrBlvdNumber,
                             CommunicationBlock, HeadquarterPopulatedPlace, HeadquarterZip, HeadquarterDistrict, HeadquarterQuarterRaType,
                             CommunicationEntrance, CommunicationFloor, CommunicationApartment, CommunicationAdditionalInfo,
                             HeadquarterQuarterRaName, HeadquarterStrBlvdType, HeadquarterStrBlvdName, CommunicationPopulatedPlace,
                             CommunicationZip, CommunicationDistrict, CommunicationQuarterRaType, CommunicationQuarterRaName,
                             CommunicationStrBlvdType, CommunicationStrBlvdName, Currency
                    """)
    ReminderDocumentModelMiddleResponse getReminderJsonInfoByCustomerDetailId(Long customerDetailId, Long customerCommunicationId);

}
