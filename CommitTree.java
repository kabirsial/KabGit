import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Scanner;

public class CommitTree implements Serializable {
    private ArrayList<String> stagedFiles = new ArrayList<String>();
    private ArrayList<String> removeFiles = new ArrayList<String>();
    private HashMap<String, ArrayList<Integer>> messageToID = new HashMap<String, ArrayList<Integer>>();
    private HashMap<String, Commit> branchMap = new HashMap<String, Commit>();
    private HashMap<Integer, Commit> commitMap = new HashMap<Integer, Commit>();
    private int globalCount = 0;
    private Commit currPointer;
    private String currBranch;
    
    /** Initializes the CommitTree with the given commit and starts at master branch. 
     *  @param commit Commit object with which to build the commit tree. */
    public CommitTree(Commit commit) {
        currPointer = commit;
        currBranch = "master";
        branchMap.put(currBranch, currPointer);
        commitMap.put(commit.getID(), commit);
    }
    
    /** Adds file with fileName to the list of staged files. 
     *  @param fileName */
    public void stageFile(String fileName) {
        if (!stagedFiles.contains(fileName)) {
            stagedFiles.add(fileName);
        }
    }
    
    /** Unstage the file with the given fileName. 
     *  @param fileName */
    public void unStageFile(String fileName) {
        stagedFiles.remove(fileName);
    }

    /** Marks the file with the given fileName for removal. 
     *  @param fileName */
    public void markRemoval(String fileName) {
        removeFiles.add(fileName);
    }
    
    /** Removes file with fileName from the list of files that are to be removed 
     *  @param fileName */
    public void unmarkRemoval(String fileName) {
        removeFiles.remove(fileName);
    }
    
    /** Returns the Commit object that the tree currently points at */
    public Commit currentCommit() {
        return currPointer;
    }
    
    /** Adds the given commit to the commit tree. Reassigns the currPointer to the given
     *  CommitTree. Also assigns the parent of this commit to the what was originally the
     *  currPointer. 
     *  @param commit */
    public void add(Commit commit) {
        Commit temp = currPointer;
        commit.setParent(temp);
        currPointer = commit;
        if (!messageToID.containsKey(commit.getMessage())) {
            messageToID.put(commit.getMessage(), new ArrayList<Integer>());
        }
        messageToID.get(commit.getMessage()).add(commit.getID());
        commitMap.put(commit.getID(), commit);
    }
    
    /** Returns a list of the stagedFiles. */
    public ArrayList<String> stagedFiles() {
        return stagedFiles;
    }
    
    /** Returns a list of the removedFiles. */
    public ArrayList<String> removedFiles() {
        return removeFiles;
    }
    
    /** Clears the lists containing the staged files and the files marked for removal. */
    public void clearStagedAndRemove() {
        stagedFiles.clear();
        removeFiles.clear();
    }

    /** Returns the global count of all the commit objects ever made. */
    public int globalCount() {
        return globalCount;
    }

    public void incrementCount() {
        globalCount += 1;
    }

    /** Returns the Commit object corresponding to the given global id. 
     *  @param id */
    public Commit get(int id) {
        return commitMap.get(id);
    }

    /** Prints out the log of all the commits starting at the current head pointer. */
    public void logData() {
        Commit temp = currPointer;
        while (temp != null) {
            System.out.println("====\nCommit " + temp.getID() + ".\n" + temp.getDateTime());
            temp = temp.parent();
        }
    }

    /** Prints out the global log of all commits ever made in the commit tree. */
    public void globalLog() {
        for (Integer id: commitMap.keySet()) {
            System.out.println("====\nCommit " + id + ".\n" + commitMap.get(id).getDateTime());
        }
    }
    
    /** Returns a hashmap that maps from string branch names to the Commit objects they point to. */
    public HashMap<String, Commit> branchMap() {
        return branchMap;
    }

    /** Creates a new branch with the given branchName and adds it to the branchMap. 
     *  @param branchName */
    public void createBranch(String branchName) {
        Commit newPointer = currPointer;
        branchMap.put(branchName, newPointer);
    }

    /** Switches the current pointer to point at the the branch specified by given branchName. 
     *  @param branchName */
    public void switchBranch(String branchName) {
        Commit temp = currPointer;
        branchMap.put(currBranch, temp);
        currPointer = branchMap.get(branchName);
        currBranch = branchName;
    }

    public String currBranch() {
        return currBranch;
    }

    /** Removes the branch with the given branchName. 
     *  @param branchName */
    public void removeBranch(String branchName) {
        if (branchName.equals(currBranch)) {
            System.out.println("Cannot remove the current branch.");
        } else if (!branchMap.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else {
            branchMap.remove(branchName);
        }
    }

    /** Prints out all the global Commit IDs that refer to the same commit message. 
     *  @param message */
    public void find(String message) {
        if (!messageToID.containsKey(message)) {
            System.out.println("Found no commit with that message");
        } else  {
            for (Integer id: messageToID.get(message)) {
                System.out.println(id);
            }
        }
    }
    
    /** Resets the currPointer to point at the given commit. 
     *  @param commit */
    public void resetPointer(Commit commit) {
        currPointer = commit;
    }

    /** Finds the split point between the currPointer and the given branchName. 
     *  @param branchName */
    public Commit findSplitPoint(String branchName) {
        Commit curr = currPointer;
        Commit given = branchMap.get(branchName);
        HashSet<Commit> currParents = new HashSet<Commit>();
        HashSet<Commit> givenParents = new HashSet<Commit>();
        while (curr != null) {
            currParents.add(curr);
            curr = curr.parent();
        }
        while (given != null) {
            if (currParents.contains(given)) {
                return given;
            }
            givenParents.add(given);
            given = given.parent();
        }
        return null;
    }

    /** Checks if the commit corresponding to the given branch is in history of current branch 
     *  @param branchName */
    public boolean checkHistory(String branchName) {
        Commit branchPoint = branchMap.get(branchName);
        Commit currTemp = currPointer;
        while (currTemp != null) {
            if (currTemp == branchPoint) {
                return true;
            }    
            currTemp = currTemp.parent();
        }
        return false;
    }

    /** Checks if the commit corresponding to the current branch is in history of given branch 
     *  @param branchName */
    public boolean checkHistory2(String branchName) {
        Commit branchTemp = branchMap.get(branchName);
        while (branchTemp != null) {
            if (branchTemp == currPointer) {
                return true;
            }
            branchTemp = branchTemp.parent();
        }
        return false;
    }

    /** Returns the map that maps integer commit ids to commit objects. */
    public HashMap<Integer, Commit> commitMap() {
        return commitMap;
    }

    /** Returns an array list of copies of commit objects starting from the current commit node
     *  till the passed in splitCommit object. Also takes in a map that makes fileNames to their
     *  locations. This is used while using rebase.
     *  @param splitCommit
     *  @param map */
    public ArrayList<Commit> findShallowCopy(Commit splitCommit, Map<String, String> map) {
        Commit currTemp = currPointer;
        ArrayList<Commit> shallowCopy = new ArrayList<Commit>();
        while (currTemp != splitCommit) {
            globalCount += 1;
            Commit commit = new Commit(currTemp.getMessage(), globalCount, currTemp.fileMap());
            commit.fileMap().putAll(map);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            String dateTime = dateFormat.format(cal.getTime());
            commit.setDateTime(dateTime);
            shallowCopy.add(commit);
            currTemp = currTemp.parent();
        }
        return shallowCopy;
    }

    /** Has the same functionality as the previous method, except that this is called while using
     *  interactive rebase. Has the option to skip commits, change the commit messages while 
     *  replaying them, and to simply continue, preserving the functionality of the above mentioned
     *  method. */
    public ArrayList<Commit> findShallowCopyI(Commit splitCommit, Map<String, String> map) {
        Commit currTemp = currPointer;
        ArrayList<Commit> shallowCopy = new ArrayList<Commit>();
        String msg = "Would you like to (c)ontinue, (s)kip this commit, or change this commit's (m)essage?";
        Scanner scanner = new Scanner(System.in);
        String s = "";
        int i = 0;
        while (true) {
            if (currTemp == splitCommit) {
                break;
            }
            System.out.println("Currently replaying:");
            System.out.println(currTemp.logData());
            System.out.println(msg);
            s = scanner.nextLine();
            if (s.equals("c")) {
                globalCount += 1;
                Commit commit = new Commit(currTemp.getMessage(), globalCount, currTemp.fileMap());
                commit.fileMap().putAll(map);
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Calendar cal = Calendar.getInstance();
                String dateTime = dateFormat.format(cal.getTime());
                commit.setDateTime(dateTime);
                shallowCopy.add(commit);
                currTemp = currTemp.parent();
                i += 1;
            } else if (s.equals("s") && i != 0 && currTemp.parent() != splitCommit) {
                currTemp = currTemp.parent();
                i += 1;
            } else if (s.equals("m")) {
                System.out.println("Please enter a new message for this commit.");
                String input = scanner.nextLine();
                globalCount += 1;
                Commit commit = new Commit(input, globalCount, currTemp.fileMap());
                commit.fileMap().putAll(map);
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Calendar cal = Calendar.getInstance();
                String dateTime = dateFormat.format(cal.getTime());
                commit.setDateTime(dateTime);
                shallowCopy.add(commit);
                i += 1;
                currTemp = currTemp.parent();
            }
        }
        return shallowCopy;
    }

    /** Changes the current pointer to point to the commit objec that the commit object that the 
     *  given branch points to. 
     *  @param branchName */
    public void changePointer(String branchName) {
        currPointer = branchMap.get(branchName);
    }
    
}


