package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriodsKwhRestrictions;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementPeriodsKwhRestrictionsRepository extends JpaRepository<SettlementPeriodsKwhRestrictions, Long> {
    List<SettlementPeriodsKwhRestrictions> findAllByVolumesBySettlementPeriodIdAndStatusIn(Long modelId, List<ApplicationModelSubObjectStatus> statuses);
}
