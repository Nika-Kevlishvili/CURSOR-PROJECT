package bg.energo.phoenix.repository.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.ElectricityPriceType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
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
public interface ElectricityPriceTypeRepository extends JpaRepository<ElectricityPriceType, Long> {
    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(
                    e.id,
                     e.name,
                     e.orderingId,
                     e.defaultSelection,
                     e.status
                     )
                    from ElectricityPriceType e
                    where (:prompt is null or (lower(e.name) like :prompt))
                    and (e.status in (:statuses))
                    """
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    @Query(value = """
                    select e from ElectricityPriceType e
                    where e.id=:id
                    and e.status in :statuses
            """)
    Optional<ElectricityPriceType> findByIdAndStatuses(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                    select e from ElectricityPriceType e
                     where e.id <> :currentId
                     and (e.orderingId >= :start and e.orderingId <= :end)
                    """
    )
    List<ElectricityPriceType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            value = """
                                       select e from ElectricityPriceType as e
                                        where e.orderingId is not null
                                        order by e.name
                    """
    )
    List<ElectricityPriceType> orderByName();

    @Query(value = """
                    select e from ElectricityPriceType e
                    where e.name=:name
                    and e.status in :statuses
            """)
    Optional<ElectricityPriceType> findByNameAndStatuses(@Param("name") String name, List<NomenclatureItemStatus> statuses);

    @Query("select max(e.orderingId) from ElectricityPriceType e")
    Long findLastOrderingId();

    @Query(
            value = """
                     select e from ElectricityPriceType e
                     where (:prompt is null or (lower(e.name) like :prompt ))
                     and (e.status in (:statuses))
                     and (:excludedItemId is null or e.id <> :excludedItemId)
                     order by e.defaultSelection desc , e.orderingId asc
                    """
    )
    Page<ElectricityPriceType> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<ElectricityPriceType> findByDefaultSelectionTrue();

    @Query("""
                select count(1) from  ElectricityPriceType sa
             where sa.id = :id
             and
             ( exists
             (select 1 from Product p
               join ProductDetails pd on pd.product.id = p.id
                 and pd.electricityPriceType.id = sa.id
               where
                 pd.productDetailStatus in (:productDetailStatuses)
                 and p.productStatus = 'ACTIVE'))
           """)
    Long activeConnectionCount(@Param("id") Long id,
                               @Param("productDetailStatuses") List<ProductDetailStatus> productDetailStatuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            e.id,
                            e.name
                        )
                        from ElectricityPriceType e
                        where e.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
