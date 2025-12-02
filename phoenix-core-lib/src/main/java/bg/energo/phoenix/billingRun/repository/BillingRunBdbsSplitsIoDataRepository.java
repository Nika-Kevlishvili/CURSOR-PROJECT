package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunBdbsSplitsIoData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunBdbsSplitsIoDataRepository extends JpaRepository<BillingRunBdbsSplitsIoData,Long> {

    List<BillingRunBdbsSplitsIoData> findAllByRunContractId(Long runContractId);

}
