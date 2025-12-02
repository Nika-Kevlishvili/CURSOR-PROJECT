package bg.energo.phoenix.model.request.activity;

import bg.energo.phoenix.model.entity.activity.SystemActivityJsonField;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BaseSystemActivityRequest {

    @NotEmpty(message = "fields-At least one field should be specified;")
    private List<SystemActivityJsonField> fields;

}
