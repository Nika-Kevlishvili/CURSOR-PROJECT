package bg.energo.phoenix.model;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingDisplayColor;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingRole;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingType;
import bg.energo.phoenix.model.enums.receivable.offsetting.OffsettingOperationType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ObjectOffsettingDetail {

    private Long objectId;
    private ObjectOffsettingType objectType;
    private BigDecimal offsettingAmount;
    private Long currencyId;
    private String currencyName;
    private LocalDate operationDate;
    private OffsettingOperationType operationType;
    private EntityStatus status;
    private ObjectOffsettingDisplayColor displayColor;
    private OperationContext operationContext;
    private ObjectOffsettingRole objectRole;

}
