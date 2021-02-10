package ci;

/**
 * Class for file input/output
 * Have functions for creating the log files and index files
 */
public class FileIO {


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