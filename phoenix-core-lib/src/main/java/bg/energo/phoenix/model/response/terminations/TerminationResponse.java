package bg.energo.phoenix.model.response.terminations;

import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import bg.energo.phoenix.model.entity.product.termination.terminations.TerminationNotificationChannel;
import bg.energo.phoenix.model.enums.product.termination.terminations.*;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TerminationResponse {

    private Long id;
    private String name;
    private String contractClauseNumber;
    private Boolean autoTermination;
    private AutoTerminationFrom autoTerminationFrom;
    private TerminationEvent terminationEvent;
    private Boolean noticeDue;
    private Integer noticeDueValueMin;
    private Integer noticeDueValueMax;
    private CalculateFrom calculateFrom;
    private NoticeDueType noticeDueType;
    private Boolean autoEmailNotification;
    private String additionalInfo;
    private TerminationStatus status;
    private Set<TerminationNotificationChannelType> terminationNotificationChannels;
    private ContractTemplateShortResponse templateResponse;

    private Boolean isLocked;

    public TerminationResponse(Termination termination) {
        this.id = termination.getId();
        this.name = termination.getName();
        this.contractClauseNumber = termination.getContractClauseNumber();
        this.autoTermination = termination.getAutoTermination();
        this.autoTerminationFrom = termination.getAutoTerminationFrom();
        this.terminationEvent = termination.getEvent();
        this.noticeDue = termination.getNoticeDue();
        this.noticeDueValueMin = termination.getNoticeDueValueMin();
        this.noticeDueValueMax = termination.getNoticeDueValueMax();
        this.calculateFrom = termination.getCalculateFrom();
        this.noticeDueType = termination.getNoticeDueType();
        this.autoEmailNotification = termination.getAutoEmailNotification();
        this.additionalInfo = termination.getAdditionalInfo();
        this.status = termination.getStatus();
        this.terminationNotificationChannels = termination.getTerminationNotificationChannels().
                stream().
                map(TerminationNotificationChannel::getTerminationNotificationChannelType)
                .collect(Collectors.toSet());
    }

    public TerminationResponse(Termination termination, boolean isLocked) {
        this.id = termination.getId();
        this.name = termination.getName();
        this.contractClauseNumber = termination.getContractClauseNumber();
        this.autoTermination = termination.getAutoTermination();
        this.autoTerminationFrom = termination.getAutoTerminationFrom();
        this.terminationEvent = termination.getEvent();
        this.noticeDue = termination.getNoticeDue();
        this.noticeDueValueMin = termination.getNoticeDueValueMin();
        this.noticeDueValueMax = termination.getNoticeDueValueMax();
        this.calculateFrom = termination.getCalculateFrom();
        this.noticeDueType = termination.getNoticeDueType();
        this.autoEmailNotification = termination.getAutoEmailNotification();
        this.additionalInfo = termination.getAdditionalInfo();
        this.status = termination.getStatus();
        this.isLocked = isLocked;
        this.terminationNotificationChannels = termination.getTerminationNotificationChannels().
                stream().
                map(TerminationNotificationChannel::getTerminationNotificationChannelType)
                .collect(Collectors.toSet());
    }

}
