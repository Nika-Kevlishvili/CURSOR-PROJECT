package bg.energo.phoenix.model.documentModels.latePaymentFine;

import java.util.List;

public interface LatePaymentFineCustomerInfoResponse {

    String getCustomerNameComb();

    String getCustomerNameCombTrsl();

    String getCustomerIdentifier();

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

    List<String> getCustomerSegments();
}
