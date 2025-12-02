package bg.energo.phoenix.model.request.pod.billingByScales;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingByScalesEditRequest {

    @NotNull(message = "id-[id] must not be null;")
    private Long id;

    @NotNull(message = "saveRecordForIntermediatePeriod-[saveRecordForIntermediatePeriod] Can't be null;")
    private Boolean saveRecordForIntermediatePeriod;

    @NotNull(message = "saveRecordForMeterReadings-[saveRecordForMeterReadings] Can't be null;")
    private Boolean saveRecordForMeterReadings;

    @Size(max = 100, message = "BillingByScalesEditRequest-billingByScalesTableEditRequests should contain {max} items;")
    List<@Valid BillingByScalesTableEditRequest> billingByScalesTableEditRequests;

}
