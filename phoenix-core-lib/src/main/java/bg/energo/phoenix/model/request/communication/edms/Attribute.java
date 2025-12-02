package bg.energo.phoenix.model.request.communication.edms;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Attribute {
    private String attributeGuid;
    private Object value;
}
