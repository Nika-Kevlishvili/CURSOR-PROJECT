package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductGroupOfInterimAndAdvancePayments;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductGroupOfInterimAndAdvancePaymentsRepository extends JpaRepository<ProductGroupOfInterimAndAdvancePayments, Long> {
    List<ProductGroupOfInterimAndAdvancePayments> findByProductDetailsIdAndProductSubObjectStatusIn(Long serviceDetailsId, List<ProductSubObjectStatus> statuses);
}
