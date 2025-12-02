package bg.energo.phoenix.model.response.nomenclature.systemMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@AllArgsConstructor
public class SystemMessageFilterResponse {
    Page<SystemMessageResponseListItem> systemMessageResponseListItems;
    Long count;
}
