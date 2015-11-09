import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;


public class Commit implements Serializable {
    private Commit parent;
    private String message;
    private int id;
    private HashMap<String, String> committedFiles = new HashMap<String, String>();
    private String dateTime;
    
    /** Constructor for the commit object. Initializes with a commit message msg, a global
     * commit ID i, a map of file names to the respective addresses of their most recent
     * versions, that were inherited from the previous commit filesi, and a pointer to the
     * previous commit object in the commitTree.
     */
    public Commit(String msg, int i, HashMap<String, String> filesi) {
        id  = i;
        message = msg;
        committedFiles = filesi;
    }

    public Commit(Commit commit) {
        parent = commit.parent();
        message = commit.getMessage();
        id = commit.getID();
        committedFiles = commit.fileMap();
        dateTime = commit.dateTime();
    }
    
    /** Returns the current commit message. */
    public String getMessage() {
        return message;
    }
    
    /** Returns the global commit ID. */
    public int getID() {
        return id;
    }

    public void setID(int i) {
        id = i;
    }
    
    /** Returns the list of the files that this commit object currently holds */
    public Set<String> files() {
        return committedFiles.keySet();
    }
    
    /** Retrieves the location of the given fileName to the place where it was most 
     *  recently modified, and committed. 
     *  @param fileName */
    public String retrieveFile(String fileName) {
        return committedFiles.get(fileName);
    }
    
    /** Returns a map that maps file names to their respective addresses where each file
     * was most recently modifed, added, and committed. */
    public HashMap<String, String> fileMap() {
        return committedFiles;
    }
    
    /** Sets the date and time of the given commit. */
    public void setDateTime(String dt) {
        dateTime = dt;
    }

    public String dateTime() {
        return dateTime;
    }

    /** Returns the string of date and the time of this particular commit. */
    public String getDateTime() {
        return dateTime + "\n" + message + "\n";
    }
    
    /** Whenever something is committed, it inherits files from the previous commit which
     * are already passed into the constructor. This method adds to the map, the files  
     * that were staged for that commit and contain new locations. This method is called
     * in KabGit's commit method.
     * @param stagedFiles
     */
    public void addStagedFiles(ArrayList<String> stagedFiles) {
        String newDir = "./.kabgit/" + id;
        File dir = new File(newDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        for (String file: stagedFiles) {
            Path source = Paths.get(file);
            Path destination = Paths.get(newDir + "/" + file);
            try {
                File tempFile = new File(newDir + "/" + file);
                tempFile.getParentFile().mkdirs(); 
                tempFile.createNewFile();
                Files.copy(source, destination, REPLACE_EXISTING, COPY_ATTRIBUTES);
            } catch (IOException e) {
                e.printStackTrace();
            }
            committedFiles.put(file, newDir + "/" + file);
        }
    }

    /** Returns the data of the log */
    public String logData() {
        String res = "Commit " + id + ".\n";
        res += getDateTime();
        return res;
    }

    public Commit parent() {
        return parent;
    }

    public void setParent(Commit p) {
        parent = p;
    }   
}


