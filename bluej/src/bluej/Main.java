package bluej;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import bluej.extensions.event.ApplicationEvent;
import bluej.extmgr.ExtensionsManager;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;

/**
 * BlueJ starts here.
 * The Boot class, which is responsible for dealing with specialised
 * class loaders, constructs an object of this class to initiate the 
 * "real" BlueJ.
 *
 * @author  Michael Kolling
 * @version $Id: Main.java 3785 2006-02-16 02:35:45Z davmac $
 */
public class Main
{
    private int FIRST_X_LOCATION = 20;
    private int FIRST_Y_LOCATION = 20;

    /**
     * Entry point to starting up the system. Initialise the
     * system and start the first package manager frame.
     */
    public Main()
    {
        Boot boot = Boot.getInstance();
        String [] args = boot.getArgs();
        Properties commandLineProps = boot.getCommandLineProperties();
		File bluejLibDir = boot.getBluejLibDir();

        Config.initialise(bluejLibDir,commandLineProps);
        if (! Config.isGreenfoot()) {
            Config.setVMIconsName("vm.icns");
            Config.setVMDockName("BlueJ Virtual Machine");
        }
        else {
            Config.setVMIconsName("greenfootvm.icns");
            Config.setVMDockName("Greenfoot");
        }

        // workaround java's broken UNC path handling on Windows
        if (Config.getPropString("bluej.windows.customUNCHandler", "false").equals("true")) {
            String osname = System.getProperty("os.name", "");
            if(osname.startsWith("Windows"))
                URL.setURLStreamHandlerFactory(new BluejURLStreamHandlerFactory());
        }
        
        // process command line arguments, start BlueJ!
        processArgs(args);
    }

   
    
    /**
     * Start everything off. This is used to open the projects
     * specified on the command line when starting BlueJ.
     * Any parameters starting with '-' are ignored for now.
     */
    private void processArgs(String[] args)
    {
        boolean oneOpened = false;
        
        // Open any projects specified on the command line
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (!args[i].startsWith("-")) {
                    Project openProj;
                    if ((openProj = Project.openProject(args[i])) != null) {
                        oneOpened = true;

                        Package pkg = openProj.getPackage(openProj.getInitialPackageName());

                        PkgMgrFrame pmf = PkgMgrFrame.createFrame(pkg);

                        pmf.setLocation(i * 30 + FIRST_X_LOCATION, i * 30 + FIRST_Y_LOCATION);
                        pmf.show();
                    }
                }
            }
        }

        // if we have orphaned packages, these are re-opened
        if (!oneOpened) {
            // check for orphans...
            boolean openOrphans = "true".equals(Config.getPropString("bluej.autoOpenLastProject"));
            if (openOrphans && PkgMgrFrame.hadOrphanPackages()) {
                String exists = "";
                // iterate through unknown number of orphans
                for (int i = 1; exists != null; i++) {
                    exists = Config.getPropString(Config.BLUEJ_OPENPACKAGE + i, null);
                    if (exists != null) {
                        Project openProj;
                        // checking all is well (project exists)
                        if ((openProj = Project.openProject(exists)) != null) {
                            Package pkg = openProj.getPackage(openProj.getInitialPackageName());
                            PkgMgrFrame.createFrame(pkg);
                            oneOpened = true;
                        }
                    }
                }
            }
        }

        // Make sure at least one frame exists
        if (!oneOpened) {
            if (Config.isGreenfoot()) {
                // TODO: open default project
            }
            else {
            openEmptyFrame();
        }
        }

        ExtensionsManager.getInstance().delegateEvent(new ApplicationEvent(ApplicationEvent.APP_READY_EVENT));
    }
    
    /**
     * Open a single empty bluej window.
     *  
     */
    private void openEmptyFrame()
    {
        PkgMgrFrame frame = PkgMgrFrame.createFrame();
        frame.setLocation(FIRST_X_LOCATION, FIRST_Y_LOCATION);
        frame.show(); 
    }

    /**
     * Exit BlueJ.
     *
     * The open frame count should be zero by this point as PkgMgrFrame
     * is responsible for cleaning itself up before getting here.
     */
    public static void exit()
    {
        if (PkgMgrFrame.frameCount() > 0)
            Debug.reportError("Frame count was not zero when exiting. Work may not have been saved");

        // save configuration properties
        Config.handleExit();
        // exit with success status
        System.exit(0);
    }
}
