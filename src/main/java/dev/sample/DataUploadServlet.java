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

// Nginx가 배분해주는 "/api/upload" 요청을 처리
@WebServlet("/api/upload")
// 대용량 파일 업로드를 위한 설정 
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 10, // 10MB
    maxFileSize = 1024 * 1024 * 50,       // 50MB
    maxRequestSize = 1024 * 1024 * 100    // 100MB
)
public class DataUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // 프론트엔드에서 전송할 때 사용할 파일명 "csvFile"
        Part filePart = request.getPart("csvFile"); 
        
        if (filePart == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("파일이 업로드되지 않았습니다.");
            return;
        }

        int count = 0;
        int port = request.getServerPort(); // 현재 실행 중인 서버 포트 (8080 또는 8090)
        
        System.out.println("\n[" + port + " 서버] CSV 파일 수신 및 데이터 처리 시작...");

        // BufferedReader를 사용하여 스트림 방식으로 한 줄씩 읽어 메모리를 보호
        try (InputStream fileContent = filePart.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(fileContent, StandardCharsets.UTF_8))) {
             
            String line;
            // 첫 줄이 컬럼 제목인 경우 스킵하려면 아래 주석을 해제
            // reader.readLine(); 
            
            while ((line = reader.readLine()) != null) {
                // TODO: 2단계에서 이곳에 메시지 큐(Producer)로 데이터를 보내는 로직이 들어갑니다.
                
                count++;
                
                // 진행 상황 확인을 위해 5만 건마다 로그 출력
                if (count % 50000 == 0) {
                    System.out.println("[" + port + " 서버] 현재 " + count + "건 읽기 완료...!!");
                }
            }
            
            System.out.println("[" + port + " 서버] 총 " + count + "건의 데이터를 성공적으로 수신했습니다.\n");
            
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