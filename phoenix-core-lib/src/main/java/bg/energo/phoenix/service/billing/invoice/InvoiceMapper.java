package bg.energo.phoenix.service.billing.invoice;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceCommunicationResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceLiabilitiesReceivableResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceResponse;
import bg.energo.phoenix.model.response.customer.CustomerVersionShortResponse;
import bg.energo.phoenix.model.response.nomenclature.bank.BankShortResponse;
import bg.energo.phoenix.model.response.proxy.ProxyFileResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
public class InvoiceMapper {
    public InvoiceResponse mapFromEntityToResponse(Invoice invoice,
                                                   InterestRate interestRate,
                                                   Bank bank,
                                                   CustomerVersionShortResponse customer,
                                                   CustomerVersionShortResponse alternativeCustomer,
                                                   GoodsOrder goodsOrder,
                                                   ServiceOrder serviceOrder,
                                                   ContractBillingGroup billingGroup,
                                                   ProductContract productContract,
                                                   ServiceContracts serviceContract,
                                                   List<InvoiceVatRateResponse> invoiceVatRates,
                                                   BillingRun billingRun,
                                                   Currency currency,
                                                   InvoiceCommunicationResponse customerCommunications,
                                                   ProductDetails productDetails,
                                                   ServiceDetails serviceDetails,
                                                   List<Document> invoiceDocument,
                                                   List<InvoiceLiabilitiesReceivableResponse> liabilities,
                                                   List<ShortResponse> debitCredits,
                                                   ContractTemplateShortResponse template,
                                                   ProxyFileResponse cancelDocumentResponse,
                                                   List<ShortResponse> compensations,
                                                   ShortResponse issuer,
                                                   boolean detailedData) {
        String invoiceNumber = StringUtils.defaultIfBlank(invoice.getInvoiceNumber(), "");
        String[] splitInvoiceNumber = invoiceNumber.split("-");

        String prefix = "";
        String extractedInvoiceNumber;
        if (splitInvoiceNumber.length == 2) {
            prefix = splitInvoiceNumber[0];
            extractedInvoiceNumber = splitInvoiceNumber[1];
        } else {
            extractedInvoiceNumber = invoiceNumber;
        }

        return new InvoiceResponse(
                invoice.getId(),
                prefix,
                extractedInvoiceNumber,
                invoice.getInvoiceDate(),
                Objects.equals(invoice.getInvoiceStatus(), InvoiceStatus.DRAFT_GENERATED) ? InvoiceStatus.DRAFT : invoice.getInvoiceStatus(),
                invoice.getInvoiceType(),
                invoice.getInvoiceDocumentType(),
                invoice.getTaxEventDate(),
                invoice.getPaymentDeadline(),
                invoice.getMeterReadingPeriodFrom(),
                invoice.getMeterReadingPeriodTo(),
                invoice.getBasisForIssuing(),
                invoice.getIncomeAccountNumber(),
                invoice.getCostCenterControllingOrder(),
                interestRate == null ? null : new ShortResponse(interestRate.getId(), interestRate.getName()),
                invoice.getDirectDebit(),
                bank == null ? null : new BankShortResponse(bank.getId(), bank.getName(), bank.getBic()),
                invoice.getIban(),
                customer,
                alternativeCustomer,
                goodsOrder == null ? null : new ShortResponse(goodsOrder.getId(), "%s/%s".formatted(goodsOrder.getOrderNumber(), goodsOrder.getCreateDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))),
                serviceOrder == null ? null : new ShortResponse(serviceOrder.getId(), "%s/%s".formatted(serviceOrder.getOrderNumber(), serviceOrder.getCreateDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))),
                billingGroup == null ? null : new ShortResponse(billingGroup.getId(), billingGroup.getGroupNumber()),
                productContract == null ? null : new ShortResponse(productContract.getId(), "%s/%s".formatted(productContract.getContractNumber(), productContract.getCreateDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))),
                productDetails == null ? null : new ShortResponse(productDetails.getProduct().getId(), productDetails.getName()),
                serviceContract == null ? null : new ShortResponse(serviceContract.getId(), "%s/%s".formatted(serviceContract.getContractNumber(), serviceContract.getCreateDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))),
                serviceDetails == null ? null : new ShortResponse(serviceDetails.getService().getId(), serviceDetails.getName()),
                invoiceVatRates,
                EPBDecimalUtils.convertToCurrencyScale(Objects.requireNonNullElse(invoice.getTotalAmountExcludingVat(), BigDecimal.ZERO)).toPlainString(),
                EPBDecimalUtils.convertToCurrencyScale(Objects.requireNonNullElse(invoice.getTotalAmountOfVat(), BigDecimal.ZERO)).toPlainString(),
                EPBDecimalUtils.convertToCurrencyScale(Objects.requireNonNullElse(invoice.getTotalAmountIncludingVat(), BigDecimal.ZERO)).toPlainString(),
                EPBDecimalUtils.convertToCurrencyScale(Objects.requireNonNullElse(invoice.getTotalAmountIncludingVatInOtherCurrency(), BigDecimal.ZERO)).toPlainString(),
                billingRun == null ? null : new ShortResponse(billingRun.getId(), billingRun.getBillingNumber()),
                currency == null ? null : new ShortResponse(currency.getId(), currency.getName()),
                customerCommunications,
                invoiceDocument.stream().map(x -> new ShortResponse(x.getId(), x.getName())).toList(),
                liabilities,
                debitCredits,
                template,
                cancelDocumentResponse,
                compensations,
                invoice.getCurrentStatusChangeDate(),
                issuer,
                Objects.nonNull(invoice.getCompensationIndex()),
                detailedData
        );
    }
}
