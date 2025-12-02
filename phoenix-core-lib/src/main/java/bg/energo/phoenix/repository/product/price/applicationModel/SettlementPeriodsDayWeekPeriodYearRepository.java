package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.SettlementPeriodsDayWeekPeriodYear;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementPeriodsDayWeekPeriodYearRepository extends JpaRepository<SettlementPeriodsDayWeekPeriodYear,Long> {
    List<SettlementPeriodsDayWeekPeriodYear> findAllByVolumesBySettlementPeriodIdAndStatusIn(Long modelId, List<ApplicationModelSubObjectStatus> statuses);

    @Query("""
        select dwpy from SettlementPeriodsDayWeekPeriodYear  dwpy
        join VolumesBySettlementPeriod sp on dwpy.volumesBySettlementPeriod.id=sp.id
        where sp.applicationModel.id=:apId
        and sp.status='ACTIVE'
        and dwpy.status='ACTIVE'
""")
    List<SettlementPeriodsDayWeekPeriodYear> findAllByApplicationModelId(Long apId);
}
