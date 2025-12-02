package bg.energo.phoenix.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestResponseLog {

    LocalDateTime requestTime;
    LocalDateTime responseTime;
    String uri;
    Long timeTook;
}

