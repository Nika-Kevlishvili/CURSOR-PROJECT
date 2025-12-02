package bg.energo.phoenix.model.response.communication.xEnergie;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdditionalInformationForPointOfDeliveries {
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private String owner;
    private Long ownerPDT;
    private String EANorEIC;
    private String dealId;
    private String name;
    private String description;
    private String type;
    private String voltageLevelOne;
    private String measurement;
    private String sourceType;
    private String distributionNetwork;
    private String network;
    private String supplier;
    private String lpRegion1;
    private String lpRegion2;
    private String coordinatorOfBalancingGroup;
    private String measurementSystem;
    private String ancillaryServicesProvider;
    private String meteringServicesProvider;
}
