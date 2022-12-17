package com.mzc.quiz.play.config;

import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    public static final String ENDPOINT = "/connect";
    public static final String TOPIC = "/pin/";
    public static final String DIRECT = "/queue/";
    public static final String PREFIX = "/quiz/";

    @Value("${spring.rabbitmq.host}")
    private String RabbitMQ_Host;
    @Value("${spring.rabbitmq.username}")
    private String RabbitMQ_ID;
    @Value("${spring.rabbitmq.password}")
    private String RabbitMQ_PW;
    @Value("${spring.rabbitmq.port}")
    private int RabbitMQ_Port;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ENDPOINT)
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new CustomHandshakeHandler())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes(PREFIX);
        config.enableSimpleBroker(TOPIC, DIRECT);

    }
}
