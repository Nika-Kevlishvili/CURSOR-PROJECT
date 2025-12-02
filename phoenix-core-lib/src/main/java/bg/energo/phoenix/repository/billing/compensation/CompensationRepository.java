package bg.energo.phoenix.repository.billing.compensation;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.compensation.Compensations;
import bg.energo.phoenix.model.enums.billing.compensation.CompensationStatus;
import bg.energo.phoenix.model.response.billing.compensations.CompensationListingProjection;
import bg.energo.phoenix.model.response.billing.compensations.CompensationResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentCompensationDAO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompensationRepository extends JpaRepository<Compensations, Long> {

    Optional<Compensations> findByIdAndStatusInAndCompensationStatusIn(
            Long id,
            List<EntityStatus> statuses,
            List<CompensationStatus> compensationStatus
    );

    @Query("""
            select new bg.energo.phoenix.model.response.billing.compensations.CompensationResponse(
            comp.id,
            comp.compensationDocumentNumber,
            comp.compensationDocumentDate,
            comp.compensationDocumentVolumes,
            comp.compensationDocumentPrice,
            comp.compensationReason,
            comp.compensationDocumentPeriod,
            comp.compensationDocumentAmount,
            curr.id,
            curr.name,
            c.id,
            case when c.customerType = 'LEGAL_ENTITY' then concat(c.identifier, ' (', cd.name, ')')
                else replace(concat(c.identifier , ' (',coalesce(cd.name, '') , ' ', coalesce(cd.middleName, '') , ' ', coalesce(cd.lastName, ''), ')'), '\\s+', ' ') end,
            pod.id,
            concat(pod.identifier, ' (', podd.name, ')'),
            rec.id,
            case when rec.customerType = 'LEGAL_ENTITY' then concat(rec.identifier, ' (', recd.name, ')')
                else replace(concat(rec.identifier , ' (',coalesce(recd.name, '') , ' ', coalesce(recd.middleName, '') , ' ', coalesce(recd.lastName, ''), ')'), '\\s+', ' ') end,
            comp.compensationIndex,
            comp.invoiceUsageDate,
            rl.id,
            rl.liabilityNumber,
            cr.id,
            cr.receivableNumber,
            cl.id,
            cl.liabilityNumber,
            rr.id,
            rr.receivableNumber,
            i.id,
            i.invoiceNumber,
            comp.status,
            comp.compensationStatus
            )
            from Compensations comp
            join Customer c on comp.customerId = c.id
            join CustomerDetails cd on c.lastCustomerDetailId = cd.id
            join Customer rec on comp.recipientId = rec.id
            join CustomerDetails recd on rec.lastCustomerDetailId = recd.id
            join PointOfDelivery pod on pod.id = comp.podId
            join PointOfDeliveryDetails podd on pod.lastPodDetailId = podd.id
            join Currency curr on curr.id = comp.compensationDocumentCurrencyId
            left join CustomerLiability cl on cl.id = comp.liabilityForCustomerId
            left join CustomerLiability rl on rl.id = comp.liabilityForRecipientId
            left join CustomerReceivable cr on cr.id = comp.receivableForCustomerId
            left join CustomerReceivable rr on rr.id = comp.receivableForRecipientId
            left join Invoice i on i.id = comp.invoiceId
            where comp.id = :id
            """)
    CompensationResponse view(Long id);

    @Query(value = """
            select id,
                   number,
                   date,
                   period,
                   documentVolumes,
                   price,
                   compensationReason,
                   amount,
                   currency,
                   customer,
                   pod,
                   recipient,
                   compensationStatus,
                   status,
                   index,
                   invoice_usage_date
            from (select comp.id                                             as id,
                         comp.compensation_document_number                   as number,
                         comp.compensation_document_date                     as date,
                         to_char(comp.compensation_document_period, 'Month') as period,
                         comp.compensation_document_volumes                  as documentVolumes,
                         comp.compensation_document_price                    as price,
                         comp.compensation_reason                            as compensationReason,
                         comp.compensation_document_amount                   as amount,
                         cur.name                                            as currency,
                         cur.id                                              as currencyId,
                         (case
                              when comp_cust.customer_type = 'PRIVATE_CUSTOMER'
                                  then concat(
                                      comp_cust.identifier,
                                      ' (',
                                      comp_cust.identifier,
                                      comp_cust_det.middle_name,
                                      comp_cust_det.last_name,
                                      ')'
                                       )
                              when comp_cust.customer_type = 'LEGAL_ENTITY'
                                  then concat(
                                      comp_cust.identifier,
                                      ' (',
                                      comp_cust_det.name,
                                      ')'
                                       )
                             end
                             )                                               as customer,
                         pod.identifier                                      as pod,
                         (case
                              when recipient_cust.customer_type = 'PRIVATE_CUSTOMER'
                                  then concat(
                                      recipient_cust.identifier,
                                      ' (',
                                      recipient_cust.identifier,
                                      recipient_cust_det.middle_name,
                                      recipient_cust_det.last_name,
                                      ')'
                                       )
                              when recipient_cust.customer_type = 'LEGAL_ENTITY'
                                  then concat(
                                      recipient_cust.identifier,
                                      ' (',
                                      recipient_cust_det.name,
                                      ')'
                                       )
                             end
                             )                                               as recipient,
                         comp.compensation_status                            as compensationStatus,
                         comp.status                                         as status,
                         comp.compensation_index                             as index,
                         cast(comp.invoice_usage_date as date)               as invoice_usage_date,
                         comp.create_date                                    as create_date
                  from billing.compensations comp
                           join customer.customers comp_cust on comp.customer_id = comp_cust.id
                           join customer.customer_details comp_cust_det on comp_cust.last_customer_detail_id = comp_cust_det.id
                           join pod.pod pod on pod.id = comp.pod_id
                           left join nomenclature.currencies cur on comp.compensation_document_currency_id = cur.id
                           left join customer.customers recipient_cust on recipient_cust.id = comp.recipient_id
                           left join customer.customer_details recipient_cust_det
                                     on recipient_cust.last_customer_detail_id = recipient_cust_det.id) as compensations
            where (
                (
                    :prompt is null
                        or (
                        :searchBy = 'ALL'
                            and (
                            (lower(compensations.number) like :prompt)
                                or (lower(compensations.period) like :prompt)
                                or (lower(text(compensations.documentVolumes)) like :prompt)
                                or (lower(text(compensations.price)) like :prompt)
                                or (lower(compensations.compensationReason) like :prompt)
                                or (lower(text(compensations.amount)) like :prompt)
                                or (lower(compensations.customer) like :prompt)
                                or (lower(compensations.pod) like :prompt)
                                or (lower(compensations.recipient) like :prompt)
                            )
                        )
                        or (:searchBy = 'NUMBER' and lower(compensations.number) like :prompt)
                        or (:searchBy = 'PERIOD' and lower(text(compensations.period)) like :prompt)
                        or (:searchBy = 'DOCUMENT_VOLUMES' and lower(text(compensations.documentVolumes)) like :prompt)
                        or (:searchBy = 'PRICE' and lower(text(compensations.price)) like :prompt)
                        or (:searchBy = 'COMPENSATION_REASON' and lower(compensations.compensationReason) like :prompt)
                        or (:searchBy = 'AMOUNT' and text(compensations.amount) like :prompt)
                        or (:searchBy = 'CUSTOMER' and lower(compensations.customer) like :prompt)
                        or (:searchBy = 'POD' and lower(compensations.pod) like :prompt)
                        or (:searchBy = 'RECIPIENT' and lower(compensations.recipient) like :prompt)
                    )
                )
              and (coalesce(:compensationStatuses, '0') = '0'
                or (text(compensations.compensationStatus) = any (:compensationStatuses :: text[])))
              and (:compensationCurrencies is null or currencyId in (:compensationCurrencies))
              and (to_date(text(:date), 'yyyy-MM-dd') is null or (compensations.date = to_date(text(:date), 'yyyy-MM-dd')))
              and (coalesce(:validStatuses, '0') = '0'
                or (text(compensations.status) = any (:validStatuses :: text[])))
            """, countQuery = """
            select count(compensations.id)
            from (select comp.id                                             as id,
                         comp.compensation_document_number                   as number,
                         comp.compensation_document_date                     as date,
                         to_char(comp.compensation_document_period, 'Month') as period,
                         comp.compensation_document_volumes                  as documentVolumes,
                         comp.compensation_document_price                    as price,
                         comp.compensation_reason                            as compensationReason,
                         comp.compensation_document_amount                   as amount,
                         cur.name                                            as currency,
                         cur.id                                              as currencyId,
                         (case
                              when comp_cust.customer_type = 'PRIVATE_CUSTOMER'
                                  then concat(
                                      comp_cust.identifier,
                                      ' (',
                                      comp_cust.identifier,
                                      comp_cust_det.middle_name,
                                      comp_cust_det.last_name,
                                      ')'
                                       )
                              when comp_cust.customer_type = 'LEGAL_ENTITY'
                                  then concat(
                                      comp_cust.identifier,
                                      ' (',
                                      comp_cust_det.name,
                                      ')'
                                       )
                             end
                             )                                               as customer,
                         pod.identifier                                      as pod,
                         (case
                              when recipient_cust.customer_type = 'PRIVATE_CUSTOMER'
                                  then concat(
                                      recipient_cust.identifier,
                                      ' (',
                                      recipient_cust.identifier,
                                      recipient_cust_det.middle_name,
                                      recipient_cust_det.last_name,
                                      ')'
                                       )
                              when recipient_cust.customer_type = 'LEGAL_ENTITY'
                                  then concat(
                                      recipient_cust.identifier,
                                      ' (',
                                      recipient_cust_det.name,
                                      ')'
                                       )
                             end
                             )                                               as recipient,
                         comp.compensation_status                            as compensationStatus,
                         comp.status                                         as status,
                         comp.compensation_index                             as index,
                         cast(comp.invoice_usage_date as date)               as invoice_usage_date,
                         comp.create_date                                    as create_date
                  from billing.compensations comp
                           join customer.customers comp_cust on comp.customer_id = comp_cust.id
                           join customer.customer_details comp_cust_det on comp_cust.last_customer_detail_id = comp_cust_det.id
                           join pod.pod pod on pod.id = comp.pod_id
                           left join nomenclature.currencies cur on comp.compensation_document_currency_id = cur.id
                           left join customer.customers recipient_cust on recipient_cust.id = comp.recipient_id
                           left join customer.customer_details recipient_cust_det
                                     on recipient_cust.last_customer_detail_id = recipient_cust_det.id) as compensations
            where (
                (
                    :prompt is null
                        or (
                        :searchBy = 'ALL'
                            and (
                            (lower(compensations.number) like :prompt)
                                or (lower(compensations.period) like :prompt)
                                or (lower(text(compensations.documentVolumes)) like :prompt)
                                or (lower(text(compensations.price)) like :prompt)
                                or (lower(compensations.compensationReason) like :prompt)
                                or (lower(text(compensations.amount)) like :prompt)
                                or (lower(compensations.customer) like :prompt)
                                or (lower(compensations.pod) like :prompt)
                                or (lower(compensations.recipient) like :prompt)
                            )
                        )
                        or (:searchBy = 'NUMBER' and lower(compensations.number) like :prompt)
                        or (:searchBy = 'PERIOD' and lower(text(compensations.period)) like :prompt)
                        or (:searchBy = 'DOCUMENT_VOLUMES' and lower(text(compensations.documentVolumes)) like :prompt)
                        or (:searchBy = 'PRICE' and lower(text(compensations.price)) like :prompt)
                        or (:searchBy = 'COMPENSATION_REASON' and lower(compensations.compensationReason) like :prompt)
                        or (:searchBy = 'AMOUNT' and text(compensations.amount) like :prompt)
                        or (:searchBy = 'CUSTOMER' and lower(compensations.customer) like :prompt)
                        or (:searchBy = 'POD' and lower(compensations.pod) like :prompt)
                        or (:searchBy = 'RECIPIENT' and lower(compensations.recipient) like :prompt)
                    )
                )
              and (coalesce(:compensationStatuses, '0') = '0'
                or (text(compensations.compensationStatus) = any (:compensationStatuses :: text[])))
              and (:compensationCurrencies is null or currencyId in (:compensationCurrencies))
              and (to_date(text(:date), 'yyyy-MM-dd') is null or (compensations.date = to_date(text(:date), 'yyyy-MM-dd')))
              and (coalesce(:validStatuses, '0') = '0'
                or (text(compensations.status) = any (:validStatuses :: text[])))
            """, nativeQuery = true)
    Page<CompensationListingProjection> filter(
            String prompt,
            String compensationStatuses,
            List<Long> compensationCurrencies,
            LocalDate date,
            String searchBy,
            String validStatuses,
            Pageable pageable
    );

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                c.id,
                text(c.id)
            )
            from Compensations c
            where c.invoiceId = :invoiceId
            and c.compensationIndex = :compensationIndex
            and c.status = 'ACTIVE'
            """)
    List<ShortResponse> findByInvoiceIdAndCompensationIndexAndStatus(
            Long invoiceId,
            Integer compensationIndex
    );

    @Query("""
            select c
            from Compensations c
            where c.invoiceId = :invoiceId
            and c.compensationIndex = :compensationIndex
            and c.status = 'ACTIVE'
            """)
    List<Compensations> findByInvoiceIdAndCompensationIndex(
            Long invoiceId,
            Integer compensationIndex
    );

    @Query(value = """
            select distinct comp.*
            from invoice.invoices inv
                     join billing.compensations comp on comp.customer_id = inv.customer_id
                     join customer.customers c on c.id = comp.customer_id
                     join pod.pod pod on pod.id = comp.pod_id
                     left join invoice.invoice_standard_detailed_data isdd on isdd.pod_id = pod.id
                     left join invoice.invoice_standard_detailed_data_vat_base isddvb on isddvb.pod_id = pod.id
            where inv.id = :invoiceId
              and comp.status = 'ACTIVE'
              and c.status = 'ACTIVE'
              and pod.status = 'ACTIVE'
              and comp.compensation_status = 'UNINVOICED'
              and comp.compensation_index is null
            """, nativeQuery = true)
    List<Compensations> findAllCompensationsForInvoice(Long invoiceId);


    @Query("""
            select new bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentCompensationDAO(comp.compensationDocumentNumber,comp.compensationDocumentDate,comp.compensationDocumentPeriod,comp.compensationDocumentVolumes,comp.compensationDocumentAmount,cur.name,comp.compensationReason,pod.identifier)
            from  Compensations comp
            left join Currency cur on cur.id=comp.compensationDocumentCurrencyId
            left join PointOfDelivery  pod on pod.id=comp.podId
            left join  Invoice inv on inv.id = comp.invoiceId
            where comp.invoiceId = :invoiceId
                and comp.compensationIndex = inv.compensationIndex
                and comp.status = 'ACTIVE'
            """)
    List<BillingRunDocumentCompensationDAO> findCompensationDaoByInvoiceId(Long invoiceId);

}
