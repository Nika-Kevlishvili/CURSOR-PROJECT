package bg.energo.phoenix.cashterminal.repository;

import bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashTerminalPaymentDetailsRepository extends JpaRepository<CashTerminalPaymentDetailsEntity, Long> {
    List<CashTerminalPaymentDetailsEntity> findByCashTerminalPaymentId(Long cashTerminalPaymentId);

    List<CashTerminalPaymentDetailsEntity> findByLiabilityIdInAndCashTerminalPaymentId(
            @Param("liabilityId") List<Long> liabilityIds,
            @Param("cashTerminalPaymentId") Long cashTerminalPaymentId
    );
}

