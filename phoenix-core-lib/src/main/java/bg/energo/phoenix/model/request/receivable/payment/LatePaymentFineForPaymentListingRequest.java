package bg.energo.phoenix.model.request.receivable.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatePaymentFineForPaymentListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    @NotNull(message = "customerId-Customer id must not be null;")
    private Long customerId;

    private String prompt;
}
