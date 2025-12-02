package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.PeriodicallyDayWeekPeriodYear;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimePeriodicallyDayWeekYearStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeriodicallyDayWeekPeriodYearRepository extends JpaRepository<PeriodicallyDayWeekPeriodYear,Long > {
    List<PeriodicallyDayWeekPeriodYear> findAllByOverTimePeriodicallyIdAndStatusIn(Long overTimePeriodicallyId, List<OverTimePeriodicallyDayWeekYearStatus> statusList);
}
