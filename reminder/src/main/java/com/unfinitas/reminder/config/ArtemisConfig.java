package com.unfinitas.reminder.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@EnableJms
public class ArtemisConfig {

    private static final String BROKER_URL = "tcp://86.50.252.76:61616";

    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    @Primary
    public ConnectionFactory producerConnectionFactory() throws Exception {
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
        factory.setUser("anycast-producer");
        factory.setPassword("tamkswarchap");
        return factory;
    }

    @Bean
    public JmsTemplate jmsTemplate(final ConnectionFactory producerConnectionFactory) {
        return new JmsTemplate(producerConnectionFactory);
    }

    @Bean
    public ConnectionFactory consumerConnectionFactory() throws Exception {
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
        factory.setUser("anycast-consumer");
        factory.setPassword("tamkswarchac");
        return factory;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() throws Exception {
        final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(consumerConnectionFactory());
        return factory;
    }
}
