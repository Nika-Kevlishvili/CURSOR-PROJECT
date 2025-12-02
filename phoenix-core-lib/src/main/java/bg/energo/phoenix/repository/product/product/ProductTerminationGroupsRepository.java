package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductTerminationGroups;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductTerminationGroupsRepository extends JpaRepository<ProductTerminationGroups, Long> {
    List<ProductTerminationGroups> findByProductDetailsIdAndProductSubObjectStatusIn(Long productDetailsId, List<ProductSubObjectStatus> statuses);

    @Query("""
            select ptg from ProductTerminationGroups ptg 
            where ptg.productDetails.id = :productDetailId
            and ptg.productSubObjectStatus='ACTIVE'
                """)
    List<ProductTerminationGroups> findByProductDetailId(Long productDetailId);
}
