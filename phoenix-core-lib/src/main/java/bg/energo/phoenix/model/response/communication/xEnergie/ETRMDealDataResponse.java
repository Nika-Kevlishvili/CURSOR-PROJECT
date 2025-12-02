package bg.energo.phoenix.model.response.communication.xEnergie;

import bg.energo.phoenix.model.request.communication.xEnergie.ETRMDealDataRequest;
import bg.energo.phoenix.model.request.communication.xEnergie.Value;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "ETRMResponse")
public class ETRMDealDataResponse {
    @JacksonXmlProperty(isAttribute = true, localName = "date-created")
    private String dateCreated;

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
    private final String xmlnsXSI = "http://www.w3.org/2001/XMLSchema-instance";

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsd")
    private final String xmlnsXSD = "http://www.w3.org/2001/XMLSchema";

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns")
    private final String xmlns = "http://www.microstep-hdo.sk/schema/ETRMDealData";

    @JacksonXmlProperty(localName = "MessageIdentification")
    private ETRMDealDataRequest.IdentificationProperties messageIdentification;

    @JacksonXmlProperty(localName = "SenderIdentification")
    private ETRMDealDataRequest.IdentificationProperties senderIdentification;

    @JacksonXmlProperty(localName = "ReceiverIdentification")
    private ETRMDealDataRequest.IdentificationProperties receiverIdentification;

    @JacksonXmlProperty(localName = "Deal")
    private Deal deal;

    @JacksonXmlProperty(localName = "Reason")
    private Reason reason;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Deal {
        @JacksonXmlProperty(localName = "Id")
        private Id id;

        @JacksonXmlProperty(localName = "Number")
        private Number number;

        @JacksonXmlProperty(localName = "Type")
        private Type type;

        @JacksonXmlProperty(localName = "DateTimeFrom")
        private bg.energo.phoenix.model.request.communication.xEnergie.Deal.DateTimeParameter dateTimeFrom;

        @JacksonXmlProperty(localName = "DateTimeTo")
        private bg.energo.phoenix.model.request.communication.xEnergie.Deal.DateTimeParameter dateTimeTo;

        @JacksonXmlProperty(localName = "OutParty")
        private Party outParty;

        @JacksonXmlProperty(localName = "OutArea")
        private Area outArea;

        @JacksonXmlProperty(localName = "InParty")
        private Party inParty;

        @JacksonXmlProperty(localName = "InArea")
        private Area inArea;

        @JacksonXmlProperty(localName = "Direction")
        private bg.energo.phoenix.model.request.communication.xEnergie.Deal.Direction direction;

        @JacksonXmlProperty(localName = "ShortName")
        private bg.energo.phoenix.model.request.communication.xEnergie.Deal.ShortName shortName;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Id {
            @JacksonXmlProperty(localName = "Value")
            private Value value;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Number {
            @JacksonXmlProperty(localName = "Value")
            private Value value;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Type {
            @JacksonXmlProperty(localName = "Value")
            private Value value;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            public static class Value {
                @JacksonXmlProperty(localName = "Id")
                private Id id;
            }
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Party {
            @JacksonXmlProperty(localName = "Value")
            private Value value;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            public static class Value {
                @JacksonXmlProperty(localName = "Id")
                private Id id;

                @JacksonXmlProperty(localName = "Name")
                private Name name;

                @JacksonXmlProperty(localName = "ShortName")
                private Name shortName;

                @JacksonXmlProperty(localName = "Ean")
                private Ean ean;

                @JacksonXmlProperty(localName = "Attributes")
                private Attributes attributes;

                @Data
                @AllArgsConstructor
                @NoArgsConstructor
                public static class Name {
                    @JacksonXmlProperty(localName = "Value")
                    private bg.energo.phoenix.model.request.communication.xEnergie.Value value;
                }

                @Data
                @AllArgsConstructor
                @NoArgsConstructor
                public static class Ean {
                    @JacksonXmlProperty(localName = "Value")
                    private bg.energo.phoenix.model.request.communication.xEnergie.Value value;
                }

                @Data
                @AllArgsConstructor
                @NoArgsConstructor
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Attributes {
                    @JacksonXmlProperty(localName = "name", isAttribute = true)
                    private String name;

                    @JacksonXmlElementWrapper(useWrapping = false)
                    @JacksonXmlProperty(localName = "Attribute")
                    private List<Attribute> attributes;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @JsonIgnoreProperties(ignoreUnknown = true)
                    public static class Attribute {
                        @JacksonXmlProperty(localName = "String")
                        private bg.energo.phoenix.model.request.communication.xEnergie.Value stringValue;

                        @JacksonXmlProperty(localName = "Number")
                        private bg.energo.phoenix.model.request.communication.xEnergie.Value numberValue;
                    }
                }
            }
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Area {
            @JacksonXmlProperty(localName = "Value")
            private Value value;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            public static class Value {
                @JacksonXmlProperty(localName = "Id")
                private Id id;

                @JacksonXmlProperty(localName = "Name")
                private Name name;

                @Data
                @AllArgsConstructor
                @NoArgsConstructor
                public static class Name {
                    @JacksonXmlProperty(localName = "Value")
                    private bg.energo.phoenix.model.request.communication.xEnergie.Value value;
                }
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Reason {
        @JacksonXmlProperty(localName = "code", isAttribute = true)
        private String code;

        @JacksonXmlText
        private String description;
    }
}
