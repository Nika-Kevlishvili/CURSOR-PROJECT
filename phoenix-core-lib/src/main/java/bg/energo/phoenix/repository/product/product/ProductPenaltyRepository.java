package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductPenalty;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPenaltyRepository extends JpaRepository<ProductPenalty, Long> {
    List<ProductPenalty> findAllByProductDetailsIdAndProductSubObjectStatusIn(Long productDetailId, List<ProductSubObjectStatus> statuses);
}
