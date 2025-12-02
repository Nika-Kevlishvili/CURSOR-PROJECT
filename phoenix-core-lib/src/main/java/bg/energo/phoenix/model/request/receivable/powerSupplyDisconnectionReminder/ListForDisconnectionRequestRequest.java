package bg.energo.phoenix.model.request.receivable.powerSupplyDisconnectionReminder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ListForDisconnectionRequestRequest {

    @Size(min = 1, max = 512, message = "prompt-Prompt does not match the allowed length")
    private String prompt;

    @NotNull(message = "page-page should not be null")
    private Integer page;

    @NotNull(message = "size-size should not be null")
    private Integer size;

}
