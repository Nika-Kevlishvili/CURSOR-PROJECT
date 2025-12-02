package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriodsProfiles;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SettlementPeriodsProfileRepository extends JpaRepository<SettlementPeriodsProfiles,Long> {
    List<SettlementPeriodsProfiles> findAllByVolumesBySettlementPeriodIdAndStatusIn(Long modelId, List<ApplicationModelSubObjectStatus> statuses);


    @Query("""
             select spp from SettlementPeriodsProfiles spp
             join VolumesBySettlementPeriod vsp on vsp.id=spp.volumesBySettlementPeriod.id
            where vsp.applicationModel.id in (:apIds)
            and vsp.status='ACTIVE'
            and spp.status='ACTIVE'
                   
                 """)
    List<SettlementPeriodsProfiles> findAllByApplicationModelId(Collection<Long> apIds);
}
