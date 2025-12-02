package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ProductContractResignedContracts;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractResignResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductContractResignedContractsRepository extends JpaRepository<ProductContractResignedContracts, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.contract.productContract.ProductContractResignResponse(
                pcrc.resignedContractId,
                (select innerPC.contractNumber from ProductContract innerPC where innerPC.id = pcrc.resignedContractId)
            )
            from ProductContract pc
            join ProductContractResignedContracts pcrc on pcrc.contractId = pc.id
            where pcrc.contractId = :contractId
            """)
    List<ProductContractResignResponse> findResignedContractsFrom(Long contractId);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.productContract.ProductContractResignResponse(pc.id, pc.contractNumber)
            from ProductContract pc
            where pc.id = :contractId
            """)
    List<ProductContractResignResponse> findResignedContractsTo(Long contractId);
}
