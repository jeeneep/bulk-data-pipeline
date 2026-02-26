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

        HttpSession session = req.getSession(false);
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<html>");
        out.println("<head><title>Auth Check</title></head>");
        out.println("<body>");

        if (session != null && session.getAttribute("user") != null) {

            String userId = (String) session.getAttribute("user");
            int currentPort = req.getLocalPort();
            String firstPort = (String) session.getAttribute("serverInfo");

            out.println("<h2>인증 성공</h2>");
            out.println("<p>ID: " + userId + "</p>");
            out.println("<p>현재 서버 포트: " + currentPort + "</p>");
            out.println("<p>최초 로그인 서버: " + firstPort + "</p>");
            out.println("<br>");
            out.println("<button onclick='location.reload()'>새로고침</button>");

        } else {
        	int currentPort = req.getLocalPort();
            out.println("<h2>인증되지 않은 사용자</h2>");
            out.println("<p>현재 서버 포트: " + currentPort + "</p>");
            out.println("<p>세션이 없습니다.</p>");
            out.println("<a href='../login.html'>로그인 페이지로 이동</a>");

        }

        out.println("</body>");
        out.println("</html>");
    }
}