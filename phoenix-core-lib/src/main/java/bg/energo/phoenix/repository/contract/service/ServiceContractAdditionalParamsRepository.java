package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.contract.service.ServiceContractAdditionalParams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceContractAdditionalParamsRepository extends JpaRepository<ServiceContractAdditionalParams, Long> {
    void deleteAllByContractDetailId(Long serviceContractDetailId);
    Optional<ServiceContractAdditionalParams> findByContractDetailIdAndServiceAdditionalParamId(Long contractDetailId, Long serviceAdditionalParamId);
    List<ServiceContractAdditionalParams> findAllByContractDetailId(Long contractDetailId);
}
