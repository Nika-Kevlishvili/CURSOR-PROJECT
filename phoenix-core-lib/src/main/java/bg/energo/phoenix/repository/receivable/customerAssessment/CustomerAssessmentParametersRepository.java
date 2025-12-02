package bg.energo.phoenix.repository.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessmentParameters;
import bg.energo.phoenix.model.response.receivable.customerAssessment.CustomerAssessmentFinalStatusResponse;
import bg.energo.phoenix.model.response.receivable.customerAssessment.CustomerAssessmentMiddleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAssessmentParametersRepository extends JpaRepository<CustomerAssessmentParameters, Long> {

    List<CustomerAssessmentParameters> findByCustomerAssessmentId(Long customerAssessmentId);

    Optional<CustomerAssessmentParameters> findByCustomerAssessmentIdAndCustomerAssessmentCriteriaId(Long customerAssessmentId, Long customerAssessmentCriteriaId);

    @Query(nativeQuery = true,
            value = """
                    SELECT
                                           assessment.conditions,
                                           assessment.value,
                                           assessment.customer_assessment_criteria_id as customerAssessmentCriteriaId,
                                           assessment.is_value as isValue,
                                           assessment.value_from as valueFrom,
                                           assessment.value_to as valueTo,
                                           assessment.customer_type as customerType,
                                           COALESCE(
                                               CASE
                                                   WHEN assessment.is_value = false THEN
                                                       CASE
                                                           WHEN assessment.value ~ '^[0-9]+$' AND CAST(assessment.value AS numeric) BETWEEN COALESCE(assessment.value_from, 0) AND COALESCE(assessment.value_to, 999999999) THEN true
                                                           ELSE false
                                                       END
                                                   WHEN assessment.is_value = true THEN
                                                       CASE assessment.value
                                                           WHEN 'true' THEN true
                                                           WHEN 'false' THEN false
                                                           ELSE NULL
                                                       END
                                                   ELSE false
                                               END,
                                               false
                                           ) assessment
                                       FROM
                                           (
                                               SELECT
                                                   cac.name conditions,
                                                   CASE cac.criteria_name
                                                       WHEN 'REMINDERS' THEN
                                                           CAST((
                                                               SELECT COUNT(DISTINCT psdr.id)
                                                               FROM receivable.power_supply_disconnection_reminders psdr
                                                               JOIN receivable.power_supply_disconnection_reminder_customers psdrc ON psdrc.power_supply_disconnection_reminder_id = psdr.id
                                                                   AND psdrc.customer_id = :customerId
                                                                   AND psdr.create_date BETWEEN current_date - INTERVAL '1 year' AND current_date
                                                                   AND psdr.reminder_status = 'EXECUTED'
                                                                   AND psdr.status = 'ACTIVE'
                                                           ) AS text)
                                                       WHEN 'REQUESTS_FOR_DISCONNECTION_OF_THE_POWER_SUPPLY' THEN
                                                           CAST((
                                                               SELECT COUNT(DISTINCT psdr.id)
                                                               FROM receivable.power_supply_disconnection_requests psdr
                                                               JOIN receivable.power_supply_disconnection_request_pods psdrp ON psdrp.power_supply_disconnection_request_id = psdr.id
                                                                   AND psdrp.customer_id = :customerId
                                                                   AND psdr.disconnection_request_status = 'EXECUTED'
                                                                   AND psdr.status = 'ACTIVE'
                                                                   AND psdr.create_date > current_date - INTERVAL '1 year'
                                                                   AND NOT EXISTS (
                                                                       SELECT *
                                                                       FROM receivable.power_supply_dcn_cancellations psdc
                                                                       JOIN receivable.power_supply_dcn_cancellation_pods psdcp ON psdcp.power_supply_dcn_cancellation_id = psdc.id
                                                                           AND psdc.cancellation_status = 'EXECUTED'
                                                                           AND psdc.status = 'ACTIVE'
                                                                           AND psdcp.customer_id = psdrp.customer_id
                                                                           AND psdcp.pod_id = psdrp.pod_id
                                                                   )
                                                           ) AS text)
                                                       WHEN 'RESCHEDULING_AGREEMENTS_FOR_THE_LAST_TWELVE_MONTHS' THEN
                                                           CAST((
                                                               SELECT COUNT(*)
                                                               FROM (
                                                                   SELECT
                                                                       rp.rescheduling_id,
                                                                       MIN(rp.due_date) AS first_installment_due_date,
                                                                       MAX(rp.due_date) AS last_installment_due_date
                                                                   FROM receivable.reschedulings r
                                                                   JOIN receivable.rescheduling_plans rp ON rp.rescheduling_id = r.id
                                                                   WHERE r.customer_id = :customerId
                                                                       AND r.rescheduling_status = 'EXECUTED'
                                                                       AND r.status = 'ACTIVE'
                                                                   GROUP BY rp.rescheduling_id
                                                               ) AS res
                                                               WHERE res.first_installment_due_date >= current_date - INTERVAL '1 year'
                                                                   AND res.last_installment_due_date <= current_date
                                                           ) AS text)
                                                       WHEN 'ACTIVE_RESCHEDULING_AGREEMENT' THEN
                                                           CAST((
                                                               SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END
                                                               FROM (
                                                                   SELECT
                                                                       rp.rescheduling_id,
                                                                       MAX(rp.due_date) AS last_installment_due_date
                                                                   FROM receivable.reschedulings r
                                                                   JOIN receivable.rescheduling_plans rp ON rp.rescheduling_id = r.id
                                                                   WHERE r.customer_id = :customerId
                                                                       AND r.rescheduling_status = 'EXECUTED'
                                                                       AND r.status = 'ACTIVE'
                                                                   GROUP BY rp.rescheduling_id
                                                               ) AS res
                                                               WHERE res.last_installment_due_date > current_date
                                                           ) AS text)
                                                       WHEN 'ACTIVE_REQUEST_FOR_DISCONNECTION' THEN
                                                           CAST((
                                                               SELECT COUNT(DISTINCT psdr.id)
                                                               FROM receivable.power_supply_disconnection_requests psdr
                                                               JOIN receivable.power_supply_disconnection_request_pods psdrp ON psdrp.power_supply_disconnection_request_id = psdr.id
                                                                   AND psdrp.customer_id = :customerId
                                                                   AND psdr.disconnection_request_status = 'EXECUTED'
                                                                   AND psdr.status = 'ACTIVE'
                                                                   AND NOT EXISTS (
                                                                       SELECT *
                                                                       FROM receivable.power_supply_dcn_cancellations psdc
                                                                       JOIN receivable.power_supply_dcn_cancellation_pods psdcp ON psdcp.power_supply_dcn_cancellation_id = psdc.id
                                                                           AND psdc.cancellation_status = 'EXECUTED'
                                                                           AND psdc.status = 'ACTIVE'
                                                                           AND psdcp.customer_id = psdrp.customer_id
                                                                           AND psdcp.pod_id = psdrp.pod_id
                                                                   )
                                                                   AND NOT EXISTS (
                                                                       SELECT *
                                                                       FROM receivable.power_supply_reconnections psr
                                                                       JOIN receivable.power_supply_reconnection_pods psrp ON psrp.power_supply_reconnection_id = psr.id
                                                                           AND psr.reconnection_status = 'EXECUTED'
                                                                           AND psr.status = 'ACTIVE'
                                                                           AND psrp.customer_id = psdrp.customer_id
                                                                           AND psrp.pod_id = psdrp.pod_id
                                                                   )
                                                           ) AS text)
                                                       ELSE 'false'
                                                   END value,
                                                   cac.id AS customer_assessment_criteria_id,
                                                   cac.criteria_name AS criteria,
                                                   cac.value_from,
                                                   cac.value_to,
                                                   (SELECT c.customer_type FROM customer.customers c WHERE id = :customerId) AS customer_type,
                                                   cac.value AS is_value
                                               FROM nomenclature.customer_assessment_criterias cac
                                               WHERE status = 'ACTIVE'
                                               ORDER BY ordering_id
                                           ) AS assessment
                    """)
    List<CustomerAssessmentMiddleResponse> getParametersByCustomerId(
            @Param("customerId") Long customerId
    );

    @Query(nativeQuery = true,
            value = """
                    SELECT
                                           assessment.conditions,
                                           assessment.value,
                                           assessment.customer_assessment_criteria_id as customerAssessmentCriteriaId,
                                           assessment.is_value as isValue,
                                           assessment.value_from as valueFrom,
                                           assessment.value_to as valueTo,
                                           assessment.customer_type as customerType,
                                           COALESCE(
                                                                  CASE
                                                                      WHEN assessment.is_value = false THEN
                                                                          CASE
                                                                              WHEN assessment.value ~ '^[0-9]+$' AND CAST(assessment.value AS numeric) BETWEEN COALESCE(assessment.value_from, 0) AND COALESCE(assessment.value_to, 999999999) THEN true
                                                                              ELSE false
                                                                          END
                                                                      WHEN assessment.is_value = true THEN
                                                                          CASE assessment.value
                                                                              WHEN 'true' THEN true
                                                                              WHEN 'false' THEN false
                                                                              ELSE NULL
                                                                          END
                                                                      ELSE assessment.assessment
                                                                  END,
                                                                  false
                                                              ) assessment,
                                           assessment.final_assessment as finalAssessment,
                                           assessment.id as customerAssessmentParametersId,
                                           assessment.customer_assessment_id as customerAssessmentId
                                       FROM
                                           (
                                               SELECT
                                                   cac.name conditions,
                                                   CASE cac.criteria_name
                                                       WHEN 'REMINDERS' THEN
                                                           CAST((
                                                               SELECT COUNT(DISTINCT psdr.id)
                                                               FROM receivable.power_supply_disconnection_reminders psdr
                                                               JOIN receivable.power_supply_disconnection_reminder_customers psdrc ON psdrc.power_supply_disconnection_reminder_id = psdr.id
                                                                   AND psdrc.customer_id = :customerId
                                                                   AND psdr.create_date BETWEEN current_date - INTERVAL '1 year' AND current_date
                                                                   AND psdr.reminder_status = 'EXECUTED'
                                                                   AND psdr.status = 'ACTIVE'
                                                           ) AS text)
                                                       WHEN 'REQUESTS_FOR_DISCONNECTION_OF_THE_POWER_SUPPLY' THEN
                                                           CAST((
                                                               SELECT COUNT(DISTINCT psdr.id)
                                                               FROM receivable.power_supply_disconnection_requests psdr
                                                               JOIN receivable.power_supply_disconnection_request_pods psdrp ON psdrp.power_supply_disconnection_request_id = psdr.id
                                                                   AND psdrp.customer_id = :customerId
                                                                   AND psdr.disconnection_request_status = 'EXECUTED'
                                                                   AND psdr.status = 'ACTIVE'
                                                                   AND psdr.create_date > current_date - INTERVAL '1 year'
                                                                   AND NOT EXISTS (
                                                                       SELECT *
                                                                       FROM receivable.power_supply_dcn_cancellations psdc
                                                                       JOIN receivable.power_supply_dcn_cancellation_pods psdcp ON psdcp.power_supply_dcn_cancellation_id = psdc.id
                                                                           AND psdc.cancellation_status = 'EXECUTED'
                                                                           AND psdc.status = 'ACTIVE'
                                                                           AND psdcp.customer_id = psdrp.customer_id
                                                                           AND psdcp.pod_id = psdrp.pod_id
                                                                   )
                                                           ) AS text)
                                                       WHEN 'RESCHEDULING_AGREEMENTS_FOR_THE_LAST_TWELVE_MONTHS' THEN
                                                           CAST((
                                                               SELECT COUNT(*)
                                                               FROM (
                                                                   SELECT
                                                                       rp.rescheduling_id,
                                                                       MIN(rp.due_date) AS first_installment_due_date,
                                                                       MAX(rp.due_date) AS last_installment_due_date
                                                                   FROM receivable.reschedulings r
                                                                   JOIN receivable.rescheduling_plans rp ON rp.rescheduling_id = r.id
                                                                   WHERE r.customer_id = :customerId
                                                                       AND r.rescheduling_status = 'EXECUTED'
                                                                       AND r.status = 'ACTIVE'
                                                                   GROUP BY rp.rescheduling_id
                                                               ) AS res
                                                               WHERE res.first_installment_due_date >= current_date - INTERVAL '1 year'
                                                                   AND res.last_installment_due_date <= current_date
                                                           ) AS text)
                                                       WHEN 'ACTIVE_REQUEST_FOR_DISCONNECTION' THEN
                                                           CAST((
                                                               SELECT COUNT(DISTINCT psdr.id)
                                                               FROM receivable.power_supply_disconnection_requests psdr
                                                               JOIN receivable.power_supply_disconnection_request_pods psdrp ON psdrp.power_supply_disconnection_request_id = psdr.id
                                                                   AND psdrp.customer_id = :customerId
                                                                   AND psdr.disconnection_request_status = 'EXECUTED'
                                                                   AND psdr.status = 'ACTIVE'
                                                                   AND NOT EXISTS (
                                                                       SELECT *
                                                                       FROM receivable.power_supply_dcn_cancellations psdc
                                                                       JOIN receivable.power_supply_dcn_cancellation_pods psdcp ON psdcp.power_supply_dcn_cancellation_id = psdc.id
                                                                           AND psdc.cancellation_status = 'EXECUTED'
                                                                           AND psdc.status = 'ACTIVE'
                                                                           AND psdcp.customer_id = psdrp.customer_id
                                                                           AND psdcp.pod_id = psdrp.pod_id
                                                                   )
                                                                   AND NOT EXISTS (
                                                                       SELECT *
                                                                       FROM receivable.power_supply_reconnections psr
                                                                       JOIN receivable.power_supply_reconnection_pods psrp ON psrp.power_supply_reconnection_id = psr.id
                                                                           AND psr.reconnection_status = 'EXECUTED'
                                                                           AND psr.status = 'ACTIVE'
                                                                           AND psrp.customer_id = psdrp.customer_id
                                                                           AND psrp.pod_id = psdrp.pod_id
                                                                   )
                                                           ) AS text)
                                                       ELSE cap.value
                                                   END value,
                                                   cac.id AS customer_assessment_criteria_id,
                                                   cac.criteria_name AS criteria,
                                                   cac.value_from,
                                                   cac.value_to,
                                                   (SELECT c.customer_type FROM customer.customers c WHERE id = :customerId) AS customer_type,
                                                   cac.value AS is_value,
                                                   cap.final_assessment,
                                                   cap.assessment,
                                                   cap.id,
                                                   cap.customer_assessment_id
                                               FROM nomenclature.customer_assessment_criterias cac
                                               JOIN receivable.customer_assessment_parameters cap ON cap.customer_assessment_criteria_id = cac.id
                                               JOIN receivable.customer_assessments ca ON cap.customer_assessment_id = ca.id
                                                   AND ca.id = :customerAssessmentId
                                                   AND ca.assessment_status = 'DRAFT'
                                                   AND ca.status = 'ACTIVE'
                                               ORDER BY ordering_id
                                           ) AS assessment
                    """
    )
    List<CustomerAssessmentMiddleResponse> getParametersForDraft(
            @Param("customerId") Long customerId,
            @Param("customerAssessmentId") Long customerAssessmentId
    );

    @Query(nativeQuery = true,
    value = """
            select
            cac.name as conditions,
            cap.value,
            cap.assessment,
            cap.final_assessment as finalAssessment,
            cap.id as customerAssessmentParametersId,
            cap.customer_assessment_criteria_id as customerAssessmentCriteriaId,
            ca.id as customerAssessmentId
            from
            receivable.customer_assessments ca
            join
            receivable.customer_assessment_parameters cap
            on
            cap.customer_assessment_id = ca.id
            and ca.id = :customerAssessmentId
            and ca.assessment_status = 'FINAL'
            join
            nomenclature.customer_assessment_criterias cac
            on cap.customer_assessment_criteria_id = cac.id
            """)
    List<CustomerAssessmentFinalStatusResponse> getParametersForFinal(
            @Param("customerAssessmentId") Long customerAssessmentId
    );
}
