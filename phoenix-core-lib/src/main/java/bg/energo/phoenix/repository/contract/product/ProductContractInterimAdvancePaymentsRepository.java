package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.documentModels.contract.response.InterimAdvancePaymentDetailResponse;
import bg.energo.phoenix.model.entity.contract.product.ProductContractInterimAdvancePayments;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductContractInterimAdvancePaymentsRepository extends JpaRepository<ProductContractInterimAdvancePayments, Long> {

    List<ProductContractInterimAdvancePayments> findAllByContractDetailIdAndStatusIn(Long detailId, List<ContractSubObjectStatus> statuses);

    @Query(nativeQuery = true, value = """
            select replace(text(iap.value_type), '_', ' ')                                      as Type,
                   text(ciap.value)                                                             as Value,
                   c.name                                                                       as Currency,
                   replace(text(iap.date_of_issue_type), '_', ' ')                              as DateIssueType,
                   coalesce(text(ciap.issue_date), text(iap.date_of_issue_value))               as DateIssueValue,
                   replace(text(iapt.type), '_', ' ')                                           as PaymentTermType,
                   text(contr_terms.value)                                                      as ContractTermValue,
                   case when iap.match_term_of_standard_invoice = true then 'YES' else 'NO' end as MatchesWithInvoiceYN,
                   1                                                                            as priority
            from interim_advance_payment.interim_advance_payments iap
                     join product_contract.contract_interim_advance_payments ciap
                          on iap.id = ciap.interim_advance_payment_id and ciap.status = 'ACTIVE'
                     join product_contract.contract_details pcd on ciap.contract_detail_id = pcd.id
                     join product_contract.contracts pc on pcd.contract_id = pc.id
                     join nomenclature.currencies c on iap.currency_id = c.id
                     left join interim_advance_payment.interim_advance_payment_terms iapt
                               on iap.id = iapt.interim_advance_payment_id and iapt.status = 'ACTIVE'
                     left join product.product_contract_terms contr_terms
                               on pcd.product_contract_term_id = contr_terms.id and contr_terms.status = 'ACTIVE'
            where pc.id = :id
              and pcd.version_id = :versionId
            union
            (select replace(text(iap.value_type), '_', ' ')                                      as Type,
                    text(iap.value)                                                              as Value,
                    c.name                                                                       as Currency,
                    replace(text(iap.date_of_issue_type), '_', ' ')                              as DateIssueType,
                    text(iap.date_of_issue_value)                                                as DateIssueValue,
                    replace(text(iapt.type), '_', ' ')                                           as PaymentTermType,
                    text(contr_terms.value)                                                      as ContractTermValue,
                    case when iap.match_term_of_standard_invoice = true then 'YES' else 'NO' end as MatchesWithInvoiceYN,
                    2                                                                            as priority
             from product.product_interim_advance_payment_groups piapg
                      join
                  interim_advance_payment.interim_advance_payment_groups iapg
                  on
                      piapg.interim_advance_payment_group_id = iapg.id
                      join
                  interim_advance_payment.interim_advance_payment_group_details iapgd
                  on
                      iapgd.interim_advance_payment_group_id = iapg.id
                      join
                  interim_advance_payment.interim_advance_payments iap
                  on
                      iap.iap_group_detail_id = iapgd.id
                      join nomenclature.currencies c on iap.currency_id = c.id
                      left join interim_advance_payment.interim_advance_payment_terms iapt
                                on iap.id = iapt.interim_advance_payment_id and iapt.status = 'ACTIVE'
                      join product_contract.contract_details cd on cd.product_detail_id = piapg.product_detail_id
                      join product_contract.contracts contr on cd.contract_id = contr.id
                      left join product.product_contract_terms contr_terms
                                on cd.product_contract_term_id = contr_terms.id and contr_terms.status = 'ACTIVE'
             where piapg.status = 'ACTIVE'
               and iapg.status = 'ACTIVE'
               and iap.status = 'ACTIVE'
               and iapgd.start_date =
                   (select max(start_date)
                    from interim_advance_payment.interim_advance_payment_group_details tt
                    where tt.interim_advance_payment_group_id
                        = iapgd.interim_advance_payment_group_id
                      and start_date < now())
               and contr.id = :id
               and cd.version_id = :versionId
             order by iap.id)
            order by priority
            """)
    List<InterimAdvancePaymentDetailResponse> fetchInterimAdvancePaymentsForDocument(Long id, Integer versionId);
}
