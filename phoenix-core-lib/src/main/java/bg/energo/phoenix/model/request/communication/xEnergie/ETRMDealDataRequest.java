package bg.energo.phoenix.model.request.communication.xEnergie;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@JacksonXmlRootElement(localName = "ETRMDealData")
public class ETRMDealDataRequest {
    @JacksonXmlProperty(isAttribute = true, localName = "date-created")
    private String dateCreated;

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
    private final String xmlnsXSI = "http://www.w3.org/2001/XMLSchema-instance";

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns")
    private final String xmlns = "http://www.microstep-hdo.sk/schema/ETRMDealData";

    @JacksonXmlProperty(localName = "MessageIdentification")
    private IdentificationProperties messageIdentification;

    @JacksonXmlProperty(localName = "SenderIdentification")
    private IdentificationProperties senderIdentification;

    @JacksonXmlProperty(localName = "ReceiverIdentification")
    private IdentificationProperties receiverIdentification;


    @JacksonXmlProperty(localName = "Deal")
    private Deal deal;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IdentificationProperties {
        @JacksonXmlText
        private String identification;

        @JacksonXmlProperty(localName = "v", isAttribute = true)
        private String v;
    }
}
