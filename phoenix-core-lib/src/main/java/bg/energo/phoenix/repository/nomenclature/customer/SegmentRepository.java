package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
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

import java.util.List;
import java.util.Optional;

public interface SegmentRepository extends JpaRepository<Segment, Long> {

    @Query("""
        select s
        from Segment s
        where s.id = :id
        and s.status in :statuses
    """)
    Optional<Segment> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query(
            "select s from Segment as s" +
                    " where (:prompt is null or lower(s.name) like :prompt)" +
                    " and (((s.status in (:statuses))" +
                    " and (:excludedItemId is null or s.id <> :excludedItemId)) " +
                    " or (s.id in (:includedItemIds)))" +
                    " order by case when s.id in (:includedItemIds) then 1 else 2 end," +
                    " s.defaultSelection desc, s.orderingId asc "
    )
    Page<Segment> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "s.id," +
                    "s.name, " +
                    "s.orderingId, " +
                    "s.defaultSelection, " +
                    "s.status" +
                    ") " +
                    "from Segment as s" +
                    " where (:prompt is null or lower(s.name) like :prompt)" +
                    " and (s.status in (:statuses))" +
                    " order by s.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<Segment> findByDefaultSelectionTrue();

    @Query("select max(s.orderingId) from Segment s")
    Long findLastOrderingId();

    @Query(
            "select s from Segment as s" +
                    " where s.id <> :currentId " +
                    " and (s.orderingId >= :start and s.orderingId <= :end) "
    )
    List<Segment> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select s from Segment as s" +
                    " where s.orderingId is not null" +
                    " order by s.name"
    )
    List<Segment> orderByName();


    @Query(
        """
            select new bg.energo.phoenix.model.CacheObject(s.id, s.name)
            from Segment s
            where s.name = :name
            and s.status = :status
        """
    )
    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    Optional<CacheObject> findByNameAndStatus(@Param("name")String name,
                                              @Param("status") NomenclatureItemStatus status);

    @Query(
            """
            select count(1) from Segment s
                where s.id = :id
                and exists (select 1 from CustomerSegment cs, CustomerDetails cd, Customer c
                    where cs.segment.id = :id
                    and cs.customerDetail.id = cd.id
                    and cd.customerId = c.id
                    and cs.status = 'ACTIVE' and c.status = 'ACTIVE')
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query("""
        select s
        from Segment s
        join ProductSegments pd on pd.segment.id=s.id
        where pd.productDetails.id = :productDetailsId
        and pd.productSubObjectStatus in :statuses
    """)
    List<Segment> findByProductDetailsId(@Param("productDetailsId")Long productDetailsId,
                                            @Param("statuses")List<ProductSubObjectStatus> statuses);

    List<Segment> findByIdInAndStatusIn(List<Long> ids, List<NomenclatureItemStatus> statuses);

    @Query(
            """
            select count(s.id) from Segment s
                where lower(s.name) = lower(:name)
                and s.status in (:statuses)
            """
    )
    Long countSegmentByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(value = """
            select count(1) from  Segment sa
            where sa.id = :segmentId
            and
            ( exists
            (select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
              join ProductSegments psa
               on psa.productDetails.id = pd.id
                and psa.segment.id = sa.id
              where
                pd.productDetailStatus in (:productDetailStatuses)
                and p.productStatus = 'ACTIVE'
                and psa.productSubObjectStatus = 'ACTIVE')
            or
             exists
            (select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
              join ServiceSegment ssa
               on ssa.serviceDetails.id = sd.id
               and ssa.segment.id = sa.id
              where
                sd.status in (:serviceDetailStatuses)
                and s.status = 'ACTIVE'
                and ssa.status = 'ACTIVE')
            or
             exists
            (select 1 from Goods g
              join GoodsDetails gd on gd.goods.id = g.id
              join GoodsSegments gsa
               on gsa.goodsDetails.id = gd.id
               and gsa.segment.id = sa.id
              where
                gd.status in (:goodsDetailStatuses)
                and g.goodsStatusEnum = 'ACTIVE'
                and gsa.status = 'ACTIVE'))
            """)
    Long activeConnectionCount(
            @Param("segmentId") Long segmentId,
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
                        from Segment s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
