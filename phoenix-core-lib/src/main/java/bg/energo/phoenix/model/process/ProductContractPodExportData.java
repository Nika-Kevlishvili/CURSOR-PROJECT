package bg.energo.phoenix.model.process;

import lombok.Data;

import java.time.LocalDate;
import java.util.Objects;

@Data
public class ProductContractPodExportData {
    public ProductContractPodExportData(
            String podIdentifier,
            String podAdditionalIdentifier,
            String podIdentifierForEANorEIC,
            LocalDate dateOfActivation,
            LocalDate dateOfDeactivation,
            String voltageLevelOne,
            String measurementType,
            Long customerNumber,
            String distributionNetwork,
            String meteringDataProvider,
            String ancillaryServicesProvider,
            String lpRegion,
            String dealId,
            String measurementSystem
    ) {
        this.podIdentifier = podIdentifier;
        this.podAdditionalIdentifier = podAdditionalIdentifier;
        this.podIdentifierForEANorEIC = podIdentifierForEANorEIC;
        this.dateOfActivation = dateOfActivation;
        this.dateOfDeactivation = Objects.requireNonNullElse(dateOfDeactivation, LocalDate.of(2030, 12, 31));
        this.voltageLevelOne = voltageLevelOne;
        this.measurementType = measurementType;
        this.customerNumber = customerNumber;
        this.distributionNetwork = distributionNetwork;
        this.meteringDataProvider = meteringDataProvider;
        this.ancillaryServicesProvider = ancillaryServicesProvider;
        this.lpRegion = lpRegion;
        this.dealId = dealId;
        this.measurementSystem = measurementSystem;
    }

    private String podIdentifier;
    private String podAdditionalIdentifier;
    private String podIdentifierForEANorEIC;
    private LocalDate dateOfActivation;
    private LocalDate dateOfDeactivation; // default value if date of deactivation not added
    private final String type = "002"; // hardcoded
    private String voltageLevelOne;
    private final String isleOperation = "N"; // hardcoded
    private final String ancillaryServicesProviderCheckbox = "N"; // hardcoded
    private String measurementType;
    private final String sourceType = "007"; // hardcoded
    private Long customerNumber;
    private String distributionNetwork;
    private final String neighbouringNetwork = ""; // hardcoded
    private final Integer supplier = 0; // hardcoded
    private final String alternateSupplier = ""; // hardcoded
    private String meteringDataProvider;
    private String ancillaryServicesProvider;
    private final String neighbouringPod = ""; // hardcoded
    private final String accountingSubject = "CBG005"; // hardcoded
    private final String installedPower = ""; // hardcoded
    private final String consumptionEstimate = ""; // hardcoded
    private String lpRegion;
    private final String lpClass = "";
    private final String summaryForAccountingSubjectCheckbox = ""; // hardcoded
    private final String abroadCheckbox = ""; // hardcoded
    private final String lockedCheckbox = ""; // hardcoded
    private final String LRS = ""; // hardcoded
    private final String observer = ""; // hardcoded
    private String dealId;
    private String measurementSystem;
    private final String nodata = ""; // hardcoded
    private final String BGnoEPRES = ""; // hardcoded
}
