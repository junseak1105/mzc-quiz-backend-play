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
    public static final String quizExchange = "quiz.fanout";
    public static final String quizRoutingKey = "";


    @Bean
    public FanoutExchange fanoutExchange(){
        return new FanoutExchange(quizExchange);
    }

    @Bean
    public Queue autoDeleteQueue() {
        return new AnonymousQueue();
    }
    @Bean
    public Binding binding(FanoutExchange fanoutExchange, Queue autoDeleteQueue) {
        return BindingBuilder.bind(autoDeleteQueue).to(fanoutExchange);
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
