package dev.sample.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.sample.RabbitMQProducer;

@Service 
public class CsvUploadService {

    private static final Logger logger = LoggerFactory.getLogger(CsvUploadService.class);
    
    private final RabbitMQProducer producer = new RabbitMQProducer();

    // 파일 받아서 큐에 전달
    public int processData(InputStream fileContent, int port) throws Exception {
        int count = 0;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileContent, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                producer.sendData(line); 
                count++;
                
                if (count % 10000 == 0) {
                    logger.debug("[Port {}] Progress: {} rows sent.", port, count);
                }
            }
        }
        return count; 
    }
}