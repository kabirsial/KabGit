import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Paths;
import java.util.Scanner;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

/** In order to compare the contents of two files, I received help from the following link:
 *  http://stackoverflow.com/questions/27379059/determine-if-two-files-store-the-same-content
 *  For the process of serialization, I received help from the CS61B UGSI Sarah Kim's Main.java
 *  file.
 *  For copying one file to another, I received help from the following link:
 *  https://docs.oracle.com/javase/tutorial/essential/io/copy.html 
 *  In order to generate the date and time, I received help from the following link:
 *  http://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/ 
 */
public class KabGit {
    private CommitTree commitTree;
    
    /** Initializes by creating a .kabgit folder to store all metadata. If folder already
     *  exists, prints an error message saying that the folder already exists.
     */
    public void init() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String dateTime = dateFormat.format(cal.getTime());
        File file = new File("./.kabgit");
        Commit commit = new Commit("initial commit", 0, new HashMap<String, String>());
        commit.setDateTime(dateTime);
        CommitTree newTree = new CommitTree(commit);        
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Successfully initialized.");
            }
        } else {
            String msg = "A kabgit version control system already exists in the current directory.";
            System.out.println(msg);
        }
        commitTree = newTree;
        saveTree();
    }
    
    /** Loads the commitTree by deserializing from CommitTree.ser . After loading, reassigns. */
    private void loadTree() {
        CommitTree tree = null;
        File treeFile = new File("./.kabgit/CommitTree.ser");
        if (treeFile.exists()) {
            try {
                FileInputStream fileIn = new FileInputStream(treeFile);
                ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                tree = (CommitTree) objectIn.readObject();
            } catch (IOException e) {
                String msg = "IOException while loading treeFile.";
                e.printStackTrace();
                System.out.println(msg);
            } catch (ClassNotFoundException e) {
                String msg = "ClassNotFoundException while loading treeFile.";
                System.out.println(msg);
            }
        }
        commitTree = tree;
    }
    
    /** Saves the commitTree by serializing it. */
    private void saveTree() {
        try {
            File treeFile = new File("./.kabgit/CommitTree.ser");
            FileOutputStream fileOut = new FileOutputStream(treeFile);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(commitTree);
        } catch (IOException e) {
            e.printStackTrace();
            String msg = "IOException while saving CommitTree.";
            System.out.println(msg);
        }
    }
        
    /** Indicates you want to include the file in the upcoming commit as a file that's been
     *  changed. If the file has not been modified since last commit, print error message.
     *  Also if file doesn't exist, throws error message. */
    public void add(String fileName) {
        File fileInput = new File(fileName);
        if (fileInput.exists()) {
            Commit lastCommit = commitTree.currentCommit();
            if (!lastCommit.fileMap().isEmpty()) {
                String lastFileName = lastCommit.retrieveFile(fileName);
                if (lastFileName != null) {
                    File lastFile = new File(lastFileName);
                    if (compareFiles(fileInput, lastFile)) {
                        System.out.println("File has not been modified since the last commit.");
                        return;
                    }
                }   
            }
            commitTree.stageFile(fileName);
            commitTree.unmarkRemoval(fileName);
        } else {
            System.out.println("File does not exist.");
        }
        saveTree();
    }
    
    /** Checks byte-by-byte if two files are equal. 
     *  @param fileIn The first File object to compare.
     *  @param fileOut The second File object to compare.    
     */
    private boolean compareFiles(File fileIn, File fileOut) {
        try {
            byte[] f1 = Files.readAllBytes(fileIn.toPath());
            byte[] f2 = Files.readAllBytes(fileOut.toPath());
            if (Arrays.equals(f1, f2)) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /** Creates a new commit object with a commit message and adds all the currently staged files
     * to it. Also sets the date and time of the commit to the current time and date. Finally 
     * it saves the tree. 
     * @param message String message of what we're about the commit.
     */
    public void commit(String message) {
        if (message == null) {
            return;
        }
        if (commitTree.stagedFiles().isEmpty()) {
            System.out.println("No changes added to the commit.");
            commitTree.clearStagedAndRemove();
            return;
        }
        commitTree.incrementCount();
        Commit lastCommit = commitTree.currentCommit();
        HashMap<String, String> files = new HashMap<String, String>();
        ArrayList<String> stagedFiles = commitTree.stagedFiles();
        ArrayList<String> removedFiles = commitTree.removedFiles();
        for (String fileName: lastCommit.fileMap().keySet()) {
            if (!stagedFiles.contains(fileName) && !removedFiles.contains(fileName)) {
                files.put(fileName, lastCommit.fileMap().get(fileName));
            }
        }
        Commit newCommit = new Commit(message, commitTree.globalCount(), files);
        newCommit.addStagedFiles(commitTree.stagedFiles());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String dateTime = dateFormat.format(cal.getTime());
        newCommit.setDateTime(dateTime);
        commitTree.add(newCommit);
        commitTree.clearStagedAndRemove();
        saveTree();
    }

    /** Takes in a fileName. Checks if it belongs to the map of branches. If it does, we checkout
     * the branch with the given fileName by restoring all files in the working directory
     * to their versions in the commit at the head of the branch, and considers given branch
     * to now be the current branch.
     * If the fileName doesn't exist in the branch map, we check if it exists in the files held
     * at the most recent commit of the given branch. If it exists in these files, call checkout
     * method with the id of the most recent and the fileName.
     * If fileName doesn't exist in the most recent commit, prints an error message.
     * @param fileName String Name of file or branch we're trying to check out.
     */
    public void checkout(String fileName) {
        if (commitTree.branchMap().containsKey(fileName)) {
            checkoutBranch(fileName);
        } else if (commitTree.currentCommit().fileMap().containsKey(fileName)) {
            checkout(commitTree.currentCommit().getID(), fileName);
        } else {
            String msg = "File does not exist in the most recent commit, or no such branch exists.";
            System.out.println(msg);
        }
        saveTree();
    }

    /** Uses a passed in integer commit ID to obtain the Commit object corresponding to that ID.
     *  If the no commit with the given ID exists, prints an error message. If the commit does exist
     *  but doesn't hold the passed in fileName, prints an error message. Otherwise, it restores
     *  the file with fileName in the working directory to be the file that the commit with the 
     *  given ID.
     *  @param commitID integer ID of commit we're trying to checkout the file from.
     *  @param fileName String namme of file we're trying to checkout.
     */
    public void checkout(int commitID, String fileName) {
        Commit lastCommit = commitTree.get(commitID);       
        if (lastCommit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        String copy = lastCommit.retrieveFile(fileName);
        if (copy == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File destFile = new File(copy);
        if (!destFile.exists()) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File toDelete = new File(fileName);
        toDelete.delete();
        copyFile(copy, fileName);
        saveTree();
    }
    
    /** Switches the branch so that the current or most recent commit points to the commit
     *  that the branch with the given branchName points to. Then checks out or restores
     *  files in the working directory to be the files that the given branch's commit holds.
     *  @param branchName String name of branch to checkout
     */
    public void checkoutBranch(String branchName) {
        if (branchName.equals(commitTree.currBranch())) {
            System.out.println("No need to checkout the current branch.");
        }
        Commit currCommit = commitTree.currentCommit();
        Set<String> currFiles = currCommit.files();
        commitTree.switchBranch(branchName);
        Commit commit = commitTree.currentCommit();
        for (String file: commit.files()) {
            checkout(commit.getID(), file);
        }

    }

    /** Marks the file with the given fileName for removal so it is not included
     *  in the next commit. If the file was earlier staged, it removes the file from 
     *  the list of staged files. Also, if the file has not been staged and the most
     *  recent commit doesn't contain the fileName, it prints an error message.
     *  @param fileName String name of file to mark for removal.
     */
    public void remove(String fileName) {
        ArrayList<String> stagedFiles = commitTree.stagedFiles();
        Set<String> currCommitFiles = commitTree.currentCommit().files();
        if (!stagedFiles.contains(fileName) && !currCommitFiles.contains(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        commitTree.markRemoval(fileName);
    }

    /** Prints out the status of all the branches, staged files and files
     *  marked for removal, printing the name of the current branch with a '*'.
     */
    public void status() {
        System.out.println("=== Branches ===");
        for (String branchName: commitTree.branchMap().keySet()) {
            if (branchName.equals(commitTree.currBranch())) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println("\n=== Staged Files ===");
        for (String file: commitTree.stagedFiles()) {
            System.out.println(file);
        }
        System.out.println("\n=== Files Marked for Removal ===");
        for (String rfile: commitTree.removedFiles()) {
            System.out.println(rfile);
        }
    }

    /** Restores all files to their versions in the commit with the given ID
     *  Also moves current branch's head to that commit node.
     *  @param commitID integer id of the commit which we use to reset.
     */
    public void reset(int commitID) {
        Commit commit = commitTree.get(commitID);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        int id = commit.getID();
        for (String file: commit.files()) {
            checkout(id, file);
        }
        commitTree.resetPointer(commit);
    }

    
    /** Merges files from the head of the given branch onto the head of the current
     *  branch. If the passed in branchName is of the branch that we're currently at,
     *  prints an error message
     *  Finds the split node. Finds the files contained in the commit at the split
     *  node, the commit at the current branch, and the commit at the given branch.  
     *  Iterates through the current files, and finds the files that were modified
     *  since the split commit and stores in these modified files in a set.
     *  Then, iterates through the files in the given branch's commit in order 
     *  to find out the files that were modified since the split commit.
     *  Finally, iterates through the files in the given branch's commit and copies
     *  conflicted files to working directory, if the file exists in the modified
     *  files of the given branch's commit, and if the file exists in the modified
     *  files of the current branch's commit. These conflicted copies are made with .conflicted
     *  The files that exist as part of the given modified files and non modified files
     *  are simply copied to the working directory.
     *  @param branchName String name of the branch to merge with.
     */
    public void merge(String branchName) {
        if (branchName.equals(commitTree.currBranch())) {
            System.out.println("Cannot merge a branch with itself."); return;
        } else if (!commitTree.branchMap().containsKey(branchName)) {
            System.out.println("A branch with that name does not exist."); return;
        }
        Commit splitCommit = commitTree.findSplitPoint(branchName); 
        Commit givenCommit = commitTree.branchMap().get(branchName); 
        Commit currCommit = commitTree.currentCommit(); 
        Set<String> givenFiles = givenCommit.files(); 
        Set<String> currFiles = currCommit.files(); 
        Set<String> splitFiles = new HashSet<String>();
        if (splitCommit != null) {
            splitFiles = splitCommit.files();
        }
        Set<String> modGiven = new HashSet<String>();
        for (String givenFile: givenFiles) {
            if (splitFiles != null) {
                if (!splitFiles.contains(givenFile)) {
                    modGiven.add(givenFile);
                } else {
                    File in = new File(splitCommit.retrieveFile(givenFile));
                    File out = new File(givenCommit.retrieveFile(givenFile));
                    if (!compareFiles(in, out)) {
                        modGiven.add(givenFile);
                    }
                }
            } else {
                modGiven.add(givenFile);
            }
        }
        Set<String> modCurr = new HashSet<String>();
        for (String currFile: currFiles) {
            if (splitFiles != null) {
                if (!splitFiles.contains(currFile)) {
                    modCurr.add(currFile);
                } else {
                    File in = new File(splitCommit.retrieveFile(currFile));
                    File out = new File(currCommit.retrieveFile(currFile));
                    if (!compareFiles(in, out)) {
                        modCurr.add(currFile);
                    }
                }
            } else {
                modCurr.add(currFile);
            }
        }
        for (String gFile: givenFiles) {
            if (modGiven.contains(gFile) && !modCurr.contains(gFile)) {
                copyFile(givenCommit.retrieveFile(gFile), gFile);
            } else if (modGiven.contains(gFile) && modCurr.contains(gFile)) {
                copyFile(givenCommit.retrieveFile(gFile), gFile + ".conflicted");
            }
        }
    }

    /** Copies the file from the given source path, to the the given dest 
     *  @param source String source file
     *  @param dest String destination file
     */
    private void copyFile(String source, String dest) {
        try {
            Files.copy(Paths.get(source), Paths.get(dest), REPLACE_EXISTING, COPY_ATTRIBUTES);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** First checks if a branch with branchName exists in the branchMap. If it doesn't, 
     *  prints an error message. Then, checks if the given branchName equals the current 
     *  branch's name, and prints an error message if it does. If the given branch is in
     *  history of the current branch, prints an error message. If the current branch is in
     *  the history of the given, branch, just switches the current branch's commit node to
     *  point to the commit node that the given branch points to.
     *  Finds the files modified in the given branch since the split commit and stores them in a
     *  set. Finds the files modified in the current branch since the split and stores them in a
     *  set. Removes all the files from the set of modified files in the given branch that existed 
     *  in the set of files that were modified in the current branch.
     *  Creates an array list of replayed commits to be rebased.
     *  Changes the current branch's pointer to point to the commit node that the given branch 
     *  points to. Iterates through this array list in order to add these replayed commits to the 
     *  commit tree. Finally, copies all the most recent files in the most recent replayed commit, 
     *  into the working directory.
     * @param branchName String name of branch to rebase with
     * @param interactive Rebases the commits interactively when this is true
     */
    public void rebase(String branchName, boolean interactive) {
        if (!commitTree.branchMap().containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (commitTree.currBranch().equals(branchName)) {
            System.out.println("Cannot rebase a branch onto itself.");
            return;
        } else if (commitTree.checkHistory(branchName)) {
            System.out.println("Already up-to-date.");
            return;
        } else if (commitTree.checkHistory2(branchName)) {
            commitTree.changePointer(branchName);
            return;
        }
        Commit splitCommit = commitTree.findSplitPoint(branchName);
        Commit givenCommit = commitTree.branchMap().get(branchName);
        Commit currCommit = commitTree.currentCommit();
        Set<String> splitCommitFiles = splitCommit.files();
        Set<String> currCommitFiles = currCommit.files();
        Set<String> givenCommitFiles = givenCommit.files();
        HashMap<String, String> givenModified = new HashMap<String, String>();
        for (String givenFile: givenCommitFiles) {
            if (!splitCommitFiles.contains(givenFile)) {
                givenModified.put(givenFile, givenCommit.retrieveFile(givenFile));
            } else if (splitCommitFiles.contains(givenFile)) {
                File in = new File(splitCommit.retrieveFile(givenFile));
                File out = new File(givenCommit.retrieveFile(givenFile));
                if (!compareFiles(in, out)) {
                    givenModified.put(givenFile, givenCommit.retrieveFile(givenFile));
                }
            }
        }       
        HashSet<String> currModified = new HashSet<String>();
        for (String currFile: currCommitFiles) {
            if (!splitCommitFiles.contains(currFile)) {
                currModified.add(currFile);
            } else if (splitCommitFiles.contains(currFile)) {
                File in = new File(splitCommit.retrieveFile(currFile));
                File out = new File(currCommit.retrieveFile(currFile));
                if (!compareFiles(in, out)) {
                    currModified.add(currFile);
                }
            }
        }
        for (String mod: currModified) {
            givenModified.remove(mod);
        }
        ArrayList<Commit> shallowCommit = new ArrayList<Commit>();
        int last = commitTree.globalCount();
        if (interactive) {
            shallowCommit = commitTree.findShallowCopyI(splitCommit, givenModified);
        } else {
            shallowCommit = commitTree.findShallowCopy(splitCommit, givenModified);
        }
        commitTree.changePointer(branchName);
        int j = 0;
        for (int i = shallowCommit.size() - 1; i >= 0; i--) {
            shallowCommit.get(i).setID(last + j + 1);
            commitTree.add(shallowCommit.get(i));
            j += 1;
        }
        for (String file: commitTree.currentCommit().files()) {
            copyFile(commitTree.currentCommit().retrieveFile(file), file);
        }
    }
    
    /** Reads input from the user for dangerous commands and returns true only if 
      * this input is the string "yes". */
    public boolean dangerousAnswer() {
        Scanner scanner = new Scanner(System.in);
        String prompt = "The command you entered may alter the files in your working directory. ";
        prompt += "Uncommitted changes may be lost. Are you sure you want to continue? (yes/no)";
        System.out.println(prompt);
        String msg = scanner.nextLine();
        return msg.equals("yes");
    }
    
    public static void main(String[] args) {
        KabGit git = new KabGit();
        git.loadTree();
        String cmd = "";
        if (args.length > 0) {
            cmd = args[0];
        }
        try {
            switch (cmd) {
                case "init": 
                    git.init(); break;
                case "add": 
                    git.add(args[1]); break;
                case "commit": 
                    git.commit(args[1]); break;
                case "log": 
                    git.commitTree.logData(); break;
                case "global-log": 
                    git.commitTree.globalLog(); break;
                case "checkout":
                    if (git.dangerousAnswer()) {
                        if (args.length == 2) {
                            git.checkout(args[1]);
                        } else if (args.length == 3) {
                            git.checkout(Integer.parseInt(args[1]), args[2]);
                        }
                    }
                    break;
                case "rm": 
                    git.remove(args[1]); break;
                case "status": 
                    git.status(); break;
                case "find": 
                    git.commitTree.find(args[1]); break;
                case "branch": 
                    git.commitTree.createBranch(args[1]); break;
                case "rm-branch": 
                    git.commitTree.removeBranch(args[1]); break;
                case "reset": 
                    if (git.dangerousAnswer()) {
                        git.reset(Integer.parseInt(args[1]));
                    }
                    break;
                case "merge": 
                    if (git.dangerousAnswer()) {
                        git.merge(args[1]);
                    }
                    break;
                case "rebase": 
                    if (git.dangerousAnswer()) {
                        git.rebase(args[1], false);
                    }
                    break;
                case "i-rebase": 
                    if (git.dangerousAnswer()) {
                        git.rebase(args[1], true);
                    }   
                    break;
                case "add-remote":    
                default: 
                    break;
            }
            git.saveTree();
        } catch (IndexOutOfBoundsException e1) {
            e1.printStackTrace();
        } catch (NullPointerException e2) {
            e2.printStackTrace();
        }
    }

}


