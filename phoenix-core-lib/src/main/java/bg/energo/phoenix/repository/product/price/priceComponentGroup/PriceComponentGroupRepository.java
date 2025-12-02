package bg.energo.phoenix.repository.product.price.priceComponentGroup;

import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroup;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroupDetails;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupStatus;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupListResponse;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedGroupEntityResponse;
import bg.energo.phoenix.model.response.service.AvailableServiceRelatedGroupEntityResponse;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceComponentGroupRepository extends JpaRepository<PriceComponentGroup, Long> {

    Optional<PriceComponentGroup> findByIdAndStatusIn(Long id, List<PriceComponentGroupStatus> statuses);

    List<PriceComponentGroup> findByIdInAndStatusIn(List<Long> ids, List<PriceComponentGroupStatus> statuses);

    boolean existsByIdAndStatusIn(Long id, List<PriceComponentGroupStatus> statuses);

    /**
     * Executes a native SQL query to retrieve a list of price component group details with the given parameters.
     *
     * @param priceComponentGroupStatuses a list of status values for the price component groups to search for depending on user permissions
     * @param searchBy                    the type of search to perform, either "ALL", "PRICE_COMPONENT_GROUP_NAME", or "PRICE_COMPONENT_NAME"
     * @param prompt                      the search string to use for the search
     * @return a list of price component group details that match the search criteria
     */
    @Query(nativeQuery = true,
            value = """
                    select
                        tbl.groupId as groupId,
                        tbl.name as name,
                        tbl.numberOfPriceComponents as numberOfPriceComponents,
                        tbl.dateOfCreation as dateOfCreation,
                        tbl.status as status
                    from (
                    select
                        pcg.id as groupId,
                        pcgd.name as name,
                        pcgd.start_date as startDate,
                        (select count(1) from price_component.pc_group_pcs pcgpc where pcgpc.price_component_group_detail_id = pcgd.id and pcgpc.status = 'ACTIVE') as numberOfPriceComponents,
                        (select max(start_date) from price_component.price_component_group_details tt where tt.price_component_group_id = pcg.id and start_date <= now()) as currentstartdate,
                        pcg.create_date as dateOfCreation,
                        pcg.status as status
                    from
                      price_component.price_component_group_details pcgd
                      join price_component.price_component_groups pcg on pcg.id = pcgd.price_component_group_id
                        where pcg.id in(
                            select pcg.id from price_component.price_component_group_details pcgd
                            left join price_component.pc_group_pcs pcgpc on pcgpc.price_component_group_detail_id = pcgd.id and pcgpc.status = 'ACTIVE'
                            left join price_component.price_components pc on pcgpc.price_component_id = pc.id
                            join price_component.price_component_groups pcg on pcg.id = pcgd.price_component_group_id
                            where text(pcg.status) in (:priceComponentGroupStatuses)
                            and (
                                :searchBy is null
                                or (:searchBy = 'ALL' and (lower(pcgd.name) like :prompt or lower(pc.name) like :prompt))
                                or (
                                    (:searchBy = 'PRICE_COMPONENT_GROUP_NAME' and lower(pcgd.name) like :prompt)
                                    or (:searchBy = 'PRICE_COMPONENT_NAME' and lower(pc.name) like :prompt)
                                )
                            )
                            and (coalesce(:excludeVersion,'0') = '0' or (:excludeVersion = 'excludeOld' 
                                and pcgd.start_date >=
                                    (select max(start_date)
                                        from price_component.price_component_group_details gdtls
                                        where gdtls.price_component_group_id = pcg.id
                                        and start_date <= current_date)
                                        )
                            or (:excludeVersion = 'excludeFuture' 
                                and pcgd.start_date <=
                                    (select max(start_date)
                                        from price_component.price_component_group_details gdtls
                                        where gdtls.price_component_group_id = pcg.id
                                        and start_date <= current_date))
                            or (:excludeVersion = 'excludeOldAndFuture' 
                                and pcgd.start_date =
                                    (select max(start_date)
                                        from price_component.price_component_group_details gdtls
                                        where gdtls.price_component_group_id = pcg.id
                                        and start_date <= current_date))
                                        )
                        )
                    ) as tbl
                    where startDate = currentstartdate
                    """,
            countQuery =
                    """
                            select
                                count(1)
                            from (
                            select
                                pcg.id as groupId,
                                pcgd.name as name,
                                pcgd.start_date as startDate,
                                (select count(1) from price_component.pc_group_pcs pcgpc where pcgpc.price_component_group_detail_id = pcgd.id and pcgpc.status = 'ACTIVE') as numberOfpriceComponents,
                                (select max(start_date) from price_component.price_component_group_details tt where tt.price_component_group_id = pcg.id and start_date <= now()) as currentstartdate,
                                pcg.create_date as dateOfCreation,
                                pcg.status as status
                            from
                              price_component.price_component_group_details pcgd
                              join price_component.price_component_groups pcg on pcg.id = pcgd.price_component_group_id
                                where pcg.id in(
                                    select pcg.id from price_component.price_component_group_details tgd
                                    left join price_component.pc_group_pcs pcgpc on pcgpc.price_component_group_detail_id = pcgd.id and pcgpc.status = 'ACTIVE'
                                    left join price_component.price_components pc on pcgpc.price_component_id = pc.id
                                    join price_component.price_component_groups pcg on pcg.id = pcgd.price_component_group_id
                                    where text(pcg.status) in (:priceComponentGroupStatuses)
                                    and (
                                        :searchBy is null
                                        or (:searchBy = 'ALL' and (lower(pcgd.name) like :prompt or lower(pc.name) like :prompt))
                                        or (
                                            (:searchBy = 'PRICE_COMPONENT_GROUP_NAME' and lower(pcgd.name) like :prompt)
                                            or (:searchBy = 'PRICE_COMPONENT_NAME' and lower(pc.name) like :prompt)
                                        )
                                    )
                                    and (coalesce(:excludeVersion,'0') = '0' or (:excludeVersion = 'excludeOld' 
                                        and pcgd.start_date >=
                                            (select max(start_date)
                                                from price_component.price_component_group_details gdtls
                                                where gdtls.price_component_group_id = pcg.id
                                                and start_date <= current_date)
                                                )
                                    or (:excludeVersion = 'excludeFuture' 
                                        and pcgd.start_date <=
                                            (select max(start_date)
                                                from price_component.price_component_group_details gdtls
                                                where gdtls.price_component_group_id = pcg.id
                                                and start_date <= current_date))
                                    or (:excludeVersion = 'excludeOldAndFuture' 
                                        and pcgd.start_date =
                                            (select max(start_date)
                                                from price_component.price_component_group_details gdtls
                                                where gdtls.price_component_group_id = pcg.id
                                                and start_date <= current_date))
                                                )
                                )
                            ) as tbl
                            where startDate = currentstartdate
                            """
    )
    Page<PriceComponentGroupListResponse> list(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("priceComponentGroupStatuses") List<String> priceComponentGroupStatuses,
            @Param("excludeVersion") String excludeVersion,
            Pageable pageable
    );

    @Query(
            value = """
                    select count(pd.id) > 0 from ProductDetails pd
                    join ProductPriceComponentGroups ppcg on ppcg.productDetails.id = pd.id
                        where pd.product.productStatus in (:productStatuses)
                        and ppcg.productSubObjectStatus in (:ppcgStatuses)
                        and ppcg.priceComponentGroup.id = :pcgId
                    """
    )
    boolean hasConnectionToProduct(
            @Param("pcgId") Long pcgId,
            @Param("productStatuses") List<ProductStatus> productStatuses,
            @Param("ppcgStatuses") List<ProductSubObjectStatus> ppcgStatuses
    );

    @Query(
            value = """
                    select count (sd.id) > 0 from ServiceDetails sd
                    join ServicePriceComponentGroup  spcg on spcg.serviceDetails.id = sd.id
                        where sd.service.status in (:serviceStatuses)
                        and spcg.status in (:spcgStatuses)
                        and spcg.priceComponentGroup.id = :pcgId
                    """
    )
    boolean hasConnectionToService(
            @Param("pcgId") Long pcgId,
            @Param("serviceStatuses") List<ServiceStatus> serviceStatuses,
            @Param("spcgStatuses") List<ServiceSubobjectStatus> spcgStatuses
    );

    @Query(nativeQuery = true,
            value = """
                            select
                                tg.id,
                                tgd.name||' ('||tg.id||')' as name
                            from price_component.price_component_groups tg
                            join price_component.price_component_group_details tgd on tgd.price_component_group_id = tg.id
                                where tg.id in (
                                    select tg1.id from price_component.price_component_groups tg1
                                    join price_component.price_component_group_details tgd1 on tgd1.price_component_group_id = tg1.id
                                        where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                                )
                                and tg.status = 'ACTIVE'
                                and tgd.start_date = (
                                    select max(tgd3.start_date) from price_component.price_component_group_details tgd3
                                        where tgd3.price_component_group_id = tg.id
                                        and tgd3.start_date <= current_date
                                )
                            order by tg.create_date desc
                    """,
            countQuery = """
                    select count(1) from price_component.price_component_groups tg
                    join price_component.price_component_group_details tgd on tgd.price_component_group_id = tg.id
                        where tg.id in (
                            select tg1.id from price_component.price_component_groups tg1
                            join price_component.price_component_group_details tgd1 on tgd1.price_component_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from price_component.price_component_group_details tgd3
                                where tgd3.price_component_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    """
    )
    Page<AvailableProductRelatedGroupEntityResponse> findAvailablePriceComponentGroupsForProduct(
            @Param("prompt") String prompt,
            PageRequest pageRequest
    );


    @Query(nativeQuery = true,
            value = """
                    select
                        tg.id,
                        tgd.name||' ('||tg.id||')' as name
                    from price_component.price_component_groups tg
                    join price_component.price_component_group_details tgd on tgd.price_component_group_id = tg.id
                        where tg.id in (
                            select tg1.id from price_component.price_component_groups tg1
                            join price_component.price_component_group_details tgd1 on tgd1.price_component_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from price_component.price_component_group_details tgd3
                                where tgd3.price_component_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    order by tg.create_date desc
                    """,
            countQuery = """
                    select count(1) from price_component.price_component_groups tg
                    join price_component.price_component_group_details tgd on tgd.price_component_group_id = tg.id
                        where tg.id in (
                            select tg1.id from price_component.price_component_groups tg1
                            join price_component.price_component_group_details tgd1 on tgd1.price_component_group_id = tg1.id
                                where (:prompt is null or lower(tgd1.name) like :prompt or cast(tg1.id as text) like :prompt)
                        )
                        and tg.status = 'ACTIVE'
                        and tgd.start_date = (
                            select max(tgd3.start_date) from price_component.price_component_group_details tgd3
                                where tgd3.price_component_group_id = tg.id
                                and tgd3.start_date <= current_date
                        )
                    """
    )
    Page<AvailableServiceRelatedGroupEntityResponse> findAvailablePriceComponentGroupsForService(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse(
                        pcg.id,
                        concat(pcgd.name, ' (', pcg.id, ')')
                    )
                    from PriceComponentGroup pcg
                    join PriceComponentGroupDetails pcgd on pcgd.priceComponentGroupId = pcg.id
                    where pcg.id in (
                        select distinct ntg.id from PriceComponentGroup ntg
                            join PriceComponentGroupDetails ntgd on ntgd.priceComponentGroupId = ntg.id
                            where (:prompt is null or (lower(ntgd.name) like :prompt or cast(ntg.id as string) like :prompt))
                            and ntg.status ='ACTIVE'
                    )
                    and pcgd.startDate = (
                        select max (pcgd.startDate) from PriceComponentGroupDetails pcgd
                        where pcgd.priceComponentGroupId = pcg.id
                        and pcgd.startDate <= :currentDate
                    )
                    order by pcg.id DESC
                    """
    )
    Page<CopyDomainWithVersionBaseResponse> findByCopyGroupBaseRequest(
            @Param("prompt") String prompt,
            @Param("currentDate") LocalDate now,
            PageRequest page
    );

    @Query("""
            select pcfv
            from ProductPriceComponentGroups ppcg
            join PriceComponentGroup pcg on ppcg.priceComponentGroup.id = pcg.id and pcg.status = 'ACTIVE' and ppcg.productSubObjectStatus = 'ACTIVE'
            join PriceComponentGroupDetails pcgd on pcgd.priceComponentGroupId = pcg.id and pcg.status = 'ACTIVE'
            join PriceComponentGroupPriceComponent pcgpc on pcgpc.priceComponentGroupDetailId = pcgd.id and pcgpc.status = 'ACTIVE'
            join PriceComponent pc on pc.id = pcgpc.priceComponentId and pc.status = 'ACTIVE'
            join PriceComponentFormulaVariable pcfv on pcfv.priceComponent.id = pc.id
            where ppcg.productDetails.id = :productDetailId
            and pc.xenergieApplicationType = 'CONSUMER'
            and pcgd.startDate = (
                select max(innerPCGD.startDate)
                from PriceComponentGroupDetails innerPCGD 
                where innerPCGD.priceComponentGroupId = pcgd.priceComponentGroupId
                and innerPCGD.startDate <= current_date
            )
            """)
    List<PriceComponentFormulaVariable> findConsumerPriceComponentGroupPriceComponentFormulaVariablesByProductDetailId(Long productDetailId);

    @Query("""
            select pcfv
            from ProductPriceComponentGroups ppcg
            join PriceComponentGroup pcg on ppcg.priceComponentGroup.id = pcg.id and pcg.status = 'ACTIVE' and ppcg.productSubObjectStatus = 'ACTIVE'
            join PriceComponentGroupDetails pcgd on pcgd.priceComponentGroupId = pcg.id and pcg.status = 'ACTIVE'
            join PriceComponentGroupPriceComponent pcgpc on pcgpc.priceComponentGroupDetailId = pcgd.id and pcgpc.status = 'ACTIVE'
            join PriceComponent pc on pc.id = pcgpc.priceComponentId and pc.status = 'ACTIVE'
            join PriceComponentFormulaVariable pcfv on pcfv.priceComponent.id = pc.id
            where ppcg.productDetails.id = :productDetailId
            and pc.xenergieApplicationType = 'GENERATOR'
            and pcgd.startDate = (
                select max(innerPCGD.startDate)
                from PriceComponentGroupDetails innerPCGD 
                where innerPCGD.priceComponentGroupId = pcgd.priceComponentGroupId
                and innerPCGD.startDate <= current_date
            )
            """)
    List<PriceComponentFormulaVariable> findGeneratorPriceComponentGroupPriceComponentFormulaVariablesByProductDetailId(Long productDetailId);

    @Query(nativeQuery = true,value= """
      select 
      pcd.id as contractDetailId,
      pcomp.id as priceComponentId,
      pcg.id priceComponentGroupId,
      pcomp.issued_separate_invoice as issueSeparateInvoice,
      ap.application_type as applicationType,
       pcgd.start_date as startDate, (
      select pcgd2.start_date
      from price_component.price_component_groups pcg2 
      join price_component.price_component_group_details pcgd2 on pcg2.id = pcgd2.price_component_group_id
      where pcg2.id=pcg.id
      and pcgd2.start_date> pcgd.start_date
      order by pcgd2.start_date limit 1
      ) as endDate,
      current_date+1
      from product_contract.contracts pc
      join product_contract.contract_details pcd on pcd.contract_id =pc.id
      join product.product_price_component_groups ppcg on ppcg.product_detail_id=pcd.product_detail_id
      join price_component.price_component_groups pcg on ppcg.price_component_group_id = pcg.id
      join price_component.price_component_group_details pcgd on pcg.id = pcgd.price_component_group_id
      join price_component.price_components pcomp on pcomp.price_component_group_detail_id = pcgd.id
      join price_component.application_models ap on ap.price_component_id=pcomp.id
      where pc.id=:contractId
      and ppcg.status='ACTIVE'
      and ap.status='ACTIVE'
""")
    List<BillingDataPriceComponentGroup> findBillingPriceGroupsForContractId(Long contractId);

    @Query(nativeQuery = true,
            value = """
                    select coalesce(max('true'),'false') as is_connected from price_component.price_component_groups pcg  
                    where pcg.id = :pricecomponentgroupid and
                     (exists(select 1 
                    	from product.products p
                    	join product.product_details pd
                     			  on pd.product_id = p.id
                    	 and p.status = 'ACTIVE'
                    	join product.product_price_component_groups ppcg
                    	  on ppcg.product_detail_id = pd.id
                    	 and ppcg.price_component_group_id = pcg.id
                    	 and ppcg.status = 'ACTIVE'
                    	join price_component.price_component_group_details pcgd
                    	  on pcgd.price_component_group_id = pcg.id
                    	join price_component.pc_group_pcs pgp 
                    	  on pgp.price_component_group_detail_id = pcgd.id
                    	 and pgp.status = 'ACTIVE'
                    	join product_contract.contract_details cd 
                    	  on cd.product_detail_id =  pd.id
                    	join product_contract.contracts c
                    	  on cd.contract_id =  c.id
                    	 and c.status = 'ACTIVE')
                    or
                     exists(select 1 
                    	from service.services s
                    	join service.service_details sd
                     			  on sd.service_id = s.id
                    	 and s.status = 'ACTIVE'
                    	join service.service_price_component_groups spcg
                    	  on spcg.service_detail_id = sd.id
                    	 and spcg.price_component_group_id = pcg.id
                    	 and spcg.status = 'ACTIVE'
                    	join price_component.price_component_group_details pcgd
                    	  on pcgd.price_component_group_id = pcg.id
                    	join price_component.pc_group_pcs pgp 
                    	  on pgp.price_component_group_detail_id = pcgd.id
                    	 and pgp.status = 'ACTIVE'
                    	join service_contract.contract_details cd 
                    	  on cd.service_detail_id =  sd.id
                    	join product_contract.contracts c
                    	  on cd.contract_id =  c.id
                    	 and c.status = 'ACTIVE')
                    or
                     exists(select 1 
                    	from service.services s
                    	join service.service_details sd
                     			  on sd.service_id = s.id
                    	 and s.status = 'ACTIVE'
                    	join service.service_price_component_groups spcg
                    	  on spcg.service_detail_id = sd.id
                    	 and spcg.price_component_group_id = pcg.id
                    	 and spcg.status = 'ACTIVE'
                    	join price_component.price_component_group_details pcgd
                    	  on pcgd.price_component_group_id = pcg.id
                    	join price_component.pc_group_pcs pgp 
                    	  on pgp.price_component_group_detail_id = pcgd.id
                    	 and pgp.status = 'ACTIVE'
                              join service_order.orders o 
                                on o.service_detail_id =  sd.id
                               and o.status = 'ACTIVE')
                     )
                    """
    )
    boolean hasLockedConnection(
            @Param("pricecomponentgroupid") Long id
    );

    @Query("""
            select pcgd
            from PriceComponentGroupDetails pcgd
            where pcgd.priceComponentGroupId = :priceComponentGroupId
            """)
    List<PriceComponentGroupDetails> findPriceComponentGroupDetailsByPriceComponentGroup(Long priceComponentGroupId);
}
