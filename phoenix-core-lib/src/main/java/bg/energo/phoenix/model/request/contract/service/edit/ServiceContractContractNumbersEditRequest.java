package bg.energo.phoenix.model.request.contract.service.edit;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractContractNumbersEditRequest {

    @NotBlank(message = "contractNumbersEditList.contractNumber-[contractNumber] can't be empty;")
    private String contractNumber;

}
