package dev.sample;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import dev.sample.service.CsvUploadService;

@WebServlet("/api/upload")
@MultipartConfig
public class DataUploadServlet extends HttpServlet {
	
	private static final Logger logger = LoggerFactory.getLogger(DataUploadServlet.class);
	
	private CsvUploadService uploadService;
	private ClassPathXmlApplicationContext context; 

	@Override
	public void init(ServletConfig config) throws ServletException {

		// dev/sample 패키지 안에 넣었다면 앞에 경로를 붙여줍니다.
		context = new ClassPathXmlApplicationContext("dev/sample/applicationContext.xml");
		
		this.uploadService = context.getBean(CsvUploadService.class);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		Part filePart = request.getPart("csvFile"); 
		if (filePart == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		int port = request.getServerPort();
		
		try {
			int count = uploadService.processData(filePart.getInputStream(), port);
			
			response.setContentType("text/plain;charset=UTF-8");
			response.getWriter().write(port + " 서버가 " + count + "건의 데이터를 받았습니다.");
			
		} catch (Exception e) {
			logger.error("[Port {}] Error during CSV processing: ", port, e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

}