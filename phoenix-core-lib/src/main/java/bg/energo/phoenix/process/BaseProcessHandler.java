package bg.energo.phoenix.process;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.process.model.request.ProcessCreatedEvent;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;

import static bg.energo.phoenix.event.EventType.INVOICE_CANCELLATION_PROCESS;
import static bg.energo.phoenix.event.EventType.REMINDER_PROCESS;

@Slf4j
public abstract class BaseProcessHandler {

    @Autowired
    private FileService fileService;

    /**
     * Check if the process handler supports the provided {@link EventType}
     *
     * @param eventType {@link EventType}
     * @return true if supports
     */
    public abstract boolean supports(EventType eventType);

    /**
     * Handles a {@link ProcessCreatedEvent} by downloading the file associated with the event and starting processing for the file.
     *
     * @param event the {@link ProcessCreatedEvent} to be handled
     */
    public void handleProcessCreatedEvent(ProcessCreatedEvent event) {
        log.debug("Received process created event: {}", event);
        var payload = event.getPayload();
        EventType eventType = EventType.valueOf(event.getMetadata().getType());
        if (eventType.equals(REMINDER_PROCESS)) {
            startReminderProcessing(event.getPayload().getReminderId(), event.getPayload().getProcessId());
        }else if(eventType.equals(INVOICE_CANCELLATION_PROCESS)){
            startCancellationProcessing(event.getPayload().getProcessId());
        }else {
            var file = fileService.downloadFile(payload.getFileUrl());
            startFileProcessing(file, payload.getProcessId());
        }
    }

    /**
     * Passes the file and ID of {@link Process} to start processing
     *
     * @param file      {@link ByteArrayResource}
     * @param processId ID of {@link bg.energo.phoenix.process.model.entity.Process}
     */
    protected abstract void startFileProcessing(ByteArrayResource file, Long processId);

    protected void startReminderProcessing(Long reminderId, Long processId) {

    }

    protected void startCancellationProcessing(Long processId){

    }

}
