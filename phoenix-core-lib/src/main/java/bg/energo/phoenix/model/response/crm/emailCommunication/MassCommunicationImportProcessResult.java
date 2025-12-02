package bg.energo.phoenix.model.response.crm.emailCommunication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MassCommunicationImportProcessResult {
    private Set<MassCommunicationFileProcessedResult> results;
    private String popupMessage;
}
