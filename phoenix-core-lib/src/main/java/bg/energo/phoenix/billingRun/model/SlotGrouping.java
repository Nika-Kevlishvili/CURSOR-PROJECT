package bg.energo.phoenix.billingRun.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlotGrouping {
    protected Long bgInvoiceSlotId;
    protected Long podId;
    protected Long pcId;
}
