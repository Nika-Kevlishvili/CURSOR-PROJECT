package bg.energo.phoenix.repository.receivable.disconnectionPowerSupply;

import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply.DisconnectionPowerSupplyFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisconnectionPowerSupplyFileRepository extends JpaRepository<DisconnectionPowerSupplyFile, Long> {
}
