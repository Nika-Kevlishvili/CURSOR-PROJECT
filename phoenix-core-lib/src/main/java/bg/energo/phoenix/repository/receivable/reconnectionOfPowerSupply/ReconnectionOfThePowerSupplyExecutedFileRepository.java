package bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyExecutedFile;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReconnectionOfThePowerSupplyExecutedFileRepository extends JpaRepository<ReconnectionOfThePowerSupplyExecutedFile,Long> {

    Optional<ReconnectionOfThePowerSupplyExecutedFile> findByIdAndStatus(Long id, ReceivableSubObjectStatus status);
}
