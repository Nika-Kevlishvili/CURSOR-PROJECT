package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.Product;
import bg.energo.phoenix.model.entity.product.product.ProductContractProductListingMiddleResponse;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionParameterResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse;
import bg.energo.phoenix.model.response.product.ProductListResponse;
import bg.energo.phoenix.model.response.product.ProductShortResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductRepository extends JpaRepository<Product, Long> {

    //TODO this fields should be added to the query response
     /*   pct.name as product_contract_term_name,
          pct.name_desc as product_contract_term_name_desc,
          psc.name as product_sales_channel_name,
          psc.name_desc as product_sales_channel_name_desc,
          '_-_' as product_contract_template,*/
    @Query(value = """
            select new  bg.energo.phoenix.model.response.product.ProductListResponse(
                    p.id ,
                    pd.name ,
                    pg.name ,
                    p.productStatus ,
                    pd.productDetailStatus ,
                    pt.id ,
                    pt.name ,
                    pct.name ,
                    psc.name ,
                    p.createDate ,
                    pd.globalSalesChannel ,
                    (case when p.customerIdentifier is not null then 'Yes' else 'No' end) as individualProduct
            )
            from Product p
            join ProductDetails pd on p.lastProductDetail = pd.id
            left join ProductGroups pg on pd.productGroups.id = pg.id
            join ProductTypes pt on pd.productType.id = pt.id
            join VwProductContractTerms pct on pct.productDetailsId = pd.id
            left join VwProductSalesChannels psc on psc.productDetailId = pd.id
                where p.id in (
                     select p.id from Product p
                     join ProductDetails pd on pd.product.id = p.id
                        where (coalesce(:excludeOldVersion,'false') = 'false' or (:excludeOldVersion = 'true' and pd.id = p.lastProductDetail))
                        and (
                            (:individualproduct = 'ALL' and ((p.customerIdentifier is not null and p.productStatus in (:individualProductStatuses)) or (p.customerIdentifier is null and p.productStatus in (:standardProductStatuses))))
                            or (:individualproduct = 'YES' and (p.customerIdentifier is not null and p.productStatus in (:individualProductStatuses)))
                            or (:individualproduct = 'NO' and (p.customerIdentifier is null and p.productStatus in (:standardProductStatuses)))
                        )
                        and (coalesce(:detailsStatus,'0') = '0' or (pd.productDetailStatus in (:detailsStatus)))
                        and (coalesce(:group,'0') = '0'  or  pd.productGroups.id in (:group))
                        and (coalesce(:type ,'0') = '0' or  pd.productType.id in (:type))
                        and (coalesce(:contractterm ,'0') = '0' or exists(select 1 from ProductContractTerms pct
                            where pct.productDetailsId = pd.id
                            and pct.name in (:contractterm)
                            and pct.status = 'ACTIVE'))
                        and (
                             (coalesce(:saleschannel, '0') <> '0' and :globalSalesChannel is not null 
                                and exists(select 1 from ProductSalesChannel pch
                                where pch.productDetails.id = pd.id
                                and pch.salesChannel.id in :saleschannel
                                and pch.productSubObjectStatus = 'ACTIVE') or pd.globalSalesChannel = :globalSalesChannel)
                            or (coalesce(:saleschannel, '0') <> '0' and :globalSalesChannel is null 
                                and exists(select 1 from ProductSalesChannel pch
                                where pch.productDetails.id = pd.id
                                and pch.salesChannel.id in :saleschannel
                                and pch.productSubObjectStatus = 'ACTIVE'))
                            or (coalesce(:saleschannel, '0') = '0' and :globalSalesChannel is not null and pd.globalSalesChannel = :globalSalesChannel)
                            or (coalesce(:saleschannel, '0') = '0' and :globalSalesChannel is null )
                         )
                        and (
                             (coalesce(:segment, '0') <> '0' and :globalSegment is not null
                                 and exists(select 1 from ProductSegments ps
                                 where ps.productDetails.id = pd.id
                                 and ps.segment.id in :segment
                                 and ps.productSubObjectStatus = 'ACTIVE') or pd.globalSegment = :globalSegment)
                             or (coalesce(:segment, '0') <> '0' and :globalSegment is null 
                                 and exists(select 1 from ProductSegments ps
                                 where ps.productDetails.id = pd.id
                                 and ps.segment.id in :segment
                                 and ps.productSubObjectStatus = 'ACTIVE'))
                             or (coalesce(:segment, '0') = '0' and :globalSegment is not null and pd.globalSegment = :globalSegment)
                             or (coalesce(:segment, '0') = '0' and :globalSegment is null )
                        )
                        and (coalesce(:consumptiontype ,'0') = '0' or  arrays_intersect(pd.consumptionPurposes, :consumptiontype) = true)
                        and (:columnvalue is null or (:columnname =  'ALL' and (lower(pd.name) like  :columnvalue
                               or exists (select 1 from ProductGroups pg
                                           where pd.productGroups.id  = pg.id and lower(pg.name) like :columnvalue)
                               or exists (select 1 from ProductPenaltyGroups ppg
                                        join PenaltyGroup pg2 on ppg.penaltyGroup.id = pg2.id
                                        join PenaltyGroupDetails pgd on pgd.penaltyGroupId = pg2.id
                                            where ppg.productDetails.id = pd.id
                                            and ppg.productSubObjectStatus ='ACTIVE'
                                            and pg2.status = 'ACTIVE'
                                            and lower(pgd.name) like :columnvalue)
                               or exists(select 1 from ProductPenalty pp
                                          join Penalty p2 on pp.penalty.id = p2.id
                                          where pp.productDetails.id = pd.id
                                            and pp.productSubObjectStatus = 'ACTIVE'
                                            and p2.status = 'ACTIVE'
                                            and lower(p2.name) like :columnvalue)
                               or exists (select 1 from ProductTerminationGroups ptg
                                    join TerminationGroup tg on ptg.terminationGroup.id = tg.id
                                    join TerminationGroupDetails tgd on tgd.terminationGroupId = tg.id
                                        where ptg.productDetails.id  = pd.id
                                        and tg.status ='ACTIVE'
                                        and lower(tgd.name) like :columnvalue)
                               or exists(select 1 from ProductTerminations pt
                                      join Termination t on pt.termination.id = t.id
                                      where pt.productDetails.id = pd.id
                                           and pt.productSubObjectStatus = 'ACTIVE'
                                           and t.status = 'ACTIVE'
                                           and lower(t.name) like :columnvalue)
                               or exists(select 1 from Terms t2
                                          where pd.terms.id = t2.id
                                            and t2.status = 'ACTIVE'
                                            and lower(t2.name) like :columnvalue)
                               or exists (select 1 from TermsGroups t3
                                      join TermGroupDetails tgd on tgd.groupId = t3.id
                                           where pd.termsGroups.id = t3.id
                                           and t3.status = 'ACTIVE'
                                           and lower(tgd.name) like :columnvalue)                                            
                               or exists (select 1 from ProductPriceComponentGroups ppcg
                                    join PriceComponentGroup pcg on ppcg.priceComponentGroup.id = pcg.id
                                    join PriceComponentGroupDetails pcgd on pcgd.priceComponentGroupId  = pcg.id
                                        where ppcg.productDetails.id = pd.id
                                        and ppcg.productSubObjectStatus ='ACTIVE'
                                        and pcg.status ='ACTIVE'
                                        and lower(pcgd.name) like :columnvalue)
                               or exists(select 1 from ProductPriceComponents ppc
                                    join PriceComponent pc on ppc.priceComponent.id = pc.id
                                        where ppc.productDetails.id = pd.id
                                        and ppc.productSubObjectStatus = 'ACTIVE'
                                        and pc.status = 'ACTIVE'
                                        and lower(pc.name) like :columnvalue)
                              or exists(select 1 from ProductGroupOfInterimAndAdvancePayments piap
                                     join InterimAdvancePayment iap on piap.interimAdvancePaymentGroup.id = iap.id
                                         where piap.productDetails.id = pd.id
                                          and piap.productSubObjectStatus = 'ACTIVE'
                                          and iap.status = 'ACTIVE'
                                          and lower(iap.name) like :columnvalue)
                              or exists(select 1 from ProductGroupOfInterimAndAdvancePayments piapg
                                         join AdvancedPaymentGroup iapg on piapg.interimAdvancePaymentGroup.id = iapg.id
                                         join AdvancedPaymentGroupDetails iapgd on iapgd.advancedPaymentGroupId = iapg.id
                                              where piapg.productDetails.id = pd.id
                                                and piapg.productSubObjectStatus = 'ACTIVE'
                                               and iapg.status  = 'ACTIVE'
                                               and lower(iapgd.name) like :columnvalue)
                               )
                           )
                           or ((:columnname = 'NAME' and lower(pd.name) like :columnvalue)
                                or (:columnname = 'GROUP' and  exists (select 1 from ProductGroups pg
                                           where pd.productGroups.id  = pg.id and lower(pg.name) like :columnvalue))
                                or(:columnname = 'PENALTYGROUP' and  exists (select 1 from ProductPenaltyGroups ppg
                                    join PenaltyGroup pg2 on ppg.penaltyGroup.id = pg2.id
                                    join PenaltyGroupDetails pgd on pgd.penaltyGroupId = pg2.id
                                           where ppg.productDetails.id = pd.id
                                             and ppg.productSubObjectStatus ='ACTIVE'
                                             and pg2.status = 'ACTIVE'
                                            and lower(pgd.name) like :columnvalue))
                                or(:columnname = 'PENALTY' and  exists(select 1 from ProductPenalty pp
                                    join Penalty p2 on pp.penalty.id = p2.id
                                          where pp.productDetails.id = pd.id
                                           and pp.productSubObjectStatus = 'ACTIVE'
                                           and p2.status = 'ACTIVE'
                                           and lower(p2.name) like :columnvalue))
                                or(:columnname = 'TERMINATIONGROUP' and  exists (select 1 from ProductTerminationGroups ptg
                                    join TerminationGroup tg on ptg.terminationGroup.id = tg.id
                                    join TerminationGroupDetails tgd on tgd.terminationGroupId = tg.id
                                       where ptg.productDetails.id  = pd.id
                                         and tg.status ='ACTIVE'
                                        and lower(tgd.name) like :columnvalue))
                               or(:columnname = 'TERMINATION' and exists(select 1 from ProductTerminations pt
                                    join Termination t on pt.termination.id = t.id
                                      where pt.productDetails.id = pd.id
                                       and pt.productSubObjectStatus = 'ACTIVE'
                                       and t.status = 'ACTIVE'
                                       and lower(t.name) like :columnvalue))
                               or (:columnname = 'TERM' and  exists(select 1 from Terms t2
                                  where pd.terms.id = t2.id
                                   and t2.status = 'ACTIVE'
                                   and lower(t2.name) like :columnvalue))
                               or (:columnname = 'TERMS_GROUP_NAME' and exists (select 1 from TermsGroups t3
                                   join TermGroupDetails tgd on tgd.groupId = t3.id
                                       where pd.termsGroups.id = t3.id
                                       and t3.status = 'ACTIVE'
                                       and lower(tgd.name) like :columnvalue))
                               or (:columnname = 'PRICECOMPONENTGROUP' and  exists (select 1 from ProductPriceComponentGroups ppcg
                                    join PriceComponentGroup pcg on ppcg.priceComponentGroup.id = pcg.id
                                    join PriceComponentGroupDetails pcgd on pcgd.priceComponentGroupId  = pcg.id
                                       where ppcg.productDetails.id = pd.id
                                         and ppcg.productSubObjectStatus ='ACTIVE'
                                         and pcg.status ='ACTIVE'
                                        and lower(pcgd.name) like :columnvalue))
                               or (:columnname = 'PRICECOMPONENT' and  exists(select 1 from ProductPriceComponents ppc
                                  join PriceComponent pc on ppc.priceComponent.id = pc.id
                                      where ppc.productDetails.id = pd.id
                                       and ppc.productSubObjectStatus = 'ACTIVE'
                                       and pc.status = 'ACTIVE'
                                       and lower(pc.name) like :columnvalue))
                               or(:columnname = 'INTERIMADVANCEPAYMENT' and exists(select 1 from ProductInterimAndAdvancePayments piap
                                 join InterimAdvancePayment iap on piap.interimAdvancePayment.id = iap.id
                                     where piap.productDetails.id = pd.id
                                      and piap.productSubObjectStatus = 'ACTIVE'
                                      and iap.status = 'ACTIVE'
                                      and lower(iap.name) like :columnvalue))
                              or(:columnname = 'INTERIMADVANCEPAYMENTGROUP' and exists(select 1 from ProductGroupOfInterimAndAdvancePayments piapg
                                 join AdvancedPaymentGroup iapg on piapg.interimAdvancePaymentGroup.id = iapg.id
                                 join AdvancedPaymentGroupDetails iapgd on iapgd.advancedPaymentGroupId = iapg.id
                                      where piapg.productDetails.id = pd.id
                                       and piapg.productSubObjectStatus = 'ACTIVE'
                                       and iapg.status  = 'ACTIVE'
                                       and lower(iapgd.name) like :columnvalue))
                              ) 
                          )
             )
             """
    )
    Page<ProductListResponse> filter(
            @Param("columnvalue") String prompt,
            @Param("columnname") String columnName,
            @Param("standardProductStatuses") List<ProductStatus> standardProductStatuses,
            @Param("individualProductStatuses") List<ProductStatus> individualProductStatuses,
            @Param("detailsStatus") List<ProductDetailStatus> detailsStatus,
            @Param("group") Set<Long> groupIds,
            @Param("type") Set<Long> typeIds,
            @Param("contractterm") Set<String> contractTermsNames,
            @Param("saleschannel") Set<Long> salesChannelIds,
            @Param("segment") Set<Long> segmentIds,
            @Param("consumptiontype") String consumptionType,
            @Param("globalSalesChannel") Boolean globalSalesChannel,
            @Param("globalSegment") Boolean globalSegment,
            @Param("individualproduct") String individualProduct,
            @Param("excludeOldVersion") String excludeOldVersion,
            Pageable pageable
    );

    Optional<Product> findByIdAndProductStatusIn(Long id, List<ProductStatus> statuses);

    List<Product> findByIdInAndProductStatusIn(List<Long> ids, List<ProductStatus> statuses);

    @Query(
           """
            select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse(
                  tg.id,
                  concat(tgd.name, ' (', tg.id, ')')
              )
              from Product tg
              join ProductDetails tgd on tg.lastProductDetail = tgd.id
              where tg.id in (
                  select distinct ntg.id
                  from Product ntg
                  join ProductDetails ntgd on ntgd.product.id = ntg.id
                      where (:prompt is null or (lower(ntgd.name) like :prompt or cast(ntg.id as string) like :prompt))
                      and ntg.productStatus ='ACTIVE'
                      and ((:individual is null and ntg.customerIdentifier is null)  or (:individual = 'INDIVIDUAL_PRODUCT' and ntg.customerIdentifier is not null))
              )
              and tg.productStatus = 'ACTIVE'
              """
    )
    Page<CopyDomainWithVersionBaseResponse> findByCopyGroupBaseRequest(
            String prompt,
            String individual,
            Pageable pageable
    );

    @Query("""
            select new bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse(
                aps.id,
                aps.displayName,
                aps.type
            )
            from VwAvailableProductAndServices aps
                where (:prompt is null or concat(lower(aps.name), aps.id) like :prompt)
                and (:excludedId is null or (aps.type = 'PRODUCT' AND aps.id <> :excludedId) OR aps.type <> 'PRODUCT')
                and (:excludedItemId is null or (text(aps.type) = :excludedItemType and aps.id <> :excludedItemId) or text(aps.type) <> :excludedItemType)
            order by aps.name
            """)
    Page<AvailableProductRelatedEntitiesResponse> findAvailableProductsAndServices(
            @Param("prompt") String prompt,
            @Param("excludedId") Long excludedId,
            @Param("excludedItemId") Long excludedItemId,
            @Param("excludedItemType") String excludedItemType,
            PageRequest pageRequest
    );

    @Query("""
            select aps.id
            from VwAvailableProductAndServices aps
            where aps.type = 'PRODUCT'
            """)
    List<Long> findAvailableProductIdsForProduct();


    @Query(
            value = """
                    select aps.id
                    from VwAvailableProductAndServices aps
                    where aps.type = 'PRODUCT'
                    and aps.id in :ids
                    """
    )
    List<Long> findAvailableProductIdsForService(List<Long> ids);


    @Query(
            value = """
                    select count (p.id) > 0 from Product p
                    join ProductLinkToProduct pltp on pltp.linkedProduct.id = p.id
                        where p.id = :productId
                        and pltp.productSubObjectStatus = 'ACTIVE'
                        and pltp.productDetails.product.productStatus = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionToProduct(@Param("productId") Long productId);


    @Query(
            value = """
                    select count (p.id) > 0 from Product p
                    join ServiceLinkedProduct slp on slp.product.id = p.id
                        where p.id = :productId
                        and slp.status = 'ACTIVE'
                        and slp.serviceDetails.service.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionToService(@Param("productId") Long productId);

    boolean existsByIdAndProductStatusIn(Long productId, List<ProductStatus> statuses);


    @Query(value = """
select distinct pd.id as detailId,
       pd.product_id as id,
       pd.name as name,
       pd.version_id as versionId,
       p.customer_identifier
from product.products p
    join product.product_details pd on pd.product_id = p.id
    left join product.product_sales_channels psc on psc.product_detail_id=pd.id and psc.status='ACTIVE'
    left join nomenclature.sales_channels sc on sc.id=psc.sales_channel_id
    left join customer.account_manager_tags amt on amt.portal_tag_id=sc.portal_tag_id
    left join customer.account_managers am on am.id=amt.account_manager_id
where pd.status = 'ACTIVE'
  and p.status =  'ACTIVE'
and (:productId is null or pd.product_id = :productId)
  and (coalesce(:prompt,'0') = '0' or  lower(pd.name) like concat('%',lower(:prompt),'%' ) )
  and (pd.global_sales_channel  or p.customer_identifier is not null or  :username is null or am.user_name = :username)
  and ((:customerDetailId  is null and (pd.available_for_sale = true and current_timestamp between coalesce(pd.available_From,current_timestamp) and coalesce(pd.available_To,current_timestamp)))
    or
       (:customerDetailId is not null
            and
        (p.customer_identifier is not null
            and exists(select 1
                       from customer.customer_details cd
                                join customer.customers c on c.id=cd.customer_id
                       where c.identifier = p.customer_identifier
                         and cd.id = :customerDetailId
                         and c.status = 'ACTIVE')
            and not exists(select 1
                           from product_contract.contract_details cdt
                                    join product_contract.contracts cnt
                                         on cdt.contract_id = cnt.id
                           where cdt.product_detail_id = pd.id
                             and cnt.status = 'ACTIVE'
                             and cnt.contract_status in ('DRAFT',
                                                         'READY',
                                                         'SIGNED',
                                                         'ENTERED_INTO_FORCE',
                                                         'ACTIVE_IN_TERM',
                                                         'ACTIVE_IN_PERPETUITY',
                                                         'CHANGED_WITH_AGREEMENT')))
           or
        (p.customer_identifier is null
            and
         (pd.available_for_sale = true and current_timestamp between coalesce(pd.available_From,current_timestamp) and coalesce(pd.available_To,current_timestamp))
            and
         (pd.global_segment = 'true'
             or
          exists(select 1
                 from customer.customer_details cd
                          join customer.customers c
                               on cd.customer_id = c.id
                          join customer.customer_segments cs
                               on cs.customer_detail_id = cd.id
                 where cd.id = :customerDetailId
                   and c.status = 'ACTIVE'
                   and cs.status = 'ACTIVE'
                   and exists(select 1 from product.product_segments ps
                              where ps.product_detail_id = pd.id
                                and ps.segment_id = cs.segment_id
                                and ps.status = 'ACTIVE'))))))
order by p.customer_identifier, pd.name
                        """, nativeQuery = true)
    Page<ProductContractProductListingMiddleResponse> searchForContract(
            @Param("customerDetailId") Long customerDetailId,
            @Param("username") String username,
            @Param("prompt") String prompt,
            @Param("productId") Long productId,
            Pageable pageable
    );

    @Query(value = """

            select
       pd.id as detailId,
       pd.product_id as id,
       pd.name as name,
       pd.version_id as versionId
     from
      product.products p join product.product_details pd on pd.product_id = p.id
      where
     p.status = 'ACTIVE'
      and not exists(select 1 from product.product_additional_params pap where pap.product_detail_id = pd.id and pap.value is null and pap.label is not null)
      and pd.status = 'ACTIVE'
      and pd.available_For_Sale = true
      and current_date between coalesce(pd.available_From, current_date) and coalesce(pd.available_To, current_date)
      and p.customer_identifier is null
     and (pd.global_sales_channel or  exists(select 1 from customer.account_managers am
                         join customer.account_manager_tags amt on amt.account_manager_id = am.id and am.user_name=:userName
                         join nomenclature.sales_channels sc on sc.portal_tag_id=amt.portal_tag_id
                         join product.product_sales_channels psc on psc.sales_channel_id=sc.id and psc.product_detail_id=pd.id and psc.status='ACTIVE'))
      and (:prompt is null or lower(pd.name) like :prompt)
      and array_length(pd.contract_type, 1) = 1
      and 1 = (select count(1) from product.product_contract_terms pct where pct.product_details_id = pd.id and pct.status =  'ACTIVE')
      and 1 = (select count(1) from product.product_contract_terms pct where pct.product_details_id = pd.id and pct.status =  'ACTIVE' and pct.contract_term_period_type <> 'CERTAIN_DATE')
      and (pd.term_id is null or
           (1 = (select
                 count(1)
                  from terms.terms t
                  join terms.invoice_payment_terms ipt on ipt.term_id = t.id
                  where t.id = pd.term_id
                    and t.status = 'ACTIVE'
                    and ipt.status = 'ACTIVE')
            and
           1 = (select count(1)
                  from terms.terms t
                  join terms.invoice_payment_terms ipt on ipt.term_id = t.id
                  where t.id = pd.term_id
                    and ipt.value is not null
                    and t.status = 'ACTIVE'
                    and ipt.status = 'ACTIVE')
          and
            (
           exists
             (select 1 from terms.terms trm
               where trm.id = pd.term_id
                 and trm.status = 'ACTIVE'
                 and array_length(trm.contract_entry_into_force,1) = 1
                 and trm.contract_entry_into_force not in ('{EXACT_DAY}','{MANUAL}')
                 and array_length(trm.start_initial_term_of_contract,1) = 1
                 and trm.start_initial_term_of_contract not in ('{EXACT_DATE}','{MANUAL}')
                 and array_length(trm.supply_activation,1) = 1
                 and trm.supply_activation <> '{EXACT_DATE}'
                 and (trm.supply_activation = '{MANUAL}' or array_length(trm.wait_for_old_contract_term_to_expire,1) = 1)
             )
            )
           )
          )
      and (pd.term_group_id  is null or
           ( 1 = (select
                 count(1)
                  from terms.terms t
                  join terms.invoice_payment_terms ipt on ipt.term_id = t.id
                  join terms.term_group_terms tgt on tgt.term_id = t.id
                  join terms.term_group_details tgd on tgt.term_group_detail_id = tgd.id
                  join terms.term_groups tg on tgd.group_id = tg.id
                  where pd.term_group_id = tg.id
                    and t.status = 'ACTIVE'
                    and ipt.status = 'ACTIVE'
                    and tgt.status = 'ACTIVE'
                    and tg.status = 'ACTIVE'
                    )
          and
           1 = (select
                 count(1)
                  from terms.terms t
                  join terms.invoice_payment_terms ipt on ipt.term_id = t.id
                  join terms.term_group_terms tgt on tgt.term_id = t.id
                  join terms.term_group_details tgd on tgt.term_group_detail_id = tgd.id
                  join terms.term_groups tg on tgd.group_id = tg.id
                  where pd.term_group_id = tg.id
                    and ipt.value is not null
                    and t.status = 'ACTIVE'
                    and ipt.status = 'ACTIVE'
                    and tgt.status = 'ACTIVE'
                    and tg.status = 'ACTIVE')
           and
            (
           exists
             (select 1
               from terms.terms trm
               join terms.term_group_terms tgt on tgt.term_id = trm.id
               join terms.term_group_details tgd on tgt.term_group_detail_id = tgd.id
               join terms.term_groups tg on tgd.group_id = tg.id
               where pd.term_group_id = tg.id
                 and trm.status = 'ACTIVE'
                 and array_length(trm.contract_entry_into_force,1) = 1
                 and trm.contract_entry_into_force not in ('{EXACT_DAY}','{MANUAL}')
                 and array_length(trm.start_initial_term_of_contract,1) = 1
                 and trm.start_initial_term_of_contract not in ('{EXACT_DATE}','{MANUAL}')
                 and array_length(trm.supply_activation,1) = 1
                 and trm.supply_activation <> '{EXACT_DATE}'
                 and (trm.supply_activation = '{MANUAL}' or array_length(trm.wait_for_old_contract_term_to_expire,1) = 1)
             )
            )
           )
          )
     and array_length(pd.payment_guarantee, 1) = 1
     and (case when pd.payment_guarantee  = '{CASH_DEPOSIT}' then
                    pd.cash_deposit_amount is not null and pd.cash_deposit_currency_id is not null else 1=1 end)
     and (case when pd.payment_guarantee  = '{BANK}' then
                    pd.bank_guarantee_amount  is not null and pd.bank_guarantee_currency_id  is not null else 1=1 end)
     and (case when pd.payment_guarantee  = '{CASH_DEPOSIT_AND_BANK}' then
                     pd.bank_guarantee_amount  is not null and pd.bank_guarantee_currency_id  is not null and
                     pd.cash_deposit_amount is not null and
                     pd.cash_deposit_currency_id is not null else 1=1 end)         \s
      and not exists(select 1 from product.product_price_components ppc
                     join price_component.price_components pc
                     on ppc.price_component_id = pc.id
                     and pc.status = 'ACTIVE'
                    join price_component.price_component_formula_variables pcfv
                    on pcfv.price_component_id = pc.id
                   where ppc.product_detail_id = pd.id
                   and pcfv.value is null)
      and not exists
       (select 1
        from
        product.product_interim_advance_payments piap
        join
         interim_advance_payment.interim_advance_payments iap
         on piap.interim_advance_payment_id = iap.id
          and piap.product_detail_id = pd.id
          and piap.status = 'ACTIVE'
          and iap.status = 'ACTIVE'
         join price_component.price_components pc
          on iap.price_component_id = pc.id
         and pc.status = 'ACTIVE'
         join price_component.price_component_formula_variables pcfv
         on pcfv.price_component_id = pc.id
         and pcfv.value is null)             \s
     and(not exists (select 1 from product.product_interim_advance_payments piap where piap.product_detail_id = pd.id and piap.status = 'ACTIVE')
           or
          1 = (select count(1) from product.product_interim_advance_payments piap
                join interim_advance_payment.interim_advance_payments iap on piap.interim_advance_payment_id = iap.id
               where piap.product_detail_id = pd.id
                and
                iap.payment_type = 'OBLIGATORY'
                and
               (iap.match_term_of_standard_invoice = true
                 or
                 (
                 1= (select count(1) from interim_advance_payment.interim_advance_payment_terms iapt where iapt.interim_advance_payment_id = iap.id and iapt.status = 'ACTIVE')
                  and
                 1= (select count(1) from interim_advance_payment.interim_advance_payment_terms iapt where iapt.interim_advance_payment_id = iap.id and iapt.status = 'ACTIVE' and iapt.value is not null)
                 )
               )
               and
               (iap.value_type = 'PRICE_COMPONENT' or ( (iap.value_type in('PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT','EXACT_AMOUNT')) and iap.value is not null ))
               and
               (case when iap.date_of_issue_type  in ('DATE_OF_THE_MONTH','WORKING_DAYS_AFTER_INVOICE_DATE') then iap.date_of_issue_value is not null else 1=1 end)
               and iap.status = 'ACTIVE'
               and piap.status  = 'ACTIVE')
          )
     and
      (pd.equal_monthly_installments_activation = 'false' or (pd.installment_number is not null and pd.amount is not null))
     and
       (:customerDetailId is null or pd.global_segment = 'true'
        or
        exists
        (select 1
          from product.product_segments ps
           where ps.product_detail_id = pd.id
           and ps.status = 'ACTIVE'
           and exists (select 1 from customer.customer_segments cs
                          where cs.customer_detail_id = :customerDetailId
                          and cs.segment_id = ps.segment_id
                          and cs.status = 'ACTIVE'))
       )""", nativeQuery = true)
    Page<ProductContractProductListingMiddleResponse> searchForExpressContract(
            @Param("customerDetailId") Long customerDetailId,
            @Param("prompt") String prompt,
            @Param("userName") String userName,
            Pageable pageable
    );

    @Query("""
            select count(pcd.id) > 0
            from Product p
            join ProductDetails pd on pd.product.id = p.id
            join ProductContractDetails pcd on pcd.productDetailId = pd.id
            join ProductContract pc on pc.id = pcd.contractId
            where p.id = :id
            and pc.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionToProductContract(Long id);


    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionParameterResponse(
                            p.id,
                            concat(pd.name, ' (Version ', pd.version, ')')
                        )
                        from Product p
                        join ProductDetails pd on p.lastProductDetail = pd.id
                        where p.id in :ids
                    """
    )
    List<ConditionParameterResponse> findByIdIn(List<Long> ids);

    @Modifying
    @Query("delete from ProductContractAdditionalParams pcap where pcap.productAdditionalParamId in (:params)")
    void deleteContractAdditionalParams(List<Long> params);

    @Query("""
            select count(sc.id)>0 
            from ProductDetails pd
            join ProductSalesChannel psc on psc.productDetails.id =pd.id
            join SalesChannel sc on sc.id=psc.salesChannel.id
            left join AccountManagerTag amt on amt.portalTagId=sc.portalTagId
            join AccountManager am on am.id = amt.accountManagerId
            where pd.id= :detailId
            and am.userName =:loggedInUserId
            and psc.productSubObjectStatus='ACTIVE'
            """)
    boolean checkSegments(Long detailId, String loggedInUserId);


    @Query("""
            select new bg.energo.phoenix.model.response.product.ProductShortResponse(
                        p.id,
                        concat(pd.name, ' (Version ', pd.version, ')')
            )
            from Product p
            join ProductDetails pd on pd.product.id=p.id
            where (:prompt is null or
             (lower(pd.name) like lower(concat('%',:prompt,'%') ))) or (cast(p.id as string ) = :prompt)
             and pd.createDate = (select max(pd2.createDate) from ProductDetails pd2
             where pd2.product.id =p.id and pd2.createDate<:currentDate)
            """)
    Page<ProductShortResponse> findAllForListings(String prompt, LocalDateTime currentDate, Pageable id);
}
