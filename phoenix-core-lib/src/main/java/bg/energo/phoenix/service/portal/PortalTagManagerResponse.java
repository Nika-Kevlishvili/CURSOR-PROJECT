package bg.energo.phoenix.service.portal;

import bg.energo.phoenix.model.enums.task.PerformerType;

public interface PortalTagManagerResponse {

    PerformerType getPerformerType();
    Long getId();
    String getPortalId();
    String getName();
    String getNameBg();
}
