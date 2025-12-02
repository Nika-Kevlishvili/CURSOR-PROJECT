package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceVersion {

    private Long id; // version id

    private Long detailId;

    private ServiceDetailStatus status;

    private LocalDateTime createDate;

    public ServiceVersion(Long id, Long detailId, ServiceDetailStatus status, LocalDateTime createDate) {
        this.id = id;
        this.detailId = detailId;
        this.status = status;
        this.createDate = createDate;
    }
}
