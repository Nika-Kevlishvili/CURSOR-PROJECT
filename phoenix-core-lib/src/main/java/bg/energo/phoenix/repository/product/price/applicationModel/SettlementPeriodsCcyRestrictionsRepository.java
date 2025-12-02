package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriodsCcyRestrictions;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementPeriodsCcyRestrictionsRepository extends JpaRepository<SettlementPeriodsCcyRestrictions, Long> {
    List<SettlementPeriodsCcyRestrictions> findAllByVolumesBySettlementPeriodIdAndStatusIn(Long modelId, List<ApplicationModelSubObjectStatus> statuses);
}
