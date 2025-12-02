package bg.energo.phoenix.repository.nomenclature.product;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.meter.MeterStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
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

@Repository
public interface GridOperatorRepository extends JpaRepository<GridOperator, Long> {

    @Query(
            """
                    select go from GridOperator as go
                        where (:prompt is null or (
                            lower(go.name) like :prompt or
                            lower (go.fullName) like :prompt
                        ))
                        and (go.status in (:statuses))
                        and (:excludedItemId is null or go.id <> :excludedItemId)
                        order by go.orderingId asc
                    """
    )
    Page<GridOperator> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
                    select go from GridOperator as go
                        where go.id<> :currentId
                        and (go.orderingId >= :start and go.orderingId <= :end)
                    """
    )
    List<GridOperator> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
                    select go from GridOperator as go
                        where go.orderingId is not null
                        order by go.name
                    """
    )
    List<GridOperator> orderByName();

    @Query(
            value = """
                    select go from GridOperator as go
                        where (:prompt is null or (
                            lower(go.name) like :prompt or
                            lower(go.fullName) like :prompt
                        ))
                        and (
                            (go.status in (:statuses))
                            and (:excludedItemId is null or go.id <> :excludedItemId)
                            or (go.id in (:includedItemIds)))
                        order by case when go.id in (:includedItemIds) then 1 else 2 end,
                        go.defaultSelection desc, go.orderingId asc
                    """, countQuery = """
            select count(go.id) from GridOperator as go
                where (:prompt is null or (
                    lower(go.name) like :prompt or
                    lower(go.fullName) like :prompt
                ))
                and (
                    (go.status in (:statuses))
                    and (:excludedItemId is null or go.id <> :excludedItemId)
                    or (go.id in (:includedItemIds)))
            """
    )
    Page<GridOperator> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    @Query(
            """
                    select count(go.id) from GridOperator go
                        where lower(go.name) = lower(:name)
                        and go.status in :statuses
                    """
    )
    Long countGridOperatorsByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
                    select max(go.orderingId) from GridOperator go
                    """
    )
    Long findLastOrderingId();

    Optional<GridOperator> findByDefaultSelectionTrue();

    @Query("""
             select go from GridOperator go
             where go.id = :id and go.status in(:statuses)
            """
    )
    Optional<GridOperator> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    List<GridOperator> findByIdInAndStatusIn(List<Long> ids, List<NomenclatureItemStatus> statuses);

    @Query(value = """
            select count(sa.id) from  GridOperator sa
            where sa.id = :id
            and
            ( exists
            (select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
              join ProductGridOperator psa
               on psa.productDetails.id = pd.id
                and psa.gridOperator.id = sa.id
              where
                pd.productDetailStatus in (:productDetailStatuses)
                and p.productStatus = 'ACTIVE'
                and psa.productSubObjectStatus = 'ACTIVE')
            or
             exists
            (select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
              join ServiceGridOperator ssa
               on ssa.serviceDetails.id = sd.id
               and ssa.gridOperator.id = sa.id
              where
                sd.status in (:serviceDetailStatuses)
                and s.status = 'ACTIVE'
                and ssa.status = 'ACTIVE')
            or
             exists
            (select 1 from Scales g
              where  g.gridOperator.id = sa.id and g.status in (:scaleStatuses)
                )
            or exists
             (select 1 from PointOfDelivery pod
                where pod.status in (:podStatus)
                and pod.gridOperatorId = sa.id
                )
            or exists
            (
            select 1 from Meter m
            where m.status in (:meterStatus)
            and m.gridOperatorId = sa.id
            )
            )
            """)
    Long activeConnectionCount(
            @Param("id") Long id,
            @Param("productDetailStatuses") List<ProductDetailStatus> productDetailStatuses,
            @Param("serviceDetailStatuses") List<ServiceDetailStatus> serviceDetailStatuses,
            @Param("scaleStatuses") List<NomenclatureItemStatus> scaleStatuses,
            @Param("podStatus") List<PodStatus> podStatus,
            @Param("meterStatus") List<MeterStatus> meterStatuses
    );

    @Query("""
                select new bg.energo.phoenix.model.CacheObject(g.id, g.name)
                from GridOperator g
                where g.name = :name
                and g.status =:status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getCacheObjectByNameAndStatus(@Param("name") String name, @Param("status") NomenclatureItemStatus status);

    Optional<GridOperator> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            g.id,
                            g.name
                        )
                        from GridOperator g
                        where g.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);


    @Query("""
            select pd from PointOfDeliveryDetails pd
            join PointOfDelivery p on pd.podId = p.id
            where pd.id=:podDetailId
            and p.gridOperatorId in :gridOperators
                """)
    boolean checkPodForGridOperator(@Param("podDetailId") Long podDetailId,
                                    @Param("gridOperators") List<Long> energoProGridOperatorIds);

    @Query("""
            select go.id
            from GridOperator go
            where go.status = 'ACTIVE'
            and go.ownedByEnergoPro = true
            """)
    List<Long> fetchEnergoProGridOperators();
}
