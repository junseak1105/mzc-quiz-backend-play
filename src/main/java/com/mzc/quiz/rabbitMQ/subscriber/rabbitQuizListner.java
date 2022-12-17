package com.mzc.quiz.rabbitMQ.subscriber;

import com.mzc.quiz.play.entity.websocket.QuizMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.mzc.quiz.play.stompConfig.StompWebSocketConfig.TOPIC;

@Component
public class rabbitQuizListner {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;


    // RabbitMQ -> Spring -> React 보내는 WS 브로드캐스트
    @RabbitListener(queues = "#{autoDeleteQueue.name}")
    public void consumeMessage(QuizMessage quizMessage){
        System.out.println("**************** Rabbit MQ ******************");
        System.out.println("message Return : " + quizMessage);
        simpMessagingTemplate.convertAndSend(TOPIC + quizMessage.getPinNum(), quizMessage);
    }
}
