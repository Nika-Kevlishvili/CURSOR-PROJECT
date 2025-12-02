package bg.energo.phoenix.model.documentModels.contract.response;

public interface InterimAdvancePaymentDetailResponse {
    String getType();

    String getValue();

    String getCurrency();

    String getDateIssueType();

    String getDateIssueValue();

    String getPaymentTermType();

    String getContractTermValue();

    String getMatchesWithInvoiceYN();
}
