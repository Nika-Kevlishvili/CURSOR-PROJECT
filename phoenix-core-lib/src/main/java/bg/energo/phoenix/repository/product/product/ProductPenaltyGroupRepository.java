package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductPenaltyGroups;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPenaltyGroupRepository extends JpaRepository<ProductPenaltyGroups, Long> {
    List<ProductPenaltyGroups> findByProductDetailsIdAndProductSubObjectStatusIn(Long productDetailsId, List<ProductSubObjectStatus> statuses);
}
