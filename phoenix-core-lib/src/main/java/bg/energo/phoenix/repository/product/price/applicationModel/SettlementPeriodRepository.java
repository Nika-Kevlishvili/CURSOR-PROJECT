package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriods;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface SettlementPeriodRepository extends JpaRepository<SettlementPeriods,Long> {
    List<SettlementPeriods> findAllByVolumesBySettlementPeriodIdAndStatusIn(Long modelId, List<ApplicationModelSubObjectStatus> statuses);

    @Query("""
            select sp from SettlementPeriods sp
            join VolumesBySettlementPeriod  vbsp on vbsp.id=sp.volumesBySettlementPeriod.id
            where vbsp.applicationModel.id in :apIds
            and vbsp.status='ACTIVE'
            and sp.status='ACTIVE'
            """)
    List<SettlementPeriods> findAllByApplicationModelIds(Set<Long> apIds);
}
