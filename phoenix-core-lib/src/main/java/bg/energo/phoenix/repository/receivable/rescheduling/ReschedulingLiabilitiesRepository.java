package bg.energo.phoenix.repository.receivable.rescheduling;

import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingLiabilities;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingContractsResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReschedulingLiabilitiesRepository extends JpaRepository<ReschedulingLiabilities, Long> {

    List<ReschedulingLiabilities> findByReschedulingId(Long reschedulingId);

    @Query("""
        select distinct lpf.id from  ReschedulingLiabilities rl
        join CustomerLiability cl on rl.customerLiabilitieId=cl.id
        join LatePaymentFine lpf on lpf.id=cl.childLatePaymentFineId
        where rl.reschedulingId=:reschedulingId
""")
    List<Long> findFinesByReschedulingId(Long reschedulingId);


    @Query(nativeQuery = true,value = """
        select string_agg(sc.contract_number || '/' || to_char(sc.create_date, 'DD.MM.YYYY'),',')
            as serviceContractNumbers,
               string_agg(pc.contract_number || '/' || to_char(pc.create_date, 'DD.MM.YYYY'),',')
                   as productContractNumbers
        from receivable.reschedulings r
            join receivable.rescheduling_liabilities rl on rl.rescheduling_id = r.id
        join receivable.customer_liabilities cl on rl.customer_liabilitie_id = cl.id
        join invoice.invoices i on cl.invoice_id=i.id
        left join product_contract.contracts pc on i.product_contract_id = pc.id
        left join service_contract.contracts sc on i.service_contract_id = sc.id
        where r.id=:reschedulingId
""")
    Optional<ReschedulingContractsResponse> getContractsByReschedulingId(Long reschedulingId);

}
