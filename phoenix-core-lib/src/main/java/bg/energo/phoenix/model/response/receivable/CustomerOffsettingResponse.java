package bg.energo.phoenix.model.response.receivable;

import bg.energo.phoenix.model.ObjectOffsettingDetail;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByDeposit;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByPayment;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByReceivable;
import bg.energo.phoenix.model.entity.receivable.payment.PaymentLiabilityOffsetting;
import bg.energo.phoenix.model.entity.receivable.payment.PaymentReceivableOffsetting;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingDisplayColor;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingType;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
public class CustomerOffsettingResponse {

    Long id;
    LocalDate date;
    BigDecimal amount;
    OffsettingObject offsettingObject;
    ObjectOffsettingType offsettingObjectType;
    CurrencyShortResponse currencyResponse;
    String description;
    ObjectOffsettingDisplayColor displayColor;

    public static CustomerOffsettingResponse from(PaymentReceivableOffsetting paymentReceivableOffsetting, Currency currency, OffsettingObject offsettingObject) {
        CustomerOffsettingResponse customerOffsettingResponse = new CustomerOffsettingResponse();
        customerOffsettingResponse.id = offsettingObject.equals(OffsettingObject.RECEIVABLE) ? paymentReceivableOffsetting.getCustomerReceivableId() : paymentReceivableOffsetting.getCustomerPaymentId();
        customerOffsettingResponse.date = LocalDate.from(paymentReceivableOffsetting.getCreateDate());
        customerOffsettingResponse.amount = paymentReceivableOffsetting.getAmount();
        customerOffsettingResponse.offsettingObject = offsettingObject;
        customerOffsettingResponse.currencyResponse = new CurrencyShortResponse(currency);

        return customerOffsettingResponse;
    }

    public static CustomerOffsettingResponse from(PaymentLiabilityOffsetting paymentLiabilityOffsetting, Currency currency, OffsettingObject offsettingObject) {
        CustomerOffsettingResponse customerOffsettingResponse = new CustomerOffsettingResponse();
        customerOffsettingResponse.id = offsettingObject.equals(OffsettingObject.LIABILITY) ? paymentLiabilityOffsetting.getCustomerLiabilityId() : paymentLiabilityOffsetting.getCustomerPaymentId();
        customerOffsettingResponse.date = LocalDate.from(paymentLiabilityOffsetting.getCreateDate());
        customerOffsettingResponse.amount = paymentLiabilityOffsetting.getAmount();
        customerOffsettingResponse.offsettingObject = offsettingObject;
        customerOffsettingResponse.currencyResponse = new CurrencyShortResponse(currency);

        return customerOffsettingResponse;
    }

    public static CustomerOffsettingResponse from(CustomerLiabilityPaidByReceivable customerLiabilityPaidByReceivable, Currency currency, OffsettingObject offsettingObject) {
        CustomerOffsettingResponse customerOffsettingResponse = new CustomerOffsettingResponse();
        customerOffsettingResponse.id = offsettingObject.equals(OffsettingObject.LIABILITY) ? customerLiabilityPaidByReceivable.getCustomerLiabilityId() : customerLiabilityPaidByReceivable.getCustomerReceivableId();
        customerOffsettingResponse.date = LocalDate.from(customerLiabilityPaidByReceivable.getCreateDate());
        customerOffsettingResponse.amount = customerLiabilityPaidByReceivable.getAmount();
        customerOffsettingResponse.offsettingObject = offsettingObject;
        customerOffsettingResponse.currencyResponse = new CurrencyShortResponse(currency);
        return customerOffsettingResponse;
    }

    public static CustomerOffsettingResponse from(CustomerLiabilityPaidByDeposit customerLiabilityPaidByDeposit, Currency currency, OffsettingObject offsettingObject) {
        CustomerOffsettingResponse customerOffsettingResponse = new CustomerOffsettingResponse();
        customerOffsettingResponse.id = offsettingObject.equals(OffsettingObject.LIABILITY) ? customerLiabilityPaidByDeposit.getCustomerLiabilityId() : customerLiabilityPaidByDeposit.getCustomerDepositId();
        customerOffsettingResponse.date = LocalDate.from(customerLiabilityPaidByDeposit.getCreateDate());
        customerOffsettingResponse.amount = customerLiabilityPaidByDeposit.getAmount();
        customerOffsettingResponse.offsettingObject = offsettingObject;
        customerOffsettingResponse.currencyResponse = new CurrencyShortResponse(currency);

        return customerOffsettingResponse;
    }

    public static CustomerOffsettingResponse from(CustomerLiabilityPaidByPayment customerLiabilityPaidByPayment, Currency currency, OffsettingObject offsettingObject) {
        CustomerOffsettingResponse customerOffsettingResponse = new CustomerOffsettingResponse();
        customerOffsettingResponse.id = offsettingObject.equals(OffsettingObject.LIABILITY) ? customerLiabilityPaidByPayment.getCustomerLiabilityId() : customerLiabilityPaidByPayment.getCustomerPaymentId();
        customerOffsettingResponse.date = LocalDate.from(customerLiabilityPaidByPayment.getCreateDate());
        customerOffsettingResponse.amount = customerLiabilityPaidByPayment.getAmount();
        customerOffsettingResponse.offsettingObject = offsettingObject;
        customerOffsettingResponse.currencyResponse = new CurrencyShortResponse(currency);

        return customerOffsettingResponse;
    }

    public static CustomerOffsettingResponse from(ObjectOffsettingDetail detail) {
        CustomerOffsettingResponse customerOffsettingResponse = new CustomerOffsettingResponse();
        customerOffsettingResponse.id = detail.getObjectId();
        customerOffsettingResponse.date = detail.getOperationDate();
        BigDecimal amount = detail.getOffsettingAmount();
        if (amount != null) {
            amount = detail.getOffsettingAmount().setScale(2, RoundingMode.HALF_UP);
        }
        customerOffsettingResponse.amount = amount;
        customerOffsettingResponse.offsettingObjectType = detail.getObjectType();

        String formattedDate = "";
        if (detail.getOperationDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            formattedDate = detail.getOperationDate().format(formatter);
        }
        customerOffsettingResponse.description = String.format(
                "%s %s %s-%d %s%s",
                amount == null ? "" : amount,
                detail.getCurrencyName(),
                detail.getObjectType().getValue(),
                detail.getObjectId(),
                formattedDate,
                detail.getStatus() == EntityStatus.REVERSED ? " (Reversed)" : ""
        );
        customerOffsettingResponse.displayColor = detail.getDisplayColor();
        return customerOffsettingResponse;
    }
}
