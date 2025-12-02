package bg.energo.phoenix.util.epb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EPBXmlUtils {
    private static XmlMapper xmlMapper = new XmlMapper();

    public EPBXmlUtils() {
        xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String writeValueAsString(Object o) {
        try {
            return xmlMapper.writeValueAsString(o);
        } catch (Exception e) {
            log.error("Cannot deserialize object to xml string", e);
        }
        return null;
    }

    public static <T> T writeValueAs(String xml, Class<T> clazz) {
        try {
            return xmlMapper.readValue(xml, clazz);
        } catch (JsonProcessingException e) {
            log.error("Cannot serialize xml string to object", e);
        }
        return null;
    }
}
