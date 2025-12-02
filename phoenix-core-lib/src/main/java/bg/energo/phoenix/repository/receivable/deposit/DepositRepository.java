package bg.energo.phoenix.repository.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByDeposit;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import bg.energo.phoenix.model.response.receivable.deposit.DepositListingMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, Long> {

    @Query(value = "select nextval('receivable.customer_deposits_id_seq')", nativeQuery = true)
    Long getNextSequenceValue();

    @Query("""
            select d
            from Deposit d
            where d.id = :id
            and d.status in :list
            """)
    Optional<Deposit> findByIdAndStatusIn(Long id, @Param("list") List<EntityStatus> list);

    @Query(nativeQuery = true,
            value = """
                    select id,
                           depositNumber,
                           customerNumber,
                           contractOrderNumber,
                           paymentDeadline,
                           initialAmount,
                           currentAmount,
                           currencyName,
                           status,
                           canDelete
                    from (select cdep.id                                                                    as id,
                                 cdep.deposit_number                                                        as depositNumber,
                                 case
                                     when c.customer_type = 'PRIVATE_CUSTOMER' then
                                         concat(
                                                 c.identifier, ' (',
                                                 cd.name,
                                                 case when cd.middle_name is not null then concat(' ', cd.middle_name) else '' end,
                                                 case when cd.last_name is not null then concat(' ', cd.last_name) else '' end,
                                                 ')'
                                         )
                                     when c.customer_type = 'LEGAL_ENTITY' then
                                         concat(c.identifier, ' (', cd.name, ' ', lf.name, ')')
                                     end                                                                    as customerNumber,
                                 (case
                                      when :contractOrderDirection = 'ASC' then vcdco.contract_order_number
                                      when :contractOrderDirection = 'DESC' then vcdco.contract_order_number_desc
                                      else vcdco.contract_order_number end)                                 as contractOrderNumber,
                                 cdep.payment_deadline                                                      as paymentDeadline,
                                 cdep.initial_amount                                                        as initialAmount,
                                 cdep.current_amount                                                        as currentAmount,
                                 (select name from nomenclature.currencies c where cdep.currency_id = c.id) as currencyName,
                                 cdep.status                                                                as status,
                                 cdep.currency_id,
                                 (SELECT CASE WHEN COUNT(*) > 0 THEN FALSE ELSE TRUE END
                                  FROM receivable.customer_liabilitie_paid_by_deposits clpd
                                  WHERE clpd.customer_deposit_id = cdep.id)                                 AS canDelete
                          from receivable.customer_deposits cdep
                                   join customer.customers c on cdep.customer_id = c.id
                                   join customer.customer_details cd on c.last_customer_detail_id = cd.id
                                   left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                                   left join receivable.vw_customer_deposit_contract_orders vcdco on vcdco.customer_deposit_id = cdep.id
                          where ((:currencyIds) is null or cdep.currency_id in :currencyIds)
                            and (date(:paymentDeadlineDateFrom) is null or cdep.payment_deadline >= date(:paymentDeadlineDateFrom))
                            and (date(:paymentDeadlineDateTo) is null or cdep.payment_deadline <= date(:paymentDeadlineDateTo))
                            and (coalesce(:initialAmountFrom, '0') = '0' or cdep.initial_amount >= :initialAmountFrom)
                            and (coalesce(:initialAmountTo, '0') = '0' or cdep.initial_amount <= :initialAmountTo)
                            and (coalesce(:currentAmountFrom, '0') = '0' or cdep.current_amount >= :currentAmountFrom)
                            and (coalesce(:currentAmountTo, '0') = '0' or cdep.current_amount <= :currentAmountTo)
                            and ((:statuses) is null or text(cdep.status) in :statuses)
                            and (:prompt is null
                              or (:searchBy = 'ALL' and (lower(cdep.deposit_number) like :prompt
                                  or c.identifier like :prompt
                                  or vcdco.contract_order_number like :prompt
                                  ))
                              or (:searchBy = 'CUSTOMER_NUMBER' and lower(text(c.customer_number)) like :prompt)
                              or (:searchBy = 'DEPOSIT_NUMBER' and lower(text(cdep.deposit_number)) like :prompt)
                              or (:searchBy = 'CONTRACT_ORDER' and lower(text(vcdco.contract_order_number)) like :prompt)
                              )) as tbl
                    """,
            countQuery = """
                   select count(tbl.id)
                    from (select cdep.id                                                                    as id,
                                 cdep.deposit_number                                                        as depositNumber,
                                 case
                                     when c.customer_type = 'PRIVATE_CUSTOMER'
                                         then concat(c.identifier, concat(' (', cd.name),
                                                     case when cd.middle_name is not null then cd.middle_name end,
                                                     case when cd.last_name is not null then cd.last_name end, ')')
                                     when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier, ' (', cd.name, ')')
                                     end                                                                       customerNumber,
                                 (case
                                      when :contractOrderDirection = 'ASC' then vcdco.contract_order_number
                                      when :contractOrderDirection = 'DESC' then vcdco.contract_order_number_desc
                                      else vcdco.contract_order_number end)                                 as contractOrderNumber,
                                 cdep.payment_deadline                                                      as paymentDeadline,
                                 cdep.initial_amount                                                        as initialAmount,
                                 cdep.current_amount                                                        as currentAmount,
                                 (select name from nomenclature.currencies c where cdep.currency_id = c.id) as currencyName,
                                 cdep.status                                                                as status,
                                 cdep.currency_id
                          from receivable.customer_deposits cdep
                                   join customer.customers c on cdep.customer_id = c.id
                                   join customer.customer_details cd on c.last_customer_detail_id = cd.id
                                   left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                                   left join receivable.vw_customer_deposit_contract_orders vcdco on vcdco.customer_deposit_id = cdep.id
                          where ((:currencyIds) is null or cdep.currency_id in :currencyIds)
                            and (date(:paymentDeadlineDateFrom) is null or cdep.payment_deadline >= date(:paymentDeadlineDateFrom))
                            and (date(:paymentDeadlineDateTo) is null or cdep.payment_deadline <= date(:paymentDeadlineDateTo))
                            and (coalesce(:initialAmountFrom, '0') = '0' or cdep.initial_amount >= :initialAmountFrom)
                            and (coalesce(:initialAmountTo, '0') = '0' or cdep.initial_amount <= :initialAmountTo)
                            and (coalesce(:currentAmountFrom, '0') = '0' or cdep.current_amount >= :currentAmountFrom)
                            and (coalesce(:currentAmountTo, '0') = '0' or cdep.current_amount <= :currentAmountTo)
                            and ((:statuses) is null or text(cdep.status) in :statuses)
                            and (:prompt is null
                              or (:searchBy = 'ALL' and (lower(cdep.deposit_number) like :prompt
                                  or c.identifier like :prompt
                                  or vcdco.contract_order_number like :prompt
                                  ))
                              or (:searchBy = 'CUSTOMER_NUMBER' and lower(text(c.customer_number)) like :prompt)
                              or (:searchBy = 'DEPOSIT_NUMBER' and lower(text(cdep.deposit_number)) like :prompt)
                              or (:searchBy = 'CONTRACT_ORDER' and lower(text(vcdco.contract_order_number)) like :prompt)
                              )) as tbl
                    """
    )
    Page<DepositListingMiddleResponse> filter(
            @Param("currencyIds") List<Long> currencyIds,
            @Param("paymentDeadlineDateFrom") LocalDate paymentDeadlineDateFrom,
            @Param("paymentDeadlineDateTo") LocalDate paymentDeadlineDateTo,
            @Param("initialAmountFrom") BigDecimal initialAmountFrom,
            @Param("initialAmountTo") BigDecimal initialAmountTo,
            @Param("currentAmountFrom") BigDecimal currentAmountFrom,
            @Param("currentAmountTo") BigDecimal currentAmountTo,
            @Param("contractOrderDirection") String contractOrderDirection, // TODO: 19.04.2024 some silent exception handling here, need to fix
            @Param("searchBy") String searchBy,
            @Param("statuses") List<String> statuses,
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query(value = """
            select clpbd
            from CustomerLiabilityPaidByDeposit clpbd
            where clpbd.customerDepositId = :customerDepositId
            and clpbd.status = 'ACTIVE'
            """)
    List<CustomerLiabilityPaidByDeposit> getCustomerLiabilitiesPaidByDeposit(
            @Param("customerDepositId") Long customerDepositId
    );

    Optional<Deposit> findByDepositNumber(String number);

    @Query(value = """
                SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
                FROM receivable.customer_liabilitie_paid_by_deposits clpd
                WHERE clpd.customer_deposit_id = :depositId
            """, nativeQuery = true)
    boolean isDepositUsedInOffsetting(@Param("depositId") Long depositId);

    @Query("""
                    select c,p from CustomerLiability c
                    join Deposit  p on c.depositId= p.id
                    where c.currentAmount=0
                    and c.status= 'ACTIVE'
                    and p.status='ACTIVE'
                    and (c.addedToDeposit=false or c.addedToDeposit is null)
            """)
    List<Object[]> liabilitiesForDepositJob();
}
