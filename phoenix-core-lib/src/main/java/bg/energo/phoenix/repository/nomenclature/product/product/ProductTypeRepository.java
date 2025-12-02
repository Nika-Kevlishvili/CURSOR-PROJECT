package bg.energo.phoenix.repository.nomenclature.product.product;

import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductTypes;
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

import java.util.List;
import java.util.Optional;

public interface ProductTypeRepository extends JpaRepository<ProductTypes, Long> {
    @Query(
            "select pt from ProductTypes as pt" +
                    " where (:prompt is null or lower(pt.name) like :prompt)" +
                    " and (pt.status in (:statuses))" +
                    " and (:excludedItemId is null or pt.id <> :excludedItemId) " +
                    " order by pt.isDefault desc, pt.orderingId asc"
    )
    Page<ProductTypes> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(pt.id, pt.name, pt.orderingId, pt.isDefault, pt.status) " +
                    "from ProductTypes as pt" +
                    " where (:prompt is null or lower(pt.name) like :prompt)" +
                    " and (pt.status in (:statuses))" +
                    " order by pt.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<ProductTypes> findByIsDefaultTrue();

    @Query("select max(pt.orderingId) from ProductTypes pt")
    Long findLastOrderingId();

    @Query(
            "select pt from ProductTypes as pt" +
                    " where pt.id <> :currentId " +
                    " and (pt.orderingId >= :start and pt.orderingId <= :end) "
    )
    List<ProductTypes> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select pt from ProductTypes as pt" +
                    " where pt.orderingId is not null" +
                    " order by pt.name"
    )
    List<ProductTypes> orderByName();

    @Query(
            """
                                select pt from ProductTypes pt
                                where pt.id = :id
                                and pt.status in :statuses
                    """
    )
    Optional<ProductTypes> findByIdAndStatus(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(
            """
                    select count(1) from ProductTypes as pt
                    where lower(pt.name) = :name and pt.status in :statuses
                    """
    )
    Integer getExistingRecordsCountByName(@Param("name")String name, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
             select count(1) from  ProductTypes sa
             where sa.id = :id
             and
             ( exists
             (select 1 from Product p
               join ProductDetails pd on pd.product.id = p.id
                 and pd.productType.id = sa.id
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
                            p.id,
                            p.name
                        )
                        from ProductTypes p
                        where p.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
