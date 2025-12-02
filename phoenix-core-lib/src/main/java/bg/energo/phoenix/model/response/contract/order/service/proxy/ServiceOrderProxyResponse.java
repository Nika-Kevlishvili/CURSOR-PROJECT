package bg.energo.phoenix.model.response.contract.order.service.proxy;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceOrderProxyResponse {
    private Boolean foreignEntity;
    private String name;
    private String customerIdentifier;
    private String email;
    private String phone;
    private String powerOfAttorneyNumber;
    private LocalDate date;
    private LocalDate validTill;
    private String notaryPublic;
    private String registrationNumber;
    private String operationArea;
}
