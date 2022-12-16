package com.mzc.quiz.play.service;

import com.mzc.quiz.play.model.websocket.QuizMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import static com.mzc.quiz.play.config.RabbitConfig.*;

@Service
@RequiredArgsConstructor
public class RabbitmqService {


    private final AmqpTemplate amqpTemplate;

    public void publishQuizMessage(QuizMessage quizMessage){

        System.out.println("pub Message");
        amqpTemplate.convertAndSend(quizExchange, quizRoutingKey, quizMessage);
    }



}
