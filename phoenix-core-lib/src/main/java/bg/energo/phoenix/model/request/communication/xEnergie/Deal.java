package bg.energo.phoenix.model.request.communication.xEnergie;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class Deal {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JacksonXmlProperty(localName = "Number")
    private Number number;

    @JacksonXmlProperty(localName = "Type")
    private Type type;

    @JacksonXmlProperty(localName = "DateTimeFrom")
    private DateTimeParameter dateTimeFrom;

    @JacksonXmlProperty(localName = "DateTimeTo")
    private DateTimeParameter dateTimeTo;

    @JacksonXmlProperty(localName = "OutParty")
    private Party outParty;

    @JacksonXmlProperty(localName = "OutArea")
    private Area outArea;

    @JacksonXmlProperty(localName = "InParty")
    private Party inParty;

    @JacksonXmlProperty(localName = "InArea")
    private Area inArea;

    @JacksonXmlProperty(localName = "Direction")
    private Direction direction;

    @JacksonXmlProperty(localName = "ShortName")
    private ShortName shortName;

    @JacksonXmlProperty(localName = "DealData")
    private DealData dealData;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Number {
        @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
        private Boolean deleteValue;

        @JacksonXmlProperty(localName = "Value")
        private Value value;
    }

    @Data
    @Builder
    public static class Type {
        @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
        private Boolean deleteValue;

        @JacksonXmlProperty(localName = "Value")
        private Value value;

        @Data
        @Builder
        public static class Value {
            @JacksonXmlProperty(localName = "Code")
            private Code code;

            @Data
            @Builder
            public static class Code {
                @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
                private Boolean deleteValue;

                @JacksonXmlProperty(localName = "Value")
                private bg.energo.phoenix.model.request.communication.xEnergie.Value value;
            }
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DateTimeParameter {
        @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
        private Boolean deleteValue;

        @JacksonXmlProperty(localName = "Value")
        private Value value;
    }

    @Data
    @Builder
    public static class Party {
        @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
        private Boolean deleteValue;

        @JacksonXmlProperty(localName = "Value")
        private Value value;

        @Data
        @Builder
        public static class Value {
            @JacksonXmlProperty(localName = "Ean")
            private Ean ean;

            @JacksonXmlProperty(localName = "Attributes")
            private Attributes attributes;

            @Data
            @Builder
            public static class Ean {
                @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
                private Boolean deleteValue;

                @JacksonXmlProperty(localName = "Value")
                private bg.energo.phoenix.model.request.communication.xEnergie.Value value;
            }
        }
    }

    @Data
    @Builder
    public static class Area {
        @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
        private Boolean deleteValue;

        @JacksonXmlProperty(localName = "Value")
        private Value value;

        @Data
        @Builder
        public static class Value {
            @JacksonXmlProperty(localName = "Id")
            private Id id;

            @Data
            @Builder
            public static class Id {
                @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
                private Boolean deleteValue;

                @JacksonXmlProperty(localName = "Value", isAttribute = true)
                private bg.energo.phoenix.model.request.communication.xEnergie.Value value;
            }
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Direction {
        @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
        private Boolean deleteValue;

        @JacksonXmlProperty(localName = "Value")
        private Value value;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShortName {
        @JacksonXmlProperty(localName = "delete_value", isAttribute = true)
        private Boolean deleteValue;

        @JacksonXmlProperty(localName = "Value")
        private Value value;
    }

    @Data
    @Builder
    public static class DealData {
        @JacksonXmlProperty(localName = "name", isAttribute = true)
        private String name;

        @JacksonXmlProperty(localName = "resolution", isAttribute = true)
        private String resolution;

        @JacksonXmlProperty(localName = "Parametric")
        private Parametric parametric;
    }
}
