package bluej.pkgmgr;

import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;

import bluej.Config;
import bluej.compiler.*;
import bluej.debugger.*;
import bluej.debugmgr.*;
import bluej.editor.*;
import bluej.extensions.event.CompileEvent;
import bluej.extmgr.ExtensionsManager;
import bluej.graph.*;
import bluej.parser.ClassParser;
import bluej.parser.symtab.*;
import bluej.pkgmgr.dependency.*;
import bluej.pkgmgr.target.*;
import bluej.terminal.Terminal;
import bluej.utility.*;
import bluej.utility.filefilter.*;


/**
 * A Java package (collection of Java classes).
 *
 * @author  Michael Kolling
 * @author  Axel Schmolitzky
 * @author  Andrew Patterson
 * @version $Id: Package.java 2330 2003-11-13 04:10:34Z ajp $
 */
public final class Package extends Graph
    implements MouseListener, MouseMotionListener
{
    /** message to be shown on the status bar */
    static final String compiling = Config.getString("pkgmgr.compiling");
    /** message to be shown on the status bar */
    static final String compileDone = Config.getString("pkgmgr.compileDone");
    /** message to be shown on the status bar */
    static final String chooseUsesTo = Config.getString("pkgmgr.chooseUsesTo");
    /** message to be shown on the status bar */
    static final String chooseInhTo = Config.getString("pkgmgr.chooseInhTo");

    /** the name of the package file in a package directory that holds
     *  information about the package and its targets. */
    public static final String pkgfileName = "bluej.pkg";
    /** the name of the backup file of the package file */
    public static final String pkgfileBackup = "bluej.pkh";

    public static final String readmeName = "README.TXT";

    /** error code */ public static final int NO_ERROR = 0;
    /** error code */ public static final int FILE_NOT_FOUND = 1;
    /** error code */ public static final int ILLEGAL_FORMAT = 2;
    /** error code */ public static final int COPY_ERROR = 3;
    /** error code */ public static final int CLASS_EXISTS = 4;
    /** error code */ public static final int CREATE_ERROR = 5;

    /*    private static final int STARTROWPOS = 20;
          private static final int STARTCOLUMNPOS = 20;
          private static final int DEFAULTTARGETHEIGHT = 50;
          private static final int TARGETGAP = 20;
          private static final int RIGHT_LAYOUT_BOUND = 500; */

    /* In the top left corner of each package we have a fixed target -
       either a ParentPackageTarget or a ReadmeTarget. These are there
       locations */
    public static final int FIXED_TARGET_X = 10;
    public static final int FIXED_TARGET_Y = 10;

    /** Interface to editor */
    //bq    public static EditorManager editorManager = new MoeEditorManager();
    public static EditorManager editorManager = EditorManager.getEditorManager();
    // static EditorManager editorManager = new RedEditorManager(false);
    // static EditorManager editorManager = new SimpleEditorManager();

    /* the Project this package is in */
    private Project project;

    /* the parent Package object for this package or null if this is the unnamed package
       ie. the root of the package tree */
    private Package parentPackage = null;

    /* base name of package (eg util) ("" for the unnamed package) */
    private String baseName = "";

    /* this properties object contains the properties loaded off disk for
       this package, or the properties which were most recently saved to
       disk for this package */
    private SortedProperties lastSavedProps = new SortedProperties();

    /* all the targets in a package */
    private TargetCollection targets;

    /** all the uses-arrows in a package */
    private List usesArrows;

    /** all the extends-arrows in a package */
    private List extendsArrows;

    /** the currently selected graphElement */
    private GraphElement selected;

    /** Holds the choice of "from" target for a new dependency */
    private DependentTarget fromChoice;

    /** the currently selected arrow */
    private Dependency currentArrow;

    /** the CallHistory of a package */
    private CallHistory callHistory;

    /** whether extends-arrows should be shown */
    private boolean showExtends = true;
    /** whether uses-arrows should be shown */
    private boolean showUses = true;

    /** needed when debugging with breakpoints to see if the editor window
     *  needs to be brought to the front */
    private String lastSourceName = "";

    /** state constant */ public static final int S_IDLE = 0;
    /** state constant */ public static final int S_CHOOSE_USES_FROM = 1;
    /** state constant */ public static final int S_CHOOSE_USES_TO = 2;
    /** state constant */ public static final int S_CHOOSE_EXT_FROM = 3;
    /** state constant */ public static final int S_CHOOSE_EXT_TO = 4;

    /** determines the maximum length of the CallHistory of a package */
    public static final int HISTORY_LENGTH = 6;

    /** the state a package can be in (one of the S_* values) */
    private int state = S_IDLE;

    protected PackageEditor editor;



    /* ------------------- end of field declarations ------------------- */

    /**
     * Create a package of a project with the package name of
     * baseName (ie reflect) and with a parent package of parent (which may
     * represent java.lang for instance)
     * If the package file (bluej.pkg) is not found, an IOException is thrown.
     */
    public Package(Project project, String baseName, Package parent)
        throws IOException
    {
        if (parent == null)
            throw new NullPointerException("Package must have a valid parent package");

        if (baseName.length() == 0)
            throw new IllegalArgumentException("unnamedPackage must be created using Package(project)");

        if (!JavaNames.isIdentifier(baseName))
            throw new IllegalArgumentException(baseName + " is not a valid name for a Package");

        this.project = project;
        this.baseName = baseName;
        this.parentPackage = parent;

        init();
    }

    /**
     * Create the unnamed package of a project
     * If the package file (bluej.pkg) is not found, an IOException is thrown.
     */
    public Package(Project project)
        throws IOException
    {
        this.project = project;
        this.baseName = "";
        this.parentPackage = null;

        init();
    }

    private void init()
        throws IOException
    {
        targets = new TargetCollection();
        usesArrows = new ArrayList();
        extendsArrows = new ArrayList();
        selected = null;
        callHistory = new CallHistory(HISTORY_LENGTH);
        load();
    }

    public boolean isUnnamedPackage()
    {
        return parentPackage == null;
    }

    /**
     * Return the project this package belongs to.
     */
    public Project getProject()
    {
        return project;
    }

    /**
     * Get the unique identifier for this package (it's directory name
     * at present)
     */
    public String getId()
    {
        return getPath().getPath();
    }

    /**
     * Return this package's base name (eg util)
     * ("" for the unnamed package)
     */
    public String getBaseName()
    {
        return baseName;
    }

    /**
     * Return the qualified name of an identifier in this
     * package (eg java.util.Random if given Random)
     */
    public String getQualifiedName(String identifier)
    {
        if(isUnnamedPackage())
            return identifier;
        else
            return getQualifiedName() + "." + identifier;
    }

    /**
     * Return the qualified name of the package (eg. java.util)
     * ("" for the unnamed package)
     */
    public String getQualifiedName()
    {
        Package currentPkg = this;
        String retName = "";

        while(!currentPkg.isUnnamedPackage()) {
            if(retName == "")
                retName = currentPkg.getBaseName();
            else
                retName = currentPkg.getBaseName() + "." + retName;

            currentPkg = currentPkg.getParent();
        }

        return retName;
    }

    /**
     * get the readme target for this package 
     *
     */ 
    public ReadmeTarget getReadmeTarget()
    {
        ReadmeTarget readme = (ReadmeTarget)targets.get(ReadmeTarget.README_ID);  
        return readme;
    }

    private File getRelativePath()
    {
        Package currentPkg = this;
        File retFile = new File(currentPkg.getBaseName());

        /* loop through our parent packages constructing a relative
           path for this file */
        while(!currentPkg.isUnnamedPackage()) {
            currentPkg = currentPkg.getParent();

            retFile = new File(currentPkg.getBaseName(), retFile.getPath());
        }

        return retFile;
    }

    /**
     * Return a file object of the directory location of this package.
     *
     * @return  The file object representing the full path to the
     *          packages directory
     */
    public File getPath()
    {
        /* append our relative path onto the absolute path which our project
           gives us */
        return new File(project.getProjectDir(), getRelativePath().getPath());
    }

    /**
     * Return our parent package or null if we are the unnamed package.
     */
    public Package getParent()
    {
        return parentPackage;
    }

    /**
     * Returns the sub-package if this package is "boring".
     * Our definition of boring is that the package has no classes in
     * it and only one sub package. If this package is not
     * boring, this method returns null.
     */
    protected Package getBoringSubPackage()
    {
        PackageTarget pt = null;

        for(Iterator e = targets.iterator(); e.hasNext(); ) {
            Target target = (Target)e.next();

            if(target instanceof ClassTarget)
                return null;

            if((target instanceof PackageTarget) &&
               !(target instanceof ParentPackageTarget)) {
                // we have found our second sub package
                // this means this package is not boring
                if(pt != null)
                    return null;

                pt = (PackageTarget) target;
            }
        }

        if (pt == null) return null;

        // it is a getPackage 311003 Damiano
        return getProject().getPackage(pt.getQualifiedName());
    }

    /**
     * Return an array of package objects which are nested one level
     * below us. Will return null if there are no
     * children.
     */
    protected List getChildren()
    {
        List children = new ArrayList();

        for(Iterator e = targets.iterator(); e.hasNext(); ) {
            Target target = (Target)e.next();

            if(target instanceof PackageTarget &&
               !(target instanceof ParentPackageTarget)) {
                PackageTarget pt = (PackageTarget)target;

                // It is a getPackage() 311003 Damiano
                Package child = getProject().getPackage(pt.getQualifiedName());

                if (child == null)
                    continue;

                children.add(child);
            }
        }

        if (children.size() == 0)
            children = null;

        return children;
    }

    public void setStatus(String msg)
    {
        PkgMgrFrame.displayMessage(this, msg);
    }

    public void repaint()
    {
        if(editor != null) {
            editor.revalidate();
            editor.repaint();
        }
    }

    public PackageEditor getEditor()
    {
        return (PackageEditor)editor;
    }

    public Properties getLastSavedProperties()
    {
        return lastSavedProps;
    }

    
    /**
     * Get the currently selected Targets.
     * It will return an empty array if no target is selected.
     * @return the currently selected array of Targets.
     */    
    public Target[] getSelectedTargets(){
        Target[] targetArray = new Target[0];
        LinkedList list = new LinkedList();
        for(Iterator it = getVertices(); it.hasNext(); ) {
            Vertex vertex = (Vertex)it.next();
            if (vertex instanceof Target && ((Target)vertex).isSelected()) {
                list.add(vertex);
            }
        }
        return (Target[]) list.toArray(targetArray);
    }

    /**
     * Search a directory for Java source and class files and add their
     * names to a set which is returned.
     * Will delete any __SHELL files which are found in the directory
     * and will ignore any single .class files which do not contain
     * public classes.
     *
     * The returned set is guaranteed to be only valid Java identifiers.
     */
    private Set findTargets(File path)
    {
        File srcFiles[] = path.listFiles(new JavaSourceFilter());
        File classFiles[] = path.listFiles(new JavaClassFilter());

        Set interestingSet = new HashSet();

        // process all *.java files
        for(int i=0; i<srcFiles.length; i++) {
            // remove all __SHELL*.java files (temp files created by us)
            if (srcFiles[i].getName().startsWith(Invoker.SHELLNAME)) {
                srcFiles[i].delete();
                continue;
            }
            String javaFileName =
                JavaNames.stripSuffix(srcFiles[i].getName(), ".java");

            // check if the name would be a valid java name
            if (!JavaNames.isIdentifier(javaFileName))
                continue;

            // files with a $ in them signify inner classes (which we want to ignore)
            if (javaFileName.indexOf('$') == -1)
                interestingSet.add(javaFileName);
        }

        // process all *.class files
        for(int i=0; i<classFiles.length; i++) {
            // remove all __SHELL*.class files (temp files created by us)
            if (classFiles[i].getName().startsWith(Invoker.SHELLNAME)) {
                classFiles[i].delete();
                continue;
            }
            String classFileName =
                JavaNames.stripSuffix(classFiles[i].getName(), ".class");

            // check if the name would be a valid java name
            if (!JavaNames.isIdentifier(classFileName))
                continue;

            if (classFileName.indexOf('$') == -1) {
                // add only if there is no corresponding .java file
                if (!interestingSet.contains(classFileName)) {
                    try {
                        Class c = loadClass(getQualifiedName(classFileName));
                
                        // fix for bug 152
                        // check that this class is a public class which means that
                        // private and package .class files generated because there are
                        // multiple classes defined in a single file will not add a target
                        if (Modifier.isPublic(c.getModifiers()))
                            interestingSet.add(classFileName);
                    }
                    catch (LinkageError e) {
                        Debug.message(e.toString());
                    }
                }
            }
        }

        return interestingSet;
    }

    /**
     * Load the elements of a package from a specified directory.
     * If the package file (bluej.pkg) is not found, an IOException is thrown.
     */
    private void load()
        throws IOException
    {
        // read the package properties
        File pkgFile = new File(getPath(), pkgfileName);

        // try to load the package file for this package
        FileInputStream input = new FileInputStream(pkgFile);
        lastSavedProps.load(input);
        input.close();

        // read in all the targets contained in this package
        // into this temporary map
        Map propTargets = new HashMap();

		int numTargets = 0, numDependencies = 0;
		
		try {
			numTargets = Integer.parseInt(lastSavedProps.getProperty("package.numTargets", "0"));
			numDependencies = Integer.parseInt(lastSavedProps.getProperty("package.numDependencies", "0"));
		}
		catch(Exception e) {
			Debug.reportError("Error loading from bluej.pkg file " +
						  pkgFile + ": " + e);
			e.printStackTrace();
			return;
		}

        for(int i = 0; i < numTargets; i++) {
            Target target = null;
            String type = lastSavedProps.getProperty("target" + (i + 1) + ".type");
            String identifierName = lastSavedProps.getProperty("target" + (i + 1) + ".name");

            if("PackageTarget".equals(type))
                target = new PackageTarget(this, identifierName);
            else {
                target = new ClassTarget(this, identifierName);
            }

            if(target != null) {
                //Debug.message("Load target " + target);
                target.load(lastSavedProps, "target" + (i + 1));
                //Debug.message("Putting " + identifierName);
                propTargets.put(identifierName, target);
            }
        }

        // add our immovable targets (either a text note or a package
        // which goes to the parent package)
        if (!isUnnamedPackage()) {
            Target t = new ParentPackageTarget(this);
            t.setPos(FIXED_TARGET_X,FIXED_TARGET_Y);
            addTarget(t);
        }
        else {
            Target t = new ReadmeTarget(this);
            t.setPos(FIXED_TARGET_X,FIXED_TARGET_Y);
            addTarget(t);
        }
        addImmovableTargets();
        // make our Package targets reflect what is actually on disk
        // note that we consider this on-disk version the master
        // version so if we have a class target called Foo but we
        // discover a directory call Foo, a PackageTarget will be
        // inserted to replace the ClassTarget
        File subDirs[] = getPath().listFiles(new SubPackageFilter());

        for(int i=0; i<subDirs.length; i++) {
            // first check if the directory name would be a valid package name
            if (!JavaNames.isIdentifier(subDirs[i].getName()))
                continue;

            Target target = (Target) propTargets.get(subDirs[i].getName());

            if(target == null || !(target instanceof PackageTarget)) {
                target = new PackageTarget(this, subDirs[i].getName());
                findSpaceForVertex(target);
            }

            addTarget(target);
        }

		// now look for Java sorce files that may have been
		// added to the directory
        Set interestingSet = findTargets(getPath());

		// also we migrate targets from propTargets across
		// to our real list of targets in this loop.
        Iterator it = interestingSet.iterator();

        while(it.hasNext()) {
            String targetName = (String) it.next();

            Target target = (Target) propTargets.get(targetName);
            if(target == null || !(target instanceof ClassTarget)) {
                target = new ClassTarget(this, targetName);
                findSpaceForVertex(target);
            }

            try {
                ((ClassTarget)target).enforcePackage(getQualifiedName());
            }
            catch(IOException ioe) {
                Debug.message(ioe.getLocalizedMessage());
            }
            catch(ClassCastException cce) { }

            addTarget(target);
        }

        for(int i = 0; i < numDependencies; i++) {
            Dependency dep = null;
            String type = lastSavedProps.getProperty("dependency" + (i+1) + ".type");

            if("UsesDependency".equals(type))
                dep = new UsesDependency(this);
            //		else if("ExtendsDependency".equals(type))
            //		    dep = new ExtendsDependency(this);
            //		else if("ImplementsDependency".equals(type))
            //		    dep = new ImplementsDependency(this);

            if(dep != null) {
                dep.load(lastSavedProps, "dependency" + (i + 1));
                addDependency(dep, false);
            }
        }
        recalcArrows();

		// our associations are based on name so we mustn't deal with
		// them till all classes/packages have been loaded
		for(int i = 0; i < numTargets; i++) {
			String assoc = lastSavedProps.getProperty("target" + (i + 1) + ".association");
			String identifierName = lastSavedProps.getProperty("target" + (i + 1) + ".name");
			
			if (assoc != null) {
				Target t1 = getTarget(identifierName), t2 = getTarget(assoc);
				
				if(t1 != null && t2 != null && t1 instanceof DependentTarget) {
					DependentTarget dt = (DependentTarget) t1;
					dt.setAssociation(t2);
				}
			}
		}

        for(it = targets.iterator(); it.hasNext(); ) {
            Target target = (Target)it.next();

            if(target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget)target;
                ct.analyseSource(false);
            }
        }


        for(it = targets.iterator(); it.hasNext(); ) {
            Target t = (Target)it.next();
            if((t instanceof ClassTarget)
               && ((ClassTarget)t).upToDate()) {
                ClassTarget ct = (ClassTarget)t;
                //                if (readyToPaint)
                ct.setState(Target.S_NORMAL);
                // XXX: Need to invalidate things dependent on t
            }
        }
    }
    
    private void addImmovableTargets() {
        // add our immovable targets (either a text note or a package
        // which goes to the parent package)
//        if (isUnnamedPackage()) {
//            Target t = new ReadmeTarget(this);
//            t.setPos(FIXED_TARGET_X,FIXED_TARGET_Y);
//            addTarget(t);
//        }
//        else {
//            Target t = new ParentPackageTarget(this);
//            t.setPos(FIXED_TARGET_X,FIXED_TARGET_Y);
//            addTarget(t);
//        }
        Target t = new ReadmeTarget(this);
        t.setPos(FIXED_TARGET_X,FIXED_TARGET_Y);
        addTarget(t);
        if (!isUnnamedPackage()) {
            t = new ParentPackageTarget(this);
            findSpaceForVertex(t);
            addTarget(t);
        }

        
    }


    /**
     * Reload a package.
     *
     * This means we check the existing directory contents and compare
     * it against the targets we have in the package. Any new
     * directories or java source is added to the package.
     * This function will not remove targets that have had their
     * corresponding on disk counterparts removed.
     *
     * Any new source files will have their package lines updated to
     * match the package we are in.
     */
    public void reload()
    {
        File subDirs[] = getPath().listFiles(new SubPackageFilter());

        for(int i=0; i<subDirs.length; i++) {
            // first check if the directory name would be a valid package name
            if (!JavaNames.isIdentifier(subDirs[i].getName()))
                continue;

            Target target = (Target) targets.get(subDirs[i].getName());

            if(target == null) {
                Target newtarget = addPackage(subDirs[i].getName());
                findSpaceForVertex(newtarget);
            }
        }

        Set interestingSet = findTargets(getPath());

        for(Iterator it = interestingSet.iterator(); it.hasNext(); ) {
            String targetName = (String) it.next();

            Target target = (Target) targets.get(targetName);

            if(target == null) {
                Target newtarget = addClass(targetName);
                findSpaceForVertex(newtarget);
            }
        }

        for(Iterator it = targets.iterator(); it.hasNext(); ) {
            Target target = (Target)it.next();

            if(target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget)target;
                ct.analyseSource(false);
            }
        }

        repaint();
    }

    /**
     * Save this package to disk. The package is saved to the standard
     * package file (bluej.pkg).
     */
    public boolean save(Properties frameProperties)
    {
        /* create the directory if it doesn't exist */
        File dir = getPath();
        if(!dir.exists()) {
            if(!dir.mkdir()) {
                Debug.reportError("Error creating directory " + dir);
                return false;
            }
        }

        File file = new File(dir, pkgfileName);
        if(!file.canWrite())
            return false;
        if(file.exists()) {			// make backup of original
            file.renameTo(new File(getPath(), pkgfileBackup));
        }

        SortedProperties props = new SortedProperties();

        if (frameProperties != null)
            props.putAll(frameProperties);

        // save targets and dependencies in package

        props.put("package.numDependencies",
                  String.valueOf(usesArrows.size()));

        int t_count = 0;

        Iterator t_enum = targets.iterator();
        for(int i = 0; t_enum.hasNext(); i++) {
            Target t = (Target)t_enum.next();
            // should we save this target
            if(t.isSaveable()) {
                t.save(props, "target" + (t_count + 1));
                t_count++;
            }
        }
        props.put("package.numTargets", String.valueOf(t_count));

        for(int i = 0; i < usesArrows.size(); i++) {        // uses arrows
            Dependency d = (Dependency)usesArrows.get(i);
            d.save(props, "dependency" + (i + 1));
        }

        try {
            FileOutputStream output = new FileOutputStream(file);
            props.store(output, "BlueJ package file");
            output.close();
        } catch(IOException e) {
            Debug.reportError("Error saving project file " + file + ": " + e);
            return false;
        }

        lastSavedProps = props;

        return true;
    }

    /**
     * Import a source file into this package as a new
     * class target. Returns an error code:
     *   NO_ERROR       - everything is fine
     *   FILE_NOT_FOUND - file does not exist
     *   ILLEGAL_FORMAT - the file name does not end in ".java"
     *   CLASS_EXISTS   - a class with this name already exists
     *   COPY_ERROR     - could not copy
     */
    public int importFile(File sourceFile)
    {
        // check whether specified class exists and is a java file

        if(! sourceFile.exists())
            return FILE_NOT_FOUND;
        String fileName = sourceFile.getName();

        String className;
        if(fileName.endsWith(".java"))		// it's a Java source file
            className = fileName.substring(0, fileName.length() - 5);
        else
            return ILLEGAL_FORMAT;

        // check whether name is already used
        if(getTarget(className) != null)
            return CLASS_EXISTS;

        // copy class source into package

        File destFile = new File(getPath(),fileName);
        if(!FileUtility.copyFile(sourceFile, destFile))
            return COPY_ERROR;

        ClassTarget t = addClass(className);

        findSpaceForVertex(t);
        t.analyseSource(false);

        return NO_ERROR;
    }

    public ClassTarget addClass(String className)
    {
        // create class icon (ClassTarget) for new class
        ClassTarget target = new ClassTarget(this, className);
        addTarget(target);

        // make package line in class source match our package
        try {
            target.enforcePackage(getQualifiedName());
        }
        catch(IOException ioe) {
            Debug.message(ioe.getLocalizedMessage());
        }

        return target;
    }

    public PackageTarget addPackage(String packageName)
    {
        PackageTarget target = new PackageTarget(this, packageName);
        addTarget(target);

        return target;
    }

	public Debugger getDebugger()
	{
		return getProject().getDebugger();
	}
	
    /**
     * Loads a class using the current project class loader.
     */
    public Class loadClass(String className)
    {
        return getProject().loadClass(className);
    }


    public Iterator getVertices()
    {
        return targets.sortediterator();
    }

    public Iterator getEdges()
    {
        List iterations = new ArrayList();

        if(showUses)
            iterations.add(usesArrows.iterator());
        if(showExtends)
            iterations.add(extendsArrows.iterator());

        return new MultiIterator(iterations);
    }

	/**
	 * Return a List of all ClassTargets that have the role of
	 * a unit test.
	 */
	public List getTestTargets()
	{
		List l = new ArrayList();
		
		for(Iterator it = targets.iterator(); it.hasNext(); ) {
			Target target = (Target)it.next();

			if(target instanceof ClassTarget) {
				ClassTarget ct = (ClassTarget)target;

				if (ct.isUnitTest())
					l.add(ct);
			}
		}
		
		return l;
	}

    /**
     *  The standard compile user function: Find and compile all uncompiled
     *  classes.
     */
    public void compile()
    {
        if(!checkCompile())
            return;

        List toCompile = new ArrayList();

        for(Iterator it = targets.iterator(); it.hasNext(); ) {
            Target target = (Target)it.next();

            if(target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget)target;
                if (ct.editorOpen())
                    ct.getEditor().save();
                if(ct.isInvalidState())
                    toCompile.add(ct);
            }
        }

        for(int i = toCompile.size() - 1; i >= 0; i--)
            searchCompile((ClassTarget)toCompile.get(i), 1,
                          new Stack(), new PackageCompileObserver());
    }

    /**
     *  Compile a single class.
     */
    public void compile(ClassTarget ct)
    {
        if(!checkCompile())
            return;

        if (ct.editorOpen())
            ct.getEditor().save();
        ct.setInvalidState();		// to force compile

        searchCompile(ct, 1, new Stack(), new PackageCompileObserver());

		if (ct.getAssociation() != null) {
			ClassTarget assocTarget = (ClassTarget) ct.getAssociation();

			assocTarget.setInvalidState();		// to force compile
			searchCompile(assocTarget, 1, new Stack(), new QuietPackageCompileObserver());
		}
    }


    /**
     *  Compile a single class quietly.
     */
    public void compileQuiet(ClassTarget ct)
    {
        if(!checkCompile())
            return;
        
        if (ct.editorOpen())
            ct.getEditor().save();
        ct.setInvalidState();		// to force compile
        
        searchCompile(ct, 1, new Stack(), new QuietPackageCompileObserver());
    }

    /**
     * Force compile of all classes. Called by user function "rebuild".
     */
    public void rebuild()
    {
        if(!checkCompile())
            return;

        List compileTargets = new ArrayList();

        for(Iterator it = targets.iterator(); it.hasNext(); ) {
            Target target = (Target)it.next();

            if(target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget)target;
                if (ct.editorOpen())
                    ct.getEditor().save();
                ct.setState(Target.S_INVALID);
                ct.analyseSource(false);
                compileTargets.add(ct);
            }
        }
        doCompile(compileTargets, new PackageCompileObserver());
    }

    /**
     * Use Tarjan's algorithm to construct compiler Jobs. 
     */
    private void searchCompile(ClassTarget t, int dfcount, Stack stack, CompileObserver observer)
    {
        if((t.getState() != Target.S_INVALID) || t.isQueued())
            return;

        t.setQueued(true);
        t.dfn = dfcount;
        t.link = dfcount;

        stack.push(t);

        Iterator dependencies = t.dependencies();

        while(dependencies.hasNext()) {
            Dependency d = (Dependency)dependencies.next();
            if(!(d.getTo() instanceof ClassTarget))
                continue;

            ClassTarget to = (ClassTarget)d.getTo();

            if(to.isQueued()) {
                if((to.dfn < t.dfn) && (stack.search(to) != -1))
                    t.link = Math.min(t.link, to.dfn);
            }
            else if(to.getState() == Target.S_INVALID) {
                searchCompile((ClassTarget)to, dfcount + 1, stack, observer);
                t.link = Math.min(t.link, to.link);
            }
        }

        if(t.link == t.dfn) {
            List compileTargets = new ArrayList();
            ClassTarget x;

            do {
                x = (ClassTarget)stack.pop();
                compileTargets.add(x);
            } while(x != t);

            doCompile(compileTargets, observer);
        }
    }

    /**
     *  Compile every Target in 'targetList'. Every compilation goes through
     *  this method.
     */
    private void doCompile(List targetList, CompileObserver observer)
    {
        if(targetList.size() == 0)
            return;

        File[] srcFiles = new File[targetList.size()];
        for(int i = 0; i < targetList.size(); i++) {
            ClassTarget ct = (ClassTarget)targetList.get(i);
            srcFiles[i] = ct.getSourceFile();
        }
        removeBreakpoints();
        //Terminal.getTerminal().clear();
        
        JobQueue.getJobQueue().addJob(srcFiles, observer,
                                        getProject().getClassPath(),
                                      	getProject().getProjectDir());
    }


    /**
     * Returns true if the debugger is not busy. This is true if it is either
     * IDLE, or has not been completely constructed (NOTREADY).
     */
    public boolean isDebuggerIdle ()
    {
        int status = getDebugger().getStatus();
        return (status == Debugger.IDLE) || (status == Debugger.NOTREADY);
    }


    /**
     * Check whether it's okay to compile and display a message about it.
     */
    private boolean checkCompile()
    {
        if ( isDebuggerIdle() ) return true;
        
        // The debugger is NOT idle, show a message about it.
        showMessage("compile-while-executing");
        return false;
    }

    /**
     * Generate documentation for this package.
     * @return "" if everything was alright, an error message otherwise.
     */
    public String generateDocumentation()
    {
        // This implementation currently just delegates the generation to
        // the project this package is part of.
        return project.generateDocumentation();
    }

	/**
	 * Generate documentation for class in 'filename'
	 * @param filename
	 */
	public void generateDocumentation(ClassTarget ct)
	{
		ct.getEditor().save();
		String filename = ct.getSourceFile().getPath();
		project.generateDocumentation(filename);
	}

    /**
     *  Remove all breakpoints in all classes.
     */
    public void removeBreakpoints()
    {
        for(Iterator it = targets.iterator(); it.hasNext(); ) {
            Target target = (Target)it.next();

            if(target instanceof ClassTarget)
                ((ClassTarget)target).removeBreakpoints();
        }
    }

    /**
     *  Remove all step marks in all classes.
     */
    public void removeStepMarks()
    {
        for(Iterator it = targets.iterator(); it.hasNext(); ) {
            Target target = (Target)it.next();

            if(target instanceof ClassTarget)
                ((ClassTarget)target).removeStepMark();
        }
    }

    public void addTarget(Target t)
    {
        if(t.getPackage() != this)
            throw new IllegalArgumentException();

        targets.add(t.getIdentifierName(), t);
    }

    public void removeTarget(Target t)
    {
        targets.remove(t.getIdentifierName());
    }

    /**
     * Changes the Target identifier. Targets are stored in a hashtable
     * with their name as the key.  If class name changes we need to
     * remove the target and add again with the new key.
     */
    public void updateTargetIdentifier(Target t, String oldIdentifier, String newIdentifier)
    {
        if(t == null || newIdentifier == null) {
            Debug.reportError("cannot properly update target name...");
            return;
        }
        targets.remove(oldIdentifier);
        targets.add(newIdentifier, t);
    }

    /**
     *  Removes a class from the Package
     *
     *  @param removableTarget   the ClassTarget representing the class to
     *				 be removed.
     */
    public void removeClass(ClassTarget removableTarget)
    {
        removeTarget(removableTarget);
        getEditor().repaint();
    }

    /**
     *  Removes a class from the Package
     *
     *  @param removableTarget   the ClassTarget representing the class to
     *	be removed.
     */
    public void removePackage(PackageTarget removableTarget)
    {
        removeTarget(removableTarget);
        getEditor().repaint();
    }

    /**
     *  Add a dependancy in this package. The dependency is also added to the
     *  individual targets involved.
     */
    public void addDependency(Dependency d, boolean recalc)
    {
        DependentTarget from = (DependentTarget)d.getFrom();
        DependentTarget to = (DependentTarget)d.getTo();

        if(from == null || to == null) {
            // Debug.reportError("Found invalid dependency - ignored.");
            return;
        }

        if(d instanceof UsesDependency) {
            int index = usesArrows.indexOf(d);
            if(index != -1) {
                ((UsesDependency)usesArrows.get(index)).setFlag(true);
                return;
            }
            else
                usesArrows.add(d);
        }
        else {
            if(extendsArrows.contains(d))
                return;
            else
                extendsArrows.add(d);
        }

        from.addDependencyOut(d, recalc);
        to.addDependencyIn(d, recalc);

    }

    /**
     * A user initiated addition of an "implements" clause from a class to
     * an interface
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddImplementsClassDependency(Dependency d)
    {
        ClassTarget from = (ClassTarget)d.getFrom();    // a class
        ClassTarget to = (ClassTarget)d.getTo();        // an interface
        Editor ed = from.getEditor();
        ed.save();

        // Debug.message("Implements class dependency from " + from.getName() + " to " + to.getName());

        try {
            ClassInfo info = ClassParser.parse(from.getSourceFile(), new Vector(getAllClassnames()));

            Selection s1 = info.getImplementsInsertSelection();
            ed.setSelection(s1.getLine(), s1.getColumn(), s1.getLength());

            if (info.hasInterfaceSelections()) {
                // if we already have an implements clause then we need to put a
                // comma and the interface name but not before checking that we don't
                // already have it

                List exists = info.getInterfaceTexts();

                // XXX make this equality check against full package name
                if(!exists.contains(to.getBaseName()))
                    ed.insertText(", " + to.getBaseName(), false);
            } else {
                // otherwise we need to put the actual "implements" word
                // and the interface name
                ed.insertText(" implements " + to.getBaseName(), false);
            }
            ed.save();
        }
        catch(Exception e) {
            // exception during parsing so we have to ignore
            // perhaps we should display a message here
            return;
        }
    }

    /**
     * A user initiated addition of an "extends" clause from an interface to
     * an interface
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddImplementsInterfaceDependency(Dependency d)
    {
        ClassTarget from = (ClassTarget)d.getFrom();    // an interface
        ClassTarget to = (ClassTarget)d.getTo();        // an interface
        Editor ed = from.getEditor();
        ed.save();

        // Debug.message("Implements interface dependency from " + from.getName() + " to " + to.getName());

        try {
            ClassInfo info = ClassParser.parse(from.getSourceFile(), new Vector(getAllClassnames()));

            Selection s1 = info.getExtendsInsertSelection();
            ed.setSelection(s1.getLine(), s1.getColumn(), s1.getLength());

            if (info.hasInterfaceSelections()) {
                // if we already have an extends clause then we need to put a
                // comma and the interface name but not before checking that we don't
                // already have it

                List exists = info.getInterfaceTexts();

                // XXX make this equality check against full package name
                if(!exists.contains(to.getBaseName()))
                    ed.insertText(", " + to.getBaseName(), false);
            } else {
                // otherwise we need to put the actual "extends" word
                // and the interface name
                ed.insertText(" extends " + to.getBaseName(), false);
            }
            ed.save();
        }
        catch(Exception e) {
            // exception during parsing so we have to ignore
            // perhaps we should display a message here
            return;
        }
    }

    /**
     * A user initiated addition of an "extends" clause from a class to
     * a class
     *
     * @pre d.getFrom() and d.getTo() are both instances of ClassTarget
     */
    public void userAddExtendsClassDependency(Dependency d)
    {
        ClassTarget from = (ClassTarget)d.getFrom();
        ClassTarget to = (ClassTarget)d.getTo();
        Editor ed = from.getEditor();
        ed.save();

        try {
            ClassInfo info = ClassParser.parse(from.getSourceFile(), new Vector(getAllClassnames()));

            if (info.getSuperclass() == null) {
                Selection s1 = info.getExtendsInsertSelection();

                ed.setSelection(s1.getLine(), s1.getColumn(), s1.getLength());
                ed.insertText(" extends " + to.getBaseName(), false);
            } else {
                Selection s1 = info.getSuperReplaceSelection();

                ed.setSelection(s1.getLine(), s1.getColumn(), s1.getLength());
                ed.insertText(to.getBaseName(), false);
            }
            ed.save();
        }
        catch(Exception e) {
            // exception during parsing so we have to ignore
            // perhaps we should display a message here
            return;
        }
    }

    /**
     * A user initiated removal of a dependency
     *
     * @pre d is an instance of an Implements or Extends dependency
     */
    public void userRemoveDependency(Dependency d)
    {
        // if they are not both classtargets then I don't want to know about it
        if (!(d.getFrom() instanceof ClassTarget) ||
            !(d.getTo() instanceof ClassTarget))
            return;

        ClassTarget from = (ClassTarget)d.getFrom();
        ClassTarget to = (ClassTarget)d.getTo();
        Editor ed = from.getEditor();
        ed.save();

        try {
            ClassInfo info = ClassParser.parse(from.getSourceFile(), new Vector(getAllClassnames()));
            Selection s1 = null;
            Selection s2 = null;               // set to the selections we wish to delete
            Selection sinsert = null;          // our selection if we want to insert something
            String sinserttext = "";

            if(d instanceof ImplementsDependency) {
                List vsels, vtexts;

                if(info.isInterface()) {
                    vsels = info.getInterfaceSelections();
                    vtexts = info.getInterfaceTexts();
                    sinserttext = "extends ";
                } 
                else {
                    vsels = info.getInterfaceSelections();
                    vtexts = info.getInterfaceTexts();
                    sinserttext = "implements ";
                }

                int where = vtexts.indexOf(to.getBaseName());

                if (where > 0) {             // should always be true
                    s1 = (Selection)vsels.get(where-1);
                    s2 = (Selection)vsels.get(where);
                }
                // we have a special case if we deleted the first bit of an "implements"
                // clause, yet there are still clauses left.. we have to replace the ","
                // with "implements" (note that there must already be a leading space so we
                // do not need to insert one but we may need a trailing space)
                if(where == 1 && vsels.size() > 2) {
                    sinsert = (Selection)vsels.get(where+1);
                }
            }
            else if(d instanceof ExtendsDependency) {
                // a class extends
                s1 = info.getExtendsReplaceSelection();
                s2 = info.getSuperReplaceSelection();
            }

            // delete (maybe insert) text from the end backwards so that our line/col positions
            // for s1 are not mucked up by the deletion
            if(sinsert != null) {
                ed.setSelection(sinsert.getLine(), sinsert.getColumn(), sinsert.getLength());
                ed.insertText(sinserttext, false);
            }
            if(s2 != null) {
                ed.setSelection(s2.getLine(), s2.getColumn(), s2.getLength());
                ed.insertText("", false);
            }
            if(s1 != null) {
                ed.setSelection(s1.getLine(), s1.getColumn(), s1.getLength());
                ed.insertText("", false);
            }

            ed.save();
        }
        catch(Exception e) {
            // exception during parsing so we have to ignore
            // perhaps we should display a message here
            e.printStackTrace();
            Debug.message("Parse error attempting to delete dependency arrow");
            return;
        }
    }


    /**
     *  Remove a dependancy from this package. The dependency is also removed
     *  from the individual targets involved.
     */
    public void removeDependency(Dependency d, boolean recalc)
    {
        if(d instanceof UsesDependency)
            usesArrows.remove(d);
        else
            extendsArrows.remove(d);

        DependentTarget from = (DependentTarget)d.getFrom();
        from.removeDependencyOut(d, recalc);

        DependentTarget to = (DependentTarget)d.getTo();
        to.removeDependencyIn(d, recalc);
    }

    public void recalcArrows()
    {
        Iterator it = getVertices();
        while(it.hasNext()) {
            Target t = (Target)it.next();

            if (t instanceof DependentTarget) {
                DependentTarget dt = (DependentTarget)t;

                dt.recalcInUses();
                dt.recalcOutUses();
            }
        }
    }

    /**
     * Sets which GraphElement is currently active. If the graphElement is 
     * selectable (implements Selectable) the element gets a message that it
     * is selected. The element (if any) that was selected before, get a 
     * messeage that it is no longer selected.
     * @param ge the GraphElement that now active.
     */
    public void setActiveGraphElement(GraphElement ge)
    {
        if(selected != null && selected instanceof Selectable){
            ((Selectable)selected).setSelected(false);
        }
        selected = ge;
        if(selected != null && selected instanceof Selectable){
            ((Selectable)selected).setSelected(true);
        }
            
    }

    /**
     * Return the target with name "identifierName".
     *
     * @param   identifierName the unique name of a target.
     * @return  the target with name "tname" if existent, null otherwise.
     */
    public Target getTarget(String identifierName)
    {
        if(identifierName == null)
            return null;
        Target t = (Target)targets.get(identifierName);
        return t;
    }

    /**
     * Return the dependent target with name "identifierName".
     *
     * @param   identifierName the unique name of a target.
     * @return  the target with name "tname" if existent and if it
     *          is a DependentTarget, null otherwise.
     */
    public DependentTarget getDependentTarget(String identifierName)
    {
        if(identifierName == null)
            return null;
        Target t = (Target)targets.get(identifierName);

        if (t instanceof DependentTarget)
            return (DependentTarget) t;

        return null;
    }

    /**
     * Return a List of Strings with names of all classes
     * in this package.
     */
    public List getAllClassnames()
    {
        List names = new ArrayList();

        for(Iterator it = targets.iterator(); it.hasNext(); ) {
            Target t = (Target)it.next();

            if(t instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget)t;
                names.add(ct.getBaseName());
            }
        }
        return names;
    }

    /**
     * Given a file name, find the target that represents that file.
     *
     * @return The target with the given file name or <null> if not found.
     */
    public ClassTarget getTargetFromFilename(String filename)
    {
        getProject().convertPathToPackageName(filename);

        for(Iterator it = targets.iterator(); it.hasNext(); ) {
            Target t = (Target)it.next();
            if(!(t instanceof ClassTarget))
                continue;

            ClassTarget ct = (ClassTarget)t;

            if(filename.equals(ct.getSourceFile().getPath()))
                return ct;
        }

        return null;
    }

    public EditableTarget getTargetFromEditor(Editor editor)
    {
        for(Iterator it = targets.iterator(); it.hasNext(); ) {
            Target t = (Target)it.next();
            if(!(t instanceof EditableTarget))
                continue;

            EditableTarget et = (EditableTarget)t;

            if(et.usesEditor(editor))
                return et;
        }

        return null;
    }

    public void setShowUses(boolean state)
    {
        showUses = state;
    }

    public void setShowExtends(boolean state)
    {
        showExtends = state;
    }

    public void setState(int state)
    {
        this.state = state;
    }

    public int getState()
    {
        return state;
    }

    /**
     *  Test whether a file instance denotes a BlueJ package directory.
     *  @param f the file instance that is tested for denoting a BlueJ package.
     *  @return true if f denotes a directory and a BlueJ package.
     */
    public static boolean isBlueJPackage(File f)
    {
        if (f == null)
            return false;

        // don't try to test Windows root directories (you'll get in
        // trouble with disks that are not in drives...).

        if(f.getPath().endsWith(":\\"))
            return false;

        if(!f.isDirectory())
            return false;

        File packageFile = new File(f, pkgfileName);
        return (packageFile.exists());
    }

    /**
     * Called when in an interesting state (e.g. adding a new dependency)
     * and a target is selected.
     */
    public void targetSelected(Target t)
    {
        switch(getState()) {
        case S_CHOOSE_USES_FROM:
            if (t instanceof DependentTarget) {
                fromChoice = (DependentTarget)t;
                setState(S_CHOOSE_USES_TO);
                setStatus(chooseUsesTo);
            } else {
                setState(S_IDLE);
                setStatus(" ");
            }
            break;

        case S_CHOOSE_USES_TO:
            if (t != fromChoice && t instanceof DependentTarget) {
                setState(S_IDLE);
                addDependency(new UsesDependency(this, fromChoice,(DependentTarget)t), true);
                setStatus(" ");
            }
            break;

        case S_CHOOSE_EXT_FROM:

            if (t instanceof DependentTarget) {
                fromChoice = (DependentTarget)t;
                setState(S_CHOOSE_EXT_TO);
                setStatus(chooseInhTo);
            } else {
                setState(S_IDLE);
                setStatus(" ");
            }
            break;

        case S_CHOOSE_EXT_TO:
            if (t != fromChoice) {
                setState(S_IDLE);
                if(t instanceof ClassTarget && fromChoice instanceof ClassTarget) {

                    ClassTarget from = (ClassTarget)fromChoice;
                    ClassTarget to = (ClassTarget)t;

                    // if the target is an interface then we have an implements
                    // dependency
                    if(to.isInterface()) {
                        Dependency d = new ImplementsDependency(this, from, to);

                        if(from.isInterface()) {
                            userAddImplementsInterfaceDependency(d);
                        } else {
                            userAddImplementsClassDependency(d);
                        }

                        addDependency(d, true);
                    }
                    else {
                        // an extends dependency can only be from a class to another
                        // class
                        if(!from.isInterface()) {
                            Dependency d = new ExtendsDependency(this, from, to);
                            userAddExtendsClassDependency(d);
                            addDependency(d, true);
                        }
                    }
                }
                setStatus(" ");
            }
            break;

        default:
            // e.g. deleting arrow - selecting target ignored
            break;
        }
    }

    /**
     * Use the dialog manager to display an error message.
     * The PkgMgrFrame is used to find a parent window so we
     * can correctly offset the dialog.
     */
    public void showError(String msgId)
    {
        PkgMgrFrame.showError(this, msgId);
    }

    /**
     * Use the dialog manager to display a message.
     * The PkgMgrFrame is used to find a parent window so we
     * can correctly offset the dialog.
     */
    public void showMessage(String msgId)
    {
        PkgMgrFrame.showMessage(this, msgId);
    }


    /**
     * Use the dialog manager to display a message with text.
     * The PkgMgrFrame is used to find a parent window so we
     * can correctly offset the dialog.
     */
    public void showMessageWithText(String msgId, String text)
    {
        PkgMgrFrame.showMessageWithText(this, msgId, text);
    }

    /**
     * Report an execption. Usually, we do this through "errorMessage", but
     * if we cannot make sense of the message format, and thus cannot figure
     * out class name and line number, we use this way.
     */
    public void reportException(String text)
    {
        showMessageWithText("exception-thrown", text);
    }

    /**
     * Don't remember the last shown source anymore.
     */
    public void forgetLastSource()
    {
        lastSourceName = "";
    }

    /**
     * A thread has hit a breakpoint or done a step. Organise display
     * (highlight line in source, pop up exec controls).
     */
    public boolean showSource(String sourcename, int lineNo,
                              String threadName, boolean breakpoint)
    {
        String msg = " ";

        if(breakpoint)
            msg = "Thread \"" + threadName + "\" stopped at breakpoint.";

        boolean bringToFront = !sourcename.equals(lastSourceName);
        lastSourceName = sourcename;

        if(! showEditorMessage(new File(getPath(),sourcename).getPath(), lineNo,
                               msg, false, false, bringToFront, true, null))
            showMessageWithText("break-no-source", sourcename);

        return bringToFront;
    }

    /**
     * Display an error message associated with a specific line in a class.
     * This is done by opening the class's source, highlighting the line
     * and showing the message in the editor's information area.
     */
    private boolean showEditorMessage(String filename, int lineNo,
                        String message, boolean invalidate, boolean beep,
                        boolean bringToFront, boolean setStepMark, String help)
    {
        String fullName = getProject().
            convertPathToPackageName(filename);
        String packageName = JavaNames.getPrefix(fullName);
        String className = JavaNames.getBase(fullName);

        ClassTarget t;

        // check if the error is from a file belonging to another package
        if (packageName != getQualifiedName()) {

            // It is a getPackage() 31103 Damiano
            Package pkg = getProject().getPackage(packageName);
            PkgMgrFrame pmf;

            if ((pmf = PkgMgrFrame.findFrame(pkg)) == null) {
                pmf = PkgMgrFrame.createFrame(pkg);
            }

            pmf.show();

            t = (ClassTarget) pkg.getTarget(className);
        }
        else
            t = (ClassTarget) getTarget(className);

        if(t == null)
            return false;

        if(invalidate) {
            t.setState(Target.S_INVALID);
            t.setQueued(false);
        }

        if(bringToFront || !t.getEditor().isShowing())
            t.open();
        Editor editor = t.getEditor();
        if(editor!=null)
            editor.displayMessage(message, lineNo, 0, beep, setStepMark,
                                  help);
        else
            Debug.message(t.getDisplayName() + ", line" + lineNo + ": " +
                          message);
        return true;
    }

    /**
     * A breakpoint in this package was hit.
     */
    public void hitBreakpoint(DebuggerThread thread)
    {
        showSource(thread.getClassSourceName(0),
                   thread.getLineNumber(0),
                   thread.getName(), true);

		getProject().getExecControls().showHide(true);
		getProject().getExecControls().makeSureThreadIsSelected(thread);                   
    }

    /**
     * Execution stopped by someone pressing the "halt" button
     * or we have just done a "step".
     */
    public void hitHalt(DebuggerThread thread)
    {
        showSourcePosition(thread);

		getProject().getExecControls().showHide(true);
		getProject().getExecControls().makeSureThreadIsSelected(thread);                   
    }

    /**
     * showSourcePosition - The debugger display needs updating.
     */
    public void showSourcePosition(DebuggerThread thread)
    {
        int frame = thread.getSelectedFrame();
        if(showSource(thread.getClassSourceName(frame),
                      thread.getLineNumber(frame),
                      thread.getName(), false))
        {
			getProject().getExecControls().setVisible(true);
			//getProject().getExecControls().makeSureThreadIsSelected(thread);
        }
    }

	/**
	 * Display an exception message. This is almost the same as "errorMessage"
	 * except for different help texts.
	 */
	public void exceptionMessage(List stack, String message,
								 boolean invalidate)
	{
		if((stack == null ) || (stack.size() == 0)) {
			Debug.message("Stack missing in exception event");
			Debug.message("exc message: " + message);
			return;
		}
	
		// using the stack, try to find the source code
		boolean done = false;
		Iterator iter = stack.iterator();
		boolean firstTime = true;
	
		while(!done && iter.hasNext()) {
			SourceLocation loc = (SourceLocation)iter.next();
			String filename = new File(getPath(), loc.getFileName()).getPath();
			int lineNo = loc.getLineNumber();
			done = showEditorMessage(filename, lineNo, message, invalidate,
									 true, true, false, "exception");
			if(firstTime && !done) {
				message += " (in " + loc.getClassName() + ")";
				firstTime = false;
			}
		}
		if(!done) {
			SourceLocation loc = (SourceLocation)stack.get(0);
			showMessageWithText("error-in-file",
								loc.getClassName() + ":" +
								loc.getLineNumber() + "\n" + message);
		}
	}

    // ---- bluej.compiler.CompileObserver interface ----

	/**
	 * Observe compilation jobs and change the PkgMgr interface
	 * elements as compilation goes through different stages. 
	 */
	class PackageCompileObserver implements CompileObserver
	{
		protected void markAsCompiling(File[] sources)
		{
			for(int i = 0; i < sources.length; i++) {
				String fileName = sources[i].getPath();
				String fullName = getProject().convertPathToPackageName(fileName);
	
				Target t = (Target) getTarget(JavaNames.getBase(fullName));
	
				if(t != null)
					t.setState(ClassTarget.S_COMPILING);
			}
		}

	    /**
	     *  A compilation has been started. Mark the affected classes as being
	     *  currently compiled.
	     */
	    public void startCompile(File[] sources)
	    {
	        // The following two lines will send a compilation event to extensions.
	        CompileEvent aCompileEvent = new CompileEvent(CompileEvent.COMPILE_START_EVENT, sources);
	        ExtensionsManager.get().delegateEvent(aCompileEvent);
	   
	        setStatus(compiling);

	        if (sources.length > 0) {
	            getProject().removeLocalClassLoader();
	            getProject().newRemoteClassLoader();
	        }

			markAsCompiling(sources);
	    }
	
	    /**
	     * Display an error message associated with a specific line in a class.
	     * This is done by opening the class's source, highlighting the line
	     * and showing the message in the editor's information area.
	     */
	    public void errorMessage(String filename, int lineNo, String message,
	                             boolean invalidate)
	    {
	        // The following lines will send a compilation Error event to extensions.
	        int eventId = invalidate?CompileEvent.COMPILE_ERROR_EVENT:CompileEvent.COMPILE_WARNING_EVENT;
	        File [] sources = new File[1]; 
	        sources [0] = new File(filename);
	        CompileEvent aCompileEvent = new CompileEvent(eventId, sources);
	        aCompileEvent.setErrorLineNumber(lineNo);
	        aCompileEvent.setErrorMessage(message);
	        ExtensionsManager.get().delegateEvent(aCompileEvent);
	 
	        if(! showEditorMessage(filename, lineNo, message, invalidate, true,
	                               true, false, Config.compilertype))
	            showMessageWithText("error-in-file",
	                                filename + ":" + lineNo +
	                                "\n" + message);
	    }

		public void exceptionMessage(List stack, String message,
									 boolean invalidate)
		{
			Package.this.exceptionMessage(stack, message, invalidate);
		}
	
	    public void checkTarget(String qualifiedName)
	    {
	
	    }
	
	
	    /**
	     *  Compilation has ended.  Mark the affected classes as being
	     *  normal again.
	     */
	    public void endCompile(File[] sources, boolean successful)
	    {
	        // The following three lines will send a compilation event to extensions.
	        int eventId = successful?CompileEvent.COMPILE_DONE_EVENT:CompileEvent.COMPILE_FAILED_EVENT;
	        CompileEvent aCompileEvent = new CompileEvent(eventId,sources);
	        ExtensionsManager.get().delegateEvent(aCompileEvent);
	    
	        for(int i = 0; i < sources.length; i++) {
	            String filename = sources[i].getPath();
	
	            String fullName = getProject().convertPathToPackageName(filename);
	
	            ClassTarget t = (ClassTarget) targets.get(JavaNames.getBase(fullName));
	
	            if (t == null)
	                continue;
	
	            if (successful) {
	                t.endCompile();
	
	                /* compute ctxt files (files with comments and parameters names) */
	                try {
	                    ClassInfo info = ClassParser.parse(t.getSourceFile(), new Vector(getAllClassnames()));
	
	                    OutputStream out = new FileOutputStream(t.getContextFile());
	                    info.getComments().store(out, "BlueJ class context");
	                    out.close();
	                } catch (Exception ex) {
	                    ex.printStackTrace();
	                }
	            }
	
	            t.setState(successful ? Target.S_NORMAL : Target.S_INVALID);
                t.setQueued(false);
	            if(successful && t.editorOpen())
	                t.getEditor().setCompiled(true);
	        }
	        setStatus(compileDone);
	        getEditor().repaint();
	    }
	}
	
	class QuietPackageCompileObserver extends PackageCompileObserver
	{
		public void startCompile(File[] sources)
		{
			// the following two lines will send a compilation event to extensions.
			CompileEvent aCompileEvent = new CompileEvent(CompileEvent.COMPILE_START_EVENT, sources);
			ExtensionsManager.get().delegateEvent(aCompileEvent);
	   
			setStatus(compiling);
	
			markAsCompiling(sources);
		}

		public void errorMessage(String filename, int lineNo, String message,
								 boolean invalidate)
	    {
	    }

		public void exceptionMessage(List stack, String message,
									 boolean invalidate)
		{
		}
		
/*		public void endCompile(File[] sources, boolean successful)
		{
			
		} */
		
	}

    // ---- end of bluej.compiler.CompileObserver interface ----


    /**
     * Report an exit of a method through "System.exit()" where we expected
     * a result or an object being created.
     */
    public void reportExit(String exitCode)
    {
        showMessageWithText("system-exit", exitCode);
    }



    /**
     * closeAllEditors - closes all currently open editors within package
     * Should be run whenever a package is removed from PkgFrame.
     */
    public void closeAllEditors()
    {
        for(Iterator it = targets.iterator(); it.hasNext(); ) {
            Target t = (Target)it.next();
            if(t instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget)t;
                if(ct.editorOpen())
                    ct.getEditor().close();
            }
        }
    }

    /**
     * get history of invocation calls
     * @return CallHistory object
     */
    public CallHistory getCallHistory()
    {
        return callHistory;
    }

    /**
     * Called after a change to a Target
     */
    public void invalidate(Target t)
    {
        if(t instanceof ClassTarget) {
            ClassTarget ct = (ClassTarget)t;
        }
    }

    /**
     * find an arrow, given a point on the screen
     */
    Dependency findArrow(int x, int y)
    {
        for(Iterator it = usesArrows.iterator(); it.hasNext(); ) {
            Dependency d = (Dependency)it.next();
            if(d.contains(x, y))
                return d;
        }

        for(Iterator it = extendsArrows.iterator(); it.hasNext(); ) {
            Dependency d = (Dependency)it.next();
            if(d.contains(x, y))
                return d;
        }

        return null;
    }

   
	// MouseListener interface - only used while deleting arrow

    /**
     * remove the arrow representing the dependency d
     * @param d the dependency to remove
     */
    public void removeArrow(Dependency d){
        if (!(d instanceof UsesDependency)) {
            userRemoveDependency(d);
        }
        removeDependency(d, true);
        currentArrow = null;
        getEditor().repaint();
    }

    public void mousePressed(MouseEvent evt){}
    public void mouseReleased(MouseEvent evt) {}
    public void mouseClicked(MouseEvent evt) {}
    public void mouseEntered(MouseEvent evt) {}
    public void mouseExited(MouseEvent evt) {}

    // MouseMotionListener interface

    public void mouseDragged(MouseEvent evt) {}

    public void mouseMoved(MouseEvent evt) {}	
}
