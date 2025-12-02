package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractInterimPriceFormula;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractPriceComponentsResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceContractInterimPriceFormulaRepository extends JpaRepository<ServiceContractInterimPriceFormula, Long> {

    List<ServiceContractInterimPriceFormula> findAllByContractInterimAdvancePaymentIdInAndStatusIn(List<Long> contractIapId, List<EntityStatus> statuses);

    @Query("""
            select cf from ServiceContractInterimPriceFormula cf
            where cf.contractInterimAdvancePaymentId = :contractIapId
            and cf.status in :statuses
                    """)
    List<ServiceContractInterimPriceFormula> findByContractIapIdAndStatus(@Param("contractIapId") Long contractIapId, @Param("statuses") List<EntityStatus> statuses);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractPriceComponentsResponse(cf.formulaId,pcfv.description,cf.value)
            from ServiceContractInterimPriceFormula cf
            join PriceComponentFormulaVariable pcfv on pcfv.id= cf.formulaId
            where cf.contractInterimAdvancePaymentId = :contractIapId
            and cf.status in :statuses
                    """)
    List<ServiceContractPriceComponentsResponse> getPriceComponentFormulaByContractIapIdAndStatus(@Param("contractIapId") Long contractIapId, @Param("statuses") List<EntityStatus> statuses);

}
