package bg.energo.phoenix.model.response.AdvancedPaymentGroup;

import java.time.LocalDateTime;

/**
 * <h1>AdvancedPaymentGroupListResponse</h1>
 * {@link #getId()} Interim Advanced Payment group Id
 * {@link #getName()} ()} Interim Advanced Payment group Name
 * {@link #getNumberOfAdvancedPayments()} attached Interim Advanced Payments count
 * {@link #getDateOfCreation()} Interim Advanced Payment group Id
 * {@link #getStatus()} Interim Advanced Payment group Id
 */
public interface AdvancedPaymentGroupListResponse {
    Long getId();

    String getName();

    Long getNumberOfAdvancedPayments();

    LocalDateTime getDateOfCreation();

    String getStatus();
}
