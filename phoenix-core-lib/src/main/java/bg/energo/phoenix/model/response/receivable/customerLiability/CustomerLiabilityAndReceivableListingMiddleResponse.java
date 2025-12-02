package bg.energo.phoenix.model.response.receivable.customerLiability;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface CustomerLiabilityAndReceivableListingMiddleResponse {
    String getId();
    Long getIdRaw();
    String getOutgoingDocument();
    String getCreateDate();
    LocalDateTime getCreateDateRaw();
    String getDueDate();
    LocalDate getDueDateRaw();
    String getBillingGroup();
    String getContractOrder();
    String getContractOrderType();
    Long getContractOrderId();
    BigDecimal getInitialAmount();
    BigDecimal getCurrentAmount();
    String getPods();
    String getAddress();
    String getOffseting();
    String getObject();
    String getOutgoingDocumentType();
    String getOutgoingDocumentId();
}
