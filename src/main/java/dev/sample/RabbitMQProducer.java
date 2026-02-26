package dev.sample;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;

public class RabbitMQProducer {
    // 메시지가 쌓일 큐
    private final static String QUEUE_NAME = "card_data_queue";

    public void sendData(String message) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); 

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            
            // 큐가 없으면 생성
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            
            // CSV 한 줄(message)을 큐로 전송
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            
        } catch (Exception e) {
            System.err.println("[RabbitMQ Error] 메시지 전송 실패: " + e.getMessage());
        }
    }
}