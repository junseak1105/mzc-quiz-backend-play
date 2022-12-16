package com.mzc.quiz.play.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public static final String quizQueue = "quiz.queue.multi";
    public static final String quizExchange = "quizmulti.exchange";
    public static final String quizRoutingKey = "pin.#";

    @Bean
    public Queue quizQueue(){
        return new Queue(quizQueue, true);
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(quizExchange);
    }
    @Bean
    Binding quizBinding(Queue quizQueue, TopicExchange topicExchange){
        return BindingBuilder.bind(quizQueue).to(topicExchange).with(quizRoutingKey);
    }


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
