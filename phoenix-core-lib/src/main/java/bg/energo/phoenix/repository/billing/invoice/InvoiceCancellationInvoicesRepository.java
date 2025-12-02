package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceCancellationInvoice;
import bg.energo.phoenix.service.billing.invoice.cancellation.InvoiceCancellationDto;
import bg.energo.phoenix.service.billing.invoice.cancellation.InvoiceCancellationShortDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceCancellationInvoicesRepository extends JpaRepository<InvoiceCancellationInvoice,Long> {



    @Query(nativeQuery = true,
    value = """
            -- select max(max_date)
            -- from (select max(coalesce(i.meter_reading_period_to, i.invoice_date)) max_date
            --       from invoice.invoices i
            --       where i.type in ('STANDARD', 'INTERIM_AND_ADVANCE_PAYMENT')
            --         and i.product_contract_id = :_contract_id
            --       union
            --       select max(coalesce(b.max_end_date, b.invoice_date)) max_date
            --       from billing.billings b
            --                inner join
            --            billing_run.run_contracts rc on rc.run_id = b.id and rc.contract_type = 'PRODUCT_CONTRACT'
            --       where rc.contract_id = :_contract_id) as max_date_subquery;


            with cancel_invoices as (select ci.invoice_id as id
                                     from invoice.invoice_cancellation_invoices ci
                                     where ci.invoice_cancellation_id = :cancellationId),

                 billing_profile as (select isdd.id,
                                            isdd.invoice_id,
                                            isdd.detail_type,
                                            unnest(isdd.billing_data_profile_ids) as billingProfileId
                                     from invoice.invoice_standard_detailed_data isdd
                                              join cancel_invoices ci on ci.id = isdd.invoice_id
                                     where isdd.billing_data_profile_ids is not null)
                    ,
                 middle_invoices as
                     (select unnest(bbp.invoice_ids) invoiceIds,
                             bbp.billing_scale_id    scaleId,
                             bbp.billing_run_id      billingRunId,
                             bbp.id,
                             bp.invoice_id as        baseInvoiceId
                      from billing_profile bp
                               join pod.billing_by_profile bbp on bp.billingProfileId = bbp.id)
                    ,
                 profile_invoices as (select array_agg(distinct mi.invoiceIds) as invoiceIds,
                                             mi.id                             as billingProfileIds,
                                             mi.billingRunId                   as billingRunId,
                                             mi.scaleId                        as scaleId,
                                             mi.baseInvoiceId                  as baseInvoiceId
                                      from middle_invoices mi
                                               join invoice.invoices inv on mi.invoiceIds = inv.id
                                      where inv.status = 'REAL'
                                      group by mi.id, mi.baseInvoiceId, mi.scaleId, mi.billingRunId),
                 invoice_ids as (select inv.id, inti.id as interimInvoiceId
                                 from invoice.invoices inv
                                          left join invoice.invoices inti on inti.standard_invoice_id = inv.id
                                          join cancel_invoices ci on ci.id = inv.id),
                 validation_result as (select case
                                                  when exists (select 1
                                                               from invoice.invoices inv2
                                                               where inv2.id = i.id
                                                                 and inv2.type = 'STANDARD')
                                                      then not exists (select unnest(bi.invoiceids)
                                                                       except
                                                                       select *
                                                                       from cancel_invoices)
                                                  else true
                                                  end as validInvoice,
                                              i.*,
                                              bi.*
                                       from invoice_ids i
                                                join profile_invoices bi on bi.baseInvoiceId = i.id),
                 billing_scale as (select isdd.id,
                                          isdd.invoice_id,
                                          isdd.detail_type,
                                          unnest(isdd.billing_data_scale_ids) as billingProfileId
                                   from invoice.invoice_standard_detailed_data isdd
                                            join cancel_invoices ci on ci.id = isdd.invoice_id
                                   where isdd.billing_data_scale_ids is not null)
                    ,
                 middle_scales as
                     (select unnest(bbp.invoice_ids) invoiceIds,
                             bbp.id,
                             bp.invoice_id as        baseInvoiceId
                      from billing_scale bp
                               join pod.billing_by_scale bbp on bp.billingProfileId = bbp.id)
                    ,
                 scale_invoices as (select array_agg(distinct mi.invoiceIds) as invoiceIds,
                                           mi.id                             as billingProfileIds,
                                           mi.baseInvoiceId                  as baseInvoiceId
                                    from middle_scales mi
                                             join invoice.invoices inv on mi.invoiceIds = inv.id
                                    where inv.status = 'REAL'
                                    group by mi.id, mi.baseInvoiceId),
                 scale_validation_result as (select case
                                                        when exists (select 1
                                                                     from invoice.invoices inv2
                                                                     where inv2.id = i.id
                                                                       and inv2.type = 'STANDARD')
                                                            Then not exists (select unnest(bi.invoiceids)
                                                                             except
                                                                             select *
                                                                             from cancel_invoices)
                                                        else true
                                                        end as validInvoice,
                                                    i.*,
                                                    bi.*
                                             from invoice_ids i
                                                      join scale_invoices bi on bi.baseInvoiceId = i.id)

            select vr.baseInvoiceId            as baseInvoiceId,
                   vr.validInvoice             as validInvoice,
                   vr.billingProfileIds        as billingDataId,
                   vr.interimInvoiceId         as interimInvoiceId,
                   vr.billingRunId is not null as shouldDelete,
                   vr.scaleId                  as scaleId,
                   'PROFILE'                   as type
            from validation_result vr
            union
            select scr.baseInvoiceId     as baseInvoiceId,
                   scr.validInvoice      as validInvoice,
                   scr.billingProfileIds as billingDataId,
                   scr.interimInvoiceId  as interimInvoiceId,
                   false                 as shouldDelete,
                   null                  as scaleId,
                   'SCALE'               as type

            from scale_validation_result scr
            """)
    List<InvoiceCancellationDto> findInvoiceToCancel(Long cancellationId);



    @Query(value = """
            with main_invoices as (
                select inv.id,inv.invoice_number,inv.parent_invoice_id
                from invoice.invoice_cancellation_invoices ici
                         join invoice.invoices inv on ici.invoice_id=inv.id or ici.invoice_id=inv.id
            
                where ici.invoice_cancellation_id=:cancellationId
            ),
                 grouped_invoices as (
                     select mi.id as main_id,
                            mi.invoice_number as main_number,
                            inv.id child_id
                     from main_invoices mi
                          join billing.billing_invoices bi on bi.invoice_id=mi.id
                          join invoice.invoices inv on inv.billing_id=bi.billing_id
                     where inv.status='REAL'
                 )
            
            select gi.main_id as baseInvoiceId,
                   gi.main_number,bool_and(gi.child_id in (select mi.id from main_invoices mi)) as validInvoice
            from grouped_invoices gi
            group by gi.main_id,gi.main_number
            """,nativeQuery = true)

    List<InvoiceCancellationShortDto> findInvalidDebitCreditInvoice(Long cancellationId);


    @Query("""
        select inv from InvoiceCancellationInvoice ici
        join Invoice inv on inv.id=ici.invoiceId
        where ici.invoiceCancellationId=:cancellationId
        """)
    List<Invoice> findInvoiceByCancellationId(Long cancellationId);

    @Query(value = """
            with cancelInvoices as (select ici.invoice_id
                                    from invoice.invoice_cancellation_invoices ici
                                    where ici.invoice_cancellation_id = :cancellationId)
            
            select inv.id
            from cancelInvoices ici
                     join invoice.invoices inv
                          on inv.id = ici.invoice_id and inv.type = 'INTERIM_AND_ADVANCE_PAYMENT' and inv.status = 'REAL' and
                             inv.is_deducted = true
            where  EXISTS((select st.id
                              from invoice.invoices st
                              where st.id = inv.standard_invoice_id and st.status='REAL'
                              union
                              distinct
                              select mc.id
                              from billing.billing_invoices bi
                                       join billing.billing_invoices biB on biB.billing_id = bi.billing_id
                                       join invoice.invoices mc on mc.billing_id = bi.billing_id or mc.id = bib.invoice_id
                              where bi.invoice_id = inv.id and mc.status='REAL')
                             except
                             (select * from cancelInvoices))
            """,nativeQuery = true)
    List<Long> findInvalidInterims(Long cancellationId);


}
