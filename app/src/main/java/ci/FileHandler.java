
package ci;

import java.io.File;
import java.io.OutputStream;
import java.io.IOException;

import java.nio.file.Files;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class FileHandler implements HttpHandler {

	private static String PROJECT_DIR;	//root directory. absolute path
	private static String TARGET_DIR;	//directory where build logs are stored

	FileHandler(String projectDir, String targetDir) {
		PROJECT_DIR = projectDir;
		TARGET_DIR = targetDir;
	}

	public void handle(HttpExchange exchange) throws IOException {

		//String name = new File(exchange.getRequestURI().getPath()).toString();
		String name = new File(exchange.getRequestURI().getPath()).getName();

		String index_path = PROJECT_DIR+"/app/"+TARGET_DIR+"/index.html";	//index of entire history.
		String commit_path = PROJECT_DIR+"/app/"+TARGET_DIR+"/buildLogs/"+name;		//specific to a saved build. retrieved based on path

		File file = null;
		if(name.equals(""))
			file = new File(index_path);
		else
			file = new File(commit_path);

		Headers headers = exchange.getResponseHeaders();
		headers.add("Content-Type", "text/html");

		OutputStream out = exchange.getResponseBody();

		if(file.exists()) {
			exchange.sendResponseHeaders(200, file.length());
			out.write(Files.readAllBytes(file.toPath()));
		}
		else {
			System.err.println("Build logs file not found.");

			exchange.sendResponseHeaders(404, 0);
			out.write("404 File not found. Build logs might be empty.".getBytes());
		}

		out.close();
	}
}
