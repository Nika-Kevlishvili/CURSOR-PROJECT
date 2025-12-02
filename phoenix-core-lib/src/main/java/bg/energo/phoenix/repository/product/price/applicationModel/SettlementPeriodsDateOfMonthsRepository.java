package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriodsDateOfMonths;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementPeriodsDateOfMonthsRepository extends JpaRepository<SettlementPeriodsDateOfMonths,Long> {
    List<SettlementPeriodsDateOfMonths> findAllByVolumesBySettlementPeriodIdAndStatusIn(Long modelId, List<ApplicationModelSubObjectStatus> statuses);

    @Query("""
            select spdm from SettlementPeriodsDateOfMonths spdm
            join ApplicationModel am on spdm.volumesBySettlementPeriod.applicationModel.id=am.id
            where am.id=:apId
            and spdm.volumesBySettlementPeriod.status='ACTIVE'
            and spdm.status='ACTIVE'
            """)
    List<SettlementPeriodsDateOfMonths> findAllByApplicationModelId(Long apId);
}
