package bg.energo.phoenix.model.request.communication.edms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SetAttributesRequest {
    private List<Attribute> attributes;
}
