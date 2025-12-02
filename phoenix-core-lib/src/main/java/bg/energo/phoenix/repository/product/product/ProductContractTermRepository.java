package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductContractTerms;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.response.service.ContractTermNameResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductContractTermRepository extends JpaRepository<ProductContractTerms, Long> {

    List<ProductContractTerms> findAllByProductDetailsIdAndStatusInOrderByCreateDate(Long id, List<ProductSubObjectStatus> statuses);

    @Query(
            value = """
                    select distinct new bg.energo.phoenix.model.response.service.ContractTermNameResponse(
                        pct.name
                    )
                    from ProductContractTerms pct
                    join ProductDetails pd on pd.id = pct.productDetailsId
                        where pct.status in :statuses
                        and pd.product.productStatus = 'ACTIVE'
                        and (:prompt is null or lower(pct.name) like :prompt)
                    """
    )
    Page<ContractTermNameResponse> findDistinctNameByStatusIn(
            @Param("statuses") List<ProductSubObjectStatus> status,
            @Param("prompt") String prompt,
            Pageable pageable
    );

}
