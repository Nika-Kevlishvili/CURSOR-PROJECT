package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.CacheObjectForDetails;
import bg.energo.phoenix.model.entity.product.product.Product;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.product.CostCenterAndIncomeAccountResponse;
import bg.energo.phoenix.model.response.product.ProductVersion;
import bg.energo.phoenix.model.response.product.ProductVersionShortResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
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
import java.util.Set;

@Repository
public interface ProductDetailsRepository extends JpaRepository<ProductDetails, Long> {
    @Query("""
            select pd from ProductDetails pd
            join Product p on p.id = pd.product.id
            where pd.name like (:name)
            and p.productStatus in (:statuses)
            """)
    List<ProductDetails> findAllByProductNameAndStatusesForUniqueness(@Param("name") String name, @Param("statuses") List<ProductStatus> statuses);


    @Query("""
            select pd from ProductDetails pd
            where pd.product.id = :productId
            and pd.productDetailStatus in (:statuses)
            and pd.version = (
                select max(productDetails.version) from ProductDetails productDetails
                where productDetails.product.id = :productId
                and pd.productDetailStatus in (:statuses)
                )
            """)
    Optional<ProductDetails> findLatestDetails(@Param("productId") Long productId,
                                               @Param("statuses") List<ProductDetailStatus> statuses,
                                               Sort sort);

    /**
     * @param productId - {@link Product#id}
     * @return count of {@link ProductDetails} that linked with another product/service
     */
    @Query("""
            select count(pd.id) from ProductDetails pd
            join Product p on p.id = pd.product.id
            where p.id = :productId
            and pd.productDetailStatus = 'ACTIVE'
            and (exists (select 1 from ProductLinkToProduct plp where plp.productDetails.id = pd.id and plp.productSubObjectStatus = 'ACTIVE')
                 or
                 exists (select 1 from ProductLinkToService pls where pls.productDetails.id = pd.id and pls.productSubObjectStatus = 'ACTIVE'))
            """)
    long countProductDetailsWithLinkedEntitiesByProductId(@Param("productId") Long productId);

    @Query("""
            select pd from ProductDetails pd
            join Product p on p.id = pd.product.id
            and p.id = :productId
            and pd.version = :version
            """)
    Optional<ProductDetails> findByProductIdAndVersion(@Param("productId") Long id, @Param("version") Long version);

    List<ProductDetails> findAllByName(String name);

    @Query("""
            select max(pd.version) from ProductDetails pd
            where pd.product.id = :productId
            """)
    long findLastDetailVersion(@Param("productId") long productId);

    Optional<ProductDetails> findByProductIdAndVersionAndProductDetailStatusIn(
            Long productId,
            Long version,
            List<ProductDetailStatus> statuses
    );

    @Query("""
            select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse(tgd.id,tgd.version,tgd.createDate)
            from ProductDetails tgd
            where tgd.product.id =:id
            order by tgd.version ASC
            """)
    List<CopyDomainWithVersionMiddleResponse> findByCopyGroupBaseRequest(@Param("id") Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.product.ProductVersion(
                pd.version,
                pd.id,
                pd.createDate,
                pd.productDetailStatus
            )
            from ProductDetails pd
            join Product p on p.id = pd.product.id
            where p.id = :productId
            and pd.productDetailStatus in (:statuses)
            order by pd.version
            """)
    List<ProductVersion> findAllByProductIdAndProductDetailStatusIn(@Param("productId") Long id, @Param("statuses") List<ProductDetailStatus> status);

    @Query("""
            select pd from ProductDetails pd
            join Product p on p.id = pd.product.id
            where pd.id = :productDetailId
            and pd.productDetailStatus = :productDetailStatus
            and p.productStatus = :productStatus
            """)
    Optional<ProductDetails> findByIdProductDetailStatusProductStatus(Long productDetailId, ProductDetailStatus productDetailStatus, ProductStatus productStatus);

    @Query("""
            select pd from ProductDetails pd
            join Product p on p.id = pd.product.id
            where p.id = :productId
            and pd.version = :version
            and pd.productDetailStatus in (:statuses)
            """)
    Optional<ProductDetails> findByProductIdAndVersionAndStatus(@Param("productId") Long id, @Param("version") Long version, @Param("statuses") List<ProductDetailStatus> statuses);

    @Query(value = """
            select count(pd.id)
            from
                product.products p join product.product_details pd on pd.product_id = p.id
            where
                    pd.id = :productDetailId
              and p.status = 'ACTIVE'
              and pd.status = 'ACTIVE'
              and pd.available_For_Sale = true
              and current_date between coalesce(pd.available_From, current_date) and coalesce(pd.available_To, current_date)
              and p.customer_identifier is null
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
                                pd.cash_deposit_currency_id is not null else 1=1 end)
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
                           (iap.value_type = 'PRICE_COMPONENT' or ( (iap.value_type = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT' or iap.value_type = 'EXACT_AMOUNT') and iap.value is not null ))
                         and
                           (case when iap.date_of_issue_type =  'DATE_OF_THE_MONTH' then iap.date_of_issue_value is not null else 1=1 end)
                         and iap.status = 'ACTIVE'
                         and piap.status  = 'ACTIVE')
                )
              and
                (pd.equal_monthly_installments_activation = 'false' or (pd.installment_number is not null and pd.amount is not null))
            """, nativeQuery = true)
    Integer canCreateExpressContractForProductDetail(Long productDetailId);



    @Query("""
            select case when count(c)>0 then true else false end 
            from ProductContractDetails c 
            where c.contractId = :contractId
            and c.startDate = :startDate
                """)
    boolean versionWithStartDateExists(@Param("contractId") Long contractId,
                                       @Param("startDate") LocalDate startDate);

    @Query("""
            select new  bg.energo.phoenix.model.CacheObjectForDetails(p.id,pd.version,pd.id)
            from ProductDetails pd
            join Product p on pd.product.id=p.id
            where p.id=:productId
            and pd.version = :productVersion
            and p.productStatus='ACTIVE'
                """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObjectForDetails> findCacheObjectByProductId(Long productId, Long productVersion);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.product.ProductVersionShortResponse(
                        pd.id,
                        pd.name,
                        pd.version,
                        pd.product.customerIdentifier
                    )
                    from ProductDetails pd
                    where exists (
                        select 1 from ProductContractDetails pcd
                        where pcd.productDetailId = pd.id
                    )
                    and (:prompt is null or lower(pd.name) like :prompt)
                    order by pd.createDate desc
                    """
    )
    Page<ProductVersionShortResponse> getProductVersionsForProductContractsListing(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query("""

            select distinct pd from ProductDetails pd
            join ProductContractDetails pcd on pcd.productDetailId = pd.id
            join ProductContract        pc  on pcd.contractId = pc.id
            where pd.id = :id
            and pc.status =  'ACTIVE'
            """)
    List<ProductDetails> checkForBoundObjects(Long id);


    @Query(nativeQuery = true,
            value = """
            select distinct p.id
            from terms.penalties p
            left join terms.penalty_action_types pat on (p.id = pat.penalty_id and pat.status = 'ACTIVE')
            where (exists(select 1
                          from product_contract.contract_details cd
                                   join product.product_penalties pp2 on pp2.product_detail_id = cd.product_detail_id
                          where pp2.penalty_id = p.id
                            and pp2.status = 'ACTIVE'
                            and cd.id = :contractDetailId
                            and (
                              :executionDate >= cd.start_date and :executionDate < coalesce((select min(start_date)
                                                                                             from product_contract.contract_details cd1
                                                                                             where cd1.contract_id = cd.contract_id
                                                                                               and cd1.start_date > cd.start_date),
                                                                                            date(:executionDate) + 1)
                              ))
                or exists(select 1
                          from product_contract.contract_details cd
                                   join product.product_penalty_groups ppg
                                        on ppg.product_detail_id = cd.product_detail_id
                                            and ppg.status = 'ACTIVE'
                                   join terms.penalty_group_details pgd on pgd.penalty_group_id = ppg.penalty_group_id
                                   join terms.penalty_group_penalties pgp
                                        on pgp.penalty_group_detail_id = pgd.id
                                            and pgp.penalty_id = p.id
                                            and pgp.status = 'ACTIVE'
                          where cd.id = :contractDetailId
                            and (
                              :executionDate >= cd.start_date and :executionDate < coalesce((select min(start_date)
                                                                                             from product_contract.contract_details cd1
                                                                                             where cd1.contract_id = cd.contract_id
                                                                                               and cd1.start_date > cd.start_date),
                                                                                            date(:executionDate) + 1)
                              )
                            and (
                              :executionDate >= pgd.start_date and :executionDate < coalesce((select min(start_date)
                                                                                              from terms.penalty_group_details pgd3
                                                                                              where pgd3.penalty_group_id = pgd.penalty_group_id
                                                                                                and pgd3.start_date > pgd.start_date),
                                                                                             date(:executionDate) + 1)
                              ))
                )
              and p.status = 'ACTIVE'
              and pat.action_type_id in (:actionTypeIds)
              and text(p.penalty_party_receiver) like '%CUSTOMER%'
            """
    )
            //Todo this is removed for testing purposes should be added in future.
//    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Set<Long> findPenaltyIdsForProductWithContractDetailId(
            Long contractDetailId,
            @Param("executionDate") LocalDate executionDate,
            @Param("actionTypeIds") List<Long> actionTypeIds);

    @Query("""
            select new bg.energo.phoenix.model.response.product.CostCenterAndIncomeAccountResponse(
            pd.id,
            pd.costCenterControllingOrder,
            pd.incomeAccountNumber
            )
            from ProductDetails pd
            where pd.id in (:ids)
            """)
    List<CostCenterAndIncomeAccountResponse> getCostCenterAndIncomeAccountByDetailId(@Param("ids") List<Long> ids);
}
