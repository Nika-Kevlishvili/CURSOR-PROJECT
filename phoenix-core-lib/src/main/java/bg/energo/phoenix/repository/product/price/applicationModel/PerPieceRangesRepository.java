package bg.energo.phoenix.repository.product.price.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.PerPieceRanges;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.PerPieceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerPieceRangesRepository extends JpaRepository<PerPieceRanges,Long > {

    List<PerPieceRanges> findByApplicationModelIdAndStatusIn(Long applicationModelId, List<PerPieceStatus> statuses);
}
