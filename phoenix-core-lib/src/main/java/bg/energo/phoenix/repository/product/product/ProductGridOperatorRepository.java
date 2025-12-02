package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductGridOperator;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductGridOperatorRepository extends JpaRepository<ProductGridOperator, Long> {
    List<ProductGridOperator> findAllByProductDetailsIdAndProductSubObjectStatus(Long productDetailId, ProductSubObjectStatus status);

}
