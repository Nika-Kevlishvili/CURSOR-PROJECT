package bg.energo.phoenix.repository.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.deposit.DepositServiceContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface DepositServiceContractRepository extends JpaRepository<DepositServiceContract, Long> {

    @Query("""
            select dsc
            from DepositServiceContract dsc
            where dsc.customerDepositId = :depositId
            and dsc.status in :statuses
            """)
    List<DepositServiceContract> findDepositServiceContractByContractIdAndStatus(Long depositId, List<EntityStatus> statuses);

    @Query("""
            select dsc
            from DepositServiceContract dsc
            where dsc.contractId = :contractId
            and dsc.customerDepositId = :depositId
            and dsc.status = :status
            """)
    Optional<DepositServiceContract> findByContractIdAndDepositIdAndStatus(Long contractId,Long depositId, EntityStatus status);

}
