package bg.energo.phoenix.repository.receivable.paymentPackage;

import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackageFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentPackageFilesRepository extends JpaRepository<PaymentPackageFiles, Long> {

    PaymentPackageFiles findByPaymentPackageId(Long paymentPackageId);
}
