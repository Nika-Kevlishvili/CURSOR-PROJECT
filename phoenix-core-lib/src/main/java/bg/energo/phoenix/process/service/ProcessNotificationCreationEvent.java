package bg.energo.phoenix.process.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessNotificationCreationEvent {
    private Long processId;
}
