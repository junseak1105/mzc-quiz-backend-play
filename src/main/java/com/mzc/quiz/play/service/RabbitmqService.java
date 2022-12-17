package com.mzc.quiz.play.service;

import com.mzc.quiz.play.model.websocket.QuizMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.mzc.quiz.play.config.RabbitConfig.*;

@Service
@RequiredArgsConstructor
public class RabbitmqService {


    private final AmqpTemplate amqpTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final FanoutExchange fanout;

    public void publishQuizMessage(QuizMessage quizMessage){

        System.out.println("pub Message");
        System.out.println(fanout.getName());
//        rabbitTemplate.convertAndSend(fanout.getName(),"", quizMessage);
        amqpTemplate.convertAndSend(fanout.getName(), "", quizMessage);

    }



}
