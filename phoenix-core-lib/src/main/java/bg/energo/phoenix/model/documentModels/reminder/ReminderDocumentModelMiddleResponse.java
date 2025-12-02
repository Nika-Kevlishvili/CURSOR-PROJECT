package bg.energo.phoenix.model.documentModels.reminder;

import java.util.List;


public interface ReminderDocumentModelMiddleResponse {

    String getCurrency();

    String getCustomerNameComb();

    String getCustomerNameCombTrsl();

    String getCustomerIdentifier();

    String getCustomerNumber();

    String getHeadquarterAddressComb();

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

    String getCommunicationAddressComb();

    String getCommunicationPopulatedPlace();

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

    List<String> getCustomerSegments();

}