package bg.energo.phoenix.service.billing.companyDetails;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.*;
import bg.energo.phoenix.model.request.billing.companyDetails.CompanyBankDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.CompanyCommunicationChannelDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.CompanyDetailedParameterDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyBankDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyCommunicationChannelDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyDetailedParameterDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyDetailMapper {

    /**
     * Use only for create new entity obj
     */
    public CompanyDetails mapToCompanyDetail(String identifier, String vatNumber,
                                             String numberUnderExciseDutiesTaxWhAct,
                                             String name,
                                             String nameTranslated,
                                             String managementAddress,
                                             String managementAddressTranslated,
                                             Long companyId,
                                             LocalDate startDate) {
        CompanyDetails companyDetails = new CompanyDetails();
        companyDetails = fillCompanyDetail(identifier, vatNumber,
                numberUnderExciseDutiesTaxWhAct,
                name,
                nameTranslated,
                managementAddress,
                managementAddressTranslated,
                companyDetails);
        companyDetails.setCompanyId(companyId);
        companyDetails.setStartDate(startDate);
        return companyDetails;
    }

    public CompanyDetails fillCompanyDetail(String identifier, String vatNumber,
                                            String numberUnderExciseDutiesTaxWhAct,
                                            String name,
                                            String nameTranslated,
                                            String managementAddress,
                                            String managementAddressTranslated,
                                            CompanyDetails companyDetails) {
        companyDetails.setIdentifier(identifier);
        companyDetails.setVatNumber(vatNumber);
        companyDetails.setNumberUnderExciseDutiesTaxWhAct(numberUnderExciseDutiesTaxWhAct);
        companyDetails.setName(name);
        companyDetails.setNameTranslated(nameTranslated);
        companyDetails.setManagementAddress(managementAddress);
        companyDetails.setManagementAddressTranslated(managementAddressTranslated);
        return companyDetails;
    }

    /**
     * Use only for create new entity obj
     */
    public CompanyBank mapToCompanyBank(Long bankId, String iban, Long companyDetailId) {
        CompanyBank companyBank = new CompanyBank();
        return fillCompanyBank(bankId, iban, companyDetailId, companyBank);
    }


    public CompanyBank fillCompanyBank(Long bankId, String iban, Long companyDetailId, CompanyBank companyBank) {
        companyBank.setCompanyDetailId(companyDetailId);
        companyBank.setBankId(bankId);
        companyBank.setIban(iban);
        companyBank.setStatus(EntityStatus.ACTIVE);
        return companyBank;
    }

    /**
     * Use only for create new entity obj
     */
    public CompanyEmail mapToCompanyEmail(String email, Long companyDetailId) {
        CompanyEmail companyEmail = new CompanyEmail();
        return fillCompanyEmail(email, companyDetailId, companyEmail);
    }

    public CompanyEmail fillCompanyEmail(String email, Long companyDetailId, CompanyEmail companyEmail) {
        companyEmail.setEmail(email);
        companyEmail.setCompanyDetailId(companyDetailId);
        companyEmail.setStatus(EntityStatus.ACTIVE);
        return companyEmail;
    }

    /**
     * Use only for create new entity obj
     */
    public CompanyTelephone mapToCompanyTelephone(String telephone, Long companyDetailId) {
        CompanyTelephone companyTelephone = new CompanyTelephone();
        return fillCompanyTelephone(telephone, companyDetailId, companyTelephone);
    }

    public CompanyTelephone fillCompanyTelephone(String telephone, Long companyDetailId, CompanyTelephone companyTelephone) {
        companyTelephone.setCompanyDetailId(companyDetailId);
        companyTelephone.setTelephone(telephone);
        companyTelephone.setStatus(EntityStatus.ACTIVE);
        return companyTelephone;
    }

    /**
     * Use only for create new entity obj
     */
    public CompanyInvoiceCompiler mapToCompanyInvoiceCompiler(String parameter, String parameterTranslated, Long companyDetailId) {
        CompanyInvoiceCompiler companyInvoiceCompiler = new CompanyInvoiceCompiler();
        return fillCompanyInvoiceCompiler(parameter, parameterTranslated, companyDetailId, companyInvoiceCompiler);
    }


    public CompanyInvoiceCompiler fillCompanyInvoiceCompiler(String parameter, String parameterTranslated, Long companyDetailId, CompanyInvoiceCompiler companyInvoiceCompiler) {
        companyInvoiceCompiler.setCompanyDetailId(companyDetailId);
        companyInvoiceCompiler.setInvoiceCompiler(parameter);
        companyInvoiceCompiler.setInvoiceCompilerTranslated(parameterTranslated);
        companyInvoiceCompiler.setStatus(EntityStatus.ACTIVE);
        return companyInvoiceCompiler;
    }

    /**
     * Use only for create new entity obj
     */
    public CompanyInvoiceIssuePlace mapToCompanyInvoiceIssuePlace(String parameter, String parameterTranslated, Long companyDetailId) {
        CompanyInvoiceIssuePlace companyInvoiceIssuePlace = new CompanyInvoiceIssuePlace();
        return fillCompanyInvoiceIssuePlace(parameter, parameterTranslated, companyDetailId, companyInvoiceIssuePlace);
    }

    public CompanyInvoiceIssuePlace fillCompanyInvoiceIssuePlace(String parameter, String parameterTranslated, Long companyDetailId, CompanyInvoiceIssuePlace companyInvoiceIssuePlace) {
        companyInvoiceIssuePlace.setCompanyDetailId(companyDetailId);
        companyInvoiceIssuePlace.setInvoiceIssuePlace(parameter);
        companyInvoiceIssuePlace.setInvoiceIssuePlaceTranslated(parameterTranslated);
        companyInvoiceIssuePlace.setStatus(EntityStatus.ACTIVE);
        return companyInvoiceIssuePlace;
    }

    /**
     * Use only for create new entity obj
     */
    public CompanyManager mapToCompanyManager(String parameter, String parameterTranslated, Long companyDetailId) {
        CompanyManager companyManager = new CompanyManager();
        return fillCompanyManager(parameter, parameterTranslated, companyDetailId, companyManager);
    }


    public CompanyManager fillCompanyManager(String parameter, String parameterTranslated, Long companyDetailId, CompanyManager companyManager) {
        companyManager.setCompanyDetailId(companyDetailId);
        companyManager.setManager(parameter);
        companyManager.setManagerTranslated(parameterTranslated);
        companyManager.setStatus(EntityStatus.ACTIVE);
        return companyManager;
    }

    /**
     * Use only for create new entity obj
     */
    public CompanyCommunicationAddress mapToCompanyCommunicationAddress(String parameter, String parameterTranslated, Long companyDetailId) {
        CompanyCommunicationAddress companyCommunicationAddress = new CompanyCommunicationAddress();
        return fillCompanyCommunicationAddress(parameter, parameterTranslated, companyDetailId, companyCommunicationAddress);
    }

    public CompanyCommunicationAddress fillCompanyCommunicationAddress(String parameter, String parameterTranslated, Long companyDetailId, CompanyCommunicationAddress companyCommunicationAddress) {
        companyCommunicationAddress.setCompanyDetailId(companyDetailId);
        companyCommunicationAddress.setAddress(parameter);
        companyCommunicationAddress.setAddressTranslated(parameterTranslated);
        companyCommunicationAddress.setStatus(EntityStatus.ACTIVE);
        return companyCommunicationAddress;
    }

    public List<BaseCompanyCommunicationChannelDTO> castToBaseCompanyCommunicationChannelDTOList(List<CompanyCommunicationChannelDTO> companyCommunicationChannelDTOList) {
        return companyCommunicationChannelDTOList
                .stream()
                .map(e -> (BaseCompanyCommunicationChannelDTO) e)
                .collect(Collectors.toList());
    }

    public List<BaseCompanyDetailedParameterDTO> castToBaseCompanyDetailedParameterDTOList(List<CompanyDetailedParameterDTO> CompanyDetailedParameterDTOList) {
        return CompanyDetailedParameterDTOList
                .stream()
                .map(e -> (BaseCompanyDetailedParameterDTO) e)
                .collect(Collectors.toList());
    }

    public List<BaseCompanyBankDTO> castToBaseCompanyBankDTOList(List<CompanyBankDTO> companyBankDTOList) {
        return companyBankDTOList
                .stream()
                .map(e -> (BaseCompanyBankDTO) e)
                .collect(Collectors.toList());
    }

}
