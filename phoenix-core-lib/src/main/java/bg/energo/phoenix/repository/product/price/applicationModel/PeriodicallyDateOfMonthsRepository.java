package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.PeriodicallyDateOfMonths;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.PeriodicallyDateOfMonthsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeriodicallyDateOfMonthsRepository extends JpaRepository<PeriodicallyDateOfMonths,Long > {
    List<PeriodicallyDateOfMonths> findAllByOverTimePeriodicallyIdAndStatusIn(Long overTimePeriodicallyId,List<PeriodicallyDateOfMonthsStatus> statuses);
}
