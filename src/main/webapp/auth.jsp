<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%
    javax.servlet.http.HttpSession sess = request.getSession(false);
    
    String userId = (sess != null) ? (String) sess.getAttribute("user") : null; 
    Object firstServer = (sess != null) ? sess.getAttribute("serverInfo") : "정보 없음";
    
    boolean isLoggedIn = (userId != null);
    int currentPort = request.getLocalPort();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>인증 확인</title>
    <style>
        .info-box { border: 2px solid #333; padding: 20px; background-color: #f9f9f9; border-radius: 8px; }
        .highlight { color: blue; font-weight: bold; }
        .origin { color: green; font-weight: bold; }
    </style>
</head>
<body>
    <% if (isLoggedIn) { %>
        <h2 id="title">인증 성공</h2>
        <div id="content" class="info-box">
            <p>접속 ID: <span class="highlight"><%= userId %></span></p>
            <p>최초 로그인 서버 포트: <span class="origin"><%= firstServer %></span></p>
            <p>현재 응답 서버 포트: <span class="highlight"><%= currentPort %></span></p>
            <p>세션 ID: <b><%= sess.getId() %></b></p>
        </div>
    <% } else { %>
        <h2 id="title" style="color: red;">인증되지 않은 사용자</h2>
        <div id="content" class="info-box">
            <p>세션이 존재하지 않습니다. 현재 포트: <%= currentPort %></p>
            <p>로그인을 먼저 진행해 주세요.</p>
        </div>
    <% } %>

    <br>
    <button onclick="location.reload()">새로고침 (포트 변경 확인)</button>
    <button onclick="location.href='login.html'">로그인 페이지로</button>
</body>
</html>