package bg.energo.phoenix.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private Metadata metadata;

    @Getter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String id;
        private String userId;
        private LocalDateTime createdAt;
        private String type;
    }
}
