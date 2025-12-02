package bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder;

import lombok.Data;

@Data
public class RemindersForDPSRequestResponse {

    private Long id;
    private String name;

    public RemindersForDPSRequestResponse(RemindersForDPSRequestMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.name = middleResponse.getReminderNumber();
    }

}
