package bg.energo.phoenix.model.documentModels.remiderForDcn;

import bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder.ReminderForDcnDocumentMiddleResponse;
import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;

import java.time.LocalDate;
import java.util.List;

public class ReminderForDcnDocumentModel extends CompanyDetailedInformationModelImpl {
    public String ReminderNumber;
    public String CommunicationNumber;
    public LocalDate DisconnectionDate;
    public String LiabilitiesTotalAmount;
    public String Currency;

    public String CustomerNameComb;
    public String CustomerNameCombTrsl;
    public String CustomerIdentifier;
    public String CustomerNumber;

    public String HeadquarterAddressComb;
    public String HeadquarterPopulatedPlace;
    public String HeadquarterZip;
    public String HeadquarterDistrict;
    public String HeadquarterQuarterRaType;
    public String HeadquarterQuarterRaName;
    public String HeadquarterStrBlvdType;
    public String HeadquarterStrBlvdName;
    public String HeadquarterStrBlvdNumber;
    public String HeadquarterBlock;
    public String HeadquarterEntrance;
    public String HeadquarterFloor;
    public String HeadquarterApartment;
    public String HeadquarterAdditionalInfo;

    public String CommunicationAddressComb;
    public String CommunicationPopulatedPlace;
    public String CommunicationZip;
    public String CommunicationDistrict;
    public String CommunicationQuarterRaType;
    public String CommunicationQuarterRaName;
    public String CommunicationStrBlvdType;
    public String CommunicationStrBlvdName;
    public String CommunicationStrBlvdNumber;
    public String CommunicationBlock;
    public String CommunicationEntrance;
    public String CommunicationFloor;
    public String CommunicationApartment;
    public String CommunicationAdditionalInfo;

    public String CustomerSegments;
    public List<LiabilityForReminderForDcn> Liabilities;


    public void setAdditionalFields(String reminderNumber, String communicationNumber, LocalDate disconnectionDate, String liabilitiesTotalAmount, String currency) {
        ReminderNumber = reminderNumber;
        CommunicationNumber = communicationNumber;
        DisconnectionDate = disconnectionDate;
        LiabilitiesTotalAmount = liabilitiesTotalAmount;
        Currency = currency;
    }

    public void from(ReminderForDcnDocumentMiddleResponse response) {
        this.CustomerNameComb = response.getCustomerNameComb();
        this.CustomerNameCombTrsl = response.getCustomerNameCombTrsl();
        this.CustomerIdentifier = response.getCustomerIdentifier();
        this.CustomerNumber = response.getCustomerNumber();

        this.HeadquarterAddressComb = response.getHeadquarterAddressComb();
        this.HeadquarterPopulatedPlace = response.getHeadquarterPopulatedPlace();
        this.HeadquarterZip = response.getHeadquarterZip();
        this.HeadquarterDistrict = response.getHeadquarterDistrict();
        this.HeadquarterQuarterRaType = response.getHeadquarterQuarterRaType();
        this.HeadquarterQuarterRaName = response.getHeadquarterQuarterRaName();
        this.HeadquarterStrBlvdType = response.getHeadquarterStrBlvdType();
        this.HeadquarterStrBlvdName = response.getHeadquarterStrBlvdName();
        this.HeadquarterStrBlvdNumber = response.getHeadquarterStrBlvdNumber();
        this.HeadquarterBlock = response.getHeadquarterBlock();
        this.HeadquarterEntrance = response.getHeadquarterEntrance();
        this.HeadquarterFloor = response.getHeadquarterFloor();
        this.HeadquarterApartment = response.getHeadquarterApartment();
        this.HeadquarterAdditionalInfo = response.getHeadquarterAdditionalInfo();

        this.CommunicationAddressComb = response.getCommunicationAddressComb();
        this.CommunicationPopulatedPlace = response.getCommunicationPopulatedPlace();
        this.CommunicationZip = response.getCommunicationZip();
        this.CommunicationDistrict = response.getCommunicationDistrict();
        this.CommunicationQuarterRaType = response.getCommunicationQuarterRaType();
        this.CommunicationQuarterRaName = response.getCommunicationQuarterRaName();
        this.CommunicationStrBlvdType = response.getCommunicationStrBlvdType();
        this.CommunicationStrBlvdName = response.getCommunicationStrBlvdName();
        this.CommunicationStrBlvdNumber = response.getCommunicationStrBlvdNumber();
        this.CommunicationBlock = response.getCommunicationBlock();
        this.CommunicationEntrance = response.getCommunicationEntrance();
        this.CommunicationFloor = response.getCommunicationFloor();
        this.CommunicationApartment = response.getCommunicationApartment();
        this.CommunicationAdditionalInfo = response.getCommunicationAdditionalInfo();
        this.CustomerSegments = response.getCustomerSegments();
    }

    public void mapLiabilities( List<LiabilityForReminderForDcn> liabilitiesForReminderForDcn) {
        this.Liabilities = liabilitiesForReminderForDcn;
    }

}