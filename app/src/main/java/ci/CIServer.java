
package ci;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Properties;
import java.util.HashMap;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Continuous integration server implementation. 
 * Handles push events by Github. 
 * Compiles/tests commits and generates commit status checks. 
 * Sends email notifications regarding build status.
 * */
public class CIServer implements HttpHandler {

	/**
	 * Represents action to take for a commit.
	 * Used to communicate whether a commit should be built or tested.
	 * */
	private enum Action {
		BUILD, TEST;
	}

	public final String GITHUB_TOKEN;
	public final String TARGET_DIR;		//directory where build logs are stored

	CIServer(String targetDir) {
		TARGET_DIR = targetDir;
		GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");
	}

	/**
	 * Handles a push event from github.
	 *
	 * @param exchange html exchange of push event. A POST request is sent by github.
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
			System.exit(0);
		}

		JSONArray commits = getCommits(body);
		ArrayList<Boolean> isBuildSuccessful = processCommits(commits, TARGET_DIR, getOwner(body), getRepo(body), Action.BUILD);
		ArrayList<Boolean> areTestsSuccessful = processCommits(commits, TARGET_DIR, getOwner(body), getRepo(body), Action.TEST);

		createCommitStatuses(getRepo(body), getOwner(body), commits, isBuildSuccessful, areTestsSuccessful, GITHUB_TOKEN);

		sendEmail(getOwner(body),commits, isBuildSuccessful);

		exchange.sendResponseHeaders(200, 0);
		return;
	}

	/**
	 * Retrieves the repository name from a JSONObject
	 *
	 * @param obj a JSONObject, which is the root body of the github POST request.
	 * @return repository name as string
	 * */
	public static String getRepo(JSONObject obj) {
		JSONObject repoObj = (JSONObject)obj.get("repository");
		String repo = (String)repoObj.get("name");
		return repo;
	}

	/**
	 * Retrieves the owner name of a repository from a JSONObject
	 *
	 * @param obj a JSONObject, which is the root body of the github POST request.
	 * @return owner name of a repository as string
	 * */
	public static String getOwner(JSONObject obj) {
		JSONObject repoObj = (JSONObject)obj.get("repository");
		JSONObject ownerObj = (JSONObject)repoObj.get("owner");
		String owner = (String)ownerObj.get("name");
		return owner;
	}

	/**
	 * Makes a POST request to Github's api endpoint for setting the status of a commit.
	 *
	 * @param repo name of repository
	 * @param owner name of owner of repository
	 * @param commits a JSONArray object of the commits in the push event
	 * @param isBuildSuccessful an arraylist of booleans representing whether the commit of an index built successfully
	 * @param github_token used to authorize the Github user making the request (user hosting the server).
	 * */
	public static void createCommitStatuses(String repo, String owner, JSONArray commits, 
											 ArrayList<Boolean> isBuildSuccessful, 
											 ArrayList<Boolean> areTestsSuccessful,
											 String github_token) 
	{
		for(int i = 0; i < commits.size(); i++) {

			//get sha of commit
			JSONObject commit = (JSONObject)commits.get(i);
			String sha = (String)commit.get("id");

			//url for Github's create commit status api endpoit
			URL url = createURL(owner, repo, sha);

			HttpURLConnection conn = createHttpURLConnection(url);
			
			try {
				conn.setRequestMethod("POST");
			}
			catch(ProtocolException e) {
				System.out.println("Failed to set POST request method.");
				System.exit(0);
			}

			//set request headers
			conn.setRequestProperty("accept", "application/vnd.github.v3+json");
			conn.setRequestProperty("Authorization", "token "+github_token);

			conn.setDoOutput(true);	//allows writing to request body

			//prepare body
			JSONObject root = new JSONObject();
			if(isBuildSuccessful.get(i).booleanValue() && areTestsSuccessful.get(i).booleanValue()) {
				root.put("state", "success");
				root.put("description", "Build/Test successful");
			}
			else if(!isBuildSuccessful.get(i).booleanValue()) {
				root.put("state", "failure");
				root.put("description", "Build failed!");
			}
			else if(areTestsSuccessful.get(i).booleanValue()) {
				root.put("state", "failure");
				root.put("description", "Tests failed!");
			}
			else {
				root.put("state", "error");
				root.put("description", "Something went wrong with setting the commit status.");
			}

			String body = root.toJSONString();

			try {	//write to body
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
	 * Creates and returns a URL object for Github's api endpoint for creating commit statuses
	 *
	 * @param owner name of repo owner
	 * @param repo name of repository
	 * @param sha of specific commit
	 * @return URL object for api endpoint
	 * */
	public static URL createURL(String owner, String repo, String sha) {

		URL url = null;
		try {
			url = new URL("https://api.github.com/repos/"+owner+"/"+repo+"/statuses/"+sha);
		}
		catch(MalformedURLException e) {
			System.out.println("Provided URL is malformed");
			System.exit(0);
		}

		return url;
	}

	/**
	 * Create an HTTP connection to an URL object.
	 *
	 * @param url target destination of connection
	 * @return HttpURLConnection object to url destination
	 * */
	public static HttpURLConnection createHttpURLConnection(URL url) {

		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)url.openConnection();
		}
		catch(IOException e) {
			System.out.println("Failed to establish HTTP connection.");
			System.exit(0);
		}

		return conn;
	}

	/**
	 * Returns a JSONArray object of commits from the JSON body of a POST request
	 * See Github push event API for detailed info
	 * */
	public static JSONArray getCommits(JSONObject body) {
		JSONArray commits = (JSONArray)body.get("commits");
		return commits;
	}

	/**
	 * Builds or tests each commit sent in a push request.
	 *
	 * @param commits a JSONArray of the commits in a push request
	 * @param owner name of owner of the repository
	 * @param repo name of the repository
	 * @param action represents the action to take for a commit. Can be BUILD or TEST.
	 * @return an arraylist representing whether a commit was built successfully.
	 *		   Index corresponds to commit in given array.
	 * */
	public static ArrayList<Boolean> processCommits(JSONArray commits, String buildLogsDir, String owner, String repo, Action action) {

		ArrayList<Boolean> isSuccessful = new ArrayList<Boolean>();	//wether a build or index at index is successful

		String targetDir = null;
		switch(action) {
			case BUILD:	targetDir = "builds";
			case TEST:	targetDir = "tests";
		}

		try {FileIO.createFolder(targetDir); }
		catch(InvalidObjectException e) { System.exit(0); }

		for(int i = 0; i < commits.size(); i++) {
			JSONObject commitObj = (JSONObject)commits.get(i);
			String sha = (String)commitObj.get("id");
			String targetCommitDir = targetDir+"/"+sha;

			try {FileIO.createFolder(targetCommitDir); }
			catch(InvalidObjectException e) { System.exit(0); }

			cloneRepo("https://github.com/"+owner+"/"+repo+".git", targetCommitDir);

			String log = processCommit(owner, repo, buildLogsDir, targetCommitDir, sha, action);

			isSuccessful.add(log.contains("BUILD SUCCESSFUL"));		//true if contains given string
		}

		try { FileIO.deleteFolder(targetDir); }
		catch(IOException e) { System.exit(0); }

		return isSuccessful;
	}

	/**
	 * Clone a github repository into a chosen directory.
	 *
	 * @param repoName name of repository.
	 * @param targetDir name of directory that repo is cloned into.
	 * */
	public static void cloneRepo(String repoName, String targetDir) {
		try {
			Runtime.getRuntime().exec("git clone "+repoName+" "+targetDir);
		}
		catch(IOException e) {
			System.out.println("Failed to clone github repository: "+repoName+" to directory: "+targetDir);
			System.exit(0);
		}
	}

	/**
	 * Builds or tests an individual commit.
	 *
	 * @param dir name of directory where repository exists.
	 * @param sha string of the sha of the commit.
	 * @param action determines whether commit should be built or tested.
	 * @return the log produced while testing or building commit.
	 * */
	public static String processCommit(String owner, String repo, String buildLogsDir, String dir, String sha, Action action) {

		String option = null; //option used with gradle build system. Determines whether repo is built or tests are run
		switch(action) {
			case BUILD:	option = "build";
			case TEST:	option = "test";
		}

		setRepoVersion(dir, sha);	//set repo version to specified commit

		Process process = null;	//stores build action as a process
		try {					//build or test commit
			process = Runtime.getRuntime().exec("gradle -p "+dir+" clean "+option+" --no-daemon");
		}
		catch(IOException e) {
			System.out.println("Failed to "+option+" "+dir);
			System.exit(0);
		}

		//retrieve output log
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String log = reader.lines().collect(Collectors.joining());

		if(action == Action.BUILD)
			FileIO.constructLog(sha, log, buildLogsDir+'/', "https://github.com/"+owner+"/"+repo);

		process.destroy();	//clean up created process

		return log;
	}

	/**
	 * Sets the repository version to the specified commit.
	 *
	 * @param dir local directory of the repository
	 * @param sha sha string of the commit
	 * */
	public static void setRepoVersion(String dir, String sha) {
		try {
			Runtime.getRuntime().exec("git --git-dir "+dir+"/.git reset --hard "+sha);
		}
		catch(IOException e) {
			System.out.println("Failed to set repo to version: "+sha);
			System.exit(0);
		}
	}

	/**
	 * Send email to the branch owner to notify the build results.
	 * @param owner name of the branch
	 * @param commits a JSONArray of the commits in a push request
	 * @param isBuildSuccessful an arraylist representing whether a commit was built successfully.
	 */
	public static void sendEmail(String owner, JSONArray commits , ArrayList<Boolean> isBuildSuccessful){
		final String username = "bunnybunny.zhou@gmail.com";
        final String password = "TheBestGroup11";
		final String email = getEmail(owner);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
          new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
          });
		  session.setDebug(true);
        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("bunnybunny.zhou@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(email));
            message.setSubject("The build results:");
			String text = getText(commits, isBuildSuccessful);
            message.setText(text);

            Transport.send(message);

            System.out.println("Mail Sent Successfully");

        } catch (MessagingException e) {
            System.out.println("Failed to send email.");
			System.exit(0);
        }
    }
	/**
	 * 
	 * @param commits a JSONArray of the commits in a push request
	 * @param isBuildSuccessful an arraylist representing whether a commit was built successfully.
	 * @return the email context.
	 */
	public static String getText(JSONArray commits , ArrayList<Boolean> isBuildSuccessful){
		String text = "sha:  ";		
		for(int i = 0; i < commits.size(); i++) {
			JSONObject commit = (JSONObject)commits.get(i);
			String sha = (String)commit.get("id");			//sha of commit
			text = text + sha + "\n";
			String message = (String)commit.get("message");
			text = text + "commit information:  "+ message + "\n";
			if(isBuildSuccessful.get(i) == true){
				text = text + "Build success!" + "\n";
			}
				else{
					text = text + "Build failed!" + "\n";	
			}			

		}
		return text;
	}
	/**
	 * 
	 * @param owner the branch owner
	 * @return the email address of the owner.
	 */
	public static String getEmail(String owner){
		HashMap<String, String> emailMap = new HashMap<String, String>();
		emailMap.put("DanielH4","danhalv@kth.se") ;
		emailMap.put("audreyeternal","yuzho@kth.se") ;
		emailMap.put("nwessman","nwessman@kth.se") ;
		emailMap.put("HannesSundin","hannessu@kth.se") ;
		String email = emailMap.get(owner);
		return email;

	}
}
