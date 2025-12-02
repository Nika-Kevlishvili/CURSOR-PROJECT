package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductInterimAndAdvancePayments;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductInterimAndAdvancePaymentRepository extends JpaRepository<ProductInterimAndAdvancePayments, Long> {
    List<ProductInterimAndAdvancePayments> findByProductDetailsIdAndProductSubObjectStatusIn(Long serviceDetailId, List<ProductSubObjectStatus> statuses);
}
