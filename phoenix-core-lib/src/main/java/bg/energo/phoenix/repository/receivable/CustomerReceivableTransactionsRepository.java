package bg.energo.phoenix.repository.receivable;

import bg.energo.phoenix.model.entity.receivable.CustomerReceivableTransactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerReceivableTransactionsRepository extends JpaRepository<CustomerReceivableTransactions, Long> {

    @Query("SELECT crt FROM CustomerReceivableTransactions crt " +
           "WHERE crt.sourceObjectType = 'PAYMENT' " +
           "AND crt.destinationObjectType = 'LIABILITY' " +
           "AND crt.status = 'ACTIVE' " +
           "AND crt.operationContext IN ('APO', 'OPO')" +
           "AND crt.sourceObjectId = :paymentId")
    List<CustomerReceivableTransactions> findPaymentConnectedToLiabilityViaAutomaticOffsetting(
            @Param("paymentId") Long paymentId
    );

    @Query("""
            select crt from CustomerReceivableTransactions crt
            where crt.sourceObjectType = 'PAYMENT'
            and crt.destinationObjectType = 'RECEIVABLE'
            and crt.destinationObjectId = :receivableId
            and crt.sourceObjectId = :paymentId
            and crt.status = 'ACTIVE'
            """)
    List<CustomerReceivableTransactions> findNonReversedReceivableTransactions(
            @Param("receivableId") Long receivableId,
            @Param("paymentId") Long paymentId
    );

}
