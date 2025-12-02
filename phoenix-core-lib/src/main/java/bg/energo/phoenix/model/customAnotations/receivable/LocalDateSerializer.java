package bg.energo.phoenix.model.customAnotations.receivable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateSerializer extends StdSerializer<LocalDate> {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public LocalDateSerializer() {
        super(LocalDate.class);
    }

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value != null) {
            gen.writeString(value.format(DATE_FORMAT));
        } else {
            gen.writeNull();
        }
    }
}