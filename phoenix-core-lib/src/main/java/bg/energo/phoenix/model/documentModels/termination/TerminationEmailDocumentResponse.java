package bg.energo.phoenix.model.documentModels.termination;

import java.time.LocalDate;

public interface TerminationEmailDocumentResponse {
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

    String getCustomerSegments();

    String getContractNumber();

    LocalDate getContractDate();

    String getContractProductName();

    String getContractType();

    LocalDate getContractTerminationDate();

    LocalDate getContractTerminationDatePlus1();

    LocalDate getCalculatedTerminationDate();

    LocalDate getCalculatedTerminationDatePlus1();

    String getEventType();

    Long getCustomerDetailId();
}
