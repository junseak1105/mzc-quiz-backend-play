package com.mzc.quiz.rabbitMQ.publisher;

import com.mzc.quiz.play.entity.websocket.QuizMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitmqService {


    private final AmqpTemplate amqpTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final FanoutExchange fanout;


    // React -> Spring -> RabbitMQ
    public void publishQuizMessage(QuizMessage quizMessage){

        System.out.println("pub Message");
        System.out.println(fanout.getName());
//        rabbitTemplate.convertAndSend(fanout.getName(),"", quizMessage);
        amqpTemplate.convertAndSend(fanout.getName(), "", quizMessage);

    }



}
