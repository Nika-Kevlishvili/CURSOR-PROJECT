package bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLOCustomerDeposits;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.LiabilitiesOffsettingChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MLOCustomerDepositsRepository extends JpaRepository<MLOCustomerDeposits, Long> {

    @Query(
            """
                    select new bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.LiabilitiesOffsettingChoice(
                    cd.customerDepositId,
                    concat(d.depositNumber,'-',cd.afterCurrentAmount),
                    d.customerId,
                    d.currentAmount,
                    d.currencyId)
                    from MLOCustomerDeposits as cd
                    left join Deposit as d
                    on cd.customerDepositId = d.id
                    where cd.manualLiabilityOffsettingId = :id
                    """
    )
    List<LiabilitiesOffsettingChoice> getDepositIdsIdsByManualOffsettingId(@Param("id") Long id);

    @Query(
            """
                    select cd
                    from MLOCustomerDeposits as cd
                    left join Deposit as d
                    on cd.customerDepositId = d.id
                    where cd.manualLiabilityOffsettingId = :id
                    """
    )
    List<MLOCustomerDeposits> getDepositInfoByManualOffsettingId(@Param("id") Long id);

    List<MLOCustomerDeposits> findMLOCustomerDepositsByManualLiabilityOffsettingIdAndCustomerDepositId(Long id, Long depositId);

    List<MLOCustomerDeposits> findMLOCustomerDepositsByManualLiabilityOffsettingId(Long manualLiabilityOffsettingId);
}
