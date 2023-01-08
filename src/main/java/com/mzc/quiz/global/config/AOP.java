package com.mzc.quiz.global.config;

import com.mzc.quiz.global.Response.DefaultRes;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Log4j2
@Component
public class AOP {

    @Before("execution(* com.mzc.quiz..controller..*.*(..))")
    public void logBefore() {
    }

    @AfterReturning(pointcut = "execution(* com.mzc.quiz..controller..*.*(..)))", returning = "result")
    public void logReturn(DefaultRes result) {
        log.info("| [statuscode]: " + result.getStatusCode());
        log.info("| [responsemessage]: " + result.getResponseMessage());
        log.info("| [data]: " + result.getData());
    }

    @Around("execution(* com.mzc.quiz..controller..*.*(..))")
    public Object logTime(ProceedingJoinPoint pjp) {
        log.info("=================Request Start==================");
        long start = System.currentTimeMillis();

        log.info("| [Target]: " + pjp.getTarget());
        log.info("| [Param]: " + Arrays.toString(pjp.getArgs()));


        //invoke method
        Object result = null;

        try {
            result = pjp.proceed();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();

        log.info("| [TIME]: "  + (end - start));

        log.info("=================Request End====================");
        return result;
    }


}
