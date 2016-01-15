/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2014,2016  Michael Kolling and John Rosenberg
 
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
package bluej.compiler;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This class adapts CompileObserver messages to run on the GUI thread.
 * 
 * @author Davin McCall
 */
final public class EventqueueCompileObserverAdapter
    implements CompileObserver, Runnable
{
    private EDTCompileObserver link;
    private int command;
    
    private static final int COMMAND_START = 0;
    private static final int COMMAND_DIAG = 1;
    private static final int COMMAND_END = 2;
    
    // parameters for COMMAND_START/COMMAND_END
    private CompileInputFile [] sources;
    private boolean successful;  // COMMAND_END only
    
    // parameters for COMMAND_DIAG
    private Diagnostic diagnostic;
    private boolean automatic; // COMMAND_START only

    /**
     * Constructor for EventqueueCompileObserver. The link parameter is a compiler
     * observer; all messages will be passed on to it, but on the GUI thread.
     */
    public EventqueueCompileObserverAdapter(EDTCompileObserver link)
    {
        this.link = link;
    }
    
    /**
     * This method switches execution to the GUI thread.
     */
    private void runOnEventQueue()
    {
        try {
            EventQueue.invokeAndWait(this);
        }
        catch (InterruptedException ie) {}
        catch (InvocationTargetException ite) { throw new RuntimeException(ite); }
    }
    
    // ---------------- CompileObserver interface ---------------------
    
    public synchronized void compilerMessage(Diagnostic diagnostic)
    {
        command = COMMAND_DIAG;
        this.diagnostic = diagnostic;
        runOnEventQueue();
    }
    
    public synchronized void startCompile(CompileInputFile[] csources, boolean automatic)
    {
        command = COMMAND_START;
        this.sources = csources;
        this.automatic = automatic;
        runOnEventQueue();
    }

    public synchronized void endCompile(CompileInputFile[] sources, boolean successful)
    {
        command = COMMAND_END;
        this.sources = sources;
        this.successful = successful;
        runOnEventQueue();

    }
    
    // ------------------ Runnable interface ---------------------
    
    @OnThread(value = Tag.Swing, ignoreParent = true)
    public void run()
    {
        // We're now running on the GUI thread. Call the chained compile observer.
        
        switch (command) {
            case COMMAND_START:
                link.startCompile(sources, automatic);
                break;
            case COMMAND_DIAG:
                link.compilerMessage(diagnostic);
                break;
            case COMMAND_END:
                link.endCompile(sources, successful);
                break;
        }
    }

}
