package bg.energo.phoenix.security;

import bg.energo.phoenix.apis.service.ApisService;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.rabbit.impl.*;
import bg.energo.phoenix.service.crm.emailClient.EmailSenderServiceInterface;
import bg.energo.phoenix.service.crm.smsCommunication.SmsSenderServiceInterface;
import bg.energo.phoenix.service.document.ftpService.FileService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class TestSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authz) -> authz
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(withDefaults());
        return http.build();
    }

    @Bean("massImportRabbitMQProducer")
    @Primary
    public MassImportRabbitMQProducerService massImportRabbitMQProducerServiceMock() {
        return Mockito.mock(MassImportRabbitMQProducerService.class);
    }

    @Bean("billingResumeRabbitMQProducerService")
    @Primary
    public BillingResumeRabbitMQProducerService billingResumeRabbitMQProducerService() {
        return Mockito.mock(BillingResumeRabbitMQProducerService.class);
    }

    @Bean("billingRunGenerationRabbitMQProducer")
    @Primary
    public BillingRunGenerationRabbitMQProducerService billingRunGenerationRabbitMQProducerService() {
        return Mockito.mock(BillingRunGenerationRabbitMQProducerService.class);
    }

    @Bean("billingRunAccountingRabbitMQProducer")
    @Primary
    public BillingRunAccountingRabbitMQProducerService billingRunAccountingRabbitMQProducerService() {
        return Mockito.mock(BillingRunAccountingRabbitMQProducerService.class);
    }

    @Bean("apReportRabbitMQProducer")
    @Primary
    public APReportRabbitMQProducerService apReportRabbitMQProducerService() {
        return Mockito.mock(APReportRabbitMQProducerService.class);
    }

    @Bean
    @Primary
    public FileService fileServiceMock() {
        return Mockito.mock(FileService.class);
    }

    @Bean
    @Primary
    public ApisService apisServiceMock() {
        return Mockito.mock(ApisService.class);
    }

    @Bean
    @Primary
    public TemplateRepository templateRepositoryMock() {
        return Mockito.mock(TemplateRepository.class);
    }

    @Bean
    @Primary
    public SimpMessagingTemplate simpMessagingTemplateMock() {
        return Mockito.mock(SimpMessagingTemplate.class);
    }

    @Bean
    @Primary
    public EmailSenderServiceInterface emailSenderServiceInterface() {
        return Mockito.mock(EmailSenderServiceInterface.class);
    }

    @Bean
    @Primary
    public SmsSenderServiceInterface smsSenderServiceInterface()  {
        return Mockito.mock(SmsSenderServiceInterface.class);
    }
}
