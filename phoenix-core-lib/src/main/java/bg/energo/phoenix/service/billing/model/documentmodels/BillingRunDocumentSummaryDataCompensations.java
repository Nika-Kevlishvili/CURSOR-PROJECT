package bg.energo.phoenix.service.billing.model.documentmodels;

import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentCompensationDAO;

import java.math.BigDecimal;

import java.time.LocalDate;

public class BillingRunDocumentSummaryDataCompensations {
    public String DocumentNumber;
    public LocalDate DocumentDate;
    public LocalDate Period;
    public String Volumes;
    public String Amount;
    public String Currency;


    public BillingRunDocumentSummaryDataCompensations(
            DocumentCompensationKey dao,
            BigDecimal amount,
            BigDecimal volumes
    ) {
        DocumentNumber = dao.getDocumentNumber();
        DocumentDate = dao.getDocumentDate();
        Period = dao.getPeriod();
        Volumes = volumes.toString();
        Amount = amount.toString();
        Currency = dao.getCurrency();
    }
}
