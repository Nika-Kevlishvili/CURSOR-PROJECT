package bg.energo.phoenix.repository.product.service.subObject;

import bg.energo.phoenix.model.entity.product.service.ServiceTerminationGroup;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceTerminationGroupRepository extends JpaRepository<ServiceTerminationGroup, Long> {

    List<ServiceTerminationGroup> findByServiceDetailsIdAndStatusIn(Long serviceDetailsId, List<ServiceSubobjectStatus> status);

}
