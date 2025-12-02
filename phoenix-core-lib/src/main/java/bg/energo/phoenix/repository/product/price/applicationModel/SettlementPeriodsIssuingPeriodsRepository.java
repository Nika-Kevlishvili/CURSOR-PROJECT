package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriodsIssuingPeriods;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementPeriodsIssuingPeriodsRepository extends JpaRepository<SettlementPeriodsIssuingPeriods,Long> {
    List<SettlementPeriodsIssuingPeriods> findAllByVolumesBySettlementPeriodIdAndStatusIn(Long modelId,List<ApplicationModelSubObjectStatus> statuses);

    @Query("""
     select spip from SettlementPeriodsIssuingPeriods spip
     join VolumesBySettlementPeriod sp on spip.volumesBySettlementPeriod.id=sp.id
     where sp.applicationModel.id =:apId
     and sp.status='ACTIVE'
     and spip.status='ACTIVE'
""")
    List<SettlementPeriodsIssuingPeriods> findAllByApplicationModelId(Long apId);
}
