package bluej.browser;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.plaf.basic.*;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import java.awt.*;
import java.awt.event.*;

import java.util.*;
import java.util.zip.*;
import java.util.jar.*;

import java.io.*;

import bluej.Config;
import bluej.pkgmgr.PackageTarget;
import bluej.pkgmgr.Package;
import bluej.utility.ModelessMessageBox;
import bluej.utility.Utility;
import bluej.utility.DialogManager;
import bluej.utility.Debug;
import bluej.classmgr.ClassMgr;
import bluej.classmgr.ClassPathEntry;

/**
 * A JPanel subclass with a JTree containing all the packages specified in the
 * configuration files.
 * 
 * @author Andy Marks
 * @author Andrew Patterson
 * @version $Id: LibraryChooser.java 277 1999-11-16 00:57:17Z ajp $
 */
public class LibraryChooser extends JPanel implements Runnable
{

    private LibraryChooserNode root = null;
    private JTree tree = null;
    private DefaultTreeModel treeModel = null;
    
    // popup menus
    //  private LibraryPopupMenu libPopup = new LibraryPopupMenu(this);

    /**
     * Create a new LibraryChooser.  Initialize the tree from the libraries
     * specified in the configuration files.
     * 
     * @param parent the LibraryBrowser object containing the LibraryChooser.
     */
    public LibraryChooser()
    {
        setBackground(Color.white);
    
        loadLibraries();

    }

    /**
     * This component will raise LibraryChooserEvents when nodes are
     * selected in the tree. The following functions manage this.
     */
         
    public void addLibraryChooserListener(LibraryChooserListener l) {
        listenerList.add(LibraryChooserListener.class, l);
    }

    public void removeLibraryChooserListener(LibraryChooserListener l) {
        listenerList.remove(LibraryChooserListener.class, l);
    }

    // notify all listeners that have registered interest for
    // notification on this event type.
    protected void fireNodeEvent(LibraryChooserNode node) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i] == LibraryChooserListener.class) {
                ((LibraryChooserListener)listeners[i+1]).nodeEvent(
                        new LibraryChooserEvent(this,
                                LibraryChooserEvent.NODE_CLICKED, node));
            }	       
        }
    }	

    /**
     * Create and return an array of Strings to identify all the 
     * classes that belong to the package represented by the node
     * ie if the node represented the package java.lang, the returned
     * array would contains values such as { "Integer", "Float" etc }.
     * 
     * @param node  the node representing the package
     * @return the base names of classes in this package
     */
    public String[] findClassesOfPackage(LibraryChooserNode node)
    {
        Object files[] = node.getFiles();
        int numberFiles = files == null ? 0 : files.length;
        String result[] = new String[numberFiles];
    
        // all values in files array will be class files in this package's directory
        for (int current = 0; current < numberFiles; current++)
            result[current] = (String)files[current];

        return result;
    }

    /**
     * Create and return an array of Strings to identify all the 
     * packages which are nested in the package represented by the node
     * ie if the node represented the package java.lang, the returned
     * array would contain { "reflect", etc }
     * 
     * @param node  the node representing the package
     * @return the base names of nested packages in this package
     */
    public String[] findNestedPackagesOfPackage(LibraryChooserNode node)
    {
        int numberEntries = node.getChildCount();
        String result[] = new String[numberEntries];

        // all node children will be nested packages
        for (int current=0; current < numberEntries; current++)
            result[current] = ((LibraryChooserNode)node.getChildAt(current)).toString();

        return result;
    }

    /**
     * Use the user and system configuration files to load the libraries
     * into the tree.  Create a hashtable of all the libraries and their
     * aliases, and then use the hashtable to build the tree in a separate
     * thread.
     */
    public void loadLibraries()
    {
        if (tree == null) {
            // first time
            root = new LibraryChooserNode("");
            tree = new JTree(root);        // init tree here so refresh works properly
            treeModel = (DefaultTreeModel)tree.getModel();
            tree.setRootVisible(false);
            tree.setBorder(new EmptyBorder(5,5,5,5));
        } else {
            // clean tree
            Enumeration topLevelNodes = root.children();

            while (topLevelNodes.hasMoreElements())
                treeModel.removeNodeFromParent((DefaultMutableTreeNode)topLevelNodes.nextElement());
            root.removeAllChildren();
            treeModel.reload();
        }
    
        new Thread(this).start();
    }
    
    /**
     * Separate process used to load libraries whilst the rest of the browser
     * inits.  Iterate through the libraries adding each one to the tree.
     * 
     * The methods which are invoked from this method could conceivably
     * take quite a while to run, so they have been spawned from a separare
     * thread to allow the rest of the library chooser to begin immediately.
     * No attempt at thread synchronization is made here, so beware of any
     * attemps to access the tree's contents before this method has completed.
     */
    public void run() {

        Iterator libraries = ClassMgr.getClassMgr().getAllClassPathEntries();
    
        while (libraries.hasNext()) {
            ClassPathEntry cpe = (ClassPathEntry)libraries.next();
                
            addLibraryToTree(new ClassPathEntryNode(cpe));
        }
                               
        setupTree();
    
        // dont' add tree to UI until all it's data has been loaded.
        JScrollPane scrollPane = new JScrollPane(tree);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    
        revalidate();
    }
    
    /**
     * Perform tree initialization such as setting up
     * listeners to display information about a selected
     * tree node on the status line, expanding the tree
     * to make all the top level nodes visible and setting
     * the renderer to a LibraryTreeCellRenderer.
     */
    private void setupTree() {
        tree.getSelectionModel().setSelectionMode (TreeSelectionModel.SINGLE_TREE_SELECTION);

        // listen for when the selection changes.

        tree.addTreeSelectionListener(
            new TreeSelectionListener()
            {
                public void valueChanged(TreeSelectionEvent e) {
                    LibraryChooserNode node = (LibraryChooserNode)(e.getPath().getLastPathComponent());

                    fireNodeEvent(node);
                }
            }
        );     
        
        tree.setEditable(false);
        
        // make sure all the top level libraries are initially visible and expanded
    
        Enumeration topLevelNodes = root.children();
        while (topLevelNodes.hasMoreElements())
            tree.expandPath(new TreePath(((DefaultMutableTreeNode)topLevelNodes.nextElement()).getPath()));
            
        // let's use the same icon for all nodes
        DefaultTreeCellRenderer rend = new DefaultTreeCellRenderer();
        rend.setLeafIcon(null);
        rend.setClosedIcon(null);
        rend.setOpenIcon(null);
        
        tree.setCellRenderer(rend); //new LibraryTreeCellRenderer(this));
    }

    /**
     * Remove all branches of the tree that do not contain any files
     * (unless they are .class files).
     * 
     * @param node the node of the tree from which to start pruning.
     */
    private void pruneNodeOfEmptyDirectories(LibraryChooserNode node) {
        boolean prunedOnLastPass = true;
        
        // short circuit the pruning for a completely empty node
        if(node.getChildCount() == 0) {
            node.removeFromParent();
            return;
        }
        
        while (prunedOnLastPass) {
            prunedOnLastPass = false;
            
            Enumeration nodes = node.depthFirstEnumeration();
            
            LibraryChooserNode nextNode = null;

            while (nodes.hasMoreElements()) {
                nextNode = (LibraryChooserNode) nodes.nextElement();
                
                if (nextNode.isLeaf() && 
                    nextNode.getFiles() == null &&
                    nextNode != root)
                    {
                        nextNode.removeFromParent();
                        prunedOnLastPass = true;
                    }
            }
        }
    }

    /**
     * Make sure the node for the <code>thePackage</code> is visible in
     * the tree.  Invoked when a new package in the class chooser is
     * double clicked.
     * <strong>Note: will not select the root node.</strong>
     * <strong>Known bugs: will not work with a package with more than 20 elements (i.e., 1.2.,19.20.21)</strong>
     * 
     * @param thePackage the name of the package to select in absolute path format
     * @return true if the package was found and selected
     */
    public boolean selectPackageInTree(String thePackage) {
        /*
          Sample thePackage parameter: "C:\bluej.src\bluej.debugger"
          Algorithm to find tree node to select based on thePackage parameter:
          (1) find alias matching start of string
          (2) if no alias found, no matching node
          (3) open node containing alias
          (4) tokenize rest of parameter based on File.separator
          (5) take next token
          (6) find node from previous root matching token
          (7) select that node
          (8) go to step 5
        */
//        LibraryChooserNode tmpNodesInPathToFocus[] = new LibraryChooserNode[20];
  //      tmpNodesInPathToFocus[0] = root;
    //    LibraryChooserNode topLevelNode = getFirstNodeContainingDir(thePackage);
      //  if (topLevelNode == null) {
            // we couldn't find the top level node containing the package
       //     DialogManager.showError(LibraryBrowserPkgMgrFrame.getFrame(), 
        //                      "cannot-locate-lib-node");
         //   return false;
        //}
        //tmpNodesInPathToFocus[1] = topLevelNode;
    
        // tokenize the remainder of thePackage that is left over after what is matching
        // in topLevelNode is removed (i.e., if thePackage = c:\a\b\c and topLevelNode 
        // includes c:\a, then tokenize on \b\c).  If nothing is left to tokenize, then the
        // top level node is an exact match for thePackage, so skip the loop and select it.
   /*     String stringToTokenize = thePackage.substring(topLevelNode.getInternalName().toString().length(), thePackage.length());
        StringTokenizer nodeTokens = new StringTokenizer(stringToTokenize, File.separator);
        LibraryChooserNode root = topLevelNode;
        int nextPosInPath = 2;
        boolean foundNode = false;
        while (nodeTokens.hasMoreElements()) {
	    Enumeration rootChildren = root.children();
            String nextBitInPath = nodeTokens.nextElement().toString();
            // search in the children of root for a UserObject matching nextBitInPath
            foundNode = false;
            while(rootChildren.hasMoreElements()) {
	        LibraryChooserNode nextChild = (LibraryChooserNode)rootChildren.nextElement();
                if (nextChild.getInternalName().equals(nextBitInPath)) {
		    root = tmpNodesInPathToFocus[nextPosInPath] = nextChild;
                    foundNode = true;
                    break;
		}
	    }
            if (foundNode == true)
	        nextPosInPath++;
	    else {
	        DialogManager.showError(LibraryBrowserPkgMgrFrame.getFrame(), 
			"cannot-locate-lib-node");
                return false;
            }
        }
        
        LibraryChooserNode nodesInPathToFocus[] = new LibraryChooserNode[nextPosInPath];
        System.arraycopy(tmpNodesInPathToFocus, 0, nodesInPathToFocus, 0, nextPosInPath);
        tree.setSelectionPath(new TreePath(nodesInPathToFocus));        
                    */
        return true;
    }
    
    /**
     * Return an array of TreePath objects containing all nodes in the tree
     * that contain the <code>pattern</code>.  Called (originally) by the
     * FindLibraryDialog once a search term has been entered.
     * 
     * @return all paths in the tree matching the pattern, given the criteria, or null
     * @param pattern the string to find within the paths in the tree
     * @param caseSensitive true if the match must be case sensitive
     * @param substringSearch true if the match can be on part of the library
     */
    public TreePath[] findAllLibrariesMatching(String pattern, boolean caseSensitive, boolean substringSearch) {
        Enumeration nodes = root.depthFirstEnumeration();
        Vector results = new Vector();
        while (nodes.hasMoreElements()) {
            LibraryChooserNode thisNode = (LibraryChooserNode)nodes.nextElement();
            if (doesLibraryMatchPattern(new TreePath(thisNode.getPath()), pattern, caseSensitive, substringSearch))
                results.addElement(new TreePath(thisNode.getPath()));
        }
        if (results.size() == 0)
            return null;
        
        TreePath[] matches = new TreePath[results.size()];
        results.copyInto(matches);
        return matches;
    }

    /**
     * Given a particular TreePath, try to match it against a pattern.
     * 
     * @param library the library to search
     * @param pattern the pattern to look for in the library
     * @param caseSensitive true if a case sensitive match is required
     * @param substringSearch true if the match can be on any part of the string
     * @return true if the library matches the pattern (given the criteria)
     */
    private static boolean doesLibraryMatchPattern(TreePath library, String pattern, boolean caseSensitive, boolean substringSearch) {
        if (!caseSensitive) {
            pattern = pattern.toLowerCase();
        }
        
        String node = null;
        for (int current = 0; current < library.getPathCount(); current++) {
            node = ((LibraryChooserNode)library.getPathComponent(current)).toString();
            if (!caseSensitive)
                node = node.toLowerCase();
            
            if (substringSearch) {
                if (node.indexOf(pattern) != -1) {
                    return true;
                }
            } else {
                if (node.equals(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Return the first top level tree node containing a substring of <code>dir</code>
     * at the start of it's user object.
     * <strong>Note: assumes case insensitivity of directory names (BAD!)</strong>
     * 
     * @param dir the directory to try and match in the top level nodes
     * @return the first top level node with a user object matching <code>dir</code>
     */
    private LibraryChooserNode getFirstNodeContainingDir(String dir) {
        Enumeration topLevelNodes = root.children();
        LibraryChooserNode node = null;
        while (topLevelNodes.hasMoreElements()) {
            node = (LibraryChooserNode)topLevelNodes.nextElement();
            //String search = (node.hasShadowArea() ? node.getShadowArea() : node.getInternalName());
//            String search = node.getInternalName();
//            if (dir.toLowerCase().indexOf(search.toLowerCase()) != -1)
//                return node;
        }
        return null; // no matching top level node found
    }
    
    /**
     * Add a new library to the tree. New libraries can only be added
     * to the root of the tree.  The library is checked to ensure the path is
     * a valid one on the current filesystem and that the library
     * hasn't already been added to the tree.  Responsibility for
     * adding the library is delegated to a function specialized for
     * adding this <em>type</em> of library (i.e., ZIP/JAR or directory)
     * 
     * @param cpeNode the ClassPathEntryNode object to add to the tree
     */
    private void addLibraryToTree(ClassPathEntryNode cpeNode)
    {     
        //        if (getFirstNodeWithObject(root, library) != null) {
        //          System.err.println(library + " already exists");
        //          return;
        //      }

        // add the new node to the root
        treeModel.insertNodeInto(cpeNode, root, root.getChildCount());
        
        if (cpeNode.isJar()) {
            //                setStatusText("Opening jarzip " + alias + " " + library + "...");
            openArchiveLibrary(cpeNode);
        } else if (cpeNode.isClassRoot()) {
            //      setStatusText("Opening dir " + alias + " " + library + "...");
            openDirectoryLibrary(cpeNode);
        }

        // we need to prune with each new node,
        // rather than the entire tree because when new
        // libraries are added - we only need to worry about
        // the new library, not the entire tree
        pruneNodeOfEmptyDirectories(cpeNode);
    }
    
    /**
     * Add an archive library (i.e., ZIP or JAR file) to the tree.
     * Because the archive could (very likely) be stored with directories,
     * We need to parse each entry, extract the paths contained within, 
     * and see if part/all/none of the tree has been created yet.
     * 
     * @param top the node of the tree to be the parent of the library
     * @param file the file representation of the library
     * @param alias the display name for the library
     */
    private void openArchiveLibrary(ClassPathEntryNode cpe) {

        try {
            JarFile archiveFile = new JarFile(cpe.getFile());

            Enumeration files = archiveFile.entries();
            String entryName = "";
            String statusString = Config.getString("browser.librarychooser.openingarchive.status") + " " + archiveFile.getName();

            while (files.hasMoreElements()) {
                    
                entryName = ((JarEntry) files.nextElement()).getName();

                if (entryName.endsWith(".class") &&
                        entryName.indexOf('$') == -1) {
                    addArchiveEntry(cpe, entryName);
                }
            }
        
            archiveFile.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    /**
     * Add a single entry from an archive file to the tree.
     * We have a single entry from an archive file, which could be a file
     * (with or without a preceeding directory), or simply a directory.
     * We need to add this file (with directory structure, if any) to
     * the appropriate place in the tree.
     * 
     * @param top the node to be the parent of the new entry.
     * @param entry the new entry to add to the tree.
     */
    private void addArchiveEntry(LibraryChooserNode top, String entry) {
        // it must be a class file
        if (!entry.endsWith(".class"))
            return;

        // find first slash to see whether entry contains directories or not
        int slashPos = entry.indexOf("/");
    
        // simplest case - no directories in entry,
        // so we just have a .class file.  This can be thrown
        // away as we can assume it's parent directory structure
        // has been created.
        if (slashPos == -1) {

            int ind = entry.indexOf(".class");
            if(ind >= 0)        // must be true but better safe than sorry
                entry = entry.substring(0, ind);                                                              
            
            top.addFile(entry);
            return;
        }
    
        // entry starts with a slash - remove it and continue
        if (slashPos == 0)
            entry = entry.substring(1, entry.length());
    
        // normal case - slash found not at start
        int nextSlashPos = entry.indexOf("/", slashPos);
        if (nextSlashPos == -1) {
            // we have something like /dirname or /file.class
            return;
        }
    
        LibraryChooserNode newNode = new PackageNode(entry.substring(0, slashPos));
        LibraryChooserNode foundNode = getFirstNodeWithObject(top, newNode.getUserObject().toString());
        if (foundNode != null) {
            // recurse
            addArchiveEntry(foundNode, entry.substring(slashPos + 1, entry.length()));    
        } else {
            top.add(newNode);
            // recurse
            addArchiveEntry(newNode, entry.substring(slashPos + 1, entry.length()));    
        }
            
    }

    private void openDirectoryLibrary(ClassPathEntryNode cpe) {
        openDirectoryLibrary(cpe, cpe.getFile());
    }

    /**
     * This library is a directory, so traverse it, building the tree as you go.
     * The library is checked to ensure it exists and hasn't already been added
     * to the tree.  The directory is recursed, with each child directory being
     * added to the tree.
     * 
     * @param top the node of the tree to be the parent of the library.
     * @param the file representation of the library.
     **/
    private void openDirectoryLibrary(LibraryChooserNode top, File file)
    {
        if (!file.exists() && !file.isDirectory()) {
            // because of the long startup time, this should probably be in a separate thread
            DialogManager.showErrorWithText((JFrame)getParent(), 
					    "missing-shadow", file.getName());
            return;
        }

        String[] contents = file.list();

        File newFile = null;
        LibraryChooserNode newNode = null;
            
        // needed to allow correct identification of file type
        String path = file.getPath(); 
            
        for (int current = 0; current < contents.length; current++) {

            // contruct the new file with the path of the parent
            newFile = new File(path, contents[current]);
            
            // we're only interested in class files
            if (!newFile.isFile() && !newFile.isDirectory())
                continue;
            
            if (newFile.isFile())
            {
                if (newFile.getName().endsWith(".class") &&
                     newFile.getName().indexOf('$') == -1)
                {
                    String entry = newFile.getName();
                    int ind = entry.indexOf(".class");
                    if(ind >= 0)        // must be true but better safe than sorry
                        entry = entry.substring(0, ind);                                                              

                    top.addFile(entry);
                }
            } else {
            
                newNode = new LibraryChooserNode(contents[current]);
                top.add(newNode);
            
                // recurse if directory (implied from isFile() test above I
                // guess but its best to be safe)
                if (newFile.isDirectory())
                    openDirectoryLibrary(newNode, newFile);
            }
        }
    }

    /**
     * Helper method to find the first child node of a node
     * containing the user object matching <code>object</code>.
     * Examine the children of a specified parent node for a 
     * matching user object.
     * 
     * @param node the parent node of the children to examine
     * @param object the user object to search for.
     * @return the found node, or null if no node found
     */
    private static LibraryChooserNode getFirstNodeWithObject(LibraryChooserNode node, Object object)
    {
        LibraryChooserNode child = null;

        if (!node.isLeaf()) {
            Enumeration children = node.children();
            while (children.hasMoreElements()) {
                child = (LibraryChooserNode) children.nextElement();
                if (child.getUserObject().equals(object))
                    return child;
            }
        }
        
        return null;
    }

    /**
     * Handle all popup menu commands.
     * 
     * @param ae the popup menu command to handle.
     */
    /*    public void actionPerformed(ActionEvent ae) {
      String command = ae.getActionCommand();
      Object source = ae.getSource();
      TreePath pathToUse = new TreePath(this.selectedNode.getPath());
      if (command == LibraryPopupMenu.EXPANDCOMMAND) {
      tree.expandPath(pathToUse);
      } else if (command == LibraryPopupMenu.CONTRACTCOMMAND) {
      tree.collapsePath(pathToUse);
      } else if (command == LibraryPopupMenu.USECOMMAND) {
      TreePath selectedPath = new TreePath(selectedNode.getPath());
      if (selectedPath == null)
      return;
      String packageName = pathToPackageName(selectedPath);
      if (packageName != null)
      parent.usePackage(packageName, false);
      } else if (command == LibraryPopupMenu.OPENCOMMAND) {
      openSelectedPackageInClassChooser();
      } 
      else if (command == LibraryPopupMenu.PROPCOMMAND) {
      }
      }
    */
    
    /**
     * Convert a tree path to a dot delimited package name,
     * based on the current contents of the tree.
     * 
     * @return the name of the package in this directory, or null.
     * @param thePackage the path to the package.
     */
    public String pathToPackageName(TreePath thePackage)
    {
        Object[] nodesInPath = thePackage.getPath();
        String packageName = "";

        // let's parse the rest of the LibraryChooserNodes
        for (int current = 2; current < nodesInPath.length; current++) {
            packageName += ((LibraryChooserNode)nodesInPath[current]).toString() + ".";
        }

        if (!packageName.equals("")) {
            // remove the last character, which will be the last decimal point added
            packageName = packageName.substring(0, packageName.length() - 1);
        }
        
        return packageName;
    }
    

}





