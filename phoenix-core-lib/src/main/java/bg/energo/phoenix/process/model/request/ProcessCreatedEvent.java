package bg.energo.phoenix.process.model.request;

import bg.energo.phoenix.event.Event;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
@NoArgsConstructor
public class ProcessCreatedEvent extends Event {

    private Payload payload;

    public ProcessCreatedEvent(Metadata metadata,
                               Payload payload) {
        super(metadata);
        this.payload = payload;
    }

    @ToString
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Payload {

        private String fileUrl;
        private Long processId;
        private String permissions;
        private Long reminderId;

    }
}
