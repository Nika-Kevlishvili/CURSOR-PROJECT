package bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLOCustomerReceivables;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.LiabilitiesOffsettingChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MLOCustomerReceivablesRepository extends JpaRepository<MLOCustomerReceivables, Long> {

    @Query("""
        select new bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.LiabilitiesOffsettingChoice(
        crv.id,
        CASE
            WHEN crv.outgoingDocumentFromExternalSystem IS NOT NULL
            THEN concat(crv.outgoingDocumentFromExternalSystem, '/', coalesce(to_char(crv.occurrenceDate, 'DD.MM.YYYY'), 'N/A'), ' | ', coalesce(to_char(crv.occurrenceDate, 'DD.MM.YYYY'), 'N/A') , ' | -', abs(crv.currentAmount), ' ', c.name)
            ELSE concat(crv.receivableNumber, '/', coalesce(to_char(crv.occurrenceDate, 'DD.MM.YYYY'), 'N/A') , ' | ', crv.dueDate, ' | -', abs(crv.currentAmount), ' ', c.name)
        END,
        crv.customerId,
        crv.currentAmount,
        crv.currencyId
        )
        from MLOCustomerReceivables cr
        left join CustomerReceivable crv on cr.customerReceivablesId = crv.id
        join Currency c on crv.currencyId = c.id
        where cr.manualLiabilityOffsettingId = :id
        order by crv.id asc
        """)
    List<LiabilitiesOffsettingChoice> getReceivableIdsIdsByManualOffsettingId(@Param("id") Long id);

    List<MLOCustomerReceivables> findAllByCustomerReceivablesId(Long customerReceivableId);
}
