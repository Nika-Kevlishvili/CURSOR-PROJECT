package bg.energo.phoenix.model.response.contract.serviceContract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubObjectPodsResponse {

    private Long id;
    private Long podId;
    private String podName;
    private String podIdentifier;

}
