package bg.energo.phoenix.model.response.pod.pod;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PointOfDeliveryView {

    private Long podId;
    private Long detailId;
}
