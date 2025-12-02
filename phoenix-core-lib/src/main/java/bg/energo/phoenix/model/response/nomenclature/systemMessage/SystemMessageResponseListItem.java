package bg.energo.phoenix.model.response.nomenclature.systemMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemMessageResponseListItem {
    Long id;
    String name;
    String key;

    public SystemMessageResponseListItem(SystemMessageResponseListItem systemMessageResponseListItem) {
        this.id = systemMessageResponseListItem.getId();
        this.name = systemMessageResponseListItem.getName();
        this.key = systemMessageResponseListItem.getKey();
    }
}
