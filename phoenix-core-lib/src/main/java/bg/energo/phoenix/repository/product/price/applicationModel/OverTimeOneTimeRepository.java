package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.OverTimeOneTime;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeOneTimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OverTimeOneTimeRepository extends JpaRepository<OverTimeOneTime,Long > {

    Optional<OverTimeOneTime> findByApplicationModelIdAndStatusIn(Long applicationId, List<OverTimeOneTimeStatus> status);
}
