package com.mzc.quiz.Test.controller;

import com.mzc.quiz.play.entity.websocket.QuizMessage;
import com.mzc.quiz.rabbitMQ.publisher.RabbitmqService;
import com.mzc.quiz.Test.service.Test_WS_RedisService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Log4j2
public class Test_WS_RedisController {

    @Autowired
    Test_WS_RedisService testWSRedisService;

    @Autowired
    RabbitmqService rabbitmqService;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;


    @MessageMapping("/session/test")
    public void sessionSampleCode(@RequestBody QuizMessage quizMessage, Principal principal){
        String user = principal.getName();
        String retUrl = "/queue/"+quizMessage.getPinNum();
        simpMessagingTemplate.convertAndSendToUser(user, retUrl, "123123123123");
    }

    @MessageMapping("/Test")
    public void rabbitMQTest(@RequestBody QuizMessage quizMessage, Principal principal) {
       rabbitmqService.publishQuizMessage(quizMessage);
    }

}
