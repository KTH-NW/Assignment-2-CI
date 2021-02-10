package ci;

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
     */
    public static void constructLog(String sha, String buildLog){
        String filepath = "Logs/";
        File[] pathnames = listFileNames(filepath);
        
        String logHTML = createLogHTML(sha, buildLog);
        int fileNumber = pathnames.length + 1;
        String filename = fileNumber + ".html";
        writeToFile(filename,logHTML,filepath);
        pathnames = listFileNames(filepath);

        String indexHTML = createIndexHTML(pathnames);
        writeToFile("index.html", indexHTML, "");

    }

    /**
     * 
     * @param filename The name of the file that should be written to
     * @param content The content that should be written to the file
     * @param filepath The path to where the file should be written.
     */
    public static void writeToFile(String filename, String content, String filepath){
        try{
            FileWriter fw = new FileWriter(filepath+filename);
            fw.write(content);
            fw.close();
        }catch(IOException e){
            System.out.print(e);
        }
    }


     /**
     * 
     * @param sha SHA code for the github commit
     * @param buildLog The buildlog for the commit
     * @return The HTML page as a string with the build information, sha, and link to github.
     */
    public static String createLogHTML(String sha, String buildLog){
        StringBuilder sb = new StringBuilder();
        String link = "https://github.com/DD2480-Group-11/Assignment-2-CI/commit/";
        
        sb.append("<!DOCTYPE html><html><head><title>Index</title></head><body>");
        sb.append("<a href='" + link + sha  + "'>Github Commit</a></br>");
        sb.append(buildLog);
        sb.append("</body></html>");

        return sb.toString();
    }   

    /**
     * 
     * @param pathnames Array of Files that the index should link to
     * @return the HTML page as a string
     */
    public static String createIndexHTML(File[] pathnames){
        StringBuilder sb = new StringBuilder();
        
        sb.append("<!DOCTYPE html><html><head><title>Index</title></head><body>");
        try {
            for(int i = pathnames.length-1; i >= 0; i--){
                File pathname = pathnames[i];
                String url = pathname.toString();
                BasicFileAttributes attr = Files.readAttributes(Paths.get(url), BasicFileAttributes.class);
                String linkText = attr.creationTime().toString();
                sb.append("<a href='" + url + "'>" + linkText + "</a></br>");
            }
        } catch(IOException e){
            return "";
        }
        

        sb.append("</body></html>");

        return sb.toString();

    }


     /**
     * 
     * @param pathname The file that we want to inspect
     * @return The content of the file as a string
     */
    public static String contentOfFile(File pathname){
        StringBuilder sb = new StringBuilder();

        try{
            BufferedReader br = new BufferedReader(new FileReader(pathname));
            String str;
            while((str = br.readLine()) != null){
                sb.append(str);
            }
            br.close();
        }
        catch(FileNotFoundException e){
            System.out.println(e);
            return "";
        }
        catch(IOException e){
            System.out.println(e);
            return "";
        }
        return sb.toString();
    }

    /**
     * 
     * @param folderPath The folder we want to look at
     * @return An array of File with all files in the folder
     */
    public static File[] listFileNames(String folderPath){
        File[] pathnames;
        File f = new File(folderPath);
        pathnames = f.listFiles();

        return pathnames; 
    }

}