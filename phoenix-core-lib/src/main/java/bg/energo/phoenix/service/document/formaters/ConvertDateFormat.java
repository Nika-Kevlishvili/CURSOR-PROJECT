package bg.energo.phoenix.service.document.formaters;

import hr.ngs.templater.DocumentFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class ConvertDateFormat implements DocumentFactoryBuilder.Formatter {
    @Override
    public Object format(Object value, String metadata) {
        log.debug("Formatting date: {} with metadata: {}", value, metadata);
        if (metadata == null || value == null) {
            return value;
        }

        if (metadata.startsWith("format(")) {
            String expression = metadata.substring("format(".length(), metadata.length() - 1);
            log.debug("Expression: {}", expression);

            try {
                if (value instanceof LocalDate) {
                    log.debug("Value type is LocalDate");
                    String formattedDate = ((LocalDate) value).format(DateTimeFormatter.ofPattern(expression));
                    log.debug("Formatted date: {}", formattedDate);
                    return formattedDate;
                }

                if (value instanceof LocalDateTime) {
                    log.debug("Value type is LocalDateTime");
                    String formattedDate = ((LocalDateTime) value).format(DateTimeFormatter.ofPattern(expression));
                    log.debug("Formatted date: {}", formattedDate);
                    return formattedDate;
                }
            } catch (Exception e) {
                log.trace("Failed to format date", e);
            }
        }

        return value;
    }
}
