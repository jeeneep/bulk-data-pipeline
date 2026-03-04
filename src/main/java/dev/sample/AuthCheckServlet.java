package dev.sample;

import dev.sample.service.AuthService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@WebServlet("/auth")
public class AuthCheckServlet extends HttpServlet {

    private AuthService authService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        // 에러 나던 WebApplicationContextUtils 코드를 지우고, 
        // 우리가 알던 익숙한 방식으로 직접 스프링 설정 파일(xml)을 읽어옵니다.
        // (괄호 안의 파일명은 실제 만들어두신 빈 설정 파일명으로 맞춰주세요)
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        
        // 스프링 컨테이너에서 빈을 찾아 주입
        this.authService = context.getBean(AuthService.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        
        int currentPort = req.getLocalPort();
        
        // 비즈니스 객체(Bean)에게 로직 위임
        Map<String, Object> result = authService.checkAuth(req.getSession(false), currentPort);

        // 결과 출력 (JSON 구성)
        out.print(buildJsonResponse(result));
    }

    private String buildJsonResponse(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        map.forEach((key, value) -> {
            json.append("\"").append(key).append("\": ");
            if (value instanceof String) json.append("\"").append(value).append("\",");
            else json.append(value).append(",");
        });
        if (json.length() > 1) json.setLength(json.length() - 1); // 마지막 콤마 제거
        json.append("}");
        return json.toString();
    }
}