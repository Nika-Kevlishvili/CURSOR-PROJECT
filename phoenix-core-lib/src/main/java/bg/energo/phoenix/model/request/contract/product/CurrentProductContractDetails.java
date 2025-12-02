package bg.energo.phoenix.model.request.contract.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CurrentProductContractDetails {
    private Long contractDetailsId;
    private LocalDate startDate;
    private Long contractId;
    private int versionId;
}
