package bg.energo.phoenix.model.response.pod.pod;

import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointOfDeliveryVersionResponse {

    private Integer version;
    private String name;
    private LocalDateTime createDate;

    public PointOfDeliveryVersionResponse(PointOfDeliveryDetails details) {
        this.version = details.getVersionId();
        this.name = details.getName();
        this.createDate = details.getCreateDate();
    }
}
