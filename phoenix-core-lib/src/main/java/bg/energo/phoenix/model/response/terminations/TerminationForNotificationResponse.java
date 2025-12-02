package bg.energo.phoenix.model.response.terminations;

public interface TerminationForNotificationResponse {
    Long getCommunicationId();

    Long getCustomerDetailId();

    String getCommContactIds();

    String getEmails();

    Long getTemplateId();

    Long getActionId(); //todo remove after testing

    Long getTerminationId(); //todo remove after testing

    String getEmailSubject();
}
