package com.mzc.quiz.global.config.exception;

import com.mzc.quiz.global.Response.DefaultRes;
import com.mzc.quiz.global.Response.ResponseMessages;
import com.mzc.quiz.global.Response.StatusCode;
import lombok.extern.log4j.Log4j2;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Log4j2
public class CommonExceptionAdvice {

    /**
     * Exception Handler를 추가하려면 아래의 Exception을 제거하고 처리할 Exception 별로 처리문을 만듭니다.
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    protected DefaultRes All_Exception(Exception ex, Model model) {
        log.error("DefaultRes............" + ex.getMessage());
        log.error(model);
        return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessages.BAD_REQUEST);
    }

}
