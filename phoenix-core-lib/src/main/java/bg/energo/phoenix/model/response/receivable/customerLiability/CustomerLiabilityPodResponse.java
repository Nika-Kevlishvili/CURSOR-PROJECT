package bg.energo.phoenix.model.response.receivable.customerLiability;

import lombok.Data;

import static bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityMapperService.POD_DATA_COLUMN_DELIMITER;

@Data
public class CustomerLiabilityPodResponse {
    private Long podId;
    private String podName;

    public CustomerLiabilityPodResponse(String podString) {
        String[] parts = podString.split(POD_DATA_COLUMN_DELIMITER);
        this.podId = Long.valueOf(parts[0]);
        this.podName = parts[1];
    }
}
