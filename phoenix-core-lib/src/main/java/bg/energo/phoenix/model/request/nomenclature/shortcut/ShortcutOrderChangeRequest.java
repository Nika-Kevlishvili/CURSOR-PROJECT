package bg.energo.phoenix.model.request.nomenclature.shortcut;

import bg.energo.phoenix.model.enums.nomenclature.shortcut.UserShortcuts;
import lombok.Data;

@Data
public class ShortcutOrderChangeRequest {
    private Long orderingId;
    private UserShortcuts shortcuts;
}
