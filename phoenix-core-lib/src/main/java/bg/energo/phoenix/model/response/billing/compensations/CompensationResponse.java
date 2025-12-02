package bg.energo.phoenix.model.response.billing.compensations;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.compensation.CompensationStatus;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceShortResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@Getter
public class CompensationResponse {

    private Long id;

    private String number;

    private LocalDate date;

    private String volumes;

    private String price;

    private String reason;

    private LocalDate documentPeriod;

    private String documentAmount;

    private ShortResponse documentCurrency;

    private ShortResponse customer;

    private ShortResponse pod;

    private ShortResponse recipient;

    private Integer index;

    private LocalDate invoiceUsageDate;

    private ShortResponse liabilityForRecipient;

    private ShortResponse receivableForCustomer;

    private ShortResponse liabilityForCustomer;

    private ShortResponse receivableForRecipient;

    private InvoiceShortResponse invoice;

    private EntityStatus status;

    private CompensationStatus compensationStatus;

    public CompensationResponse(Long id,
                                String number,
                                LocalDate date,
                                BigDecimal volumes,
                                BigDecimal price,
                                String reason,
                                LocalDate documentPeriod,
                                BigDecimal documentAmount,
                                Long currencyId,
                                String currencyName,
                                Long customerId,
                                String customerName,
                                Long podId,
                                String podIdentifier,
                                Long recipientId,
                                String recipientName,
                                Integer index,
                                LocalDate invoiceUsageDate,
                                Long liabilityForRecipientId,
                                String liabilityForRecipientName,
                                Long receivableForCustomerId,
                                String receivableForCustomerName,
                                Long liabilityForCustomerId,
                                String liabilityForCustomerName,
                                Long receivableForRecipientId,
                                String receivableForRecipientName,
                                Long invoiceId,
                                String invoiceNumber,
                                EntityStatus status,
                                CompensationStatus compensationStatus) {
        this.id = id;
        this.number = number;
        this.date = date;
        this.volumes = ObjectUtils.defaultIfNull(volumes, BigDecimal.ZERO).toPlainString();
        this.price = ObjectUtils.defaultIfNull(price, BigDecimal.ZERO).toPlainString();
        this.reason = reason;
        this.documentPeriod = documentPeriod;
        this.documentAmount = ObjectUtils.defaultIfNull(documentAmount, BigDecimal.ZERO).toPlainString();
        this.documentCurrency = new ShortResponse(currencyId, currencyName);
        this.customer = new ShortResponse(customerId, customerName);
        this.pod = new ShortResponse(podId, podIdentifier);
        this.recipient = new ShortResponse(recipientId, recipientName);
        this.index = index;
        this.invoiceUsageDate = invoiceUsageDate;
        this.liabilityForRecipient = new ShortResponse(liabilityForRecipientId, liabilityForRecipientName);
        this.receivableForCustomer = new ShortResponse(receivableForCustomerId, receivableForCustomerName);
        this.liabilityForCustomer = new ShortResponse(liabilityForCustomerId, liabilityForCustomerName);
        this.receivableForRecipient = new ShortResponse(receivableForRecipientId, receivableForRecipientName);
        this.invoice = new InvoiceShortResponse(invoiceId, invoiceNumber);
        this.status = status;
        this.compensationStatus = compensationStatus;
    }
}
