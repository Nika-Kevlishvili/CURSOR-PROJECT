package bg.energo.phoenix.model.request.contract.action;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.contract.ContractType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@PromptSymbolReplacer
public class ActionTerminationRequest {

    private String prompt;

    private int page;

    private int size;

    @NotNull(message = "contractId-Contract ID is required;")
    private Long contractId;

    @NotNull(message = "contractType-Contract type is required;")
    private ContractType contractType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "executionDate-Execution date is required;")
    private LocalDate executionDate;

}
