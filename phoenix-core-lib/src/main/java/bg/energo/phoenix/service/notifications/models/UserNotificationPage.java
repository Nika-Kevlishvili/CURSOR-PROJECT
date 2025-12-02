package bg.energo.phoenix.service.notifications.models;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

@Getter
@Setter
@JsonSerialize(using = UserNotificationPage.UserNotificationPageSerializer.class)
public class UserNotificationPage<T> extends PageImpl<T> {
    private Long readenNotificationCount;

    public UserNotificationPage(List<T> content, Pageable pageable, long total, long readenNotificationCount) {
        super(content, pageable, total);
        this.readenNotificationCount = readenNotificationCount;
    }

    public static class UserNotificationPageSerializer<T> extends StdSerializer<UserNotificationPage<T>> {
        UserNotificationPageSerializer() {
            this(null);
        }

        public UserNotificationPageSerializer(Class<UserNotificationPage<T>> t) {
            super(t);
        }

        @Override
        public void serialize(UserNotificationPage value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("content", value.getContent());
            gen.writeObjectField("pageable", value.getPageable());
            gen.writeNumberField("totalElements", value.getTotalElements());
            gen.writeNumberField("unreadenNotificationCount", value.getReadenNotificationCount());
            gen.writeNumberField("totalPages", value.getTotalPages());
            gen.writeNumberField("size", value.getSize());
            gen.writeNumberField("number", value.getNumber());
            gen.writeBooleanField("first", value.isFirst());
            gen.writeBooleanField("last", value.isLast());
            gen.writeNumberField("numberOfElements", value.getNumberOfElements());
            gen.writeBooleanField("empty", value.isEmpty());
            gen.writeEndObject();
        }
    }
}
