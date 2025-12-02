package bg.energo.phoenix.model.response.receivable.collectionChannel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConditionInfo {
    private Long id;
    private String name;
    private String key;
}
