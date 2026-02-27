package dev.sample;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        
        // ID가 admin이면 세션 생성
        if ("admin".equals(id)) {
            HttpSession session = req.getSession(true); // 새로운 세션 생성
            session.setAttribute("user", id);
            session.setAttribute("serverInfo", "Tomcat-" + req.getLocalPort());
            
            resp.sendRedirect("/auth.html");
        } else {
        	resp.sendRedirect("/login.html");
        }
    }
}