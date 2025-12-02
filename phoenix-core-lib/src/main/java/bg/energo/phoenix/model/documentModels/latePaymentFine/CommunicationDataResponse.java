package bg.energo.phoenix.model.documentModels.latePaymentFine;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommunicationDataResponse {
    private Long id;
    private String contactValues;

    public CommunicationDataResponse(CommunicationDataMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.contactValues = middleResponse.getContactValues();
    }
}
