package bg.energo.phoenix.repository.receivable.reminder;

import bg.energo.phoenix.model.entity.receivable.reminder.ReminderProcessItem;
import bg.energo.phoenix.model.response.receivable.reminder.LiabilityCommunicationMiddleResponse;
import bg.energo.phoenix.model.response.receivable.reminder.ReminderLiabilityProcessingMiddleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderProcessItemRepository extends JpaRepository<ReminderProcessItem, Long> {
    List<ReminderProcessItem> findAllByReminderIdAndProcessIdOrderByRecordIndex(Long reminderId, Long processId);

    @Query(value = """
                select distinct reminderId,
                contactPurposeId,
                customerId,
                totalliabilityamount,
                liabilityId,
                text(dd.communication_channel) as communicationChannel
                from (select rm.reminderId,
                             rm.contact_purpose_id                                                                   as contactPurposeId,
                             rm.communication_channel,
                             rm.customerId,
                             rm.totalliabilityamount,
                             rm.liabilityId,
                             receivable.liability_condition_eval_exact(rm.liabilityId, rm.customer_under_conditions) as cond_eval
                      from (select distinct reminder.id           as reminderId,
                                            reminder.due_amount_from,
                                            reminder.due_amount_to,
                                            reminder.contact_purpose_id,
                                            reminder.communication_channel,
                                            liability.customer_id as customerId,
                                            SUM(liability.current_amount) OVER (
                                                partition by reminder.id, liability.customer_id
                                                order by reminder.id
                                                )                 as totalliabilityamount,
                                            liability.id          as liabilityId,
                                            reminder.customer_under_conditions
                            from receivable.customer_liabilities liability
                                     join receivable.reminders reminder on 1 = 1
                                and reminder.id = :reminderId and reminder.status = 'ACTIVE'
                            where liability.status = 'ACTIVE'
                              and liability.current_amount > 0
                              and (reminder.currency_id IS NULL OR reminder.currency_id = liability.currency_id)
                              and (
                                case reminder.trigger_for_liabilities
                                    when 'WHEN_OVERDUE' then liability.due_date + COALESCE(reminder.postponement_in_days, 0) + 1 =
                                                             CURRENT_DATE
                                    when 'ON_DUE_DATE' then liability.due_date + COALESCE(reminder.postponement_in_days, 0) =
                                                            CURRENT_DATE
                                    when 'WHEN_INVOICES' then DATE(liability.create_date) + COALESCE(reminder.postponement_in_days, 0) =
                                                              CURRENT_DATE
                                    end
                                )
                              and (
                                CASE reminder.customer_condition
                                    when 'ALL_CUSTOMERS' then 1 = 1
                                    when 'LIST_OF_CUSTOMERS' then exists (select 1
                                                                          from receivable.vw_reminder_customer_list rc
                                                                          where rc.reminder_id = reminder.id
                                                                            and rc.customer_id = liability.customer_id)
                                    when 'CUSTOMERS_UNDER_CONDITIONS' then
                                        liability.id in (select receivable.liability_condition_eval(reminder.customer_under_conditions))
                                    end
                                )) AS rm
                      where (rm.due_amount_from is null or rm.totalliabilityamount >= rm.due_amount_from)
                        and (rm.due_amount_to is null or rm.totalliabilityamount <= rm.due_amount_to)) dd
                        where dd.cond_eval > 0 and (:customerId IS NULL OR dd.customerId = :customerId)
            """,
            nativeQuery = true
    )
    List<ReminderLiabilityProcessingMiddleResponse> getLiabilitiesForReminderProcessing(@Param("reminderId") Long reminderId, @Param("customerId") Long customerId);


    @Query(nativeQuery = true, value = """
                        select distinct cc.id             as communicationId,
                                        ccc.contact_type  as contactType,
                                        ccc.contact_value as contactValue,
                                        ccc.id            as contactId
                        from receivable.customer_liabilities cl
                                 join product_contract.contract_billing_groups cb on cl.contract_billing_group_id = cb.id
                                 join product_contract.contract_details cond
                                      on cond.contract_id = cb.contract_id and cond.version_id = (select max(cond2.version_id)
                                                                                                  from product_contract.contract_details cond2
                                                                                                  where cond2.contract_id = cb.contract_id)
                                 join customer.customer_communications cc on cc.customer_detail_id = cond.customer_detail_id
                                 join customer.customer_comm_contact_purposes cccp
                                      on cccp.customer_communication_id = cc.id and cccp.contact_purpose_id = :purposeId
                                 join customer.customer_communication_contacts ccc
                                      on ccc.customer_communication_id = cc.id
                                          AND CASE
                                                  WHEN text(:communicationChannel) = '{ON_PAPER}' THEN TRUE
                                                  ELSE
                                                      CASE
                                                          WHEN ccc.contact_type = 'MOBILE_NUMBER'
                                                              THEN ccc.send_sms = TRUE AND text(:communicationChannel) LIKE '%SMS%'
                                                          WHEN ccc.contact_type = 'EMAIL' THEN text(:communicationChannel) LIKE '%EMAIL%'
                                                          ELSE FALSE
                                                          END
                                             END
                        where cl.outgoing_document_type IS NULL
                          and cl.contract_billing_group_id is not null
                          and cl.id = :liabilityId
            """
    )
    List<LiabilityCommunicationMiddleResponse> getCommunicationByLiabilityId(Long liabilityId, Long purposeId, String communicationChannel);

    @Query(nativeQuery = true,
            value = """
                        WITH LatestCustomerDetails AS (
                        SELECT c.id AS customer_id, MAX(cd.id) AS max_customer_detail_id
                        FROM customer.customers c
                                 JOIN customer.customer_details cd ON c.id = cd.customer_id
                        WHERE c.id = :customerId
                        GROUP BY c.id
                    ),
                         LatestCustomerCommunications AS (
                             SELECT cc.customer_detail_id, MAX(cc.id) AS max_customer_communication_id
                             FROM customer.customer_communications cc
                                      JOIN LatestCustomerDetails lcd ON lcd.max_customer_detail_id = cc.customer_detail_id
                                      JOIN customer.customer_comm_contact_purposes p ON cc.id = p.customer_communication_id
                             WHERE cc.status = 'ACTIVE'
                               AND p.contact_purpose_id = :purposeId
                             GROUP BY cc.customer_detail_id
                         )
                    SELECT DISTINCT ON (cc.id)  -- Ensures only one record per communicationId
                                                cc.id             AS communicationId,
                                                ccc.contact_value AS contactValues,
                                                ccc.id            AS contactId,
                                                ccc.contact_type  AS contactType
                    FROM customer.customer_comm_contact_purposes cccp
                             JOIN customer.customers c ON c.id = :customerId
                             JOIN customer.customer_details cd ON c.id = cd.customer_id
                             JOIN customer.customer_communications cc ON cc.id = cccp.customer_communication_id
                             JOIN LatestCustomerDetails lcd ON lcd.customer_id = c.id AND lcd.max_customer_detail_id = cd.id
                             JOIN LatestCustomerCommunications lcc
                                  ON lcc.customer_detail_id = cd.id AND lcc.max_customer_communication_id = cc.id
                             JOIN customer.customer_communication_contacts ccc ON ccc.customer_communication_id = cc.id
                    WHERE cc.customer_detail_id = cd.id
                      AND cccp.status = 'ACTIVE'
                      AND ccc.status = 'ACTIVE'
                      AND CASE
                              WHEN text(:communicationChannel) = '{ON_PAPER}' THEN TRUE
                              ELSE
                                  CASE
                                      WHEN ccc.contact_type = 'MOBILE_NUMBER' THEN ccc.send_sms = TRUE AND text(:communicationChannel) LIKE '%SMS%'
                                      WHEN ccc.contact_type = 'EMAIL' THEN text(:communicationChannel) LIKE '%EMAIL%'
                                      ELSE FALSE
                                      END
                        END
                      AND ccc.contact_value IS NOT NULL
                    """
    )
    List<LiabilityCommunicationMiddleResponse> getCustomerCommunicationDataListForDocument(
            @Param("customerId") Long customerId,
            @Param("purposeId") Long purposeId,
            @Param("communicationChannel") String communicationChannel
    );

}
