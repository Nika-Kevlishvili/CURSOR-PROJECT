package bg.energo.phoenix.service.pod.pod;


import bg.energo.phoenix.model.entity.pod.pod.PodContractResponse;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.request.pod.pod.*;
import bg.energo.phoenix.model.response.pod.pod.BlockedBillingResponse;
import bg.energo.phoenix.model.response.pod.pod.BlockedDisconnectionResponse;
import bg.energo.phoenix.model.response.pod.pod.PointOfDeliveryResponse;

import static bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType.SETTLEMENT_PERIOD;
import static bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType.SLP;

public class PointOfDeliveryMapper {
    private PointOfDeliveryMapper() {

    }

    public static PointOfDelivery mapCreateRequestToPod(PodCreateRequest request) {
        PointOfDelivery pointOfDelivery = new PointOfDelivery();
        pointOfDelivery.setIdentifier(request.getIdentifier());
        pointOfDelivery.setStatus(PodStatus.ACTIVE);
        pointOfDelivery.setBlockedBilling(request.isBlockedBilling());
        pointOfDelivery.setBlockedDisconnection(request.isBlockedDisconnection());
        pointOfDelivery.setImpossibleToDisconnect(request.isImpossibleToDisconnect());
        if (request.isBlockedDisconnection()) {
            addDisconnection(request, pointOfDelivery);
        }
        if (request.isBlockedBilling()) {
            addBilling(request, pointOfDelivery);
        }
        pointOfDelivery.setGridOperatorId(request.getGridOperatorId());
        pointOfDelivery.setDisconnectionPowerSupply(false);
        return pointOfDelivery;
    }

    public static void addDisconnection(PodBaseRequest request, PointOfDelivery pointOfDelivery) {
        if (request.isBlockedDisconnection()) {
            BlockedDisconnectionRequest blockedDisconnectionRequest = request.getBlockedDisconnectionRequest();
            pointOfDelivery.setBlockedDisconnectionDateTo(blockedDisconnectionRequest.getTo());
            pointOfDelivery.setBlockedDisconnectionDateFrom(blockedDisconnectionRequest.getFrom());
            pointOfDelivery.setBlockedDisconnectionReason(blockedDisconnectionRequest.getReason());
            pointOfDelivery.setBlockedDisconnectionInfo(blockedDisconnectionRequest.getAdditionalInfo());
        }
    }

    public static void addBilling(PodBaseRequest request, PointOfDelivery pointOfDelivery) {
        if (request.isBlockedBilling()) {
            BlockedBillingRequest blockedBillingRequest = request.getBlockedBillingRequest();

            pointOfDelivery.setBlockedBillingDateTo(blockedBillingRequest.getTo());
            pointOfDelivery.setBlockedBillingDateFrom(blockedBillingRequest.getFrom());
            pointOfDelivery.setBlockedBillingReason(blockedBillingRequest.getReason());
            pointOfDelivery.setBlockedBillingInfo(blockedBillingRequest.getAdditionalInfo());
        }
    }

    public static PointOfDeliveryDetails mapCreateRequestToPodDetails(PodCreateRequest request, Long podId) {
        PointOfDeliveryDetails details = new PointOfDeliveryDetails();
        fillPointOfDeliveryDetails(request, details);
        details.setPodId(podId);
        details.setVersionId(1);
        return details;
    }


    public static PointOfDeliveryResponse createPointOfDeliveryResponse(PointOfDelivery pointOfDelivery, PointOfDeliveryDetails details) {
        PointOfDeliveryResponse response = new PointOfDeliveryResponse();
        response.setId(pointOfDelivery.getId());
        response.setVersionId(details.getVersionId());
        response.setIdentifier(pointOfDelivery.getIdentifier());
        response.setName(details.getName());
        response.setAdditionalIdentifier(details.getAdditionalIdentifier());
        response.setEstimatedMonthlyAvgConsumption(details.getEstimatedMonthlyAvgConsumption());
        response.setCustomerIdentifierByGridOperator(details.getCustomerIdentifierByGridOperator());
        response.setCustomerNumberByGridOperator(details.getCustomerNumberByGridOperator());
        response.setProvidedPower(details.getProvidedPower());
        response.setMultiplier(details.getMultiplier());
        response.setImpossibleToDisconnect(pointOfDelivery.getImpossibleToDisconnect());
        response.setType(details.getType());
        response.setConsumptionPurpose(details.getConsumptionPurpose());
        response.setVoltageLevel(details.getVoltageLevel());
        response.setMeasurementType(details.getMeasurementType());
        response.setStatus(pointOfDelivery.getStatus());
        response.setSettlementPeriod(details.getMeasurementType().equals(SETTLEMENT_PERIOD));
        response.setSlp(details.getMeasurementType().equals(SLP));

        Boolean blockedDisconnection = pointOfDelivery.getBlockedDisconnection();
        response.setBlockedDisconnection(blockedDisconnection);
        Boolean blockedBilling = pointOfDelivery.getBlockedBilling();
        response.setBlockedBilling(blockedBilling);
        if (Boolean.TRUE.equals(blockedDisconnection)) {
            BlockedDisconnectionResponse disconnectionResponse = new BlockedDisconnectionResponse();
            disconnectionResponse.setTo(pointOfDelivery.getBlockedDisconnectionDateTo());
            disconnectionResponse.setFrom(pointOfDelivery.getBlockedDisconnectionDateFrom());
            disconnectionResponse.setReason(pointOfDelivery.getBlockedDisconnectionReason());
            disconnectionResponse.setAdditionalInfo(pointOfDelivery.getBlockedDisconnectionInfo());
            response.setBlockedDisconnectionResponse(disconnectionResponse);
        }
        if (Boolean.TRUE.equals(blockedBilling)) {
            BlockedBillingResponse billingResponse = new BlockedBillingResponse();
            billingResponse.setTo(pointOfDelivery.getBlockedBillingDateTo());
            billingResponse.setFrom(pointOfDelivery.getBlockedBillingDateFrom());
            billingResponse.setReason(pointOfDelivery.getBlockedBillingReason());
            billingResponse.setAdditionalInfo(pointOfDelivery.getBlockedBillingInfo());
            response.setBlockedBillingResponse(billingResponse);
        }
        return response;
    }

    public static PodContractResponse createPointOfDeliveryContractResponse(PointOfDelivery pointOfDelivery, PointOfDeliveryDetails details) {
        PodContractResponse response = new PodContractResponse();
        response.setIdentifier(pointOfDelivery.getIdentifier());
        response.setName(details.getName());
        response.setEstimatedMonthlyAvgConsumption(details.getEstimatedMonthlyAvgConsumption());
        response.setProvidedPower(details.getProvidedPower());
        response.setMultiplier(details.getMultiplier());
        response.setType(details.getType());
        response.setConsumptionPurpose(details.getConsumptionPurpose());
        response.setVoltageLevel(details.getVoltageLevel());
        response.setMeasurementType(details.getMeasurementType());
        response.setConsumptionPurpose(details.getConsumptionPurpose());
        response.setSettlementPeriod(details.getMeasurementType().equals(SETTLEMENT_PERIOD));
        response.setSlp(details.getMeasurementType().equals(SLP));

        return response;
    }

    private static void fillPointOfDeliveryDetails(PodBaseRequest request, PointOfDeliveryDetails details) {
        details.setName(request.getName());
        details.setAdditionalIdentifier(request.getAdditionalIdentifier());
        details.setBalancingGroupCoordinatorId(request.getBalancingGroupCoordinatorId());
        details.setType(request.getType());
        details.setEstimatedMonthlyAvgConsumption(request.getEstimatedMonthlyAvgConsumption());
        details.setConsumptionPurpose(request.getConsumptionPurpose());
        details.setUserTypeId(request.getUserTypeId());
        details.setVoltageLevel(request.getVoltageLevel());
        details.setCustomerIdentifierByGridOperator(request.getCustomerIdentifierByGridOperator());
        details.setCustomerNumberByGridOperator(request.getCustomerNumberByGridOperator());
        details.setMeasurementType(request.getSlp() ? PODMeasurementType.SLP : SETTLEMENT_PERIOD);
        details.setProvidedPower(request.getProvidedPower());
        details.setMultiplier(request.getMultiplier());
        details.setPodMeasurementTypeId(request.getMeasurementTypeId());
        PodAddressRequest addressRequest = request.getAddressRequest();

        fillGeneralAddressData(details, addressRequest);
    }

    public static void fillGeneralAddressData(PointOfDeliveryDetails details, PodAddressRequest addressRequest) {
        details.setForeignAddress(addressRequest.getForeign());
        details.setStreetNumber(addressRequest.getNumber());
        details.setAddressAdditionalInfo(addressRequest.getAdditionalInformation());
        details.setBlock(addressRequest.getBlock());
        details.setEntrance(addressRequest.getEntrance());
        details.setFloor(addressRequest.getFloor());
        details.setApartment(addressRequest.getApartment());
        details.setMailbox(addressRequest.getMailbox());
    }

    public static void updatePointOfDeliveryDetails(PodUpdateRequest request, PointOfDeliveryDetails details, Integer versionId, Long podId) {
        fillPointOfDeliveryDetails(request, details);
        details.setPodId(podId);
        details.setVersionId(versionId);
    }

    public static PointOfDeliveryDetails fillNewDetails(PointOfDeliveryDetails oldDetails, PointOfDeliveryDetails details, Integer versionId, Long podId) {
        details.setPodId(podId);
        details.setVersionId(versionId);
        details.setCustomerId(oldDetails.getCustomerId());
        details.setAdditionalIdentifier(oldDetails.getAdditionalIdentifier());
        details.setBalancingGroupCoordinatorId(oldDetails.getBalancingGroupCoordinatorId());
        details.setUserTypeId(oldDetails.getUserTypeId());
        details.setCustomerNumberByGridOperator(oldDetails.getCustomerNumberByGridOperator());
        details.setCustomerIdentifierByGridOperator(oldDetails.getCustomerIdentifierByGridOperator());
        return details;
    }

    public static PointOfDelivery createContractPod(PodContractRequest request) {
        PointOfDelivery pointOfDelivery = new PointOfDelivery();
        pointOfDelivery.setIdentifier(request.getIdentifier());
        pointOfDelivery.setStatus(PodStatus.ACTIVE);
        pointOfDelivery.setBlockedBilling(false);
        pointOfDelivery.setBlockedDisconnection(false);
        pointOfDelivery.setImpossibleToDisconnect(false);
        pointOfDelivery.setGridOperatorId(request.getGridOperatorId());
        return pointOfDelivery;
    }

    public static PointOfDeliveryDetails createContractPodDetails(PodContractRequest request) {
        PointOfDeliveryDetails details = new PointOfDeliveryDetails();
        details.setName(request.getName());
        details.setType(request.getType());
        details.setEstimatedMonthlyAvgConsumption(request.getEstimatedMonthlyAvgConsumption());
        details.setConsumptionPurpose(request.getConsumptionPurpose());
        details.setConsumptionPurpose(request.getConsumptionPurpose());
        details.setVoltageLevel(request.getVoltageLevel());
        details.setMeasurementType(request.getSlp() ? SLP : SETTLEMENT_PERIOD);
        details.setProvidedPower(request.getProvidedPower());
        details.setMultiplier(request.getMultiplier());
        details.setPodMeasurementTypeId(request.getMeasurementTypeId());
        return details;

    }
}
