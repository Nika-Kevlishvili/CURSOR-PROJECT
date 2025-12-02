package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunPerPieceDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunPerPieceDetailsRepository extends JpaRepository<BillingRunPerPieceDetails, Long> {
    List<BillingRunPerPieceDetails> findAllByRunContractId(Long runContractId);
}
