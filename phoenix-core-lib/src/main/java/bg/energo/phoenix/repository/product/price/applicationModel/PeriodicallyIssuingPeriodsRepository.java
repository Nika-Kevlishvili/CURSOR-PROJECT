package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.PeriodicallyIssuingPeriods;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.PeriodicallyIssuingPeriodsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeriodicallyIssuingPeriodsRepository extends JpaRepository<PeriodicallyIssuingPeriods,Long > {

    List<PeriodicallyIssuingPeriods> findAllByOverTimePeriodicallyIdAndStatusIn(Long overTimePeriodicallyId, List<PeriodicallyIssuingPeriodsStatus> statuses);
}
