package bg.energo.phoenix.repository.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessment;
import bg.energo.phoenix.model.response.receivable.customerAssessment.CustomerAssessmentListingMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAssessmentRepository extends JpaRepository<CustomerAssessment, Long> {

    @Query(value = "select nextval('receivable.customer_assessments_id_seq')", nativeQuery = true)
    Long getNextSequenceValue();

    Optional<CustomerAssessment> findByIdAndStatus(Long id, EntityStatus status);

    @Query(
            nativeQuery = true,
            value = """
                    select ca.assessment_number  as assessmentNumber,
                           ca.assessment_status  as assessmentStatus,
                           cat.name              as type,
                           date(ca.create_date)  as creationDate,
                           case
                               when c.customer_type = 'PRIVATE_CUSTOMER' then
                                   concat(
                                           c.identifier, ' (',
                                           cd.name,
                                           case when cd.middle_name is not null then concat(' ', cd.middle_name) else '' end,
                                           case when cd.last_name is not null then concat(' ', cd.last_name) else '' end,
                                           ')'
                                   )
                               when c.customer_type = 'LEGAL_ENTITY' then
                                   concat(c.identifier, ' (', cd.name, ' ', lf.name, ')')
                               end               as customer,
                           r.rescheduling_number as reschedulingAgreement,
                           ca.final_assessment   as finalAssessment,
                           ca.status             as status,
                           ca.id                 as id
                    from receivable.customer_assessments ca
                             join nomenclature.customer_assessment_types cat on ca.customer_assessment_type_id = cat.id
                             join customer.customers c on ca.customer_id = c.id
                             join customer.customer_details cd on c.last_customer_detail_id = cd.id
                             left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                             left join receivable.reschedulings r on r.customer_assessment_id = ca.id
                        and r.status <> 'DELETED' and r.rescheduling_status = 'EXECUTED'
                    where ((:assessmentStatus) is null or text(ca.assessment_status) in :assessmentStatus)
                      and ((:status) is null or text(ca.status) in :status)
                      and (date(:createDateFrom) is null or date(ca.create_date) >= date(:createDateFrom))
                      and (date(:createDateTo) is null or date(ca.create_date) <= date(:createDateTo))
                      and ((:finalAssessment) is null or text(ca.final_assessment) in :finalAssessment)
                      and ((:assessmentType) is null or ca.customer_assessment_type_id in :assessmentType)
                      and (:prompt is null or (:searchBy = 'ALL' and (
                        lower(ca.assessment_number) like :prompt or
                        c.identifier like :prompt or
                        lower(r.rescheduling_number) like :prompt)
                        ) or ((:searchBy = 'ASSESSMENT_NUMBER' and lower(ca.assessment_number) like :prompt)
                        or (:searchBy = 'CUSTOMER' and c.identifier like :prompt)
                        or (:searchBy = 'RESCHEDULING_AGREEMENT' and lower(r.rescheduling_number) like :prompt)))
                    """,
            countQuery = """
                    select count(1)
                    from receivable.customer_assessments ca
                             join nomenclature.customer_assessment_types cat on ca.customer_assessment_type_id = cat.id
                             join customer.customers c on ca.customer_id = c.id
                             join customer.customer_details cd on c.last_customer_detail_id = cd.id
                             left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                             left join receivable.reschedulings r on r.customer_assessment_id = ca.id
                        and r.status <> 'DELETED' and r.rescheduling_status = 'EXECUTED'
                    where ((:assessmentStatus) is null or text(ca.assessment_status) in :assessmentStatus)
                      and ((:status) is null or text(ca.status) in :status)
                      and (date(:createDateFrom) is null or date(ca.create_date) >= date(:createDateFrom))
                      and (date(:createDateTo) is null or date(ca.create_date) <= date(:createDateTo))
                      and ((:finalAssessment) is null or text(ca.final_assessment) in :finalAssessment)
                      and ((:assessmentType) is null or ca.customer_assessment_type_id in :assessmentType)
                      and (:prompt is null or (:searchBy = 'ALL' and (
                        lower(ca.assessment_number) like :prompt or
                        c.identifier like :prompt or
                        lower(r.rescheduling_number) like :prompt)
                        ) or ((:searchBy = 'ASSESSMENT_NUMBER' and lower(ca.assessment_number) like :prompt)
                        or (:searchBy = 'CUSTOMER' and c.identifier like :prompt)
                        or (:searchBy = 'RESCHEDULING_AGREEMENT' and lower(r.rescheduling_number) like :prompt)))
                    """
    )
    Page<CustomerAssessmentListingMiddleResponse> filter(
            @Param("prompt") String prompt,
            @Param("assessmentStatus") List<String> assessmentStatus,
            @Param("status") List<String> status,
            @Param("createDateFrom") LocalDate createDateFrom,
            @Param("createDateTo") LocalDate createDateTo,
            @Param("finalAssessment") List<String> finalAssessment,
            @Param("assessmentType") List<Long> assessmentType,
            @Param("searchBy") String searchBy,
            Pageable pageable
    );

    @Query("""
            SELECT ca
            FROM CustomerAssessment ca
            WHERE ca.customerId = :customerId
              AND NOT EXISTS (
                    SELECT 1
                    FROM Rescheduling r
                    WHERE r.customerAssessmentId = ca.id
                      AND r.reschedulingStatus = 'EXECUTED'
                )
            """)
    List<CustomerAssessment> getCustomerAssessmentsForRescheduling(
            @Param("customerId") Long customerId
    );

    List<CustomerAssessment> findAllByIdInAndStatusIn(Collection<Long> id, Collection<EntityStatus> status);

    @Query("""
            select ca
            from CustomerAssessment ca
            where ca.customerId = :customerId
            and (:prompt is null or lower(ca.assessmentNumber) like :prompt)
            and ca.status = 'ACTIVE'
            """)
    List<CustomerAssessment> getCustomerAssessmentsByCustomerId(
            @Param("customerId") Long customerId,
            @Param("prompt") String prompt
    );

}
