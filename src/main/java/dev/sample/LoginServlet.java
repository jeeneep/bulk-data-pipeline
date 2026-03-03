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
        
        // test1이면 세션 불일치 테스트
        if ("test1".equals(id)) {
            HttpSession session = req.getSession(true);
            session.setAttribute("user", id);
            session.setAttribute("serverInfo", "Tomcat-" + req.getLocalPort());
            
            resp.sendRedirect("/bulk-data-pipeline/auth.jsp");
        } else if ("test2".equals(id)) {  // test2이면 세션 클러스터링 테스트
            HttpSession session = req.getSession(true);
            session.setAttribute("user", id);
            session.setAttribute("serverInfo", "Tomcat-" + req.getLocalPort());
            
            resp.sendRedirect("/auth.jsp");
        } else {
        	resp.sendRedirect("/login.html");
        }
    }
}