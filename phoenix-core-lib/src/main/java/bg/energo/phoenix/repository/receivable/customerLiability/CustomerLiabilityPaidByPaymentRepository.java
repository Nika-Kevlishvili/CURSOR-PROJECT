package bg.energo.phoenix.repository.receivable.customerLiability;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerLiabilityPaidByPaymentRepository extends JpaRepository<CustomerLiabilityPaidByPayment, Long> {
    List<CustomerLiabilityPaidByPayment> findByCustomerLiabilityIdAndStatus(Long customerLiabilityId, EntityStatus status);
    boolean existsByCustomerPaymentId(Long paymentId);

    @Query("""
        select p,clp,cl.id from CustomerLiabilityPaidByPayment clp
        join CustomerLiability cl on clp.customerLiabilityId=cl.id
        join Payment p on clp.customerPaymentId=p.id
        where clp.operationContext='APO'
        and clp.customerLiabilityId in (:ids)
        order by cl.dueDate
""")
    List<Object[]> findPaymentsByLiabilityIds(List<Long> ids);
}
