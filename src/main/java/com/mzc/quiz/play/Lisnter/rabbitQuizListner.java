package com.mzc.quiz.play.Lisnter;

import com.mzc.quiz.play.model.websocket.QuizMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.mzc.quiz.play.config.StompWebSocketConfig.TOPIC;

@Component
public class rabbitQuizListner {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;


    @RabbitListener(queues = "#{autoDeleteQueue1.name}")
    public void consumeMessage(QuizMessage quizMessage){
        System.out.println("**************** Rabbit MQ ******************");
        System.out.println("message Return : " + quizMessage);
        simpMessagingTemplate.convertAndSend(TOPIC + quizMessage.getPinNum(), quizMessage);
    }
}
