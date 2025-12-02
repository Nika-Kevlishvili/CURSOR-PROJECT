package bg.energo.phoenix.model.request.communication.xEnergie;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Value {
    @JacksonXmlProperty(isAttribute = true, localName = "v")
    private String v;

    @JacksonXmlProperty(isAttribute = true, localName = "is_key")
    private Boolean isKey;
}