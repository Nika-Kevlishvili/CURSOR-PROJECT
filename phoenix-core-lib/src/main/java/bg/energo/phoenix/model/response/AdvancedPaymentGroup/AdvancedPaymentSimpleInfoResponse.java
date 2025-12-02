package bg.energo.phoenix.model.response.AdvancedPaymentGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h1>AdvancedPaymentSimpleInfoResponse</h1>
 * {@link #id} InterimAdvancedPayment id
 * {@link #name} InterimAdvancedPayment name
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdvancedPaymentSimpleInfoResponse {
    private Long id;
    private String name;
    private String displayName;

    public AdvancedPaymentSimpleInfoResponse(Long id, String name) {
        this.id = id;
        this.name = name;
        this.displayName = "%s (%s)".formatted(this.name, this.id.toString());
    }
}
