package bg.energo.phoenix.repository.crm.smsCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunication;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
import bg.energo.phoenix.model.response.crm.smsCommunication.SmsCommunicationListingMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmsCommunicationRepository extends JpaRepository<SmsCommunication, Long> {

    Optional<SmsCommunication> findByIdAndStatusAndCommunicationChannel(Long id, EntityStatus entityStatus, SmsCommunicationChannel communicationChannel);


    @Query(value = """
        with ind_sms as(SELECT
                            scc.id AS massOrIndSmsCommunicationId,
                            case
                                when sc.communication_channel = 'MASS_SMS' then sc.id
                                else null
                                end as LinkedSmsCommunicationId,
                            CASE
                                WHEN c.customer_type = 'PRIVATE_CUSTOMER' THEN
                                    CONCAT(cd.name, COALESCE(' ' || cd.middle_name, ''), COALESCE(' ' || cd.last_name, ''))
                                WHEN c.customer_type = 'LEGAL_ENTITY' THEN
                                    CONCAT(cd.name, ' ', lf.name)
                                END AS customerName,
                            c.identifier AS uicOrPersonalNumber,
                            sca.activitydesc as activityDesc,
                            sca.activityasc as activityAsc,
                            vsccp.contactpurposedesc contactPurposeDesc,
                            vsccp.contactpurposeasc contactPurposeAsc,
                            am.display_name AS creatorEmployee,
                            am2.display_name AS senderEmployee,
                            sc.communication_type AS communicationType,
                            cc.contact_type_name AS communicationData,
                            sc.sent_date AS sentReceiveDate,
                            sc.create_date AS createDate,
                            ct.name AS communicationTopic,
                            scc.sms_comm_status AS communicationStatus,
                            sc.status AS status,
                            'SMS' AS communicationchannel,
                            'Sms-' || scc.id as name
        
                            FROM
                                crm.sms_communications sc
                            JOIN crm.sms_communication_customers scc ON scc.sms_communication_id = sc.id
                            JOIN customer.customer_details cd ON scc.customer_detail_id = cd.id
                            JOIN customer.customers c ON cd.customer_id = c.id
                            LEFT JOIN customer.customer_communications cc ON scc.customer_communication_id = cc.id
                            LEFT JOIN nomenclature.legal_forms lf ON cd.legal_form_id = lf.id
                            JOIN nomenclature.communication_topics ct ON sc.communication_topic_id = ct.id
                            LEFT JOIN customer.account_managers am ON sc.system_user_id = am.user_name
                            LEFT JOIN customer.account_managers am2 ON sc.sender_employee_id = am2.id
                            LEFT JOIN crm.vw_sms_communication_activity sca ON sca.sms_communication_id = sc.id
                            LEFT JOIN crm.vw_sms_communication_contact_purposes vsccp ON vsccp.sms_communication_id = sc.id
                        WHERE
                            (:entityStatuses IS NULL OR sc.status::text in (:entityStatuses))
                          AND (:creatorEmployeeId IS NULL OR am.id in(:creatorEmployeeId))
                          AND (:senderEmployeeId IS NULL OR am2.id in(:senderEmployeeId))
                          and (date(:createDateFrom) is null or date(sc.create_date) >= date(:createDateFrom))
                          and (date(:createDateTo) is null or date(sc.create_date) <= date(:createDateTo))
                          AND (:contactPurposeId IS NULL OR EXISTS (
                            SELECT 1
                            FROM crm.sms_communication_contact_purposes sccp
                            JOIN nomenclature.contact_purposes cp ON sccp.contact_purpose_id = cp.id
                            WHERE sccp.sms_communication_id = sc.id
                          AND sccp.status = 'ACTIVE'
                          AND cp.id in (:contactPurposeId)
                          AND cp.status = 'ACTIVE'))
                          AND (:communicationType IS NULL OR sc.communication_type::text in(:communicationType))
                          AND (:taskId IS NULL OR EXISTS (
                            SELECT 1
                            FROM crm.sms_communication_tasks sct1
                            WHERE sct1.sms_communication_id = sc.id
                          AND sct1.task_id in (:taskId)
                          AND sct1.status = 'ACTIVE'))
                          AND (:activityId IS NULL OR EXISTS (
                            SELECT 1
                            FROM crm.sms_communication_activity sca
                            JOIN activity.activity act ON sca.activity_id = act.id
                            WHERE sca.sms_communication_id = sc.id
                          AND act.activity_id in (:activityId)
                          AND sca.status = 'ACTIVE'))
                          AND (:communicationTopicId IS NULL OR sc.communication_topic_id in (:communicationTopicId))
                          AND (:smsCommunicationStatus IS NULL OR scc.sms_comm_status::text in (:smsCommunicationStatus))
                          AND (:prompt IS NULL OR (
                            :searchBy = 'ALL'
                          AND :prompt IS NOT NULL
                          AND (
                            (:prompt != '%%' and scc.id::text =  replace(:prompt,'%','')) or
                            (:prompt != '%%' and  sc.id::text =  replace(:prompt,'%',''))
                           OR LOWER(cc.contact_type_name) LIKE :prompt
                           OR LOWER(CONCAT(cd.name, c.identifier, c.customer_number)) LIKE :prompt
                           OR EXISTS (
                            SELECT 1
                            FROM crm.sms_communication_customer_contacts sccc
                            WHERE sccc.sms_communication_customer_id = scc.id
                          AND LOWER(sccc.phone_number) LIKE :prompt)
                            )
                           OR (
                            (:searchBy = 'COMMUNICATION_ID'  and :prompt != '%%' and scc.id::text =  replace(:prompt,'%',''))
                           OR (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and :prompt != '%%' and sc.id::text =  replace(:prompt,'%',''))
                           OR (:searchBy = 'COMMUNICATION_DATA' AND LOWER(cc.contact_type_name) LIKE :prompt)
                           OR (:searchBy = 'CUSTOMER_NAME' AND LOWER(cd.name) LIKE :prompt)
                           OR (:searchBy = 'CUSTOMER_IDENTIFIER' AND LOWER(c.identifier) LIKE :prompt)
                           OR (:searchBy = 'CUSTOMER_NUMBER' AND c.customer_number::text LIKE :prompt)
                           OR (:searchBy = 'PHONE_NUMBER' AND EXISTS (
                            SELECT 1
                            FROM crm.sms_communication_customer_contacts sccc
                            WHERE sccc.sms_communication_customer_id = scc.id
                          AND LOWER(sccc.phone_number) LIKE :prompt)))))
        )
                ,
             mass_sms as(
                 SELECT
                      sc.id as massOrIndSmsCommunicationId,
                      null::bigint as LinkedSmsCommunicationId,
                      null::text as customerName,
                      null::character varying as uicOrPersonalNumber,
                    sca.activitydesc,
                    sca.activityasc,
                    vsccp.contactpurposedesc,
                    vsccp.contactpurposeasc,
                    am.display_name as creatorEmployee,
                    am2.display_name as senderEmployee,
              sc.communication_type as communicationType,
              null::character varying as communicationData,
              sc.sent_date as sentReceiveDate,
              sc.create_date as createDate,
              ct.name as communicationTopic,
              sc.communication_status as communicationStatus,
              sc.status as status,
              'MASS_SMS' as communicationchannel,
                'Mass sms-' || sc.id as name
        
             from
              crm.sms_communications sc
             join nomenclature.communication_topics ct on sc.communication_topic_id = ct.id
             left join customer.account_managers am on sc.system_user_id = am.user_name
             left join customer.account_managers am2 on sc.sender_employee_id = am2.id
             LEFT JOIN crm.vw_sms_communication_activity sca ON sca.sms_communication_id = sc.id
             LEFT JOIN crm.vw_sms_communication_contact_purposes vsccp ON vsccp.sms_communication_id = sc.id
             where
                    sc.communication_channel = 'MASS_SMS'
                    and (:entityStatuses is null or sc.status::text in (:entityStatuses))
                    and (:creatorEmployeeId is null or am.id in (:creatorEmployeeId))
                    and (:senderEmployeeId is null or am2.id in (:senderEmployeeId))
                    and (date(:createDateFrom) is null or date(sc.create_date) >= date(:createDateFrom))
                    and (date(:createDateTo) is null or date(sc.create_date) <= date(:createDateTo))
                and (:contactPurposeId is null or exists (select 1
                                                   from crm.sms_communication_contact_purposes sccp
                                                     join nomenclature.contact_purposes cp
                                                     on sccp.contact_purpose_id = cp.id
                                                     and sccp.sms_communication_id = sc.id and sccp.status = 'ACTIVE'
                                                    and cp.id in (:contactPurposeId)
                                                    and cp.status = 'ACTIVE'))
                 and (:communicationType is null or sc.communication_type::text in (:communicationType))
                 and (:taskId is null or exists (select 1
                                                   from crm.sms_communication_tasks sct1
                                                   where sct1.sms_communication_id = sc.id and
                                                    sct1.task_id in (:taskId)
                                                    and sct1.status = 'ACTIVE'))
                 and (:activityId is null or exists (select 1
                                                   from crm.sms_communication_activity sca
                                                    join activity.activity act on sca.activity_id = act.id
                                                    and sca.sms_communication_id = sc.id
                                                    and act.activity_id in (:activityId)
                                                    and sca.status = 'ACTIVE'))
                 and (:communicationTopicId is null or sc.communication_topic_id in (:communicationTopicId))
                 and (:massSmsCommunicationStatus is null or sc.communication_status::text in (:massSmsCommunicationStatus))
                and (:prompt is null or (:searchBy = 'ALL' and :prompt is not null and (
                exists(select 1
                        from crm.sms_communication_customers scc
                         where scc.customer_communication_id = sc.id and scc.id::text like :prompt)
                 or (:prompt != '%%' and sc.id::text =  replace(:prompt,'%',''))
                 or exists (select 1 from crm.sms_communication_customers scc
                            join
                            customer.customer_communications cc
                             on scc.customer_communication_id = cc.id
                            and scc.sms_communication_id = sc.id
                            and lower(cc.contact_type_name) like :prompt)
                 or exists(select 1 from crm.sms_communication_customers scc
                            join customer.customer_details cd
                            on scc.customer_detail_id = cd.id
                            and scc.sms_communication_id = sc.id
                            join customer.customers c on cd.customer_id = c.id
                            and lower(concat(cd.name,c.identifier,c.customer_number)) like :prompt
                            )
                 or exists (select 1 from crm.sms_communication_customers scc
                            join
                            crm.sms_communication_customer_contacts sccc
                             on sccc.sms_communication_customer_id = scc.id
                            and scc.sms_communication_id = sc.id
                            and lower(sccc.phone_number) like :prompt)
                 )
                  or((:searchBy = 'COMMUNICATION_ID' and :prompt != '%%' and sc.id::text =  replace(:prompt,'%',''))
                  or(:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and :prompt != '%%' and sc.id::text =  replace(:prompt,'%',''))
                  or(:searchBy = 'COMMUNICATION_DATA' and exists (select 1 from crm.sms_communication_customers scc
                            join customer.customer_communications cc on scc.customer_communication_id = cc.id
                            and scc.sms_communication_id = sc.id and lower(cc.contact_type_name) like :prompt))
                  or(:searchBy = 'CUSTOMER_NAME' and exists ( select 1 from crm.sms_communication_customers scc
                                                               join customer.customer_details cd on scc.customer_detail_id = cd.id
                                                               and scc.sms_communication_id = sc.id
                                                               and lower(cd.name) like :prompt))
                  or(:searchBy = 'CUSTOMER_IDENTIFIER' and exists ( select 1 from crm.sms_communication_customers scc
                                                               join customer.customer_details cd on scc.customer_detail_id = cd.id
                                                               and scc.sms_communication_id = sc.id
                                                               join customer.customers c
                                                               on cd.customer_id = c.id
                                                               and lower(c.identifier) like :prompt))
                  or(:searchBy = 'CUSTOMER_NUMBER' and exists ( select 1 from crm.sms_communication_customers scc
                                                               join customer.customer_details cd on scc.customer_detail_id = cd.id
                                                               and scc.sms_communication_id = sc.id
                                                               join customer.customers c
                                                               on cd.customer_id = c.id
                                                               and c.customer_number::text like :prompt))
                  or(:searchBy = 'PHONE_NUMBER' and exists (select 1 from crm.sms_communication_customers scc
                            join
                            crm.sms_communication_customer_contacts sccc
                             on sccc.sms_communication_customer_id = scc.id
                            and scc.sms_communication_id = sc.id
                            and lower(sccc.phone_number) like :prompt)))))
             )
        select * from (
                          select * from ind_sms
                          union all
                          select * from mass_sms  ) as sms
        where ((:kindOfCommunication) is null or text(sms.communicationChannel)  in (:kindOfCommunication))
              
""", nativeQuery = true,
    countQuery = """
            with ind_sms as(SELECT \s
                    count(1) as cnt,
                    'SMS' AS communicationchannel\s
                FROM\s
                    crm.sms_communications sc
                JOIN crm.sms_communication_customers scc ON scc.sms_communication_id = sc.id
                JOIN customer.customer_details cd ON scc.customer_detail_id = cd.id
                JOIN customer.customers c ON cd.customer_id = c.id
                LEFT JOIN customer.customer_communications cc ON scc.customer_communication_id = cc.id \s
                LEFT JOIN customer.account_managers am ON sc.system_user_id = am.user_name
                --LEFT JOIN customer.account_managers am2 ON sc.sender_employee_id = am2.id
                WHERE\s
                    (:entityStatuses IS NULL OR sc.status::text in (:entityStatuses))
                    AND (:creatorEmployeeId IS NULL OR am.id in(:creatorEmployeeId))
                    AND (:senderEmployeeId IS NULL OR sc.sender_employee_id in(:senderEmployeeId))
                    and (date(:createDateFrom) is null or date(sc.create_date) >= date(:createDateFrom))
                    and (date(:createDateTo) is null or date(sc.create_date) <= date(:createDateTo))
                    AND (:contactPurposeId IS NULL OR EXISTS (
                        SELECT 1
                        FROM crm.sms_communication_contact_purposes sccp\s
                        JOIN nomenclature.contact_purposes cp ON sccp.contact_purpose_id = cp.id
                        WHERE sccp.sms_communication_id = sc.id\s
                        AND sccp.status = 'ACTIVE'
                        AND cp.id in (:contactPurposeId)
                        AND cp.status = 'ACTIVE'))
                    AND (:communicationType IS NULL OR sc.communication_type::text in(:communicationType))
                    AND (:taskId IS NULL OR EXISTS (
                        SELECT 1
                        FROM crm.sms_communication_tasks sct1
                        WHERE sct1.sms_communication_id = sc.id\s
                        AND sct1.task_id in (:taskId)
                        AND sct1.status = 'ACTIVE'))
                    AND (:activityId IS NULL OR EXISTS (
                        SELECT 1
                        FROM crm.sms_communication_activity sca
                        JOIN activity.activity act ON sca.activity_id = act.id
                        WHERE sca.sms_communication_id = sc.id
                        AND act.activity_id in (:activityId)
                        AND sca.status = 'ACTIVE'))
                    AND (:communicationTopicId IS NULL OR sc.communication_topic_id in (:communicationTopicId))
                    AND (:smsCommunicationStatus IS NULL OR scc.sms_comm_status::text in (:smsCommunicationStatus))
                    AND (:prompt IS NULL OR (
                        :searchBy = 'ALL'\s
                        AND :prompt IS NOT NULL\s
                        AND (
                            (:prompt != '%%' and scc.id::text =  replace(:prompt,'%',''))
                            OR (:prompt != '%%' and sc.id::text =  replace(:prompt,'%',''))
                            OR LOWER(cc.contact_type_name) LIKE :prompt
                            OR LOWER(CONCAT(cd.name, c.identifier, c.customer_number)) LIKE :prompt
                            OR EXISTS (
                                SELECT 1\s
                                FROM crm.sms_communication_customer_contacts sccc\s
                                WHERE sccc.sms_communication_customer_id = scc.id
                                AND LOWER(sccc.phone_number) LIKE :prompt)
                        )
                    OR (
                        (:searchBy = 'COMMUNICATION_ID' AND :prompt != '%%' and scc.id::text =  replace(:prompt,'%',''))
                        OR (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' AND :prompt != '%%' and sc.id::text =  replace(:prompt,'%',''))
                        OR (:searchBy = 'COMMUNICATION_DATA' AND LOWER(cc.contact_type_name) LIKE :prompt)
                        OR (:searchBy = 'CUSTOMER_NAME' AND LOWER(cd.name) LIKE :prompt)
                        OR (:searchBy = 'CUSTOMER_IDENTIFIER' AND LOWER(c.identifier) LIKE :prompt)
                        OR (:searchBy = 'CUSTOMER_NUMBER' AND c.customer_number::text LIKE :prompt)
                        OR (:searchBy = 'PHONE_NUMBER' AND EXISTS (
                            SELECT 1\s
                            FROM crm.sms_communication_customer_contacts sccc\s
                            WHERE sccc.sms_communication_customer_id = scc.id
                            AND LOWER(sccc.phone_number) LIKE :prompt)))))
            )
            ,
            mass_sms as(
               SELECT count(1) as cnt,
              'MASS_SMS' as communicationchannel
             from\s
              crm.sms_communications sc
             left join customer.account_managers am on sc.system_user_id = am.user_name
             where\s
                    sc.communication_channel = 'MASS_SMS'
                    and (:entityStatuses is null or sc.status::text in (:entityStatuses))
                    and (:creatorEmployeeId is null or am.id in (:creatorEmployeeId))
                    and (:senderEmployeeId is null or sc.sender_employee_id in (:senderEmployeeId))
                    and (date(:createDateFrom) is null or date(sc.create_date) >= date(:createDateFrom))
                    and (date(:createDateTo) is null or date(sc.create_date) <= date(:createDateTo))
                and (:contactPurposeId is null or exists (select 1
                                                   from crm.sms_communication_contact_purposes sccp\s
                                                     join nomenclature.contact_purposes cp\s
                                                     on sccp.contact_purpose_id = cp.id
                                                     and sccp.sms_communication_id = sc.id and sccp.status = 'ACTIVE'
                                                    and cp.id in (:contactPurposeId)
                                                    and cp.status = 'ACTIVE'))
                 and (:communicationType is null or sc.communication_type::text in (:communicationType))
                 and (:taskId is null or exists (select 1
                                                   from crm.sms_communication_tasks sct1
                                                   where sct1.sms_communication_id = sc.id and
                                                    sct1.task_id in (:taskId)
                                                    and sct1.status = 'ACTIVE'))       \s
                 and (:activityId is null or exists (select 1
                                                   from crm.sms_communication_activity sca
                                                    join activity.activity act on sca.activity_id = act.id
                                                    and sca.sms_communication_id = sc.id
                                                    and act.activity_id in (:activityId)
                                                    and sca.status = 'ACTIVE'))
                 and (:communicationTopicId is null or sc.communication_topic_id in (:communicationTopicId))
                 and (:massSmsCommunicationStatus is null or sc.communication_status::text in (:massSmsCommunicationStatus))
                and (:prompt is null or (:searchBy = 'ALL' and :prompt is not null and (
                exists(select 1\s
                        from crm.sms_communication_customers scc\s
                         where scc.customer_communication_id = sc.id and scc.id::text like :prompt)
                 or (:prompt != '%%' and sc.id::text =  replace(:prompt,'%',''))
                 or exists (select 1 from crm.sms_communication_customers scc
                            join\s
                            customer.customer_communications cc
                             on scc.customer_communication_id = cc.id
                            and scc.sms_communication_id = sc.id
                            and lower(cc.contact_type_name) like :prompt)
                 or exists(select 1 from crm.sms_communication_customers scc
                            join customer.customer_details cd\s
                            on scc.customer_detail_id = cd.id
                            and scc.sms_communication_id = sc.id
                            join customer.customers c on cd.customer_id = c.id
                            and lower(concat(cd.name,c.identifier,c.customer_number)) like :prompt
                            )
                 or exists (select 1 from crm.sms_communication_customers scc
                            join\s
                            crm.sms_communication_customer_contacts sccc\s
                             on sccc.sms_communication_customer_id = scc.id
                            and scc.sms_communication_id = sc.id
                            and lower(sccc.phone_number) like :prompt)
                 )
                  or((:searchBy = 'COMMUNICATION_ID' and :prompt != '%%' and sc.id::text =  replace(:prompt,'%',''))
                  or(:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and :prompt != '%%' and sc.id::text = replace(:prompt,'%',''))
                  or(:searchBy = 'COMMUNICATION_DATA' and exists (select 1 from crm.sms_communication_customers scc
                            join customer.customer_communications cc on scc.customer_communication_id = cc.id
                            and scc.sms_communication_id = sc.id and lower(cc.contact_type_name) like :prompt))
                  or(:searchBy = 'CUSTOMER_NAME' and exists ( select 1 from crm.sms_communication_customers scc
                                                               join customer.customer_details cd on scc.customer_detail_id = cd.id
                                                               and scc.sms_communication_id = sc.id
                                                               and lower(cd.name) like :prompt))
                  or(:searchBy = 'CUSTOMER_IDENTIFIER' and exists ( select 1 from crm.sms_communication_customers scc
                                                               join customer.customer_details cd on scc.customer_detail_id = cd.id
                                                               and scc.sms_communication_id = sc.id
                                                               join customer.customers c\s
                                                               on cd.customer_id = c.id
                                                               and lower(c.identifier) like :prompt))
                  or(:searchBy = 'CUSTOMER_NUMBER' and exists ( select 1 from crm.sms_communication_customers scc
                                                               join customer.customer_details cd on scc.customer_detail_id = cd.id
                                                               and scc.sms_communication_id = sc.id
                                                               join customer.customers c\s
                                                               on cd.customer_id = c.id
                                                               and c.customer_number::text like :prompt))
                  or(:searchBy = 'PHONE_NUMBER' and exists (select 1 from crm.sms_communication_customers scc
                            join\s
                            crm.sms_communication_customer_contacts sccc\s
                             on sccc.sms_communication_customer_id = scc.id
                            and scc.sms_communication_id = sc.id
                            and lower(sccc.phone_number) like :prompt)))))
                            )
              select sum(cnt) as record_count from (
              select cnt,communicationChannel from ind_sms  
               union all
              select cnt,communicationChannel from mass_sms  ) as sms
               where ((:kindOfCommunication) is null or text(sms.communicationChannel)  in (:kindOfCommunication))
""")
    Page<SmsCommunicationListingMiddleResponse> filter(
            @Param("creatorEmployeeId") List<Long> creatorEmployeeId,
            @Param("senderEmployeeId") List<Long> senderEmployeeId,
            @Param("createDateFrom") LocalDateTime createDateFrom,
            @Param("createDateTo") LocalDateTime createDateTo,
            @Param("contactPurposeId") List<Long> contactPurposeId,
            @Param("communicationType") List<String> communicationType,
            @Param("activityId") List<Long> activityId,
            @Param("taskId") List<Long> taskId,
            @Param("communicationTopicId") List<Long> communicationTopicId,
            @Param("kindOfCommunication") List<String> kindOfCommunication,
            @Param("smsCommunicationStatus") List<String> smsCommunicationStatus,
            @Param("massSmsCommunicationStatus") List<String> massSmsCommunicationStatus,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("entityStatuses") List<String> entityStatuses,
            Pageable pageable
            );

    @Query("""
        select s from SmsCommunication s
        where s.id=:id
        and s.communicationChannel='MASS_SMS'
""")
    Optional<SmsCommunication> findMassById(Long id);

    @Query("""
        select sc from SmsCommunication sc
        join SmsCommunicationCustomers scc on scc.smsCommunicationId = sc.id
        where scc.id=:id
""")
    Optional<SmsCommunication> findBySmsCommunicationCustomerId(Long id);

    @Query("""
        select sc from SmsCommunication sc
        join SmsCommunicationCustomers scc on scc.smsCommunicationId = sc.id
        where scc.id=:id
        and sc.status='ACTIVE'
""")
    Optional<SmsCommunication> findBySmsCommunicationCustomerWithActiveStatus(Long id);
}
