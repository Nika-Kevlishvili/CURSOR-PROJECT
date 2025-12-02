package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductSegments;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSegmentRepository extends JpaRepository<ProductSegments, Long> {

    List<ProductSegments> findAllByProductDetailsIdAndProductSubObjectStatus(Long productDetailId, ProductSubObjectStatus status);
}
