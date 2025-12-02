package bg.energo.phoenix.model.request.communication.xEnergie;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
public class Parametric {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Formula")
    private List<Formula> formulas;

    @Data
    @Builder
    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Formula {
        @JacksonXmlProperty(localName = "name", isAttribute = true)
        private String name;

        /**
         * Must be decimal value with scale, example: 20.00
         */
        @JacksonXmlProperty(localName = "formula", isAttribute = true)
        private String formula;
    }
}
