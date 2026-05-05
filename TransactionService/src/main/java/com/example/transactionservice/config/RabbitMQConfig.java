package com.example.transactionservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${account.queue.json.name}")
    private String JSON_QUEUE_NAME;

    @Value("${account.exchange.name}")
    private String EXCHANGE_NAME;

    @Value("${account.routing.json.key}")
    private String JSON_ROUTING_KEY;

    // Dead Letter Queue constants — must match AccountService declaration
    private static final String DLQ_SUFFIX = ".dlq";
    private static final String DLQ_EXCHANGE_SUFFIX = ".dlx";

    @Bean
    public Queue jsonQueueAccount() {
        return QueueBuilder.durable(JSON_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NAME + DLQ_EXCHANGE_SUFFIX)
                .withArgument("x-dead-letter-routing-key", JSON_ROUTING_KEY + DLQ_SUFFIX)
                .withArgument("x-message-ttl", 300000) // 5 minutes TTL
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(JSON_QUEUE_NAME + DLQ_SUFFIX).build();
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(EXCHANGE_NAME + DLQ_EXCHANGE_SUFFIX, true, false);
    }

    @Bean
    public Binding jsonBinding() {
        return BindingBuilder.bind(jsonQueueAccount()).to(exchange()).with(JSON_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(JSON_ROUTING_KEY + DLQ_SUFFIX);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
