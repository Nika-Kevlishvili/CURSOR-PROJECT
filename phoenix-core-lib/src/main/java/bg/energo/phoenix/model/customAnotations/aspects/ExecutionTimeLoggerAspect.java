package bg.energo.phoenix.model.customAnotations.aspects;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExecutionTimeLoggerAspect {
    @Around("@annotation(ExecutionTimeLogger)")
    public Object executionTimeLogger(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            log.debug("""
                    \n
                    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                    {}: {} method execution started
                    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                    """, Thread.currentThread().getName(), joinPoint.getSignature());

            long startTime = System.currentTimeMillis();
            try {
                Object proceed = joinPoint.proceed();

                long executionTime = (System.currentTimeMillis() - startTime);
                log.debug("""
                        \n
                        <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                        {}: {} method was executed in {}
                        <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                        """, Thread.currentThread().getName(), joinPoint.getSignature(), DurationFormatUtils.formatDurationHMS(executionTime));
                return proceed;
            } catch (Throwable e) {
                long executionTime = (System.currentTimeMillis() - startTime);
                log.debug("""
                        \n
                        <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                        {}: {} method was executed in {}
                        <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                        """, Thread.currentThread().getName(), joinPoint.getSignature(), DurationFormatUtils.formatDurationHMS(executionTime));

                throw e;
            }
        } catch (Throwable e) {
            log.debug("{}: There was an error while calculating method execution time for {}", Thread.currentThread().getName(), joinPoint.getSignature(), e);
            throw e;
        }
    }
}
