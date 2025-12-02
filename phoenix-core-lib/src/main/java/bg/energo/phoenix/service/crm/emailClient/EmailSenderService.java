package bg.energo.phoenix.service.crm.emailClient;

import bg.energo.common.utils.JsonUtils;
import bg.energo.mass_comm.client.SendEmailRequestBuilder;
import bg.energo.mass_comm.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class EmailSenderService {

    private final EmailCommunicationClient emailCommunicationClient;
    private final String senderEmailAddress;


    public EmailSenderService(
            EmailCommunicationClient emailCommunicationClient,
            @Value("${app.email.sender_email_address}") String senderEmailAddress
    ) {
        this.emailCommunicationClient = emailCommunicationClient;
        this.senderEmailAddress = senderEmailAddress;
    }

    public void sendTestEmail(String recipientEmailAddress) {
        final SendEmailRequest emailRequest = SendEmailRequestBuilder
                .newBuilder()
                .withPriority(EmailPriority.NORMAL)
                .setFromAddress(senderEmailAddress)
                .setFromEws("EWS1")
                .addToRecepient(recipientEmailAddress)
                .withSubject("Test Email")
                .setBody(new Body(
                        "text/html", "UTF-8", "This is a test email.".getBytes()
                ))
                .addAttachment(
                        "test_attachment.txt", "text/plain", "This is a test attachment.".getBytes()
                ).build();

        try {
            SendEmailResponse sendEmailResponse = emailCommunicationClient.sendEmail(emailRequest);
            System.out.printf("Email response:\n%s\n", JsonUtils.toJson(sendEmailResponse));
            fetchTaskStatus(sendEmailResponse.getTaskId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<SendEmailResponse> sendEmail(
            String recipientEmailAddress,
            String subject,
            String body,
            List<Attachment> attachments
    ) {
        final SendEmailRequest emailRequest = SendEmailRequestBuilder
                .newBuilder()
                .withPriority(EmailPriority.NORMAL)
                .setFromAddress(senderEmailAddress)
                .setFromEws("EWS1")
                .addToRecepient(recipientEmailAddress)
                .withSubject(subject)
                .setBody(new Body("text/html", "UTF-8", body.getBytes()))
                .build();

        emailRequest.setAttachments(attachments);

        SendEmailResponse sendEmailResponse = null;
        try {
            sendEmailResponse = emailCommunicationClient.send(emailRequest);
        } catch (Exception e) {
            log.error("Sending email failed, cause: {} ", e.getMessage());
        }
        if (sendEmailResponse != null) {
            System.out.printf("Email response:\n%s\n", JsonUtils.toJson(sendEmailResponse));
            fetchTaskStatus(sendEmailResponse.getTaskId());
        }
        return Optional.ofNullable(sendEmailResponse);
    }

    public Optional<SendEmailResponse> sendEmailFrom(
            String recipientEmailAddress,
            String subject,
            String body,
            String emailSenderAddress,
            List<Attachment> attachments
    ) {
        final SendEmailRequest emailRequest = SendEmailRequestBuilder
                .newBuilder()
                .withPriority(EmailPriority.NORMAL)
                .setFromAddress(emailSenderAddress)
                .setFromEws("EWS1")
                .addToRecepient(recipientEmailAddress)
                .withSubject(subject)
                .setBody(new Body("text/html", "UTF-8", body.getBytes()))
                .build();

        emailRequest.setAttachments(attachments);

        SendEmailResponse sendEmailResponse = null;
        try {
            sendEmailResponse = emailCommunicationClient.send(emailRequest);
        } catch (Exception e) {
            log.error("Sending email failed, cause: {} ", e.getMessage());
        }
        if (sendEmailResponse != null) {
            System.out.printf("Email response:\n%s\n", JsonUtils.toJson(sendEmailResponse));
            fetchTaskStatus(sendEmailResponse.getTaskId());
        }
        return Optional.ofNullable(sendEmailResponse);
    }


    private Optional<TaskStatusResponse> fetchTaskStatus(UUID taskId) {
        TaskStatusResponse taskStatusResponse = null;
        TaskStatusRequest taskStatusRequest = new TaskStatusRequest();

        log.debug("Fetching task status for taskId: {}", taskId);

        taskStatusRequest.addTask(taskId);

        try {
            log.debug("Sending request to fetch task status for taskId: {}", taskId);

            taskStatusResponse = emailCommunicationClient.fetchTaskStatus(taskStatusRequest);

            log.debug("Successfully fetched task status for taskId: {}", taskId);
        } catch (Exception e) {
            log.error("Failed to fetch task status for taskId: {}, cause: {}", taskId, e.getMessage(), e);
        }

        if (taskStatusResponse != null) {
            log.debug("Task status response is present for taskId: {}", taskId);
        } else {
            log.warn("Task status response is empty for taskId: {}", taskId);
        }

        return Optional.ofNullable(taskStatusResponse);
    }

    public Optional<TaskStatusResponse> fetchTaskStatus(String taskId) {
        TaskStatusResponse taskStatusResponse = null;
        TaskStatusRequest taskStatusRequest = new TaskStatusRequest();

        log.debug("Fetching task status for taskId: {}", taskId);

        taskStatusRequest.addTask(UUID.fromString(taskId));

        try {
            log.debug("Sending request to fetch task status...");
            taskStatusResponse = emailCommunicationClient.fetchTaskStatus(taskStatusRequest);
            log.debug("Successfully fetched task status for taskId: {}", taskId);
        } catch (Exception e) {
            log.error("Failed to fetch task status for taskId: {}, cause: {}", taskId, e.getMessage(), e);
        }

        if (taskStatusResponse != null) {
            log.debug("Task status response is present for taskId: {}. Full Response: {}", taskId, taskStatusResponse);
        } else {
            log.debug("Task status response is empty for taskId: {}", taskId);
        }

        return Optional.ofNullable(taskStatusResponse);
    }

    public Optional<TaskStatusResponse> fetchContactStatuses(List<UUID> taskUUIDs) {
        TaskStatusResponse taskStatusResponse = null;
        TaskStatusRequest taskStatusRequest = new TaskStatusRequest();

        log.info("Fetching task statuses for taskUUIDs: {}", taskUUIDs);

        taskStatusRequest.addTasks(taskUUIDs);

        try {
            log.debug("Sending request to fetch task statuses for taskUUIDs: {}", taskUUIDs);

            taskStatusResponse = emailCommunicationClient.fetchTaskStatus(taskStatusRequest);

            log.debug("Successfully fetched task statuses for taskUUIDs: {}. Response: {}", taskUUIDs, taskStatusResponse);
        } catch (Exception e) {
            log.error("Failed to fetch task statuses for taskUUIDs: {}, cause: {}", taskUUIDs, e.getMessage(), e);
        }

        if (taskStatusResponse != null) {
            log.debug("Task status response is present for taskUUIDs: {}. Full Response: {}", taskUUIDs, taskStatusResponse);
        } else {
            log.debug("Task status response is empty for taskUUIDs: {}", taskUUIDs);
        }

        return Optional.ofNullable(taskStatusResponse);
    }


    private void manageTask(final UUID taskId, TaskCommand cmd) throws Exception {
        // Manage task
        final TaskManageRequest taskManageRequest = new TaskManageRequest(taskId, cmd);
        final TaskManageResponse taskManageResponse = emailCommunicationClient.manageTask(taskManageRequest);
        System.out.printf("TaskManageResponse:\n%s\n", JsonUtils.toJson(taskManageResponse));

    }
}
