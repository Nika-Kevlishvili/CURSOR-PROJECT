package bg.energo.phoenix.model.response.billing.compensations;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.compensation.CompensationStatus;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompensationListingResponse(
        Long id,
        String number,
        LocalDate date,
        String period,
        String documentVolumes,
        String compensationReason,
        String price,
        String amount,
        String currency,
        String customer,
        String pod,
        String recipient,
        CompensationStatus compensationStatus,
        EntityStatus status,
        String index,
        LocalDate invoiceUsageDate
) {
    public CompensationListingResponse(CompensationListingProjection projector) {
        this(
                projector.getId(),
                projector.getNumber(),
                projector.getDate(),
                projector.getPeriod(),
                ObjectUtils.defaultIfNull(projector.getDocumentVolumes(), BigDecimal.ZERO).toPlainString(),
                projector.getCompensationReason(),
                ObjectUtils.defaultIfNull(projector.getPrice(), BigDecimal.ZERO).toPlainString(),
                ObjectUtils.defaultIfNull(projector.getAmount(), BigDecimal.ZERO).toPlainString(),
                projector.getCurrency(),
                projector.getCustomer(),
                projector.getPod(),
                projector.getRecipient(),
                projector.getCompensationStatus(),
                projector.getStatus(),
                projector.getIndex(),
                projector.getInvoiceUsageDate()
        );
    }
}
