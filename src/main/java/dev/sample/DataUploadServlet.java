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

@WebServlet("/api/upload")
// 대용량 파일 업로드를 위한 설정 -> 추후 수정 예정
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 10, // 10MB
    maxFileSize = 1024 * 1024 * 50,       // 50MB
    maxRequestSize = 1024 * 1024 * 100    // 100MB
)
public class DataUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Part filePart = request.getPart("csvFile"); 
        
        if (filePart == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("파일이 업로드되지 않았습니다.");
            return;
        }

        int count = 0;
        int port = request.getServerPort();
        
        System.out.println("\n[" + port + " 서버] CSV 파일 수신 및 데이터 처리 시작...");

        RabbitMQProducer producer = new RabbitMQProducer();
        
        // 스트림 방식으로 한 줄씩 읽음
        try (InputStream fileContent = filePart.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(fileContent, StandardCharsets.UTF_8))) {
             
            String line;
            
            while ((line = reader.readLine()) != null) {
                //메세지큐로 데이터 보냄
            	producer.sendData(line);
            	
                count++;               
                
            }
            
            System.out.println("[" + port + " 서버] 총 " + count + "건의 데이터 전송 완료.\n");
            
        } catch (Exception e) {
            System.err.println("[" + port + " 서버] 처리 중 오류 발생: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // 성공 응답 전송
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(port + " 서버가 " + count + "건의 데이터를 받았습니다.");
    }
}