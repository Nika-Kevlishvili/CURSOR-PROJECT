package bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationPods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;


@Repository
public interface PowerSupplyDcnCancellationPodsRepository extends JpaRepository<PowerSupplyDcnCancellationPods, Long> {

    @Query("""
        select r from PowerSupplyDcnCancellationPods r
        where r.id in :ids
        and r.powerSupplyDcnCancellationId = :pwsId
""")
    Set<PowerSupplyDcnCancellationPods> findByIdsIn(Set<Long> ids, Long pwsId);

    Optional<PowerSupplyDcnCancellationPods> findFirstByPodIdAndPowerSupplyDcnCancellationId(Long podId, Long powerSupplyDcnCancellationId);
}
