package bg.energo.phoenix.util.epb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EPBJsonUtils {

    /**
     * This is a utility method that converts an object to a JSON string.
     *
     * @param obj the object to be converted
     * @return the converted string
     */
    public static String asJsonString(final Object obj) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * This is a utility method that converts a JSON string to an object.
     *
     * @param value the string to be converted
     * @param clazz the class of the object to be converted
     * @param <T>   the type of the object to be converted
     * @return the converted object
     */
    public static <T> T asObject(String value, Class<T> clazz) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.readValue(value, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> readList(String str, Class<T> type) {
        return readList(str, ArrayList.class, type);
    }

    public static <T> List<T> readList(String str, Class<? extends Collection> type, Class<T> elementType) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(str, mapper.getTypeFactory().constructCollectionType(type, elementType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
