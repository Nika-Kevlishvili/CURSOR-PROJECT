package bg.energo.phoenix.repository.billing.accountingPeriods;

import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriodsStatusChangeHistory;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsChangeHistoryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountingPeriodsChangeHistoryRepository extends JpaRepository<AccountingPeriodsStatusChangeHistory, Long> {

    @Query(
            """
                    select ap from AccountingPeriodsStatusChangeHistory as ap
                    where ap.accountingPeriodId = :accountingPeriodId
                    order by ap.id desc
                    """
    )
    List<AccountingPeriodsChangeHistoryResponse> getHistory(Long accountingPeriodId);
}
