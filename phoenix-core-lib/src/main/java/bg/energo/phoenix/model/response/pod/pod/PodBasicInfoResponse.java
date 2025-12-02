package bg.energo.phoenix.model.response.pod.pod;

import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PodBasicInfoResponse {
    private Long id;
    private String identifier;
    private String name;
    private PodStatus status;
    private LocalDateTime dateOfCreation;
}
