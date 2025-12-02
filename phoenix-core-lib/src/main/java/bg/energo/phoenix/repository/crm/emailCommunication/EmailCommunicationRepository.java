package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.documentModels.EmailAndSmsDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunication;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationChannelType;
import bg.energo.phoenix.model.response.crm.emailCommunication.EmailCommunicationListingMiddleResponse;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface EmailCommunicationRepository extends JpaRepository<EmailCommunication, Long> {

    Optional<EmailCommunication> findByIdAndEntityStatus(Long id, EntityStatus entityStatus);

    Optional<EmailCommunication> findByIdAndEntityStatusAndCommunicationChannel(
            Long id,
            EntityStatus entityStatus,
            EmailCommunicationChannelType communicationChannel
    );

    @Query(
            value = """
                    select ec
                    from EmailCommunicationCustomer ecc
                    join EmailCommunication ec on ec.id = ecc.emailCommunicationId
                    where ecc.id = :emailCommunicationCustomerId
                    """
    )
    Optional<EmailCommunication> findByEmailCommunicationCustomerId(Long emailCommunicationCustomerId);

    @Query(
            value = """
                    with ind_email as (SELECT case
                                                  when ec.communication_channel = 'MASS_EMAIL' then ecc.id
                                                  else ec.id end             AS massOrIndEmailCommunicationId,
                                              case
                                                  when ec.communication_channel = 'MASS_EMAIL' then ec.id
                                                  else null
                                                  end                        as LinkedEmailCommunicationId,
                                              CASE
                                                  WHEN c.customer_type = 'PRIVATE_CUSTOMER' THEN
                                                      CONCAT(cd.name, COALESCE(' ' || cd.middle_name, ''),
                                                             COALESCE(' ' || cd.last_name, ''))
                                                  WHEN c.customer_type = 'LEGAL_ENTITY' THEN
                                                      CONCAT(cd.name, ' ', lf.name)
                                                  END                        AS customerName,
                                              c.identifier                   AS uicOrPersonalNumber,
                                              case
                                                  when :activityDirection = 'desc'
                                                      then
                                                      veca.activityDesc
                                                  else
                                                      veca.activityAsc
                                                  end                        as activity,
                                              case
                                                  when :contactPurposeDirection = 'desc'
                                                      then
                                                      veccp.contactPurposeDesc
                                                  else
                                                      veccp.contactPurposeAsc
                                                  end                        as contactPurpose,
                                              am.display_name                AS creatorEmployee,
                                              am2.display_name               AS senderEmployee,
                                              ec.communication_type          AS communicationType,
                                              cc.contact_type_name           AS communicationData,
                                              ec.sent_date                   AS sentReceiveDate,
                                              ec.create_date                 AS createDate,
                                              ct.name                        AS communicationTopic,
                                              ec.communication_status        AS communicationStatus,
                                              ec.status                      AS status,
                                              'EMAIL'                        AS communicationchannel,
                                              ec.email_subject               as emailSubject,
                                              case
                                                  when ec.communication_channel = 'MASS_EMAIL' then 'Email-' || ecc.id
                                                  else 'Email-' || ec.id end as name
                                       FROM crm.email_communications ec
                                                JOIN crm.email_communication_customers ecc ON ecc.email_communication_id = ec.id
                                                JOIN customer.customer_details cd ON ecc.customer_detail_id = cd.id
                                                JOIN customer.customers c ON cd.customer_id = c.id
                                                LEFT JOIN customer.customer_communications cc ON ecc.customer_communication_id = cc.id
                                                LEFT JOIN nomenclature.legal_forms lf ON cd.legal_form_id = lf.id
                                                JOIN nomenclature.communication_topics ct ON ec.communication_topic_id = ct.id
                                                LEFT JOIN customer.account_managers am ON ec.system_user_id = am.user_name
                                                LEFT JOIN customer.account_managers am2 ON ec.sender_employee_id = am2.id
                                                LEFT JOIN crm.vw_email_communication_activity veca ON veca.email_communication_id = ec.id
                                                LEFT JOIN crm.vw_email_communication_contact_purposes veccp
                                                          ON veccp.email_communication_id = ec.id
                                       WHERE (:entityStatuses IS NULL OR ec.status::text in (:entityStatuses))
                                         AND (:creatorEmployeeId IS NULL OR am.id in (:creatorEmployeeId))
                                         AND (:senderEmployeeId IS NULL OR am2.id in (:senderEmployeeId))
                                         and (date(:createDateFrom) is null or date(ec.create_date) >= date(:createDateFrom))
                                         and (date(:createDateTo) is null or date(ec.create_date) <= date(:createDateTo))
                                           /*and (case when ec.communication_channel = 'MASS_EMAIL' and ec.communication_status = 'DRAFT'
                                                then 1=2 else 1=1 end)*/
                                         AND (concat('', :contactPurposeId, '') = '' OR EXISTS (SELECT 1
                                                                                                FROM crm.email_communication_contact_purposes eccp
                                                                                                         JOIN nomenclature.contact_purposes cp ON eccp.contact_purpose_id = cp.id
                                                                                                WHERE eccp.email_communication_id = ec.id
                                                                                                  AND eccp.status = 'ACTIVE'
                                                                                                  AND cp.id in (:contactPurposeId)
                                                                                                  AND cp.status = 'ACTIVE'))
                                         AND (:communicationType IS NULL OR ec.communication_type::text in (:communicationType))
                                         AND (concat('', :taskId, '') = '' OR EXISTS (SELECT 1
                                                                                      FROM crm.email_communication_tasks ect1
                                                                                      WHERE ect1.email_communication_id = ec.id
                                                                                        AND ect1.task_id in (:taskId)
                                                                                        AND ect1.status = 'ACTIVE'))
                                         AND (concat('', :activityId, '') = '' OR EXISTS (SELECT 1
                                                                                          FROM crm.email_communication_activity eca
                                                                                                   JOIN activity.activity act ON eca.activity_id = act.id
                                                                                          WHERE eca.email_communication_id = ec.id
                                                                                            AND act.activity_id in (:activityId)
                                                                                            AND eca.status = 'ACTIVE'))
                                         AND (:communicationTopicId IS NULL OR ec.communication_topic_id in (:communicationTopicId))
                                         AND (:communicationStatus IS NULL OR ec.communication_status::text in (:communicationStatus))
                                         AND (:prompt IS NULL OR (
                                           :searchBy = 'ALL'
                                               AND :prompt IS NOT NULL
                                               AND (
                                               ecc.id::text = replace(:prompt, '%', '')
                                                   OR ec.id::text = replace(:prompt, '%', '')
                                                   OR LOWER(cc.contact_type_name) LIKE :prompt
                                                   OR LOWER(ec.dms_number) LIKE :prompt
                                                   OR LOWER(ec.email_subject) LIKE :prompt
                                                   OR LOWER(CONCAT(cd.name, c.identifier, c.customer_number)) LIKE :prompt
                                                   OR EXISTS (SELECT 1
                                                              FROM crm.email_communication_customer_contacts eccc
                                                              WHERE eccc.email_communication_customer_id = ecc.id
                                                                AND LOWER(eccc.email_address) LIKE :prompt)
                                               )
                                               OR (
                                               (:searchBy = 'COMMUNICATION_ID' AND ecc.id::text = replace(:prompt, '%', ''))
                                                   OR
                                               (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' AND ec.id::text = replace(:prompt, '%', ''))
                                                   OR (:searchBy = 'COMMUNICATION_DATA' AND LOWER(cc.contact_type_name) LIKE :prompt)
                                                   OR (:searchBy = 'CUSTOMER_NAME' AND LOWER(cd.name) LIKE :prompt)
                                                   OR (:searchBy = 'DMS_NUMBER' AND LOWER(ec.dms_number) LIKE :prompt)
                                                   OR (:searchBy = 'EMAIL_SUBJECT' AND LOWER(ec.email_subject) LIKE :prompt)
                                                   OR (:searchBy = 'CUSTOMER_IDENTIFIER' AND LOWER(c.identifier) LIKE :prompt)
                                                   OR (:searchBy = 'CUSTOMER_NUMBER' AND c.customer_number::text LIKE :prompt)
                                                   OR (:searchBy = 'EMAIL_ADDRESS' AND EXISTS (SELECT 1
                                                                                               FROM crm.email_communication_customer_contacts eccc
                                                                                               WHERE eccc.email_communication_customer_id = ecc.id
                                                                                                 AND LOWER(eccc.email_address) LIKE :prompt))))))
                            ,
                         mass_email as (SELECT ec.id                   as massOrIndEmailCommunicationId,
                                               null::bigint            as LinkedEmailCommunicationId,
                                               null::text              as customerName,
                                               null::character varying as uicOrPersonalNumber,
                                               case
                                                   when :activityDirection = 'desc'
                                                       then
                                                       veca.activityDesc
                                                   else
                                                       veca.activityAsc
                                                   end                 as activity,
                                               case
                                                   when :contactPurposeDirection = 'desc'
                                                       then
                                                       veccp.contactPurposeDesc
                                                   else
                                                       veccp.contactPurposeAsc
                                                   end                 as contactPurpose,
                                               am.display_name         as creatorEmployee,
                                               am2.display_name        as senderEmployee,
                                               ec.communication_type   as communicationType,
                                               null::character varying as communicationData,
                                               ec.sent_date            as sentReceiveDate,
                                               ec.create_date          as createDate,
                                               ct.name                 as communicationTopic,
                                               ec.communication_status as communicationStatus,
                                               ec.status               as status,
                                               'MASS_EMAIL'            as communicationchannel,
                                               ec.email_subject        as emailSubject,
                                               'Mass email-' || ec.id  as name
                                        from crm.email_communications ec
                                                 join nomenclature.communication_topics ct on ec.communication_topic_id = ct.id
                                                 left join customer.account_managers am on ec.system_user_id = am.user_name
                                                 left join customer.account_managers am2 on ec.sender_employee_id = am2.id
                                                 LEFT JOIN crm.vw_email_communication_activity veca ON veca.email_communication_id = ec.id
                                                 LEFT JOIN crm.vw_email_communication_contact_purposes veccp
                                                           ON veccp.email_communication_id = ec.id
                                        where ec.communication_channel = 'MASS_EMAIL'
                                          and (:entityStatuses is null or ec.status::text in (:entityStatuses))
                                          and (:creatorEmployeeId is null or am.id in (:creatorEmployeeId))
                                          and (:senderEmployeeId is null or am2.id in (:senderEmployeeId))
                                          and (date(:createDateFrom) is null or date(ec.create_date) >= date(:createDateFrom))
                                          and (date(:createDateTo) is null or date(ec.create_date) <= date(:createDateTo))
                                          and (concat('', :contactPurposeId, '') = '' or exists (select 1
                                                                                                 from crm.email_communication_contact_purposes eccp
                                                                                                          join nomenclature.contact_purposes cp
                                                                                                               on eccp.contact_purpose_id =
                                                                                                                  cp.id
                                                                                                                   and
                                                                                                                  eccp.email_communication_id =
                                                                                                                  ec.id and
                                                                                                                  eccp.status = 'ACTIVE'
                                                                                                                   and cp.id in
                                                                                                                       (:contactPurposeId)
                                                                                                                   and
                                                                                                                  cp.status = 'ACTIVE'))
                                          and (:communicationType is null or ec.communication_type::text in (:communicationType))
                                          and (concat('', :taskId, '') = '' or exists (select 1
                                                                                       from crm.email_communication_tasks ect1
                                                                                       where ect1.email_communication_id = ec.id
                                                                                         and ect1.task_id in (:taskId)
                                                                                         and ect1.status = 'ACTIVE'))
                                          and (concat('', :activityId, '') = '' or exists (select 1
                                                                                           from crm.email_communication_activity eca
                                                                                                    join activity.activity act
                                                                                                         on eca.activity_id = act.id
                                                                                                             and
                                                                                                            eca.email_communication_id =
                                                                                                            ec.id
                                                                                                             and
                                                                                                            act.activity_id in (:activityId)
                                                                                                             and eca.status = 'ACTIVE'))
                                          and (:communicationTopicId is null or ec.communication_topic_id in (:communicationTopicId))
                                          and (:communicationStatus is null or ec.communication_status::text in (:communicationStatus))
                                          and (:prompt is null or (:searchBy = 'ALL' and :prompt is not null and (
                                            exists(select 1
                                                   from crm.email_communication_customers ecc
                                                   where ecc.email_communication_id = ec.id
                                                     and ecc.id::text = replace(:prompt, '%', ''))
                                                or ec.id::text = replace(:prompt, '%', '')
                                                or lower(ec.dms_number) like :prompt
                                                or lower(ec.email_subject) like :prompt
                                                or exists (select 1
                                                           from crm.email_communication_customers ecc
                                                                    join
                                                                customer.customer_communications cc
                                                                on ecc.customer_communication_id = cc.id
                                                                    and ecc.email_communication_id = ec.id
                                                                    and lower(cc.contact_type_name) like :prompt)
                                                or exists(select 1
                                                          from crm.email_communication_customers ecc
                                                                   join customer.customer_details cd
                                                                        on ecc.customer_detail_id = cd.id
                                                                            and ecc.email_communication_id = ec.id
                                                                   join customer.customers c on cd.customer_id = c.id
                                                              and lower(concat(cd.name, c.identifier, c.customer_number)) like :prompt)
                                                or exists (select 1
                                                           from crm.email_communication_customers ecc
                                                                    join
                                                                crm.email_communication_customer_contacts eccc
                                                                on eccc.email_communication_customer_id = ecc.id
                                                                    and ecc.email_communication_id = ec.id
                                                                    and lower(eccc.email_address) like :prompt)
                                            )
                                            or ((:searchBy = 'COMMUNICATION_ID' and exists (select 1
                                                                                            from crm.email_communication_customers ecc
                                                                                            where ecc.email_communication_id = ec.id
                                                                                              and ecc.id::text = replace(:prompt, '%', '')))
                                                --  or(:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and ec.id::text = replace(:prompt,'%',''))
                                                or (:searchBy = 'DMS_NUMBER' AND LOWER(ec.dms_number) LIKE :prompt)
                                                or (:searchBy = 'EMAIL_SUBJECT' AND LOWER(ec.email_subject) LIKE :prompt)
                                                or (:searchBy = 'COMMUNICATION_DATA' and exists (select 1
                                                                                                 from crm.email_communication_customers ecc
                                                                                                          join customer.customer_communications cc
                                                                                                               on ecc.customer_communication_id =
                                                                                                                  cc.id
                                                                                                                   and
                                                                                                                  ecc.email_communication_id =
                                                                                                                  ec.id and
                                                                                                                  lower(cc.contact_type_name) like
                                                                                                                  :prompt))
                                                or (:searchBy = 'CUSTOMER_NAME' and exists (select 1
                                                                                            from crm.email_communication_customers ecc
                                                                                                     join customer.customer_details cd
                                                                                                          on ecc.customer_detail_id = cd.id
                                                                                                              and
                                                                                                             ecc.email_communication_id =
                                                                                                             ec.id
                                                                                                              and
                                                                                                             lower(cd.name) like :prompt))
                                                or (:searchBy = 'CUSTOMER_IDENTIFIER' and exists (select 1
                                                                                                  from crm.email_communication_customers ecc
                                                                                                           join customer.customer_details cd
                                                                                                                on ecc.customer_detail_id =
                                                                                                                   cd.id
                                                                                                                    and
                                                                                                                   ecc.email_communication_id =
                                                                                                                   ec.id
                                                                                                           join customer.customers c
                                                                                                                on cd.customer_id = c.id
                                                                                                                    and
                                                                                                                   lower(c.identifier) like
                                                                                                                   :prompt))
                                                or (:searchBy = 'CUSTOMER_NUMBER' and exists (select 1
                                                                                              from crm.email_communication_customers ecc
                                                                                                       join customer.customer_details cd
                                                                                                            on ecc.customer_detail_id =
                                                                                                               cd.id
                                                                                                                and
                                                                                                               ecc.email_communication_id =
                                                                                                               ec.id
                                                                                                       join customer.customers c
                                                                                                            on cd.customer_id = c.id
                                                                                                                and
                                                                                                               c.customer_number::text like
                                                                                                               :prompt))
                                                or (:searchBy = 'EMAIL_ADDRESS' and exists (select 1
                                                                                            from crm.email_communication_customers ecc
                                                                                                     join
                                                                                                 crm.email_communication_customer_contacts eccc
                                                                                                 on eccc.email_communication_customer_id =
                                                                                                    ecc.id
                                                                                                     and ecc.email_communication_id = ec.id
                                                                                                     and lower(eccc.email_address) like
                                                                                                         :prompt))))))
                    select *
                    from (select *
                          from ind_email
                          union all
                          select *
                          from mass_email) as email
                    where ((:kindOfCommunication) is null or text(email.communicationChannel) in (:kindOfCommunication))
                    """,
            nativeQuery = true,
            countQuery = """
                          with ind_email as(SELECT
                          count(1) as cnt,
                          'EMAIL' AS communicationchannel
                      FROM
                          crm.email_communications ec
                      JOIN crm.email_communication_customers ecc ON ecc.email_communication_id = ec.id
                      JOIN customer.customer_details cd ON ecc.customer_detail_id = cd.id
                      JOIN customer.customers c ON cd.customer_id = c.id
                      LEFT JOIN customer.customer_communications cc ON ecc.customer_communication_id = cc.id
                      LEFT JOIN customer.account_managers am ON ec.system_user_id = am.user_name
                      WHERE
                          (:entityStatuses IS NULL OR ec.status::text in (:entityStatuses))
                          AND (:creatorEmployeeId IS NULL OR am.id in(:creatorEmployeeId))
                          AND (:senderEmployeeId IS NULL OR ec.sender_employee_id in(:senderEmployeeId))
                          and (date(:createDateFrom) is null or date(ec.create_date) >= date(:createDateFrom))
                          and (date(:createDateTo) is null or date(ec.create_date) <= date(:createDateTo))
                          /*and (case when ec.communication_channel = 'MASS_EMAIL' and ec.communication_status = 'DRAFT'
                               then 1=2 else 1=1 end) */                 
                          AND (concat('',:contactPurposeId,'') = '' OR EXISTS (
                              SELECT 1
                              FROM crm.email_communication_contact_purposes eccp
                              JOIN nomenclature.contact_purposes cp ON eccp.contact_purpose_id = cp.id
                              WHERE eccp.email_communication_id = ec.id
                              AND eccp.status = 'ACTIVE'
                              AND cp.id in (:contactPurposeId)
                              AND cp.status = 'ACTIVE'))
                          AND (:communicationType IS NULL OR ec.communication_type::text in(:communicationType))
                          AND (concat('',:taskId,'') = '' OR EXISTS (
                              SELECT 1
                              FROM crm.email_communication_tasks ect1
                              WHERE ect1.email_communication_id = ec.id
                              AND ect1.task_id in (:taskId)
                              AND ect1.status = 'ACTIVE'))
                          AND (concat('',:activityId,'') = '' OR EXISTS (
                              SELECT 1
                              FROM crm.email_communication_activity eca
                              JOIN activity.activity act ON eca.activity_id = act.id
                              WHERE eca.email_communication_id = ec.id
                              AND act.activity_id in (:activityId)
                              AND eca.status = 'ACTIVE'))
                          AND (:communicationTopicId IS NULL OR ec.communication_topic_id in (:communicationTopicId))
                          AND (:communicationStatus IS NULL OR ec.communication_status::text in (:communicationStatus))
                          AND (:prompt IS NULL OR (
                              :searchBy = 'ALL'
                              AND :prompt IS NOT NULL
                              AND (
                                  ecc.id::text =  replace(:prompt,'%','')
                                  OR ec.id::text = replace(:prompt,'%','')
                                  OR LOWER(cc.contact_type_name) LIKE :prompt
                                  OR LOWER(ec.dms_number) LIKE :prompt
                                  OR LOWER(ec.email_subject) LIKE :prompt                                  
                                  OR LOWER(CONCAT(cd.name, c.identifier, c.customer_number)) LIKE :prompt
                                  OR EXISTS (
                                      SELECT 1
                                      FROM crm.email_communication_customer_contacts eccc
                                      WHERE eccc.email_communication_customer_id = ecc.id
                                      AND LOWER(eccc.email_address) LIKE :prompt)
                              )
                          OR (
                              (:searchBy = 'COMMUNICATION_ID' AND ecc.id::text = replace(:prompt,'%',''))
                              OR (:searchBy = 'LINKED_MASS_COMMUNICATION_ID' AND ec.id::text = replace(:prompt,'%',''))
                              OR (:searchBy = 'COMMUNICATION_DATA' AND LOWER(cc.contact_type_name) LIKE :prompt)
                              OR (:searchBy = 'CUSTOMER_NAME' AND LOWER(cd.name) LIKE :prompt)
                              OR (:searchBy = 'DMS_NUMBER' AND LOWER(ec.dms_number) LIKE :prompt)
                              OR (:searchBy = 'EMAIL_SUBJECT' AND LOWER(ec.email_subject) LIKE :prompt)                               
                              OR (:searchBy = 'CUSTOMER_IDENTIFIER' AND LOWER(c.identifier) LIKE :prompt)
                              OR (:searchBy = 'CUSTOMER_NUMBER' AND c.customer_number::text LIKE :prompt)
                              OR (:searchBy = 'EMAIL_ADDRESS' AND EXISTS (
                                  SELECT 1
                                  FROM crm.email_communication_customer_contacts eccc
                                  WHERE eccc.email_communication_customer_id = ecc.id
                                  AND LOWER(eccc.email_address) LIKE :prompt)))))
                          )
                          ,
                          mass_email as(
                     SELECT count(1) as cnt,
                    'MASS_EMAIL' as communicationchannel
                          from
                    crm.email_communications ec
                          left join customer.account_managers am on ec.system_user_id = am.user_name
                          where
                          ec.communication_channel = 'MASS_EMAIL'
                          and (:entityStatuses is null or ec.status::text in (:entityStatuses))
                          and (:creatorEmployeeId is null or am.id in (:creatorEmployeeId))
                          and (:senderEmployeeId is null or ec.sender_employee_id in (:senderEmployeeId))
                          and (date(:createDateFrom) is null or date(ec.create_date) >= date(:createDateFrom))
                          and (date(:createDateTo) is null or date(ec.create_date) <= date(:createDateTo))
                      and (concat('',:contactPurposeId,'') = '' or exists (select 1
                                                         from crm.email_communication_contact_purposes eccp
                                                           join nomenclature.contact_purposes cp
                                                           on eccp.contact_purpose_id = cp.id
                                                           and eccp.email_communication_id = ec.id and eccp.status = 'ACTIVE'
                                                          and cp.id in (:contactPurposeId)
                                                          and cp.status = 'ACTIVE'))
                       and (:communicationType is null or ec.communication_type::text in (:communicationType))
                       and (concat('',:taskId,'') = '' or exists (select 1
                                                         from crm.email_communication_tasks ect1
                                                         where ect1.email_communication_id = ec.id and
                                                          ect1.task_id in (:taskId)
                                                          and ect1.status = 'ACTIVE'))
                       and (concat('',:activityId,'') = '' or exists (select 1
                                                         from crm.email_communication_activity eca
                                                          join activity.activity act on eca.activity_id = act.id
                                                          and eca.email_communication_id = ec.id
                                                          and act.activity_id in (:activityId)
                                                          and eca.status = 'ACTIVE'))
                       and (:communicationTopicId is null or ec.communication_topic_id in (:communicationTopicId))
                       and (:communicationStatus is null or ec.communication_status::text in (:communicationStatus))
                      and (:prompt is null or (:searchBy = 'ALL' and :prompt is not null and (
                      exists(select 1
                              from crm.email_communication_customers ecc
                               where ecc.email_communication_id = ec.id and ecc.id::text = replace(:prompt,'%',''))
                       or ec.id::text =  replace(:prompt,'%','')
                       or exists (select 1 from crm.email_communication_customers ecc
                                  join
                                  customer.customer_communications cc
                                   on ecc.customer_communication_id = cc.id
                                  and ecc.email_communication_id = ec.id
                                  and lower(cc.contact_type_name) like :prompt)
                       or exists(select 1 from crm.email_communication_customers ecc
                                  join customer.customer_details cd
                                  on ecc.customer_detail_id = cd.id
                                  and ecc.email_communication_id = ec.id
                                  join customer.customers c on cd.customer_id = c.id
                                  and lower(concat(cd.name,c.identifier,c.customer_number)) like :prompt
                                  )
                       or exists (select 1 from crm.email_communication_customers ecc
                                  join
                                  crm.email_communication_customer_contacts eccc
                                   on eccc.email_communication_customer_id = ecc.id
                                  and ecc.email_communication_id = ec.id
                                  and lower(eccc.email_address) like :prompt)
                       )
                        or((:searchBy = 'COMMUNICATION_ID' and exists (select 1
                              from crm.email_communication_customers ecc
                               where ecc.email_communication_id = ec.id and ecc.id::text =  replace(:prompt,'%','')))
                       -- or(:searchBy = 'LINKED_MASS_COMMUNICATION_ID' and ec.id::text = :prompt)
                        or(:searchBy = 'DMS_NUMBER' AND LOWER(ec.dms_number) LIKE :prompt)
                        or(:searchBy = 'EMAIL_SUBJECT' AND LOWER(ec.email_subject) LIKE :prompt)                               
                        or(:searchBy = 'COMMUNICATION_DATA' and exists (select 1 from crm.email_communication_customers ecc
                                  join customer.customer_communications cc on ecc.customer_communication_id = cc.id
                                  and ecc.email_communication_id = ec.id and lower(cc.contact_type_name) like :prompt))
                        or(:searchBy = 'CUSTOMER_NAME' and exists ( select 1 from crm.email_communication_customers ecc
                                                                     join customer.customer_details cd on ecc.customer_detail_id = cd.id
                                                                     and ecc.email_communication_id = ec.id
                                                                     and lower(cd.name) like :prompt))
                        or(:searchBy = 'CUSTOMER_IDENTIFIER' and exists ( select 1 from crm.email_communication_customers ecc
                                                                     join customer.customer_details cd on ecc.customer_detail_id = cd.id
                                                                     and ecc.email_communication_id = ec.id
                                                                     join customer.customers c
                                                                     on cd.customer_id = c.id
                                                                     and lower(c.identifier) like :prompt))
                        or(:searchBy = 'CUSTOMER_NUMBER' and exists ( select 1 from crm.email_communication_customers ecc
                                                                     join customer.customer_details cd on ecc.customer_detail_id = cd.id
                                                                     and ecc.email_communication_id = ec.id
                                                                     join customer.customers c
                                                                     on cd.customer_id = c.id
                                                                     and c.customer_number::text like :prompt))
                        or(:searchBy = 'EMAIL_ADDRESS' and exists (select 1 from crm.email_communication_customers ecc
                                  join
                                  crm.email_communication_customer_contacts eccc
                                   on eccc.email_communication_customer_id = ecc.id
                                  and ecc.email_communication_id = ec.id
                                  and lower(eccc.email_address) like :prompt)))))
                                  )
                    select sum(cnt) as record_count from (
                    select cnt,communicationChannel from ind_email
                     union all
                    select cnt,communicationChannel from mass_email  ) as email
                     where ((:kindOfCommunication) is null or text(email.communicationChannel)  in (:kindOfCommunication))
                    """
    )
    Page<EmailCommunicationListingMiddleResponse> filter(
            @Param("creatorEmployeeId")
            List<Long> creatorEmployeeId,
            @Param("senderEmployeeId")
            List<Long> senderEmployeeId,
            @Param("createDateFrom")
            LocalDateTime createDateFrom,
            @Param("createDateTo")
            LocalDateTime createDateTo,
            @Param("contactPurposeId")
            List<Long> contactPurposeId,
            @Param("communicationType")
            List<String> communicationType,
            @Param("activityId")
            List<Long> activityId,
            @Param("taskId")
            List<Long> taskId,
            @Param("communicationTopicId")
            List<Long> communicationTopicId,
            @Param("kindOfCommunication")
            List<String> kindOfCommunication,
            @Param("communicationStatus")
            List<String> communicationStatus,
            @Param("entityStatuses")
            List<String> entityStatuses,
            @Param("prompt")
            String prompt,
            @Param("searchBy")
            String searchBy,
            @Param("activityDirection")
            String activityDirection,
            @Param("contactPurposeDirection")
            String contactPurposeDirection,
            Pageable pageable
    );

    @Query(value = """
            SELECT c.id                  AS CustomerId,
                   cd.id                 AS CustomerDetailId,
                   cam.display_name      AS CreatorUsername,
                   sc.sender_employee_id AS SenderUsername,
                   CURRENT_DATE          AS SystemDATE,
                   c.identifier          AS CustomerIdentifier,
                   CASE
                       WHEN c.customer_type = 'LEGAL_ENTITY' THEN
                           CONCAT(cd.name, ' ', lf.name)
                       ELSE
                           CONCAT(cd.name, ' ', COALESCE(cd.middle_name, ''), ' ', COALESCE(cd.last_name, ''))
                       END               AS CustomerNameComb,
                   c.customer_number     AS CustomerNumber,
                   translation.translate_text(CASE
                                                  WHEN c.customer_type = 'LEGAL_ENTITY' THEN
                                                      CASE
                                                          WHEN cd.foreign_address THEN
                                                              CONCAT(
                                                                      cd.district_foreign, ', ',
                                                                      CASE
                                                                          WHEN cd.residential_area_foreign IS NOT NULL THEN
                                                                              COALESCE(cd.foreign_residential_area_type::TEXT, '') || ' ' ||
                                                                              COALESCE(cd.residential_area_foreign, '')
                                                                          ELSE
                                                                              ''
                                                                          END,
                                                                      CASE
                                                                          WHEN cd.street_foreign IS NOT NULL THEN
                                                                              COALESCE(cd.foreign_street_type::TEXT, '') || ' ' ||
                                                                              COALESCE(cd.street_foreign, '')
                                                                          ELSE
                                                                              ''
                                                                          END,
                                                                      cd.street_number, ', бл. ',
                                                                      cd.block, ', вх. ',
                                                                      cd.entrance, ', ет. ',
                                                                      cd.floor, ', ап. ',
                                                                      cd.apartment, ', ',
                                                                      cd.address_additional_info
                                                              )
                                                          ELSE
                                                              CONCAT(district.name, ', ',
                                                                     CASE
                                                                         WHEN residentialarea.name IS NOT NULL THEN
                                                                             COALESCE(residentialarea.type::TEXT, '') || ' ' ||
                                                                             residentialarea.name
                                                                         ELSE
                                                                             ''
                                                                         END, ', ',
                                                                     CASE
                                                                         WHEN street.name IS NOT NULL THEN
                                                                             COALESCE(street.type::TEXT, '') || ' ' || street.name
                                                                         ELSE
                                                                             ''
                                                                         END, ', ',
                                                                     COALESCE(cd.street_number, ''), ', бл. ',
                                                                     COALESCE(cd.block, ''), ', вх. ',
                                                                     COALESCE(cd.entrance, ''), ', ет. ',
                                                                     COALESCE(cd.floor, ''), ', ап. ',
                                                                     COALESCE(cd.apartment, ''), ', ',
                                                                     COALESCE(cd.address_additional_info, '')
                                                              )
                                                          END
                       END             ,text('BULGARIAN'))  AS AddressHeadquarterComb,
            
                   CASE
                       WHEN c.customer_type = 'LEGAL_ENTITY' THEN
                           CASE
                               WHEN cd.foreign_address THEN cd.populated_place_foreign
                               ELSE place.name
                               END
                       END               AS AddressHeadquarterPopulatedPlace,
            
                   CASE
                       WHEN c.customer_type = 'LEGAL_ENTITY' THEN
                           CASE
                               WHEN cd.foreign_address THEN cd.zip_code_foreign
                               ELSE zip.zip_code
                               END
                       END               AS AddressHeadquarterZIP,
            
                   translation.translate_text(CASE
                                                  WHEN c.customer_type = 'PRIVATE_CUSTOMER' THEN
                                                      CASE
                                                          WHEN cd.foreign_address THEN
                                                              CONCAT(
                                                                      cd.district_foreign, ', ',
                                                                      CASE
                                                                          WHEN cd.residential_area_foreign IS NOT NULL THEN
                                                                              COALESCE(cd.foreign_residential_area_type::TEXT, '') || ' ' ||
                                                                              COALESCE(cd.residential_area_foreign, '')
                                                                          ELSE
                                                                              ''
                                                                          END,
                                                                      CASE
                                                                          WHEN cd.street_foreign IS NOT NULL THEN
                                                                              COALESCE(cd.foreign_street_type::TEXT, '') || ' ' ||
                                                                              COALESCE(cd.street_foreign, '')
                                                                          ELSE
                                                                              ''
                                                                          END,
                                                                      cd.street_number, ', бл. ',
                                                                      cd.block, ', вх. ',
                                                                      cd.entrance, ', ет. ',
                                                                      cd.floor, ', ап. ',
                                                                      cd.apartment, ', ',
                                                                      cd.address_additional_info
                                                              )
                                                          ELSE
                                                              CONCAT(district.name, ', ',
                                                                     CASE
                                                                         WHEN residentialarea.name IS NOT NULL THEN
                                                                             COALESCE(residentialarea.type::TEXT, '') || ' ' ||
                                                                             residentialarea.name
                                                                         ELSE
                                                                             ''
                                                                         END, ', ',
                                                                     CASE
                                                                         WHEN street.name IS NOT NULL THEN
                                                                             COALESCE(street.type::TEXT, '') || ' ' || street.name
                                                                         ELSE
                                                                             ''
                                                                         END, ', ',
                                                                     COALESCE(cd.street_number, ''), ', бл. ',
                                                                     COALESCE(cd.block, ''), ', вх. ',
                                                                     COALESCE(cd.entrance, ''), ', ет. ',
                                                                     COALESCE(cd.floor, ''), ', ап. ',
                                                                     COALESCE(cd.apartment, ''), ', ',
                                                                     COALESCE(cd.address_additional_info, ''))
                                                          END
                       END             ,text('BULGARIAN'))  AS AddressComb,
            
                   CASE
                       WHEN c.customer_type = 'PRIVATE_CUSTOMER' THEN
                           CASE
                               WHEN cd.foreign_address THEN cd.populated_place_foreign
                               ELSE place.name
                               END
                       END               AS AddressPopulatedPlace,
            
                   CASE
                       WHEN c.customer_type = 'PRIVATE_CUSTOMER' THEN
                           CASE
                               WHEN cd.foreign_address THEN cd.zip_code_foreign
                               ELSE zip.zip_code
                               END
                       END               AS AddressZIP
            FROM crm.sms_communication_customers scc
                     LEFT JOIN crm.sms_communications sc ON sc.id = scc.sms_communication_id
                     LEFT JOIN customer.account_managers cam ON cam.user_name = sc.system_user_id
                     LEFT JOIN customer.customer_details cd ON cd.id = scc.customer_detail_id
                     LEFT JOIN customer.customers c ON c.id = cd.customer_id
                     LEFT JOIN nomenclature.legal_forms lf ON lf.id = cd.legal_form_id
                     LEFT JOIN nomenclature.districts district ON district.id = cd.district_id
                     LEFT JOIN nomenclature.residential_areas residentialarea ON residentialarea.id = cd.residential_area_id
                     LEFT JOIN nomenclature.streets street ON street.id = cd.street_id
                     LEFT JOIN nomenclature.populated_places place ON place.id = cd.populated_place_id
                     LEFT JOIN nomenclature.zip_codes zip ON zip.id = cd.zip_code_id
            WHERE scc.id = :smsCommunicationCustomerId
              and c.status = 'ACTIVE'
              and sc.status = 'ACTIVE'
              and sc.communication_type = 'OUTGOING'
            """,
            nativeQuery = true
    )
    EmailAndSmsDocumentModel.CustomerAdditionalInfoProjection fetchCustomerAdditionalInfoFromSms(Long smsCommunicationCustomerId);

    @Query(value = """
            SELECT c.id                  AS CustomerId,
                   cd.id                 AS CustomerDetailId,
                   cam.display_name      AS CreatorUsername,
                   ec.sender_employee_id AS SenderUsername,
                   ec.dms_number         AS ArchivingNumber,
                   CURRENT_DATE          AS SystemDATE,
                   c.identifier          AS CustomerIdentifier,
                   CASE
                       WHEN c.customer_type = 'LEGAL_ENTITY' THEN
                           CONCAT(cd.name, ' ', COALESCE(lf.name, ''))
                       ELSE
                           CONCAT(cd.name, ' ', COALESCE(cd.middle_name, ''), ' ', COALESCE(cd.last_name, ''))
                       END               AS CustomerNameComb,
                   c.customer_number     AS CustomerNumber,
            
                   translation.translate_text(CASE
                                                  WHEN c.customer_type = 'LEGAL_ENTITY' THEN
                                                      CASE
                                                          WHEN cd.foreign_address THEN
                                                              CONCAT(
                                                                      cd.district_foreign, ', ',
                                                                      CASE
                                                                          WHEN cd.residential_area_foreign IS NOT NULL THEN
                                                                              COALESCE(cd.foreign_residential_area_type::TEXT, '') || ' ' ||
                                                                              COALESCE(cd.residential_area_foreign, '')
                                                                          ELSE
                                                                              ''
                                                                          END,
                                                                      CASE
                                                                          WHEN cd.street_foreign IS NOT NULL THEN
                                                                              COALESCE(cd.foreign_street_type::TEXT, '') || ' ' ||
                                                                              COALESCE(cd.street_foreign, '')
                                                                          ELSE
                                                                              ''
                                                                          END,
                                                                      cd.street_number, ', бл. ',
                                                                      cd.block, ', вх. ',
                                                                      cd.entrance, ', ет. ',
                                                                      cd.floor, ', ап. ',
                                                                      cd.apartment, ', ',
                                                                      cd.address_additional_info
                                                              )
                                                          ELSE
                                                              CONCAT(COALESCE(district.name, ''), ', ',
                                                                     CASE
                                                                         WHEN residentialarea.name IS NOT NULL THEN
                                                                             COALESCE(residentialarea.type::TEXT, '') || ' ' ||
                                                                             COALESCE(residentialarea.name, '')
                                                                         ELSE
                                                                             ''
                                                                         END, ', ',
                                                                     CASE
                                                                         WHEN street.name IS NOT NULL THEN
                                                                             COALESCE(street.type::TEXT, '') || ' ' || COALESCE(street.name, '')
                                                                         ELSE
                                                                             ''
                                                                         END, ', ',
                                                                     COALESCE(cd.street_number, ''), ', бл. ',
                                                                     COALESCE(cd.block, ''), ', вх. ',
                                                                     COALESCE(cd.entrance, ''), ', ет. ',
                                                                     COALESCE(cd.floor, ''), ', ап. ',
                                                                     COALESCE(cd.apartment, ''), ', ',
                                                                     COALESCE(cd.address_additional_info, '')
                                                              )
                                                          END
                       END            ,text('BULGARIAN'))   AS AddressHeadquarterComb,
            
                   CASE
                       WHEN c.customer_type = 'LEGAL_ENTITY' THEN
                           CASE
                               WHEN cd.foreign_address THEN cd.populated_place_foreign
                               ELSE place.name
                               END
                       END               AS AddressHeadquarterPopulatedPlace,
            
                   CASE
                       WHEN c.customer_type = 'LEGAL_ENTITY' THEN
                           CASE
                               WHEN cd.foreign_address THEN cd.zip_code_foreign
                               ELSE zip.zip_code
                               END
                       END               AS AddressHeadquarterZIP,
            
                   translation.translate_text(CASE
                                                  WHEN c.customer_type = 'PRIVATE_CUSTOMER' THEN
                                                      CASE
                                                          WHEN cd.foreign_address THEN
                                                              CONCAT(
                                                                      cd.district_foreign, ', ',
                                                                      CASE
                                                                          WHEN cd.residential_area_foreign IS NOT NULL THEN
                                                                              COALESCE(cd.foreign_residential_area_type::TEXT, '') || ' ' ||
                                                                              COALESCE(cd.residential_area_foreign, '')
                                                                          ELSE
                                                                              ''
                                                                          END,
                                                                      CASE
                                                                          WHEN cd.street_foreign IS NOT NULL THEN
                                                                              COALESCE(cd.foreign_street_type::TEXT, '') || ' ' ||
                                                                              COALESCE(cd.street_foreign, '')
                                                                          ELSE
                                                                              ''
                                                                          END,
                                                                      cd.street_number, ', бл. ',
                                                                      cd.block, ', вх. ',
                                                                      cd.entrance, ', ет. ',
                                                                      cd.floor, ', ап. ',
                                                                      cd.apartment, ', ',
                                                                      cd.address_additional_info
                                                              )
                                                          ELSE
                                                              CONCAT(COALESCE(district.name, ''), ', ',
                                                                     CASE
                                                                         WHEN residentialarea.name IS NOT NULL THEN
                                                                             COALESCE(residentialarea.type::TEXT, '') || ' ' ||
                                                                             COALESCE(residentialarea.name, '')
                                                                         ELSE
                                                                             ''
                                                                         END, ', ',
                                                                     CASE
                                                                         WHEN street.name IS NOT NULL THEN
                                                                             COALESCE(street.type::TEXT, '') || ' ' || COALESCE(street.name, '')
                                                                         ELSE
                                                                             ''
                                                                         END, ', ',
                                                                     COALESCE(cd.street_number, ''), ', бл. ',
                                                                     COALESCE(cd.block, ''), ', вх. ',
                                                                     COALESCE(cd.entrance, ''), ', ет. ',
                                                                     COALESCE(cd.floor, ''), ', ап. ',
                                                                     COALESCE(cd.apartment, ''), ', ',
                                                                     COALESCE(cd.address_additional_info, ''))
                                                          END
                       END , text('BULGARIAN')) AS AddressComb,
            
                   CASE
                       WHEN c.customer_type = 'PRIVATE_CUSTOMER' THEN
                           CASE
                               WHEN cd.foreign_address THEN cd.populated_place_foreign
                               ELSE place.name
                               END
                       END               AS AddressPopulatedPlace,
            
                   CASE
                       WHEN c.customer_type = 'PRIVATE_CUSTOMER' THEN
                           CASE
                               WHEN cd.foreign_address THEN cd.zip_code_foreign
                               ELSE zip.zip_code
                               END
                       END               AS AddressZIP
            FROM crm.email_communication_customers ecc
                     LEFT JOIN crm.email_communications ec ON ec.id = ecc.email_communication_id
                     LEFT JOIN customer.account_managers cam ON cam.user_name = ec.system_user_id
                     LEFT JOIN customer.customer_details cd ON cd.id = COALESCE(ecc.customer_detail_id, ecc.customer_detail_id)
                     LEFT JOIN customer.customers c ON c.id = cd.customer_id
                     LEFT JOIN nomenclature.legal_forms lf ON lf.id = cd.legal_form_id
                     LEFT JOIN nomenclature.districts district ON district.id = cd.district_id
                     LEFT JOIN nomenclature.residential_areas residentialarea ON residentialarea.id = cd.residential_area_id
                     LEFT JOIN nomenclature.streets street ON street.id = cd.street_id
                     LEFT JOIN nomenclature.populated_places place ON place.id = cd.populated_place_id
                     LEFT JOIN nomenclature.zip_codes zip ON zip.id = cd.zip_code_id
            WHERE ecc.id = :emailCommunicationCustomerId
              and c.status = 'ACTIVE'
              and ec.status = 'ACTIVE'
              and ec.communication_type = 'OUTGOING'
            """,
            nativeQuery = true
    )
    EmailAndSmsDocumentModel.CustomerAdditionalInfoProjection fetchCustomerAdditionalInfoFromEmail(Long emailCommunicationCustomerId);

    @Query(
            value = """
                    SELECT JSON_AGG(
                                   JSON_BUILD_OBJECT(
                                           'title', title.name, -- Join the titles table directly
                                           'name', cm.name,
                                           'surname', cm.surname,
                                           'jobPosition', cm.job_position
                                   )
                           ) AS Managers
                    FROM customer.customer_managers cm
                             LEFT JOIN nomenclature.titles title ON title.id = cm.title_id
                    WHERE cm.customer_detail_id = :customerDetailId
                    GROUP BY cm.customer_detail_id
                    """,
            nativeQuery = true
    )
    Optional<String> findManagersByCustomerDetailId(Long customerDetailId);

    @Query(value = """
                    SELECT JSON_AGG(
                                   JSON_BUILD_OBJECT(
                                           'invoiceNumber', invoice.invoice_number,
                                           'invoiceDate', invoice.invoice_date,
                                           'dueDate', liability.due_date,
                                           'initialAmount', liability.initial_amount,
                                           'currentAmount', liability.current_amount,
                                           'invoicePeriodFrom', invoice.meter_reading_period_from,
                                           'invoicePeriodTo', invoice.meter_reading_period_to
                                   )
                           )                             AS Liabilities,
                           SUM(liability.current_amount) AS SumLiabilities,
                           SUM(
                                   CASE
                                       WHEN liability.due_date < CURRENT_DATE THEN liability.current_amount
                                       ELSE 0
                                       END
                           )                             AS SumOverdueLiabilities
                    FROM receivable.customer_liabilities liability
                             LEFT JOIN invoice.invoices invoice ON invoice.id = liability.invoice_id
                    WHERE liability.customer_id = :customerId
                      AND liability.status = 'ACTIVE'
                      AND invoice.status = 'REAL'
                      AND liability.current_amount > 0
                    GROUP BY liability.customer_id
            """,
            nativeQuery = true
    )
    Map<String, Object> findLiabilityDataBasedOnCustomer(Long customerId);

    @Query(value = """
            SELECT contract.contract_number                AS ContractNumber,
                   contract.signing_date                   AS ContractDate,
                   productDetails.name                     AS ContractProductName,
                   null                                    AS ContractServiceName,
                   contract.contract_status::TEXT          AS ContractStatus,
                   contract.activation_date                AS ContractActivationDate,
                   contract.termination_date               AS ContractTerminationDate,
                   contract.contract_term_end_date         AS ContractTerminationEndDate,
                   contract.contract_term_end_date + 1     AS ContractTerminationEndDate1,
                   translation.translate_text(contractDetails.payment_guarantee::TEXT,text('BULGARIAN')) AS ContractPaymentGuaranteeType,
                   contractDetails.bank_guarantee_amount   AS ContractPaymentGuaranteeBankAmount,
                   bankCurrency.name                       AS ContractPaymentGuaranteeBankCurrency,
                   contractDetails.cash_deposit_amount     AS ContractPaymentGuaranteeCashDepositAmount,
                   cashCurrency.name                       AS ContractPaymentGuaranteeCashDepositCurrency,
                   CASE
                       WHEN term.contract_delivery_activation_value IS NULL AND term.contract_delivery_activation_type IS NULL
                           THEN NULL
                       ELSE CONCAT(term.contract_delivery_activation_value, ' ', term.contract_delivery_activation_type::TEXT)
                       END                                 AS ContractTermForActivation,
                   CASE
                       WHEN contractDetails.type = 'EX_OFFICIO_AGREEMENT'
                           THEN contractDetails.start_date::date
                       END                                 AS ContractVersionActivationDatePriceChange,
                   CASE
                       WHEN contractDetails.type = 'EX_OFFICIO_AGREEMENT'
                           THEN (contractDetails.start_date - INTERVAL '30 days')::date
                       END                                 AS ContractVersionActivationDatePriceChange30,
                   CASE
                       WHEN contractDetails.type = 'EX_OFFICIO_AGREEMENT'
                           THEN (contractDetails.start_date - INTERVAL '45 days')::date
                       END                                 AS ContractVersionActivationDatePriceChange45
            FROM product_contract.contracts contract
                     LEFT JOIN product_contract.contract_details contractDetails
                               ON contract.id = contractDetails.contract_id
            
                     LEFT JOIN product.product_details productDetails
                               ON productDetails.id = contractDetails.product_detail_id
            
                     LEFT JOIN terms.terms term
                               ON productDetails.term_id = term.id
            
                     LEFT JOIN nomenclature.currencies bankCurrency
                               ON bankCurrency.id = contractDetails.bank_guarantee_currency_id
            
                     LEFT JOIN nomenclature.currencies cashCurrency
                               ON cashCurrency.id = contractDetails.cash_deposit_currency_id
            WHERE contract.contract_number = (:contractNumber)
              AND contractDetails.id = (:contractDetailId)
            
            UNION
            
            SELECT contract.contract_number                AS ContractNumber,
                   contract.signing_date                   AS ContractDate,
                   null                                    AS ContractProductName,
                   serviceDetails.name                     AS ContractServiceName,
                   contract.contract_status::TEXT          AS ContractStatus,
                   null                                    AS ContractActivationDate,
                   contract.termination_date               AS ContractTerminationDate,
                   contract.contract_term_end_date         AS ContractTerminationEndDate,
                   contract.contract_term_end_date + 1     AS ContractTerminationEndDate1,
                   translation.translate_text(contractDetails.payment_guarantee::TEXT,text('BULGARIAN')) AS ContractPaymentGuaranteeType,
                   contractDetails.bank_guarantee_amount   AS ContractPaymentGuaranteeBankAmount,
                   bankCurrency.name                       AS ContractPaymentGuaranteeBankCurrency,
                   contractDetails.cash_deposit_amount     AS ContractPaymentGuaranteeCashDepositAmount,
                   cashCurrency.name                       AS ContractPaymentGuaranteeCashDepositCurrency,
                   CASE
                       WHEN term.contract_delivery_activation_value IS NULL AND term.contract_delivery_activation_type IS NULL
                           THEN NULL
                       ELSE CONCAT(term.contract_delivery_activation_value, ' ', term.contract_delivery_activation_type::TEXT)
                       END                                 AS ContractTermForActivation,
                   CASE
                       WHEN contractDetails.type = 'EX_OFFICIO_AGREEMENT'
                           THEN contractDetails.start_date::date
                       END                                 AS ContractVersionActivationDatePriceChange,
                   CASE
                       WHEN contractDetails.type = 'EX_OFFICIO_AGREEMENT'
                           THEN (contractDetails.start_date - INTERVAL '30 days')::date
                       END                                 AS ContractVersionActivationDatePriceChange30,
                   CASE
                       WHEN contractDetails.type = 'EX_OFFICIO_AGREEMENT'
                           THEN (contractDetails.start_date - INTERVAL '45 days')::date
                       END                                 AS ContractVersionActivationDatePriceChange45
            FROM service_contract.contracts contract
                     LEFT JOIN service_contract.contract_details contractDetails
                               ON contract.id = contractDetails.contract_id
            
                     LEFT JOIN service.service_details serviceDetails
                               ON serviceDetails.id = contractDetails.service_detail_id
            
                     LEFT JOIN terms.terms term
                               ON serviceDetails.term_id = term.id
            
                     LEFT JOIN nomenclature.currencies bankCurrency
                               ON bankCurrency.id = contractDetails.bank_guarantee_currency_id
            
                     LEFT JOIN nomenclature.currencies cashCurrency
                               ON cashCurrency.id = contractDetails.cash_deposit_currency_id
            
            WHERE contract.contract_number = (:contractNumber)
              AND contractDetails.id = (:contractDetailId)
            """,
            nativeQuery = true
    )
    EmailAndSmsDocumentModel.ContractsInfoProjection fetchContractInfoByContractDetailIdAndContractNumber(
            Long contractDetailId,
            String contractNumber
    );

    @Query(
            value = """
                     WITH podsInfo AS (SELECT pod.id                      AS pod_id,
                           pod.identifier              AS additionalID,
                           podDetails.name             as podDetailsName,
                           CASE
                               WHEN podDetails.foreign_address THEN
                                   CONCAT(
                                           podDetails.district_foreign, ', ',
                                           podDetails.foreign_residential_area_type, ' ',
                                           podDetails.residential_area_foreign, ', ',
                                           podDetails.foreign_street_type, ' ',
                                           podDetails.street_foreign,
                                           podDetails.street_number, ', бл. ',
                                           podDetails.block, ', вх. ',
                                           podDetails.entrance, ', ет. ',
                                           podDetails.floor, ', ап. ',
                                           podDetails.apartment, ', ',
                                           podDetails.address_additional_info, ''
                                   )
                               ELSE
                                   CONCAT(
                                           COALESCE(district.name, ''), ', ',
                                           COALESCE(residentalArea.type::TEXT, ''), ' ',
                                           COALESCE(residentalArea.name, ''), ', ',
                                           COALESCE(street.type::TEXT, ''), ' ',
                                           COALESCE(street.name, ''), ', ',
                                           COALESCE(podDetails.street_number, ''), ', бл. ',
                                           COALESCE(podDetails.block, ''), ', вх. ',
                                           COALESCE(podDetails.entrance, ''), ', ет. ',
                                           COALESCE(podDetails.floor, ''), ', ап. ',
                                           COALESCE(podDetails.apartment, ''), ', ',
                                           COALESCE(podDetails.address_additional_info, '')
                                   )
                               END                     AS addressComb,
                           CASE
                               WHEN podDetails.foreign_address THEN
                                   podDetails.populated_place_foreign
                               ELSE
                                   populatedPlace.name
                               END                     AS place,
                           CASE
                               WHEN podDetails.foreign_address THEN
                                   podDetails.zip_code_foreign
                               ELSE
                                   zipCode.zip_code
                               END                     AS zip,
                           podDetails.type             as podDetailsType,
                           podDetails.provided_power   as providedPower,
                           translation.translate_text(text(podDetails.measurement_type),text('BULGARIAN')) as measurementType
                    FROM product_contract.contract_pods contractPod
                             LEFT JOIN pod.pod_details podDetails
                                       ON podDetails.id = contractPod.pod_detail_id
                             LEFT JOIN pod.pod pod
                                       ON pod.id = podDetails.pod_id
                             LEFT JOIN nomenclature.populated_places populatedPlace
                                       ON populatedPlace.id = podDetails.populated_place_id
                             LEFT JOIN nomenclature.zip_codes zipCode
                                       ON zipCode.id = podDetails.zip_code_id
                             LEFT JOIN nomenclature.districts district
                                       ON district.id = podDetails.district_id
                             LEFT JOIN nomenclature.residential_areas residentalArea
                                       ON residentalArea.id = podDetails.residential_area_id
                             LEFT JOIN nomenclature.streets street
                                       ON street.id = podDetails.street_id
                    WHERE contractPod.contract_detail_id = :contractDetailId
                      AND contractPod.status = 'ACTIVE')
                     SELECT JSON_AGG(
                                    JSON_BUILD_OBJECT(
                                            'id', pod_id,
                                            'additionalID', additionalID,
                                            'name', podDetailsName,
                                            'addressComb', addressComb,
                                            'place', place,
                                            'zip', zip,
                                            'type', podDetailsType,
                                            'providedPower', providedPower,
                                            'measurementType', measurementType
                                    )
                            ) AS contractPods
                     FROM podsInfo
                    """,
            nativeQuery = true
    )
    Optional<String> fetchProductContractPodsByContractDetailId(Long contractDetailId);

    @Query(
            value = """
                    WITH podsInfo AS (SELECT pod.id                      AS pod_id,
                                             pod.identifier              AS additionalID,
                                             podDetails.name             as podDetailsName,
                                             CASE
                                                 WHEN podDetails.foreign_address THEN
                                                     CONCAT(
                                                             podDetails.district_foreign, ', ',
                                                             podDetails.foreign_residential_area_type, ' ',
                                                             podDetails.residential_area_foreign, ', ',
                                                             podDetails.foreign_street_type, ' ',
                                                             podDetails.street_foreign,
                                                             podDetails.street_number, ', бл. ',
                                                             podDetails.block, ', вх. ',
                                                             podDetails.entrance, ', ет. ',
                                                             podDetails.floor, ', ап. ',
                                                             podDetails.apartment, ', ',
                                                             podDetails.address_additional_info, ''
                                                     )
                                                 ELSE
                                                     CONCAT(
                                                             COALESCE(district.name, ''), ', ',
                                                             COALESCE(residentalArea.type::TEXT, ''), ' ',
                                                             COALESCE(residentalArea.name, ''), ', ',
                                                             COALESCE(street.type::TEXT, ''), ' ',
                                                             COALESCE(street.name, ''), ', ',
                                                             COALESCE(podDetails.street_number, ''), ', бл. ',
                                                             COALESCE(podDetails.block, ''), ', вх. ',
                                                             COALESCE(podDetails.entrance, ''), ', ет. ',
                                                             COALESCE(podDetails.floor, ''), ', ап. ',
                                                             COALESCE(podDetails.apartment, ''), ', ',
                                                             COALESCE(podDetails.address_additional_info, '')
                                                     )
                                                 END                     AS addressComb,
                                             CASE
                                                 WHEN podDetails.foreign_address THEN
                                                     podDetails.populated_place_foreign
                                                 ELSE
                                                     populatedPlace.name
                                                 END                     AS place,
                                             CASE
                                                 WHEN podDetails.foreign_address THEN
                                                     podDetails.zip_code_foreign
                                                 ELSE
                                                     zipCode.zip_code
                                                 END                     AS zip,
                                             podDetails.type             as podDetailsType,
                                             podDetails.provided_power   as providedPower,
                                             translation.translate_text(text(podDetails.measurement_type),text('BULGARIAN')) as measurementType
                                      FROM service_contract.contract_pods contractPod
                                               LEFT JOIN pod.pod pod
                                                         ON pod.id = contractPod.pod_id
                                               LEFT JOIN pod.pod_details podDetails
                                                         ON podDetails.id = pod.last_pod_detail_id
                                               LEFT JOIN nomenclature.populated_places populatedPlace
                                                         ON populatedPlace.id = podDetails.populated_place_id
                                               LEFT JOIN nomenclature.zip_codes zipCode
                                                         ON zipCode.id = podDetails.zip_code_id
                                               LEFT JOIN nomenclature.districts district
                                                         ON district.id = podDetails.district_id
                                               LEFT JOIN nomenclature.residential_areas residentalArea
                                                         ON residentalArea.id = podDetails.residential_area_id
                                               LEFT JOIN nomenclature.streets street
                                                         ON street.id = podDetails.street_id
                                      WHERE contractPod.contract_detail_id = :contractDetailId
                                        AND contractPod.status = 'ACTIVE')
                    SELECT JSON_AGG(
                                   JSON_BUILD_OBJECT(
                                           'id', pod_id,
                                           'additionalID', additionalID,
                                           'name', podDetailsName,
                                           'addressComb', addressComb,
                                           'place', place,
                                           'zip', zip,
                                           'type', podDetailsType,
                                           'providedPower', providedPower,
                                           'measurementType', measurementType
                                   )
                           ) AS contractPods
                    FROM podsInfo
                    """,
            nativeQuery = true
    )
    Optional<String> fetchServiceContractPodsByContractId(Long contractDetailId);

    @Query(
            value = """
                    WITH contract_price_component_service AS (SELECT pc.contract_template_tag,
                                                                     pcfv.price_component_id,
                                                                     pcfv.value,
                                                                     pcfv.description,
                                                                     pcfv.formula_variable
                                                              FROM service_contract.contract_details cd
                                                                       join service.service_details sd on cd.service_detail_id = sd.id
                                                                       join service.service_price_components spc on spc.service_detail_id = sd.id
                                                                       join price_component.price_components pc on pc.id = spc.price_component_id
                                                                       join price_component.price_component_formula_variables pcfv
                                                                            on pcfv.price_component_id = pc.id
                                                              WHERE cd.id = :contractDetailId and spc.status = 'ACTIVE')
                    SELECT JSONB_AGG(
                                   JSONB_BUILD_OBJECT(
                                           'tag', cpc.contract_template_tag,
                                           'value', cpc.value,
                                           'description', cpc.description,
                                           'variableName', cpc.formula_variable
                                   )
                           ) AS price_components
                    FROM contract_price_component_service cpc
                    GROUP BY cpc.price_component_id
                    """,
            nativeQuery = true
    )
    List<String> fetchPriceComponentTagsService(Long contractDetailId);

    @Query(
            value = """
                     WITH contract_price_component_product AS (SELECT pc.contract_template_tag,
                                                                      pcfv.price_component_id,
                                                                      pcfv.value,
                                                                      pcfv.description,
                                                                      pcfv.formula_variable
                                                               FROM product_contract.contract_details cd
                                                                        join product.product_details pd on cd.product_detail_id = pd.id
                                                                        join product.product_price_components ppc on ppc.product_detail_id = pd.id
                                                                        join price_component.price_components pc on pc.id = ppc.price_component_id
                                                                        join price_component.price_component_formula_variables pcfv
                                                                             on pcfv.price_component_id = pc.id
                                                               WHERE cd.id = :contractDetailId and ppc.status = 'ACTIVE')
                     SELECT JSONB_AGG(
                                    JSONB_BUILD_OBJECT(
                                            'tag', cpc.contract_template_tag,
                                            'value', cpc.value,
                                            'description', cpc.description,
                                            'variableName', cpc.formula_variable
                                    )
                            ) AS price_components
                     FROM contract_price_component_product cpc
                     GROUP BY cpc.price_component_id
                    """,
            nativeQuery = true
    )
    List<String> fetchPriceComponentTagsProduct(Long contractDetailId);

    @Query(value = """
            WITH contractActions AS (
                SELECT
                    action.id AS actionId,
                    action.execution_date AS lastExecutionDate,
                    termination.contract_clause_number AS terminationContractClauseNumber,
                    penalty.contract_clause_number AS penaltyContractClauseNumber,
                    action.penalty_claim_amount AS penaltyClaimAmount,
                    currency.name AS penaltyClaimCurrency,
                    penaltyLiability.due_date AS penaltyPaymentDueDate,
                    termination.notice_due_value_min AS terminationNoticePeriodMin,
                    termination.notice_due_value_max AS terminationNoticePeriodMax,
                    termination.notice_due_type AS terminationPeriodType
                FROM action.actions action
                         LEFT JOIN product_contract.contracts prodactContract
                                   ON prodactContract.contract_number = :contractNumber
                         LEFT JOIN service_contract.contracts serviceContract
                                   ON serviceContract.contract_number = :contractNumber
                         LEFT JOIN product.terminations termination
                                   ON termination.id = action.termination_id
                         LEFT JOIN terms.penalties penalty
                                   ON penalty.id = action.penalty_id
                         LEFT JOIN nomenclature.currencies currency
                                   ON action.penalty_claim_currency_id = currency.id
                         LEFT JOIN receivable.customer_liabilities penaltyLiability
                                   ON action.id = penaltyLiability.action_id
                WHERE
                    (action.product_contract_id = prodactContract.id OR action.service_contract_id = serviceContract.id)
                  AND action.action_status = 'EXECUTED'
                  AND action.status = 'ACTIVE'
            )
            SELECT
                actionId,
                lastExecutionDate,
                terminationContractClauseNumber,
                penaltyContractClauseNumber,
                penaltyClaimAmount,
                penaltyClaimCurrency,
                penaltyPaymentDueDate,
                terminationNoticePeriodMin,
                terminationNoticePeriodMax,
                terminationPeriodType
            FROM contractActions
            WHERE lastExecutionDate = (
                SELECT MAX(lastExecutionDate)
                FROM contractActions
            )
            ORDER BY actionId DESC
            LIMIT 1
            """,
            nativeQuery = true
    )
    EmailAndSmsDocumentModel.ContractsActionsProjection fetchContractActionsByContractNumber(String contractNumber);

    @Query(
            value = """
                    SELECT JSON_AGG(
                                    JSON_BUILD_OBJECT(
                                            'actionId', action.id,
                                            'podId', pod.id,
                                            'podDetailsId', podDetails.id,
                                            'gridOperatorId', gridOperator.id,
                                            'go', gridOperator.name,
                                            'id', pod.identifier,
                                            'additionalId', podDetails.additional_identifier,
                                            'executionDate', action.execution_date
                                    )
                            ) AS actionTerminationGOPODs
                     FROM action.actions action
                              join nomenclature.action_types actionType on actionType.id = action.action_type_id
                              join action.action_pods actionPods on action.id = actionPods.action_id
                              join pod.pod pod on pod.id = actionPods.pod_id
                              join pod.pod_details podDetails on podDetails.id = pod.last_pod_detail_id
                              join nomenclature.grid_operators gridOperator on gridOperator.id = pod.grid_operator_id
                     where action.id = :actionId
                       and actionType.name = 'POD termination without notice'
                       and action.status = 'ACTIVE'
                       and action.action_status = 'EXECUTED'
                     GROUP BY action.id
                    """,
            nativeQuery = true
    )
    Optional<String> findActionGoPodsByActionId(Long actionId);

    @Query(
            value = """
                            WITH podsAndGrids AS (
                                SELECT
                                    go.id AS gridId,
                                    p.identifier AS podName,
                                    go.name AS gridName,
                                    a.execution_date AS executionDate
                                FROM
                                    action.actions a
                                        JOIN nomenclature.action_types at ON at.id = a.action_type_id
                                        JOIN action.action_pods ap ON a.id = ap.action_id
                                        JOIN pod.pod p ON p.id = ap.pod_id
                                        JOIN pod.pod_details pd ON pd.id = p.last_pod_detail_id
                                        JOIN nomenclature.grid_operators go ON go.id = p.grid_operator_id
                                WHERE
                                        a.id = :actionId
                                  AND at.name = 'POD termination without notice'
                                  AND a.status = 'ACTIVE'
                                  AND a.action_status = 'EXECUTED'
                            )
                            SELECT
                                gridId,
                                STRING_AGG(podName, ', ') AS podNames,
                                gridName,
                                executionDate
                            FROM
                                podsAndGrids
                            GROUP BY
                                gridId, gridName, executionDate
                    """,
            nativeQuery = true
    )
    List<EmailAndSmsDocumentModel.ActionTerminationGOListPODProjection> findActionGoListPodsByActionId(Long actionId);

    @Query("""
            select ec from EmailCommunication ec
            where  ec.communicationChannel = 'MASS_EMAIL'
            and ec.entityStatus = 'ACTIVE'
            and ec.emailCommunicationStatus = 'SENT'
            and ec.creationType = 'MANUAL'
            and ec.emailCommunicationType = 'OUTGOING'
            and ec.docGenerationStatus = 'READY'
            """
    )
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "5000")})
    List<EmailCommunication> findEmailsForBodyGeneration();

}
