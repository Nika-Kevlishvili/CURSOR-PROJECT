package bg.energo.phoenix.repository.nomenclature.product.priceComponent;

import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.PriceComponentValueType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
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
public interface PriceComponentValueTypeRepository extends JpaRepository<PriceComponentValueType,Long> {

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(
                    s.id,
                     s.name,
                     s.orderingId,
                     s.defaultSelection,
                     s.status
                     )
                    from PriceComponentValueType s
                    where (:prompt is null or (lower(s.name) like :prompt))
                    and (s.status in (:statuses))
                    """
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    @Query(value = """
                    select s from PriceComponentValueType s
                    where s.id=:id
                    and s.status in :statuses
            """)
    Optional<PriceComponentValueType> findByIdAndStatuses(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                    select s from PriceComponentValueType s
                     where s.id <> :currentId
                     and (s.orderingId >= :start and s.orderingId <= :end)
                    """
    )
    List<PriceComponentValueType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            value = """
                                       select s from PriceComponentValueType as s
                                        where s.orderingId is not null
                                        order by s.name
                    """
    )
    List<PriceComponentValueType> orderByName();

    @Query(value = """
                    select s from PriceComponentValueType s
                    where s.name=:name
                    and s.status in :statuses
            """)
    Optional<PriceComponentValueType> findByNameAndStatuses(@Param("name") String name, List<NomenclatureItemStatus> statuses);

    @Query("select max(s.orderingId) from PriceComponentValueType s")
    Long findLastOrderingId();

    @Query(
            value = """
                     select s from PriceComponentValueType s
                     where (:prompt is null or (lower(s.name) like :prompt ))
                     and (s.status in (:statuses))
                     and (:excludedItemId is null or s.id <> :excludedItemId)
                     order by s.defaultSelection desc, s.orderingId asc
                    """
    )
    Page<PriceComponentValueType> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<PriceComponentValueType> findByDefaultSelectionTrue();

    @Query("""
            select count(1) from  PriceComponentValueType sa
            where sa.id = :id
            and
            ( exists
            (select 1 from PriceComponent pc
              where
               pc.valueType.id  = sa.id
               and
               pc.status in (:priceComponentStatuses)))
           """)
    Long activeConnectionCount(@Param("id") Long id,
                               @Param("priceComponentStatuses") List<PriceComponentStatus> priceComponentStatuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            p.id,
                            p.name
                        )
                        from PriceComponentValueType p
                        where p.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
