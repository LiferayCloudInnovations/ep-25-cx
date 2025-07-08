package com.liferay.sample;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

/**
 * @author Marcel Tanuri
 */
@Component
public class RabbitMqMessageListener {

    @RabbitListener(queues = "liferay/background_task")
    public void receive(String message) {
        System.out.println("Received message: " + message);
    }
}
