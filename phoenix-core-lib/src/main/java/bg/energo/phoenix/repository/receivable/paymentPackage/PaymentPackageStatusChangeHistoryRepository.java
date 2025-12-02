package bg.energo.phoenix.repository.receivable.paymentPackage;

import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackageStatusChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentPackageStatusChangeHistoryRepository extends JpaRepository<PaymentPackageStatusChangeHistory, Long> {

    Optional<List<PaymentPackageStatusChangeHistory>> findAllByPaymentPackageId(Long paymentPackageId);

    @Modifying
    void deleteAllByPaymentPackageId(Long paymentPackageId);
}
