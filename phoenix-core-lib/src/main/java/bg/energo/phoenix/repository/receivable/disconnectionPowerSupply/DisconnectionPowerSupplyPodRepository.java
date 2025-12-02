package bg.energo.phoenix.repository.receivable.disconnectionPowerSupply;

import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply.DisconnectionOfPowerSupplyPod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisconnectionPowerSupplyPodRepository extends JpaRepository<DisconnectionOfPowerSupplyPod, Long> {
    void deleteAllByPowerSupplyDisconnectionId(Long id);

    @Query(nativeQuery = true, value = """
        select *
        from receivable.power_supply_disconnection_pods
        where pod_id = :podId
          and customer_id = :customerId
          and power_supply_disconnection_id = :powerSupplyDisconnectionId
        limit 1
    """)
    Optional<DisconnectionOfPowerSupplyPod> findExistingDisconnectionPod(Long podId, Long customerId, Long powerSupplyDisconnectionId);
}
