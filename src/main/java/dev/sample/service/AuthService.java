package dev.sample.service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    
    // 로그인 상태 및 사용자 정보를 확인하는 비즈니스 로직
    public Map<String, Object> checkAuth(HttpSession session, int currentPort) {
        Map<String, Object> authData = new HashMap<>();
        authData.put("currentPort", currentPort);

        if (session != null && session.getAttribute("user") != null) {
            authData.put("isLoggedIn", true);
            authData.put("userId", session.getAttribute("user"));
            authData.put("firstServer", session.getAttribute("serverInfo"));
        } else {
            authData.put("isLoggedIn", false);
        }
        return authData;
    }
}