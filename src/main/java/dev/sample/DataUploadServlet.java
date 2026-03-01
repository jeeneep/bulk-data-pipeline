package dev.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/upload")
@MultipartConfig
public class DataUploadServlet extends HttpServlet {
	
	private static final Logger logger = LoggerFactory.getLogger(DataUploadServlet.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Part filePart = request.getPart("csvFile"); 
        
        if (filePart == null) {
        	logger.warn("Upload attempt failed: No 'csvFile' part found."); // 클라이언트 실수 추적
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("파일이 업로드되지 않았습니다.");
            return;
        }

        int count = 0;
        int port = request.getServerPort();
        long startTime = System.currentTimeMillis(); // 2. 성능 측정을 위한 시작 시간
        
        logger.info("[Port {}] CSV upload started. FileName: {}, Size: {} bytes", 
                port, filePart.getSubmittedFileName(), filePart.getSize());
        
        RabbitMQProducer producer = new RabbitMQProducer();
        
        // 스트림 방식으로 한 줄씩 읽음
        try (InputStream fileContent = filePart.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(fileContent, StandardCharsets.UTF_8))) {
             
            String line;
            
            while ((line = reader.readLine()) != null) {
                //메세지큐로 데이터 보냄
            	producer.sendData(line);
                count++;
                
                if (count % 10000 == 0) {  // 값 변경 가능, 로그 기록 최소화
                    logger.debug("[Port {}] Progress: {} rows sent to Queue.", port, count);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime; // 소요 시간 계산
            logger.info("[Port {}] Upload completed. Total: {} rows, Time: {}ms", port, count, duration);
            
        } catch (Exception e) {
        	logger.error("[Port {}] Error during CSV processing: ", port, e); // 예외 상세 기록
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // 성공 응답 전송
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(port + " 서버가 " + count + "건의 데이터를 받았습니다.");
    }
}
