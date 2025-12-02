package bg.energo.phoenix.model.response.crm;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter()
public class LongArrayToListConverter implements AttributeConverter<Long[], List<Long>> {

    // Convert Long[] (entity attribute) to List<Long> (database column)
    @Override
    public List<Long> convertToDatabaseColumn(Long[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return List.of();
        }
        // Convert Long[] to List<Long>
        return Arrays
                .stream(attribute)
                .collect(Collectors.toList());
    }

    // Convert List<Long> (database column) back to Long[] (entity attribute)
    @Override
    public Long[] convertToEntityAttribute(List<Long> dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new Long[0];
        }
        // Convert List<Long> back to Long[]
        return dbData.toArray(new Long[0]);
    }
}
