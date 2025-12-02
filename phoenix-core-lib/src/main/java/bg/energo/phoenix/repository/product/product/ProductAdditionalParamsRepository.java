package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.product.product.ProductAdditionalParams;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractAdditionalParamsResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductAdditionalParamsRepository extends JpaRepository<ProductAdditionalParams, Long> {

    List<ProductAdditionalParams> findProductAdditionalParamsByProductDetailId (Long productDetailId);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.productContract.ProductContractAdditionalParamsResponse (
                 prAdPar.id,
                 prAdPar.orderingId,
                 prAdPar.label,
                 prAdPar.value
                 )
                 from ProductAdditionalParams as prAdPar
                 where prAdPar.productDetailId = :productDetailId
                 and prAdPar.label is not null""")
    List<ProductContractAdditionalParamsResponse> findProductFilledAdditionalParamsByProductDetailId(@Param("productDetailId") Long productDetailId);
}
