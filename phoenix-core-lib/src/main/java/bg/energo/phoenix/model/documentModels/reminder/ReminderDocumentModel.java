package bg.energo.phoenix.model.documentModels.reminder;

import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ReminderDocumentModel extends CompanyDetailedInformationModelImpl {

    @JsonProperty("ReminderNumber")
    public String ReminderNumber;

    @JsonProperty("CommunicationNumber")
    public String CommunicationNumber;

    @JsonProperty("DisconnectionDate")
    public LocalDate DisconnectionDate;

    @JsonProperty("LiabilitiesTotalAmount")
    public String LiabilitiesTotalAmount;

    @JsonProperty("Currency")
    public String Currency;

    @JsonProperty("CustomerNameComb")
    public String CustomerNameComb;

    @JsonProperty("CustomerNameCombTrsl")
    public String CustomerNameCombTrsl;

    @JsonProperty("CustomerIdentifier")
    public String CustomerIdentifier;

    @JsonProperty("CustomerNumber")
    public String CustomerNumber;

    @JsonProperty("HeadquarterAddressComb")
    public String HeadquarterAddressComb;

    @JsonProperty("HeadquarterPopulatedPlace")
    public String HeadquarterPopulatedPlace;

    @JsonProperty("HeadquarterZip")
    public String HeadquarterZip;

    @JsonProperty("HeadquarterDistrict")
    public String HeadquarterDistrict;

    @JsonProperty("HeadquarterQuarterRaType")
    public String HeadquarterQuarterRaType;

    @JsonProperty("HeadquarterQuarterRaName")
    public String HeadquarterQuarterRaName;

    @JsonProperty("HeadquarterStrBlvdType")
    public String HeadquarterStrBlvdType;

    @JsonProperty("HeadquarterStrBlvdName")
    public String HeadquarterStrBlvdName;

    @JsonProperty("HeadquarterStrBlvdNumber")
    public String HeadquarterStrBlvdNumber;

    @JsonProperty("HeadquarterBlock")
    public String HeadquarterBlock;

    @JsonProperty("HeadquarterEntrance")
    public String HeadquarterEntrance;

    @JsonProperty("HeadquarterFloor")
    public String HeadquarterFloor;

    @JsonProperty("HeadquarterApartment")
    public String HeadquarterApartment;

    @JsonProperty("HeadquarterAdditionalInfo")
    public String HeadquarterAdditionalInfo;

    @JsonProperty("CommunicationAddressComb")
    public String CommunicationAddressComb;

    @JsonProperty("CommunicationPopulatedPlace")
    public String CommunicationPopulatedPlace;

    @JsonProperty("CommunicationZip")
    public String CommunicationZip;

    @JsonProperty("CommunicationDistrict")
    public String CommunicationDistrict;

    @JsonProperty("CommunicationQuarterRaType")
    public String CommunicationQuarterRaType;

    @JsonProperty("CommunicationQuarterRaName")
    public String CommunicationQuarterRaName;

    @JsonProperty("CommunicationStrBlvdType")
    public String CommunicationStrBlvdType;

    @JsonProperty("CommunicationStrBlvdName")
    public String CommunicationStrBlvdName;

    @JsonProperty("CommunicationStrBlvdNumber")
    public String CommunicationStrBlvdNumber;

    @JsonProperty("CommunicationBlock")
    public String CommunicationBlock;

    @JsonProperty("CommunicationEntrance")
    public String CommunicationEntrance;

    @JsonProperty("CommunicationFloor")
    public String CommunicationFloor;

    @JsonProperty("CommunicationApartment")
    public String CommunicationApartment;

    @JsonProperty("CommunicationAdditionalInfo")
    public String CommunicationAdditionalInfo;

    @JsonProperty("CustomerSegments")
    public List<String> CustomerSegments;

    @JsonProperty("Liabilities")
    public LiabilityForReminder Liabilities;

    public void mapToJson(ReminderDocumentModelMiddleResponse middleResponse) {
        this.CustomerNameComb = middleResponse.getCustomerNameComb();
        this.CustomerNameCombTrsl = middleResponse.getCustomerNameCombTrsl();
        this.CustomerIdentifier = middleResponse.getCustomerIdentifier();
        this.CustomerNumber = middleResponse.getCustomerNumber();

        this.HeadquarterAddressComb = middleResponse.getHeadquarterAddressComb();
        this.HeadquarterPopulatedPlace = middleResponse.getHeadquarterPopulatedPlace();
        this.HeadquarterZip = middleResponse.getHeadquarterZip();
        this.HeadquarterDistrict = middleResponse.getHeadquarterDistrict();
        this.HeadquarterQuarterRaType = middleResponse.getHeadquarterQuarterRaType();
        this.HeadquarterQuarterRaName = middleResponse.getHeadquarterQuarterRaName();
        this.HeadquarterStrBlvdType = middleResponse.getHeadquarterStrBlvdType();
        this.HeadquarterStrBlvdName = middleResponse.getHeadquarterStrBlvdName();
        this.HeadquarterStrBlvdNumber = middleResponse.getHeadquarterStrBlvdNumber();
        this.HeadquarterBlock = middleResponse.getHeadquarterBlock();
        this.HeadquarterEntrance = middleResponse.getHeadquarterEntrance();
        this.HeadquarterFloor = middleResponse.getHeadquarterFloor();
        this.HeadquarterApartment = middleResponse.getHeadquarterApartment();
        this.HeadquarterAdditionalInfo = middleResponse.getHeadquarterAdditionalInfo();

        this.CommunicationAddressComb = middleResponse.getCommunicationAddressComb();
        this.CommunicationPopulatedPlace = middleResponse.getCommunicationPopulatedPlace();
        this.CommunicationZip = middleResponse.getCommunicationZip();
        this.CommunicationDistrict = middleResponse.getCommunicationDistrict();
        this.CommunicationQuarterRaType = middleResponse.getCommunicationQuarterRaType();
        this.CommunicationQuarterRaName = middleResponse.getCommunicationQuarterRaName();
        this.CommunicationStrBlvdType = middleResponse.getCommunicationStrBlvdType();
        this.CommunicationStrBlvdName = middleResponse.getCommunicationStrBlvdName();
        this.CommunicationStrBlvdNumber = middleResponse.getCommunicationStrBlvdNumber();
        this.CommunicationBlock = middleResponse.getCommunicationBlock();
        this.CommunicationEntrance = middleResponse.getCommunicationEntrance();
        this.CommunicationFloor = middleResponse.getCommunicationFloor();
        this.CommunicationApartment = middleResponse.getCommunicationApartment();
        this.CommunicationAdditionalInfo = middleResponse.getCommunicationAdditionalInfo();

        this.CustomerSegments = middleResponse.getCustomerSegments();
        this.Currency = middleResponse.getCurrency();
    }

}