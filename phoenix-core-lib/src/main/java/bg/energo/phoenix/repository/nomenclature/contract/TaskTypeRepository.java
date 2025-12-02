package bg.energo.phoenix.repository.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.TaskType;
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
public interface TaskTypeRepository extends JpaRepository<TaskType, Long> {
    @Query(value = """
            select count(tt.id) from TaskType tt
            where tt.status in(:statuses)
            and tt.name like(:name)
            """)
    Long countByStatusAndName(List<NomenclatureItemStatus> statuses, String name);

    @Query(value = """
            select max(tt.orderingId) from TaskType tt
            """)
    Long findLastOrderingId();

    @Query(value = """
            select tt from TaskType tt
            where tt.isDefault = true
            """)
    Optional<TaskType> findCurrentDefaultSelection();

    @Query("""
            select tt from TaskType tt
            where (:prompt is null or (lower(tt.name) like (lower(:prompt))))
            and (:excludedItemId is null or (tt.id <> :excludedItemId))
            and (tt.status in(:statuses))
            order by tt.isDefault desc, tt.orderingId
            """)
    Page<TaskType> filter(@Param("prompt") String prompt, @Param("statuses") List<NomenclatureItemStatus> statuses, @Param("excludedItemId") Long excludedItemId, Pageable pageRequest);

    @Query("""
            select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(tt.id, tt.name, tt.orderingId, tt.isDefault, tt.status) from TaskType tt
            where (:prompt is null or (lower(tt.name) like (lower(:prompt))))
            and (:excludedItemId is null or (tt.id <> :excludedItemId))
            and (tt.status in(:statuses))
            order by tt.isDefault desc, tt.orderingId
            """)
    Page<NomenclatureResponse> filterNomenclature(@Param("prompt") String prompt, @Param("statuses") List<NomenclatureItemStatus> statuses, @Param("excludedItemId") Long excludedItemId, Pageable pageRequest);

    @Query("""
            select tt from TaskType tt
            where tt.id <> :currentId
            and (tt.orderingId >= :start and tt.orderingId <= :end)
            """)
    List<TaskType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query("""
            select tt from TaskType tt
            where tt.orderingId is not null
            order by tt.name
            """)
    List<TaskType> orderByName();

    @Query("""
            select tt from TaskType tt
            where tt.id = :id
            and tt.status in(:statuses)
            """)
    Optional<TaskType> findByIdAndStatusIn(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            t.id,
                            t.name
                        )
                        from TaskType t
                        where t.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query("""
            select count(t.id) > 0
            from Task t
            join TaskType tt on t.taskTypeId = tt.id
            where tt.id = :id
            and t.status = 'ACTIVE'
            """)
    boolean hasActiveConnections(Long id);
}
