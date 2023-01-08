package com.mzc.quiz.Test.service;

import com.mzc.quiz.play.entity.websocket.QuizMessage;
import com.mzc.quiz.global.redisUtil.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class Test_WS_RedisService {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    private static final String PIN_QUEUE_NAME = "quiz.queue";
    private static final String PIN_EXCHANGE_NAME = "quiz.exchange";
    private static final String ROUTING_KEY_PREFIX = "pin.";
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    RedisUtil redisUtil;


    public void RabbitMQTest(QuizMessage quizMessage){
        String RoutingKey = ROUTING_KEY_PREFIX + quizMessage.getPinNum();
        String EXName = quizMessage.getNickName();
        System.out.println(EXName);
        System.out.println(RoutingKey);
        rabbitTemplate.convertAndSend(EXName, RoutingKey, quizMessage);
    }

    public void WebsocketTest(int pin, QuizMessage quizMessage){
       log.info("pin - "+pin);
       log.info("message : "+quizMessage);
    }

    public void RedisTest(int pin, QuizMessage quizMessage){
        ValueOperations<String, String> vop = redisTemplate.opsForValue();
        String key = "pin:"+pin;
        String value = quizMessage.getNickName();
        vop.set(key, value);
//        log.info("quiz message : "+quizMessage.getContent());
        log.info("Redis get " + vop.get(key));
    }

    public void RedisUtilTest(int pin, QuizMessage quizMessage){
        String key = "UserList:"+pin;
        String value = quizMessage.getNickName();
        redisUtil.SET(key, value);

        log.info("Redis get :" + redisUtil.getData(key));
    }
}
