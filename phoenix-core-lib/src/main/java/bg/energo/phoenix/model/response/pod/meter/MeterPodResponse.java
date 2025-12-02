package bg.energo.phoenix.model.response.pod.meter;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MeterPodResponse {

    private Long id;
    private String podIdentifier;
    private String podName;

}
