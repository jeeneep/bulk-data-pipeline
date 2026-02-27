package dev.sample;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;

public class RabbitMQProducer {
    private final static String QUEUE_NAME = "card_data_queue";
    private Connection connection;
    private Channel channel;

    // 생성자에서 연결을 미리 한 번만 맺어둡니다.
    public RabbitMQProducer() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            this.channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendData(String message) {
        try {
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("[RabbitMQ Error] 전송 실패: " + e.getMessage());
        }
    }

    // 연결을 닫음
    public void close() {
        try { if(channel != null) channel.close(); if(connection != null) connection.close(); } 
        catch (Exception e) { e.printStackTrace(); }
    }
}