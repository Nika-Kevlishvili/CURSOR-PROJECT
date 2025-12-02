package bg.energo.phoenix.service.billing.model.documentmodels;

import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentCompensationDAO;

import java.time.LocalDate;

public class BillingRunDocumentDetailedDataCompensations {
    public String Reason;
    public String DocumentNumber;
    public LocalDate DocumentDate;
    public LocalDate Period;
    public String Volumes;
    public String Amount;
    public String Currency;

    public BillingRunDocumentDetailedDataCompensations(
            BillingRunDocumentCompensationDAO dao
    ) {
        {
            DocumentNumber = dao.getDocumentNumber();
            DocumentDate = dao.getDocumentDate();
            Period = dao.getPeriod();
            Volumes = dao.getVolumes() == null ? null : dao.getVolumes()
                    .toString();
            Amount = dao.getAmount() == null ? null : dao.getAmount()
                    .toString();
            Currency = dao.getCurrency();
            Reason = dao.getReason();
        }
    }

}
