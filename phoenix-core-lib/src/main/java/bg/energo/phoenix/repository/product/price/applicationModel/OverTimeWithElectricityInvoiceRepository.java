package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.OverTimeWithElectricityInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OverTimeWithElectricityInvoiceRepository extends JpaRepository<OverTimeWithElectricityInvoice, Long> {

    Optional<OverTimeWithElectricityInvoice> findByApplicationModelIdAndStatusIn(Long applicationId, List<EntityStatus> status);
}
