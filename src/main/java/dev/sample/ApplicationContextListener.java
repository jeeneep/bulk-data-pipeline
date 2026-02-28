package dev.sample;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@WebListener
public class ApplicationContextListener implements ServletContextListener {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationContextListener.class);
    private HikariDataSource ds;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    	logger.info("ApplicationContextListener initialized."); // 시작점 확인
        ServletContext ctx = sce.getServletContext();
        
        try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			logger.error("Failed to load JDBC Driver: ", e); // 에러 로그로 변경
		}

        HikariConfig config = new HikariConfig();

        // db 연결
        config.setJdbcUrl("jdbc:mysql://localhost:3306/card_db?serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true");
        config.setUsername("root");
        config.setPassword("1234");

        // 선택 설정값 예시
//        config.setMaximumPoolSize(10);
//        config.setMinimumIdle(2);
//        config.setConnectionTimeout(3000);
//        config.setIdleTimeout(600000);
//        config.setMaxLifetime(1800000);

        ds = new HikariDataSource(config);
        logger.info("HikariCP DataSource created."); // 커넥션 풀 생성 확인
        ctx.setAttribute("DATA_SOURCE", ds);
        
        // consumer 가동
        new Thread(() -> {
            try {
            	logger.info("RabbitMQ Consumer thread start."); // 스레드 시작 확인
                RabbitMQConsumer consumer = new RabbitMQConsumer();
                consumer.startConsume(ds); 
            } catch (Exception e) {
            	logger.error("Error in RabbitMQ Consumer thread: ", e); // 예외 발생 시 로그 기록
            }
        }).start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (ds != null) {
        	ds.close(); // 애플리케이션 종료 시 커넥션 풀 자원해제
        	logger.info("HikariCP DataSource closed."); // 자원 해제 확인
        }
    }

    public static DataSource getDataSource(ServletContext ctx) {
        return (DataSource) ctx.getAttribute("DATA_SOURCE");
    }
}
