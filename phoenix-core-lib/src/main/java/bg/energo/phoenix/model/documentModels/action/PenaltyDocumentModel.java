package bg.energo.phoenix.model.documentModels.action;

import bg.energo.phoenix.model.documentModels.mlo.Manager;
import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import bg.energo.phoenix.util.epb.EPBListUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PenaltyDocumentModel extends CompanyDetailedInformationModelImpl {
    public String CustomerNameComb;
    public String CustomerNameCombTrsl;
    public String CustomerIdentifer;
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
    public List<String> CustomerSegments;
    public List<Manager> Managers;
    public String ContractNumber;
    public LocalDate ContractDate;
    public String ContractProductName;
    public String ContractType;
    public LocalDate ContractTerminationDate;
    public LocalDate ContractTerminationDatePlus1;
    public LocalDate ActionExecutionDate;
    public LocalDate ActionNoticeDate;
    public String TerminationClauseNumber;
    public String PenaltyClauseNumber;
    public BigDecimal PenaltyClaimAmount;
    public String PenaltyClaimCurrency;
    public LocalDate PenaltyPaymentDueDate;
    public String PenaltyPayer;
    public String TerminationNoticePeriod;
    public String TerminationNoticePeriodType;
    public LocalDate CalculatedTerminationDate;
    public LocalDate CalculatedTerminationDatePlus1;
    public List<ActionPodModel> TerminationPODs;

    public void from(PenaltyDocumentResponse response, List<Manager> managers, List<ActionPodModel> terminationPODs, LocalDate penaltyDueDate) {
        this.CustomerNameComb = response.getCustomerNameComb();
        this.CustomerNameCombTrsl = response.getCustomerNameCombTrsl();
        this.CustomerIdentifer = response.getCustomerIdentifier();
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

        this.CustomerSegments = EPBListUtils.convertDBStringArrayIntoListString(response.getCustomerSegments());
        this.Managers = managers;

        this.ContractNumber = response.getContractNumber();
        this.ContractDate = response.getContractDate();
        this.ContractTerminationDate = response.getContractTerminationDate();
        this.ContractTerminationDatePlus1 = response.getContractTerminationDatePlus1();
        this.ContractType = response.getContractType();
        this.ContractProductName = response.getContractProductName();
        this.ActionExecutionDate = response.getActionExecutionDate();
        this.ActionNoticeDate = response.getActionNoticeDate();
        this.TerminationClauseNumber = response.getTerminationClauseNumber();
        this.PenaltyClauseNumber = response.getPenaltyClauseNumber();
        this.PenaltyClaimAmount = response.getPenaltyClaimAmount();
        this.PenaltyClaimCurrency = response.getPenaltyClaimCurrency();
        this.PenaltyPayer = response.getPenaltyPayer();
        this.PenaltyPaymentDueDate = penaltyDueDate;
        this.TerminationNoticePeriod = response.getTerminationNoticePeriod();
        this.TerminationNoticePeriodType = response.getTerminationNoticePeriodType();
        this.CalculatedTerminationDate = response.getCalculatedTerminationDate();
        this.CalculatedTerminationDatePlus1 = response.getCalculatedTerminationDatePlus1();
        this.TerminationPODs = terminationPODs;
    }
}
