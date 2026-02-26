package dev.sample.test;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/test/auth")
public class AuthCheckServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 응답 타입을 JSON으로 설정
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        
        HttpSession session = req.getSession(false); // 기존 세션이 있는지 확인
        int currentPort = req.getLocalPort(); // 현재 응답하는 톰캣 포트

        if (session != null && session.getAttribute("user") != null) {
            String userId = (String) session.getAttribute("user");
            String firstServer = (String) session.getAttribute("serverInfo");

            out.print("{");
            out.print("\"isLoggedIn\": true,");
            out.print("\"userId\": \"" + userId + "\",");
            out.print("\"currentPort\": " + currentPort + ",");
            out.print("\"firstServer\": \"" + firstServer + "\"");
            out.print("}");
        } else {
            out.print("{");
            out.print("\"isLoggedIn\": false,");
            out.print("\"currentPort\": " + currentPort);
            out.print("}");
        }
    }
}