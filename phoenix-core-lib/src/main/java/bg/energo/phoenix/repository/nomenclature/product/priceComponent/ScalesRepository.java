package bg.energo.phoenix.repository.nomenclature.product.priceComponent;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForParent;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.Scales;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.PodSubObjectStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ScalesRepository extends JpaRepository<Scales, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(s.id, CONCAT(s.name, ' - ', s.scaleType), s.orderingId, s.defaultSelection, s.status)
            from Scales as s
            where (:prompt is null or lower(s.name) like :prompt)
            and (:excludedItemId is null or s.id <> :excludedItemId)
            and (s.status in (:statuses))
            order by s.orderingId asc
            """)
    Page<NomenclatureResponse> filterNomenclature(@Param("prompt") String prompt, @Param("statuses") List<NomenclatureItemStatus> statuses, @Param("excludedItemId") Long excludedItemId, Pageable pageable);

    @Query(value = """
                    select s from Scales s
                    where s.id=:id
                    and s.status in :statuses
            """)
    Optional<Scales> findByIdAndStatuses(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(value = """
            select s from Scales s
             where s.id <> :currentId
             and (s.orderingId >= :start and s.orderingId <= :end)
            """)
    List<Scales> findInOrderingIdRange(@Param("start") Long start, @Param("end") Long end, @Param("currentId") Long currentId, Sort sort);

    @Query(value = """
                               select s from Scales as s
                                where s.orderingId is not null
                                order by s.name
            """)
    List<Scales> orderByName();

    Optional<Scales> findByDefaultSelectionTrue();

    @Query("""
             select s from Scales s
             where s.name like :name
             and s.status in(:statuses)
            """)
    List<Scales> findByNameAndStatuses(@Param("name") String name, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
             select max(s.orderingId) from Scales s
            """)
    Long findLastOrderingId();

    @Query(value = """
            select s from Scales s
            where (:prompt is null or lower(s.name) like :prompt)
            and ((s.status in (:statuses)
            and (:excludedItemId is null or s.id <> :excludedItemId)
            and (:gridOperatorId is null or s.gridOperator.id = :gridOperatorId))
            or (s.id in (:includedItemIds)))
            order by case when s.id in (:includedItemIds) then 1 else 2 end,
            s.defaultSelection desc, s.orderingId asc
            """, countQuery = """
            select count(s.id) from Scales s
            where (:prompt is null or lower(s.name) like :prompt)
            and ((s.status in (:statuses))
            and (:excludedItemId is null or s.id <> :excludedItemId)
            and (:gridOperatorId is null or s.gridOperator.id = :gridOperatorId)
            or (s.id in (:includedItemIds)))
            """)
    Page<Scales> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("gridOperatorId") Long gridOperatorId,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    @Query("""
                select new bg.energo.phoenix.model.CacheObject(s.id, s.name)
                from Scales s
                where s.name = :name
                and s.status =:status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getCacheObjectByNameAndStatus(@Param("name") String name, @Param("status") NomenclatureItemStatus status);

    @Query("""
                select new bg.energo.phoenix.model.CacheObjectForParent(s.id, s.name,s.gridOperator.name,s.status)
                from Scales s
                where s.status in (:status)
                order by s.gridOperator.name, s.id
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<CacheObjectForParent> getCacheObjectByStatus(@Param("status") List<NomenclatureItemStatus> status);

    @Query("""
            select count(s.id) from Scales s
            where s.id = :id
            and
            (
                exists
                    (select 1 from MeterScale ms where ms.scaleId = :id and ms.status in(:meterStatuses))
            )
            """)
    Long activeConnectionCount(@Param("id") Long scaleId, @Param("meterStatuses") List<PodSubObjectStatus> meterStatuses);

    Optional<Scales> findByScaleCodeAndScaleTypeAndTariffScaleAndStatus(String code, String type, String tariff, NomenclatureItemStatus status);

    Optional<Scales> findByScaleCodeAndScaleTypeAndStatusAndGridOperatorId(String scaleCode, String scaleType, NomenclatureItemStatus status, Long gridOperator_id);

    Optional<Scales> findByTariffScaleAndScaleTypeAndGridOperatorIdAndStatus(String tariff, String type, Long gridOperatorId, NomenclatureItemStatus status);

    Optional<Scales> findByScaleTypeAndStatus(String type, NomenclatureItemStatus status);

    Optional<Scales> findByGridOperatorIdAndScaleCodeAndStatus(Long gridOperatorId, String scaleCode, NomenclatureItemStatus status);

    Optional<Scales> findByGridOperatorIdAndScaleCodeAndStatusAndIdIsNot(Long gridOperatorId, String scaleCode, NomenclatureItemStatus status, Long id);

    Optional<Scales> findByGridOperatorIdAndTariffScaleAndStatus(Long gridOperatorId, String scaleCode, NomenclatureItemStatus status);

    Optional<Scales> findByGridOperatorIdAndTariffScaleAndStatusAndIdIsNot(Long gridOperatorId, String scaleCode, NomenclatureItemStatus status, Long id);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            s.id,
                            s.name
                        )
                        from Scales s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    boolean existsByScaleCodeAndGridOperatorId(String scaleCode, Long gridOperatorId);

    boolean existsByScaleCodeAndGridOperatorIdAndIdNotIn(String scaleCode, Long gridOperatorId, List<Long> id);

    boolean existsByTariffScaleAndGridOperatorId(String tariffScale, Long gridOperatorId);

    boolean existsByTariffScaleAndGridOperatorIdAndIdNotIn(String tariffScale, Long gridOperatorId, List<Long> id);

    @Query("""
            select s from Scales s 
            where s.id in :scaleIds
            and s.status in :statuses
            """)
    List<Scales> findScaleIdsInAndStatuses(@Param("scaleIds") Set<Long> scaleIds,
                                         @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
            select distinct s.id from Scales s 
            join MeterScale ms on ms.scaleId = s.id
            where ms.meterId = :meterId
            and ms.status = 'ACTIVE'
            and s.status = 'INACTIVE'
            """)
    List<Long> findInactivesForMeter(Long meterId);
}
