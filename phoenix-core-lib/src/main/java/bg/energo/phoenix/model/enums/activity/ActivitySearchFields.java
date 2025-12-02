package bg.energo.phoenix.model.enums.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActivitySearchFields {
    ALL,
    ID,
    JSON_DATA,
    CONTRACT_NUMBER,
    ORDER_NUMBER,
    COMMUNICATION_ID, // should be implemented in the following bundles and also, customer's search field value should be added when specified
    TASK_ID;
}
