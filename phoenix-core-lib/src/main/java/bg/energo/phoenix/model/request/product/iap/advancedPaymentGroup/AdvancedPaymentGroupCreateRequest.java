package bg.energo.phoenix.model.request.product.iap.advancedPaymentGroup;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <h1>AdvancedPaymentGroupCreateRequest</h1>
 * {@link #name} interim advanced payment group name
 * {@link #advancedPayments} interim advanced payment ids list
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdvancedPaymentGroupCreateRequest {

    @NotNull(message = "name-Name should not be null;")
    @Size(min = 1, max = 1024, message = "name-Name does not match the allowed length;")
    private String name;
    private List<Long> advancedPayments;



}
