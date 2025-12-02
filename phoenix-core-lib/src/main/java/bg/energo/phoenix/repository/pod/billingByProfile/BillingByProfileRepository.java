package bg.energo.phoenix.repository.pod.billingByProfile;

import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingByProfile;
import bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileStatus;
import bg.energo.phoenix.model.response.pod.billingByProfile.BillingByProfileListResponse;
import bg.energo.phoenix.model.response.pod.billingByProfile.BillingByProfilePreviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingByProfileRepository extends JpaRepository<BillingByProfile, Long> {

    @Query("""
                select bp
                from BillingByProfile as bp
                where bp.id =:id
                and bp.status =:status
            """)
    Optional<BillingByProfile> findByIdAndStatus(
            @Param("id") Long id,
            @Param("status") BillingByProfileStatus status
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.pod.billingByProfile.BillingByProfilePreviewResponse(
                        bbp,
                        pod,
                        p
                    )
                    from BillingByProfile bbp
                    join PointOfDelivery pod on bbp.podId = pod.id
                    join Profiles p on bbp.profileId = p.id
                        where bbp.id = :id
                        and bbp.status in :statuses
                    """
    )
    Optional<BillingByProfilePreviewResponse> previewByIdAndStatusIn(
            @Param("id") Long id,
            @Param("statuses") List<BillingByProfileStatus> statuses
    );


    @Query(
            nativeQuery = true,
            value = """
                    select
                        tbl.id,
                        tbl.podIdentifier,
                        tbl.periodFrom,
                        tbl.periodTo,
                        tbl.profileName,
                        tbl.periodType,
                        tbl.invoiced,
                        tbl.status,
                        tbl.createDate
                    from (
                    select
                        bbp.id as id,
                        p.identifier as podIdentifier,
                        bbp.period_from as periodFrom,
                        bbp.period_to as periodTo,
                        pr.name as profileName,
                        bbp.period_type as periodType,
                        case when bbp.invoiced then 'YES' else 'NO'end  as invoiced ,
                        bbp.status,
                        date(bbp.create_date) as createDate,
                        bbp.create_date as createDateSort
                    from pod.billing_by_profile bbp
                    join pod.pod p on bbp.pod_id = p.id
                    join nomenclature.profiles pr on bbp.profile_id = pr.id
                        where text(bbp.status) in (:statuses)
                        and (:periodType is null or text(bbp.period_type) = :periodType)
                        and (cast(:dateFromStartingWith as date) is null or bbp.period_from >= :dateFromStartingWith)
                        and (cast(:dateFromEndingWith as date) is null or bbp.period_from <= :dateFromEndingWith)
                        and (cast(:dateToStartingWith as date) is null or bbp.period_to >= :dateToStartingWith)
                        and (cast(:dateToEndingWith as date) is null or bbp.period_to <= :dateToEndingWith)
                        and ((:gridOperatorIds) is null or p.grid_operator_id in (:gridOperatorIds))
                        and ((:profileIds) is null or bbp.profile_id in (:profileIds))
                        and (:prompt is null
                            or (:searchBy = 'ALL' and (lower(p.identifier) like :prompt or text(bbp.id) like :prompt))
                            or (
                                (:searchBy = 'POD_IDENTIFIER' and Lower(p.identifier) like :prompt)
                                or (:searchBy = 'BBP_ID' and text(bbp.id) like :prompt)
                            )
                        )
                    ) as tbl
                    where :invoiced is null or invoiced = :invoiced
                    """,
            countQuery = """
                    select count(1) from (
                    select
                        bbp.id as id,
                        p.identifier as podIdentifier,
                        bbp.period_from as periodFrom,
                        bbp.period_to as periodTo,
                        pr.name as profileName,
                        bbp.period_type as periodType,
                        case when bbp.invoiced then 'YES' else 'NO'end  as invoiced,
                        bbp.status,
                        bbp.create_date as createDate
                    from pod.billing_by_profile bbp
                    join pod.pod p on bbp.pod_id = p.id
                    join nomenclature.profiles pr on bbp.profile_id = pr.id
                        where text(bbp.status) in (:statuses)
                        and (:periodType is null or text(bbp.period_type) = :periodType)
                        and (cast(:dateFromStartingWith as date) is null or bbp.period_from >= :dateFromStartingWith)
                        and (cast(:dateFromEndingWith as date) is null or bbp.period_from <= :dateFromEndingWith)
                        and (cast(:dateToStartingWith as date) is null or bbp.period_to >= :dateToStartingWith)
                        and (cast(:dateToEndingWith as date) is null or bbp.period_to <= :dateToEndingWith)
                        and ((:gridOperatorIds) is null or p.grid_operator_id in (:gridOperatorIds))
                        and ((:profileIds) is null or bbp.profile_id in (:profileIds))
                        and (:prompt is null
                            or (:searchBy = 'ALL' and (lower(p.identifier) like :prompt or text(bbp.id) like :prompt))
                            or (
                                (:searchBy = 'POD_IDENTIFIER' and Lower(p.identifier) like :prompt)
                                or (:searchBy = 'BBP_ID' and text(bbp.id) like :prompt)
                            )
                        )
                    ) as tbl
                    where :invoiced is null or invoiced = :invoiced
                    """
    )
    Page<BillingByProfileListResponse> list(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("statuses") List<String> statuses,
            @Param("gridOperatorIds") List<Long> gridOperatorIds,
            @Param("profileIds") List<Long> profileIds,
            @Param("dateFromStartingWith") LocalDate dateFromStartingWith,
            @Param("dateFromEndingWith") LocalDate dateFromEndingWith,
            @Param("dateToStartingWith") LocalDate dateToStartingWith,
            @Param("dateToEndingWith") LocalDate dateToEndingWith,
            @Param("periodType") String periodType,
            @Param("invoiced") String invoiced,
            Pageable pageable
    );


    @Query(
            value = """
                    select count(bbp.id) > 0 from BillingByProfile bbp
                        where bbp.podId = :podId
                        and bbp.profileId = :profileId
                        and (bbp.periodFrom <= :periodTo and bbp.periodTo >= :periodFrom)
                        and bbp.status = 'ACTIVE'
                    """
    )
    boolean isBillingOverlappingForProfileAndPodInPeriod(
            @Param("podId") Long podId,
            @Param("profileId") Long profileId,
            @Param("periodFrom") LocalDateTime periodFrom,
            @Param("periodTo") LocalDateTime periodTo
    );

    @Query("""
            select bbp
            from BillingByProfile bbp
            where bbp.podId = :pointOfDeliveryId
            and (bbp.periodFrom >= :periodFrom and bbp.periodTo <= :periodTo)
            and bbp.status = 'ACTIVE'
            """)
    List<BillingByProfile> findBillingByProfileByPeriodIntersectionForPointOfDelivery(@Param("pointOfDeliveryId") Long pointOfDeliveryId,
                                                                                      @Param("periodFrom") LocalDateTime periodFrom,
                                                                                      @Param("periodTo") LocalDateTime periodTo);

    @Query("""
            select b
            from BillingByProfile b
            where b.podId = :podId
            and b.profileId = :profileId
            and b.periodFrom = :periodFrom
            and b.periodTo = :periodTo
            and b.status = 'ACTIVE'
            """)
    List<BillingByProfile> findByPodIdAndProfileIdAndPeriodFromAndPeriodTo(Long podId,
                                                                           Long profileId,
                                                                           LocalDateTime periodFrom,
                                                                           LocalDateTime periodTo);

    boolean existsByProfileIdAndStatusIn(Long profileId, Collection<BillingByProfileStatus> statuses);
}
