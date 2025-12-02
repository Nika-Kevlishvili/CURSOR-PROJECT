package bg.energo.phoenix.model.response.pod.pod;

import bg.energo.phoenix.model.enums.pod.pod.PODConsumptionPurposes;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.pod.pod.PODType;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointOfDeliveryFilterResponse {
    private Long id;
    private Long detailId;
    private String identifier;
    private PODType podType;
    private String customerIdentifier;
    private String gridOperatorName;
    private BigDecimal providedPower;
    private PODConsumptionPurposes podConsumptionPurposes;
    private PODMeasurementType podMeasurementType;
    private String disconnected;
    private PodStatus status;

    public PointOfDeliveryFilterResponse(
            Long id,
            Long detailId,
            String identifier,
            PODType podType,
            String customerIdentifier,
            String gridOperatorName,
            BigDecimal providedPower,
            PODConsumptionPurposes podConsumptionPurposes,
            PODMeasurementType podMeasurementType,
            Boolean disconnected,
            PodStatus status,
            String name,
            String middleName,
            String lastName,
            String legalFormName
    ) {
        this.id = id;
        this.detailId = detailId;
        if (customerIdentifier != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(customerIdentifier);
            sb.append(" (");
            sb.append(name);
            if (middleName != null) {
                sb.append(" ");
                sb.append(middleName);
            }
            if (lastName != null) {
                sb.append(" ");
                sb.append(lastName);
            }
            if (legalFormName != null) {
                sb.append(" ");
                sb.append(legalFormName);
            }
            sb.append(")");
            this.customerIdentifier = sb.toString();
        }
        this.identifier = identifier;
        this.podType = podType;
        this.gridOperatorName = gridOperatorName;
        this.providedPower = providedPower;
        this.podConsumptionPurposes = podConsumptionPurposes;
        this.podMeasurementType = podMeasurementType;
        this.disconnected = disconnected == null ? "NO" : disconnected ? "YES" : "NO";
        this.status = status;
    }
}
