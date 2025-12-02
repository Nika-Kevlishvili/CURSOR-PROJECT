package bg.energo.phoenix.repository.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.SalesChannel;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesChannelResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalesChannelRepository extends JpaRepository<SalesChannel, Long> {

    @Query(
            """
            select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(
            sch.id,
            case when sch.portalTagId is null then sch.name else coalesce(sch.name,' - ', pt.name) end,
            sch.orderingId,
            sch.defaultSelection,
            sch.status)
             from SalesChannel as sch
             left join PortalTag pt on pt.id=sch.portalTagId
                where (:prompt is null or (
                    lower(sch.name) like :prompt
                ))
                and sch.status in (:statuses)
                and :excludedItemId is null or sch.id <> :excludedItemId
                order by sch.orderingId asc
            """
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
            select sch from SalesChannel as sch
                where sch.id<> :currentId
                and (sch.orderingId >= :start and sch.orderingId <= :end)
            """
    )
    List<SalesChannel> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
            select sch from SalesChannel as sch
                where sch.orderingId is not null
                order by sch.name
            """
    )
    List<SalesChannel> orderByName();

    @Query("select new bg.energo.phoenix.model.response.nomenclature.product.SalesChannelResponse(sch,pt) from SalesChannel as sch" +
            " left join PortalTag pt on pt.id=sch.portalTagId" +
            " where " +
            "(:prompt is null or ( " +
            "lower(sch.name) like :prompt" +
            ")) " +
            "and ((sch.status in (:statuses)) " +
            "and (:excludedItemId is null or sch.id <> :excludedItemId) " +
            "or (sch.id in (:includedItemIds))) " +
            "order by case when sch.id in (:includedItemIds) then 1 else 2 end," +
            "sch.defaultSelection desc, sch.orderingId asc")
    Page<SalesChannelResponse> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    @Query(
            """
            select count(1) from SalesChannel sch
                where lower(sch.name) = lower(:name)
                and sch.status in :statuses
            """
    )
    Long countSalesChannelByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
            select max(sch.orderingId) from SalesChannel sch
            """
    )
    Long findLastOrderingId();

    Optional<SalesChannel> findByDefaultSelectionTrue();

    @Query("""
             select sc from SalesChannel sc
             where sc.id = :id and sc.status in(:statuses)
            """
    )
    Optional<SalesChannel> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
        select s
        from SalesChannel s
        join ProductSalesChannel ps on ps.salesChannel.id=s.id
        where ps.productDetails.id = :productDetailsId
        and ps.productSubObjectStatus in :statuses
    """)
    List<SalesChannel> findByProductDetailsId(@Param("productDetailsId")Long productDetailsId,
                                         @Param("statuses")List<ProductSubObjectStatus> statuses);

    List<SalesChannel> findByIdInAndStatusIn(List<Long> ids, List<NomenclatureItemStatus> statuses);


    @Query(value = """
            select count(1) from  SalesChannel sa
            where sa.id = :saleChannelId
            and
            ( exists
            (select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
              join ProductSalesChannel psa
               on psa.productDetails.id = pd.id
                and psa.salesChannel.id = sa.id
              where
                pd.productDetailStatus in (:productDetailStatuses)
                and p.productStatus = 'ACTIVE'
                and psa.productSubObjectStatus = 'ACTIVE')
            or
             exists
            (select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
              join ServiceSalesChannel ssa
               on ssa.serviceDetails.id = sd.id
               and ssa.salesChannel.id = sa.id
              where
                sd.status in (:serviceDetailStatuses)
                and s.status = 'ACTIVE'
                and ssa.status = 'ACTIVE')
            or
             exists
            (select 1 from Goods g
              join GoodsDetails gd on gd.goods.id = g.id
              join GoodsSalesChannels gsa
               on gsa.goodsDetails.id = gd.id
               and gsa.salesChannel.id = sa.id
              where
                gd.status in (:goodsDetailStatuses)
                and g.goodsStatusEnum = 'ACTIVE'
                and gsa.status = 'ACTIVE'))
            """)
    Long activeConnectionCount(
            @Param("saleChannelId") Long saleChannelId,
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
                        from SalesChannel s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
