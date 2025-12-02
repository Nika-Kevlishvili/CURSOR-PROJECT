package bg.energo.phoenix.model.response.customer.communicationData;

import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerCommunicationEmailDataResponse extends CustomerCommunicationDataResponse {

    private String emailAddress;

    public CustomerCommunicationEmailDataResponse(Long id, String name, LocalDateTime createDate, String emailAddress) {
        super(id, name, createDate);
        this.emailAddress = emailAddress;
    }

}
