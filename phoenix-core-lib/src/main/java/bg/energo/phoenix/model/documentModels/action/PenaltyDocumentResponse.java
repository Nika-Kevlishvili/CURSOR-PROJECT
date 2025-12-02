package bg.energo.phoenix.model.documentModels.action;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface PenaltyDocumentResponse {
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

    LocalDate getActionExecutionDate();

    LocalDate getActionNoticeDate();

    String getTerminationClauseNumber();

    String getPenaltyClauseNumber();

    BigDecimal getPenaltyClaimAmount();

    String getPenaltyClaimCurrency();

    String getPenaltyPayer();

    String getTerminationNoticePeriod();

    String getTerminationNoticePeriodType();

    LocalDate getCalculatedTerminationDate();

    LocalDate getCalculatedTerminationDatePlus1();

    Long getLastServiceContractDetailId();

    Long getLastProductContractDetailId();

    Long getActionId();

    Long getCustomerDetailId();

    Long getPcDetailId();

    Long getPenaltyId();

    LocalDateTime getCreateDate();

    LocalDate getPenaltyPaymentDueDate();

    String getEmails();

    Long getCustomerCommunicationId();
}
