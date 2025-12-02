package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductTerminations;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductTerminationRepository extends JpaRepository<ProductTerminations, Long> {
    List<ProductTerminations> findByProductDetailsIdAndProductSubObjectStatusIn(Long productDetailsId, List<ProductSubObjectStatus> statuses);
}
