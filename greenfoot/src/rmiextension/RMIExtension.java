/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2013,2016  Poul Henriksen and Michael Kolling
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package rmiextension;

import bluej.Boot;
import bluej.collect.DataSubmissionFailedDialog;
import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;
import greenfoot.core.GreenfootLauncherBlueJVM;
import greenfoot.guifx.GreenfootGuiHandler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import bluej.Config;
import bluej.Main;
import bluej.debugger.jdi.NetworkTest;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.Extension;
import bluej.extensions.event.ApplicationEvent;
import bluej.extensions.event.ApplicationListener;
import bluej.utility.Debug;
import javafx.application.Platform;

/**
 * This is the starting point of Greenfoot as a BlueJ Extension.
 * 
 * @author Poul Henriksen
 */
public class RMIExtension extends Extension implements ApplicationListener
{
    private BlueJ theBlueJ;

    /**
     * When this method is called, the extension may start its work.
     */
    public void startup(BlueJ bluej)
    {
        theBlueJ = bluej;
        Main.setGuiHandler(new GreenfootGuiHandler());

        // We can do this at any point, because although the submission failure may have already
        // happened, the event is re-issued to new listeners.
        bluej.addApplicationListener(new ApplicationListener()
        {
            @Override
            public void blueJReady(ApplicationEvent event)
            {

            }

            @Override
            public void dataSubmissionFailed(ApplicationEvent event)
            {
                if (Boot.isTrialRecording())
                {
                    Platform.runLater(() -> {
                        new DataSubmissionFailedDialog().show();
                    });
                }
            }
        });

        bluej.addCompileListener(new CompileListener() {

            @Override
            public void compileWarning(CompileEvent event) { }

            @Override
            public void compileSucceeded(CompileEvent event)
            {
                // Do a Garbage Collection to finalize any garbage JdiObjects, thereby
                // allowing objects on the remote VM to be garbage collected.
                System.gc();
            }

            @Override
            public void compileStarted(CompileEvent event) { }

            @Override
            public void compileFailed(CompileEvent event) { }

            @Override
            public void compileError(CompileEvent event) { }
        });

        try {
            new BlueJRMIServer(theBlueJ);
        }
        catch (IOException e) {
            Debug.reportError("Could not launch RMI server", e);
            NetworkTest.doTest();
            //ProjectManager.greenfootLaunchFailed(null);
        }

        theBlueJ.addApplicationListener(this);
    }

    /**
     * Opens a project in BlueJ if no other projects are open.
     * 
     * @param projectPath path of the project to open.
     */
    public void maybeOpenProject(File projectPath)
    {
        // Now we need to find out if a greenfoot project is automatically
        // opening. If not we must open the dummy project.
        boolean openOrphans = "true".equals(Config.getPropString("bluej.autoOpenLastProject"));
        if (!openOrphans || !Main.hadOrphanPackages()) {
            if (theBlueJ.getOpenProjects().length == 0) {
                openProject(projectPath);
            }
        }
    }

    /**
     * Opens a project in BlueJ
     * 
     * @param projectPath path of the project to open.
     */
    public void openProject(File projectPath)
    {
        BProject project = theBlueJ.openProject(projectPath);
        if (project == null) {
            Debug.reportError("Could not open scenario: " + projectPath);
        }
    }

    /**
     * This method must decide if this Extension is compatible with the current
     * release of the BlueJ Extensions API
     */
    public boolean isCompatible()
    {
        return Config.isGreenfoot();
    }

    /**
     * Returns the version number of this extension
     */
    public String getVersion()
    {
        return ("2003.03");
    }

    /**
     * Returns the user-visible name of this extension
     */
    public String getName()
    {
        return ("greenfoot Extension");
    }

    @Override
    public String getDescription()
    {
        return ("greenfoot extension");
    }

    /**
     * Returns a URL where you can find info on this extension. The real problem
     * is making sure that the link will still be alive in three years...
     */
    @Override
    public URL getURL()
    {
        try {
            return new URL("http://www.greenfoot.org");
        }
        catch (MalformedURLException e) {
            return null;
        }
    }
    
    // ------------- ApplicationListener interface ------------
    
    public void blueJReady(ApplicationEvent event)
    {
        GreenfootLauncherBlueJVM.getInstance().launch(this);
    }

    @Override
    public void dataSubmissionFailed(ApplicationEvent event)
    {

    }
}
