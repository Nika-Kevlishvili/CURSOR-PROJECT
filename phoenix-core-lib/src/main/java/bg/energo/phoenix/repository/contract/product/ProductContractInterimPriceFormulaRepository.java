package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractInterimPriceFormula;
import bg.energo.phoenix.model.response.contract.productContract.ContractPriceComponentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductContractInterimPriceFormulaRepository extends JpaRepository<ProductContractInterimPriceFormula, Long> {

    List<ProductContractInterimPriceFormula> findAllByContractInterimAdvancePaymentIdInAndStatusIn(List<Long> contractIapId, List<EntityStatus> statuses);
    @Query("""
select cf from ProductContractInterimPriceFormula cf 
where cf.contractInterimAdvancePaymentId = :contractIapId
and cf.status in :statuses
        """)
    List<ProductContractInterimPriceFormula> findByContractIapIdAndStatus(@Param("contractIapId")Long contractIapId,
                                                                          @Param("statuses")List<EntityStatus> statuses);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.productContract.ContractPriceComponentResponse(cf.formulaId,pcfv.description,cf.value) from ProductContractInterimPriceFormula cf
            join PriceComponentFormulaVariable pcfv on pcfv.id= cf.formulaId
            where cf.contractInterimAdvancePaymentId = :contractIapId
            and cf.status in :statuses
                    """)
    List<ContractPriceComponentResponse> getPriceComponentFormulaByContractIapIdAndStatus(@Param("contractIapId") Long contractIapId,
                                                                                          @Param("statuses") List<EntityStatus> statuses);
}
