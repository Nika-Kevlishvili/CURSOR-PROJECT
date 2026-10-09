package bg.energo.phoenix.bulgariapost.liabilities.repository;

import java.math.BigDecimal;

public interface BulgariaPostObligationsProjection {

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

