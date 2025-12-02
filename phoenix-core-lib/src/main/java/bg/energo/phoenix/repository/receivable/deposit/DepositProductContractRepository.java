package bg.energo.phoenix.repository.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.deposit.DepositProductContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface DepositProductContractRepository extends JpaRepository<DepositProductContract, Long> {

    @Query("""
            select dpc
            from DepositProductContract dpc
            where dpc.customerDepositId = :depositId
            and dpc.status in :statuses
            """)
    List<DepositProductContract> findDepositProductContractByDepositIdAndStatus(@Param("depositId") Long depositId,
                                                                                @Param("statuses") List<EntityStatus> statuses);

    @Query("""
            select dpc
            from DepositProductContract dpc
            where dpc.contractId = :contractId
            and dpc.customerDepositId= :depositId
            and dpc.status = :status
            """)
    Optional<DepositProductContract> findByContractIdAndDepositIdAndStatus(Long contractId, Long depositId, EntityStatus status);


    Optional<DepositProductContract> findByIdAndStatus(Long id, EntityStatus status);
}
