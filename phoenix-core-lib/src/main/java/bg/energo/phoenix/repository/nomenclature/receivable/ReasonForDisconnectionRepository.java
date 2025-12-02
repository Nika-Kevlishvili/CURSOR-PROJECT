package bg.energo.phoenix.repository.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.ReasonForDisconnection;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReasonForDisconnectionRepository extends JpaRepository<ReasonForDisconnection,Long> {


    @Query("""
        select case when count(r) > 0 then true else false end
            from ReasonForDisconnection r
            where lower(r.name)=lower(:name)
            and r.status in :statuses
""")
    boolean existsReasonForDisconnectionWithNameAndStatus(String name, List<NomenclatureItemStatus> statuses);
    @Query("""
        select max(r.orderingId) from ReasonForDisconnection r
    """)
    Long lastOrderingId();

    Optional<ReasonForDisconnection> findByDefaultSelectionTrue();

    @Query("""
        select r from ReasonForDisconnection r
        where r.id=:id and r.status in :statuses
""")
    Optional<ReasonForDisconnection> findByIdAndStatuses(Long id,List<NomenclatureItemStatus> statuses);

    @Query("""
        select r from ReasonForDisconnection r
        where (:prompt is null or lower(r.name) like :prompt)
        and (r.status in :statuses)
        and (:excludedItemId is null or r.id <> :excludedItemId)
        order by r.defaultSelection desc, r.orderingId asc
""")
    Page<ReasonForDisconnection> filter(String prompt, List<NomenclatureItemStatus> statuses, Long excludedItemId,Pageable pageable);

    @Query("""
        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                           r.id,
                            r.name
                        )
        from ReasonForDisconnection r
        where r.id in :ids
""")
    List<ActivityNomenclatureResponse> findByIdsIn(List<Long> ids);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query("""
        select r from ReasonForDisconnection r
        where r.orderingId is not null
        order by r.name
""")
    List<ReasonForDisconnection> orderByName();


    @Query(
            "select r from ReasonForDisconnection as r" +
                    " where r.id <> :currentId " +
                    " and (r.orderingId >= :start and r.orderingId <= :end) "
    )
    List<ReasonForDisconnection> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );


    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(r.id, r.name, r.orderingId, r.defaultSelection, r.status) " +
                    "from ReasonForDisconnection as r" +
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
    @Query("""
            select count(r.id) > 0
            from DisconnectionPowerSupplyRequests r
            where r.status = 'ACTIVE'
            and r.disconnectionReasonId = :id
            """)
    boolean isConnectedToObj(@Param("id") Long id);
}
