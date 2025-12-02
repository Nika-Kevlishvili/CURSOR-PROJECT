package bg.energo.phoenix.model.enums.portal;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h2>PortalApplicationUserStatus</h2><br>
 * Portal Application User Status Enums:
 *
 * {@link #ALL},
 * {@link #ONLY_ACTIVE},
 * {@link #ONLY_NOT_ACTIVE}
 */
@Getter
@AllArgsConstructor
public enum PortalApplicationUserStatus {
    ALL("ALL"),
    ONLY_ACTIVE("ONLY_ACTIVE"),
    ONLY_NOT_ACTIVE("ONLY_NOT_ACTIVE");

    private final String status;
}
