package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class ObjectionDocumentGenerationResponse {
    private Map<Long, String> emailDocumentId;
    private List<Long> documentIds;
}
