package in.skillsync.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Notification Service.
 *
 * Exchange:  skillsync.events   (TopicExchange, durable)
 * Queue:     skillsync.notification.queue   (durable)
 * Binding:   session.*   routes all session events to the notification queue
 *
 * Jackson converter ensures all messages are serialized/deserialized as JSON.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE          = "skillsync.events";
    public static final String NOTIFICATION_QUEUE = "skillsync.notification.queue";
    public static final String ROUTING_PATTERN   = "session.#";

    @Bean
    public TopicExchange skillSyncExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue,
                                       TopicExchange skillSyncExchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(skillSyncExchange)
                .with(ROUTING_PATTERN);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter());
        return template;
    }
}
