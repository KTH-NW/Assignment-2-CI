package ci;

/**
 * Class for file input/output
 * Have functions for creating the log files and index files
 */
public class FileIO {



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