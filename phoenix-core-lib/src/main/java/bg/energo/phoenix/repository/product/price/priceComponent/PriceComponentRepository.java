package bg.energo.phoenix.repository.product.price.priceComponent;

import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.response.contract.action.calculation.PriceComponentFormulaDto;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentForOvertimeResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentForServiceOrderResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentMiddleResponse;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse;
import bg.energo.phoenix.model.response.service.AvailableServiceRelatedEntitiyResponse;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PriceComponentRepository extends JpaRepository<PriceComponent, Long> {
    Optional<PriceComponent> findByIdAndStatusIn(Long priceComponentId, List<PriceComponentStatus> statuses);

    List<PriceComponent> findByIdInAndStatusIn(List<Long> ids, List<PriceComponentStatus> statuses);

    @Query("""
            select pc.id
            from PriceComponent pc
            join ProductPriceComponents ppc on pc.id = ppc.priceComponent.id
            where ppc.productSubObjectStatus = 'ACTIVE'
            and pc.status = 'ACTIVE'
            and ppc.productDetails.id = :productDetailId
            """)
    List<Long> findActivePriceComponentsByProductDetailId(Long productDetailId);

    @Query("""
            select pc.id
            from PriceComponent pc
            join ServicePriceComponent spc on pc.id = spc.priceComponent.id
            where spc.status = 'ACTIVE'
            and pc.status = 'ACTIVE'
            and spc.serviceDetails.id = :serviceDetailId
            """)
    List<Long> findActivePriceComponentByServiceDetailId(Long serviceDetailId);

    @Query("""
            select pc.id
            from PriceComponentGroupDetails pcgd
            join PriceComponentGroup pcg on pcg.id = pcgd.priceComponentGroupId
            join PriceComponentGroupPriceComponent pcgpc on pcgpc.priceComponentGroupDetailId = pcgd.id
            join PriceComponent pc on pcgpc.priceComponentId = pc.id
            where pcg.id = :priceComponentGroupId
            and pcgpc.status = 'ACTIVE'
            and pc.status = 'ACTIVE'
            order by pcgd.startDate desc
            """)
    Optional<Long> findRespectivePriceComponentByGroupId(
            Long priceComponentGroupId,
            PageRequest limit
    );

    @Query("""
            select pc
            from PriceComponentGroupDetails pcgd
            join PriceComponentGroup pcg on pcg.id = pcgd.priceComponentGroupId
            join PriceComponentGroupPriceComponent pcgpc on pcgpc.priceComponentGroupDetailId = pcgd.id
            join PriceComponent pc on pcgpc.priceComponentId = pc.id
            where pcg.id = :priceComponentGroupId
            and pcgpc.status = 'ACTIVE'
            and pc.status = 'ACTIVE'
            order by pcgd.startDate desc
            """)
    Optional<PriceComponent> findRespectivePriceComponentByGroup(
            Long priceComponentGroupId,
            PageRequest limit
    );

    @Query(value = """
            select pc from PriceComponent pc
                where pc.priceComponentGroupDetailId is null
                and pc.status = 'ACTIVE'
                and not exists (select 1 from ServicePriceComponent spc
                    where spc.priceComponent.id = pc.id
                    and spc.serviceDetails.service.status = 'ACTIVE'
                    and spc.status = 'ACTIVE'
                )
                and not exists (select 1 from ProductPriceComponents ppc 
                    where ppc.priceComponent.id = pc.id 
                    and ppc.productDetails.product.productStatus = 'ACTIVE'
                    and ppc.productSubObjectStatus = 'ACTIVE'
                )
                and not exists (select 1 from InterimAdvancePayment iap
                    where iap.priceComponent.id = pc.id
                    and iap.status = 'ACTIVE'
                )
                and (not exists (select 1 from PriceComponentFormulaVariable pcfv
                        where pcfv.priceComponent.id = pc.id)
                    or (
                        exists (select 1 from PriceComponentFormulaVariable pcfv
                            where pcfv.priceComponent.id = pc.id)
                            and not exists (select 1 from PriceComponentFormulaVariable pcfv2
                                where pcfv2.priceComponent.id = pc.id
                                and (pcfv2.value is null
                                or (pcfv2.value is not null and (pcfv2.valueFrom is not null or pcfv2.valueTo is not null)))
                            )
                    )
                )
                and (:priceComponentIds is null or pc.id in (:priceComponentIds))
            """
    )
    List<PriceComponent> getAvailablePriceComponentsIn(
            @Param("priceComponentIds") List<Long> priceComponentIds
    );

    @Query(value = """
            select pc from PriceComponent pc
                join PriceComponentGroupDetails pcgd on pcgd.priceComponentGroupId = :priceComponentGroupId
                join PriceComponentGroupPriceComponent pcgpc on pcgpc.priceComponentGroupDetailId = pcgd.id
                    where pcgpc.status = 'ACTIVE'
                    and pc.id = pcgpc.priceComponentId
            """
    )
    List<PriceComponent> getConnectedActivePriceComponentsByPriceComponentGroupId(
            @Param("priceComponentGroupId") Long priceComponentGroupId
    );


    /**
     * Retrieves a list of all available {@link PriceComponent} entities and, optionally, filters by a name or id.
     *
     * @param prompt prompt to filter by price components
     * @return a list of available {@link PriceComponent} objects
     */
    @Query(
            value = """
                    select pc from PriceComponent pc
                        where pc.priceComponentGroupDetailId is null
                        and pc.status = 'ACTIVE'
                        and not exists (select 1 from ServicePriceComponent spc
                            where spc.priceComponent.id = pc.id
                            and spc.serviceDetails.service.status = 'ACTIVE'
                            and spc.status = 'ACTIVE'
                        )
                        and not exists (select 1 from ProductPriceComponents ppc 
                            where ppc.priceComponent.id = pc.id 
                            and ppc.productDetails.product.productStatus = 'ACTIVE'
                            and ppc.productSubObjectStatus = 'ACTIVE'
                        )
                        and not exists (select 1 from InterimAdvancePayment iap
                            where iap.priceComponent.id = pc.id
                            and iap.status = 'ACTIVE'
                        )
                        and (
                            not exists (select 1 from PriceComponentFormulaVariable pcfv
                                where pcfv.priceComponent.id = pc.id)
                            or (
                                exists (select 1 from PriceComponentFormulaVariable pcfv
                                    where pcfv.priceComponent.id = pc.id)
                                and not exists (select 1 from PriceComponentFormulaVariable pcfv2
                                    where pcfv2.priceComponent.id = pc.id
                                    and (pcfv2.value is null
                                    or (pcfv2.value is not null and (pcfv2.valueFrom is not null or pcfv2.valueTo is not null)))
                                )
                            )
                        )
                        and (:prompt is null or concat(lower(pc.name), pc.id) like :prompt)
                        order by pc.createDate desc
                    """
    )
    Page<PriceComponent> getAvailablePriceComponentsForGroup(
            @Param("prompt") String prompt,
            Pageable pageable
    );


    /**
     * Retrieves a list of all available {@link PriceComponent} entities for interim advance payments and, optionally, filters by a name or id.
     *
     * @param prompt prompt to filter by price components
     * @return a list of available {@link PriceComponent} objects
     */
    @Query(
            value = """
                    select pc from PriceComponent pc
                        where pc.priceComponentGroupDetailId is null
                        and pc.status = 'ACTIVE'
                        and not exists (select 1 from ServicePriceComponent spc
                            where spc.priceComponent.id = pc.id
                            and spc.serviceDetails.service.status = 'ACTIVE'
                            and spc.serviceDetails.status = 'ACTIVE'
                            and spc.status = 'ACTIVE'
                        )
                        and not exists (select 1 from ProductPriceComponents ppc
                            where ppc.priceComponent.id = pc.id
                            and ppc.productDetails.product.productStatus = 'ACTIVE'
                            and ppc.productDetails.productDetailStatus = 'ACTIVE'
                            and ppc.productSubObjectStatus = 'ACTIVE'
                        )
                        and not exists (select 1 from InterimAdvancePayment iap
                            where iap.priceComponent.id = pc.id
                            and iap.status = 'ACTIVE'
                        )
                        and (:prompt is null or concat(lower(pc.name), pc.id) like :prompt)
                        order by pc.createDate desc
                    """
    )
    Page<PriceComponent> getAvailablePriceComponentsForIap(
            @Param("prompt") String prompt,
            Pageable pageable
    );


    @Query(nativeQuery = true,
            value = """
                    select tbl.id                                      as id,
                           tbl.price_component_name                    as name,
                           tbl.price_component_price_type              as price,
                           tbl.price_component_value_type              as value,
                           tbl.number_type                             as number,
                           tbl.conditions                              as conditions,
                           coalesce(tbl.group_product_service_name, 'Available') as available,
                           tbl.price_formula                           as formula,
                           tbl.create_date                             as cdate,
                           tbl.status                                  as status
                    from (select pc.id,
                                 pc.name    as                                                price_component_name,
                                 pcpt.name  as                                                price_component_price_type,
                                 pcvt.name  as                                                price_component_value_type,
                                 pc.number_type,
                                 pc.status  as                                                status,
                                 (select coalesce(
                                                 (select pcgd.name
                                                  from price_component.price_component_group_details pcgd
                                                           join price_component.pc_group_pcs pgp
                                                                on pgp.price_component_group_detail_id = pcgd.id
                                                  where pgp.price_component_id = pc.id
                                                    and pgp.status = 'ACTIVE'
                                                    and pc.status = 'ACTIVE'
                                                  limit 1),
                                                 (select pd.name
                                                  from product.product_details pd
                                                           join product.product_price_components ppc on ppc.product_detail_id = pd.id
                                                  where ppc.price_component_id = pc.id
                                                    and ppc.status = 'ACTIVE'
                                                    and pd.status = 'ACTIVE'
                                                  limit 1),
                                                 (select sd.name
                                                  from service.service_details sd
                                                           join service.service_price_components spc on spc.service_detail_id = sd.id
                                                  where spc.price_component_id = pc.id
                                                    and spc.status = 'ACTIVE'
                                                    and sd.status = 'ACTIVE'
                                                  limit 1),
                                                 (select iap.name
                                                  from interim_advance_payment.interim_advance_payments iap
                                                  where iap.price_component_id = pc.id
                                                    and iap.status = 'ACTIVE'
                                                  limit 1)
                                         )) as                                                group_product_service_name,
                                 case when pc.conditions is NOT null then 'YES' else 'NO' end conditions,
                                 pc.price_formula,
                                 pc.create_date,
                                 pc.price_component_price_type_id,
                                 pc.price_component_value_type_id
                          from price_component.price_components pc
                                   join
                               nomenclature.price_component_price_types pcpt
                               on pc.price_component_price_type_id = pcpt.id
                                   join
                               nomenclature.price_component_value_types pcvt
                               on pc.price_component_value_type_id = pcvt.id) as tbl
                    where (:columnname is null or (:columnname = 'ALL' and (lower(tbl.price_component_name) like :columnvalue))
                        or
                           (
                               :columnname = 'PRICECOMPONENTNAME' and lower(tbl.price_component_name) like :columnvalue
                               ))
                      and ((:pricetype) is null or tbl.price_component_price_type_id in (:pricetype))
                      and ((:valuetype) is null or tbl.price_component_value_type_id in (:valuetype))
                      and ((:numbertype) is null or text(tbl.number_type) in (:numbertype))
                      and ((:conditions) is null or text(tbl.conditions) in (:conditions))
                      and (coalesce(:available, '0') = '0'
                        or
                           (:available = 'YES' and tbl.group_product_service_name is null)
                        or
                           (:available = 'NO' and tbl.group_product_service_name is not null)
                        )
                      and ((:statuses) is null or text(tbl.status) in (:statuses))
                    """,
            countQuery = """
                     select count(1)
                     from
                     (select pc.id,
                                  pc.name    as                                                price_component_name,
                                  pcpt.name  as                                                price_component_price_type,
                                  pcvt.name  as                                                price_component_value_type,
                                  pc.number_type,
                                  pc.status  as                                                status,
                                  (select coalesce(
                                                  (select pcgd.name
                                                   from price_component.price_component_group_details pcgd
                                                            join price_component.pc_group_pcs pgp
                                                                 on pgp.price_component_group_detail_id = pcgd.id
                                                   where pgp.price_component_id = pc.id
                                                     and pgp.status = 'ACTIVE'
                                                     and pc.status = 'ACTIVE'
                                                   limit 1),
                                                  (select pd.name
                                                   from product.product_details pd
                                                            join product.product_price_components ppc on ppc.product_detail_id = pd.id
                                                   where ppc.price_component_id = pc.id
                                                     and ppc.status = 'ACTIVE'
                                                     and pd.status = 'ACTIVE'
                                                   limit 1),
                                                  (select sd.name
                                                   from service.service_details sd
                                                            join service.service_price_components spc on spc.service_detail_id = sd.id
                                                   where spc.price_component_id = pc.id
                                                     and spc.status = 'ACTIVE'
                                                     and sd.status = 'ACTIVE'
                                                   limit 1),
                                                  (select iap.name
                                                   from interim_advance_payment.interim_advance_payments iap
                                                   where iap.price_component_id = pc.id
                                                     and iap.status = 'ACTIVE'
                                                   limit 1)
                                          )) as                                                group_product_service_name,
                                  case when pc.conditions is NOT null then 'YES' else 'NO' end conditions,
                                  pc.price_formula,
                                  pc.create_date,
                                  pc.price_component_price_type_id,
                                  pc.price_component_value_type_id
                           from price_component.price_components pc
                                    join
                                nomenclature.price_component_price_types pcpt
                                on pc.price_component_price_type_id = pcpt.id
                                    join
                                nomenclature.price_component_value_types pcvt
                                on pc.price_component_value_type_id = pcvt.id) as tbl
                     where (:columnname is null or (:columnname = 'ALL' and (lower(tbl.price_component_name) like :columnvalue))
                         or
                            (
                                :columnname = 'PRICECOMPONENTNAME' and lower(tbl.price_component_name) like :columnvalue
                                ))
                       and ((:pricetype) is null or tbl.price_component_price_type_id in (:pricetype))
                       and ((:valuetype) is null or tbl.price_component_value_type_id in (:valuetype))
                       and ((:numbertype) is null or text(tbl.number_type) in (:numbertype))
                       and ((:conditions) is null or text(tbl.conditions) in (:conditions))
                       and (coalesce(:available, '0') = '0'
                         or
                            (:available = 'YES' and tbl.group_product_service_name is null)
                         or
                            (:available = 'NO' and tbl.group_product_service_name is not null)
                         )
                       and ((:statuses) is null or text(tbl.status) in (:statuses))
                    """
    )
    Page<PriceComponentMiddleResponse> filter(
            @Param("columnvalue") String columnvalue,
            @Param("columnname") String columnname,
            @Param("valuetype") List<Long> valueTypeIds,
            @Param("pricetype") List<Long> priceTypeIds,
            @Param("numbertype") List<String> numberType,
            @Param("available") String available,
            @Param("conditions") List<String> conditions,
            @Param("statuses") List<String> statuses,
            Pageable pageable
    );

    @Query("""
                select new bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse(
                    p.id, 
                    concat(p.name, ' (', p.id, ')')
                )
                from PriceComponent p
                    where (:prompt is null or lower(p.name) like :prompt or cast(p.id as string) = :prompt)
                    and p.status in (:statuses)
                order by p.id DESC
            """)
    Page<CopyDomainListResponse> filterForCopy(
            @Param("prompt") String prompt,
            @Param("statuses") List<PriceComponentStatus> statuses,
            Pageable pageable
    );


    @Query(
            value = """
                    select count (pd.id) > 0 from ProductDetails pd
                    join ProductPriceComponents ppc on pd.id = ppc.productDetails.id
                        where pd.product.productStatus in (:productStatuses)
                        and ppc.productSubObjectStatus in (:ppcStatuses)
                        and ppc.priceComponent.id = :pcId
                    """
    )
    boolean hasConnectionToProduct(
            @Param("pcId") Long pcId,
            @Param("productStatuses") List<ProductStatus> productStatuses,
            @Param("ppcStatuses") List<ProductSubObjectStatus> ppcStatuses
    );


    @Query(
            value = """
                    select count (sd.id) > 0 from ServiceDetails sd
                    join ServicePriceComponent spc on sd.id = spc.serviceDetails.id
                        where sd.service.status in (:serviceStatuses)
                        and spc.status in (:spcStatuses)
                        and spc.priceComponent.id = :pcId
                    """
    )
    boolean hasConnectionToService(
            @Param("pcId") Long pcId,
            @Param("serviceStatuses") List<ServiceStatus> serviceStatuses,
            @Param("spcStatuses") List<ServiceSubobjectStatus> spcStatuses
    );


    @Query(
            value = """
                    select count(iap.id) > 0 from InterimAdvancePayment iap
                        where iap.priceComponent.id = :pcId
                        and iap.status in (:statuses)
                    """
    )
    boolean hasConnectionToInterimAndAdvancePayment(
            @Param("pcId") Long pcId,
            @Param("statuses") List<InterimAdvancePaymentStatus> statuses
    );

    @Query("""
            select new bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse(pc.id, pc.name) from PriceComponent pc
            where pc.status = 'ACTIVE'
            and pc.priceComponentGroupDetailId is null
            and not exists
            (
              select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
              join ProductPriceComponents ppc on 
              ppc.productDetails.id = pd.id
              and
              ppc.priceComponent.id = pc.id
              where p.productStatus = 'ACTIVE'
              and ppc.productSubObjectStatus = 'ACTIVE'
            )
            and not exists
            (
              select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
              join ServicePriceComponent spc on 
              spc.serviceDetails.id = sd.id
              and
              spc.priceComponent.id = pc.id
              where s.status = 'ACTIVE'
              and spc.status = 'ACTIVE'
            )
            and not exists
            (
              select 1 from InterimAdvancePayment iap
              where iap.priceComponent.id = pc.id
              and iap.status = 'ACTIVE'
            )
            and (:prompt is null or concat(lower(pc.name), pc.id) like :prompt)
            order by pc.createDate desc
            """)
    Page<AvailableProductRelatedEntitiesResponse> findAvailablePriceComponentsForProduct(@Param("prompt") String prompt, PageRequest pageRequest);


    @Query("""
            select pc.id from PriceComponent pc
            where pc.status = 'ACTIVE'
            and pc.priceComponentGroupDetailId is null
            and not exists
            (
              select 1 from Product p
              join ProductDetails pd on pd.product.id = p.id
              join ProductPriceComponents ppc on 
              ppc.productDetails.id = pd.id
              and
              ppc.priceComponent.id = pc.id
              where p.productStatus = 'ACTIVE'
              and ppc.productSubObjectStatus = 'ACTIVE'
            )
            and not exists
            (
              select 1 from EPService s
              join ServiceDetails sd on sd.service.id = s.id
              join ServicePriceComponent spc on 
              spc.serviceDetails.id = sd.id
              and
              spc.priceComponent.id = pc.id
              where s.status = 'ACTIVE'
              and spc.status = 'ACTIVE'
            )
            and not exists
            (
              select 1 from InterimAdvancePayment iap
              where iap.priceComponent.id = pc.id
              and iap.status = 'ACTIVE'
            )
            and pc.id in (:ids)
            """)
    List<Long> findAvailablePriceComponentIdsForProduct(Collection<Long> ids);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.service.AvailableServiceRelatedEntitiyResponse(
                        pc.id,
                        pc.name
                    )
                    from PriceComponent pc
                        where pc.status = 'ACTIVE'
                        and pc.priceComponentGroupDetailId is null
                        and not exists
                        (
                          select 1 from Product p
                          join ProductDetails pd on pd.product.id = p.id
                          join ProductPriceComponents ppc on 
                          ppc.productDetails.id = pd.id
                          and
                          ppc.priceComponent.id = pc.id
                          where p.productStatus = 'ACTIVE'
                          and ppc.productSubObjectStatus = 'ACTIVE'
                        )
                        and not exists
                        (
                          select 1 from EPService s
                          join ServiceDetails sd on sd.service.id = s.id
                          join ServicePriceComponent spc on 
                          spc.serviceDetails.id = sd.id
                          and
                          spc.priceComponent.id = pc.id
                          where s.status = 'ACTIVE'
                          and spc.status = 'ACTIVE'
                        )
                        and not exists(
                            select 1 from InterimAdvancePayment iap
                                where iap.priceComponent.id = pc.id
                                and iap.status = 'ACTIVE'
                        )
                        and (:prompt is null or concat(lower(pc.name), pc.id) like :prompt)
                    order by pc.createDate desc
                    """
    )
    Page<AvailableServiceRelatedEntitiyResponse> findAvailablePriceComponentsForService(
            @Param("prompt") String prompt,
            Pageable pageable
    );


    @Query(
            value = """
                    select pc.id from PriceComponent pc
                        where pc.status = 'ACTIVE'
                        and pc.priceComponentGroupDetailId is null
                        and not exists
                        (
                          select 1 from Product p
                          join ProductDetails pd on pd.product.id = p.id
                          join ProductPriceComponents ppc on 
                          ppc.productDetails.id = pd.id
                          and
                          ppc.priceComponent.id = pc.id
                          where p.productStatus = 'ACTIVE'
                          and ppc.productSubObjectStatus = 'ACTIVE'
                        )
                        and not exists
                        (
                          select 1 from EPService s
                          join ServiceDetails sd on sd.service.id = s.id
                          join ServicePriceComponent spc on 
                          spc.serviceDetails.id = sd.id
                          and
                          spc.priceComponent.id = pc.id
                          where s.status = 'ACTIVE'
                          and spc.status = 'ACTIVE'
                        )
                        and not exists(
                            select 1 from InterimAdvancePayment iap
                                where iap.priceComponent.id = pc.id
                                and iap.status = 'ACTIVE'
                        )
                        and pc.id in (:ids)
                    """
    )
    List<Long> findAvailablePriceComponentIdsForService(Collection<Long> ids);

    List<PriceComponent> findByPriceFormulaContaining(String id);


    @Query(
            nativeQuery = true,
            value = """
                    select pc.id
                    from service.service_price_component_groups spcg
                    join price_component.price_component_groups pcg on spcg.price_component_group_id = pcg.id
                    join price_component.price_component_group_details pcgd on pcgd.price_component_group_id = pcg.id
                    join price_component.price_components pc on pcgd.id = pc.price_component_group_detail_id
                        where spcg.service_detail_id = :serviceDetailId
                        and spcg.status = 'ACTIVE'
                        and pcg.status = 'ACTIVE'
                        and pc.status = 'ACTIVE'
                        and pcgd.start_date = (
                            select max(start_date) from price_component.price_component_group_details dt
                            where dt.price_component_group_id = pcgd.price_component_group_id
                            and dt.start_date < now()
                        )
                    order by pc.id
                    """
    )
    List<Long> getPriceComponentsFromCurrentServicePriceComponentGroup(
            @Param("serviceDetailId") Long serviceDetailId
    );


    List<PriceComponent> findByIdIn(List<Long> ids);

    @Query("""
            select pc
            from PriceComponent pc
            join PriceComponentGroupPriceComponent pcgpc on pcgpc.priceComponentId = pc.id
            join PriceComponentGroupDetails pcgd on pcgpc.priceComponentGroupDetailId = pcgd.id
            join PriceComponentGroup pcg on pcg.id = pcgd.priceComponentGroupId
            where pcg.id in(:priceComponentGroupIds)
            """)
    List<PriceComponent> findPriceComponentByPriceComponentGroupIds(@Param("priceComponentGroupIds") Collection<Long> priceComponentGroupIds);

    @Query("""
            select pcfv
            from PriceComponent pc
            join ProductPriceComponents ppc on ppc.priceComponent.id = pc.id
            join PriceComponentFormulaVariable pcfv on pcfv.priceComponent.id = pc.id
            where ppc.productSubObjectStatus = 'ACTIVE'
            and pc.status = 'ACTIVE'
            and ppc.productDetails.id = :productDetailId
            and pc.xenergieApplicationType = 'CONSUMER'
            """)
    List<PriceComponentFormulaVariable> findConsumerPriceComponentFormulaVariablesByProductDetailId(Long productDetailId);

    @Query("""
            select pcfv
            from PriceComponent pc
            join ProductPriceComponents ppc on ppc.priceComponent.id = pc.id
            join PriceComponentFormulaVariable pcfv on pcfv.priceComponent.id = pc.id
            where ppc.productSubObjectStatus = 'ACTIVE'
            and pc.status = 'ACTIVE'
            and ppc.productDetails.id = :productDetailId
            and pc.xenergieApplicationType = 'GENERATOR'
            """)
    List<PriceComponentFormulaVariable> findGeneratorPriceComponentFormulaVariablesByProductDetailId(Long productDetailId);


    @Query("""
                    select new bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponents(pcd.id,pcomp.id,pcomp.issuedSeparateInvoice,am.applicationType,am.id)
                    from ProductContract pc
                    join ProductContractDetails pcd on pcd.contractId=pc.id
                    join ProductDetails pd on pd.id=pcd.productDetailId
                    join ProductPriceComponents ppc on ppc.productDetails.id=pd.id
                    join PriceComponent pcomp on pcomp.id=ppc.priceComponent.id
                    join ApplicationModel am on am.priceComponent.id=pcomp.id
                    where pc.id=:contractId
                    and ppc.productSubObjectStatus = 'ACTIVE'
                    and am.status='ACTIVE'
            """)
    List<BillingDataPriceComponents> findBillingPriceComponentsForContract(Long contractId);


    @Query(
            nativeQuery = true,
            value =
                    """
                            select coalesce(max('true'),'false') as is_connected from price_component.price_components pc
                                      where pc.id = :pricecomponenttid and
                                       (exists (select 1
                                                from product.products p
                                                     join product.product_details pd
                                                       on pd.product_id = p.id
                                                      and p.status = 'ACTIVE'
                                                     join product.product_price_components ppc
                                                       on ppc.product_detail_id = pd.id
                                                      and ppc.price_component_id = pc.id
                                                      and ppc.status = 'ACTIVE'
                                                     join product_contract.contract_details cd
                                                       on cd.product_detail_id =  pd.id
                                                     join product_contract.contracts c
                                                       on cd.contract_id =  c.id
                                                      and c.status = 'ACTIVE')
                                        or
                                        exists
                                                (select 1
                                                            from product.products p
                                                                 join product.product_details pd
                                                                   on pd.product_id = p.id
                                                                  and p.status = 'ACTIVE'
                                                                 join product.product_price_component_groups ppcg
                                                                   on ppcg.product_detail_id = pd.id
                                                                  and ppcg.status = 'ACTIVE'
                                                                 join price_component.price_component_groups pcg
                                                                   on ppcg.price_component_group_id = pcg.id
                                                                  and pcg.status = 'ACTIVE'
                                                                 join price_component.price_component_group_details pcgd
                                                                   on pcgd.price_component_group_id = pcg.id
                                                                 join price_component.pc_group_pcs pgp 
                                                                   on pgp.price_component_group_detail_id = pcgd.id
                                                                  and pgp.price_component_id =  pc.id
                                                                  and pgp.status = 'ACTIVE'
                                                                 join product_contract.contract_details cd
                                                                   on cd.product_detail_id =  pd.id
                                                                 join product_contract.contracts c
                                                                   on cd.contract_id =  c.id
                                                                  and c.status = 'ACTIVE')
                                        or
                                        exists (select 1
                                                    from service.services s
                                                         join service.service_details sd
                                                           on sd.service_id = s.id
                                                          and s.status = 'ACTIVE'
                                                         join service.service_price_components spc
                                                           on spc.service_detail_id = sd.id
                                                          and spc.price_component_id = pc.id
                                                          and spc.status = 'ACTIVE'                  
                                                         join service_contract.contract_details cd
                                                           on cd.service_detail_id =  sd.id
                                                         join service_contract.contracts c
                                                           on cd.contract_id =  c.id
                                                          and c.status = 'ACTIVE')
                                        or
                                        exists (select 1
                                                    from service.services s
                                                         join service.service_details sd
                                                           on sd.service_id = s.id
                                                          and s.status = 'ACTIVE'
                                                         join service.service_price_components spc
                                                           on spc.service_detail_id = sd.id
                                                          and spc.price_component_id = pc.id
                                                          and spc.status = 'ACTIVE'
                                                         join service_order.orders o
                                                           on o.service_detail_id =  sd.id
                                                          and o.status = 'ACTIVE'
                                                       )
                                        or
                                        exists
                                                (select 1
                                                            from service.services s
                                                                 join service.service_details sd
                                                                   on sd.service_id = s.id
                                                                  and s.status = 'ACTIVE'
                                                                 join service.service_price_component_groups spcg
                                                                   on spcg.service_detail_id = sd.id
                                                                  and spcg.status = 'ACTIVE'
                                                                 join price_component.price_component_groups pcg
                                                                   on spcg.price_component_group_id = pcg.id
                                                                  and pcg.status = 'ACTIVE'
                                                                 join price_component.price_component_group_details pcgd
                                                                   on pcgd.price_component_group_id = pcg.id
                                                                 join price_component.pc_group_pcs pgp
                                                                   on pgp.price_component_group_detail_id = pcgd.id
                                                                  and pgp.price_component_id =  pc.id
                                                                  and pgp.status = 'ACTIVE'                      
                                                                 join service_contract.contract_details cd
                                                                   on cd.service_detail_id =  sd.id
                                                                 join service_contract.contracts c
                                                                   on cd.contract_id = c.id
                                                                  and c.status = 'ACTIVE'
                                                                  )
                                       or
                                        exists
                                                (select 1
                                                            from service.services s
                                                                 join service.service_details sd
                                                                   on sd.service_id = s.id
                                                                  and s.status = 'ACTIVE'
                                                                 join service.service_price_component_groups spcg
                                                                   on spcg.service_detail_id = sd.id
                                                                  and spcg.status = 'ACTIVE'
                                                                 join price_component.price_component_groups pcg
                                                                   on spcg.price_component_group_id = pcg.id
                                                                  and pcg.status = 'ACTIVE'
                                                                 join price_component.price_component_group_details pcgd
                                                                   on pcgd.price_component_group_id = pcg.id
                                                                 join price_component.pc_group_pcs pgp
                                                                   on pgp.price_component_group_detail_id = pcgd.id
                                                                  and pgp.price_component_id =  pc.id
                                                                  and pgp.status = 'ACTIVE'
                                                                 join service_order.orders o
                                                                   on o.service_detail_id =  sd.id
                                                                  and o.status = 'ACTIVE'
                                                                  )   
                                                                 
                                       )                                                    """
    )
    boolean hasLockedConnection(
            @Param("pricecomponenttid") Long id
    );

    @Query("""
            select distinct pc.contractTemplateTag
            from PriceComponent pc
            where (pc.status='ACTIVE' and pc.contractTemplateTag is not null)
            and (:columnname is null or (lower(pc.contractTemplateTag) like :columnvalue))
             """)
    Page<String> filterTags(
            @Param("columnvalue") String prompt,
            @Param("columnname") String searchField,
            Pageable pageable
    );

    @Query("""
            select new bg.energo.phoenix.model.response.contract.action.calculation.PriceComponentFormulaDto(pc.id, pc.priceFormula)
            from PriceComponent pc
            where pc.id in (:ids) and pc.contractTemplateTag=:tag and pc.priceFormula not like '%PRICE_PROFILE%'
             """)
    List<PriceComponentFormulaDto> getByTagWithoutPriceProfileFormulaAndIdIn(
            @Param("ids") Set<Long> priceComponentIds,
            @Param("tag") String priceComponentTag
    );
    @Query(value = """
            select pc.id                                       as id,
                   pc.issued_separate_invoice                  as issuedSeparateInvoice,
                   pc.price_formula                            as priceFormula,
                   pc.price_component_value_type_id            as priceComponentValueTypeId,
                   pc.price_component_price_type_id            as priceComponentPriceTypeId,
                   pc.vat_rate_id                              as vatRateId,
                   pc.income_account_number                    as incomeAccountNumber,
                   pc.cost_center_controlling_order            as costCenterControllingOrder,
                   cd.id                                       as contractDetailId,
                   cd.service_detail_id                        as serviceOrProductDetailId,
                   case
                       when pc.global_vat_rate is true then (select vr.value_in_percent
                                                             from nomenclature.vat_rates vr
                                                             where vr.status = 'ACTIVE'
                                                               and vr.global_vat_rate is true
                                                               and vr.start_date = (select max(innerVr.start_date)
                                                                                    from nomenclature.vat_rates innerVr
                                                                                    where innerVr.start_date <= current_date
                                                                                      and innerVr.status = 'ACTIVE'
                                                                                      and innerVr.global_vat_rate = true))
                       else (select vr.value_in_percent
                             from nomenclature.vat_rates vr
                             where vr.id = pc.vat_rate_id) end as vatRatePercent
            from service_contract.contracts c
                     join service_contract.contract_details cd
                          on cd.contract_id = c.id
                              and c.id = :contractId
                              and c.status = 'ACTIVE'
                     join service.service_price_components spc
                          on spc.service_detail_id = cd.service_detail_id
                              and spc.status = 'ACTIVE'
                     join price_component.price_components pc
                          on pc.id = spc.price_component_id
                              and pc.status = 'ACTIVE'
            union
            select id,
                   issued_separate_invoice,
                   price_formula,
                   price_component_value_type_id,
                   price_component_price_type_id,
                   vat_rate_id,
                   income_account_number,
                   cost_center_controlling_order,
                   cdId,
                   service_detail_id,
                   vat_rate_percent
            from (select pc.id,
                         pc.issued_separate_invoice,
                         pc.price_formula,
                         pc.price_component_value_type_id,
                         pc.price_component_price_type_id,
                         pc.vat_rate_id,
                         pc.income_account_number,
                         pc.cost_center_controlling_order,
                         cd.id                                                                                 as cdId,
                         cd.service_detail_id,
                         case
                             when pc.global_vat_rate is true then (select vr.value_in_percent
                                                                   from nomenclature.vat_rates vr
                                                                   where vr.status = 'ACTIVE'
                                                                     and vr.global_vat_rate is true
                                                                     and vr.start_date = (select max(innerVr.start_date)
                                                                                          from nomenclature.vat_rates innerVr
                                                                                          where innerVr.start_date <= current_date
                                                                                            and innerVr.status = 'ACTIVE'
                                                                                            and innerVr.global_vat_rate = true))
                             else (select vr.value_in_percent
                                   from nomenclature.vat_rates vr
                                   where vr.id = pc.vat_rate_id) end                                           as vat_rate_percent,
                         pcgd.start_date,
                         coalesce(lead(pcgd.start_date, 1) OVER (order by pcgd.start_date), date '9999-12-31') as next_date
                  from service_contract.contracts c
                           join service_contract.contract_details cd
                                on cd.contract_id = c.id
                                    and c.id = :contractId
                                    and c.status = 'ACTIVE'
                           join service.service_price_component_groups spcg
                                on spcg.service_detail_id = cd.service_detail_id
                                    and spcg.status = 'ACTIVE'
                           join price_component.price_component_groups pcg
                                on spcg.price_component_group_id = pcg.id
                                    and pcg.status = 'ACTIVE'
                           join price_component.price_component_group_details pcgd
                                on pcgd.price_component_group_id = pcg.id
                           join price_component.pc_group_pcs pgp
                                on pgp.price_component_group_detail_id = pcgd.id
                                    and pgp.status = 'ACTIVE'
                           join price_component.price_components pc
                                on pc.id = pgp.price_component_id
                                    and pc.status = 'ACTIVE'
                  where pcgd.start_date >=
                        (select max(start_date)
                         from price_component.price_component_group_details tt
                         where tt.price_component_group_id
                             = pcgd.price_component_group_id
                           and start_date < current_date)) as pc_group
            where current_date between pc_group.start_date and pc_group.next_date - 1
            """, nativeQuery = true)
    List<PriceComponentForOvertimeResponse> getPriceComponentsByServiceContractId(@Param("contractId") Long contractId);

    @Query(value = """
            select pc.id                                       as id,
                   pc.issued_separate_invoice                  as issuedSeparateInvoice,
                   pc.price_formula                            as priceFormula,
                   pc.price_component_value_type_id            as priceComponentValueTypeId,
                   pc.price_component_price_type_id            as priceComponentPriceTypeId,
                   pc.vat_rate_id                              as vatRateId,
                   pc.income_account_number                    as incomeAccountNumber,
                   pc.cost_center_controlling_order            as costCenterControllingOrder,
                   cd.id                                       as contractDetailId,
                   cd.product_detail_id                        as serviceOrProductDetailId,
                   case
                       when pc.global_vat_rate is true then (select vr.value_in_percent
                                                             from nomenclature.vat_rates vr
                                                             where vr.status = 'ACTIVE'
                                                               and vr.global_vat_rate is true
                                                               and vr.start_date = (select max(innerVr.start_date)
                                                                                    from nomenclature.vat_rates innerVr
                                                                                    where innerVr.start_date <= current_date
                                                                                      and innerVr.status = 'ACTIVE'
                                                                                      and innerVr.global_vat_rate = true))
                       else (select vr.value_in_percent
                             from nomenclature.vat_rates vr
                             where vr.id = pc.vat_rate_id) end as vatRatePercent
            from product_contract.contracts c
                     join product_contract.contract_details cd
                          on cd.contract_id = c.id
                              and c.id = :contractId
                              and c.status = 'ACTIVE'
                     join product.product_price_components ppc
                          on ppc.product_detail_id = cd.product_detail_id
                              and ppc.status = 'ACTIVE'
                     join price_component.price_components pc
                          on pc.id = ppc.price_component_id
                              and pc.status = 'ACTIVE'
            union
            select id,
                   issued_separate_invoice,
                   price_formula,
                   price_component_value_type_id,
                   price_component_price_type_id,
                   vat_rate_id,
                   income_account_number,
                   cost_center_controlling_order,
                   cdId,
                   product_detail_id,
                   vat_rate_percent
            from (select pc.id,
                         pc.issued_separate_invoice,
                         pc.price_formula,
                         pc.price_component_value_type_id,
                         pc.price_component_price_type_id,
                         pc.vat_rate_id,
                         pc.income_account_number,
                         pc.cost_center_controlling_order,
                         cd.id                                                                                 as cdId,
                         cd.product_detail_id,
                         case
                             when pc.global_vat_rate is true then (select vr.value_in_percent
                                                                   from nomenclature.vat_rates vr
                                                                   where vr.status = 'ACTIVE'
                                                                     and vr.global_vat_rate is true
                                                                     and vr.start_date = (select max(innerVr.start_date)
                                                                                          from nomenclature.vat_rates innerVr
                                                                                          where innerVr.start_date <= current_date
                                                                                            and innerVr.status = 'ACTIVE'
                                                                                            and innerVr.global_vat_rate = true))
                             else (select vr.value_in_percent
                                   from nomenclature.vat_rates vr
                                   where vr.id = pc.vat_rate_id) end                                           as vat_rate_percent,
                         pcgd.start_date,
                         coalesce(lead(pcgd.start_date, 1) OVER (order by pcgd.start_date), date '9999-12-31') as next_date
                  from product_contract.contracts c
                           join product_contract.contract_details cd
                                on cd.contract_id = c.id
                                    and c.id = :contractId
                                    and c.status = 'ACTIVE'
                           join product.product_price_component_groups ppcg
                                on ppcg.product_detail_id = cd.product_detail_id
                                    and ppcg.status = 'ACTIVE'
                           join price_component.price_component_groups pcg
                                on ppcg.price_component_group_id = pcg.id
                                    and pcg.status = 'ACTIVE'
                           join price_component.price_component_group_details pcgd
                                on pcgd.price_component_group_id = pcg.id
                           join price_component.pc_group_pcs pgp
                                on pgp.price_component_group_detail_id = pcgd.id
                                    and pgp.status = 'ACTIVE'
                           join price_component.price_components pc
                                on pc.id = pgp.price_component_id
                                    and pc.status = 'ACTIVE'
                  where pcgd.start_date >=
                        (select max(start_date)
                         from price_component.price_component_group_details tt
                         where tt.price_component_group_id
                             = pcgd.price_component_group_id
                           and start_date < current_date)) as pc_group
            where current_date between pc_group.start_date and pc_group.next_date - 1
            """, nativeQuery = true)
    List<PriceComponentForOvertimeResponse> getPriceComponentsByProductContractId(@Param("contractId") Long contractId);

    @Query(value = """
            with per_piece_ranges as (select amId,
                                             string_agg(valFrom || '-' || valTo, ';') as ranges
                                      from (select amppr.application_model_id as amId,
                                                   amppr.value_from           as valFrom,
                                                   amppr.value_to             as valTo
                                            from price_component.am_per_piece_ranges amppr
                                            where amppr.status = 'ACTIVE'
                                            order by amppr.value_from) as aIvFvT
                                      group by amId)
            select id                         as id,
                   issuedSeparateInvoice      as issuedSeparateInvoice,
                   priceFormula               as priceFormula,
                   priceComponentValueTypeId  as priceComponentValueTypeId,
                   priceComponentPriceTypeId  as priceComponentPriceTypeId,
                   incomeAccountNumber        as incomeAccountNumber,
                   costCenterControllingOrder as costCenterControllingOrder,
                   serviceDetailId            as serviceDetailId,
                   vatRateId                  as vatRateId,
                   vatRatePercent             as vatRatePercent,
                   applicationType            as applicationType,
                   applicationModelType       as applicationModelType,
                   applicationModelId         as applicationModelId,
                   conditions                 as conditions,
                   pcGroupDetailId            as pcGroupDetailId,
                   serviceUnitId              as serviceUnitId,
                   perPieceRanges             as perPieceRanges,
                   applicationLevel           as applicationLevel,
                   numberType                 as numberType,
                   noPodCondition             as noPodCondition,
                   currencyId                 as currencyId,
                   valueTypeId                as valueTypeId
            from (select pc.id                                                                              as id,
                         pc.issued_separate_invoice                                                         as issuedSeparateInvoice,
                         pc.price_formula                                                                   as priceFormula,
                         pc.price_component_value_type_id                                                   as priceComponentValueTypeId,
                         pc.price_component_price_type_id                                                   as priceComponentPriceTypeId,
                         pc.income_account_number                                                           as incomeAccountNumber,
                         pc.cost_center_controlling_order                                                   as costCenterControllingOrder,
                         sd.id                                                                              as serviceDetailId,
                         coalesce(vr.id, vr2.id)                                                            as vatRateId,
                         case when vr.id is not null then vr.value_in_percent else vr2.value_in_percent end as vatRatePercent,
                         am.application_type                                                                as applicationType,
                         am.application_model_type                                                          as applicationModelType,
                         am.id                                                                              as applicationModelId,
                         pc.conditions                                                                      as conditions,
                         case
                             when am.application_type = 'PERIODICALLY'
                                 then billing.check_over_time_periodicity(aotp.id, current_date) end           periodicity_is_valid,
                         null                                                                               as pcGroupDetailId,
                         sd.service_unit_id                                                                 as serviceUnitId,
                         ppr.ranges                                                                         as perPieceRanges,
                         am.application_level                                                               as applicationLevel,
                         pc.number_type                                                                     as numberType,
                         pc.conditions not similar to
                         '%(POD_COUNTRY|POD_REGION|POD_POPULATED_PLACE|HOUSEHOLD|PURPOSE_OF_CONSUMPTION|POD_VOLTAGE_LEVEL|POD_GRID_OP|POD_ADDITIONAL_PARAMETER|POD_MEASUREMENT_TYPE|PURPOSE_OF_CONSUMPTION|POD_PROVIDED_POWER|POD_MULTIPLIER)%'
                                                                                                            as noPodCondition,
                         pc.currency_id                                                                     as currencyId,
                         pc.price_component_value_type_id                                                   as valueTypeId
                  from service_order.orders so
                           join service.service_details sd
                                on sd.id = so.service_detail_id
                           join service.services s
                                on sd.service_id = s.id
                                    and s.status = 'ACTIVE'
                           join service.service_price_components spc
                                on spc.service_detail_id = sd.id
                                    and spc.status = 'ACTIVE'
                           join price_component.price_components pc
                                on pc.id = spc.price_component_id
                                    and pc.status = 'ACTIVE'
                           join price_component.application_models am on (pc.id = am.price_component_id and am.status = 'ACTIVE')
                           left join nomenclature.vat_rates vr
                                     on (pc.global_vat_rate is true and vr.status = 'ACTIVE' and
                                         vr.start_date = (select max(innerVr.start_date)
                                                          from nomenclature.vat_rates innerVr
                                                          where innerVr.start_date <= current_date
                                                            and innerVr.status = 'ACTIVE'
                                                            and innerVr.global_vat_rate = true))
                           left join nomenclature.vat_rates vr2 on (pc.vat_rate_id = vr2.id)
                           left join price_component.am_per_piece_ranges amppr
                                     on (am.id = amppr.application_model_id and amppr.status = 'ACTIVE')
                           left join price_component.am_over_time_periodically aotp
                                     on (am.id = aotp.application_model_id)
                           left join per_piece_ranges ppr on ppr.amId = am.id
                  where (
                      ((am.application_model_type = 'PRICE_AM_PER_PIECE')
                          and so.quantity >= cast(split_part(ppr.ranges, '-', 1) as integer))
                          or
                      (am.application_model_type = 'PRICE_AM_OVERTIME' and
                       am.application_type = 'PERIODICALLY')
                      )
                    and (pc.conditions is null
                      or
                         pc.conditions not similar to
                         '%(RISK_ASSESSMENT_ADDITIONAL_CONDITIONS|CONTRACT_CAMPAIGN|CONTRACT_TYPE|CONTRACT_SUB_STATUS_IN_PERPETUITY|ACTIVE_POWER_SUPPLY_TERMINATION|MONTHS_DIFFERENCE_BETWEEN_CURRENT_DATE_CONTRACT_ACTIVATION_DATE|MONTHS_DIFFERENCE_BETWEEN_MAX_DATE_OF_BILLING_RUN_CONTRACT_ACTIVATION_DATE)%')
                    and so.id = :orderId
                  union
                  select id,
                         issuedSeparateInvoice,
                         priceFormula,
                         priceComponentValueTypeId,
                         priceComponentPriceTypeId,
                         incomeAccountNumber,
                         costCenterControllingOrder,
                         serviceDetailId,
                         vatRateId,
                         vatRatePercent,
                         applicationType,
                         applicationModelType,
                         applicationModelId,
                         conditions,
                         periodicity_is_valid,
                         pcGroupDetailId,
                         serviceUnitId,
                         perPieceRanges,
                         applicationLevel,
                         numberType,
                         noPodCondition,
                         currencyId,
                         valueTypeId
                  from (select pc.id                                                                              as id,
                               pc.issued_separate_invoice                                                         as issuedSeparateInvoice,
                               pc.price_formula                                                                   as priceFormula,
                               pc.price_component_value_type_id                                                   as priceComponentValueTypeId,
                               pc.price_component_price_type_id                                                   as priceComponentPriceTypeId,
                               pc.income_account_number                                                           as incomeAccountNumber,
                               pc.cost_center_controlling_order                                                   as costCenterControllingOrder,
                               sd.id                                                                              as serviceDetailId,
                               coalesce(vr.id, vr2.id)                                                            as vatRateId,
                               case when vr.id is not null then vr.value_in_percent else vr2.value_in_percent end as vatRatePercent,
                               am.application_type                                                                as applicationType,
                               am.application_model_type                                                          as applicationModelType,
                               am.id                                                                              as applicationModelId,
                               pc.conditions                                                                      as conditions,
                               case
                                   when am.application_type = 'PERIODICALLY'
                                       then billing.check_over_time_periodicity(aotp.id, current_date) end           periodicity_is_valid,
                               pcgd.id                                                                            as pcGroupDetailId,
                               sd.service_unit_id                                                                 as serviceUnitId,
                               ppr.ranges                                                                         as perPieceRanges,
                               am.application_level                                                               as applicationLevel,
                               pc.number_type                                                                     as numberType,
                               pc.conditions not similar to
                               '%(POD_COUNTRY|POD_REGION|POD_POPULATED_PLACE|HOUSEHOLD|PURPOSE_OF_CONSUMPTION|POD_VOLTAGE_LEVEL|POD_GRID_OP|POD_ADDITIONAL_PARAMETER|POD_MEASUREMENT_TYPE|PURPOSE_OF_CONSUMPTION|POD_PROVIDED_POWER|POD_MULTIPLIER)%'
                                                                                                                  as noPodCondition,
                               pc.currency_id                                                                     as currencyId,
                               pc.price_component_value_type_id                                                   as valueTypeId
                        from service_order.orders so
                                 join service.service_details sd
                                      on sd.id = so.service_detail_id
                                 join service.services s
                                      on sd.service_id = s.id
                                          and s.status = 'ACTIVE'
                                 join service.service_price_component_groups spcg
                                      on spcg.service_detail_id = sd.id
                                          and spcg.status = 'ACTIVE'
                                 join price_component.price_component_groups pcg
                                      on spcg.price_component_group_id = pcg.id
                                          and pcg.status = 'ACTIVE'
                                 join price_component.price_component_group_details pcgd
                                      on pcgd.price_component_group_id = pcg.id
                                 join price_component.pc_group_pcs pgp
                                      on pgp.price_component_group_detail_id = pcgd.id
                                          and pgp.status = 'ACTIVE'
                                 join price_component.price_components pc
                                      on pc.id = pgp.price_component_id
                                          and pc.status = 'ACTIVE'
                                 join price_component.application_models am
                                      on (pc.id = am.price_component_id and am.status = 'ACTIVE')
                                 left join nomenclature.vat_rates vr
                                           on (pc.global_vat_rate is true and vr.status = 'ACTIVE' and
                                               vr.start_date = (select max(innerVr.start_date)
                                                                from nomenclature.vat_rates innerVr
                                                                where innerVr.start_date <= current_date
                                                                  and innerVr.status = 'ACTIVE'
                                                                  and innerVr.global_vat_rate = true))
                                 left join nomenclature.vat_rates vr2 on (pc.vat_rate_id = vr2.id)
                                 left join price_component.am_per_piece_ranges amppr
                                           on (am.id = amppr.application_model_id and amppr.status = 'ACTIVE')
                                 left join price_component.am_over_time_periodically aotp
                                           on (am.id = aotp.application_model_id)
                                 left join per_piece_ranges ppr on ppr.amId = am.id
                        where (
                            ((am.application_model_type = 'PRICE_AM_PER_PIECE')
                                and so.quantity >= cast(split_part(ppr.ranges, '-', 1) as integer))
                                or
                            (am.application_model_type = 'PRICE_AM_OVERTIME' and
                             am.application_type = 'PERIODICALLY')
                            )
                          and (pc.conditions is null
                            or
                               pc.conditions not similar to
                               '%(RISK_ASSESSMENT_ADDITIONAL_CONDITIONS|CONTRACT_CAMPAIGN|CONTRACT_TYPE|CONTRACT_SUB_STATUS_IN_PERPETUITY|ACTIVE_POWER_SUPPLY_TERMINATION|MONTHS_DIFFERENCE_BETWEEN_CURRENT_DATE_CONTRACT_ACTIVATION_DATE|MONTHS_DIFFERENCE_BETWEEN_MAX_DATE_OF_BILLING_RUN_CONTRACT_ACTIVATION_DATE)%')
                          and so.id = :orderId
                          and pcgd.start_date =
                              (select max(start_date)
                               from price_component.price_component_group_details tt
                               where tt.price_component_group_id
                                   = pcg.id
                                 and start_date <= current_date)) as pc_group) detail
            where true = (case when applicationType = 'PERIODICALLY' then periodicity_is_valid else true end)
            """, nativeQuery = true)
    List<PriceComponentForServiceOrderResponse> getValidPriceComponentsByServiceOrderId(@Param("orderId") Long orderId);
}
