package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ProductContractVersionTypes;
import bg.energo.phoenix.model.entity.nomenclature.contract.ContractVersionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductContractVersionTypeRepository extends JpaRepository<ProductContractVersionTypes, Long> {

    @Query("""
            select cvt from ProductContractVersionTypes pcvt
            join ContractVersionType cvt on cvt.id=pcvt.contractVersionTypeId
            where pcvt.contractDetailId = :contractDetailId
            and pcvt.status = 'ACTIVE'
            and cvt.status in ('ACTIVE','INACTIVE')
                        """)
    List<ContractVersionType> findVersionTypesForContract(Long contractDetailId);

    @Query("""
            select pcvt from ProductContractVersionTypes pcvt
            where pcvt.contractDetailId = :contractDetailId
            and pcvt.status = 'ACTIVE'
                        """)
    List<ProductContractVersionTypes> findProductContractVersionTypeForContractDetail(Long contractDetailId);
}
