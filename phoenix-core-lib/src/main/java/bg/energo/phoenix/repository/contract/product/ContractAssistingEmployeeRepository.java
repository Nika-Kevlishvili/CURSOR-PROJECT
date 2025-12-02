package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ContractAssistingEmployee;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractSubObjectShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractAssistingEmployeeRepository extends JpaRepository<ContractAssistingEmployee, Long> {

    List<ContractAssistingEmployee> findByContractDetailIdAndStatusIn(Long contractDetailId, List<ContractSubObjectStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.productContract.ProductContractSubObjectShortResponse(
                        cae.accountManagerId,
                        concat(ac.displayName, ' (', ac.userName, ')')
                    )
                    from ContractAssistingEmployee cae
                    join AccountManager ac on cae.accountManagerId = ac.id
                        where cae.contractDetailId = :contractDetailId
                        and cae.status in :statuses
                    """
    )
    List<ProductContractSubObjectShortResponse> getShortResponseByContractDetailIdAndStatusIn(
            @Param("contractDetailId") Long contractDetailId,
            @Param("statuses") List<ContractSubObjectStatus> statuses
    );

}
