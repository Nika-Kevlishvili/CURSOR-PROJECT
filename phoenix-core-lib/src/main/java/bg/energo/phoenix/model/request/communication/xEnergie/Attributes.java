package bg.energo.phoenix.model.request.communication.xEnergie;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Attributes {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Attribute")
    private List<Attribute> attribute;

    @Data
    @Builder
    public static class Attribute {
        @JacksonXmlProperty(localName = "name", isAttribute = true)
        private String name;

        @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
        private Boolean deleteValue;

        @JacksonXmlProperty(localName = "String", isAttribute = true)
        private Value value;
    }
}
