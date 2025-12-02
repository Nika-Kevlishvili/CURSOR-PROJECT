package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.address.PopulatedPlace;
import bg.energo.phoenix.model.entity.nomenclature.pod.MeasurementType;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import bg.energo.phoenix.model.enums.pod.pod.PODConsumptionPurposes;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.pod.pod.PODType;
import bg.energo.phoenix.model.enums.pod.pod.PODVoltageLevels;
import bg.energo.phoenix.model.request.pod.pod.*;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.address.*;
import bg.energo.phoenix.repository.nomenclature.pod.BalancingGroupCoordinatorsRepository;
import bg.energo.phoenix.repository.nomenclature.pod.MeasurementTypeRepository;
import bg.energo.phoenix.repository.nomenclature.pod.PodAdditionalParametersRepository;
import bg.energo.phoenix.repository.nomenclature.pod.UserTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static java.time.LocalDate.from;

@Service
@RequiredArgsConstructor
@Slf4j
public class PodExcelMapper {
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final BalancingGroupCoordinatorsRepository balancingGroupCoordinatorsRepository;
    private final UserTypeRepository userTypeRepository;
    private final CountryRepository countryRepository;
    private final RegionRepository regionRepository;
    private final MunicipalityRepository municipalityRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final ZipCodeRepository zipCodeRepository;
    private final DistrictRepository districtRepository;
    private final ResidentialAreaRepository residentialAreaRepository;
    private final StreetRepository streetRepository;
    private final CustomerRepository customerRepository;
    private final MeasurementTypeRepository measurementTypeRepository;
    private final PodAdditionalParametersRepository podAdditionalParametersRepository;

    public PodCreateRequest toPODCreateRequest(PodCreateRequest request, Row row, List<String> errorMessages) {
        request.setIdentifier(getStringValue(3, row));
        request.setAdditionalIdentifier(getStringValue(4, row));
        request.setName(getStringValue(5, row));

        Long gridOperatorId = getGridOperatorId(getStringValue(6, row), errorMessages);
        request.setGridOperatorId(gridOperatorId);

        Long balancingGroupCoordinator = getBalancingGroupCoordinatorId(getStringValue(7, row), errorMessages);
        request.setBalancingGroupCoordinatorId(balancingGroupCoordinator);

        request.setEstimatedMonthlyAvgConsumption(getIntegerValue(8, row));

        Long userTypeId = getUserTypeId(getStringValue(9, row), errorMessages);
        request.setUserTypeId(userTypeId);

        request.setCustomerIdentifierByGridOperator(getStringValue(10, row));
        request.setCustomerNumberByGridOperator(getStringValue(11, row));

        request.setType(PODType.valueOf(getStringValue(12, row)));
        request.setConsumptionPurpose(PODConsumptionPurposes.valueOf(getStringValue(13, row)));
        request.setVoltageLevel(PODVoltageLevels.valueOf(getStringValue(14, row)));
        if (PODMeasurementType.valueOf(getStringValue(15, row)).equals(PODMeasurementType.SETTLEMENT_PERIOD)) {
            request.setSettlementPeriod(true);
            request.setSlp(false);
        } else if (PODMeasurementType.valueOf(getStringValue(15, row)).equals(PODMeasurementType.SLP)) {
            request.setSlp(true);
            request.setSettlementPeriod(false);
        }

        Long measurementTypeId = getMeasurementTypeId(getStringValue(16, row), errorMessages);
        if (request.getSettlementPeriod() && measurementTypeId != null) {
            errorMessages.add("measurementTypeId-It is not possible to select a measurement type when Settlement Period is chosen.");
        } else if (request.getSlp() && measurementTypeId == null) {
            errorMessages.add("measurementTypeId-A measurement type is required when SLP is chosen.");
        }
        request.setMeasurementTypeId(measurementTypeId);

        request.setProvidedPower(getDecimalValue(17, row));
        request.setMultiplier(getDecimalValue(18, row));


        request.setAddressRequest(getPodAddressRequest(19, row, errorMessages));

        request.setImpossibleToDisconnect(Boolean.TRUE.equals(getBoolean(getStringValue(37, row), errorMessages, "impossibleToDisconnect")));
        request.setBlockedDisconnection(Boolean.TRUE.equals(getBoolean(getStringValue(38, row), errorMessages, "blockedDisconnection")));
        if (request.isBlockedDisconnection()) {
            request.setBlockedDisconnectionRequest(getBlockedDisconnection(39, row));
        }
        request.setBlockedBilling(Boolean.TRUE.equals(getBoolean(getStringValue(43, row), errorMessages, "blockedBilling")));
        if (request.isBlockedBilling()) {
            request.setBlockedBillingRequest(getBlockedBillingRequest(44, row));
        }

        request.setCustomerIdentifier(getStringValue(48, row));
        setPodAdditionalParameters(request, row, errorMessages);
        return request;
    }

    public PodUpdateRequest toPODUpdateRequest(PointOfDelivery pod, Row row, List<String> errorMessages) {
        PodUpdateRequest request = new PodUpdateRequest();
        Integer versionId = getIntegerValue(1, row);
        PointOfDeliveryDetails podDetails = new PointOfDeliveryDetails();
        if (versionId == null || versionId == 0) {
            Long lastDetailId = pod.getLastPodDetailId();
            Optional<PointOfDeliveryDetails> optionalPodDetails = pointOfDeliveryDetailsRepository.findById(lastDetailId);
            if (optionalPodDetails.isPresent())
                podDetails = optionalPodDetails.get();
            else
                errorMessages.add("detailId-detail id does not exist for pod with identifier: " + pod.getIdentifier() + " with detailId: " + lastDetailId + ";");
        } else {
            request.setVersionId(versionId);
            Optional<PointOfDeliveryDetails> optionalPodDetails = pointOfDeliveryDetailsRepository.findByPodIdAndVersionId(pod.getId(), versionId);
            if (optionalPodDetails.isPresent())
                podDetails = optionalPodDetails.get();
            else
                errorMessages.add("versionId-version id does not exist for pod with identifier: " + pod.getIdentifier() + " with versionId: " + versionId + ";");
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        if (Objects.equals(getStringValue(2, row), "E")) {
            request.setUpdateExistingVersion(true);
        } else if (Objects.equals(getStringValue(2, row), "C")) {
            request.setUpdateExistingVersion(false);
        }

        String additionalIdentifier = getStringValue(4, row);
        if (StringUtils.isBlank(additionalIdentifier))
            request.setAdditionalIdentifier(podDetails.getAdditionalIdentifier());
        else
            request.setAdditionalIdentifier(additionalIdentifier);

        String name = getStringValue(5, row);
        if (StringUtils.isBlank(name))
            request.setName(podDetails.getName());
        else
            request.setName(name);


        Long balancingGroupCoordinator = getBalancingGroupCoordinatorId(getStringValue(7, row), errorMessages);
        if (balancingGroupCoordinator == null)
            request.setBalancingGroupCoordinatorId(podDetails.getBalancingGroupCoordinatorId());
        else
            request.setBalancingGroupCoordinatorId(balancingGroupCoordinator);


        Integer estimatedMonthlyAvgConsumption = getIntegerValue(8, row);
        if (estimatedMonthlyAvgConsumption == null)
            request.setEstimatedMonthlyAvgConsumption(podDetails.getEstimatedMonthlyAvgConsumption());
        else
            request.setEstimatedMonthlyAvgConsumption(estimatedMonthlyAvgConsumption);


        Long userTypeId = getUserTypeId(getStringValue(9, row), errorMessages);
        if (userTypeId == null)
            request.setUserTypeId(podDetails.getUserTypeId());
        else
            request.setUserTypeId(userTypeId);

        String customerIdentifierByGridOperator = getStringValue(10, row);
        if (StringUtils.isBlank(customerIdentifierByGridOperator))
            request.setCustomerIdentifierByGridOperator(podDetails.getCustomerIdentifierByGridOperator());
        else
            request.setCustomerIdentifierByGridOperator(customerIdentifierByGridOperator);

        String customerNumberByGridOperator = getStringValue(11, row);
        if (StringUtils.isBlank(customerNumberByGridOperator))
            request.setCustomerNumberByGridOperator(podDetails.getCustomerNumberByGridOperator());
        else
            request.setCustomerNumberByGridOperator(customerNumberByGridOperator);

        String podType = getStringValue(12, row);
        if (StringUtils.isBlank(podType))
            request.setType(podDetails.getType());
        else
            request.setType(PODType.valueOf(podType));

        String consumptionPurpose = getStringValue(13, row);
        if (StringUtils.isBlank(consumptionPurpose))
            request.setConsumptionPurpose(podDetails.getConsumptionPurpose());
        else
            request.setConsumptionPurpose(PODConsumptionPurposes.valueOf(consumptionPurpose));

        String voltageLevel = getStringValue(14, row);
        if (StringUtils.isBlank(voltageLevel))
            request.setVoltageLevel(podDetails.getVoltageLevel());
        else
            request.setVoltageLevel(PODVoltageLevels.valueOf(voltageLevel));

        String measurementType = getStringValue(15, row);
        if (!StringUtils.isBlank(measurementType)) {
//            if (podDetails.getMeasurementType().equals(PODMeasurementType.SETTLEMENT_PERIOD)) {
//                request.setSettlementPeriod(true);
//                request.setSlp(false);
//            } else if (podDetails.getMeasurementType().equals(PODMeasurementType.SLP)) {
//                request.setSlp(true);
//                request.setSettlementPeriod(false);
//            } else
            if (PODMeasurementType.valueOf(measurementType).equals(PODMeasurementType.SETTLEMENT_PERIOD)) {
                request.setSettlementPeriod(true);
                request.setSlp(false);
            } else if (PODMeasurementType.valueOf(measurementType).equals(PODMeasurementType.SLP)) {
                request.setSlp(true);
                request.setSettlementPeriod(false);
            }
        }

        Long measurementTypeId = getMeasurementTypeId(getStringValue(16, row), errorMessages);
//        Boolean settlementPeriod = request.getSettlementPeriod();
//        if (settlementPeriod != null) {
        if (request.getSettlementPeriod() && measurementTypeId != null) {
            errorMessages.add("measurementTypeId-It is not possible to select a measurement type when Settlement Period is chosen.");
        } else if (request.getSlp() && measurementTypeId == null) {
            errorMessages.add("measurementTypeId-A measurement type is required when SLP is chosen.");
        }
//        }
//        if (measurementTypeId == null) {
//            measurementTypeId = podDetails.getPodMeasurementTypeId();
//        }
        request.setMeasurementTypeId(measurementTypeId);


        BigDecimal providedPower = getDecimalValue(17, row);
        if (providedPower == null)
            request.setProvidedPower(podDetails.getProvidedPower());
        else
            request.setProvidedPower(providedPower);
        BigDecimal multiplier = getDecimalValue(18, row);
        if (multiplier == null)
            request.setMultiplier(podDetails.getMultiplier());
        else
            request.setMultiplier(multiplier);


        request.setAddressRequest(getPodAddressRequestForUpdate(podDetails, 19, row, errorMessages));

        Boolean impossibleToDisconnect = getBoolean(getStringValue(37, row), errorMessages, "impossibleToDisconnect");
        if (impossibleToDisconnect == null)
            request.setImpossibleToDisconnect(pod.getImpossibleToDisconnect());
        else
            request.setImpossibleToDisconnect(Boolean.TRUE.equals(impossibleToDisconnect));

        Boolean blockedDisconnection = getBoolean(getStringValue(38, row), errorMessages, "blockedDisconnection");
        if (blockedDisconnection == null)
            request.setBlockedDisconnection(pod.getBlockedDisconnection());
        else
            request.setBlockedDisconnection(Boolean.TRUE.equals(blockedDisconnection));
        if (request.isBlockedDisconnection()) {
            request.setBlockedDisconnectionRequest(getBlockedDisconnectionForEditRequest(pod, 39, row));
        }


        Boolean blockedBilling = getBoolean(getStringValue(43, row), errorMessages, "blockedBilling");
        if (blockedBilling == null)
            request.setBlockedBilling(pod.getBlockedBilling());
        else
            request.setBlockedBilling(Boolean.TRUE.equals(blockedBilling));
        if (request.isBlockedBilling()) {
            request.setBlockedBillingRequest(getBlockedBillingRequestForEditRequest(pod, 44, row));
        }

        String customerIdentifier = getStringValue(48, row);
//        if (customerIdentifier == null) {
//            Customer customer = customerRepository.findById(podDetails.getCustomerId()).orElse(null);
//            if (customer == null)
//                errorMessages.add("Customer Not found with pod details customer id: " + podDetails.getCustomerId());
//            else
//                request.setCustomerNumberByGridOperator(customer.getIdentifier());
//        } else
        request.setCustomerIdentifier(customerIdentifier);

        setPodAdditionalParameters(request, row, errorMessages);

        return request;
    }

    private BlockedDisconnectionRequest getBlockedDisconnection(int columnNumber, Row row) {
        BlockedDisconnectionRequest result = new BlockedDisconnectionRequest();
        result.setFrom(getDateValue(columnNumber, row));
        result.setTo(getDateValue(columnNumber + 1, row));
        result.setReason(getStringValue(columnNumber + 2, row));
        result.setAdditionalInfo(getStringValue(columnNumber + 3, row));
        return result;
    }

    private BlockedDisconnectionRequest getBlockedDisconnectionForEditRequest(PointOfDelivery pod, int columnNumber, Row row) {
        BlockedDisconnectionRequest result = new BlockedDisconnectionRequest();
        result.setFrom(getDateValue(columnNumber, row) == null ? pod.getBlockedDisconnectionDateFrom() : getDateValue(columnNumber, row));
        result.setTo(getDateValue(columnNumber + 1, row) == null ? pod.getBlockedDisconnectionDateTo() : getDateValue(columnNumber + 1, row));
        result.setReason(getStringValue(columnNumber + 2, row) == null ? pod.getBlockedDisconnectionReason() : getStringValue(columnNumber + 2, row));
        result.setAdditionalInfo(getStringValue(columnNumber + 3, row) == null ? pod.getBlockedDisconnectionInfo() : getStringValue(columnNumber + 3, row));
        return result;
    }

    private BlockedBillingRequest getBlockedBillingRequest(int columnNumber, Row row) {
        BlockedBillingRequest result = new BlockedBillingRequest();
        result.setFrom(getDateValue(columnNumber, row));
        result.setTo(getDateValue(columnNumber + 1, row));
        result.setReason(getStringValue(columnNumber + 2, row));
        result.setAdditionalInfo(getStringValue(columnNumber + 3, row));
        return result;
    }

    private BlockedBillingRequest getBlockedBillingRequestForEditRequest(PointOfDelivery pod, int columnNumber, Row row) {
        BlockedBillingRequest result = new BlockedBillingRequest();
        result.setFrom(getDateValue(columnNumber, row) == null ? pod.getBlockedBillingDateFrom() : getDateValue(columnNumber, row));
        result.setTo(getDateValue(columnNumber + 1, row) == null ? pod.getBlockedBillingDateTo() : getDateValue(columnNumber + 1, row));
        result.setReason(getStringValue(columnNumber + 2, row) == null ? pod.getBlockedBillingReason() : getStringValue(columnNumber + 2, row));
        result.setAdditionalInfo(getStringValue(columnNumber + 3, row) == null ? pod.getBlockedBillingInfo() : getStringValue(columnNumber + 3, row));
        return result;
    }

    private PodAddressRequest getPodAddressRequest(int columnNumber, Row row, List<String> errorMessages) {
        PodAddressRequest addressRequest = new PodAddressRequest();
        Boolean isForeign = getBoolean(getStringValue(columnNumber, row), errorMessages, "foreign");
        addressRequest.setForeign(isForeign);

        if (Boolean.TRUE.equals(isForeign)) {
            addressRequest.setForeignAddressData(getForeignAddressData(columnNumber, row, errorMessages));
        } else {
            addressRequest.setLocalAddressData(getLocalAddressData(columnNumber, row, errorMessages));
        }

        addressRequest.setNumber(getStringValue(columnNumber + 11, row));
        addressRequest.setAdditionalInformation(getStringValue(columnNumber + 12, row));
        addressRequest.setBlock(getStringValue(columnNumber + 13, row));
        addressRequest.setEntrance(getStringValue(columnNumber + 14, row));
        addressRequest.setFloor(getStringValue(columnNumber + 15, row));
        addressRequest.setApartment(getStringValue(columnNumber + 16, row));
        addressRequest.setMailbox(getStringValue(columnNumber + 17, row));
        return addressRequest;
    }

    private PodAddressRequest getPodAddressRequestForUpdate(PointOfDeliveryDetails podDetails, int columnNumber, Row row, List<String> errorMessages) {
        PodAddressRequest addressRequest = new PodAddressRequest();
        Boolean isForeign = getBoolean(getStringValue(columnNumber, row), errorMessages, "foreign");
        if (isForeign == null)
            isForeign = podDetails.getForeignAddress();
        addressRequest.setForeign(isForeign);

        if (Boolean.TRUE.equals(isForeign)) {
            addressRequest.setForeignAddressData(getForeignAddressDataForUpdate(podDetails, columnNumber, row, errorMessages));
        } else {
            addressRequest.setLocalAddressData(getLocalAddressDataForUpdate(podDetails, columnNumber, row, errorMessages));
        }

        String number = getStringValue(columnNumber + 11, row);
        if (StringUtils.isBlank(number))
            addressRequest.setNumber(podDetails.getStreetNumber());
        else
            addressRequest.setNumber(number);

        String additionalInformation = getStringValue(columnNumber + 12, row);
        if (StringUtils.isBlank(additionalInformation))
            addressRequest.setAdditionalInformation(podDetails.getAddressAdditionalInfo());
        else
            addressRequest.setAdditionalInformation(additionalInformation);

        String block = getStringValue(columnNumber + 13, row);
        if (StringUtils.isBlank(block))
            addressRequest.setBlock(podDetails.getBlock());
        else
            addressRequest.setBlock(block);

        String entrance = getStringValue(columnNumber + 14, row);
        if (StringUtils.isBlank(entrance))
            addressRequest.setEntrance(podDetails.getEntrance());
        else
            addressRequest.setEntrance(entrance);

        String floor = getStringValue(columnNumber + 15, row);
        if (StringUtils.isBlank(floor))
            addressRequest.setFloor(podDetails.getFloor());
        else
            addressRequest.setFloor(floor);

        String apartment = getStringValue(columnNumber + 16, row);
        if (StringUtils.isBlank(apartment))
            addressRequest.setApartment(podDetails.getApartment());
        else
            addressRequest.setApartment(apartment);

        String mailBox = getStringValue(columnNumber + 17, row);
        if (StringUtils.isBlank(mailBox))
            addressRequest.setMailbox(podDetails.getMailbox());
        else
            addressRequest.setMailbox(mailBox);
        return addressRequest;
    }

    private PODLocalAddressData getLocalAddressData(int columnNumber, Row row, List<String> errorMessages) {
        PODLocalAddressData localAddressData = new PODLocalAddressData();
        Long countryId = geCountryId(getStringValue(columnNumber + 1, row), errorMessages);
        localAddressData.setCountryId(countryId);
        Long regionId = getRegionId(getStringValue(columnNumber + 2, row), countryId, errorMessages);
        localAddressData.setRegionId(regionId);
        Long municipalityId = getMunicipalityId(getStringValue(columnNumber + 3, row), regionId, errorMessages);
        localAddressData.setMunicipalityId(municipalityId);
        Long populatedPlaceId = getPopulatedPlaceId(getStringValue(columnNumber + 4, row), municipalityId, errorMessages);
        localAddressData.setPopulatedPlaceId(populatedPlaceId);
        Long zipCodeId = getZipCodeId(getStringValue(columnNumber + 5, row), populatedPlaceId, errorMessages);
        localAddressData.setZipCodeId(zipCodeId);
        Long districtId = getDistrictId(getStringValue(columnNumber + 6, row), populatedPlaceId, errorMessages);
        localAddressData.setDistrictId(districtId);
        Long residentialAreaId = getResidentialAreaId(getStringValue(columnNumber + 7, row), populatedPlaceId, errorMessages);
        localAddressData.setResidentialAreaId(residentialAreaId);
        String residentialAreaType = getStringValue(columnNumber + 8, row);
        if (!StringUtils.isEmpty(residentialAreaType)) {
            localAddressData.setResidentialAreaType(ResidentialAreaType.valueOf(residentialAreaType));
        }
        Long streetId = getStreetId(getStringValue(columnNumber + 9, row), populatedPlaceId, errorMessages);
        localAddressData.setStreetId(streetId);
        String streetType = getStringValue(columnNumber + 10, row);
        if (!StringUtils.isEmpty(streetType)) {
            localAddressData.setStreetType(StreetType.valueOf(streetType));
        }
        return localAddressData;
    }

    private PODLocalAddressData getLocalAddressDataForUpdate(PointOfDeliveryDetails podDetails, int columnNumber, Row row, List<String> errorMessages) {
        PODLocalAddressData localAddressData = new PODLocalAddressData();

        PopulatedPlace populatedPlaceWithDetails = null;
        if (podDetails.getPopulatedPlaceId() != null)
            populatedPlaceWithDetails = populatedPlaceRepository.findById(podDetails.getPopulatedPlaceId()).orElse(null);
        Long countryId = geCountryId(getStringValue(columnNumber + 1, row), errorMessages);
        if (countryId == null)
            countryId = podDetails.getCountryId();
        localAddressData.setCountryId(countryId);
        Long regionId = getRegionId(getStringValue(columnNumber + 2, row), countryId, errorMessages);
        if (regionId == null && populatedPlaceWithDetails != null)
            regionId = populatedPlaceWithDetails.getMunicipality().getRegion().getId();
        localAddressData.setRegionId(regionId);

        Long municipalityId = getMunicipalityId(getStringValue(columnNumber + 3, row), regionId, errorMessages);
        if (municipalityId == null && populatedPlaceWithDetails != null)
            municipalityId = populatedPlaceWithDetails.getMunicipality().getId();
        localAddressData.setMunicipalityId(municipalityId);

        Long populatedPlaceId = getPopulatedPlaceId(getStringValue(columnNumber + 4, row), municipalityId, errorMessages);
        if (populatedPlaceId == null)
            populatedPlaceId = podDetails.getPopulatedPlaceId();
        localAddressData.setPopulatedPlaceId(populatedPlaceId);
        Long zipCodeId = getZipCodeId(getStringValue(columnNumber + 5, row), populatedPlaceId, errorMessages);
        if (zipCodeId == null)
            zipCodeId = podDetails.getZipCodeId();
        localAddressData.setZipCodeId(zipCodeId);
        Long districtId = getDistrictId(getStringValue(columnNumber + 6, row), populatedPlaceId, errorMessages);
        if (districtId == null)
            districtId = podDetails.getDistrictId();
        localAddressData.setDistrictId(districtId);
        Long residentialAreaId = getResidentialAreaId(getStringValue(columnNumber + 7, row), populatedPlaceId, errorMessages);
        if (residentialAreaId == null)
            residentialAreaId = podDetails.getResidentialAreaId();
        localAddressData.setResidentialAreaId(residentialAreaId);
        String residentialAreaType = getStringValue(columnNumber + 8, row);
        if (!StringUtils.isEmpty(residentialAreaType)) {
            localAddressData.setResidentialAreaType(ResidentialAreaType.valueOf(residentialAreaType));
        }
        Long streetId = getStreetId(getStringValue(columnNumber + 9, row), populatedPlaceId, errorMessages);
        if (streetId == null)
            streetId = podDetails.getStreetId();
        localAddressData.setStreetId(streetId);
        String streetType = getStringValue(columnNumber + 10, row);
        if (!StringUtils.isEmpty(streetType)) {
            localAddressData.setStreetType(StreetType.valueOf(streetType));
        }
        return localAddressData;
    }

    private PODForeignAddressData getForeignAddressData(int columnNumber, Row row, List<String> errorMessages) {
        PODForeignAddressData foreignAddressData = new PODForeignAddressData();
        Long countryId = geCountryId(getStringValue(columnNumber + 1, row), errorMessages);
        foreignAddressData.setCountryId(countryId);
        foreignAddressData.setRegion(getStringValue(columnNumber + 2, row));
        foreignAddressData.setMunicipality(getStringValue(columnNumber + 3, row));
        foreignAddressData.setPopulatedPlace(getStringValue(columnNumber + 4, row));
        foreignAddressData.setZipCode(getStringValue(columnNumber + 5, row));
        foreignAddressData.setDistrict(getStringValue(columnNumber + 6, row));
        foreignAddressData.setResidentialArea(getStringValue(columnNumber + 7, row));
        String residentialAreaType = getStringValue(columnNumber + 8, row);
        if (!StringUtils.isEmpty(residentialAreaType)) {
            foreignAddressData.setResidentialAreaType(ResidentialAreaType.valueOf(residentialAreaType));
        }
        foreignAddressData.setStreet(getStringValue(columnNumber + 9, row));
        String streetType = getStringValue(columnNumber + 10, row);
        if (!StringUtils.isEmpty(streetType)) {
            foreignAddressData.setStreetType(StreetType.valueOf(streetType));
        }
        return foreignAddressData;
    }

    private PODForeignAddressData getForeignAddressDataForUpdate(PointOfDeliveryDetails podDetails, int columnNumber, Row row, List<String> errorMessages) {
        PODForeignAddressData foreignAddressData = new PODForeignAddressData();
        Long countryId = geCountryId(getStringValue(columnNumber + 1, row), errorMessages);
        if (countryId == null)
            countryId = podDetails.getCountryId();
        foreignAddressData.setCountryId(countryId);
        foreignAddressData.setRegion(StringUtils.isBlank(getStringValue(columnNumber + 2, row)) ? podDetails.getRegionForeign() : getStringValue(columnNumber + 2, row));
        foreignAddressData.setMunicipality(StringUtils.isBlank(getStringValue(columnNumber + 3, row)) ? podDetails.getMunicipalityForeign() : getStringValue(columnNumber + 3, row));
        foreignAddressData.setPopulatedPlace(StringUtils.isBlank(getStringValue(columnNumber + 4, row)) ? podDetails.getPopulatedPlaceForeign() : getStringValue(columnNumber + 4, row));
        foreignAddressData.setZipCode(StringUtils.isBlank(getStringValue(columnNumber + 5, row)) ? podDetails.getZipCodeForeign() : getStringValue(columnNumber + 5, row));
        foreignAddressData.setDistrict(StringUtils.isBlank(getStringValue(columnNumber + 6, row)) ? podDetails.getDistrictForeign() : getStringValue(columnNumber + 6, row));
        foreignAddressData.setResidentialArea(StringUtils.isBlank(getStringValue(columnNumber + 7, row)) ? podDetails.getResidentialAreaForeign() : getStringValue(columnNumber + 7, row));
        foreignAddressData.setResidentialAreaType(StringUtils.isBlank(getStringValue(columnNumber + 8, row)) ? podDetails.getForeignResidentialAreaType() : ResidentialAreaType.valueOf(getStringValue(columnNumber + 8, row)));
        foreignAddressData.setStreet(StringUtils.isBlank(getStringValue(columnNumber + 9, row)) ? podDetails.getStreetForeign() : getStringValue(columnNumber + 9, row));
        foreignAddressData.setStreetType(StringUtils.isBlank(getStringValue(columnNumber + 10, row)) ? podDetails.getForeignStreetType() : StreetType.valueOf(getStringValue(columnNumber + 10, row)));
        return foreignAddressData;
    }


    private Long geCountryId(String countryName, List<String> errorMessages) {
        Long result = null;
        if (countryName != null) {
            Optional<CacheObject> obj = countryRepository
                    .getCacheObjectByNameAndStatus(countryName, NomenclatureItemStatus.ACTIVE);
            if (obj.isPresent()) {
                result = obj.get().getId();
            } else {
                errorMessages.add("countryId-Not found country with name: " + countryName + ";");
            }
        }
        return result;
    }

    private Long getRegionId(String stringValue, Long parentId, List<String> errorMessages) {
        Long result = null;
        if (stringValue != null) {
            Optional<CacheObject> obj = regionRepository
                    .findByNameAndCountryId(stringValue, parentId, NomenclatureItemStatus.ACTIVE);
            if (obj.isPresent()) {
                result = obj.get().getId();
            } else {
                errorMessages.add("regionId-Not found region with name: " + stringValue + ";");
            }
        }
        return result;
    }

    private Long getMunicipalityId(String stringValue, Long parentId, List<String> errorMessages) {
        Long result = null;
        if (stringValue != null) {
            Optional<CacheObject> obj = municipalityRepository
                    .getByNameAndRegionId(stringValue, parentId, NomenclatureItemStatus.ACTIVE);
            if (obj.isPresent()) {
                result = obj.get().getId();
            } else {
                errorMessages.add("regionId-Not found municipality with name: " + stringValue + ";");
            }
        }
        return result;
    }

    private Long getPopulatedPlaceId(String stringValue, Long parentId, List<String> errorMessages) {
        Long result = null;
        if (stringValue != null) {
            Optional<CacheObject> obj = populatedPlaceRepository
                    .getByNameAndMunicipalityId(stringValue, parentId, NomenclatureItemStatus.ACTIVE);
            if (obj.isPresent()) {
                result = obj.get().getId();
            } else {
                errorMessages.add("populatedPlaceId-Not found populated Place with name: " + stringValue + ";");
            }
        }
        return result;
    }

    private Long getZipCodeId(String stringValue, Long parentId, List<String> errorMessages) {
        Long result = null;
        if (stringValue != null) {
            Optional<CacheObject> obj = zipCodeRepository
                    .getByNameAndPopulatedPlaceId(stringValue, parentId, NomenclatureItemStatus.ACTIVE);
            if (obj.isPresent()) {
                result = obj.get().getId();
            } else {
                errorMessages.add("zipCodeId-Not found zipCode with name: " + stringValue + ";");
            }
        }
        return result;
    }

    private Long getDistrictId(String stringValue, Long parentId, List<String> errorMessages) {
        Long result = null;
        if (stringValue != null) {
            Optional<CacheObject> obj = districtRepository
                    .getByNameAndPopulatedPlaceId(stringValue, parentId, NomenclatureItemStatus.ACTIVE);
            if (obj.isPresent()) {
                result = obj.get().getId();
            } else {
                errorMessages.add("districtId-Not found district with name: " + stringValue + ";");
            }
        }
        return result;
    }

    private Long getResidentialAreaId(String stringValue, Long parentId, List<String> errorMessages) {
        Long result = null;
        if (stringValue != null) {
            Optional<CacheObject> obj = residentialAreaRepository
                    .getByNameAndPopulatedPlaceId(stringValue, parentId, NomenclatureItemStatus.ACTIVE);
            if (obj.isPresent()) {
                result = obj.get().getId();
            } else {
                errorMessages.add("residentialAreaId-Not found Residential area with name: " + stringValue + ";");
            }
        }
        return result;
    }

    private Long getStreetId(String stringValue, Long parentId, List<String> errorMessages) {
        Long result = null;
        if (stringValue != null) {
            Optional<CacheObject> obj = streetRepository
                    .getByNameAndPopulatedPlaceId(stringValue, parentId, NomenclatureItemStatus.ACTIVE);
            if (obj.isPresent()) {
                result = obj.get().getId();
            } else {
                errorMessages.add("streetId-Not found street with name: " + stringValue + ";");
            }
        }
        return result;
    }

    private Long getGridOperatorId(String gridOperatorName, List<String> errorMessages) {
        Long result = null;
        if (gridOperatorName != null) {
            Optional<CacheObject> gridOperator = gridOperatorRepository
                    .getCacheObjectByNameAndStatus(gridOperatorName, NomenclatureItemStatus.ACTIVE);
            if (gridOperator.isPresent()) {
                result = gridOperator.get().getId();
            } else {
                errorMessages.add("gridOperatorId-Not found gridOperator with name: " + gridOperatorName + ";");
            }
        }
        return result;
    }

    private Long getMeasurementTypeId(String measurementTypeName, List<String> errorMessages) {
        Long result = null;

        if (measurementTypeName != null) {
            Optional<MeasurementType> measurementType = measurementTypeRepository
                    .findByNameAndStatus(measurementTypeName, NomenclatureItemStatus.ACTIVE);

            if (measurementType.isPresent()) {
                result = measurementType.get().getId();
            } else {
                errorMessages.add("measurementTypeId-Not found measurementType with name: " + measurementTypeName + ";");
            }
        }

        return result;
    }


    private Long getBalancingGroupCoordinatorId(String balancingGroupCoordinatorName, List<String> errorMessages) {
        Long result = null;
        if (balancingGroupCoordinatorName != null) {
            Optional<CacheObject> balancingGroupCoordinator = balancingGroupCoordinatorsRepository
                    .getCacheObjectByNameAndStatus(balancingGroupCoordinatorName, NomenclatureItemStatus.ACTIVE);
            if (balancingGroupCoordinator.isPresent()) {
                result = balancingGroupCoordinator.get().getId();
            } else {
                errorMessages.add("balancingGroupCoordinatorId-Not found balancing group coordinator with name: " + balancingGroupCoordinatorName + ";");
            }
        }
        return result;
    }

    private Long getUserTypeId(String userTypeName, List<String> errorMessages) {
        Long result = null;
        if (userTypeName != null) {
            Optional<CacheObject> userType = userTypeRepository
                    .getCacheObjectByNameAndStatus(userTypeName, NomenclatureItemStatus.ACTIVE);
            if (userType.isPresent()) {
                result = userType.get().getId();
            } else {
                errorMessages.add("userTypeId-Not found user type with name: " + userTypeName + ";");
            }
        }
        return result;
    }

    private Integer getIntegerValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.NUMERIC);
            return (int) row.getCell(columnNumber).getNumericCellValue();
        }
        return null;
    }

    private BigDecimal getDecimalValue(int columnNumber, Row row) {

        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {

            BigDecimal result;
            if (row.getCell(columnNumber).getCellType() == CellType.STRING) {
                result = BigDecimal.valueOf(Long.parseLong(row.getCell(columnNumber).getStringCellValue()));
            } else if (row.getCell(columnNumber).getCellType() == CellType.NUMERIC) {
                result = BigDecimal.valueOf(row.getCell(columnNumber).getNumericCellValue());
            } else {
                throw new ClientException("providedPower-Invalid cell type for provided power in row " + columnNumber + ";", ILLEGAL_ARGUMENTS_PROVIDED);
            }
            return result;
        } else {
            return null;
        }
    }

    private String getStringValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return row.getCell(columnNumber).getStringCellValue();
        }
        return null;
    }

    private Boolean getBoolean(String value, List<String> errorMessages, String fieldName) {
        if (value != null) {
            if (value.equalsIgnoreCase("YES")) return true;
            if (value.equalsIgnoreCase("NO")) return false;
            errorMessages.add(fieldName + "-Must be provided only YES or NO;");
        }
        return null;
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

    private void setPodAdditionalParameters(PodBaseRequest request, Row row, List<String> errorMessages) {
        Set<Long> podAdditionalParamIds = getPodAdditionalParamIds(49, row, errorMessages);
        if (!podAdditionalParamIds.isEmpty()) {
            request.setPodAdditionalParameters(podAdditionalParamIds);
        }
    }

    private Set<Long> getPodAdditionalParamIds(int columnNumber, Row row, List<String> errorMessages) {
        Set<Long> podAdditionalParamIds = new HashSet<>();
        addPodAdditionalParamId(columnNumber, row, podAdditionalParamIds, errorMessages);
        addPodAdditionalParamId(columnNumber + 1, row, podAdditionalParamIds, errorMessages);
        addPodAdditionalParamId(columnNumber + 2, row, podAdditionalParamIds, errorMessages);
        return podAdditionalParamIds;
    }

    private void addPodAdditionalParamId(int columnNumber, Row row, Set<Long> podAdditionalParamIds, List<String> errorMessages) {
        String podAdditionalParamName = getStringValue(columnNumber, row);
        if (podAdditionalParamName != null) {
            Optional<CacheObject> optionalPreferences = podAdditionalParametersRepository.findByNameAndStatus(podAdditionalParamName, NomenclatureItemStatus.ACTIVE);
            if (optionalPreferences.isPresent()) {
                podAdditionalParamIds.add(optionalPreferences.get().getId());
            } else errorMessages.add("preference-Not found preference with name: " + podAdditionalParamName + ";");
        }
    }
}
