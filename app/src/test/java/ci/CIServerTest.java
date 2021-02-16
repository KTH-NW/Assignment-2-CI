package ci;

import org.checkerframework.checker.units.qual.C;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.ArrayList;

public class CIServerTest {
    /*
     HttpClient httpclient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://e73e8750b200.ngrok.io/github-webhooks"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofFile(Paths.get("src/test/testbadpayload.json"))).build();
        HttpResponse<String> resp = httpclient.send(request, HttpResponse.BodyHandlers.ofString());
     */
    @Test
    @DisplayName("CIServer::getCommits::returns null with malformed JSON")
    public void testGetCommitsOnBadData() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("src/test/testbadpayload.json")) {
            //Read JSON file
            JSONObject obj = (JSONObject) jsonParser.parse(reader);
            assertNull(CIServer.getCommits(obj));
        } catch (IOException | ParseException e ) {
            e.printStackTrace();
        }
    }
    @Test
    @DisplayName("CIServer::getCommits::Works as intended")
    public void testGetCommits() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("src/test/testpayload.json")) {
            //Read JSON file
            JSONObject obj = (JSONObject) jsonParser.parse(reader);
            assertEquals(1, CIServer.getCommits(obj).size());
        } catch (IOException | ParseException e ) {
            e.printStackTrace();
        }
    }
    @Test
    @DisplayName("CIServer::getRepo::returns null with bad input")
    public void testGetRepoWithBadData() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("src/test/testbadpayload.json")) {
            //Read JSON file
            JSONObject obj = (JSONObject) jsonParser.parse(reader);
            assertNull(CIServer.getRepo(obj));
        } catch (IOException | ParseException e ) {
            e.printStackTrace();
        }
    }
    @Test
    @DisplayName("CIServer::getRepo::Works as intended")
    public void testGetRepo() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("src/test/testpayload.json")) {
            //Read JSON file
            JSONObject obj = (JSONObject) jsonParser.parse(reader);
            assertEquals("Assignment-2-CI", CIServer.getRepo(obj));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
    @Test
    @DisplayName("CIServer::getOwner::Works as intended")
    public void testGetOwner() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("src/test/testpayload.json")) {
            //Read JSON file
            JSONObject obj = (JSONObject) jsonParser.parse(reader);
            assertEquals("DD2480-Group-11", CIServer.getOwner(obj));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
    @Test
    @DisplayName("CIServer::getOwner::returns null with bad input")
    public void testGetOwnerWithBadData() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("src/test/testbadpayload.json")) {
            //Read JSON file
            JSONObject obj = (JSONObject) jsonParser.parse(reader);
            assertNull(CIServer.getOwner(obj));
        } catch (IOException | ParseException e ) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("CIServer::CreateURL::works as intended")
    public void testCreateURL() {
        assertEquals("https://api.github.com/repos/\\/#/statuses/@", CIServer.createURL("\\", "#", "@").toString());
        assertEquals("https://api.github.com/repos///statuses/", CIServer.createURL("", "", "").toString());
        assertEquals("https://api.github.com/repos/null/null/statuses/null", CIServer.createURL(null, null, null).toString());
    }

    @Test
    @DisplayName("CIServer::createHttpRLConnection::works as intended")
    public void testCreateHttpURLConnection() {
        assertEquals(CIServer.createURL("\\", "#", "@"),CIServer.createHttpURLConnection(CIServer.createURL("\\", "#", "@")).getURL());
    }

    @Test
    @DisplayName("CIServer::getText::works as intended")
    public void testgetText() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("src/test/testpayload.json")) {
            //Read JSON file
            JSONObject obj = (JSONObject) jsonParser.parse(reader);
            JSONArray arr  = CIServer.getCommits(obj);
            ArrayList<Boolean> failedCommit = new ArrayList<Boolean>(1);
            failedCommit.add(false);
            ArrayList<Boolean> successfulCommit = new ArrayList<Boolean>(1);
            successfulCommit.add(true);
            assertEquals("sha:  c11d206a83d836d036b963e504d3d44700ae02d4\n" +
            "commit information:  Update README.md\n" +
            "Build success!\n", CIServer.getText(arr,successfulCommit));
            assertEquals("sha:  c11d206a83d836d036b963e504d3d44700ae02d4\n" +
                    "commit information:  Update README.md\n" +
                    "Build failed!\n", CIServer.getText(arr, failedCommit));

        } catch (IOException | ParseException e ) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("CIServer::getEmail::works as intended")
    public void testgetEmail() {
        assertEquals("hannessu@kth.se", CIServer.getEmail("HannesSundin"));
        assertEquals("danhalv@kth.se", CIServer.getEmail("DanielH4"));
        assertEquals("yuzho@kth.se", CIServer.getEmail("audreyeternal"));
        assertEquals("nwessman@kth.se", CIServer.getEmail("nwessman"));
        assertNull(CIServer.getEmail("fasdfa"));


    }


}
