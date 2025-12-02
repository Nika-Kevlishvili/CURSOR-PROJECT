package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.entity.product.service.ServiceInterimAndAdvancePayment;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceInterimAdvancePaymentResponse {
    private Long id;
    private Long interimAndAdvancePaymentId;
    private String interimAndAdvancePaymentName;
    private ServiceSubobjectStatus status;

    public ServiceInterimAdvancePaymentResponse(ServiceInterimAndAdvancePayment serviceInterimAndAdvancePayment) {
        this.id = serviceInterimAndAdvancePayment.getId();
        this.interimAndAdvancePaymentId = serviceInterimAndAdvancePayment.getInterimAndAdvancePayment().getId();
        this.interimAndAdvancePaymentName = serviceInterimAndAdvancePayment.getInterimAndAdvancePayment().getName();
        this.status = serviceInterimAndAdvancePayment.getStatus();
    }
}
