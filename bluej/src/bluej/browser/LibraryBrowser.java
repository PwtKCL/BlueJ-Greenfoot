package bluej.browser;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.tree.*;

import java.awt.event.*;
import java.awt.*;
import javax.swing.border.TitledBorder;

import bluej.graph.GraphEditor;
import bluej.Config;
import bluej.utility.ToggleMessageBox;
import bluej.utility.ToggleMessageBoxOwner;
import bluej.utility.Utility;
import bluej.utility.DialogManager;
import bluej.utility.Debug;
import bluej.editor.Editor;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageCacheMgr;

import java.util.*;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Implements a  browser for BlueJ class libraries.
 *
 * @author	Andy Marks
 *		    Andrew Patterson
 * @version	$Id: LibraryBrowser.java 277 1999-11-16 00:57:17Z ajp $
 **/
public class LibraryBrowser extends JFrame implements ActionListener
{
    private static final Image iconImage = new ImageIcon(Config.getImageFilename("image.icon")).getImage();

	// panel which holds display of the list of classes
	private ClassChooser classChooser = null;
	// panel which holds display of methods/fields of current class
	private AttributeChooser attributeChooser = null;
	// panel which holds tree listing class heirarchy
	private LibraryChooser libraryChooser = null;

    private CodeViewer codeViewer = null;
    
    private JMenuItem closeMI = null;
    private JMenuItem refreshMI = null;
    private JMenuItem addLibMI = null;
    private JMenuItem findMI = null;
    private JMenuItem aboutMI = null;
    private JMenuItem topicsMI = null;
    private JMenuItem contentsMI = null;
    private JMenuItem indexMI = null;
    private JMenuItem propMI = null;
    private JMenuItem editPackageMI = null;
    private JMenuBar menuBar = null;
  

    private static final Dimension CHOOSERPANELSIZE = new Dimension(200, 250);
	private static final Dimension classPanelSize = new Dimension(700, 450);
    /* 
	Package cache: Hashtable mapping package dir -> Package object
    */
    private PackageCacheMgr packageCache = new PackageCacheMgr();

    private String currentPackageName = null;
    private String currentPackageDir = null;
    private Package currentPackage = null;
    private static LibraryBrowser frame = null;

	private static final char INNERCLASSINDICATOR = '$';
	private static final String JAVASOURCEEXTENSION = ".java";
	
	// index into showDialog array
	private static final int INNERCLASSDIALOG = 0;
	private static final int NOSOURCEFORCLASSDIALOG = 1;
	private boolean[] showDialog = {true, true};

    /**
     * Initialize and display a new Library Browser.  Currently singleton.
     */
    public LibraryBrowser()
    {

        // hack way of making class singleton
    	if (frame != null) {
    	    DialogManager.showMessage(this, "one-browser-only");
    	    return;
    	}

    	setIconImage(iconImage);
    	setSize(new Dimension(780,580));
    	
    	// don't create these during declaration or they'll be 
    	// instantiated before we even know if we want to/can
    	// create another Library Browser
        classChooser = new ClassChooser();
    	libraryChooser = new LibraryChooser();
    	attributeChooser = new AttributeChooser();
    	codeViewer = new CodeViewer();
    	
    	frame = this;
    	setTitle(Config.getString("browser.title"));
    	
    	setupUI();
    	
    //	pack();
    	show();
    	
    	addWindowListener(new WindowAdapter() {
    		public void windowClosing(WindowEvent e) {
    			close();
    		}
    	});
	
//	classChooser.addActionListener(new ActionListener() {
//	    public void actionPerformed(ActionEvent e) {
//	        openClass(e.getActionCommand());
//	    }
//       });

        /**
         * Register a listener for when the user selects a node in the
         * library chooser. When they do, give the package selected to
         * the class chooser.
         */
        libraryChooser.addLibraryChooserListener(
            new LibraryChooserListener()
            {
                public void nodeEvent(LibraryChooserEvent e) {

                    String packageName = libraryChooser.pathToPackageName(
                                                new TreePath(e.getNode().getPath()));

                    String classes[] = libraryChooser.findClassesOfPackage(e.getNode());
                    String packages[] = libraryChooser.findNestedPackagesOfPackage(e.getNode());

                    classChooser.openPackage(packageName, classes, packages);
                }
            }            
        );
    }

    /**
     * Depending on whether we're running standalone or not, hide
     * the browser window or kill the application
     */
    public void close() {
	if (libraryChooser != null)
	
		if (frame != null) {
			setVisible(false);
			frame = null; // so that next browser start wont short circuit and quit
		}
		dispose();
    }
    
	/**
	 * Choose the layout manager, add the components to the frame
	 * and layout the frame.
	 **/
	private void setupUI() {
		setupMenus();

//		libraryChooser.setPreferredSize(new Dimension(250, 200));
//		attributeChooser.setPreferredSize(CHOOSERPANELSIZE);

		JPanel fullPane = new JPanel();
		{
//			JSplitPane splitPaneOne = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
//			{
//				JPanel chooserPanel = new JPanel();
//				{
					/* Choose the layout manager, add the components to the panel
					   and layout the panel for the class panel.  It's two main
					   components are the libraryChooser and classChooser. */

					JSplitPane splitPaneTwo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
					{
						splitPaneTwo.setLeftComponent(libraryChooser);
						splitPaneTwo.setDividerSize(Config.splitPaneDividerWidth);
						splitPaneTwo.setRightComponent(classChooser);
						splitPaneTwo.setOneTouchExpandable(false);
			         
						// set the initial location and size of the divider
						splitPaneTwo.setDividerLocation(getWidth()/3);
					}

//					chooserPanel.setLayout(new BorderLayout());
//					chooserPanel.add(splitPaneTwo, BorderLayout.CENTER);
//				}

				// classChooser is a global variable
//				{
//					classChooser.setPreferredSize(new Dimension(700, 400));
//				}

//				splitPaneOne.setDividerSize(Config.splitPaneDividerWidth);
//				splitPaneOne.setTopComponent(chooserPanel);
//				splitPaneOne.setBottomComponent(classChooser);
//				splitPaneOne.setOneTouchExpandable(false);
			        
				// set the initial location and size of the divider
//				splitPaneOne.setDividerLocation(200);
//			}

			fullPane.setBorder(Config.generalBorder);
			fullPane.setLayout(new BorderLayout());
			fullPane.add(splitPaneTwo, BorderLayout.CENTER);
//			fullPane.add(statusbar, BorderLayout.SOUTH);
		}

		getContentPane().add(fullPane);
	}

	/**
	 * Create menu bar, menus and menuitems.  Setup listeners for menu items.
	 **/
	protected void setupMenus() {
		menuBar = new JMenuBar();
		JMenu libraryM = new JMenu(Config.getString("browser.menu.library"));

		refreshMI = new JMenuItem(Config.getString("browser.menu.library.refresh"));
//		refreshMI.addActionListener(this);
//		libraryM.add(refreshMI);
//		refreshMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.getKeyText(KeyEvent.VK_F5)));
	
//		libraryM.addSeparator();
		closeMI = new JMenuItem(Config.getString("browser.menu.library.close"));
		closeMI.addActionListener(this);
		libraryM.add(closeMI);
		closeMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
	
		libraryM.setEnabled(false);
		menuBar.add(libraryM);
	
//		JMenu editM = new JMenu(Config.getString("browser.menu.edit"));
//		findMI = new JMenuItem(Config.getString("browser.menu.edit.find"));
//		findMI.addActionListener(this);
//		editM.add(findMI);
//		findMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK));

//		editM.setEnabled(false);
//		menuBar.add(editM);
			
//		JMenu packageM = new JMenu(Config.getString("browser.menu.package"));
	
//		if (!isStandalone) {
//			editPackageMI = new JMenuItem(Config.getString("browser.menu.package.edit"));
//			editPackageMI.addActionListener( new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					if (editor == null) 
//						return;
				
//				Utility.NYI(LibraryBrowserPkgMgrFrame.getFrame());
				// open a new PkgMgrFrame with this package in it
				//bluej.pkgmgr.Main.openPackage(currentPackageDir);
//			}});

//		packageM.add(editPackageMI);
//		}
	
//		packageM.setEnabled(false);
//		menuBar.add(packageM);
	
		setJMenuBar(menuBar);
	}
    
    /**
     * Handle events originated from menus, 
     **/
	public void actionPerformed(ActionEvent ae) {
	Object source = ae.getSource();
	if (source == closeMI) {
	    // we're about to exit - make sure everything is saved
				
	    close();
	} else if (source == findMI) {
//	    new FindLibraryDialog(this).display();
	} else if (source == refreshMI) {
	    libraryChooser.loadLibraries();
	}
    }


    /**
     * A package has been chosen to be used, now we need to identify where
     * to use the package.  If only one package is open, use that, otherwise
     * show a dialog listing all open packages and ask the user to choose. 
     * Either way, ask for confirmation before proceeding.
     * 
     * @param thePackage the name of the package to use in Java format (i.e., java.awt.Frame)
     * @param isClass true if the package is a single class, false otherwise
     */
    public void usePackage(String thePackage, boolean isClass) {
    
        Package[] possibleTargets = bluej.pkgmgr.Main.getAllOpenPackages();

        if (possibleTargets == null || possibleTargets.length == 0) {
//	    DialogManager.showError(LibraryBrowserPkgMgrFrame.getFrame(),
//			      "no-target-dialog");
            return;
        }

	if (possibleTargets.length > 1) {
//	    ChooseUseDestinationDIalog chooser = new ChooseUseDestinationDIalog(this, thePackage, isClass);
//	    chooser.setDestinations(possibleTargets);
//	    chooser.display();
	} else if (possibleTargets.length == 1) {
	    // no point giving them a choice of one
//	    usePackage(possibleTargets[0], thePackage, isClass);
	}
		
    }

    /**
     * Call the appropraite method in the package to use the selected package.
     * 
     * @param thePackage the Package in which to open the class or package
     */
    public void usePackage(Package thePackage, String lib, boolean isClass) {
	    if (isClass) {
		    // separate class name from package name
		    thePackage.insertLibClass(lib);
	    } else
		thePackage.insertLibPackage(lib);
    }

    
    /**
     * This is the main method to create a graphical display of a package
     * using a GraphEditor.  
     * 
     * Note: this method only adds the package to the cache if it is loaded
     * in this method (i.e., it is null upon entering this method).  If you
     * are calling this method with a valid package object, you must ensure
     * it is added to the cache yourself.
     * 
     * @param packageName the directory containing the package
     * @param pkg the package to open, or null if the package needs to be loaded
     */
	public void openPackage(String[] packageName)
	{

/*		boolean packageLoaded = true;

		if (pkg == null) {
			packageLoaded = false;
			pkg = new Package(packageName, this);
			packageCache.addPackageToCache(packageName, pkg);
		}
	
		// create a GraphEditor for this package
		// and enable the package menu if this is the first package to open
//		if (editor == null)
//			menuBar.getMenu(2).setEnabled(true);
	
//		editor = new GraphEditor(pkg, this);
//		editor.setReadOnly(true);

		// create a JScrollPane to hold the editor
		JScrollPane scroller = new JScrollPane(editor);
		// remove previous components so new GraphEditor will show
		classPanel.removeAll();
		
		// add newly created components back to panel
		classPanel.add(scroller, BorderLayout.CENTER);

		// load the package details - must do after setting up UI or NPE will occur

		Dimension d = this.getSize();

		if (packageLoaded == false) {
		    pkg.load(packageName, null, true, true);
//		    editor.setGraph(pkg);
		} else {
		    invalidate();
		    validate();
		}

		this.setSize(d);

		// store attributes of currently displayed package
		currentPackageName = packageName;
		currentPackage = pkg;
		currentPackageDir = pkg.getDirName();
	
		// make sure the scrollbar resizes to the current package dimensions
		scroller.invalidate();
		scroller.validate();
    */
	}

	/**
     * @param packageName the absolute path to the directory containing the package
     */
    public void openPackage(String packageName) {
	// check cache for package before creating a new one
	Package cachePackage = (Package)packageCache.getPackageFromCache(packageName);
	//openPackage(packageName, cachePackage);
	libraryChooser.selectPackageInTree(packageName);
   }
	

    /**
     * Update the class chooser and library chooser to reflect a new package.  
     * Make sure we're not trying to open a ZIP/JAR package here as it won't
     * exist if we try and access it using the normal approach.
     * Invoked when a package name is double clicked in the graph editor.
     * 
     * @param thePackage the PackageTarget object associated with the new package.
     */
//    public void openPackage(PackageTarget thePackage) {
	// need to use getParent() because packge base dir will end with start of the package name
//	String packageToOpen = new File(thePackage.getPackage().getBaseDir()).getParent() + File.separator + thePackage.packageName.replace('.', Config.slash);
	// make sure package is selected in tree before call to libraryChooser.openPackage#
//	libraryChooser.selectPackageInTree(packageToOpen);
//	libraryChooser.openSelectedPackageInClassChooser();
 //   }

    /**
     * Update both the code viewer and attribute chooser to reflect
     * a new class.  Invoked as the result of a class icon being
     * double clicked in the graph editor.
     * 
     * @param theClass the ClassTarget object associated with the new class
     */
    public void openClass(String theClass)
    {
        attributeChooser.openClass(theClass);
    }
	
    /**
     * Attempt to open the class in the code viewer if it exists.  If the class is
     * an inner class (embedde4 $ in name), open the parent class instead.
     * 
     * @param className the class name specified as a filesystem file (with .java extension)
     * @param isCompiled true if the file to open has been compiled
     */
//    private void openClassInCodeViewer(ClassTarget theClass) {
//	String className = theClass.sourceFile();

//	codeViewer.openClassWithNoSource(theClass, theClass.fullname, theClass.isCompiled());

/*	if (!new File(className).exists()) {
		if (showDialog[NOSOURCEFORCLASSDIALOG])
			new ToggleMessageBox(this, 
					     Utility.mergeStrings(Config.getString("browser.openclassineditor.nosourceforclassdialog.text"), theClass.sourceFile()),
					     Config.getString("browser.openclassineditor.nosourceforclassdialog.title"),
					     JOptionPane.INFORMATION_MESSAGE,
					     NOSOURCEFORCLASSDIALOG).display();
	} else {
		codeViewer.openClass(theClass, theClass.fullname, theClass.isCompiled());
		int innerClassIndicatorPos = className.indexOf(INNERCLASSINDICATOR);
		if (innerClassIndicatorPos != -1) {
			// it's an inner class - let's turn foo$bar.java into foo.java for MOE
			className = className.substring(0, innerClassIndicatorPos) + JAVASOURCEEXTENSION;
			if (showDialog[INNERCLASSDIALOG])
				new ToggleMessageBox(this, 
						     Config.getString("browser.openclassineditor.innerclassdialog.text"),
						     Config.getString("browser.openclassineditor.innerclassdialog.title"),
						     JOptionPane.INFORMATION_MESSAGE,
						     INNERCLASSDIALOG).display();
   
		}
	} */
//    }
		
    /**
     * @param className the class name specified in package notation (i.e., a.b.c)
     */
//    public void openClassInAttributeChooser(ClassTarget theClass) {
//	attributeChooser.openClass(theClass.fullname);
 //   }


}

