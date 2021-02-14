package ci;

import java.io.InvalidObjectException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Class for file input/output
 * Have functions for creating the log files and index files
 */
public class FileIO {



    /** Call this function after building the project.
     *  It creates a log-file with the sha-code, a link to the github-commit, and the build log
     *  Then it updates the index.html with the new build log
     * 
     * @param sha The SHA code for the commit
     * @param buildLog The build log for the commit
     * @param filepath Path to the folder where the logs are stored
     * @param link The http-link to the github repo
     *             Format: "https://github.com/[organisation]]/[repo]]/commit/"
     *             '/commit/' can be changed to '/tree/' depending on what you want to display
     */
    public static void constructLog(String sha, String buildLog, String filepath, String link){
        // Check that folder exists and how many files it contains
        File[] pathnames = listFileNames(filepath);
        int nameOffset = 0;
        if(pathnames == null){
            //folder does not exist, create it
            try{
                createFolder(filepath);
            }catch(InvalidObjectException e){
                System.out.println("constructLog exception: createFolder: " + e);
                System.exit(0);
            }
        }else{
            nameOffset = pathnames.length;
        }
        
        // Create the HTML file that represents this build
        String logHTML = createLogHTML(sha, buildLog, link);
        int fileNumber = nameOffset + 1;
        String filename = fileNumber + ".html";
        try{
            writeToFile(filename,logHTML,filepath);
        }catch(IOException e){
            System.out.println("constructLog exception: writeToFile: " + e);
            System.exit(0);
        }

        // Takes the content of filepath and construct the index file
        pathnames = listFileNames(filepath);
        try {
            String indexHTML = createIndexHTML(pathnames);
            writeToFile("index.html", indexHTML, "");
        } catch(IOException e){
            System.out.println("constructLog exception: createIndexHTML: " + e);
            System.exit(0);
        }

    }


    /** This function takes a filename and a path and writes the content to that file.
     * 
     * @param filename The name of the file that should be written to
     * @param content The content that should be written to the file
     * @param filepath The path to where the file should be written.
     * @throws IOException FileWriter
     */
    public static void writeToFile(String filename, String content, String filepath) throws IOException{

        FileWriter fw = new FileWriter(filepath+filename);
        fw.write(content);
        fw.close();

    }

    /** Creates a folder at specified path
     * 
     * @param pathname filepath where the folder should be created
     * @throws InvalidObjectException throws error if the folder could not be created.
     */
    public static void createFolder(String pathname) throws InvalidObjectException{
        File file = new File(pathname);
        boolean status = file.mkdir();
        if(!status){
            throw new InvalidObjectException("Create Folder exception: Could not create folder.");
        }
    }

    /** Delete folder and subfolders and files at specified path
     *  [Warning] Recursively deletes content of folder before deleting the folder
     * 
     * @param pathname Path to start deleting
     * @throws IOException Thrown if one item cannot be deleted
     */
    public static void deleteFolder(String pathname) throws IOException{
        File filepath = new File(pathname);

        if(filepath.isDirectory()){
            File[] paths = filepath.listFiles();
            if(paths != null){
                for(File path : paths){
                    deleteFolder(path.toString());
                }
            }
        }
        if(!filepath.delete()) {
            throw new IOException("deleteDirectory exception: Failure to delete: " + filepath);
        }
    }


     /**
     * 
     * @param sha SHA code for the github commit
     * @param buildLog The buildlog for the commit
     * @return The HTML page as a string with the build information, sha, and link to github.
     */
    public static String createLogHTML(String sha, String buildLog, String link){
        StringBuilder sb = new StringBuilder();
        
        sb.append("<!DOCTYPE html><html><head><title>Index</title></head><body>");
        sb.append("<a href='" + link + sha  + "'>Github Commit</a></br>");
        sb.append(buildLog);
        sb.append("</body></html>");

        return sb.toString();
    }   

    /** Creates a top level index file that is populated with links to the log files
     * 
     * @param pathnames Array of Files that the index should link to
     * @return the HTML page as a string
     * @throws IOException from Files.readAttributes or Paths.get
     */
    public static String createIndexHTML(File[] pathnames) throws IOException{
        StringBuilder sb = new StringBuilder();
        
        sb.append("<!DOCTYPE html><html><head><title>Index</title></head><body>");
        
        for(int i = pathnames.length-1; i >= 0; i--){
            File pathname = pathnames[i];
            String url = pathname.getAbsolutePath();
            BasicFileAttributes attr = Files.readAttributes(Paths.get(url), BasicFileAttributes.class);
            String linkText = attr.creationTime().toString();
            sb.append("<a href='" + url + "'>" + linkText + "</a></br>");
        }
        
        sb.append("</body></html>");

        return sb.toString();

    }


    /** Adds the content of a file to a string and returns it
     * 
     * @param pathname The file that we want to inspect
     * @throws FileNotFoundException from FileReader
     * @throws IOException from BufferedReader.readLine()
     * @return The content of the file as a string
     */
    public static String contentOfFile(File pathname) throws FileNotFoundException, IOException{
        StringBuilder sb = new StringBuilder();

        BufferedReader br = new BufferedReader(new FileReader(pathname));
        String str;
        while((str = br.readLine()) != null){
            sb.append(str);
        }
        br.close();
        return sb.toString();
    }

    /** Lists the files in the requested folder. Returns an array of the content as a File-array, or null if empty
     * 
     * @param folderPath The folder we want to look at
     * @return An array of File with all files in the folder or an empty array if the folder is empty or the value null if the folder does not exist
     */
    public static File[] listFileNames(String folderPath){
        File[] pathnames;
        File f = new File(folderPath);
        pathnames = f.listFiles();

        return pathnames; 
    }

}