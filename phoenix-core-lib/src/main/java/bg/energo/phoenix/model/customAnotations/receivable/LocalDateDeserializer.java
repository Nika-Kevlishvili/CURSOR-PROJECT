package bg.energo.phoenix.model.customAnotations.receivable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateDeserializer extends StdDeserializer<LocalDate> {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public LocalDateDeserializer() {
        super(LocalDate.class);
    }

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        try {
            String dateString = jsonParser.getText();
            return LocalDate.parse(dateString, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw deserializationContext.weirdStringException(jsonParser.getText(), LocalDate.class, "Invalid date format. The date should be in the format dd-MM-yyyy.");
        }
    }
}
