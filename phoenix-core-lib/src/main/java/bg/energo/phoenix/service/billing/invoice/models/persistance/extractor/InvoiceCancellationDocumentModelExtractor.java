package bg.energo.phoenix.service.billing.invoice.models.persistance.extractor;

import java.time.LocalDate;

public interface InvoiceCancellationDocumentModelExtractor {
    String getCustomerNameComb();

    String getCustomerNameCombTrsl();

    String getCustomerIdentifer();

    String getCustomerVat();

    String getCustomerNumber();

    String getCustomerAddressComb();

    String getCustomerPopulatedPlace();

    String getCustomerZip();

    String getCustomerDistrict();

    String getCustomerQuarterRaType();

    String getCustomerQuarterRaName();

    String getCustomerStrBlvdType();

    String getCustomerStrBlvdName();

    String getCustomerStrBlvdNumber();

    String getCustomerBlock();

    String getCustomerEntrance();

    String getCustomerFloor();

    String getCustomerApartment();

    String getCustomerAdditionalInfo();

    String getCustomerSegments();

    String getManagers();

    String getDocumentNumber();

    String getDocumentPrefix();

    LocalDate getDocumentDate();

    String getCanceledDocumentType();

    String getCanceledDocumentNumber();

    String getCanceledDocumentPrefix();
}
