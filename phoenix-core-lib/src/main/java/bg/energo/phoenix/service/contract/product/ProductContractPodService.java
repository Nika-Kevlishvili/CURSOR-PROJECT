package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.nomenclature.contract.DeactivationPurpose;
import bg.energo.phoenix.model.entity.nomenclature.pod.MeasurementType;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductGridOperator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.*;
import bg.energo.phoenix.model.enums.product.product.*;
import bg.energo.phoenix.model.request.contract.product.ContractPodRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractPointOfDeliveryRequest;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupShortResponse;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponse;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponseImpl;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractVersionShortDto;
import bg.energo.phoenix.model.response.nomenclature.pod.PodViewMeasurementType;
import bg.energo.phoenix.model.response.pod.pod.PointOfDeliveryView;
import bg.energo.phoenix.process.massImport.ExcelHelper;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.nomenclature.pod.MeasurementTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.service.contract.billing.BillingGroupService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductContractPodService {

    private final ContractPodRepository contractPodRepository;
    private final BillingGroupService billingGroupService;
    private final TemplateRepository templateRepository;
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final FileService fileService;
    private final ProductDetailsRepository productDetailsRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final MeasurementTypeRepository measurementTypeRepository;

    @Transactional
    public void addPodsToContract(List<Long> podDetailIds, Long productId, Long productVersionId, ProductContractDetails productContractDetails, List<String> errorMessages) {
        if (podDetailIds == null)
            return;
        Optional<ProductDetails> optionalProductDetails = productDetailsRepository.findByProductIdAndVersion(productId, productVersionId);
        ProductDetails productDetails;
        if (optionalProductDetails.isPresent()) {
            productDetails = optionalProductDetails.get();
        } else {
            return;
        }

        List<PurposeOfConsumption> productConsumptionPurposes = productDetails.getConsumptionPurposes() == null ? new ArrayList<>() : productDetails.getConsumptionPurposes();
        List<MeteringTypeOfThePointOfDelivery> productMeteringTypeOfThePointOfDeliveries = productDetails.getMeteringTypeOfThePointOfDeliveries() == null ? new ArrayList<>() : productDetails.getMeteringTypeOfThePointOfDeliveries();
        List<VoltageLevel> productVoltageLevels = productDetails.getMeteringTypeOfThePointOfDeliveries() == null ? new ArrayList<>() : productDetails.getVoltageLevels();
        List<ProductPodType> productPodTypes = productDetails.getProductPodTypes() == null ? new ArrayList<>() : productDetails.getProductPodTypes();
        List<Long> podDetailIdsToSaveOnContract = new ArrayList<>();
        Set<PODConsumptionPurposes> existingConsumptionPurposes = new HashSet<>();
        Set<Long> usedPods = new HashSet<>();
        for (int i = 0; i < podDetailIds.size(); i++) {
            Long podDetailId = podDetailIds.get(i);
            Optional<PointOfDeliveryDetails> podDetailsOptional = pointOfDeliveryDetailsRepository.findById(podDetailId);
            if (podDetailsOptional.isPresent()) {
                PointOfDeliveryDetails podDetails = podDetailsOptional.get();
                if (usedPods.contains(podDetails.getPodId())) {
                    errorMessages.add(String.format("podDetailIds[%s]-podDetail with id[%s] fails validation , using already used pod with id[%s]", i, podDetailId, podDetails.getPodId()));
                    continue;
                } else {
                    usedPods.add(podDetails.getPodId());
                }
                existingConsumptionPurposes.add(podDetails.getConsumptionPurpose());
                if (!productConsumptionPurposes.contains(convertFromPodValue(podDetails.getConsumptionPurpose()))
                        || !productMeteringTypeOfThePointOfDeliveries.contains(convertFromPodValue(podDetails.getMeasurementType()))
                        || !productVoltageLevels.contains(convertFromPodValue(podDetails.getVoltageLevel()))
                        || !productPodTypes.contains(convertFromPodValue(podDetails.getType()))) {

                    errorMessages.add(String.format("podDetailIds[%s]-podDetail with id[%s] does not meet product/contract requirements", i, podDetailId));
                    continue;
                }
                if (existingConsumptionPurposes.size() > 1) {
                    errorMessages.add(String.format("podDetailIds[%s]-podDetail with id[%s] fails validation, multiple consumption types", i, podDetailId));
                    continue;
                }
                if (BooleanUtils.isNotTrue(productDetails.getGlobalGridOperators())) {
                    if (productDetails.getGridOperator() != null) {
                        boolean gridOperatorExistsInProduct = false;
                        for (ProductGridOperator pgo : productDetails.getGridOperator()) {
                            PointOfDelivery pod = pointOfDeliveryRepository.findByIdAndStatusIn(podDetails.getPodId(), List.of(PodStatus.ACTIVE))
                                    .orElseThrow(() -> new DomainEntityNotFoundException(String.format("podId-Could not find pod with id[%s]", podDetails.getPodId())));
                            if (pgo.getGridOperator().getId().equals(pod.getGridOperatorId())) {
                                gridOperatorExistsInProduct = true;
                                break;
                            }
                        }
                        if (!gridOperatorExistsInProduct) {
                            errorMessages.add(String.format("podDetailIds[%s]-podDetail with id[%s] fails grid operator validation", i, podDetailId));
                            continue;
                        }
                    } else {
                        errorMessages.add(String.format("podDetailIds[%s]-podDetail with id[%s] fails grid operator validation", i, podDetailId));
                        continue;
                    }
                }
            } else {
                errorMessages.add(String.format("podDetailIds[%s]-podDetail with id[%s] does not exist", i, podDetailId));
                continue;
            }
            podDetailIdsToSaveOnContract.add(podDetailId);
        }

        if (!errorMessages.isEmpty())
            return;

        BillingGroupShortResponse billingGroupShortResponse = billingGroupService.createInitial(productContractDetails.getContractId());
        List<ContractPods> result = new ArrayList<>();
        for (Long id : podDetailIdsToSaveOnContract) {
            result.add(new ContractPods(billingGroupShortResponse.getId(), id, EntityStatus.ACTIVE, productContractDetails.getId()));
        }
        contractPodRepository.saveAll(result);
    }


    private PurposeOfConsumption convertFromPodValue(PODConsumptionPurposes consumptionPurpose) {

        return switch (consumptionPurpose) {
            case HOUSEHOLD -> PurposeOfConsumption.HOUSEHOLD;
            case NON_HOUSEHOLD -> PurposeOfConsumption.NON_HOUSEHOLD;
            default -> null;
        };
    }

    private MeteringTypeOfThePointOfDelivery convertFromPodValue(PODMeasurementType measurementType) {
        return switch (measurementType) {
            case SETTLEMENT_PERIOD -> MeteringTypeOfThePointOfDelivery.SETTLEMENT_PERIOD;
            case SLP -> MeteringTypeOfThePointOfDelivery.SLP;
            default -> null;
        };
    }

    private VoltageLevel convertFromPodValue(PODVoltageLevels voltageLevel) {
        return switch (voltageLevel) {
            case LOW -> VoltageLevel.LOW;
            case MEDIUM -> VoltageLevel.MEDIUM;
            case MEDIUM_DIRECT_CONNECTED -> VoltageLevel.MEDIUM_DIRECT_CONNECTED;
            case HIGH -> VoltageLevel.HIGH;
            default -> null;
        };
    }

    private ProductPodType convertFromPodValue(PODType podType) {
        return switch (podType) {
            case CONSUMER -> ProductPodType.CONSUMER;
            case GENERATOR -> ProductPodType.GENERATOR;
            default -> null;
        };
    }

    public List<ContractPodsResponse> getAllPods(Long customerId) {
        //TODO: add billing group status check to query
        return contractPodRepository.getAllContractPodsByCustomerId(customerId);
    }

    public List<ContractPodsResponseImpl> getAllPodsByCustomerDetailId(Long contractDetailId) {

        return contractPodRepository.getResponseByContractDetailIdAndStatusIn(contractDetailId, List.of(EntityStatus.ACTIVE));
    }

    public List<ContractPodsResponse> getActivePods(Long customerId, Long contractId) {
        //TODO: add billing group status check to queryÏ
        return contractPodRepository.getContractPodsByCustomerIdContractIdAndStatus(customerId, contractId, "ACTIVE");
    }

    public List<ContractPodsResponse> getConcretePodWithVersions(String identifier) {
        PointOfDelivery pod = pointOfDeliveryRepository.findByIdentifierAndStatus(identifier, PodStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("identifier-pod with identifier[%s] not found", identifier)));
        GridOperator gridOperator = gridOperatorRepository.findByIdAndStatus(pod.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE)).orElse(null);
        List<PointOfDeliveryDetails> podDetailsList = pointOfDeliveryDetailsRepository.findAllByPodIdOrderByVersionIdDesc(pod.getId());
        List<ContractPodsResponse> result = new ArrayList<>();
        for (PointOfDeliveryDetails podDetail : podDetailsList) {
            MeasurementType measurementType = measurementTypeRepository.findByIdAndStatus(podDetail.getPodMeasurementTypeId(), NomenclatureItemStatus.ACTIVE).orElse(null);
            PodViewMeasurementType podViewMeasurementType = new PodViewMeasurementType();
            if (measurementType != null) {
                podViewMeasurementType.setMeasurementTypeId(measurementType.getId());
                podViewMeasurementType.setMeasurementTypeName(measurementType.getName());
            }

            ContractPodsResponseImpl podDetailResult = new ContractPodsResponseImpl(
                    pod.getIdentifier(),
                    podDetail.getName(),
                    podDetail.getId(),
                    Long.parseLong(podDetail.getVersionId().toString()),
                    pod.getId(),
                    podDetail.getType(),
                    gridOperator == null ? null : gridOperator.getName(),
                    podDetail.getConsumptionPurpose(),
                    podDetail.getMeasurementType(),
                    null,
                    null,
                    podDetail.getCreateDate(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    podDetail.getEstimatedMonthlyAvgConsumption(),
                    podViewMeasurementType
            );
            result.add(podDetailResult);
        }
        return result;
    }

    public List<ContractPodsResponse> importFile(MultipartFile file, Long customerDetailId, Long contractId) {
        validateFileFormat(file);
//        validateFileContent(file, getTemplate());
        return mapAndGetResponseList(file, contractId);
    }

    private List<ContractPodsResponse> mapAndGetResponseList(MultipartFile file, Long contractId) {
        List<ContractPodsResponse> resultList = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.isXLS(file) ? new HSSFWorkbook(is) : new XSSFWorkbook(is)) {
            Sheet firstSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = firstSheet.iterator();
            if (iterator.hasNext()) iterator.next();
            Map<String, Integer> samePodDifferentVersionChecker = new HashMap<>();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                int exactRoNumber = row.getRowNum() + 1;
                String podIdentifier = getStringValue(0, row);
                if (podIdentifier == null)
                    throw new OperationNotAllowedException(String.format("identifier-identifier cannot be null on row[%s]", exactRoNumber));
                Integer podVersion = getIntegerValue(1, row);
                if (podVersion == null)
                    throw new OperationNotAllowedException(String.format("podVersion-podVersion cannot be null on row[%s]", exactRoNumber));
                BillingGroupListingResponse billingGroupResponseForMassImport;
                if (contractId != null) {
                    String billingGroupNumber = getStringValue(2, row);
                    if (billingGroupNumber == null || billingGroupNumber.equals("")) {
                        billingGroupResponseForMassImport = billingGroupService.getDefaultForContract(contractId);
                        if (billingGroupResponseForMassImport == null) {
                            throw new DomainEntityNotFoundException(String.format("contractId-could not find default billing group for contract with id[%s] on row[%s]", contractId, exactRoNumber));
                        }
                    } else {
                        Optional<ContractBillingGroup> contractBillingGroup = billingGroupService.findByContractIdGroupNumber(contractId, billingGroupNumber);
                        if (contractBillingGroup.isPresent()) {
                            billingGroupResponseForMassImport = new BillingGroupListingResponse(contractBillingGroup.get().getId(), contractBillingGroup.get().getGroupNumber());
                        } else {
                            throw new DomainEntityNotFoundException(String.format("billingGroup-could not find  billing group for billingGroup with name[%s] and contract with id[%s] on row[%s]", billingGroupNumber, contractId, exactRoNumber));
                        }
                    }

                } else {
                    billingGroupResponseForMassImport = null;
                }

                PointOfDelivery pod = pointOfDeliveryRepository.findByIdentifierAndStatus(podIdentifier, PodStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException(String.format("podIdentifier-Could not find pod with identifier[%s] on row[%s]", podIdentifier, exactRoNumber)));
                GridOperator gridOperator = gridOperatorRepository.findByIdAndStatus(pod.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE)).get();
                if (podVersion == 0)
                    podVersion = pointOfDeliveryDetailsRepository.findMaxVersionId(pod.getId())
                            .orElseThrow(() -> new DomainEntityNotFoundException(String.format("podIdentifier-Could not find pods version with identifier[%s] on row[%s]", podIdentifier, exactRoNumber)));

                if (samePodDifferentVersionChecker.containsKey(podIdentifier)) {
                    if (!podVersion.equals(samePodDifferentVersionChecker.get(podIdentifier))) {
                        throw new OperationNotAllowedException(String.format("identifier-more than one pod version found for identifier[%s] on row[%s]", podIdentifier, exactRoNumber));
                    } else {
                        continue;
                    }
                } else {
                    samePodDifferentVersionChecker.put(podIdentifier, podVersion);
                }
                Integer podFinalVersion = podVersion;
                PointOfDeliveryDetails podDetails = pointOfDeliveryDetailsRepository.findByPodIdAndVersionId(pod.getId(), podVersion)
                        .orElseThrow(() -> new DomainEntityNotFoundException(String.format("podVersion-Could not find pod Details with identifier[%s], version[%s] on row[%s]", podIdentifier, podFinalVersion, exactRoNumber)));
                Optional<ContractPods> contractPod = Optional.empty();
                Optional<DeactivationPurpose> deactivationPurpose = Optional.empty();
                //TODO: commented temporarily , Needs Tiko's recheck
//                if (billingGroupResponseForMassImport != null) {
//                    contractPod = contractPodRepository.findByBillingGroupIdAndPodDetailIdAndStatusIn(billingGroupResponseForMassImport.getId(), podDetails.getId(), List.of(EntityStatus.ACTIVE));
//                    if (contractPod.isPresent() && contractPod.get().getDeactivationPurposeId() != null) {
//                        deactivationPurpose = deactivationPurposeRepository.findByIdAndStatus(Long.parseLong(contractPod.get().getDeactivationPurposeId().toString()), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
//                    }
//                }
                MeasurementType measurementType = measurementTypeRepository.findByIdAndStatus(podDetails.getPodMeasurementTypeId(), NomenclatureItemStatus.ACTIVE).orElse(null);
                PodViewMeasurementType podViewMeasurementType = new PodViewMeasurementType();
                if (measurementType != null) {
                    podViewMeasurementType.setMeasurementTypeId(measurementType.getId());
                    podViewMeasurementType.setMeasurementTypeName(measurementType.getName());
                }

                ContractPodsResponseImpl result = new ContractPodsResponseImpl(
                        pod.getIdentifier(),
                        podDetails.getName(),
                        podDetails.getId(),
                        Long.parseLong(podDetails.getVersionId().toString()),
                        pod.getId(),
                        podDetails.getType(),
                        gridOperator.getName(),
                        podDetails.getConsumptionPurpose(),
                        podDetails.getMeasurementType(),
                        billingGroupResponseForMassImport == null ? null : billingGroupResponseForMassImport.getGroupNumber(),
                        null,
                        podDetails.getCreateDate(),
                        contractPod.isEmpty() ? null : contractPod.get().getActivationDate(),
                        contractPod.isEmpty() ? null : contractPod.get().getDeactivationDate(),
                        deactivationPurpose.isEmpty() ? null : deactivationPurpose.get().getName(),
                        contractPod.isEmpty() ? null : contractPod.get().getId(),
                        null,
                        billingGroupResponseForMassImport == null ? null : billingGroupResponseForMassImport.getId(),
                        podDetails.getEstimatedMonthlyAvgConsumption(),
                        podViewMeasurementType
                );
                resultList.add(result);
            }
        } catch (IOException e) {
            log.error("Error happened while reading import file content", e);
        }
        return resultList;
    }

    private Integer getIntegerValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.NUMERIC);
            return (int) row.getCell(columnNumber).getNumericCellValue();
        }
        return null;
    }

    private String getStringValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return row.getCell(columnNumber).getStringCellValue();
        }
        return null;
    }

    private void validateFileContent(MultipartFile file, byte[] template) {
        //TODO: will be implemented later
    }

    public void validateFileFormat(MultipartFile file) {
        if (!ExcelHelper.hasExcelFormat(file)) {
            log.error("File has invalid format");
            throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    public byte[] getTemplate() {
        try {
            var templatePath = templateRepository.findById("CONTRACT_POD_MASS_IMPORT").orElseThrow(() -> new DomainEntityNotFoundException("Unable to find template path"));
            log.info("template path ->>>> :" + templatePath.getFileUrl());
            return fileService.downloadFile(templatePath.getFileUrl()).getByteArray();
        } catch (Exception exception) {
            log.error("Could not fetch mass import template", exception);
            throw new ClientException("Could not fetch mass import template", APPLICATION_ERROR);
        }
    }

    @Transactional
    public void editProductContractPods(ProductContractDetails detailsUpdating, ProductContractDetails sourceDetails, List<ContractPodRequest> podRequests, Long productId, Long productVersionId, List<String> errorMessages) {
        Optional<ProductDetails> optionalProductDetails = productDetailsRepository.findByProductIdAndVersion(productId, productVersionId);
        ProductDetails productDetails;
        if (optionalProductDetails.isPresent()) {
            productDetails = optionalProductDetails.get();
        } else {
            return;
        }
        checkPodDuplication(podRequests, errorMessages);
        Set<PODConsumptionPurposes> existingConsumptionPurposes = new HashSet<>();
        Set<Long> usedPods = new HashSet<>();
        List<ContractPods> existingPodsByContract = contractPodRepository.findAllByContractDetailIdAndStatusIn(sourceDetails.getId(), List.of(EntityStatus.ACTIVE));

        Map<Long, ContractPods> existingPodsMap = existingPodsByContract.stream()
                .collect(Collectors.toMap(ContractPods::getPodDetailId, contractPod -> contractPod));

        List<PurposeOfConsumption> productConsumptionPurposes = productDetails.getConsumptionPurposes() == null ? new ArrayList<>() : productDetails.getConsumptionPurposes();
        List<VoltageLevel> productVoltageLevels = productDetails.getMeteringTypeOfThePointOfDeliveries() == null ? new ArrayList<>() : productDetails.getVoltageLevels();
        List<MeteringTypeOfThePointOfDelivery> productMeteringTypeOfThePointOfDeliveries = productDetails.getMeteringTypeOfThePointOfDeliveries() == null ? new ArrayList<>() : productDetails.getMeteringTypeOfThePointOfDeliveries();
        List<ProductPodType> productPodTypes = productDetails.getProductPodTypes() == null ? new ArrayList<>() : productDetails.getProductPodTypes();
        List<Long> currentPodIds = new ArrayList<>();

        List<ContractPods> result = new ArrayList<>();
        Map<Long, BigDecimal> amountToAddOnDetails = new HashMap<>();
        for (int i = 0; i < podRequests.size(); i++) {
            Long billingGroupId = podRequests.get(i).getBillingGroupId();
            Optional<ContractBillingGroup> billingGroupOptional = billingGroupService.findByContractIdAndId(detailsUpdating.getContractId(), billingGroupId);
            ContractBillingGroup billingGroup;
            if (billingGroupOptional.isPresent()) {
                billingGroup = billingGroupOptional.get();
            } else {
                errorMessages.add(String.format("podRequests[%s]-Billing group does not exist with id[%s] and contractId[%s]", i, billingGroupId, detailsUpdating.getContractId()));
                continue;
            }
            List<Long> podsInBillingGroup =
                    podRequests
                            .get(i)
                            .getProductContractPointOfDeliveries()
                            .stream()
                            .map(ProductContractPointOfDeliveryRequest::pointOfDeliveryDetailId)
                            .toList();

            for (int j = 0; j < podsInBillingGroup.size(); j++) {
                Long podDetailId = podRequests.get(i).
                        getProductContractPointOfDeliveries().get(j).pointOfDeliveryDetailId();
                Optional<PointOfDeliveryDetails> podDetailsOptional = pointOfDeliveryDetailsRepository.findById(podDetailId);
                PointOfDeliveryDetails podDetails;
                if (podDetailsOptional.isEmpty()) {
                    errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] does not exist", i, podDetailId));
                    continue;
                } else {
                    podDetails = podDetailsOptional.get();
                }


                if (existingPodsMap.containsKey(podDetailId)) {

                    if (!sourceDetails.getId().equals(detailsUpdating.getId())) {
                        ContractPods temp = existingPodsMap.get(podDetailId);
                        ContractPods contractPods = new ContractPods();
                        contractPods.setBillingGroupId(billingGroupId);
                        contractPods.setPodDetailId(temp.getPodDetailId());
                        contractPods.setContractDetailId(detailsUpdating.getId());
                        contractPods.setStatus(EntityStatus.ACTIVE);
                        result.add(contractPods);
                    } else {
                        ContractPods temp = existingPodsMap.get(podDetailId);
                        temp.setContractDetailId(detailsUpdating.getId());
                        temp.setBillingGroupId(billingGroupId);
                        result.add(temp);

                    }
                } else {
                    if (usedPods.contains(podDetails.getPodId())) {
                        errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] fails validation , using already used pod with id[%s]", i, podDetailId, podDetails.getPodId()));
                        continue;
                    } else {
                        usedPods.add(podDetails.getPodId());
                    }
                    existingConsumptionPurposes.add(podDetails.getConsumptionPurpose());
                    if (!productConsumptionPurposes.contains(convertFromPodValue(podDetails.getConsumptionPurpose()))
                            || !productMeteringTypeOfThePointOfDeliveries.contains(convertFromPodValue(podDetails.getMeasurementType()))
                            || !productVoltageLevels.contains(convertFromPodValue(podDetails.getVoltageLevel()))
                            || !productPodTypes.contains(convertFromPodValue(podDetails.getType()))) {

                        errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] fails validation", i, podDetailId));
                        continue;
                    }
                    if (existingConsumptionPurposes.size() > 1) {
                        errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] fails validation, multiple consumption types", i, podDetailId));
                        continue;
                    }
                    List<ProductGridOperator> gridOperators = productDetails.getGridOperator();
                    if (BooleanUtils.isNotTrue(productDetails.getGlobalGridOperators())) {
                        if (gridOperators != null) {
                            List<ProductGridOperator> validGridOperators = gridOperators.stream().filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)).toList();
                            boolean gridOperatorExistsInProduct = false;
                            for (ProductGridOperator pgo : validGridOperators) {
                                PointOfDelivery pod = pointOfDeliveryRepository.findByIdAndStatusIn(podDetails.getPodId(), List.of(PodStatus.ACTIVE))
                                        .orElseThrow(() -> new DomainEntityNotFoundException(String.format("pod-Could not find pod with id[%s]", podDetails.getPodId())));
                                if (pgo.getGridOperator().getId().equals(pod.getGridOperatorId())) {
                                    gridOperatorExistsInProduct = true;
                                    break;
                                }
                            }
                            if (!gridOperatorExistsInProduct) {
                                errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] fails grid operator validation", i, podDetailId));
                                continue;
                            }
                        } else {
                            errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] fails grid operator validation", i, podDetailId));
                            continue;
                        }
                    }
                    result.add(new ContractPods(billingGroup.getId(), podDetails.getId(), EntityStatus.ACTIVE, detailsUpdating.getId()));

                    currentPodIds.add(podDetails.getPodId());
                    //here we add same pods to future version of contract
                    List<Long> contractDetailIdsToAddPod = productContractDetailsRepository.getContractDetailIdsToAddPod(detailsUpdating.getId(), podDetailId);
                    for (Long contractDetailId : contractDetailIdsToAddPod) {
                        BigDecimal summedConsumption = BigDecimal.valueOf(podDetails.getEstimatedMonthlyAvgConsumption() * 12L).divide(BigDecimal.valueOf(1000), MathContext.UNLIMITED);
                        if (amountToAddOnDetails.containsKey(contractDetailId)) {
                            BigDecimal sum = amountToAddOnDetails.get(contractDetailId);
                            amountToAddOnDetails.replace(contractDetailId, sum.add(summedConsumption));
                        } else {
                            amountToAddOnDetails.put(contractDetailId, summedConsumption);
                        }
                        result.add(new ContractPods(billingGroup.getId(), podDetails.getId(), EntityStatus.ACTIVE, contractDetailId));
                    }
                }
            }
        }
        if (!errorMessages.isEmpty()) {
            return;
        }
        amountToAddOnDetails.forEach((key, value) -> {
            ProductContractDetails productContractDetails = productContractDetailsRepository.findById(key)
                    .orElseThrow(() -> new DomainEntityNotFoundException("podRequests-Something went wrong when adding estimated total consumption!;"));
            productContractDetails.setEstimatedTotalConsumptionUnderContractKwh(productContractDetails.getEstimatedTotalConsumptionUnderContractKwh().add(value));
        });

        Set<Long> existingPodsDetailsIdsSet = existingPodsByContract.stream().map(ContractPods::getPodDetailId)
                .collect(Collectors.toSet());
        Set<Long> newPodsDetailsIds = result.stream().map(ContractPods::getPodDetailId)
                .collect(Collectors.toSet());
        Set<Long> differenceSet = new HashSet<>(existingPodsDetailsIdsSet);

        // Use removeAll to remove elements from differenceSet that are also in set2
        differenceSet.removeAll(newPodsDetailsIds);

        for (Long podDetailIdToRemove : differenceSet) {
            Optional<ContractPods> contractPodsToRemoveOptional = contractPodRepository.findByContractDetailIdAndPodDetailIdAndStatusIn(detailsUpdating.getId(), podDetailIdToRemove, List.of(EntityStatus.ACTIVE));
            if (contractPodsToRemoveOptional.isPresent()) {
                ContractPods contractPodsToRemove = contractPodsToRemoveOptional.get();
                if (contractPodsToRemove.getActivationDate() != null) {
                    errorMessages.add(String.format("You can't remove POD which has activation date pod identifier[%s]", contractPodsToRemove.getPodDetailId()));
                    continue;
                }
                contractPodsToRemove.setStatus(EntityStatus.DELETED);
                result.add(contractPodsToRemove);
                List<ContractPods> contractPodsToDelete = contractPodRepository.getContractPodsToDelete(detailsUpdating.getId(), contractPodsToRemove.getPodDetailId());
                for (ContractPods cptd : contractPodsToDelete) {
                    cptd.setStatus(EntityStatus.DELETED);
                    result.add(cptd);
                }
            }
        }

        List<ContractPods> removedResult = removeExtraContractPods(result, detailsUpdating.getId());
        removedResult
                .stream()
                .filter(x -> !Objects.equals(x.getContractDetailId(), detailsUpdating.getId()) && x.getStatus().equals(EntityStatus.DELETED)).collect(Collectors.groupingBy(ContractPods::getContractDetailId))
                .forEach((key, value) -> {
                    List<Long> podDetails = value.stream().map(ContractPods::getPodDetailId).toList();
                    List<Integer> consumption = pointOfDeliveryDetailsRepository.findEstimatedMonthlyAvgConsumptionByIdIn(podDetails);
                    int sum = consumption.stream().mapToInt(Integer::intValue).sum();
                    BigDecimal summedConsumption = BigDecimal.valueOf(sum * 12L).divide(BigDecimal.valueOf(1000), MathContext.UNLIMITED);
                    ProductContractDetails details = productContractDetailsRepository.findById(key)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Product detail not found when updating consumptions!;"));
                    details.setEstimatedTotalConsumptionUnderContractKwh(details.getEstimatedTotalConsumptionUnderContractKwh().subtract(summedConsumption));
                });

        Map<Long, List<ContractPods>> filteredByStatusAndGroupedByContractDetail = removedResult.stream().filter(cp -> cp.getStatus() == EntityStatus.DELETED).collect(Collectors.groupingBy(ContractPods::getContractDetailId));
        Map<Long, List<ContractPods>> podsToAdd = removedResult.stream().filter(cp -> cp.getStatus() == EntityStatus.ACTIVE).collect(Collectors.groupingBy(ContractPods::getContractDetailId));
        for (Long productContractDetailId : filteredByStatusAndGroupedByContractDetail.keySet()) {
            ProductContractDetails productContractDetail = productContractDetailsRepository.findById(productContractDetailId).get();
            Long podsQuantityInContractDetail = contractPodRepository.countByContractDetailIdAndStatus(productContractDetailId, EntityStatus.ACTIVE);
            if (podsQuantityInContractDetail == 0 && !podsToAdd.containsKey(productContractDetailId)) {
                String podDetailIds = filteredByStatusAndGroupedByContractDetail.get(productContractDetailId).stream()
                        .map(cp -> cp.getPodDetailId().toString())
                        .collect(Collectors.joining(", "));
                errorMessages.add(String.format("POD details with ids[%s] can’t be removed, contract with id[%s] and version[%s] should include at least 1 POD", podDetailIds, productContractDetail.getContractId(), productContractDetail.getVersionId()));
            }
        }
        contractPodRepository.saveAll(removedResult);
    }

    @Transactional
    public void editProductContractPodsNew(ProductContractDetails detailsUpdating, ProductContractDetails sourceDetails, List<ContractPodRequest> podRequests, Long productId, Long productVersionId, List<String> errorMessages) {
        Optional<ProductDetails> optionalProductDetails = productDetailsRepository.findByProductIdAndVersion(productId, productVersionId);
        ProductDetails productDetails;
        if (optionalProductDetails.isPresent()) {
            productDetails = optionalProductDetails.get();
        } else {
            return;
        }
        checkPodDuplication(podRequests, errorMessages);
        Set<PODConsumptionPurposes> existingConsumptionPurposes = new HashSet<>();
        Set<Long> usedPods = new HashSet<>();
        List<ContractPods> existingPodsByContract = contractPodRepository.findAllByContractDetailIdAndStatusIn(sourceDetails.getId(), List.of(EntityStatus.ACTIVE));

        Map<Long, ContractPods> existingPodsMap = existingPodsByContract.stream()
                .collect(Collectors.toMap(ContractPods::getPodDetailId, contractPod -> contractPod));

        List<PurposeOfConsumption> productConsumptionPurposes = productDetails.getConsumptionPurposes() == null ? new ArrayList<>() : productDetails.getConsumptionPurposes();
        List<VoltageLevel> productVoltageLevels = productDetails.getMeteringTypeOfThePointOfDeliveries() == null ? new ArrayList<>() : productDetails.getVoltageLevels();
        List<MeteringTypeOfThePointOfDelivery> productMeteringTypeOfThePointOfDeliveries = productDetails.getMeteringTypeOfThePointOfDeliveries() == null ? new ArrayList<>() : productDetails.getMeteringTypeOfThePointOfDeliveries();
        List<ProductPodType> productPodTypes = productDetails.getProductPodTypes() == null ? new ArrayList<>() : productDetails.getProductPodTypes();
        List<Long> currentPodIds = new ArrayList<>();

        List<ContractPods> result = new ArrayList<>();
        Map<Long, BigDecimal> amountToAddOnDetails = new HashMap<>();
        for (int i = 0; i < podRequests.size(); i++) {
            Long billingGroupId = podRequests.get(i).getBillingGroupId();
            Optional<ContractBillingGroup> billingGroupOptional = billingGroupService.findByContractIdAndId(detailsUpdating.getContractId(), billingGroupId);
            ContractBillingGroup billingGroup;
            if (billingGroupOptional.isPresent()) {
                billingGroup = billingGroupOptional.get();
            } else {
                errorMessages.add(String.format("podRequests[%s]-Billing group does not exist with id[%s] and contractId[%s]", i, billingGroupId, detailsUpdating.getContractId()));
                continue;
            }
            List<Long> podsInBillingGroup =
                    podRequests
                            .get(i)
                            .getProductContractPointOfDeliveries()
                            .stream()
                            .map(ProductContractPointOfDeliveryRequest::pointOfDeliveryDetailId)
                            .toList();

            for (int j = 0; j < podsInBillingGroup.size(); j++) {
                Long podDetailId = podRequests.get(i).
                        getProductContractPointOfDeliveries().get(j).pointOfDeliveryDetailId();
                Optional<PointOfDeliveryDetails> podDetailsOptional = pointOfDeliveryDetailsRepository.findById(podDetailId);
                PointOfDeliveryDetails podDetails;
                if (podDetailsOptional.isEmpty()) {
                    errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] does not exist", i, podDetailId));
                    continue;
                } else {
                    podDetails = podDetailsOptional.get();
                }


                if (existingPodsMap.containsKey(podDetailId)) {

                    if (!sourceDetails.getId().equals(detailsUpdating.getId())) {
                        ContractPods temp = existingPodsMap.get(podDetailId);
                        ContractPods contractPods = new ContractPods();
                        contractPods.setBillingGroupId(billingGroupId);
                        contractPods.setPodDetailId(temp.getPodDetailId());
                        contractPods.setContractDetailId(detailsUpdating.getId());
                        contractPods.setStatus(EntityStatus.ACTIVE);
                        result.add(contractPods);
                    } else {
                        ContractPods temp = existingPodsMap.get(podDetailId);
                        temp.setContractDetailId(detailsUpdating.getId());
                        temp.setBillingGroupId(billingGroupId);
                        result.add(temp);
                    }
                } else {
                    if (usedPods.contains(podDetails.getPodId())) {
                        errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] fails validation , using already used pod with id[%s]", i, podDetailId, podDetails.getPodId()));
                        continue;
                    } else {
                        usedPods.add(podDetails.getPodId());
                    }
                    existingConsumptionPurposes.add(podDetails.getConsumptionPurpose());
                    if (!productConsumptionPurposes.contains(convertFromPodValue(podDetails.getConsumptionPurpose()))
                            || !productMeteringTypeOfThePointOfDeliveries.contains(convertFromPodValue(podDetails.getMeasurementType()))
                            || !productVoltageLevels.contains(convertFromPodValue(podDetails.getVoltageLevel()))
                            || !productPodTypes.contains(convertFromPodValue(podDetails.getType()))) {

                        errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] fails validation", i, podDetailId));
                        continue;
                    }
                    if (existingConsumptionPurposes.size() > 1) {
                        errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] fails validation, multiple consumption types", i, podDetailId));
                        continue;
                    }
                    List<ProductGridOperator> gridOperators = productDetails.getGridOperator();
                    if (BooleanUtils.isNotTrue(productDetails.getGlobalGridOperators())) {
                        if (gridOperators != null) {
                            List<ProductGridOperator> validGridOperators = gridOperators.stream().filter(x -> x.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE)).toList();
                            boolean gridOperatorExistsInProduct = false;
                            for (ProductGridOperator pgo : validGridOperators) {
                                PointOfDelivery pod = pointOfDeliveryRepository.findByIdAndStatusIn(podDetails.getPodId(), List.of(PodStatus.ACTIVE))
                                        .orElseThrow(() -> new DomainEntityNotFoundException(String.format("pod-Could not find pod with id[%s]", podDetails.getPodId())));
                                if (pgo.getGridOperator().getId().equals(pod.getGridOperatorId())) {
                                    gridOperatorExistsInProduct = true;
                                    break;
                                }
                            }
                            if (!gridOperatorExistsInProduct) {
                                errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] fails grid operator validation", i, podDetailId));
                                continue;
                            }
                        } else {
                            errorMessages.add(String.format("podRequests[%s]-podDetail with id[%s] fails grid operator validation", i, podDetailId));
                            continue;
                        }
                    }
                    result.add(new ContractPods(billingGroup.getId(), podDetails.getId(), EntityStatus.ACTIVE, detailsUpdating.getId()));

                    currentPodIds.add(podDetails.getPodId());
                    //here we add same pods to future version of contract
//                    List<Long> contractDetailIdsToAddPod = productContractDetailsRepository.getContractDetailIdsToAddPod(detailsUpdating.getId(), podDetailId);
//                    for (Long contractDetailId : contractDetailIdsToAddPod) {
//                        BigDecimal summedConsumption = BigDecimal.valueOf(podDetails.getEstimatedMonthlyAvgConsumption() * 12L).divide(BigDecimal.valueOf(1000), MathContext.UNLIMITED);
//                        if (amountToAddOnDetails.containsKey(contractDetailId)) {
//                            BigDecimal sum = amountToAddOnDetails.get(contractDetailId);
//                            amountToAddOnDetails.replace(contractDetailId, sum.add(summedConsumption));
//                        } else {
//                            amountToAddOnDetails.put(contractDetailId, summedConsumption);
//                        }
//                        result.add(new ContractPods(billingGroup.getId(), podDetails.getId(), EntityStatus.ACTIVE, contractDetailId));
//                    }
//                }
                }
            }
            if (!errorMessages.isEmpty()) {
                return;
            }
            amountToAddOnDetails.forEach((key, value) -> {
                ProductContractDetails productContractDetails = productContractDetailsRepository.findById(key)
                        .orElseThrow(() -> new DomainEntityNotFoundException("podRequests-Something went wrong when adding estimated total consumption!;"));
                productContractDetails.setEstimatedTotalConsumptionUnderContractKwh(productContractDetails.getEstimatedTotalConsumptionUnderContractKwh().add(value));
            });

            Set<Long> existingPodsDetailsIdsSet = existingPodsByContract.stream().map(ContractPods::getPodDetailId)
                    .collect(Collectors.toSet());
            Set<Long> newPodsDetailsIds = result.stream().map(ContractPods::getPodDetailId)
                    .collect(Collectors.toSet());
            Set<Long> differenceSet = new HashSet<>(existingPodsDetailsIdsSet);

            // Use removeAll to remove elements from differenceSet that are also in set2
            differenceSet.removeAll(newPodsDetailsIds);

            for (Long podDetailIdToRemove : differenceSet) {
                Optional<ContractPods> contractPodsToRemoveOptional = contractPodRepository.findByContractDetailIdAndPodDetailIdAndStatusIn(detailsUpdating.getId(), podDetailIdToRemove, List.of(EntityStatus.ACTIVE));
                if (contractPodsToRemoveOptional.isPresent()) {
                    ContractPods contractPodsToRemove = contractPodsToRemoveOptional.get();
//                    if (contractPodsToRemove.getActivationDate() != null) {
//                        errorMessages.add(String.format("You can't remove POD which has activation date pod identifier[%s]", contractPodsToRemove.getPodDetailId()));
//                        continue;
//                    }
                    contractPodsToRemove.setStatus(EntityStatus.DELETED);
                    result.add(contractPodsToRemove);
//                    List<ContractPods> contractPodsToDelete = contractPodRepository.getContractPodsToDelete(detailsUpdating.getId(), contractPodsToRemove.getPodDetailId());
//                    for (ContractPods cptd : contractPodsToDelete) {
//                        cptd.setStatus(EntityStatus.DELETED);
//                        result.add(cptd);
//                    }
                }
            }

            List<ContractPods> removedResult = removeExtraContractPods(result, detailsUpdating.getId());
            removedResult.stream().filter(x -> !Objects.equals(x.getContractDetailId(), detailsUpdating.getId()) && x.getStatus().equals(EntityStatus.DELETED)).collect(Collectors.groupingBy(ContractPods::getContractDetailId))
                    .forEach((key, value) -> {
                        List<Long> podDetails = value.stream().map(ContractPods::getPodDetailId).toList();
                        List<Integer> consumption = pointOfDeliveryDetailsRepository.findEstimatedMonthlyAvgConsumptionByIdIn(podDetails);
                        int sum = consumption.stream().mapToInt(Integer::intValue).sum();
                        BigDecimal summedConsumption = BigDecimal.valueOf(sum * 12L).divide(BigDecimal.valueOf(1000), MathContext.UNLIMITED);
                        ProductContractDetails details = productContractDetailsRepository.findById(key)
                                .orElseThrow(() -> new DomainEntityNotFoundException("Product detail not found when updating consumptions!;"));
                        details.setEstimatedTotalConsumptionUnderContractKwh(details.getEstimatedTotalConsumptionUnderContractKwh().subtract(summedConsumption));
                    });

            Map<Long, List<ContractPods>> filteredByStatusAndGroupedByContractDetail = removedResult.stream().filter(cp -> cp.getStatus() == EntityStatus.DELETED).collect(Collectors.groupingBy(ContractPods::getContractDetailId));
            Map<Long, List<ContractPods>> podsToAdd = removedResult.stream().filter(cp -> cp.getStatus() == EntityStatus.ACTIVE).collect(Collectors.groupingBy(ContractPods::getContractDetailId));
            for (Long productContractDetailId : filteredByStatusAndGroupedByContractDetail.keySet()) {
                ProductContractDetails productContractDetail = productContractDetailsRepository.findById(productContractDetailId).get();
                Long podsQuantityInContractDetail = contractPodRepository.countByContractDetailIdAndStatus(productContractDetailId, EntityStatus.ACTIVE);
                if (podsQuantityInContractDetail == 0 && !podsToAdd.containsKey(productContractDetailId)) {
                    String podDetailIds = filteredByStatusAndGroupedByContractDetail.get(productContractDetailId).stream()
                            .map(cp -> cp.getPodDetailId().toString())
                            .collect(Collectors.joining(", "));
                    errorMessages.add(String.format("POD details with ids[%s] can’t be removed, contract with id[%s] and version[%s] should include at least 1 POD", podDetailIds, productContractDetail.getContractId(), productContractDetail.getVersionId()));
                }
            }
            contractPodRepository.saveAll(removedResult);
        }
    }

    private List<ContractPods> removeExtraContractPods(List<ContractPods> fullList, Long currentDetailId) {
        Set<Long> podDetailIdsToMaintain = new HashSet<>();
        for (ContractPods current : fullList) {
            if (Objects.equals(current.getContractDetailId(), currentDetailId)) {
                if (current.getStatus() == EntityStatus.ACTIVE) {
                    Long podId = pointOfDeliveryDetailsRepository.findById(current.getPodDetailId()).get().getPodId();
                    Set<Long> podsDetailIds = pointOfDeliveryDetailsRepository.findAllByPodIdOrderByVersionIdAsc(podId).stream().map(PointOfDeliveryDetails::getId).collect(Collectors.toSet());
                    podDetailIdsToMaintain.addAll(podsDetailIds);
                }
            }
        }
        List<ContractPods> returnResult = new ArrayList<>();
        for (ContractPods current : fullList) {
            if (Objects.equals(current.getContractDetailId(), currentDetailId)) {
                returnResult.add(current);
            } else {
                if (current.getStatus() == EntityStatus.DELETED) {
                    if (podDetailIdsToMaintain.contains(current.getPodDetailId())) {
                        current.setStatus(EntityStatus.ACTIVE);
                        returnResult.add(current);
                    } else {
                        returnResult.add(current);
                    }
                } else {
                    returnResult.add(current);
                }
            }
        }
        return returnResult;
    }


    private void checkPodDuplication(List<ContractPodRequest> podRequests, List<String> errorMessages) {
        Map<Long, List<PointOfDeliveryView>> collect = pointOfDeliveryDetailsRepository
                .findPodViewForDetails(
                        podRequests
                                .stream()
                                .flatMap(x ->
                                        x.getProductContractPointOfDeliveries()
                                                .stream()
                                                .map(ProductContractPointOfDeliveryRequest::pointOfDeliveryDetailId)
                                )
                                .toList())
                .stream()
                .collect(Collectors.groupingBy(PointOfDeliveryView::getPodId));
        collect.forEach((key, value) -> {
            if (value.size() > 1) {
                errorMessages.add("podRequests-Details with ids (%s) has same pod with id: %s".formatted(value, key));
            }
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void adjustPodActivation(ProductContractDetails currentContractVersion, ProductContractVersionShortDto contractVersion, boolean savingNewVersion, List<String> errorMessages) {
        Map<Long, ContractPods> currentVersionPods = contractPodRepository.findAllByContractDetailIdAndStatusIn(currentContractVersion.getId(), List.of(EntityStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ContractPods::getPodDetailId, j -> j));
        List<ContractPods> podsToSave = new ArrayList<>();

        List<ContractPods> podVersions = contractPodRepository.findAllByContractDetailIdAndStatusIn(contractVersion.getDetailsId(), List.of(EntityStatus.ACTIVE));
        //previous
        for (ContractPods podVersion : podVersions) {
            ContractPods contractPods = currentVersionPods.get(podVersion.getPodDetailId());
            //current
            if (contractPods == null) {
                if (savingNewVersion) {
                    LocalDate deactivationDate = podVersion.getDeactivationDate();
                    if (podVersion.getActivationDate() == null) {
                        continue;
                    }
                    if (!podVersion.getActivationDate().isBefore(currentContractVersion.getStartDate())) {
                        podVersion.setActivationDate(null);
                        podVersion.setDeactivationDate(null);
                        podVersion.setDeactivationPurposeId(null);
//                        errorMessages.add("POD with id: %s can not be activated in new version!;".formatted(podVersion.getPodDetailId()));
                    }
                    if (deactivationDate == null) {
                        podVersion.setCustomModifyDate(LocalDateTime.now());
                        podVersion.setDeactivationDate(currentContractVersion.getStartDate().minusDays(1));
                    }
//                    else if (!deactivationDate.isBefore(currentContractVersion.getStartDate())) {
//                        errorMessages.add("POD with id: %s can not be activated in new version!;".formatted(podVersion.getPodDetailId()));
//                    }
                }
                continue;
            }
            if (podVersion.getActivationDate() != null && podVersion.getDeactivationDate() == null) {
                if (!podVersion.getActivationDate().isBefore(currentContractVersion.getStartDate())) {
                    contractPods.setActivationDate(podVersion.getActivationDate());
                    podVersion.setActivationDate(null);
                    podVersion.setDeactivationDate(null);
                    podVersion.setDeactivationPurposeId(6L);
                } else {
                    podVersion.setDeactivationDate(currentContractVersion.getStartDate().minusDays(1));
                    contractPods.setActivationDate(currentContractVersion.getStartDate());
                    podVersion.setDeactivationPurposeId(6L);
                }
                podVersion.setCustomModifyDate(LocalDateTime.now());
                contractPods.setCustomModifyDate(LocalDateTime.now());
                podsToSave.add(contractPods);
                podsToSave.add(podVersion);
            } else if (podVersion.getActivationDate() != null
                    && !podVersion.getDeactivationDate().isBefore(currentContractVersion.getStartDate())
            ) {
                // hola back in the future happens here
                LocalDate calculateCurrentVersionDeactivationDate = calculateCurrentVersionDeactivationDate(podVersion, contractPods);
                if (podVersion.getActivationDate().isAfter(currentContractVersion.getStartDate())) {
                    //new current
                    contractPods.setActivationDate(podVersion.getActivationDate());
                    contractPods.setDeactivationDate(podVersion.getDeactivationDate());
                    contractPods.setDeactivationPurposeId(podVersion.getDeactivationPurposeId());
                    //previous
                    podVersion.setActivationDate(null);
                    podVersion.setDeactivationDate(null);
                    podVersion.setDeactivationPurposeId(6L);
                } else {
                    contractPods.setDeactivationDate(podVersion.getDeactivationDate());
                    podVersion.setDeactivationDate(currentContractVersion.getStartDate().minusDays(1));
                    contractPods.setActivationDate(currentContractVersion.getStartDate());
                    podVersion.setCustomModifyDate(LocalDateTime.now());
                    contractPods.setCustomModifyDate(LocalDateTime.now());
                    podsToSave.add(podVersion);
                    podsToSave.add(contractPods);
                }
            }
        }

        contractPodRepository.saveAll(podsToSave);
    }

    private LocalDate calculateCurrentVersionDeactivationDate(ContractPods podVersion, ContractPods contractPods) {
        if (podVersion.isActiveInPerpetuity()) {
            return podVersion.getDeactivationDate();
        }

        if (contractPods.getDeactivationDate() == null) {
            return null;
        } else {
            if (contractPods.getDeactivationDate().isAfter(podVersion.getDeactivationDate())) {
                return contractPods.getDeactivationDate();
            } else {
                return podVersion.getDeactivationDate();
            }
        }
    }

    public List<ContractPods> getPodsThatHaveActivationDate(Long contractDetailId, EntityStatus status) {
        return contractPodRepository.getPodsThatHaveActivationDate(contractDetailId, status);
    }

    public void saveAll(List<ContractPods> contractPods) {
        contractPodRepository.saveAll(contractPods);
    }
}
