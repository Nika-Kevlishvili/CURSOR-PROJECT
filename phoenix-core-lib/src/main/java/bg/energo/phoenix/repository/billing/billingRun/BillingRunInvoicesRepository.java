package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunInvoices;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote.BillingRunInvoiceResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunInvoicesRepository extends JpaRepository<BillingRunInvoices, Long> {

    @Query("""
            select bi.invoiceId
            from BillingRunInvoices bi
            where bi.billingId = :billingRunId
            and bi.status = 'ACTIVE'
            """)
    List<Long> findByBillingRunId(@Param("billingRunId") Long billingRunId);

    @Query("""
            select bi
            from BillingRunInvoices bi
            where bi.billingId = :billingRunId
            and bi.status = 'ACTIVE'
            """)
    List<BillingRunInvoices> findAllByBillingId(@Param("billingRunId") Long billingRunId);

    @Modifying
    @Query("""
            update BillingRunInvoices bi
            set bi.status = 'DELETED'
            where bi.billingId = :billingId
            """)
    void updateStatusByBillingId(@Param("billingId") Long billingId);


    @Query("""
                select count(distinct cd.customerId) = 1
                from Invoice inv
                join CustomerDetails cd on cd.id=inv.customerDetailId
                where inv.id in :invoiceIds
            """)
    boolean invoicesHaveSameCustomer(List<Long> invoiceIds);

    @Query("""
                select  inv.id
                from Invoice inv
                where inv.id in :invoiceIds
                and inv.invoiceDocumentType <> 'INVOICE'
            """)
    List<Long> getInvoiceIdsWithoutInvoiceType(List<Long> invoiceIds);

    @Query(nativeQuery = true, value = """
            select (
                        (count(cbg.id) = count(i.id) and count(distinct cbg.id) = 1))
            from invoice.invoices i
                     left join product_contract.contract_billing_groups cbg on i.contract_billing_group_id = cbg.id
            where i.id in (:invoiceIds)
            """)
    boolean invoicesHaveSameType(List<Long> invoiceIds);

    @Query("""
            select bri, inv
            from BillingRunInvoices bri
            join Invoice inv on inv.id = bri.invoiceId
            where bri.billingId = :billingRunId
            """)
    List<Object[]> findBillingRunInvoicesAndMapWithInvoices(@Param("billingRunId") Long billingRunId);

    @Query("""
            select inv from BillingRunInvoices bri 
            join Invoice inv on bri.invoiceId=inv.id
            where bri.billingId=:billingId
            and (inv.isDeducted=false or inv.isDeducted is null) and inv.invoiceType ='INTERIM_AND_ADVANCE_PAYMENT'
            """)
    List<Invoice> findAllInterimByBillingId(Long billingId);

    @Query(value = """
            select bi.invoice_id as invoiceId,
                   inv1.invoice_number as invoiceNumber,
                                              coalesce(inv1.service_contract_id,inv1.product_contract_id,-1)<>-1 as canSelectAccordingToContract,
                                              coalesce(inv1.product_contract_id,inv1.service_contract_id,inv1.service_order_id,inv1.goods_order_id, -1) <> -1 canSelectManual
            from billing.billing_invoices bi
            join invoice.invoices inv1 on inv1.id=bi.invoice_id
            where bi.billing_id = :billingRunId
            and bi.status = 'ACTIVE'
            """,nativeQuery = true)
    List<BillingRunInvoiceResponse> findManualCreditNoteInvoices(@Param("billingRunId") Long billingRunId);

}
