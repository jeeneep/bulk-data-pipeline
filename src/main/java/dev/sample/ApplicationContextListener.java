package dev.sample;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@WebListener
public class ApplicationContextListener implements ServletContextListener {

    private HikariDataSource ds;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        
        try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

        HikariConfig config = new HikariConfig();

        // db 연결
        config.setJdbcUrl("jdbc:mysql://192.168.0.128:3306/card_db?serverTimezone=Asia/Seoul");
        config.setUsername("root");
        config.setPassword("pwpw");

        // 선택 설정값 예시
//        config.setMaximumPoolSize(10);
//        config.setMinimumIdle(2);
//        config.setConnectionTimeout(3000);
//        config.setIdleTimeout(600000);
//        config.setMaxLifetime(1800000);

        ds = new HikariDataSource(config);

        ctx.setAttribute("DATA_SOURCE", ds);
        
        // consumer 가동
        new Thread(() -> {
            try {
                RabbitMQConsumer consumer = new RabbitMQConsumer();
                consumer.startConsume(ds); 
            } catch (Exception e) {
                System.err.println("[시스템] Consumer 가동 중 오류!");
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (ds != null) ds.close(); // 애플리케이션 종료 시 커넥션 풀 자원해제
    }

    public static DataSource getDataSource(ServletContext ctx) {
        return (DataSource) ctx.getAttribute("DATA_SOURCE");
    }
}
