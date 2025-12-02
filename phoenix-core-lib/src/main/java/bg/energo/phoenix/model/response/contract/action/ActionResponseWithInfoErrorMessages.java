package bg.energo.phoenix.model.response.contract.action;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.ListUtils;

import java.util.List;

@Data
@AllArgsConstructor
public class ActionResponseWithInfoErrorMessages {

    private Long id;

    // list of "positive" error messages that should not cause the create/edit operation failure
    // but should serve as information for the user for explaining the reason behind
    // not being able to calculate penalty from the formula (if applicable)
    private List<String> infoErrorMessages;

    public static ActionResponseWithInfoErrorMessages of(Long id, List<String> infoErrorMessages) {
        return new ActionResponseWithInfoErrorMessages(
                id,
                ListUtils.emptyIfNull(infoErrorMessages)
        );
    }

}
