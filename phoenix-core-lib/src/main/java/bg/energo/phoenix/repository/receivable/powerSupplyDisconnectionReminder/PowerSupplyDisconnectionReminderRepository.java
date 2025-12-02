package bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder;


import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminder;
import bg.energo.phoenix.model.enums.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderStatus;
import bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PowerSupplyDisconnectionReminderRepository extends JpaRepository<PowerSupplyDisconnectionReminder, Long> {

    @Query(nativeQuery = true,
            value = """
              SELECT DISTINCT r.id              as id,
                r.reminder_number as reminderNumber
                FROM receivable.power_supply_disconnection_reminders r
                WHERE (:prompt IS NULL OR lower(r.reminder_number) LIKE :prompt)
                  AND r.status = 'ACTIVE'
                  AND r.reminder_status = 'EXECUTED'
                  AND (
                    EXISTS (SELECT 1
                            FROM receivable.power_supply_disconnection_requests dr
                            WHERE r.id = dr.power_supply_disconnection_reminder_id
                              AND dr.disconnection_request_status = 'EXECUTED')
                        OR NOT EXISTS (SELECT 1
                                       FROM receivable.power_supply_disconnection_requests dr
                                       WHERE r.id = dr.power_supply_disconnection_reminder_id
                                         AND dr.disconnection_request_status != 'DRAFT')
                        OR EXISTS (SELECT 1
                                   FROM receivable.power_supply_disconnection_reminder_customers psdrc
                                            JOIN receivable.customer_liabilities cl ON psdrc.customer_liability_id = cl.id
                                   WHERE psdrc.power_supply_disconnection_reminder_id = r.id
                                     AND cl.status = 'ACTIVE'
                                     AND cl.due_date <= current_date
                                     AND cl.current_amount > 0
                                     AND NOT EXISTS (SELECT 1
                                                     FROM receivable.power_supply_disconnection_requests dr
                                                              JOIN receivable.power_supply_disconnection_request_pods dp
                                                                   ON dr.id = dp.power_supply_disconnection_request_id
                                                     WHERE dp.pod_id = cl.id
                                                       AND dr.disconnection_request_status = 'EXECUTED'))
                    )
                ORDER BY r.id DESC
""", countQuery = """
                SELECT COUNT(DISTINCT r.id) AS total_count
                FROM receivable.power_supply_disconnection_reminders r
                WHERE (:prompt IS NULL OR lower(r.reminder_number) LIKE :prompt)
                  AND r.status = 'ACTIVE'
                  AND r.reminder_status = 'EXECUTED'
                  AND (
                    EXISTS (SELECT 1
                            FROM receivable.power_supply_disconnection_requests dr
                            WHERE r.id = dr.power_supply_disconnection_reminder_id
                              AND dr.disconnection_request_status = 'EXECUTED')
                        OR NOT EXISTS (SELECT 1
                                       FROM receivable.power_supply_disconnection_requests dr
                                       WHERE r.id = dr.power_supply_disconnection_reminder_id
                                         AND dr.disconnection_request_status != 'DRAFT')
                        OR EXISTS (SELECT 1
                                   FROM receivable.power_supply_disconnection_reminder_customers psdrc
                                            JOIN receivable.customer_liabilities cl ON psdrc.customer_liability_id = cl.id
                                   WHERE psdrc.power_supply_disconnection_reminder_id = r.id
                                     AND cl.status = 'ACTIVE'
                                     AND cl.due_date <= current_date
                                     AND cl.current_amount > 0
                                     AND NOT EXISTS (SELECT 1
                                                     FROM receivable.power_supply_disconnection_requests dr
                                                              JOIN receivable.power_supply_disconnection_request_pods dp
                                                                   ON dr.id = dp.power_supply_disconnection_request_id
                                                     WHERE dp.pod_id = cl.id
                                                       AND dr.disconnection_request_status = 'EXECUTED'))
                    )
""")
    Page<RemindersForDPSRequestMiddleResponse> getRemindersForDPSRequest(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query("""
                    select psdr from PowerSupplyDisconnectionReminder psdr
                    where psdr.id=:id
                    and psdr.status in :generalStatuses
                    and psdr.reminderStatus in :reminderStatuses
            """)
    Optional<PowerSupplyDisconnectionReminder> findByIdAndGeneralStatusesAndReminderStatuses(Long id, List<EntityStatus> generalStatuses, List<PowerSupplyDisconnectionReminderStatus> reminderStatuses);

    @Query("""
                    select psdr from PowerSupplyDisconnectionReminder psdr
                    where psdr.id=:id
                    and psdr.status in :generalStatuses
            """)
    Optional<PowerSupplyDisconnectionReminder> findByIdAndGeneralStatuses(Long id, List<EntityStatus> generalStatuses);

    @Query(nativeQuery = true,
            value =
                    """
                    SELECT
                        psdr.reminder_number as reminderNumber,
                        psdr.create_date as createDate,
                        psdr.customer_send_date as customerSendDate,
                        text(psdr.reminder_status) as reminderStatus,
                        cc.number_of_customers AS numberOfCustomers,
                        psdr.disconnection_date as disconnectionDate,
                        text(psdr.status) as status,
                        psdr.id as id
                    FROM receivable.power_supply_disconnection_reminders psdr
                    LEFT JOIN LATERAL ((SELECT count(DISTINCT rmc.customer_id) as number_of_customers FROM receivable.power_supply_disconnection_reminder_customers rmc WHERE rmc.power_supply_disconnection_reminder_id = psdr.id) ) AS cc ON 1=1
                    WHERE
                        ((:statuses) IS NULL OR text(psdr.status) IN (:statuses))
                        AND ((:reminderStatuses) IS NULL OR text(psdr.reminder_status) IN (:reminderStatuses))
                        AND (cast(:createDateFrom as date) IS NULL OR psdr.create_date >= cast(:createDateFrom as date))
                        AND (cast(:createDateTo as date) IS NULL OR psdr.create_date <= cast(:createDateTo as date))
                        AND (cast(:customerSendDateFrom as timestamp) IS NULL OR psdr.customer_send_date >= cast(:customerSendDateFrom as timestamp))
                        AND (cast(:customerSendDateTo as timestamp) IS NULL OR psdr.customer_send_date <=  cast(:customerSendDateTo as timestamp))
                            AND (
                        :prompt IS NULL
                        OR (:searchBy = 'ALL' AND (LOWER(psdr.reminder_number) LIKE :prompt OR EXISTS (
                            SELECT 1 FROM receivable.power_supply_disconnection_reminder_customers psdrc
                            JOIN customer.customers c ON psdrc.customer_id = c.id
                            WHERE psdrc.power_supply_disconnection_reminder_id = psdr.id AND LOWER(c.identifier) LIKE :prompt
                        )))
                        OR (:searchBy = 'REMINDER_NUMBER' AND LOWER(psdr.reminder_number) LIKE :prompt)
                        OR (:searchBy = 'CUSTOMER_IDENTIFIER' AND EXISTS (
                            SELECT 1 FROM receivable.power_supply_disconnection_reminder_customers psdrc
                            JOIN customer.customers c ON psdrc.customer_id = c.id
                            WHERE psdrc.power_supply_disconnection_reminder_id = psdr.id AND LOWER(c.identifier) LIKE :prompt
                        ))
                    )
                    AND (COALESCE(:numberOfCustomersFrom, 0) = 0 OR cc.number_of_customers >= :numberOfCustomersFrom)
                    AND (COALESCE(:numberOfCustomersTo, 0) = 0 OR cc.number_of_customers <= :numberOfCustomersTo)
            """)
    Page<PowerSupplyDisconnectionReminderListingResponse> filter(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("numberOfCustomersFrom") Integer numberOfCustomersFrom,
            @Param("numberOfCustomersTo") Integer numberOfCustomersTo,
            @Param("createDateFrom") LocalDate createDateFrom,
            @Param("createDateTo") LocalDate createDateTo,
            @Param("customerSendDateFrom") LocalDateTime customerSendDateFrom,
            @Param("customerSendDateTo") LocalDateTime customerSendDateTo,
            @Param("reminderStatuses") List<String> reminderStatuses,
            @Param("statuses") List<String> statuses,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value =
                    """
                            select distinct
                             case when c.customer_type = 'PRIVATE_CUSTOMER'
                                  then concat(c.identifier,concat(' (',cd.name),case when cd.middle_name is not null then cd.middle_name  end,case when cd.last_name is not null then cd.last_name  end,')' )
                                  when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,')')
                                  end customer,
                            concat(sum(psdrc.liability_amount) over (partition BY psdrc.customer_id order by psdrc.customer_id),' ',case when psdr.currency_id is not null then c2.name else (select name from nomenclature.currencies cc where cc.is_default = true) end) as sumOfLiabilities,
                            string_agg(cl.liability_number,',') over (partition BY cl.customer_id order by cl.customer_id desc rows between unbounded preceding and unbounded following ) as liabilities,
                            psdr.id
                            from
                            receivable.power_supply_disconnection_reminders psdr
                            join
                            receivable.power_supply_disconnection_reminder_customers psdrc
                            on psdrc.power_supply_disconnection_reminder_id = psdr.id
                            join
                            receivable.customer_liabilities cl
                            on psdrc.customer_liability_id = cl.id
                            join
                            customer.customers c
                            on psdrc.customer_id = c.id
                            join
                            customer.customer_details cd
                            on c.last_customer_detail_id = cd.id
                            left join
                            nomenclature.currencies c2
                            on psdr.currency_id = c2.id
                            where
                            psdrc.power_supply_disconnection_reminder_id = :powerSupplyDisconnectionReminderId
                            and
                             (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                                                          or
                                                                         text(c.customer_number) like :prompt
                                                                          or
                                                                         lower(cl.liability_number) like :prompt
                                                                          or
                                                                         lower(cl.outgoing_document_from_external_system) like :prompt
                                                            )
                                                        )
                                                        or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                                             or
                                                            (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                                             or
                                                            (:searchBy = 'LIABILITY_NUMBER' and lower(cl.liability_number) like :prompt)
                                                             or
                                                            (:searchBy = 'OUTGOING_DOCUMENT_NUMBER' and lower(cl.outgoing_document_from_external_system) like :prompt)
                                                        )
                                                    )
                            
                            """,
            countQuery = """
                    select count(distinct c.id)
                             from receivable.power_supply_disconnection_reminders psdr
                             join receivable.power_supply_disconnection_reminder_customers psdrc
                                 on psdrc.power_supply_disconnection_reminder_id = psdr.id
                             join receivable.customer_liabilities cl
                                 on psdrc.customer_liability_id = cl.id
                             join customer.customers c
                                 on psdrc.customer_id = c.id
                             join customer.customer_details cd
                                 on c.last_customer_detail_id = cd.id
                             left join nomenclature.currencies c2
                                 on psdr.currency_id = c2.id
                             where psdrc.power_supply_disconnection_reminder_id = :powerSupplyDisconnectionReminderId
                             and (:prompt is null or
                                 (:searchBy = 'ALL' and (
                                     lower(c.identifier) like :prompt or
                                     text(c.customer_number) like :prompt or
                                     lower(cl.liability_number) like :prompt or
                                     lower(cl.outgoing_document_from_external_system) like :prompt
                                 )) or (
                                     (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt) or
                                     (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt) or
                                     (:searchBy = 'LIABILITY_NUMBER' and lower(cl.liability_number) like :prompt) or
                                     (:searchBy = 'OUTGOING_DOCUMENT_NUMBER' and lower(cl.outgoing_document_from_external_system) like :prompt)
                                 )
                             )
                    """)
    Page<PowerSupplyDisconnectionReminderSecondTabResponse> secondTabFilter(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("powerSupplyDisconnectionReminderId") Long powerSupplyDisconnectionReminderId,
            Pageable pageable
    );

    @Query(value = """
        select psdr from PowerSupplyDisconnectionReminder psdr
        where psdr.customerSendDate>:customerSendTimeFrom
        and psdr.customerSendDate<=:customerSendTime
        and psdr.reminderStatus=:status
        and psdr.status='ACTIVE'
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PowerSupplyDisconnectionReminder> findByStatusAndSendTimePessimisticLock(PowerSupplyDisconnectionReminderStatus status, LocalDateTime customerSendTime, LocalDateTime customerSendTimeFrom);

    @Query(value = """
         WITH blocking_checks AS (
             SELECT
                 mofb.id AS mass_operation_id,
                 cl.customer_id,
                 cl.id AS customer_liability_id
             FROM receivable.mass_operation_for_blocking mofb
                      LEFT JOIN receivable.vw_mass_operation_for_blocking_excl_customers mofbec
                                ON mofbec.mass_operation_for_blocking_id = mofb.id
                      JOIN receivable.customer_liabilities cl ON cl.customer_id = mofbec.customer_id
                      LEFT JOIN invoice.invoices i ON i.id = cl.invoice_id
                      LEFT JOIN nomenclature.currencies cr1 ON cr1.id = cl.currency_id -- Join cr1 explicitly here
                      LEFT JOIN receivable.power_supply_disconnection_reminders psdr ON psdr.id in :powerSupplyDisconnectionReminderIds
             WHERE mofb.mass_operation_blocking_status = 'EXECUTED'
               AND mofb.status = 'ACTIVE'
               AND mofb.type && '{CUSTOMER_LIABILITY}'
               AND (
                 (i.invoice_number IS NOT NULL
                     AND i.invoice_number NOT LIKE ALL (
                         SELECT (pref.name || '-%')
                         FROM receivable.mass_operation_for_blocking_exclution_prefixes mofbp
                                  JOIN nomenclature.prefixes pref ON mofbp.prefix_id = pref.id
                         WHERE mofbp.mass_operation_for_blocking_id = mofb.id
                           AND mofbp.status = 'ACTIVE')
                     )
                     OR i.invoice_number IS NULL
                 )
               AND (
                 mofb.currency_id IS NULL
                     OR (cl.currency_id = mofb.currency_id
                     AND (mofb.exclusion_by_amount_less_than IS NULL OR cl.current_amount >= mofb.exclusion_by_amount_less_than)
                     AND (mofb.exclusion_by_amount_greater_than IS NULL OR cl.current_amount <= mofb.exclusion_by_amount_greater_than))
                     OR (cl.currency_id != mofb.currency_id
                     AND (mofb.exclusion_by_amount_less_than IS NULL OR cl.current_amount * cr1.alt_ccy_exchange_rate >= mofb.exclusion_by_amount_less_than)
                     AND (mofb.exclusion_by_amount_greater_than IS NULL OR cl.current_amount * cr1.alt_ccy_exchange_rate <= mofb.exclusion_by_amount_greater_than))
                 )
               AND (
                 (COALESCE(mofb.blocked_for_reminder_letters, false) = TRUE
                     AND date(psdr.customer_send_date) BETWEEN mofb.blocked_for_reminder_letters_from_date
                      AND COALESCE(mofb.blocked_for_reminder_letters_to_date, CURRENT_DATE))
                     OR (COALESCE(mofb.blocked_for_supply_termination, false) = TRUE
                     AND date(psdr.customer_send_date) BETWEEN mofb.blocked_for_supply_termination_from_date
                             AND COALESCE(mofb.blocked_for_supply_termination_to_date, CURRENT_DATE))
                 )
               AND CASE
                       WHEN mofb.customer_condition_type = 'LIST_OF_CUSTOMERS' THEN cl.customer_id = mofbec.customer_id
                       WHEN mofb.customer_condition_type = 'ALL_CUSTOMERS' THEN TRUE
                       WHEN mofb.customer_condition_type = 'CUSTOMERS_UNDER_CONDITIONS' THEN cl.id IN (SELECT receivable.liability_condition_eval(mofb.customer_conditions))
                 end group by mofb.id,
                              cl.customer_id,
                              cl.id
         ),
              liability_calculations AS (
                  SELECT
                      cl.customer_id,
                      cl.current_amount,
                      cl.id AS customer_liability_id,
                      psdr.id AS psdrId,
                      psdr.liability_amount_from AS amountfrom,
                      psdr.liability_amount_to AS amountto,
                      cr1.id AS currencyId,
                      cr1.alt_currency_id AS alternativeCurrencyId,
                      cr1.alt_ccy_exchange_rate AS alternativeExchangeRate,
                      cr2.id AS psdrCurrencyId,
                      cr2.alt_ccy_exchange_rate AS psdrCurrencyAlternativeExchangeRate,
                      date(psdr.customer_send_date) AS customerSendDate,
                      i.invoice_number AS invoiceNumber,
                      CASE
                          WHEN psdr.currency_id IS NOT NULL THEN
                              CASE
                                  WHEN cl.currency_id = psdr.currency_id THEN cl.current_amount
                                  ELSE cl.current_amount * cr1.alt_ccy_exchange_rate
                                  END
                          ELSE
                              CASE
                                  WHEN cr1.id = (SELECT id FROM nomenclature.currencies cr3 WHERE cr3.is_default = TRUE) THEN cl.current_amount
                                  ELSE cl.current_amount * cr1.alt_ccy_exchange_rate
                                  END
                          END AS calculated_amount,
                      SUM(CASE
                              WHEN psdr.currency_id IS NOT NULL THEN
                                  CASE
                                      WHEN cl.currency_id = psdr.currency_id THEN cl.current_amount
                                      ELSE cl.current_amount * cr1.alt_ccy_exchange_rate
                                      END
                              ELSE
                                  CASE
                                      WHEN cr1.id = (SELECT id FROM nomenclature.currencies cr3 WHERE cr3.is_default = TRUE) THEN cl.current_amount
                                      ELSE cl.current_amount * cr1.alt_ccy_exchange_rate
                                      END
                          END) OVER (PARTITION BY cl.customer_id, psdr.id ORDER BY cl.customer_id) AS sumCurrentAmount,
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
                  FROM
                      receivable.customer_liabilities cl
                          LEFT JOIN
                      invoice.invoices i ON i.id = cl.invoice_id
                          JOIN
                      nomenclature.currencies cr1 ON cr1.id = cl.currency_id -- Make sure to join cr1 here
                          LEFT JOIN
                      receivable.power_supply_disconnection_reminders psdr ON psdr.id in :powerSupplyDisconnectionReminderIds
                          LEFT JOIN
                      nomenclature.currencies cr2 ON cr2.id = psdr.currency_id
                  WHERE
                      cl.status = 'ACTIVE'
                    AND cl.due_date <= current_date
                    AND cl.due_date <= psdr.liabilities_max_due_date
                    AND cl.current_amount > 0
                    AND cl.contract_billing_group_id IS NOT NULL
                    AND CASE
                            WHEN COALESCE(cl.blocked_for_reminder_letters, false) = TRUE THEN
                                date(psdr.customer_send_date) NOT BETWEEN cl.blocked_for_reminder_letters_from_date AND COALESCE(blocked_for_reminder_letters_to_date, current_date)
                            ELSE
                                TRUE
                      END
                    AND CASE
                            WHEN COALESCE(cl.blocked_for_supply_termination, false) = TRUE THEN
                                date(psdr.customer_send_date) NOT BETWEEN cl.blocked_for_supply_termination_from_date AND COALESCE(blocked_for_supply_termination_to_date, current_date)
                            ELSE
                                TRUE
                      END
                    AND NOT EXISTS (
                      SELECT 1
                      FROM receivable.vw_power_supply_dcn_reminder_excl_customers psdrex
                      WHERE psdrex.customer_id = cl.customer_id
                        AND psdr.id = psdrex.reminder_id
                  )
              ),
              filtered_liabilities AS (
                  SELECT
                      lc.*
                  FROM liability_calculations lc
                  WHERE
                      (lc.amountfrom IS NULL OR lc.sumCurrentAmount >= lc.amountfrom)
                    AND (lc.amountto IS NULL OR lc.sumCurrentAmount <= lc.amountto)
              )
         SELECT
             fl.customer_id AS customerId,
             fl.customer_liability_id AS liabilityId,
             fl.calculated_amount AS currentAmount,
             fl.psdrId,
             fl.currencyId,
             fl.alternativeCurrencyId,
             fl.alternativeExchangeRate,
             fl.psdrCurrencyId,
             fl.amountFrom,
             fl.amountTo,
             fl.customerSendDate,
             fl.sumCurrentAmount,
             fl.invoiceNumber,
             fl.currentCurrencyId
         FROM filtered_liabilities fl
                  left JOIN blocking_checks bc ON fl.customer_liability_id = bc.customer_liability_id
         WHERE bc.mass_operation_id IS NULL;
""",nativeQuery = true)
    List<PowerSupplyDisconnectionReminderExecutionResponse> execute(List<Long> powerSupplyDisconnectionReminderIds);


    @Query(value = """
      with rl as (
          select coalesce(i.customer_communication_id,i.contract_communication_id) as inv_cust_communication_id,
                 psdrc.customer_id,
                 cl.id as liability_id,
                 cl.create_date,
                 cl.contract_billing_group_id,
                 row_number() over (partition by psdrc.customer_id order by cl.create_date desc
                     ) as rn
          from
              receivable.power_supply_disconnection_reminders psdr
                  join receivable.power_supply_disconnection_reminder_customers psdrc
                       on psdrc.power_supply_disconnection_reminder_id = psdr.id
                           and psdrc.power_supply_disconnection_reminder_id = :reminderForDcnId
                  join receivable.customer_liabilities cl
                       on psdrc.customer_liability_id = cl.id
                           and cl.status = 'ACTIVE'
                  left join invoice.invoices i on cl.invoice_id = i.id
      )
      select
          case
              when c.customer_type = 'PRIVATE_CUSTOMER'
                  then concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
              else concat(cd.name, ' ', lf.name) end                                as CustomerNameComb,
          case
              when c.customer_type = 'PRIVATE_CUSTOMER'
                  then concat(cd.name_transl, ' ', cd.middle_name_transl, ' ',
                              cd.last_name_transl)
              else concat(cd.name_transl, ' ', lft.name) end                         as CustomerNameCombTrsl,
          cc.id as customerCommunicationId,
          c.id as customerId,
          cd.id as customerDetailId,
          c.identifier                                                              as CustomerIdentifier,
          c.customer_number                                                         as CustomerNumber,
          case
              when cd.foreign_address is true then cd.populated_place_foreign
              else pp.name end                                                      as HeadquarterPopulatedPlace,
          case
              when cd.foreign_address is true then cd.zip_code_foreign
              else zc.zip_code end                                                  as HeadquarterZip,
          case
              when cd.foreign_address is true then cd.district_foreign
              else d.name end                                                       as HeadquarterDistrict,
          case
              when cd.foreign_address is true then cd.zip_code_foreign
              else zc2.zip_code end                                                  as CommunicationZip,
          case
              when cc.foreign_address is true then cc.district_foreign
              else d2.name end                                                      as CommunicationDistrict,
          case
              when cc.foreign_address is true then cc.residential_area_type
              else ra2.type end                                                     as CommunicationQuarterRaType,
          case
              when cc.foreign_address is true then cc.residential_area_foreign
              else ra2.name end                                                     as CommunicationQuarterRaName,
          case when cc.foreign_address is true then cc.street_type else s2.type end as CommunicationStrBlvdType,
          case
              when cc.foreign_address is true then cc.street_foreign
              else s2.name end                                                      as CommunicationStrBlvdName,
          cc.street_number                                                          as CommunicationStrBlvdNumber,
          case when cc.foreign_address is true then cc.block else cc.block end      as CommunicationBlock,
          cc.entrance                                                               as CommunicationEntrance,
          cc.floor                                                                  as CommunicationFloor,
          cc.apartment                                                              as CommunicationApartment,
          cc.address_additional_info                                                as CommunicationAdditionalInfo,
          case
              when cc.foreign_address is true then cc.populated_place_foreign
              else pp2.name end                                                      as CommunicationPopulatedPlace,
          case
              when cd.foreign_address is true then cd.residential_area_type
              else ra.type end                                                      as HeadquarterQuarterRaType,
          case
              when cd.foreign_address is true then cd.residential_area_foreign
              else ra.name end                                                      as HeadquarterQuarterRaName,
          case when cd.foreign_address is true then cd.street_type else s.type end  as HeadquarterStrBlvdType,
          case
              when cd.foreign_address is true then cd.street_foreign
              else s.name end                                                       as HeadquarterStrBlvdName,
          cd.street_number                                                          as HeadquarterStrBlvdNumber,
          case when cd.foreign_address is true then cd.block else cd.block end      as HeadquarterBlock,
          cd.entrance                                                               as HeadquarterEntrance,
          cd.floor                                                                  as HeadquarterFloor,
          cd.apartment                                                              as HeadquarterApartment,
          cd.address_additional_info                                                as HeadquarterAdditionalInfo,
          concat_ws(', ',
                    nullif(concat_ws(' ',
                                     case
                                         when cd.foreign_address is true then concat(cd.district_foreign, ',')
                                         else concat(d.name, ',') end,
                                     case
                                         when cd.foreign_address is true then
                                             case
                                                 when cd.foreign_residential_area_type is not null
                                                     then concat(cd.foreign_residential_area_type, ' ',
                                                                 cd.residential_area_foreign)
                                                 else cd.residential_area_foreign
                                                 end
                                         else
                                             case
                                                 when ra.type is not null
                                                     then concat(ra.type, ' ', ra.name)
                                                 else ra.name
                                                 end
                                         end
                           ), ''),
                    nullif(concat_ws(' ',
                                     case when cd.foreign_address is true then cd.street_type else s.type end,
                                     case when cd.foreign_address is true then cd.street_foreign else s.name end,
                                     cd.street_number
                           ), ''),
                    nullif(concat('бл. ', cd.block), 'бл. '),
                    nullif(concat('вх. ', cd.entrance), 'вх. '),
                    nullif(concat('ет. ', cd.floor), 'ет. '),
                    nullif(concat('ап. ', cd.apartment), 'ап. '),
                    nullif(cd.address_additional_info, '')
          ) as HeadquarterAddressComb,
          concat_ws(', ',
                    nullif(concat_ws(' ',
                                     case
                                         when cc.foreign_address is true then cc.district_foreign
                                         else d2.name end,
                                     case
                                         when cc.foreign_address is true then
                                             case
                                                 when cc.foreign_residential_area_type is not null
                                                     then concat(cc.foreign_residential_area_type, ' ',
                                                                 cc.residential_area_foreign)
                                                 else cc.residential_area_foreign
                                                 end
                                         else
                                             case
                                                 when ra2.type is not null
                                                     then concat(ra2.type, ' ', ra2.name)
                                                 else ra2.name
                                                 end
                                         end
                           ), ''),
                    nullif(concat_ws(' ',
                                     case when cc.foreign_address is true then cc.street_type else s2.type end,
                                     case when cc.foreign_address is true then cc.street_foreign else s2.name end,
                                     cc.street_number
                           ), ''),
                    nullif(concat('бл. ', cc.block), 'бл. '),
                    nullif(concat('вх. ', cc.entrance), 'вх. '),
                    nullif(concat('ет. ', cc.floor), 'ет. '),
                    nullif(concat('ап. ', cc.apartment), 'ап. '),
                    nullif(cc.address_additional_info, '')
          ) as CommunicationAddressComb,
          string_agg(seg.name, ', ') as CustomerSegments
      from
          rl
              join customer.customers c
                   on rl.customer_id = c.id and rn = 1
              join customer.customer_details cd
                   on c.last_customer_detail_id = cd.id
              left join product_contract.contract_billing_groups cgb
                        on rl.contract_billing_group_id = cgb.id
              join customer.customer_communications cc on 
              cc.id = (case
                           when rl.contract_billing_group_id is not null and exists (
                               select 1
                               from product_contract.contract_billing_groups cbg
                               where cbg.id = rl.contract_billing_group_id
                                 and cbg.customer_communication_id_for_billing is not null
                           ) then (select cbg.customer_communication_id_for_billing
                                   from product_contract.contract_billing_groups cbg
                                   where cbg.id = rl.contract_billing_group_id)
                           when rl.inv_cust_communication_id is not null
                               then rl.inv_cust_communication_id
                           else (select icc.id
                                 from customer.customer_communications icc
                                          left join (
                                     select pcd.customer_communication_id_for_billing, pcd.start_date
                                     from product_contract.contract_details pcd
                                              join product_contract.contracts c on pcd.contract_id = c.id
                                     where c.status = 'ACTIVE'
                                       and pcd.customer_detail_id = cd.id
                                       and pcd.start_date <= current_date
                                 ) pc on pc.customer_communication_id_for_billing = icc.id
                                          left join (
                                     select scd.customer_communication_id_for_billing, scd.start_date
                                     from service_contract.contract_details scd
                                              join service_contract.contracts sc on scd.contract_id = sc.id
                                     where sc.status = 'ACTIVE'
                                       and scd.customer_detail_id = cd.id
                                       and scd.start_date <= current_date
                                 ) sc on sc.customer_communication_id_for_billing = icc.id
                                          left join (
                                     select ord.customer_communication_id_for_billing, ord.create_date
                                     from goods_order.orders ord
                                     where ord.customer_detail_id = cd.id
                                       and ord.status = 'ACTIVE'
                                 ) ord on ord.customer_communication_id_for_billing = icc.id
                                          left join (
                                     select sord.customer_communication_id_for_billing, sord.create_date
                                     from service_order.orders sord
                                     where sord.customer_detail_id = cd.id
                                       and sord.status = 'ACTIVE'
                                 ) sord on sord.customer_communication_id_for_billing = icc.id
                                 where coalesce(pc.customer_communication_id_for_billing,0) +
                                       coalesce(sc.customer_communication_id_for_billing,0)+
                                       coalesce(ord.customer_communication_id_for_billing,0)+
                                       coalesce(sord.customer_communication_id_for_billing,0) > 0
                                 order by coalesce(pc.start_date, sc.start_date,ord.create_date,sord.create_date) desc
                                 limit 1) end)
                  and exists
                  (select 1 from customer.customer_comm_contact_purposes cccp
                                     join nomenclature.contact_purposes cp
                                          on cccp.contact_purpose_id = cp.id
                                              and cccp.customer_communication_id = cc.id
                                              and cp.name = 'Billing'
                                              and cp.status = 'ACTIVE')
              left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
              left join nomenclature.legal_forms_transl lft on lf.id= lft.legal_form_id
              left join nomenclature.residential_areas ra on cd.residential_area_id = ra.id
              left join nomenclature.districts d on cd.district_id = d.id
              left join nomenclature.streets s on cd.street_id = s.id
              left join nomenclature.populated_places pp on cd.populated_place_id = pp.id
              left join nomenclature.zip_codes zc on cd.zip_code_id = zc.id
              left join nomenclature.zip_codes zc2 on cc.zip_code_id = zc2.id
              left join nomenclature.residential_areas ra2 on cc.residential_area_id = ra2.id
              left join nomenclature.districts d2 on cc.residential_area_id = d2.id
              left join nomenclature.streets s2 on cc.street_id = s2.id
              left join nomenclature.populated_places pp2 on cc.populated_place_id = pp2.id
              left join customer.customer_segments cs on cs.customer_detail_id=cd.id
              left join nomenclature.segments seg on cs.segment_id= seg.id
      group by
          c.customer_type,
          cd.name,
          cd.middle_name,
          cd.last_name,
          cd.name_transl,
          cd.middle_name_transl,
          cd.last_name_transl,
          lf.name,
          lft.name,
          cc.id,
          c.id,
          cd.id,
          c.identifier,
          c.customer_number,
          cd.foreign_address,
          cd.populated_place_foreign,
          pp.name,
          cd.zip_code_foreign,
          zc.zip_code,
          zc2.zip_code,
          cd.district_foreign,
          d.name,
          cc.foreign_address,
          cc.district_foreign,
          d2.name,
          cc.residential_area_type,
          ra2.type,
          cc.residential_area_foreign,
          ra2.name,
          cc.street_type,
          s2.type,
          cc.street_foreign,
          s2.name,
          cc.street_number,
          cc.block,
          cc.entrance,
          cc.floor,
          cc.apartment,
          cc.address_additional_info,
          pp2.name,
          cd.residential_area_type,
          ra.type,
          cd.residential_area_foreign,
          ra.name,
          cd.street_type,
          s.type,
          cd.street_foreign,
          s.name,
          cd.street_number,
          cd.block,
          cd.entrance,
          cd.floor,
          cd.apartment,
          cd.address_additional_info,
          cd.foreign_residential_area_type,
          cc.foreign_residential_area_type,
          cc.populated_place_foreign
""", nativeQuery = true)
    List<ReminderForDcnDocumentMiddleResponse> getReminderForDcnDocumentFields(@Param("reminderForDcnId") Long reminderForDcnId);

    @Query("""
       select sum(rc.liabilityAmount)
       from PowerSupplyDisconnectionReminder r
       join PowerSupplyDisconnectionReminderCustomers rc on rc.powerSupplyDisconnectionReminderId=r.id
       where r.id = :reminderDcnId
       and rc.customerId = :customerId
""")
    BigDecimal getReminderLiabilityTotalAmount(@Param("reminderDcnId") Long reminderDcnId, @Param("customerId") Long customerId);


    @Query("""
       select cl
       from PowerSupplyDisconnectionReminderCustomers rc
       join CustomerLiability cl on rc.customerLiabilityId=cl.id
       where rc.powerSupplyDisconnectionReminderId=:reminderForDcnId
       and rc.customerId=:customerId
       and cl.status = 'ACTIVE'
""")
    List<CustomerLiability> getLiabilitiesPerCustomer(Long reminderForDcnId, Long customerId);

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


}
