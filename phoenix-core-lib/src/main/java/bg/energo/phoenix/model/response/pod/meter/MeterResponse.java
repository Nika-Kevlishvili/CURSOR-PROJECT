package bg.energo.phoenix.model.response.pod.meter;

import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.enums.pod.meter.MeterStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class MeterResponse {

    private Long id;
    private String number;
    private MeterStatus status;

    private Long gridOperatorId;
    private String gridOperatorName;

    private MeterPodResponse pod;

    private LocalDate installmentDate;
    private LocalDate removeDate;

    private List<MeterScaleResponse> meterScales;

    public MeterResponse(Long id,
                         String number,
                         MeterStatus status,
                         Long gridOperatorId,
                         String gridOperatorName,
                         PointOfDelivery pod,
                         PointOfDeliveryDetails podDetails,
                         LocalDate installmentDate,
                         LocalDate removeDate) {
        this.id = id;
        this.number = number;
        this.status = status;
        this.gridOperatorId = gridOperatorId;
        this.gridOperatorName = gridOperatorName;
        this.pod = new MeterPodResponse(pod.getId(), pod.getIdentifier(), podDetails.getName());
        this.installmentDate = installmentDate;
        this.removeDate = removeDate;
    }
}
