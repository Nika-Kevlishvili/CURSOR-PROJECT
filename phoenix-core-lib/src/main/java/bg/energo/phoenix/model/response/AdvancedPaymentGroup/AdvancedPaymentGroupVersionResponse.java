package bg.energo.phoenix.model.response.AdvancedPaymentGroup;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <h1>AdvancedPaymentGroupVersionResponse</h1>
 * {@link #id}  AdvancedPaymentGroupVersion id
 * {@link #name} AdvancedPaymentGroupVersion name
 * {@link #creationDate} AdvancedPaymentGroupVersion create date
 */
@Data
@Builder
public class AdvancedPaymentGroupVersionResponse {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime creationDate;
}
