/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.platforms.ide;

import greenfoot.GreenfootImage;
import greenfoot.platforms.GreenfootUtilDelegate;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import bluej.Config;
import bluej.runtime.ExecServer;
import bluej.utility.BlueJFileReader;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

public class GreenfootUtilDelegateIDE implements GreenfootUtilDelegate
{
    /**
     * Creates the skeleton for a new class
     */
    @Override
    public void createSkeleton(String className, String superClassName, File file, String templateFileName)
            throws IOException
    {
        Dictionary<String, String> translations = new Hashtable<String, String>();
        translations.put("CLASSNAME", className);
        if(superClassName != null) {
            translations.put("EXTENDSANDSUPERCLASSNAME", "extends " + superClassName);
        } else {
            translations.put("EXTENDSANDSUPERCLASSNAME", "");
        }
        String baseName = "greenfoot/templates/" +  templateFileName;
        File template = Config.getLanguageFile(baseName);
        
        if(!template.canRead()) {
            template = Config.getDefaultLanguageFile(baseName);
        }
        BlueJFileReader.translateFile(template, file, translations, Charset.forName("UTF-8"), Charset.defaultCharset());
        
    }
    
    @Override
    public URL getResource(String path) 
    {
        return ExecServer.getCurrentClassLoader().getResource(path);
    }
    
    @Override
    public Iterable<String> getSoundFiles()
    {
        ArrayList<String> files = new ArrayList<String>();
        try
        {
            URL url = getResource("sounds");
            if (url != null && "file".equals(url.getProtocol()))
            {
                for (String file : new File(url.toURI()).list())
                {
                    files.add(file);
                }
            }
        }
        catch (URISyntaxException e)
        {
            Debug.reportError("Bad URI in getResources", e);
        }
        // May be a blank list if something went wrong:
        return files;
    }
    
    /**
     * Returns the path to a small version of the greenfoot logo.
     */
    @Override
    public  String getGreenfootLogoPath()
    {        
        File libDir = Config.getGreenfootLibDir();
        return libDir.getAbsolutePath() + "/imagelib/other/greenfoot.png";        
    }

    @Override
    public boolean addCachedImage(String fileName, GreenfootImage image) 
    { 
        return false;
    }

    @Override
    public GreenfootImage getCachedImage(String fileName)
    { 
        return null;
    }

    @Override
    public void removeCachedImage(String fileName) { }

    @Override
    public boolean isNullCachedImage(String fileName)
    {
        return false;
    }

    @Override
    public void displayMessage(Component parent, String messageText)
    {
        DialogManager.showText(parent, messageText);
    }
}
