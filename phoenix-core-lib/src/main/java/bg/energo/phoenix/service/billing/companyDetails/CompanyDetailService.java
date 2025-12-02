package bg.energo.phoenix.service.billing.companyDetails;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.*;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.billing.companyDetails.*;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyBankDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyCommunicationChannelDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyDetailedParameterDTO;
import bg.energo.phoenix.model.response.billing.CompanyDetailFileResponse;
import bg.energo.phoenix.model.response.billing.CompanyDetailResponse;
import bg.energo.phoenix.model.response.billing.CompanyDetailUpdateResponse;
import bg.energo.phoenix.repository.billing.companyDetails.*;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompanyDetailService {

    private final CompanyRepository companyRepository;
    private final CompanyDetailRepository companyDetailRepository;
    private final CompanyCommunicationAddressRepository companyCommunicationAddressRepository;
    private final CompanyBankRepository companyBankRepository;
    private final CompanyEmailRepository companyEmailRepository;
    private final CompanyInvoiceCompilerRepository companyInvoiceCompilerRepository;
    private final CompanyInvoiceIssuePlaceRepository companyInvoiceIssuePlaceRepository;
    private final CompanyManagerRepository companyManagerRepository;
    private final CompanyTelephoneRepository companyTelephoneRepository;
    private final CompanyLogoRepository companyLogoRepository;

    private final BankRepository bankRepository;

    private final CompanyDetailMapper companyDetailMapper;


    @Transactional
//    @CacheEvict(value = "companyDetailsForTemplateCache", allEntries = true)
    public Long createCompany(CreateCompanyRequest createCompanyRequest) {
        log.info("create company with identifier: %s ".formatted(createCompanyRequest.getUic()));
        List<String> exceptionMessages = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(companyRepository.findAll())) {
            throw new OperationNotAllowedException("Company with identifier %s already exists".formatted(createCompanyRequest.getUic()));
        }
        //there will be only one registered company
        Company company = new Company();
        //temporary
        company.setCompanyNumber(15L);
        Company savedCompany = companyRepository.saveAndFlush(company);
        CompanyDetails companyDetails = companyDetailMapper.mapToCompanyDetail(createCompanyRequest.getUic(),
                createCompanyRequest.getVatNumber(),
                createCompanyRequest.getNumberUnderExciseDutiesTaxWhAct(),
                createCompanyRequest.getName(),
                createCompanyRequest.getNameTranslated(),
                createCompanyRequest.getManagementAddress(),
                createCompanyRequest.getManagementAddressTranslated(),
                savedCompany.getId(),
                LocalDate.now());
        companyDetails.setVersionId(1L);
        CompanyDetails savedCompanyDetails = companyDetailRepository.saveAndFlush(companyDetails);
        saveNewEntities(savedCompanyDetails, exceptionMessages, createCompanyRequest.getBankList(),
                createCompanyRequest.getEmailList(), createCompanyRequest.getTelephoneList(),
                createCompanyRequest.getCompanyInvoiceCompilerList(),
                createCompanyRequest.getCompanyCommunicationAddressList(),
                createCompanyRequest.getCompanyInvoiceIssuePlaceList(),
                createCompanyRequest.getCompanyManagerList());
        validateCompanyLogo(createCompanyRequest.getLogoId(), savedCompanyDetails.getId(), exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        log.info("company saved successfully");
        return savedCompany.getId();
    }

    private void validateCompanyLogo(Long logoId, Long savedCompanyDetailId, List<String> exceptionMessages) {
        if (logoId != null) {
            if (companyLogoRepository.existsCompanyLogosByIdAndCompanyDetailIdAndStatus(logoId, savedCompanyDetailId, EntityStatus.ACTIVE)) {
                return;
            }
            Optional<CompanyLogos> companyLogosOptional = companyLogoRepository.findById(logoId);
            if (companyLogosOptional.isPresent()) {
                findRedundantCompanyFileData(savedCompanyDetailId, logoId);
                if (companyLogosOptional.get().getCompanyDetailId() != null) {
                    saveNewCompanyLogoEntity(savedCompanyDetailId, companyLogosOptional);
                } else {
                    updateExistedCompanyLogo(companyLogosOptional, savedCompanyDetailId);
                }
            } else {
                exceptionMessages.add("company.logoId-logo with id [%s] does not exist;".formatted(logoId));
            }
        } else {
            Optional<CompanyLogos> companyLogo = companyLogoRepository.findFirstByCompanyDetailIdAndStatus(savedCompanyDetailId, EntityStatus.ACTIVE);
            if (companyLogo.isPresent()) {
                companyLogo.get().setStatus(EntityStatus.DELETED);
                companyLogoRepository.save(companyLogo.get());
            }
        }
    }

    private void saveNewCompanyLogoEntity(Long savedCompanyDetailId, Optional<CompanyLogos> companyLogosOptional) {
        CompanyLogos companyLogo = new CompanyLogos();
        companyLogo.setName(companyLogosOptional.get().getName());
        companyLogo.setFileUrl(companyLogosOptional.get().getFileUrl());
        companyLogo.setCompanyDetailId(savedCompanyDetailId);
        companyLogo.setStatus(EntityStatus.ACTIVE);
        companyLogoRepository.save(companyLogo);
    }

    private void updateExistedCompanyLogo(Optional<CompanyLogos> companyLogosOptional, Long savedCompanyDetailId) {
        companyLogosOptional.get().setCompanyDetailId(savedCompanyDetailId);
        companyLogosOptional.get().setStatus(EntityStatus.ACTIVE);
        companyLogoRepository.save(companyLogosOptional.get());
    }

    private void findRedundantCompanyFileData(Long savedCompanyDetailId, Long logoId) {
        //company can have max one active file
        Optional<CompanyLogos> companyLogo = companyLogoRepository.findFirstByCompanyDetailIdAndStatus(savedCompanyDetailId, EntityStatus.ACTIVE);
        if (companyLogo.isPresent() && !companyLogo.get().getId().equals(logoId)) {
            companyLogo.get().setStatus(EntityStatus.DELETED);
            companyLogoRepository.save(companyLogo.get());
        }
    }

    @Transactional
//    @CacheEvict(value = "companyDetailsForTemplateCache", allEntries = true)
    public CompanyDetailUpdateResponse editCompanyDetail(Long companyId, EditCompanyRequest request, Boolean createNewVersion, Long versionId) {
        log.info("edit company with id: %s ".formatted(companyId));
        List<String> exceptionMessages = new ArrayList<>();
        if (!companyRepository.existsById(companyId)) {
            throw new OperationNotAllowedException("Company with id %s does not exists".formatted(companyId));
        }
        CompanyDetails companyDetails;
        if (!createNewVersion) {
            companyDetails = updateCurrentVersion(request, exceptionMessages, versionId);

        } else {
            companyDetails = createNewVersion(request, exceptionMessages, companyId);
        }
        validateCompanyLogo(request.getLogoId(), companyDetails.getId(), exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        return CompanyDetailUpdateResponse.toCompanyDetailUpdateResponse(companyDetails);
    }

    public CompanyDetailResponse getCompany(Long versionId, Long companyId) {
        if (companyId != null && !companyRepository.existsById(companyId)) {
            throw new OperationNotAllowedException("Company does not exist");
        }
        CompanyDetailResponse companyDetailResponse;
        if (Objects.isNull(versionId)) {
            Long lastVersionId = companyDetailRepository.findMaxVersionId().get();
            companyDetailResponse = getCompanyDetailByVersionId(lastVersionId);
        } else {
            companyDetailResponse = getCompanyDetailByVersionId(versionId);
        }
        return companyDetailResponse;
    }

    private CompanyDetailResponse getCompanyDetailByVersionId(Long versionId) {
        Optional<CompanyDetails> companyDetailsOptional = companyDetailRepository.findByVersionId(versionId);
        if (companyDetailsOptional.isEmpty()) {
            throw new OperationNotAllowedException("Company with version %s does not exist".formatted(versionId));
        }
        CompanyDetails companyDetail = companyDetailsOptional.get();
        List<CompanyCommunicationAddress> allByCompanyDetailIdAndStatus = companyCommunicationAddressRepository.findAllByCompanyDetailIdAndStatus(companyDetail.getId(), EntityStatus.ACTIVE);
        List<CompanyInvoiceCompiler> companyInvoiceCompilerList = companyInvoiceCompilerRepository.findAllByCompanyDetailIdAndStatus(companyDetail.getId(), EntityStatus.ACTIVE);
        List<CompanyInvoiceIssuePlace> companyInvoiceIssuePlaceList = companyInvoiceIssuePlaceRepository.findAllByCompanyDetailIdAndStatus(companyDetail.getId(), EntityStatus.ACTIVE);
        List<CompanyManager> companyManagerList = companyManagerRepository.findAllByCompanyDetailIdAndStatus(companyDetail.getId(), EntityStatus.ACTIVE);
        List<CompanyTelephone> companyTelephoneList = companyTelephoneRepository.findAllByCompanyDetailIdAndStatus(companyDetail.getId(), EntityStatus.ACTIVE);
        List<CompanyEmail> companyEmailList = companyEmailRepository.findAllByCompanyDetailIdAndStatus(companyDetail.getId(), EntityStatus.ACTIVE);
        List<CompanyBank> companyBankList = companyBankRepository.findAllByCompanyDetailIdAndStatus(companyDetail.getId(), EntityStatus.ACTIVE);
        Optional<CompanyLogos> companyLogo = companyLogoRepository.findFirstByCompanyDetailIdAndStatus(companyDetail.getId(), EntityStatus.ACTIVE);

        return CompanyDetailResponse.toCompanyDetailResponse(companyDetail, allByCompanyDetailIdAndStatus,
                companyManagerList, companyTelephoneList,
                companyEmailList, mapCompanyBanks(companyBankList),
                companyInvoiceIssuePlaceList, companyInvoiceCompilerList,
                companyDetailRepository.getVersions(),
                companyDetail.getCompanyId(),
                companyDetail.getVersionId(),
                companyLogo.map(CompanyDetailFileResponse::new).orElseGet(CompanyDetailFileResponse::new));
    }

    private List<CompanyBankDTO> mapCompanyBanks(List<CompanyBank> companyBankList) {
        List<CompanyBankDTO> mappedBankDTOList = new ArrayList<>();
        for (CompanyBank banks : companyBankList) {
            Optional<Bank> bankNomenclature = bankRepository.findById(banks.getBankId());
            bankNomenclature.ifPresent(bank -> mappedBankDTOList.add(CompanyBankDTO.toDTO(banks.getBankId(), bank.getBic(), banks.getIban(), banks.getId())));
        }
        return mappedBankDTOList;
    }

    private CompanyDetails updateCurrentVersion(EditCompanyRequest request, List<String> exceptionMessages, Long versionId) {

        if (companyDetailRepository.findByVersionId(versionId).isEmpty()) {
            throw new OperationNotAllowedException("Company with version %s does not exists".formatted(versionId));
        }
        Long companyDetailId = companyDetailRepository.findByVersionId(versionId).get().getId();
        log.info("update current version");
        CompanyDetails companyDetails = companyDetailMapper.fillCompanyDetail(request.getUic(),
                request.getVatNumber(),
                request.getNumberUnderExciseDutiesTaxWhAct(),
                request.getName(),
                request.getNameTranslated(),
                request.getManagementAddress(),
                request.getManagementAddressTranslated(),
                companyDetailRepository.findById(companyDetailId).get());

        CompanyDetails savedCompanyDetails = companyDetailRepository.saveAndFlush(companyDetails);
        validateBankForUpdate(request.getBankList(), companyDetails.getId(), exceptionMessages);
        validateEmails(request.getEmailList(), companyDetailId, exceptionMessages);
        validateTelephones(request.getTelephoneList(), companyDetailId, exceptionMessages);
        validateCompanyInvoiceCompilers(request.getCompanyInvoiceCompilerList(), companyDetailId, exceptionMessages);
        validateCompanyCommunicationAddresses(request.getCompanyCommunicationAddressList(), companyDetailId, exceptionMessages);
        validateCompanyInvoiceIssuePlaces(request.getCompanyInvoiceIssuePlaceList(), companyDetailId, exceptionMessages);
        validateCompanyManagers(request.getCompanyManagerList(), companyDetailId, exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        return savedCompanyDetails;
    }

    private CompanyDetails createNewVersion(EditCompanyRequest request, List<String> exceptionMessages, Long companyId) {
        validateVersionStartDate(request.getVersionStartDate());
        log.info("create new version");
        CompanyDetails companyDetails = companyDetailMapper.mapToCompanyDetail(request.getUic(),
                request.getVatNumber(),
                request.getNumberUnderExciseDutiesTaxWhAct(),
                request.getName(),
                request.getNameTranslated(),
                request.getManagementAddress(),
                request.getManagementAddressTranslated(),
                companyId,
                request.getVersionStartDate());
        companyDetails.setId(null);
        companyDetails.setVersionId(companyDetailRepository.findMaxVersionId().get() + 1);

        CompanyDetails savedCompanyDetails = companyDetailRepository.saveAndFlush(companyDetails);
        clearRequestSubObjectIds(request);
        saveNewEntities(savedCompanyDetails, exceptionMessages,
                companyDetailMapper.castToBaseCompanyBankDTOList(request.getBankList()),
                companyDetailMapper.castToBaseCompanyCommunicationChannelDTOList(request.getEmailList()),
                companyDetailMapper.castToBaseCompanyCommunicationChannelDTOList(request.getTelephoneList()),
                companyDetailMapper.castToBaseCompanyDetailedParameterDTOList(request.getCompanyInvoiceCompilerList()),
                companyDetailMapper.castToBaseCompanyDetailedParameterDTOList(request.getCompanyCommunicationAddressList()),
                companyDetailMapper.castToBaseCompanyDetailedParameterDTOList(request.getCompanyInvoiceIssuePlaceList()),
                companyDetailMapper.castToBaseCompanyDetailedParameterDTOList(request.getCompanyManagerList()));
        log.info("companyDetail new version saved successfully");
        return savedCompanyDetails;
    }

    private void clearRequestSubObjectIds(EditCompanyRequest request) {
        request.getBankList().forEach(companyBankDTO -> companyBankDTO.setId(null));
        request.getEmailList().forEach(companyCommunicationChannelDTO -> companyCommunicationChannelDTO.setId(null));
        request.getTelephoneList().forEach(companyCommunicationChannelDTO -> companyCommunicationChannelDTO.setId(null));
        request.getCompanyInvoiceCompilerList().forEach(companyDetailedParameterDTO -> companyDetailedParameterDTO.setId(null));
        request.getCompanyCommunicationAddressList().forEach(companyDetailedParameterDTO -> companyDetailedParameterDTO.setId(null));
        request.getCompanyInvoiceIssuePlaceList().forEach(companyDetailedParameterDTO -> companyDetailedParameterDTO.setId(null));
        request.getCompanyManagerList().forEach(companyDetailedParameterDTO -> companyDetailedParameterDTO.setId(null));
    }

    private void validateVersionStartDate(LocalDate versionStartDate) {
        if (versionStartDate == null) {
            log.error("versionStartDate-Version start date should be present when creating a new version of the company");
            throw new ClientException("versionStartDate-Version start date should be present when creating a new version of the company", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        Optional<CompanyDetails> companyDetailsOptional = companyDetailRepository.findByStartDate(versionStartDate);
        if (companyDetailsOptional.isPresent()) {
            log.error("versionStartDate-Version with the same start date already exists, version N [%s];".formatted(companyDetailsOptional.get().getVersionId()));
            throw new ClientException("versionStartDate-Version with the same start date already exists, version N [%s];".formatted(companyDetailsOptional.get().getVersionId()), ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    /**
     * for create new company entity
     * for create new version of companyDetails
     */
    private void saveNewEntities(CompanyDetails savedCompanyDetails, List<String> exceptionMessages, List<BaseCompanyBankDTO> bankList,
                                 List<BaseCompanyCommunicationChannelDTO> emailList,
                                 List<BaseCompanyCommunicationChannelDTO> telephoneList,
                                 List<BaseCompanyDetailedParameterDTO> companyInvoiceCompilerList,
                                 List<BaseCompanyDetailedParameterDTO> companyCommunicationAddressList,
                                 List<BaseCompanyDetailedParameterDTO> companyInvoiceIssuePlaceList,
                                 List<BaseCompanyDetailedParameterDTO> companyManagerList) {

        validateBank(bankList, savedCompanyDetails.getId(), exceptionMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        saveEmailList(emailList, savedCompanyDetails.getId());
        saveTelephoneList(telephoneList, savedCompanyDetails.getId());
        saveCompanyInvoiceCompilerList(companyInvoiceCompilerList, savedCompanyDetails.getId());
        saveCompanyCommunicationAddressList(companyCommunicationAddressList, savedCompanyDetails.getId());
        saveCompanyInvoiceIssuePlaceList(companyInvoiceIssuePlaceList, savedCompanyDetails.getId());
        saveCompanyManagerList(companyManagerList, savedCompanyDetails.getId());
    }

    /**
     * Use only for create
     */
    private void validateBank(List<BaseCompanyBankDTO> bankList, Long companyDetailId, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(bankList)) {
            for (int i = 0; i < bankList.size(); i++) {
                BaseCompanyBankDTO companyBankDto = bankList.get(i);
                if (bankRepository.findByIdAndStatus(companyBankDto.getBankId(), List.of(NomenclatureItemStatus.ACTIVE)).isPresent()) {
                    companyBankRepository.save(companyDetailMapper.mapToCompanyBank(companyBankDto.getBankId(), companyBankDto.getIban(), companyDetailId));
                } else {
                    exceptionMessages.add("company.bankList[%s].parameter-bank with id [%s] is not active;".formatted(i, companyBankDto.getBankId()));
                }
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void validateBankForUpdate(List<CompanyBankDTO> listFromRequest, Long companyDetailId, List<String> exceptionMessages) {
        List<CompanyBank> tempList = new ArrayList<>();
        List<CompanyBank> entitiesFromDb = companyBankRepository.findAllByCompanyDetailIdAndStatus(companyDetailId, EntityStatus.ACTIVE);
        if (CollectionUtils.isEmpty(listFromRequest) && CollectionUtils.isNotEmpty(entitiesFromDb)) {
            deactivateCompanyBanks(entitiesFromDb, tempList);
            return;
        }
        if (CollectionUtils.isNotEmpty(listFromRequest)) {
            List<Long> idsFromRequest = listFromRequest.stream().map(CompanyBankDTO::getId).toList();
            if (CollectionUtils.isNotEmpty(entitiesFromDb)) {
                findRedundantCompanyBanks(idsFromRequest, entitiesFromDb, tempList);
            }
            findEditedCompanyBanks(idsFromRequest, listFromRequest, exceptionMessages, companyDetailId, tempList, entitiesFromDb);
            findNewCompanyBanks(listFromRequest, companyDetailId, tempList, exceptionMessages);
            if (CollectionUtils.isNotEmpty(tempList)) {
                companyBankRepository.saveAll(tempList);
            }

        }

    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findRedundantCompanyBanks(List<Long> idsFromRequest, List<CompanyBank> entitiesFromDb, List<CompanyBank> tempList) {
        for (CompanyBank dbEntity : entitiesFromDb) {
            if (!idsFromRequest.contains(dbEntity.getId())) {
                dbEntity.setStatus(EntityStatus.DELETED);
                tempList.add(dbEntity);
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void deactivateCompanyBanks(List<CompanyBank> entitiesFromDb, List<CompanyBank> tempList) {
        for (CompanyBank dbEntity : entitiesFromDb) {
            dbEntity.setStatus(EntityStatus.DELETED);
            tempList.add(dbEntity);
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findEditedCompanyBanks(List<Long> idsFromRequest, List<CompanyBankDTO> listFromRequest, List<String> exceptionMessages,
                                        Long companyDetailId, List<CompanyBank> tempList, List<CompanyBank> entitiesFromDb) {
        for (int i = 0; i < idsFromRequest.size(); i++) {
            Long idFromRequest = idsFromRequest.get(i);
            if (idFromRequest != null) {
                Optional<CompanyBank> fromDb = companyBankRepository.findById(idFromRequest);
                if (fromDb.isPresent()) {
                    CompanyBankDTO companyBankDTO = listFromRequest
                            .stream()
                            .filter(obj -> obj.getId().equals(idFromRequest))
                            .findFirst().get();
                    tempList.add(companyDetailMapper.fillCompanyBank(companyBankDTO.getBankId(), companyBankDTO.getIban(), companyDetailId, fromDb.get()));
                } else {
                    exceptionMessages.add("company.emailList[%s].parameter-email with id [%s] does not exist;".formatted(i, idFromRequest));
                }
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findNewCompanyBanks(List<CompanyBankDTO> listFromRequest, Long companyDetailId,
                                     List<CompanyBank> tempList, List<String> exceptionMessages) {
        List<CompanyBankDTO> newObjectsList = listFromRequest.stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        if (CollectionUtils.isNotEmpty(newObjectsList)) {
            for (CompanyBankDTO object : newObjectsList) {
                if (bankRepository.findByIdAndStatus(object.getBankId(), List.of(NomenclatureItemStatus.ACTIVE)).isEmpty()) {
                    exceptionMessages.add("company.bankList.id-bank nomenclature with id [%s] is not active;".formatted(object.getBankId()));
                } else {
                    tempList.add(companyDetailMapper.mapToCompanyBank(object.getBankId(), object.getIban(), companyDetailId));
                }
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void validateEmails(List<CompanyCommunicationChannelDTO> listFromRequest, Long companyDetailId, List<String> exceptionMessages) {
        List<CompanyEmail> tempList = new ArrayList<>();
        List<Long> idsFromRequest = listFromRequest.stream().map(CompanyCommunicationChannelDTO::getId).toList();
        List<CompanyEmail> entitiesFromDb =
                companyEmailRepository.findAllByCompanyDetailIdAndStatus(companyDetailId, EntityStatus.ACTIVE);
        if (CollectionUtils.isNotEmpty(entitiesFromDb)) {
            findRedundantEmails(idsFromRequest, entitiesFromDb, tempList);
        }
        findEditedEmails(idsFromRequest, listFromRequest, exceptionMessages, companyDetailId, tempList);
        findNewEmails(listFromRequest, companyDetailId, tempList);
        if (CollectionUtils.isNotEmpty(tempList)) {
            companyEmailRepository.saveAll(tempList);
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findRedundantEmails(List<Long> idsFromRequest, List<CompanyEmail> entitiesFromDb, List<CompanyEmail> tempList) {
        for (CompanyEmail dbEntity : entitiesFromDb) {
            if (!idsFromRequest.contains(dbEntity.getId())) {
                dbEntity.setStatus(EntityStatus.DELETED);
                tempList.add(dbEntity);
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findEditedEmails(List<Long> idsFromRequest, List<CompanyCommunicationChannelDTO> listFromRequest,
                                  List<String> exceptionMessages, Long companyDetailId, List<CompanyEmail> tempList) {
        for (int i = 0; i < idsFromRequest.size(); i++) {
            Long idFromRequest = idsFromRequest.get(i);
            if (idFromRequest != null) {
                Optional<CompanyEmail> fromDb = companyEmailRepository.findById(idFromRequest);
                if (fromDb.isPresent()) {
                    CompanyCommunicationChannelDTO companyEmailDTO = listFromRequest
                            .stream()
                            .filter(obj -> obj.getId().equals(idFromRequest))
                            .findFirst().get();
                    tempList.add(companyDetailMapper.fillCompanyEmail(companyEmailDTO.getCommunicationChannel(),
                            companyDetailId, fromDb.get()));
                } else {
                    exceptionMessages.add("company.emailList[%s].id-email with id [%s] does not exist;".formatted(i, idFromRequest));
                }
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findNewEmails(List<CompanyCommunicationChannelDTO> listFromRequest, Long companyDetailId,
                               List<CompanyEmail> tempList) {
        List<CompanyCommunicationChannelDTO> newObjectsList = listFromRequest.stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        if (CollectionUtils.isNotEmpty(newObjectsList)) {
            tempList.addAll(newObjectsList
                    .stream()
                    .map(obj -> companyDetailMapper.mapToCompanyEmail(obj.getCommunicationChannel(), companyDetailId))
                    .toList());
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void validateTelephones(List<CompanyCommunicationChannelDTO> listFromRequest, Long companyDetailId, List<String> exceptionMessages) {
        List<CompanyTelephone> tempList = new ArrayList<>();
        List<Long> idsFromRequest = listFromRequest.stream().map(CompanyCommunicationChannelDTO::getId).toList();
        List<CompanyTelephone> entitiesFromDb =
                companyTelephoneRepository.findAllByCompanyDetailIdAndStatus(companyDetailId, EntityStatus.ACTIVE);
        if (CollectionUtils.isNotEmpty(entitiesFromDb)) {
            findRedundantTelephones(idsFromRequest, entitiesFromDb, tempList);
        }
        findEditedTelephones(idsFromRequest, listFromRequest, exceptionMessages, companyDetailId, tempList);
        findNewTelephones(listFromRequest, companyDetailId, tempList);
        if (CollectionUtils.isNotEmpty(tempList)) {
            companyTelephoneRepository.saveAll(tempList);
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findRedundantTelephones(List<Long> IdsFromRequest, List<CompanyTelephone> entitiesFromDb, List<CompanyTelephone> tempList) {
        for (CompanyTelephone dbEntity : entitiesFromDb) {
            if (!IdsFromRequest.contains(dbEntity.getId())) {
                dbEntity.setStatus(EntityStatus.DELETED);
                tempList.add(dbEntity);
            }
        }

    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findEditedTelephones(List<Long> idsFromRequest, List<CompanyCommunicationChannelDTO> listFromRequest,
                                      List<String> exceptionMessages, Long companyDetailId, List<CompanyTelephone> tempList) {
        for (int i = 0; i < idsFromRequest.size(); i++) {
            Long idFromRequest = idsFromRequest.get(i);
            if (idFromRequest != null) {
                Optional<CompanyTelephone> fromDb = companyTelephoneRepository.findById(idFromRequest);
                if (fromDb.isPresent()) {
                    CompanyCommunicationChannelDTO companyCommunicationChannelDTO = listFromRequest
                            .stream()
                            .filter(obj -> obj.getId().equals(idFromRequest))
                            .findFirst().get();
                    tempList.add(companyDetailMapper.fillCompanyTelephone(companyCommunicationChannelDTO.getCommunicationChannel(),
                            companyDetailId, fromDb.get()));
                } else {
                    exceptionMessages.add("company.telephoneList[%s].id-telephone with id [%s] does not exist;".formatted(i, idFromRequest));
                }
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findNewTelephones(List<CompanyCommunicationChannelDTO> listFromRequest, Long companyDetailId,
                                   List<CompanyTelephone> tempList) {
        List<CompanyCommunicationChannelDTO> newObjectsList = listFromRequest.stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        if (CollectionUtils.isNotEmpty(newObjectsList)) {
            tempList.addAll(newObjectsList
                    .stream()
                    .map(obj -> companyDetailMapper.mapToCompanyTelephone(obj.getCommunicationChannel(), companyDetailId))
                    .toList());
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void validateCompanyInvoiceCompilers(List<CompanyDetailedParameterDTO> listFromRequest, Long companyDetailId, List<String> exceptionMessages) {
        List<CompanyInvoiceCompiler> tempList = new ArrayList<>();
        List<Long> idsFromRequest = listFromRequest.stream().map(CompanyDetailedParameterDTO::getId).toList();
        List<CompanyInvoiceCompiler> entitiesFromDb = companyInvoiceCompilerRepository.findAllByCompanyDetailIdAndStatus(companyDetailId, EntityStatus.ACTIVE);
        if (CollectionUtils.isNotEmpty(entitiesFromDb)) {
            findRedundantCompanyInvoiceCompilers(idsFromRequest, entitiesFromDb, tempList);
        }
        findEditedCompanyInvoiceCompilers(idsFromRequest, listFromRequest, exceptionMessages, companyDetailId, tempList);
        findNewCompanyInvoiceCompilers(listFromRequest, companyDetailId, tempList);
        if (CollectionUtils.isNotEmpty(tempList)) {
            companyInvoiceCompilerRepository.saveAll(tempList);
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findRedundantCompanyInvoiceCompilers(List<Long> IdsFromRequest, List<CompanyInvoiceCompiler> entitiesFromDb, List<CompanyInvoiceCompiler> tempList) {
        for (CompanyInvoiceCompiler dbEntity : entitiesFromDb) {
            if (!IdsFromRequest.contains(dbEntity.getId())) {
                dbEntity.setStatus(EntityStatus.DELETED);
                tempList.add(dbEntity);
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findEditedCompanyInvoiceCompilers(List<Long> IdsFromRequest, List<CompanyDetailedParameterDTO> listFromRequest, List<String> exceptionMessages, Long companyDetailId, List<CompanyInvoiceCompiler> tempList) {
        for (int i = 0; i < IdsFromRequest.size(); i++) {
            Long idFromRequest = IdsFromRequest.get(i);
            if (idFromRequest != null) {
                Optional<CompanyInvoiceCompiler> fromDb = companyInvoiceCompilerRepository.findById(idFromRequest);
                if (fromDb.isPresent()) {
                    CompanyDetailedParameterDTO companyDetailedParameterDTO = listFromRequest
                            .stream()
                            .filter(obj -> obj.getId().equals(idFromRequest))
                            .findFirst().get();
                    tempList.add(companyDetailMapper.fillCompanyInvoiceCompiler(companyDetailedParameterDTO.getParameter(),
                            companyDetailedParameterDTO.getParameterTranslated(),
                            companyDetailId, fromDb.get()));
                } else {
                    exceptionMessages.add("company.companyInvoiceIssuePlaceList[%s].id-companyInvoiceIssuePlace with id [%s] does not exist;".formatted(i, idFromRequest));
                }
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findNewCompanyInvoiceCompilers(List<CompanyDetailedParameterDTO> listFromRequest, Long companyDetailId,
                                                List<CompanyInvoiceCompiler> tempList) {
        List<CompanyDetailedParameterDTO> newObjectsList = listFromRequest.stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        if (CollectionUtils.isNotEmpty(newObjectsList)) {
            tempList.addAll(newObjectsList
                    .stream()
                    .map(obj -> companyDetailMapper.mapToCompanyInvoiceCompiler(obj.getParameter(), obj.getParameterTranslated(), companyDetailId))
                    .toList());
        }
    }


    /**
     * Use for edit companyDetail (same version)
     */
    private void validateCompanyCommunicationAddresses(List<CompanyDetailedParameterDTO> listFromRequest, Long companyDetailId, List<String> exceptionMessages) {
        List<CompanyCommunicationAddress> tempList = new ArrayList<>();
        List<Long> idsFromRequest = listFromRequest.stream().map(CompanyDetailedParameterDTO::getId).toList();
        List<CompanyCommunicationAddress> entitiesFromDb = companyCommunicationAddressRepository.findAllByCompanyDetailIdAndStatus(companyDetailId, EntityStatus.ACTIVE);
        if (CollectionUtils.isNotEmpty(entitiesFromDb)) {
            findRedundantCompanyCommunicationAddresses(idsFromRequest, entitiesFromDb, tempList);
        }
        findEditedCompanyCommunicationAddresses(idsFromRequest, listFromRequest, exceptionMessages, companyDetailId, tempList);
        findNewCompanyCommunicationAddresses(listFromRequest, companyDetailId, tempList);
        if (CollectionUtils.isNotEmpty(tempList)) {
            companyCommunicationAddressRepository.saveAll(tempList);
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findRedundantCompanyCommunicationAddresses(List<Long> IdsFromRequest, List<CompanyCommunicationAddress> entitiesFromDb, List<CompanyCommunicationAddress> tempList) {
        for (CompanyCommunicationAddress dbEntity : entitiesFromDb) {
            if (!IdsFromRequest.contains(dbEntity.getId())) {
                dbEntity.setStatus(EntityStatus.DELETED);
                tempList.add(dbEntity);
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findEditedCompanyCommunicationAddresses(List<Long> IdsFromRequest, List<CompanyDetailedParameterDTO> listFromRequest, List<String> exceptionMessages, Long companyDetailId, List<CompanyCommunicationAddress> tempList) {
        for (int i = 0; i < IdsFromRequest.size(); i++) {
            Long idFromRequest = IdsFromRequest.get(i);
            if (idFromRequest != null) {
                Optional<CompanyCommunicationAddress> fromDb = companyCommunicationAddressRepository.findById(idFromRequest);
                if (fromDb.isPresent()) {
                    CompanyDetailedParameterDTO companyDetailedParameterDTO = listFromRequest
                            .stream()
                            .filter(obj -> obj.getId().equals(idFromRequest))
                            .findFirst().get();
                    tempList.add(companyDetailMapper.fillCompanyCommunicationAddress(companyDetailedParameterDTO.getParameter(),
                            companyDetailedParameterDTO.getParameterTranslated(),
                            companyDetailId, fromDb.get()));
                } else {
                    exceptionMessages.add("company.companyCommunicationAddressList[%s].id-CommunicationAddress with id [%s] does not exist;".formatted(i, idFromRequest));
                }
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findNewCompanyCommunicationAddresses(List<CompanyDetailedParameterDTO> listFromRequest, Long companyDetailId,
                                                      List<CompanyCommunicationAddress> tempList) {
        List<CompanyDetailedParameterDTO> newObjectsList = listFromRequest.stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        if (CollectionUtils.isNotEmpty(newObjectsList)) {
            tempList.addAll(newObjectsList
                    .stream()
                    .map(obj -> companyDetailMapper.mapToCompanyCommunicationAddress(obj.getParameter(), obj.getParameterTranslated(), companyDetailId))
                    .toList());
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void validateCompanyInvoiceIssuePlaces(List<CompanyDetailedParameterDTO> listFromRequest, Long companyDetailId, List<String> exceptionMessages) {
        List<CompanyInvoiceIssuePlace> tempList = new ArrayList<>();
        List<Long> idsFromRequest = listFromRequest.stream().map(CompanyDetailedParameterDTO::getId).toList();
        List<CompanyInvoiceIssuePlace> entitiesFromDb = companyInvoiceIssuePlaceRepository.findAllByCompanyDetailIdAndStatus(companyDetailId, EntityStatus.ACTIVE);
        if (CollectionUtils.isNotEmpty(entitiesFromDb)) {
            findRedundantCompanyInvoiceIssuePlaces(idsFromRequest, entitiesFromDb, tempList);
        }
        findEditedCompanyInvoiceIssuePlaces(idsFromRequest, listFromRequest, exceptionMessages, companyDetailId, tempList);
        findNewCompanyInvoiceIssuePlaces(listFromRequest, companyDetailId, tempList);
        if (CollectionUtils.isNotEmpty(tempList)) {
            companyInvoiceIssuePlaceRepository.saveAll(tempList);
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findRedundantCompanyInvoiceIssuePlaces(List<Long> IdsFromRequest, List<CompanyInvoiceIssuePlace> entitiesFromDb, List<CompanyInvoiceIssuePlace> tempList) {
        for (CompanyInvoiceIssuePlace dbEntity : entitiesFromDb) {
            if (!IdsFromRequest.contains(dbEntity.getId())) {
                dbEntity.setStatus(EntityStatus.DELETED);
                tempList.add(dbEntity);
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findEditedCompanyInvoiceIssuePlaces(List<Long> IdsFromRequest, List<CompanyDetailedParameterDTO> listFromRequest, List<String> exceptionMessages, Long companyDetailId, List<CompanyInvoiceIssuePlace> tempList) {
        for (int i = 0; i < IdsFromRequest.size(); i++) {
            Long idFromRequest = IdsFromRequest.get(i);
            if (idFromRequest != null) {
                Optional<CompanyInvoiceIssuePlace> fromDb = companyInvoiceIssuePlaceRepository.findById(idFromRequest);
                if (fromDb.isPresent()) {
                    CompanyDetailedParameterDTO companyDetailedParameterDTO = listFromRequest
                            .stream()
                            .filter(obj -> obj.getId().equals(idFromRequest))
                            .findFirst().get();
                    tempList.add(companyDetailMapper.fillCompanyInvoiceIssuePlace(companyDetailedParameterDTO.getParameter(), companyDetailedParameterDTO.getParameterTranslated(),
                            companyDetailId, fromDb.get()));
                } else {
                    exceptionMessages.add("company.companyInvoiceIssuePlaceList[%s].id-InvoiceIssuePlace with id [%s] does not exist;".formatted(i, idFromRequest));
                }
            }

        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findNewCompanyInvoiceIssuePlaces(List<CompanyDetailedParameterDTO> listFromRequest, Long companyDetailId,
                                                  List<CompanyInvoiceIssuePlace> tempList) {
        List<CompanyDetailedParameterDTO> newObjectsList = listFromRequest.stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        if (CollectionUtils.isNotEmpty(newObjectsList)) {
            tempList.addAll(newObjectsList
                    .stream()
                    .map(obj -> companyDetailMapper.mapToCompanyInvoiceIssuePlace(obj.getParameter(), obj.getParameterTranslated(), companyDetailId))
                    .toList());
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void validateCompanyManagers(List<CompanyDetailedParameterDTO> listFromRequest, Long companyDetailId, List<String> exceptionMessages) {
        List<CompanyManager> tempList = new ArrayList<>();
        List<Long> idsFromRequest = listFromRequest.stream().map(CompanyDetailedParameterDTO::getId).toList();
        List<CompanyManager> entitiesFromDb = companyManagerRepository.findAllByCompanyDetailIdAndStatus(companyDetailId, EntityStatus.ACTIVE);
        if (CollectionUtils.isNotEmpty(entitiesFromDb)) {
            findRedundantCompanyManagers(idsFromRequest, entitiesFromDb, tempList);
        }
        findEditedCompanyManagers(idsFromRequest, listFromRequest, exceptionMessages, companyDetailId, tempList);
        findNewCompanyManagers(listFromRequest, companyDetailId, tempList);
        if (CollectionUtils.isNotEmpty(tempList)) {
            companyManagerRepository.saveAll(tempList);
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findRedundantCompanyManagers(List<Long> IdsFromRequest, List<CompanyManager> entitiesFromDb, List<CompanyManager> tempList) {
        for (CompanyManager dbEntity : entitiesFromDb) {
            if (!IdsFromRequest.contains(dbEntity.getId())) {
                dbEntity.setStatus(EntityStatus.DELETED);
                tempList.add(dbEntity);
            }
        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findEditedCompanyManagers(List<Long> IdsFromRequest, List<CompanyDetailedParameterDTO> listFromRequest,
                                           List<String> exceptionMessages, Long companyDetailId, List<CompanyManager> tempList) {
        for (int i = 0; i < IdsFromRequest.size(); i++) {
            Long idFromRequest = IdsFromRequest.get(i);
            if (idFromRequest != null) {
                Optional<CompanyManager> fromDb = companyManagerRepository.findById(idFromRequest);
                if (fromDb.isPresent()) {
                    CompanyDetailedParameterDTO companyDetailedParameterDTO = listFromRequest
                            .stream()
                            .filter(obj -> obj.getId().equals(idFromRequest))
                            .findFirst().get();
                    tempList.add(companyDetailMapper.fillCompanyManager(companyDetailedParameterDTO.getParameter(), companyDetailedParameterDTO.getParameterTranslated(),
                            companyDetailId, fromDb.get()));
                } else {
                    exceptionMessages.add("company.companyManagerList[%s].id-manager with id [%s] does not exist;".formatted(i, idFromRequest));
                }
            }

        }
    }

    /**
     * Use for edit companyDetail (same version)
     */
    private void findNewCompanyManagers(List<CompanyDetailedParameterDTO> listFromRequest, Long companyDetailId,
                                        List<CompanyManager> tempList) {
        List<CompanyDetailedParameterDTO> newObjectsList = listFromRequest.stream()
                .filter(obj -> obj.getId() == null)
                .toList();
        if (CollectionUtils.isNotEmpty(newObjectsList)) {
            tempList.addAll(newObjectsList
                    .stream()
                    .map(obj -> companyDetailMapper.mapToCompanyManager(obj.getParameter(), obj.getParameterTranslated(), companyDetailId))
                    .toList());
        }
    }

    /**
     * for create new company entity
     * for create new version of companyDetails
     */
    private void saveEmailList(List<BaseCompanyCommunicationChannelDTO> emailList, Long companyDetailId) {
        companyEmailRepository.saveAll(
                emailList
                        .stream()
                        .map(companyEmail -> companyDetailMapper.mapToCompanyEmail(companyEmail.getCommunicationChannel(), companyDetailId))
                        .collect(Collectors.toList()));
    }

    /**
     * for create new company entity
     * for create new version of companyDetails
     */
    private void saveTelephoneList(List<BaseCompanyCommunicationChannelDTO> telephoneList, Long companyDetailId) {
        companyTelephoneRepository.saveAll(
                telephoneList
                        .stream()
                        .map(companyTelephone -> companyDetailMapper.mapToCompanyTelephone(companyTelephone.getCommunicationChannel(), companyDetailId))
                        .collect(Collectors.toList()));
    }

    /**
     * for create new company entity
     * for create new version of companyDetails
     */
    private void saveCompanyInvoiceCompilerList(List<BaseCompanyDetailedParameterDTO> invoiceCompilerList, Long companyDetailId) {
        companyInvoiceCompilerRepository.saveAll(
                invoiceCompilerList
                        .stream()
                        .map(companyInvoiceCompiler -> companyDetailMapper.mapToCompanyInvoiceCompiler(companyInvoiceCompiler.getParameter(), companyInvoiceCompiler.getParameterTranslated(), companyDetailId))
                        .collect(Collectors.toList()));
    }

    /**
     * for create new company entity
     * for create new version of companyDetails
     */
    private void saveCompanyCommunicationAddressList(List<BaseCompanyDetailedParameterDTO> communicationAddressList, Long companyDetailId) {
        companyCommunicationAddressRepository.saveAll(
                communicationAddressList
                        .stream()
                        .map(companyCommunicationAddress -> companyDetailMapper.mapToCompanyCommunicationAddress(companyCommunicationAddress.getParameter(), companyCommunicationAddress.getParameterTranslated(), companyDetailId))
                        .collect(Collectors.toList()));
    }

    /**
     * for create new company entity
     * for create new version of companyDetails
     */
    private void saveCompanyInvoiceIssuePlaceList(List<BaseCompanyDetailedParameterDTO> invoiceIssuePlaceList, Long companyDetailId) {
        companyInvoiceIssuePlaceRepository.saveAll(
                invoiceIssuePlaceList
                        .stream()
                        .map(companyInvoiceIssuePlace -> companyDetailMapper.mapToCompanyInvoiceIssuePlace(companyInvoiceIssuePlace.getParameter(), companyInvoiceIssuePlace.getParameterTranslated(), companyDetailId))
                        .collect(Collectors.toList()));
    }

    /**
     * for create new company entity
     * for create new version of companyDetails
     */
    private void saveCompanyManagerList(List<BaseCompanyDetailedParameterDTO> managerList, Long companyDetailId) {
        companyManagerRepository.saveAll(
                managerList
                        .stream()
                        .map(companyManager -> companyDetailMapper.mapToCompanyManager(companyManager.getParameter(), companyManager.getParameterTranslated(), companyDetailId))
                        .collect(Collectors.toList()));
    }

}
