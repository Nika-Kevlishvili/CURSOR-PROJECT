package bg.energo.phoenix.repository.product.service.subObject;

import bg.energo.phoenix.model.entity.product.service.ServicePenaltyGroup;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicePenaltyGroupRepository extends JpaRepository<ServicePenaltyGroup, Long> {

    List<ServicePenaltyGroup> findByServiceDetailsIdAndStatusIn(Long serviceDetailsId, List<ServiceSubobjectStatus> statuses);

}
