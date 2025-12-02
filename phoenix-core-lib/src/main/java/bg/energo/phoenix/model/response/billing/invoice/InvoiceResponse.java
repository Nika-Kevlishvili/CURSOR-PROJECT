package bg.energo.phoenix.model.response.billing.invoice;

import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.response.customer.CustomerVersionShortResponse;
import bg.energo.phoenix.model.response.nomenclature.bank.BankShortResponse;
import bg.energo.phoenix.model.response.proxy.ProxyFileResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String prefix,
        String invoiceNumber,
        LocalDate invoiceDate,
        InvoiceStatus invoiceStatus,
        InvoiceType invoiceType,
        InvoiceDocumentType invoiceDocumentType,
        LocalDate taxEventDate,
        LocalDate paymentDeadLine,
        LocalDate meterReadingFrom,
        LocalDate meterReadingTo,
        String basisForIssuing,
        String incomeAccountNumber,
        String costCenterControllingOrder,
        ShortResponse interestRate,
        Boolean directDebit,
        BankShortResponse bank,
        String iban,
        CustomerVersionShortResponse customer,
        CustomerVersionShortResponse receiptOfInvoice,
        ShortResponse goodsOrder,
        ShortResponse serviceOrder,
        ShortResponse contractBillingGroup,
        ShortResponse productContract,
        ShortResponse product,
        ShortResponse serviceContract,
        ShortResponse service,
        List<InvoiceVatRateResponse> invoiceVatRateResponses,
        String totalAmountExcludingVat,
        String totalAmountOfVat,
        String totalAmountIncludingVat,
        String totalAmountIncludingVatInOtherCurrency,
        ShortResponse billingRun,
        ShortResponse currency,
        InvoiceCommunicationResponse customerCommunications,
        List<ShortResponse> file,
        List<InvoiceLiabilitiesReceivableResponse> liabilitiesAndReceivables,
        List<ShortResponse> debitCredits,
        ContractTemplateShortResponse template,
        ProxyFileResponse cancelDocumentResponse,
        List<ShortResponse> compensations,
        LocalDateTime currentStatusChangeDate,
        ShortResponse issuer,
        boolean isCompensationGenerated,
        boolean detailedData) {
}
