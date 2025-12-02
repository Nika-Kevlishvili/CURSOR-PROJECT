package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.VolumesByScale;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.VolumesByScaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolumesByScaleRepository extends JpaRepository<VolumesByScale,Long > {
    Optional<VolumesByScale> findByApplicationModelIdAndStatusIn(Long applicationModelId, List<VolumesByScaleStatus> statusList);
}
