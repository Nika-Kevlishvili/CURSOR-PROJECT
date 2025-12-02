package bg.energo.phoenix.model.response.customer.communicationData;

import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerCommunicationMobileDataResponse extends CustomerCommunicationDataResponse {
    private String mobileNumber;

    public CustomerCommunicationMobileDataResponse(Long id, String name, LocalDateTime createDate,String mobileNumber) {
        super(id,name,createDate);
        this.mobileNumber=mobileNumber;
    }

    public CustomerCommunicationMobileDataResponse() {
        super();
    }
}
