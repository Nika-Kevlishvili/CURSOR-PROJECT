package bg.energo.phoenix.model.response.contract.serviceContract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractVersions {
    private Long id;
    private Long serviceId;
    private Long versionId;
    private LocalDate startDate;
    private LocalDate endDate;
}
