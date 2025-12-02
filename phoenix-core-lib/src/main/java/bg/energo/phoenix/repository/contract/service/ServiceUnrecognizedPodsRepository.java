package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.contract.service.ServiceUnrecognizedPods;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceUnrecognizedPodsRepository extends JpaRepository<ServiceUnrecognizedPods, Long> {

    Optional<ServiceUnrecognizedPods> findByIdAndStatusAndContractDetailsId(Long id,ServiceSubobjectStatus status,Long contractDetailsId);

    List<ServiceUnrecognizedPods> findByContractDetailsId(Long id);
    List<ServiceUnrecognizedPods> findByContractDetailsIdAndStatus(Long id, ServiceSubobjectStatus status);

    List<ServiceUnrecognizedPods> findByContractDetailsIdAndStatusAndIdNotIn(Long id,ServiceSubobjectStatus status,List<Long> ids);
}
