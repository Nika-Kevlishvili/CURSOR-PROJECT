package bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest;

import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsPodLiabilities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisconnectionPowerSupplyRequestsPodLiabilitiesRepository extends JpaRepository<DisconnectionPowerSupplyRequestsPodLiabilities, Long> {

}
