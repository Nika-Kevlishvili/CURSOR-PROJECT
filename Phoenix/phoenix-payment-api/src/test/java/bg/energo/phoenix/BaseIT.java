package bg.energo.phoenix;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

//@Testcontainers
//@SpringBootTest
//@AutoConfigureWebMvc
//@AutoConfigureMockMvc
//@Sql(scripts = {"classpath:cleanup.sql"}, executionPhase = AFTER_TEST_METHOD)
//public abstract class BaseIT {
//
//    @Autowired
//    protected MockMvc mockMvc;
//
//
//}
