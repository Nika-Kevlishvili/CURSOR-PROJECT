package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.Scales;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.VolumesByScaleScales;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.VolumesByScaleScalesStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VolumesByScaleScalesRepository extends JpaRepository<VolumesByScaleScales, Long> {

    List<VolumesByScaleScales> findAllByVolumesByScaleIdAndStatus(Long volumesId, VolumesByScaleScalesStatus status);

    @Query("""
            select vc.scales from VolumesByScaleScales vc
            where vc.volumesByScale.id=:volumesId
            and vc.status=:status
            """)
    List<Scales> findScalesByVolumesByScaleAndStatus(Long volumesId, VolumesByScaleScalesStatus status);
}
