package bg.energo.phoenix.repository.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicity;
import bg.energo.phoenix.model.response.billing.processPeriodicity.ProcessPeriodicityListingMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessPeriodicityRepository extends JpaRepository<ProcessPeriodicity, Long> {
    Optional<ProcessPeriodicity> findByIdAndStatus(Long id, EntityStatus status);


    @Query("""
           select pp from BillingProcessPeriodicity bpp
           join ProcessPeriodicity pp on bpp.processPeriodicityId=pp.id
           where bpp.billingId=:billingId
           and pp.processPeriodicityType ='ONE_TIME'
           and bpp.status='ACTIVE'
           and ((pp.processPeriodicityBillingProcessStart = 'DATE_AND_TIME' and cast(pp.billingProcessStartDate as date )=current_date )
                    or ( pp.processPeriodicityBillingProcessStart='MANUAL' or pp.processPeriodicityBillingProcessStart='AFTER_PROCESS' or pp.processPeriodicityBillingProcessStart='INSTANT'))
           order by pp.createDate desc
            """)
    List<ProcessPeriodicity> findAllByBillingRunForToday(Long billingId);

    List<ProcessPeriodicity> findByStartAfterProcessBillingIdAndIdAndStatus(Long billingRunId, Long processPeriodicityId, EntityStatus status);

    boolean existsByIdAndStatus(Long id, EntityStatus status);

    @Query("""
            select count(1) from ProcessPeriodicity pp 
            where pp.id = :id
            and 
            exists (select 1 from BillingProcessPeriodicity bpp
                    where bpp.processPeriodicityId = pp.id)
            """)
    Long activeConnectionCount(@Param("id") Long id);

    @Query(
            nativeQuery = true,
            value =
                    """
                            select
                                 pp.name as name,
                                 pp.type as periodicity,
                                 pp.create_date as createDate,
                                 pp.id as id,
                                 pp.status as status
                                 from billing.process_periodicity pp
                                 where (:periodicity is null or text(pp.type) in :periodicity)
                                 and text(pp.status) in :statuses
                                 and (:prompt is null or (:searchBy = 'ALL' and (
                                                                     lower(pp.name) like lower(:prompt)
                                                                      or
                                                                     lower(text(pp.type)) like lower(:prompt)
                                                                 )
                                                             )
                                                             or (
                                                                 (:searchBy = 'NAME' and lower(pp.name) like lower(:prompt))
                                                                  or
                                                                 (:searchBy = 'PERIODICITY' and lower(text(pp.type)) like lower(:prompt))
                                                             )
                                                         )
                                                        """,
            countQuery = """
                                select
                                    count(1)
                                     from billing.process_periodicity pp
                                     where (:periodicity is null or text(pp.type) in :periodicity)
                                     and text(pp.status) in :statuses
                                     and (:prompt is null or (:searchBy = 'ALL' and (
                                                                         lower(pp.name) like lower(:prompt)
                                                                          or
                                                                         lower(text(pp.type)) like lower(:prompt)
                                                                     )
                                                                 )
                                                                 or (
                                                                     (:searchBy = 'NAME' and lower(pp.name) like lower(:prompt))
                                                                      or
                                                                     (:searchBy = 'PERIODICITY' and lower(text(pp.type)) like lower(:prompt))
                                                                 )
                                                             )
                    """
    )
    Page<ProcessPeriodicityListingMiddleResponse> filter(
            @Param("prompt") String prompt,
            @Param("periodicity") List<String> periodicity,
            @Param("searchBy") String searchBy,
            @Param("statuses") List<String> statuses,
            Pageable pageable
    );

    @Query(value = """
            select count(1) > 0
            from billing.billings b
                     join billing.billing_process_periodicity bpp on bpp.billing_id = b.id
                     join billing.process_periodicity pp on pp.id = bpp.process_periodicity_id
            where b.id = :billingRunId
              and bpp.status = 'ACTIVE'
              and pp.start_after_process_billing_id in (select b2.id
                                                        from billing.billings b2
                                                                 join billing.billing_process_periodicity bpp2 on bpp2.billing_id = b2.id
                                                        where bpp2.process_periodicity_id = :processPeriodicityId
                                                          and bpp2.status = 'ACTIVE')
            
            """,nativeQuery = true)
    boolean validateCircularDependency(Long billingRunId,Long processPeriodicityId);
}
