package bg.energo.phoenix.repository.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.ReasonForCancellation;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ReasonForCancellationRepository extends JpaRepository<ReasonForCancellation, Long> {
    @Query("""
                select max(r.orderingId) from ReasonForCancellation r
            """)
    Long lastOrderingId();

    @Query("""
                    select case when count(r) > 0 then true else false end
                        from ReasonForCancellation r
                        where lower(r.name)=lower(:name)
                        and r.status in :statuses
            """)
    boolean existsReasonForDisconnectionWithNameAndStatus(String name, List<NomenclatureItemStatus> statuses);

    Optional<ReasonForCancellation> findByDefaultSelectionTrue();

    @Query("""
                    select r from ReasonForCancellation r
                    where r.id=:id and r.status in :statuses
            """)
    Optional<ReasonForCancellation> findByIdAndStatuses(Long id, List<NomenclatureItemStatus> statuses);

    @Query("""
                    select r from ReasonForCancellation r
                    where (:prompt is null or lower(r.name) like :prompt)
                    and (r.status in :statuses)
                    and (:excludedItemId is null or r.id <> :excludedItemId)
                    order by r.defaultSelection desc, r.orderingId asc
            """)
    Page<ReasonForCancellation> filter(String prompt, List<NomenclatureItemStatus> statuses, Long excludedItemId, Pageable pageable);

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(r.id, r.name, r.orderingId, r.defaultSelection, r.status) " +
                    "from ReasonForCancellation as r" +
                    " where (:prompt is null or lower(r.name) like :prompt)" +
                    " and (:excludedItemId is null or r.id <> :excludedItemId) " +
                    " and (r.status in (:statuses))" +
                    " order by r.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select r from ReasonForCancellation as r" +
                    " where r.id <> :currentId " +
                    " and (r.orderingId >= :start and r.orderingId <= :end) "
    )
    List<ReasonForCancellation> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query("""
                    select r from ReasonForCancellation r
                    where r.orderingId is not null
                    order by r.name
            """)
    List<ReasonForCancellation> orderByName();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query("""
                    select r.id
                    from ReasonForCancellation r
                    where r.id in :ids
                    and r.status in :statuses
            """)
    Set<Long> findByIdsIn(Collection<Long> ids, List<NomenclatureItemStatus> statuses);

    @Query(value = """
            select count(cr.id) > 0 as is_used
            from nomenclature.cancelation_reasons cr
            where cr.id = :id
            and (
                exists
                (
                     select 1
                     from receivable.power_supply_dcn_cancellation_pods psdcp
                     join receivable.power_supply_dcn_cancellations psdc on psdc.id = psdcp.power_supply_dcn_cancellation_id
                     where psdcp.cancellation_reason_id = cr.id
                     and psdc.status = 'ACTIVE'
                )
            or
                exists(
                    select 1
                    from receivable.power_supply_reconnection_pods psrp
                    join receivable.power_supply_reconnections psr on psr.id = psrp.power_supply_reconnection_id
                    where psrp.cancelation_reason_id = cr.id
                    and psr.status = 'ACTIVE'
                )
            )
            """, nativeQuery = true)
    boolean hasActiveConnections(Long id);
}
