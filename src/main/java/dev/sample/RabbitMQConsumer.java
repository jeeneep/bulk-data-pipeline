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

        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO CARD_TRANSACTION VALUES (");
        for (int i = 0; i < 55; i++) {
            sqlBuilder.append("?");
            if (i < 54) sqlBuilder.append(", ");
        }
        sqlBuilder.append(")");
        
        String sql = sqlBuilder.toString();
        
        try (java.sql.Connection conn = ds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false); 

            for (String message : dataList) {
                // CSV 한 줄을 콤마로 분리 (55개의 데이터가 들어있어야 함)
                String[] columns = message.split(",");
                
                // 데이터 개수가 55개가 맞는지 검증 (에러 방지용)
                if (columns.length != 55) {
                    // 만약 개수가 다르면 로그만 찍고 넘어감 (필요시 수정)
                    System.err.println("데이터 개수 불일치! 기대: 55, 실제: " + columns.length);
                    continue; 
                }

                for (int i = 0; i < 55; i++) {
                    pstmt.setString(i + 1, columns[i].trim()); 
                }
                
                pstmt.addBatch(); 
            }

            pstmt.executeBatch(); 
            conn.commit();        
            System.out.println(">>> [성공] 팀원 DB에 " + dataList.size() + "건 적재 완료!");
            
        } catch (Exception e) {
            System.err.println("!!! DB 저장 오류: " + e.getMessage());
            e.printStackTrace(); // 자세한 에러 원인 확인을 위해 추가
        }
    }
}