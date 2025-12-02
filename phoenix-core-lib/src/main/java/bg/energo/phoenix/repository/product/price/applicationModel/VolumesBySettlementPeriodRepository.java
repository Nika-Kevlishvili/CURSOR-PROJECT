package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.VolumesBySettlementPeriod;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolumesBySettlementPeriodRepository extends JpaRepository<VolumesBySettlementPeriod,Long> {
    Optional<VolumesBySettlementPeriod> findByApplicationModelIdAndStatusIn(Long applicationModelId, List<ApplicationModelSubObjectStatus> statuses);

    @Query("""
        select vp.percentage from VolumesBySettlementPeriod vsp
        join SettlementPeriodsProfiles vp on vp.volumesBySettlementPeriod.id=vsp.id
        where vp.profileId=:profileId
        and vsp.applicationModel.id=:apId
        and vsp.status='ACTIVE'
        and vp.status='ACTIVE'
""")
    Double findProfilePercentage(Long apId,Long profileId);
}
