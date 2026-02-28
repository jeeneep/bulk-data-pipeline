package dev.sample;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQProducer {
	private static final Logger logger = LoggerFactory.getLogger(RabbitMQProducer.class);
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

			logger.debug("RabbitMQ Connection & Channel established. Queue: {}", QUEUE_NAME);
		} catch (Exception e) {
			logger.error("Failed to establish RabbitMQ connection: ", e);
		}
	}

	public void sendData(String message) {
		try {
			channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			logger.error("[RabbitMQ Error] Publish failed for message snippet: {}... Error: ",
					(message.length() > 20 ? message.substring(0, 20) : message), e);
		}
	}

	// 연결을 닫음
	public void close() {
		try {
			if (channel != null)
				channel.close();
			if (connection != null)
				connection.close();
			logger.info("RabbitMQ Producer connection closed safely."); // 4. 자원 해제 확인
		} catch (Exception e) {
			logger.warn("Error while closing RabbitMQ connection: ", e);
		}
	}
}