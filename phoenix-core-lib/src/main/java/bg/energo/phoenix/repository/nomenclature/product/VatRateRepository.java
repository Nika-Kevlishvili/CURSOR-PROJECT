package bg.energo.phoenix.repository.nomenclature.product;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VatRateRepository extends JpaRepository<VatRate, Long> {
    @Query("select max(v.orderingId) from VatRate v")
    Long findLastOrderingId();

    @Query(
            "select v from VatRate as v" +
                    " where v.id <> :currentId " +
                    " and (v.orderingId >= :start and v.orderingId <= :end) "
    )
    List<VatRate> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select v from VatRate as v" +
                    " where v.orderingId is not null" +
                    " order by v.name"
    )
    List<VatRate> orderByName();

    @Query(
            "select count (v.id) from VatRate v" +
                    " where v.status = 'ACTIVE' and v.globalVatRate = true"
    )
    Long countByGlobalVatRateAndStatus();

//    Optional<VatRate> findVatRateByNameAndStatus(String name, NomenclatureItemStatus status);

    @Query(
            """
            select count(v.id) from VatRate v
                where lower(v.name)  = lower(:name)
                and v.status in (:statuses)
            """
    )
    Long countVatRateByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    Optional<VatRate> findVatRateByStartDateAndGlobalVatRateAndStatus(LocalDate startDate, boolean globalVatRate, NomenclatureItemStatus status);

    @Query(
            "select v from VatRate as v" +
                    " where (:prompt is null or (" +
                    " lower(v.name) like :prompt " +
                    " ))" +
                    " and (v.status in (:statuses))" +
                    " and (:excludedItemId is null or v.id <> :excludedItemId) " +
                    " order by v.globalVatRate desc, v.orderingId asc"
    )
    Page<VatRate> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "v.id," +
                    " v.name, " +
                    " v.orderingId," +
                    " false," +
                    " v.status" +
                    ") " +
                    "from VatRate as v" +
                    " where (:prompt is null or (" +
                    " lower(v.name) like :prompt" +
                    "))" +
                    " and (v.status in (:statuses))" +
                    " and (:excludedItemId is null or v.id <> :excludedItemId) " +
                    " order by v.globalVatRate desc, v.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query("""
             select vr from VatRate vr
             where vr.id = :id and vr.status in(:statuses)
            """
    )
    Optional<VatRate> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(value = """
            select count(1) from  VatRate sa
            where sa.id = :id
            and
           (exists
            (select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
                and pd.vatRate.id = sa.id
              where
                pd.productDetailStatus in (:productDetailStatuses)
                and p.productStatus = 'ACTIVE')
            or
             exists
            (select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
               and sd.vatRate.id = sa.id
              where
                sd.status in (:serviceDetailStatuses)
                and s.status = 'ACTIVE')
            or
             exists
            (select 1 from Goods g
              join GoodsDetails gd on gd.goods.id = g.id
               and gd.vatRate.id = sa.id
              where
                gd.status in (:goodsDetailStatuses)
                and g.goodsStatusEnum = 'ACTIVE')
            or
            exists
            (select 1 from PriceComponent pc
              where
               pc.vatRate.id  = sa.id
               and
               pc.status in (:priceComponentStatuses))
                           or
            exists
            (select 1 from BillingRun br
              where
               br.vatRateId =sa.id
               and
               br.status not in (:billingStatuses)))
            """)
    Long activeConnectionCount(
            @Param("id") Long id,
            @Param("productDetailStatuses") List<ProductDetailStatus> productDetailStatuses,
            @Param("serviceDetailStatuses") List<ServiceDetailStatus> serviceDetailStatuses,
            @Param("goodsDetailStatuses") List<GoodsDetailStatus> goodsDetailStatuses,
            @Param("priceComponentStatuses") List<PriceComponentStatus> priceComponentStatuses,
            @Param("billingStatuses") List<BillingStatus> billingStatuses
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            v.id,
                            v.name
                        )
                        from VatRate v
                        where v.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    boolean existsByGlobalVatRateAndStatusIn(boolean globalVatRate, List<NomenclatureItemStatus> statuses);

    Optional<VatRate> findByGlobalVatRateAndStatusIn(boolean globalVatRate, List<NomenclatureItemStatus> statuses);

    @Query("""
        select new bg.energo.phoenix.model.CacheObject(v.id, v.name)
        from VatRate v
        where v.name = :name
        and v.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getCacheObjectByNameAndStatus(@Param("name")String name,
                                                        @Param("status") NomenclatureItemStatus status);

    Optional<VatRate> findByNameAndStatusIn(String vatRateName, List<NomenclatureItemStatus> active);

    @Query("""
            select vr
            from VatRate vr
            where vr.startDate <= :currentDate
            and vr.status = 'ACTIVE'
            and vr.globalVatRate = true
            order by vr.startDate desc
            """)
    Optional<VatRate> findGlobalVatRate(@Param("currentDate") LocalDate currentDate,
              PageRequest pageRequest);



}
