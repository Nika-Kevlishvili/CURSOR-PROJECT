package bg.energo.phoenix.controller;

import bg.energo.phoenix.apis.service.ApisService;
import bg.energo.phoenix.model.entity.Configuration;
import bg.energo.phoenix.model.enums.sysconfig.ContractNumberPrefix;
import bg.energo.phoenix.model.enums.sysconfig.RfdChannelOptionType;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.ConfigurationRepository;
import bg.energo.phoenix.service.PermissionServiceMock;
import bg.energo.phoenix.service.document.ftpService.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

@Testcontainers
@SpringBootTest
@AutoConfigureWebMvc
@AutoConfigureMockMvc
@Sql(scripts = {"classpath:cleanup.sql"}, executionPhase = AFTER_TEST_METHOD)
public abstract class BaseIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    @Qualifier("fileServiceMock")
    protected FileService fileService;

    @Autowired
    protected ApisService apisService;

    @Autowired
    protected PermissionServiceMock permissionServiceMock;

    @Autowired
    protected TemplateRepository templateRepository;

    @Autowired
    protected ConfigurationRepository configurationRepository;

    @BeforeEach
    void setUpConfiguration() {
        // Ensure at least one configuration exists for tests
        if (configurationRepository.findLatestConfiguration().isEmpty()) {
            Configuration configuration = createDefaultConfiguration();
            configurationRepository.save(configuration);
        }
    }

    protected Configuration createDefaultConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setRfdCommunicationChannel(RfdChannelOptionType.REST);
        configuration.setBgMdwInvSendType(new ArrayList<>());
        configuration.setBgMdwInvPreOption(new ArrayList<>());
        configuration.setCommunicationDataValidation(new ArrayList<>());
        configuration.setSmsIntegration(new ArrayList<>());
        configuration.setVersionId(1L);
        configuration.setSystemUserId("test");
        configuration.setCreateDate(LocalDateTime.now());
        configuration.setContractNumberPrefix(ContractNumberPrefix.EPES); // PHN-3011: contract-number generation requires a configured prefix
        configuration.setCompanyId(5);
        return configuration;
    }

    static {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:11-3.3-alpine").asCompatibleSubstituteFor("postgres"))
                .withUsername("postgres")
                .withPassword("root")
                .withInitScript("schema.sql");
        postgres.start();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());

        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine"))
                .withExposedPorts(6379);
        redis.start();
        System.setProperty("spring.redis.host", redis.getHost());
        System.setProperty("spring.redis.port", redis.getMappedPort(6379).toString());
    }


    public static String asJsonString(final Object obj) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T asObject(String value, Class<T> clazz) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.readValue(value, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String fromResourceToString(Resource resource) {
        try {
            return IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't read resource from: " + resource.getFilename());
        }
    }
}
