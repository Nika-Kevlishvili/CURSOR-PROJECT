package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.enums.customer.CommunicationDataType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CommunicationDataListRequest {

    @NotNull(message = "customerDetailsId-[customerDetailsId] shouldn't not be null;")
    private Long customerDetailsId;

    @NotNull(message = "communicationDataType-[CommunicationDataType] shouldn't not be null;")
    private CommunicationDataType communicationDataType;
}
