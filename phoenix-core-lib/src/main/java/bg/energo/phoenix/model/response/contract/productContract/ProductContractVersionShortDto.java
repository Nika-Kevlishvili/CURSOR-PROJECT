package bg.energo.phoenix.model.response.contract.productContract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductContractVersionShortDto {
    private Long contractId;
    private Long detailsId;
    private Integer versionId;
    private LocalDate startDate;
}
