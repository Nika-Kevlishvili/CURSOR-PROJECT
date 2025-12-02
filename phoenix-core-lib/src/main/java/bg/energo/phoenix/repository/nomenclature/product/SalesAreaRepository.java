package bg.energo.phoenix.repository.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.SalesArea;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalesAreaRepository extends JpaRepository<SalesArea, Long> {

    @Query(
        """
        select sa from SalesArea as sa
            where (:prompt is null or (
                lower(sa.name) like :prompt or
                lower(sa.loginPortalTag) like :prompt
            ))
            and sa.status in (:statuses)
            and :excludedItemId is null or sa.id <> :excludedItemId
            order by sa.orderingId asc
        """
    )
    Page<SalesArea> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
        """
        select sa from SalesArea as sa
            where sa.id<> :currentId
            and (sa.orderingId >= :start and sa.orderingId <= :end)
        """
    )
    List<SalesArea> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
        """
        select sa from SalesArea as sa
            where sa.orderingId is not null
            order by sa.name
        """
    )
    List<SalesArea> orderByName();

    @Query(
        value = """
        select sa from SalesArea as sa
            where (:prompt is null or (
                lower(sa.name) like :prompt or
                lower(sa.loginPortalTag) like :prompt
            ))
            and ((sa.status in (:statuses))
            and (:excludedItemId is null or sa.id <> :excludedItemId)
            or (sa.id in (:includedItemIds)))
            order by case when sa.id in (:includedItemIds) then 1 else 2 end,
            sa.defaultSelection desc, sa.orderingId asc
        """, countQuery = """
        select count(1) from SalesArea as sa
            where (:prompt is null or (
                lower(sa.name) like :prompt or
                lower(sa.loginPortalTag) like :prompt
            ))
            and ((sa.status in (:statuses))
            and (:excludedItemId is null or sa.id <> :excludedItemId)
            or (sa.id in (:includedItemIds)))
            """
    )
    Page<SalesArea> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    @Query(
        """
        select count(1) from SalesArea sa
            where lower(sa.name) = lower(:name)
            and sa.status in :statuses
        """
    )
    Long countSalesAreaByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
        """
        select max(sa.orderingId) from SalesArea sa
        """
    )
    Long findLastOrderingId();

    Optional<SalesArea> findByDefaultSelectionTrue();

    @Query("""
             select sa from SalesArea sa
             where sa.id = :id and sa.status in(:statuses)
            """
    )
    Optional<SalesArea> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
        select s
        from SalesArea s
        join ProductSalesArea ps on ps.salesArea.id=s.id
        where ps.productDetails.id = :productDetailsId
        and ps.productSubObjectStatus in :statuses
    """)
    List<SalesArea> findByProductDetailsId(@Param("productDetailsId")Long productDetailsId,
                                              @Param("statuses")List<ProductSubObjectStatus> statuses);

    List<SalesArea> findByIdInAndStatusIn(List<Long> ids, List<NomenclatureItemStatus> statuses);


    @Query(value = """
            select count(1) from  SalesArea sa
            where sa.id = :saleAreaId
            and
            ( exists
            (select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
              join ProductSalesArea psa
               on psa.productDetails.id = pd.id
                and psa.salesArea.id = sa.id
              where
                pd.productDetailStatus in (:productDetailStatuses)
                and p.productStatus = 'ACTIVE'
                and psa.productSubObjectStatus = 'ACTIVE')
            or
             exists
            (select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
              join ServiceSalesArea ssa
               on ssa.serviceDetails.id = sd.id
               and ssa.salesArea.id = sa.id
              where
                sd.status in (:serviceDetailStatuses)
                and s.status = 'ACTIVE'
                and ssa.status = 'ACTIVE')
            or
             exists
            (select 1 from Goods g
              join GoodsDetails gd on gd.goods.id = g.id
              join GoodsSalesAreas gsa
               on gsa.goodsDetails.id = gd.id
               and gsa.salesArea.id = sa.id
              where
                gd.status in (:goodsDetailStatuses)
                and g.goodsStatusEnum = 'ACTIVE'
                and gsa.status = 'ACTIVE'))
            """)
    Long activeConnectionCount(
            @Param("saleAreaId") Long saleAreaId,
            @Param("productDetailStatuses") List<ProductDetailStatus> productDetailStatuses,
            @Param("serviceDetailStatuses") List<ServiceDetailStatus> serviceDetailStatuses,
            @Param("goodsDetailStatuses") List<GoodsDetailStatus> goodsDetailStatuses
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            s.id,
                            s.name
                        )
                        from SalesArea s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
