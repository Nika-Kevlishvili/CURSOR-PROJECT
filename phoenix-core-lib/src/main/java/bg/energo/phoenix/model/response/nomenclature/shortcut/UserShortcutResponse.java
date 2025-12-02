package bg.energo.phoenix.model.response.nomenclature.shortcut;

import bg.energo.phoenix.model.entity.nomenclature.shortcut.Shortcut;
import bg.energo.phoenix.model.enums.nomenclature.shortcut.UserShortcuts;
import lombok.Data;

@Data
public class UserShortcutResponse {

    private UserShortcuts shortcut;
    private Long orderingId;

    public UserShortcutResponse(Shortcut shortcutEntity) {
        this.shortcut = shortcutEntity.getShortcut();
        this.orderingId = shortcutEntity.getOrderingId();
    }
}
