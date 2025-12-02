package bg.energo.phoenix.model.enums.template;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContractTemplateFileName {
    CUSTOMER_IDENTIFIER(1),
    CUSTOMER_NAME(2),
    CUSTOMER_NUMBER(3),
    DOCUMENT_NUMBER(4),
    FILE_ID(5),
    TIMESTAMP(6);

    private final int priority;
}
