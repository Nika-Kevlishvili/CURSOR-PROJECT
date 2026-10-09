package bg.energo.phoenix.systech.liabilities.repository;

import java.math.BigDecimal;

public interface SystechObligationsProjection {

    String getCustomerNumber();

    String getCustomerName();

    String getCustomerAddress();

    String getDocumentNumber();

    String getDocumentDate();

    String getDocumentInfo();

    boolean isAllowedForPayment();

    boolean isAllowedPartialPayment();

    String getCurrency();

    BigDecimal getPrincipal();

    BigDecimal getInterest();

    BigDecimal getAnother();
}

