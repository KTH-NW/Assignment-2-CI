
package ci;

import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class CIServer implements HttpHandler {

	public final String GITHUB_TOKEN;

	CIServer() {
		GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");
	}

	public void handle(HttpExchange exchange) throws IOException {
		exchange.sendResponseHeaders(200, 0);
		return;
	}
}
