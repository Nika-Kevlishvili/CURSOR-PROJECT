package bg.energo.phoenix.process.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditProcessRequest {

    private Set<ProcessNotificationObject> notifications;
}
