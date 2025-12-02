package bg.energo.phoenix.model.response.contract.productContract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductContractVersionResponse {
    private Long id;
    private Long contractId;
    private Integer versionId;
    private LocalDate startDate;
    private LocalDate endDate;
}
