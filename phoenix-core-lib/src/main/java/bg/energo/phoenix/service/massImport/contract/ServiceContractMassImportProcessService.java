package bg.energo.phoenix.service.massImport.contract;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.product.service.EPService;
import bg.energo.phoenix.model.request.contract.service.ServiceContractAdditionalParametersRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractBankingDetails;
import bg.energo.phoenix.model.request.contract.service.ServiceContractCreateRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractBasicParametersEditRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractEditRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractServiceParametersEditRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.service.contract.service.ServiceContractService;
import bg.energo.phoenix.service.massImport.AbstractMassImportProcessService;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.time.LocalDate.from;

@Service
@Slf4j
public class ServiceContractMassImportProcessService extends AbstractMassImportProcessService {
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final ServiceContractService serviceContractService;
    private final ServiceContractExcelMapper excelMapper;
    @Value("${app.cfg.serviceContract.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.serviceContract.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.serviceContract.massImport.numberOfThreads}")
    private Integer numberOfThreads;


    public ServiceContractMassImportProcessService(ServiceContractExcelMapper excelMapper,
                                                   ProcessRepository processRepository,
                                                   ProcessedRecordInfoRepository processedRecordInfoRepository,
                                                   ServiceContractService serviceContractService,
                                                   ServiceContractsRepository serviceContractsRepository,
                                                   ServiceContractDetailsRepository serviceContractDetailsRepository,
                                                   NotificationEventPublisher notificationEventPublisher) {
        super(processRepository, processedRecordInfoRepository, notificationEventPublisher);
        this.excelMapper = excelMapper;
        this.serviceContractService = serviceContractService;
        this.serviceContractsRepository = serviceContractsRepository;
        this.serviceContractDetailsRepository = serviceContractDetailsRepository;
    }

    @Override
    public boolean supports(EventType eventType) {
        return EventType.SERVICE_CONTRACT_MASS_IMPORT_PROCESS.equals(eventType);
    }

    /*@Override
    protected String processRow(Row row, Set<String> permissions, String processSysUserId) {*/
    @Override
    protected String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date,Long processRecordInfo) {
        List<String> errorMessages = new ArrayList<>();
        String contractNumber = getContractNumber(row);
        Long contractVersion = getContractVersion(row);
        String contractCreateEdit = getContractCreateEdit(row);

        Boolean contractExistsInSystem = false;
        Boolean versionExistsInSystem = false;
        ServiceContracts existingServiceContracts = getContractExistInSystem(contractNumber);
        if (existingServiceContracts != null) {
            contractExistsInSystem = true;
        }
        ServiceContractDetails existingContractDetails = getContractVersionExistsInSystem(contractNumber, contractVersion);
        if (existingContractDetails != null) {
            versionExistsInSystem = true;
        }
        if (contractNumber == null && contractVersion == null && contractCreateEdit == null) {
            //CREATE NEW CONTRACT
            if (!permissions.contains(PermissionEnum.SERVICE_CONTRACT_MI_CREATE.getId()))
                throw new ClientException("Not enough permission for creating service contract", ErrorCode.ACCESS_DENIED);
            ServiceContractCreateRequest request = excelMapper.toCreateServiceContractRequest(row, errorMessages, false);
            validateRequest(errorMessages, request);
            return String.valueOf(serviceContractService.create(request));
        } else if (contractNumber != null && contractVersion == null && (contractCreateEdit != null && contractCreateEdit.equals("C"))) {
            //CREATE NEW VERSION BASED ON IMPORTED DATA
            //if contractStartDate is null return error
            if (!permissions.contains(PermissionEnum.SERVICE_CONTRACT_MI_EDIT.getId()))
                throw new ClientException("Not enough permission for creating service contract", ErrorCode.ACCESS_DENIED);
            ServiceContracts serviceContracts = excelMapper.getServiceContractWithNumber(contractNumber, errorMessages);
            ServiceContractDetails serviceContractDetails = excelMapper.getMaxServiceContractDetailIdWithContractId(serviceContracts.getId(), errorMessages);
            if (serviceContractDetails == null) {
                errorMessages.add("ServiceContractDetails is null;");
            }
            ServiceContractEditRequest serviceContractEditRequest = excelMapper.toEditServiceContractRequestOnlyBasedOnExcel(row, errorMessages, contractNumber, serviceContractDetails.getVersionId());
            LocalDate contractStartDate = getDateValue(3, row);
            if (contractStartDate == null) {
                throw new ClientException("Start Date can't be null;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            } else {
                serviceContractEditRequest.getServiceParameters().setGuaranteeContract(false); //TODO WHERE GET THIS CHECKBOX VALUE
                // ServiceContractDetails serviceContractDetails = excelMapper.getServiceContractDetails(serviceContracts.getId(),contractVersion,errorMessages);
                serviceContractEditRequest.setSavingAsNewVersion(true);
                validateRequest(errorMessages, serviceContractEditRequest);
                //TODO serviceContractEditRequest add subobjects with validations
                return String.valueOf(serviceContractService.update(serviceContractEditRequest, serviceContracts.getId(), serviceContractDetails.getVersionId(), true));
            }
        } else if (contractNumber != null
                && (contractExistsInSystem != null && contractExistsInSystem)
                && (contractVersion != null && contractVersion == 0L)
                && (contractCreateEdit != null && contractCreateEdit.equals("C"))) {
            //System should create new version based on latest contract’s version with Start date
            //if contractStartDate is null return error
            //if version already exists with this start date should return error
            //only filled in values should be filled from excel in serviceContract others stay as is
            if (!permissions.contains(PermissionEnum.SERVICE_CONTRACT_MI_CREATE.getId()))
                throw new ClientException("Not enough permission for editing service contract", ErrorCode.ACCESS_DENIED);
            ServiceContractDetails serviceContractDetails = excelMapper.getMaxServiceContractDetailIdWithContractId(existingServiceContracts.getId(),errorMessages);
            ServiceContractEditRequest serviceContractEditRequest = excelMapper.toEditServiceContractRequest(row,errorMessages,contractNumber,serviceContractDetails.getVersionId());
            LocalDate contractStartDate = getDateValue(4, row);
            if (contractStartDate == null) {
                errorMessages.add("Start Date can't be null;");
                return null;
            }
            ServiceContracts serviceContracts = excelMapper.getServiceContractWithNumber(contractNumber,errorMessages);
            //ServiceContractDetails serviceContractDetails = excelMapper.getServiceContractDetails(serviceContracts.getId(),contractVersion,errorMessages);
            ServiceContractDetails latestServiceContractDetails = excelMapper.getLatestContractDetailsId(serviceContracts.getId(),errorMessages);
            serviceContractEditRequest.setSavingAsNewVersion(true);
            validateRequest(errorMessages, serviceContractEditRequest);
            return String.valueOf(serviceContractService.update(serviceContractEditRequest,serviceContracts.getId(),latestServiceContractDetails.getVersionId(),true));
        } else if(contractNumber != null
                && (contractExistsInSystem!= null && contractExistsInSystem)
                && (contractVersion!= null && contractVersion == 0L)
                && (contractCreateEdit != null && contractCreateEdit.equals("E"))){
            //System should update latest contract’s version with provided data
            //if some field is empty in excel it means that already saved value shouldn’t be updated
            //if start date is empty - do not update start date
            //only filled in values should be updated in serviceContract others stay as is
            if (!permissions.contains(PermissionEnum.SERVICE_CONTRACT_MI_EDIT.getId()))
                throw new ClientException("Not enough permission for editing service contract", ErrorCode.ACCESS_DENIED);
            ServiceContractDetails serviceContractDetails = excelMapper.getMaxServiceContractDetailIdWithContractId(existingServiceContracts.getId(),errorMessages);
            ServiceContractEditRequest serviceContractEditRequest = excelMapper.toEditServiceContractRequest(row,errorMessages,contractNumber,serviceContractDetails.getVersionId());
            ServiceContracts serviceContracts = excelMapper.getServiceContractWithNumber(contractNumber,errorMessages);
            ServiceContractDetails latestServiceContractDetails = excelMapper.getLatestContractDetailsId(serviceContracts.getId(),errorMessages);
            serviceContractEditRequest.setSavingAsNewVersion(false);
            validateRequest(errorMessages, serviceContractEditRequest);
            return String.valueOf(serviceContractService.update(serviceContractEditRequest,serviceContracts.getId(),latestServiceContractDetails.getVersionId(),true));
        }else if(contractNumber != null
                && (contractExistsInSystem!= null && contractExistsInSystem)
                && (contractVersion!= null && contractVersion == 0L)
                && contractCreateEdit == null){
            errorMessages.add("contract number , contract version and contract create edit field value combination is incorrect");
            return null;
        }else if(contractNumber != null
                && (contractExistsInSystem!= null && contractExistsInSystem)
                && (versionExistsInSystem != null && versionExistsInSystem)
                && (contractCreateEdit != null && contractCreateEdit.equals("C"))){
            //Should create new version
            if (!permissions.contains(PermissionEnum.SERVICE_CONTRACT_MI_EDIT.getId()))
                throw new ClientException("Not enough permission for editing service contract", ErrorCode.ACCESS_DENIED);
            ServiceContractEditRequest serviceContractEditRequest = excelMapper.toEditServiceContractRequestOnlyBasedOnExcel(row,errorMessages,contractNumber,contractVersion);
            LocalDate contractStartDate = getDateValue(4, row);
            if (contractStartDate == null) {
                errorMessages.add("Start Date can't be null;");
                return null;
            }
            ServiceContracts serviceContracts = excelMapper.getServiceContractWithNumber(contractNumber,errorMessages);
            ServiceContractDetails serviceContractDetails = excelMapper.getServiceContractDetails(serviceContracts.getId(),contractVersion,errorMessages);
            serviceContractEditRequest.setSavingAsNewVersion(true);
            validateRequest(errorMessages, serviceContractEditRequest);
            return String.valueOf(serviceContractService.update(serviceContractEditRequest,serviceContracts.getId(),serviceContractDetails.getVersionId(),true));
        }else if(contractNumber != null
                && (contractExistsInSystem!= null && contractExistsInSystem)
                && (versionExistsInSystem != null && versionExistsInSystem)
                && (contractCreateEdit != null && contractCreateEdit.equals("E"))){
            //Should edit existing contract
            if (!permissions.contains(PermissionEnum.SERVICE_CONTRACT_MI_EDIT.getId()))
                throw new ClientException("Not enough permission for editing service contract", ErrorCode.ACCESS_DENIED);
            ServiceContractEditRequest serviceContractEditRequest = excelMapper.toEditServiceContractRequestOnlyBasedOnExcel(row,errorMessages,contractNumber,contractVersion);
            LocalDate contractStartDate = getDateValue(4, row);
            if (contractStartDate == null) {
                errorMessages.add("Start Date can't be null;");
            }
            if(!CollectionUtils.isEmpty(errorMessages)){
                throw new ClientException("%s".formatted(errorMessages),ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
            ServiceContracts serviceContracts = excelMapper.getServiceContractWithNumber(contractNumber,errorMessages);
            ServiceContractDetails serviceContractDetails = excelMapper.getServiceContractDetails(serviceContracts.getId(),contractVersion,errorMessages);
            serviceContractEditRequest.setSavingAsNewVersion(false);
            serviceContractEditRequest = fillNullValuesOfEditRequest(serviceContractEditRequest,serviceContracts,serviceContractDetails,errorMessages);
            validateRequest(errorMessages, serviceContractEditRequest);
            return String.valueOf(serviceContractService.update(serviceContractEditRequest,serviceContracts.getId(),serviceContractDetails.getVersionId(),true));
        } else {
            throw new ClientException("contract number , contract version and contract create edit field value combination is incorrect", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    private ServiceContractEditRequest fillNullValuesOfEditRequest(ServiceContractEditRequest serviceContractEditRequest,
                                                                   ServiceContracts serviceContracts,
                                                                   ServiceContractDetails serviceContractDetails,List<String> errorMessages) {
        ServiceContractBasicParametersEditRequest basicParameters = serviceContractEditRequest.getBasicParameters();
        ServiceContractAdditionalParametersRequest additionalParameters = serviceContractEditRequest.getAdditionalParameters();
        ServiceContractServiceParametersEditRequest serviceParameters = serviceContractEditRequest.getServiceParameters();
        if(basicParameters != null){
            if(basicParameters.getServiceId() == null){
                EPService service = excelMapper.getServiceByServiceDetails(serviceContractDetails.getServiceDetailId());
                if(service != null){
                    basicParameters.setServiceId(service.getId());
                }
            }
            if(basicParameters.getServiceVersionId() == null){
                basicParameters.setServiceVersionId(serviceContractDetails.getVersionId());
            }
            if(basicParameters.getContractStatus() == null){
                basicParameters.setContractStatus(serviceContracts.getContractStatus());
            }
            if(basicParameters.getContractStatusModifyDate() == null){
                basicParameters.setContractStatusModifyDate(serviceContracts.getStatusModifyDate());
            }
            if(basicParameters.getContractType() == null){
                basicParameters.setContractType(serviceContractDetails.getType());
            }

            if(basicParameters.getDetailsSubStatus() == null){
                basicParameters.setDetailsSubStatus(serviceContracts.getSubStatus());
            }
            if(basicParameters.getSignInDate() == null){
                basicParameters.setSignInDate(serviceContracts.getSigningDate());
            }
            if(basicParameters.getEntryIntoForceDate() == null){
                basicParameters.setEntryIntoForceDate(serviceContractDetails.getEntryIntoForceValue());
            }
            if(basicParameters.getContractTermUntilAmountIsReached() == null){
                basicParameters.setContractTermUntilAmountIsReached(serviceContractDetails.getContractTermUntilTheAmountValue());
            }
            if(basicParameters.getContractTermUntilAmountIsReachedCheckbox() == null){
                basicParameters.setContractTermUntilAmountIsReachedCheckbox(serviceContractDetails.getContractTermUntilTheAmount());
            }
            if(basicParameters.getCurrencyId() == null){
                basicParameters.setCurrencyId(serviceContractDetails.getCurrencyId());
            }
            if(basicParameters.getCustomerId() == null){
                Customer customer = excelMapper.getCustomerWithDetailsId(serviceContractDetails.getCustomerDetailId(),errorMessages);
                if(customer != null){
                    basicParameters.setCustomerId(customer.getId());
                }
            }
            if(basicParameters.getCustomerVersionId() == null){
                basicParameters.setCustomerVersionId(serviceContractDetails.getCustomerDetailId());
            }
            if(basicParameters.getCommunicationDataForBilling() == null){
                basicParameters.setCommunicationDataForBilling(serviceContractDetails.getCustomerCommunicationIdForBilling());
            }
            if(basicParameters.getCommunicationDataForContract() == null){
                basicParameters.setCommunicationDataForContract(serviceContractDetails.getCustomerCommunicationIdForContract());
            }
            if(basicParameters.getContractVersionStatus() == null){
                basicParameters.setContractVersionStatus(serviceContractDetails.getContractVersionStatus());
            }
            /*if(basicParameters.getContractVersionTypes() == null){
                basicParameters.setCustomerVersionId(serviceContractDetails.getCustomerDetailId());
            }*/
            if(basicParameters.getStartOfTheInitialTermOfTheContract() == null){
                basicParameters.setStartOfTheInitialTermOfTheContract(serviceContracts.getContractInitialTermStartDate());
            }
            if(basicParameters.getTerminationDate() == null){
                basicParameters.setTerminationDate(serviceContracts.getTerminationDate());
            }
            if(basicParameters.getPerpetuityDate() == null){
                basicParameters.setPerpetuityDate(serviceContracts.getPerpetuityDate());
            }
            if(basicParameters.getContractTermEndDate() == null){
                basicParameters.setContractTermEndDate(serviceContracts.getContractTermEndDate());
            }
            if(basicParameters.getStartDate() == null){
                basicParameters.setStartDate(serviceContractDetails.getStartDate());
            }
            if(basicParameters.getContractVersionTypes() == null){
                basicParameters.setContractVersionTypes(getContractVersionTypes(serviceContractDetails));
            }

        }
        if(additionalParameters != null){
            if(additionalParameters.getBankingDetails() == null){
                ServiceContractBankingDetails bankingDetails = new ServiceContractBankingDetails();
                bankingDetails.setDirectDebit(serviceContractDetails.getDirectDebit());
                bankingDetails.setBankId(serviceContractDetails.getBankId());
                bankingDetails.setIban(serviceContractDetails.getIban());
                additionalParameters.setBankingDetails(bankingDetails);
            }
            if(additionalParameters.getInterestRateId() == null){
                additionalParameters.setInterestRateId(serviceContractDetails.getApplicableInterestRate());
            }
            if(additionalParameters.getEmployeeId() == null){
                additionalParameters.setEmployeeId(serviceContractDetails.getEmployeeId());
            }

        }
        if(serviceParameters != null){
            if(serviceParameters.getContractTermId() == null){
                serviceParameters.setContractTermId(serviceContractDetails.getServiceContractTermId());
            }
            if(serviceParameters.getContractTermEndDate() == null){
                serviceParameters.setContractTermEndDate(serviceContractDetails.getContractTermEndDate());
            }

            if(serviceParameters.getEntryIntoForce() == null){
                serviceParameters.setEntryIntoForce(serviceContractDetails.getEntryIntoForce());
            }
            if(serviceParameters.getEntryIntoForceDate() == null){
                serviceParameters.setEntryIntoForceDate(serviceContractDetails.getEntryIntoForceValue());
            }
            if(serviceParameters.getPaymentGuarantee() == null){
                serviceParameters.setPaymentGuarantee(serviceContractDetails.getPaymentGuarantee());
            }
            if(serviceParameters.getCashDepositAmount() == null){
                serviceParameters.setCashDepositAmount(serviceContractDetails.getCashDepositAmount());
            }
            if(serviceParameters.getCashDepositCurrencyId() == null){
                serviceParameters.setCashDepositCurrencyId(serviceContractDetails.getCashDepositCurrency());
            }
            if(serviceParameters.getBankGuaranteeAmount() == null){
                serviceParameters.setBankGuaranteeAmount(serviceContractDetails.getBankGuaranteeAmount());
            }
            if(serviceParameters.getBankGuaranteeCurrencyId() == null){
                serviceParameters.setBankGuaranteeCurrencyId(serviceContractDetails.getBankGuaranteeCurrencyId());
            }
            if(serviceParameters.getQuantity() == null && serviceContractsRepository.isServiceFromContractMappedToPerPiecePriceComponent(serviceContractDetails.getId())){
                serviceParameters.setQuantity(BigDecimal.valueOf(serviceContractDetails.getQuantity()));
            }


        }
        serviceContractEditRequest.setBasicParameters(basicParameters);
        serviceContractEditRequest.setAdditionalParameters(additionalParameters);
        serviceContractEditRequest.setServiceParameters(serviceParameters);
        return serviceContractEditRequest;
    }

    private List<Long> getContractVersionTypes(ServiceContractDetails details) {
       return excelMapper.getContractVersionTypesWithContract(details.getId());
    }

    private LocalDate getDateValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.NUMERIC);
            if (row.getCell(columnNumber).getDateCellValue() != null) {
                return from(
                        LocalDate.ofInstant(
                                row.getCell(columnNumber).getDateCellValue().toInstant(), ZoneId.systemDefault()));
            }
        }
        return null;
    }
    private ServiceContractDetails getContractVersionExistsInSystem(String contractNumber, Long contractVersion) {
        if(contractNumber == null || contractVersion == null){
            return null;
        } else {
            Optional<ServiceContracts> serviceContracts = serviceContractsRepository.findByContractNumberAndStatus(contractNumber, EntityStatus.ACTIVE);
            if (serviceContracts.isPresent()) {
                Optional<ServiceContractDetails> serviceContractDetails =
                        serviceContractDetailsRepository.findByContractIdAndVersionId(serviceContracts.get().getId(),contractVersion);
                return serviceContractDetails.orElse(null);
            } else return null;
        }
    }

    private ServiceContracts getContractExistInSystem(String contractNumber) {
        if(!StringUtils.isEmpty(contractNumber)){
            Optional<ServiceContracts> serviceContracts = serviceContractsRepository.findByContractNumberAndStatus(contractNumber, EntityStatus.ACTIVE);
            return serviceContracts.orElse(null);
        }
        return null;
    }

    private String getContractCreateEdit(Row row) {
        if((row.getCell(2) == null || row.getCell(2).getCellType() == CellType.BLANK)){
            return null;
        } else return row.getCell(2).getStringCellValue();
    }

    private Long getContractVersion(Row row) {
        if((row.getCell(1) == null || row.getCell(1).getCellType() == CellType.BLANK)){
            return null;
        } else return Long.parseLong(row.getCell(1).getStringCellValue());
    }

    private String getContractNumber(Row row) {
        if((row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK)){
            return null;
        } else return row.getCell(0).getStringCellValue();
    }


    @Override
    protected String getIdentifier(Row row) {
        int columnNumber = 1;
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return row.getCell(columnNumber).getStringCellValue();
        }
        return null;
    }

    @Override
    protected int getNumberOfThreads() {
        return numberOfThreads;
    }

    @Override
    protected int getNumberOfCallablesPerThread() {
        return numberOfCallablesPerThread;
    }

    @Override
    protected int getNumberOfRowsPerTask() {
        return numberOfRowsPerTsk;
    }
}
