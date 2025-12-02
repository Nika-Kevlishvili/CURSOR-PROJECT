package bg.energo.phoenix.model.response.pod.pod;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PodResponse {

    private Long id;
    private Long podDetailId;
    private Integer versionId;
}
