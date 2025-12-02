package bg.energo.phoenix.repository.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.ActionType;
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
public interface ActionTypeRepository extends JpaRepository<ActionType, Long> {


    @Query(
            value = """
                    select count(at.id) > 0 from ActionType at
                        where at.status in (:statuses)
                        and at.name = :name
                        and (:id is null or at.id <> :id)
                    """
    )
    boolean existsByStatusInAndNameAndId(
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("name") String name,
            @Param("id") Long id
    );


    @Query(value = "select max(at.orderingId) from ActionType at")
    Long findLastOrderingId();


    Optional<ActionType> findByDefaultSelectionTrue();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                        at.id,
                        at.name
                    )
                    from ActionType at
                    where at.id in (:ids)
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);


    @Query(
            value = """
                    select at from ActionType at
                        where at.orderingId is not null
                        order by at.name
                    """
    )
    List<ActionType> orderByName();


    @Query(
            value = """
                    select at from ActionType at
                        where at.id <> :currentId
                        and (at.orderingId >= :start and at.orderingId <= :end)
                    """
    )
    List<ActionType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );


    @Query(
            value = """
                    select at from ActionType at
                        where (:prompt is null or lower(at.name) like :prompt)
                        and (
                            (at.status in (:statuses) and (:excludedItemId is null or at.id <> :excludedItemId))
                            or at.id in (:includedItemIds)
                        )
                        order by
                            case when at.id in (:includedItemIds) then 1 else 2 end,
                            case when at.isHardCoded = true then 0 when at.defaultSelection = true then 1 else 2 END,
                        at.orderingId asc
                    """,
            countQuery = """
                    select at.id from ActionType at
                        where (:prompt is null or lower(at.name) like :prompt)
                        and (
                            (at.status in (:statuses) and (:excludedItemId is null or at.id <> :excludedItemId))
                            or at.id in (:includedItemIds)
                        )
                    """
    )
    Page<ActionType> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(
                        at.id,
                        at.name,
                        at.orderingId,
                        at.defaultSelection,
                        at.status
                    )
                    from ActionType at
                    where (:prompt is null or lower(at.name) like :prompt)
                    and (:excludedItemId is null or at.id <> :excludedItemId)
                    and at.status in (:statuses)
                    order by at.orderingId asc
                    """
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );


    Optional<ActionType> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query("""
            select count(a.id) > 0
            from Action a
            join ActionType at on a.actionTypeId = at.id
            where a.status = 'ACTIVE'
            and at.id = :id
            """)
    boolean hasActiveConnectionsToAction(Long id);

    List<ActionType> findAllByIdInAndStatus(List<Long> actionTypesIds,NomenclatureItemStatus status);
}
