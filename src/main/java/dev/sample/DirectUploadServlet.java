package dev.sample;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.sql.DataSource;

@WebServlet("/api/upload-direct") // 기존 MQ 버전과 주소가 다름!
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 10, // 10MB
    maxFileSize = 1024 * 1024 * 50,       // 50MB
    maxRequestSize = 1024 * 1024 * 100    // 100MB
)
public class DirectUploadServlet extends HttpServlet {

    private final static int BATCH_SIZE = 1000; // 1000건씩 묶어서 Insert

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, java.io.IOException {
        
        Part filePart = request.getPart("csvFile"); 
        if (filePart == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("파일이 업로드되지 않았습니다.");
            return;
        }

        int count = 0;
        int port = request.getServerPort();
        long startTime = System.currentTimeMillis(); // ⏱️ 시작 시간 기록

        System.out.println("\n[" + port + " 서버] Direct DB 적재 시작...");

        // 1. Context에서 DB 커넥션 풀(HikariCP) 가져오기
        DataSource ds = ApplicationContextListener.getDataSource(getServletContext());

        // 2. Insert SQL문 생성 (컬럼 55개)
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO CARD_TRANSACTION VALUES (");
        for (int i = 0; i < 55; i++) {
            sqlBuilder.append("?");
            if (i < 54) sqlBuilder.append(", ");
        }
        sqlBuilder.append(")");
        String sql = sqlBuilder.toString();

        // 3. 파일 읽기 및 DB Insert 준비
        try (InputStream fileContent = filePart.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(fileContent, StandardCharsets.UTF_8));
             Connection conn = ds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            conn.setAutoCommit(false); // 수동 커밋 모드로 전환 (배치 처리 필수)
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                
                if (columns.length != 55) continue; // 데이터 개수 안 맞으면 스킵
                
                for (int i = 0; i < 55; i++) {
                    pstmt.setString(i + 1, columns[i].trim()); 
                }
                
                pstmt.addBatch(); // 쿼리를 메모리에 담기
                count++;
                
                // 5,000건이 모이면 DB에 한 번에 쏘고 커밋
                if (count % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    pstmt.clearBatch(); // 쏘고 나서 메모리 비우기
                    System.out.println("[" + port + " 서버] " + count + "건 Direct 적재 중...");
                }
            }
            
            // 반복문이 끝난 후, 5000으로 안 떨어지고 남은 찌꺼기 데이터들 마저 쏘기
            if (count % BATCH_SIZE != 0) {
                pstmt.executeBatch();
                conn.commit();
            }
            
            long endTime = System.currentTimeMillis(); // ⏱️ 종료 시간 기록
            double executeTime = (endTime - startTime) / 1000.0; // 초 단위 변환
            
            String resultMsg = "[" + port + " 서버] 총 " + count + "건 Direct DB 저장 완료! (소요시간: " + executeTime + "초)";
            System.out.println(resultMsg + "\n");
            
            // 모든 작업이 '끝난 후'에야 브라우저에 성공 응답 전송
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(resultMsg);

        } catch (Exception e) {
            System.err.println("[" + port + " 서버] Direct 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("서버 오류가 발생했습니다.");
        }
    }
}