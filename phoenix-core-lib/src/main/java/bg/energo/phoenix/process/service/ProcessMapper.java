package bg.energo.phoenix.process.service;

import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.request.ProcessCreatedEvent;
import bg.energo.phoenix.process.model.response.ProcessListResponse;
import bg.energo.phoenix.process.model.response.ProcessNotificationResponse;
import bg.energo.phoenix.process.model.response.ProcessResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessMapper {

    public ProcessListResponse entityToListResponse(Process process) {
        return ProcessListResponse.builder()
                .id(process.getId())
                .status(process.getStatus())
                .name(process.getName())
                .creationDate(process.getCreateDate())
                .startDate(process.getProcessStartDate())
                .completeDate(process.getProcessCompleteDate())
                .systemUserId(process.getSystemUserId())
                .build();
    }

    // TODO: 08.02.23 rest of the fields need to be added later
    public ProcessResponse entityToResponse(Process process, List<ProcessNotificationResponse> responseByProcessId) {
        return ProcessResponse.builder()
                .id(process.getId())
                .status(process.getStatus())
                .processType(process.getType())
                .name(process.getName())
                .creationDate(process.getCreateDate())
                .startDate(process.getProcessStartDate())
                .completeDate(process.getProcessCompleteDate())
                .systemUserId(process.getSystemUserId())
                .processResponses(responseByProcessId)
                .build();
    }

    public ProcessCreatedEvent.Payload toEventPayload(Process process) {
        return ProcessCreatedEvent.Payload
                .builder()
                .fileUrl(process.getFileUrl())
                .processId(process.getId())
                .reminderId(process.getReminderId())
                .permissions(process.getUserPermissions())
                .build();
    }
}
