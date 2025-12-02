package bg.energo.phoenix.model.request.customer;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerActiveContractRequest {
    private Long customersDetailsId;
    @NotNull(message = "page-Page must not be null;")
    private Integer page;
    @NotNull(message = "size-Size must not be null;")
    private Integer size;
}
