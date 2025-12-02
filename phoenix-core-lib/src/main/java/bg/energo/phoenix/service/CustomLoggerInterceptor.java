package bg.energo.phoenix.service;

import bg.energo.phoenix.model.RequestResponseLog;
import bg.energo.phoenix.model.principal.EnergoProPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
@Slf4j
public class CustomLoggerInterceptor implements HandlerInterceptor {

    private final ThreadLocal<RequestResponseLog> requestResponseLog;

    public CustomLoggerInterceptor() {
        requestResponseLog=new ThreadLocal<>();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication!=null){
            MDC.put("User",((EnergoProPrincipal) authentication).getApplicationUser().getId());
            MDC.put("RequestId", UUID.randomUUID().toString());
        }
        RequestResponseLog myLog = new RequestResponseLog();
        myLog.setRequestTime(LocalDateTime.now());
        myLog.setUri(request.getRequestURI());
        requestResponseLog.set(myLog);
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        RequestResponseLog myLog = requestResponseLog.get();
        myLog.setResponseTime(LocalDateTime.now());
        myLog.setTimeTook(ChronoUnit.MILLIS.between(myLog.getRequestTime(),myLog.getResponseTime()));
        log.debug("Response time log: {}",myLog);
        requestResponseLog.remove();
    }
}

