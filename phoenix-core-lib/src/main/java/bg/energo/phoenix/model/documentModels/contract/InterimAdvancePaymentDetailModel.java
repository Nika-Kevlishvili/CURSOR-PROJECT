package bg.energo.phoenix.model.documentModels.contract;

import bg.energo.phoenix.model.documentModels.contract.response.InterimAdvancePaymentDetailResponse;

public class InterimAdvancePaymentDetailModel {
    public String Type;
    public String TypeTrsl;
    public String Value;
    public String Currency;
    public String DateIssueType;
    public String DateIssueTypeTrsl;
    public String DateIssueValue;
    public String PaymentTermType;
    public String PaymentTermTypeTrsl;
    public String ContractTermValue;
    public String MatchesWithInvoiceYN;

    public InterimAdvancePaymentDetailModel from(InterimAdvancePaymentDetailResponse response) {
        this.Type = response.getType();
        this.TypeTrsl = response.getType();//todo
        this.Value = response.getValue();
        this.Currency = response.getCurrency();
        this.DateIssueType = response.getDateIssueType();
        this.DateIssueTypeTrsl = response.getDateIssueType();//todo
        this.DateIssueValue = response.getDateIssueValue();
        this.PaymentTermType = response.getPaymentTermType();
        this.PaymentTermTypeTrsl = response.getPaymentTermType();//todo
        this.ContractTermValue = response.getContractTermValue();
        this.MatchesWithInvoiceYN = response.getMatchesWithInvoiceYN();
        return this;
    }
}
