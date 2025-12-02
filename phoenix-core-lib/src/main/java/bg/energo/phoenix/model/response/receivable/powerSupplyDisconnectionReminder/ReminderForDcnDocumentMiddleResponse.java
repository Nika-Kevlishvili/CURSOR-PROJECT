package bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder;

public interface ReminderForDcnDocumentMiddleResponse {
    String getCustomerNameComb();
    String getCustomerNameCombTrsl();
    String getCustomerCommunicationId();
    String getCustomerId();
    String getCustomerDetailId();
    String getCustomerIdentifier();
    String getCustomerNumber();

    String getHeadquarterPopulatedPlace();
    String getHeadquarterZip();
    String getHeadquarterDistrict();
    String getHeadquarterQuarterRaType();
    String getHeadquarterQuarterRaName();
    String getHeadquarterStrBlvdType();
    String getHeadquarterStrBlvdName();
    String getHeadquarterStrBlvdNumber();
    String getHeadquarterBlock();
    String getHeadquarterEntrance();
    String getHeadquarterFloor();
    String getHeadquarterApartment();
    String getHeadquarterAdditionalInfo();
    String getHeadquarterAddressComb();

    String getCommunicationZip();
    String getCommunicationDistrict();
    String getCommunicationQuarterRaType();
    String getCommunicationQuarterRaName();
    String getCommunicationStrBlvdType();
    String getCommunicationStrBlvdName();
    String getCommunicationStrBlvdNumber();
    String getCommunicationBlock();
    String getCommunicationEntrance();
    String getCommunicationFloor();
    String getCommunicationApartment();
    String getCommunicationAdditionalInfo();
    String getCommunicationPopulatedPlace();
    String getCommunicationAddressComb();
    String getCustomerSegments();
}
