package com.mzc.quiz.play.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.amqp.core.*;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableRabbit
public class RabbitConfig {
    @Value("${spring.rabbitmq.host}")
    private String RabbitMQ_Host;
    @Value("${spring.rabbitmq.username}")
    private String RabbitMQ_ID;
    @Value("${spring.rabbitmq.password}")
    private String RabbitMQ_PW;
    @Value("${spring.rabbitmq.port}")
    private int RabbitMQ_Port;

    @Value("${queue-name}")
    private String quizQueue;
//    public static final String quizQueue; // + RandomStringUtils.randomNumeric(6);


    public static final String quizExchange = "test.fanout";
    public static final String quizRoutingKey = "pin.*";


    @Bean
    public FanoutExchange fanoutExchange(){
        return new FanoutExchange(quizExchange);
    }

    @Bean
    public Queue autoDeleteQueue1() {
        return new AnonymousQueue();
    }
    @Bean
    public Binding binding1(FanoutExchange fanoutExchange,
                            Queue autoDeleteQueue1) {
        return BindingBuilder.bind(autoDeleteQueue1).to(fanoutExchange);
    }

//    @Bean
//    Binding quizBinding(Queue quizQueue, FanoutExchange fanoutExchange){
//        return BindingBuilder.bind(quizQueue).to(fanoutExchange);
//    }

//    @Bean
//    public TopicExchange topicExchange(){
//        return new TopicExchange(quizExchange);
//    }
//    @Bean
//    Binding quizBinding(Queue quizQueue, TopicExchange topicExchange){
//        return BindingBuilder.bind(quizQueue).to(topicExchange).with(quizRoutingKey);
//    }


    @Bean
    public  com.fasterxml.jackson.databind.Module dateTimeModule() {
        return new JavaTimeModule();
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        //LocalDateTime serializable 을 위해
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        objectMapper.registerModule(dateTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
