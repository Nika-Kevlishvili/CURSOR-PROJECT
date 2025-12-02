package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ProductTermRenewalCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductTermRenewalCountRepository extends JpaRepository<ProductTermRenewalCount,Long> {

    @Query("""
        select rc from ProductTermRenewalCount rc
        where rc.contractDetailId = :contractDetailId
        and rc.productContractTermId = :termId
            """)
    Optional<ProductTermRenewalCount> findByTermIdAndContractDetailId(Long termId, Long contractDetailId);
}
