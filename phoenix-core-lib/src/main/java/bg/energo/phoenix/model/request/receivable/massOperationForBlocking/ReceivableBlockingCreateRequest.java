package bg.energo.phoenix.model.request.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.customAnotations.receivable.massOperationForBlocking.ReceivableBlockingBaseRequestValidator;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ReceivableBlockingBaseRequestValidator
public class ReceivableBlockingCreateRequest extends ReceivableBlockingBaseRequest {

}

