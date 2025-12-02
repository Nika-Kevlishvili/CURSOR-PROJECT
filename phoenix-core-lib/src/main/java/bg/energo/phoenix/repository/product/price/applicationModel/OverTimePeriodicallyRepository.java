package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.OverTimePeriodically;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimePeriodicallyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OverTimePeriodicallyRepository extends JpaRepository<OverTimePeriodically,Long > {

    Optional<OverTimePeriodically> findByApplicationModelIdAndStatusIn(Long applicationModelId, List<OverTimePeriodicallyStatus> statusList);
}
