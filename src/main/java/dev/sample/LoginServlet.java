package dev.sample;

import dev.sample.service.LoginService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private LoginService loginService;

    @Override
    public void init() throws ServletException {
        ApplicationContext ctx =
                new ClassPathXmlApplicationContext("applicationContext.xml");
        this.loginService = ctx.getBean(LoginService.class);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String id = req.getParameter("id");

        String result = loginService.login(id);
        String base = req.getContextPath();

        if ("TEST1".equals(result)) {

            createSession(req, id);
            resp.sendRedirect(base + "/auth.jsp");

        } else if ("TEST2".equals(result)) {

            createSession(req, id);
            resp.sendRedirect(base + "/auth.jsp");

        } else {
            resp.sendRedirect(base + "/login.html");
        }
    }

    private void createSession(HttpServletRequest req, String id) {
        HttpSession session = req.getSession(true);
        session.setAttribute("user", id);
        session.setAttribute("serverInfo", "Tomcat-" + req.getLocalPort());
    }
}