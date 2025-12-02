package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductSalesArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSalesAreaRepository extends JpaRepository<ProductSalesArea, Long> {
}
