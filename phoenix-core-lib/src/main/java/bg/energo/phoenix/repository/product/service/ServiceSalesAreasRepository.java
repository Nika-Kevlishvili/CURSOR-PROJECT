package bg.energo.phoenix.repository.product.service;

import bg.energo.phoenix.model.entity.product.service.ServiceSalesArea;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceSalesAreasRepository extends JpaRepository<ServiceSalesArea, Long> {

    List<ServiceSalesArea> findByServiceDetailsIdAndStatusIn(Long serviceDetailsId, List<ServiceSubobjectStatus> statuses);

}
