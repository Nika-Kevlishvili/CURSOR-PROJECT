package phoenix.core.customer.model.request.manager;

import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class EditManagerRequest extends BaseManagerRequest{

    @NotNull
    private Long id;



}
