package bg.energo.phoenix.service.crm.emailClient;

import bg.energo.common.utils.JsonUtils;
import bg.energo.mass_comm.models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class EmailCommunicationClient {
    private final String sendEmailEndpoint;
    private final String sendEmailStatusEndpoint;
    private final String sendEmailManageEndpoint;
    private final String backendBaseUrl;
    private final EmailHttpClient httpClient;
    private final Map<String, String> headers;

    public EmailCommunicationClient(
            @Value("${app.email.send_email_endpoint}") String sendEmailEndpoint,
            @Value("${app.email.send_email_status_endpoint}") String sendEmailStatusEndpoint,
            @Value("${app.email.send_email_manage_endpoint}") String sendEmailManageEndpoint,
            @Value("${app.email.service_base_url}") String backendBaseUrl,
            @Value("${app.email.security_header}") String securityHeaderName,
            @Value("${app.email.security_key}") String securityKey) {

        this.sendEmailEndpoint = sendEmailEndpoint;
        this.sendEmailStatusEndpoint = sendEmailStatusEndpoint;
        this.sendEmailManageEndpoint = sendEmailManageEndpoint;

        if (backendBaseUrl.endsWith("/")) {
            this.backendBaseUrl = backendBaseUrl.substring(0, backendBaseUrl.length() - 1);
        } else {
            this.backendBaseUrl = backendBaseUrl;
        }

        this.httpClient = new EmailHttpClient();
        this.headers = new HashMap<>();
        if (securityKey != null && !securityKey.isEmpty()) {
            this.headers.put(securityHeaderName, securityKey);
        }
    }

    public SendEmailResponse send(SendEmailRequest request) throws Exception {
        byte[] body = JsonUtils.toJsonAsBytes(request);
        byte[] response = this.httpClient.post(this.constructURI(sendEmailEndpoint), this.headers, body);
        return JsonUtils.fromJson(response, SendEmailResponse.class);
    }


    public SendEmailResponse sendEmail(SendEmailRequest request) throws Exception {
        byte[] body = JsonUtils.toJsonAsBytes(request);
        byte[] response = this.httpClient.post(this.constructURI(sendEmailEndpoint), this.headers, body);
        return JsonUtils.fromJson(response, SendEmailResponse.class);
    }

    public TaskStatusResponse fetchTaskStatus(TaskStatusRequest request) throws Exception {
        byte[] body = JsonUtils.toJsonAsBytes(request);
        byte[] response = this.httpClient.post(this.constructURI(sendEmailStatusEndpoint), this.headers, body);
        return JsonUtils.fromJson(response, TaskStatusResponse.class);
    }

    public TaskManageResponse manageTask(TaskManageRequest request) throws Exception {
        byte[] body = JsonUtils.toJsonAsBytes(request);
        byte[] response = this.httpClient.post(this.constructURI(sendEmailManageEndpoint), this.headers, body);
        return JsonUtils.fromJson(response, TaskManageResponse.class);
    }

    private URI constructURI(String endpoint) {
        return URI.create(this.backendBaseUrl + endpoint);
    }
}

