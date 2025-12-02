package bg.energo.phoenix.model.request.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.customAnotations.receivable.massOperationForBlocking.ReceivableBlockingBaseRequestValidator;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ReceivableBlockingBaseRequestValidator
public class ReceivableBlockingEditRequest extends ReceivableBlockingBaseRequest {
    public List<Long> taskIds;
}
