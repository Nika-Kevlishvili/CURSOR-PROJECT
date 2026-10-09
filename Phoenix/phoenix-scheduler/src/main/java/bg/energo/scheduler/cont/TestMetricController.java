package bg.energo.scheduler.cont;

import bg.energo.scheduler.config.SchedulingConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scheduled/tasks")
public class TestMetricController {
    @GetMapping
    @Operation(security = @SecurityRequirement(name = "bearer-token"))
    public Integer getActive(){
        return SchedulingConfig.scheduler.getActiveCount();
    }
}
