package bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest;

import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsCheckedPods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisconnectionPowerSupplyRequestsCheckedPodsRepository extends JpaRepository<DisconnectionPowerSupplyRequestsCheckedPods, Long> {

    @Query(
            """
                    select dpsrp.podId from DisconnectionPowerSupplyRequestsCheckedPods dpsrp
                    where dpsrp.powerSupplyDisconnectionRequestId = :id
                    """
    )
    List<Long> findByPowerSupplyDisconnectionRequestId(Long id);

    List<DisconnectionPowerSupplyRequestsCheckedPods> findAllByPodIdIn(List<Long> podId);

}
