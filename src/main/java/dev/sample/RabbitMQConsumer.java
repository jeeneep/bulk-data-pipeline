package dev.sample;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class RabbitMQConsumer {
	private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);
    private final static String QUEUE_NAME = "card_data_queue";
    private final static int BATCH_SIZE = 1000;  // 5000건 단위로 db에 저장

    public void startConsume(DataSource ds) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        com.rabbitmq.client.Connection rabbitConn = factory.newConnection();
        com.rabbitmq.client.Channel channel = rabbitConn.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channel.basicQos(BATCH_SIZE);

        logger.info("RabbitMQ Consumer is ready. Waiting for messages...");
        
        List<String> buffer = new ArrayList<>();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            buffer.add(message);

            if (buffer.size() >= BATCH_SIZE) {
                saveToDB(ds, buffer);
                
                // db저장 후 해당 데이터들 큐에서 삭제
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), true);
                buffer.clear();
                logger.info("[Batch Success] {} records acknowledged and buffer cleared.", BATCH_SIZE);
            }
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
    }

    private void saveToDB(DataSource ds, List<String> dataList) {
    	long startTime = System.currentTimeMillis(); // 2. DB 작업 성능 측정
    	
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
                	logger.warn("Data format mismatch. Expected: 55, Actual: {}. Message: {}", columns.length, message);
                    continue; 
                }

                for (int i = 0; i < 55; i++) {
                    pstmt.setString(i + 1, columns[i].trim()); 
                }
                
                pstmt.addBatch();
            }

            int[] result = pstmt.executeBatch();
            conn.commit();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("DB Batch Inserted: {} rows, Time: {}ms, Pool: {}", result.length, duration, ds.getClass().getSimpleName());
            
        } catch (Exception e) {
        	logger.error("!!! Critical DB Error during batch insert: ", e);
        }
    }
}