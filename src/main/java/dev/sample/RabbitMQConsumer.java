package dev.sample;

import com.rabbitmq.client.*;
import java.sql.Connection; 
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class RabbitMQConsumer {
    private final static String QUEUE_NAME = "card_data_queue";
    private final static int BATCH_SIZE = 1000;  // 1000건 단위로 db에 저장

    public void startConsume(DataSource ds) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        com.rabbitmq.client.Connection rabbitConn = factory.newConnection();
        com.rabbitmq.client.Channel channel = rabbitConn.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channel.basicQos(BATCH_SIZE);

        List<String> buffer = new ArrayList<>();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            buffer.add(message);

            if (buffer.size() >= BATCH_SIZE) {
                saveToDB(ds, buffer);
                
                // db저장 후 해당 데이터들 큐에서 삭제
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), true);
                buffer.clear();
                System.out.println("[Consumer] " + BATCH_SIZE + "건 DB 적재 완료!");
            }
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
    }

    private void saveToDB(DataSource ds, List<String> dataList) {
        // 추후 수정 예정
        String sql = "INSERT INTO card_table (data_column) VALUES (?)";
        
        try (java.sql.Connection conn = ds.getConnection(); // 명시적으로 java.sql 기입
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false); 

            for (String data : dataList) {
                pstmt.setString(1, data);
                pstmt.addBatch(); 
            }

            pstmt.executeBatch(); 
            conn.commit();        
            
        } catch (Exception e) {
            System.err.println("DB 저장 오류: " + e.getMessage());
        }
    }
}