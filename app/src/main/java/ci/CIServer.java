
package ci;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CIServer implements HttpHandler {

	public final String GITHUB_TOKEN;

	CIServer() {
		GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");
	}


	/**
	 * Handles a push event
	 * */
	public void handle(HttpExchange exchange) throws IOException {

		InputStream iStream = exchange.getRequestBody();
		InputStreamReader iStreamReader = new InputStreamReader(iStream);
		JSONParser jsonParser = new JSONParser();

		//root of json body
		JSONObject body = null;
		try {
			body = (JSONObject)jsonParser.parse(iStreamReader);
		}
		catch(ParseException e) {
			System.out.println("Failed to parse JSON body.");
			System.exit(0);
		}

		JSONArray commits = getCommits(body);
		ArrayList<Boolean> isBuildSuccessful = buildCommits(commits);

		createCommitStatuses(getRepo(body), getOwner(body), commits, isBuildSuccessful, GITHUB_TOKEN);

		exchange.sendResponseHeaders(200, 0);
		return;
	}

	private static String getRepo(JSONObject obj) {
		JSONObject repoObj = (JSONObject)obj.get("repository");
		String repo = (String)repoObj.get("name");
		return repo;
	}

	private static String getOwner(JSONObject obj) {
		JSONObject repoObj = (JSONObject)obj.get("repository");
		JSONObject ownerObj = (JSONObject)repoObj.get("owner");
		String owner = (String)ownerObj.get("name");
		return owner;
	}

	private static void createCommitStatuses(String repo, String owner, JSONArray commits,
											 ArrayList<Boolean> isBuildSuccessful, String github_token) 
	{
		for(int i = 0; i < commits.size(); i++) {
			
			//get sha of commit
			JSONObject commit = (JSONObject)commits.get(i);
			String sha = (String)commit.get("id");

			//url for commit status api endpoit
			URL url = null;
			try {
				url = new URL("https://api.github.com/repos/"+owner+"/"+repo+"/statuses/"+sha);
			}
			catch(MalformedURLException e) {
				System.out.println("Provided URL is malformed");
				System.exit(0);
			}

			//configure post request
			HttpURLConnection conn = null;
			try {
				conn = (HttpURLConnection)url.openConnection();
			}
			catch(IOException e) {
				System.out.println("Failed to establish HTTP connection.");
				System.exit(0);
			}
			try {
				conn.setRequestMethod("POST");
			}
			catch(ProtocolException e) {
				System.out.println("Failed to set POST request method.");
				System.exit(0);
			}
			conn.setRequestProperty("accept", "application/vnd.github.v3+json");
			conn.setRequestProperty("Authorization", "token "+github_token);
			conn.setDoOutput(true);

			//prepare body
			JSONObject root = new JSONObject();
			if(isBuildSuccessful.get(i).booleanValue())
				root.put("state", "success");
			else
				root.put("state", "failure");

			String body = root.toJSONString();

			try {
				OutputStream os = conn.getOutputStream();
				byte[] input = body.getBytes("utf-8");
				os.write(input, 0, input.length);
				os.flush();
				os.close();
			}
			catch(IOException e) {
				System.out.println("Could not write POST request body.");
				System.exit(0);
			}

			int status = -1;
			try {
				status = conn.getResponseCode();
			}
			catch(IOException e) {
				System.out.println("Failed to get connection response code.");
				System.exit(0);
			}

			conn.disconnect();
		}
	}

	/**
	 * Returns a JSONArray object of commits from the JSON body of a POST request
	 * See Github push event API for detailed info
	 * */
	private static JSONArray getCommits(JSONObject body) {
		JSONArray commits = (JSONArray)body.get("commits");
		return commits;
	}

	/**
	 * Returns an ArrayList of booleans representing whether a commit successfully
	 * compiles or not.
	 * */
	private static ArrayList<Boolean> buildCommits(JSONArray commits) {

		try {
			//temporarily create builds directory
			Runtime.getRuntime().exec("mkdir builds");
		}
		catch(IOException e) {
			System.out.println("Failed to create new builds directory.");
			System.exit(0);
		}

		ArrayList<Boolean> didBuild = new ArrayList<Boolean>();

		for(int i = 0; i < commits.size(); i++) {
			JSONObject commit = (JSONObject)commits.get(i);
			String sha = (String)commit.get("id");			//sha of commit

			didBuild.add(buildCommit(sha));
		}

		try {
			//clean up created builds directory
			Runtime.getRuntime().exec("rm -rf builds");
		}
		catch(IOException e) {
			System.out.println("Failed to clean up created builds directory.");
			System.exit(0);
		}

		return didBuild;
	}

	/**
	 * Returns a boolean value representing whether a specific commit compiles successfully.
	 * sha parameter is used to locally build the project using the gradle wrapper.
	 * The output is parsed to determine whether build was successful or not.
	 * */
	private static boolean buildCommit(String sha) {

		try {
			//create folder for commit
			Runtime.getRuntime().exec("mkdir builds/"+sha);
		}
		catch(IOException e) {
			System.out.println("Failed to create folder for commit.");
			System.exit(0);
		}

		try {
			//clone repo
			String repo = "https://github.com/DanielH4/Assignment-2-CI.git";
			Runtime.getRuntime().exec("git clone "+repo+" builds/"+sha);
		}
		catch(IOException e) {
			System.out.println("Failed to clone repo.");
			System.exit(0);
		}

		try {
			//set repo to sha version
			Runtime.getRuntime().exec("git --git-dir builds/"+sha+"/.git reset --hard "+sha);
		}
		catch(IOException e) {
			System.out.println("Failed to set repo to sha version.");
			System.exit(0);
		}

		//stores build action as a process
		Process process = null;
		try {
			//build commit
			process = Runtime.getRuntime().exec("gradle -p builds/"+sha+"/ build");
		}
		catch(IOException e) {
			System.out.println("Failed to build commit.");
			System.exit(0);
		}

		//return true if build was successful
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		return isBuildSuccessful(reader);
	}

	/**
	 * determine if build was successful based on build output
	 * */
	private static boolean isBuildSuccessful(BufferedReader reader) {
		String line;
		try {
			while((line = reader.readLine()) != null) {
				if(line.contains("BUILD SUCCESSFUL"))
					return true;
			}
		}
		catch(IOException e) {
			System.out.println("Failed to read build output.");
			System.exit(0);
		}
		return false;
	}
}
