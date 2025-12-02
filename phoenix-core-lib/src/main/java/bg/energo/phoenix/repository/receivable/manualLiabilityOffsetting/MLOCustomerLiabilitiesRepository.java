package bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLOCustomerLiabilities;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.LiabilitiesOffsettingChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MLOCustomerLiabilitiesRepository extends JpaRepository<MLOCustomerLiabilities, Long> {

    @Query("""
        select new bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.LiabilitiesOffsettingChoice(
        cls.id,
        CASE
            WHEN cls.outgoingDocumentFromExternalSystem IS NOT NULL
            THEN concat(cls.outgoingDocumentFromExternalSystem, '/', coalesce(to_char(cls.occurrenceDate, 'DD.MM.YYYY'), 'N/A') , ' | ', cls.dueDate, ' | ', cls.currentAmount, ' ', c.name)
            ELSE concat(cls.liabilityNumber, '/', coalesce(to_char(cls.occurrenceDate, 'DD.MM.YYYY'), 'N/A') , ' | ', cls.dueDate, ' | ', cls.currentAmount, ' ', c.name)
        END,
        cls.customerId,
        cls.currentAmount,
        cls.currencyId
        )
        from MLOCustomerLiabilities cl
        left join CustomerLiability cls on cl.customerLiabilitiesId = cls.id
        join Currency c on cls.currencyId = c.id
        where cl.manualLiabilityOffsettingId = :id
        order by cls.id asc
        """)
    List<LiabilitiesOffsettingChoice> getLiabilityIdsIdsByManualOffsettingId(@Param("id") Long id);
}
