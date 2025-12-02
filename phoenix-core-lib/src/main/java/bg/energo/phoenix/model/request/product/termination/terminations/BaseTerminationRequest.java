package bg.energo.phoenix.model.request.product.termination.terminations;

import bg.energo.phoenix.model.customAnotations.product.terminations.AutoTerminationFromValidator;
import bg.energo.phoenix.model.customAnotations.product.terminations.NoticeDueFieldsValidator;
import bg.energo.phoenix.model.customAnotations.product.terminations.TerminationTemplateValidator;
import bg.energo.phoenix.model.enums.product.termination.terminations.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AutoTerminationFromValidator
@NoticeDueFieldsValidator
@TerminationTemplateValidator
public class BaseTerminationRequest {

    @NotBlank(message = "name-Name is required;")
    @Length(min = 1, max = 1024, message = "name-Name length must be between 1 and 1024;")
    private String name;

    @Length(min = 1, max = 2048, message = "contractClauseNumber-Contract Clause Number length must be between 1 and 2048;")
    private String contractClauseNumber;

    @NotNull(message = "autoTermination-Auto termination is required;")
    private Boolean autoTermination;

    private AutoTerminationFrom autoTerminationFrom;

    private TerminationEvent event;

    @NotNull(message = "noticeDue-Notice Due is required;")
    private Boolean noticeDue;

    @Range(min = 0, max = 9999, message = "noticeDueMinValue-Notice Period Min Value must be in range 1_9999;")
    private Integer noticeDueValueMin;

    @Range(min = 0, max = 9999, message = "noticeDueMaxValue-Notice Period Max Value must be in range 1_9999;")
    private Integer noticeDueValueMax;

    private CalculateFrom calculateFrom;

    private NoticeDueType noticeDueType;

    private Boolean autoEmailNotification;

    @NotEmpty(message = "terminationNotificationChannels-At least one Termination Notification Channels must be provided;")
    private Set<TerminationNotificationChannelType> terminationNotificationChannels;

    @Length(min = 1, max = 4096, message = "additionalInfo-Additional Info length must be between 1 and 4096;")
    private String additionalInfo;

    Long templateId;

}
