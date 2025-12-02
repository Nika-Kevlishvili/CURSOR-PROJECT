package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.ApplicationModel;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationModelRepository extends JpaRepository<ApplicationModel,Long > {

    Optional<ApplicationModel> findByPriceComponentIdAndStatusIn(Long priceComponentId, List<ApplicationModelStatus> statusList);

    @Query("""
            select ap from ApplicationModel ap 
            where ap.priceComponent.id in :ids
            and ap.status in :status
            """)
    List<ApplicationModel> findAllByPriceComponentIdsAndStatus(@Param("ids")List<Long> priceComponentIds,@Param("status") List<ApplicationModelStatus> statusList);


    @Query("""
        select count(ap.id)>0 from ApplicationModel ap
        join VolumesBySettlementPeriod sp on sp.applicationModel.id=ap.id
        join SettlementPeriodsProfiles  spp on spp.volumesBySettlementPeriod.id=sp.id
        where spp.profileId=:profileId
        and ap.id=:apId
        and spp.status='ACTIVE'
    """)
    boolean atLeastOneProfileContainsProfile(Long apId,Long profileId);

}
