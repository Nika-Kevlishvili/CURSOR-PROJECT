package bg.energo.phoenix.repository.product.service;

import bg.energo.phoenix.model.entity.product.service.ServiceSalesChannel;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceSalesChannelsRepository extends JpaRepository<ServiceSalesChannel, Long> {

    List<ServiceSalesChannel> findByServiceDetailsIdAndStatusIn(Long serviceDetailsId, List<ServiceSubobjectStatus> statuses);

}
