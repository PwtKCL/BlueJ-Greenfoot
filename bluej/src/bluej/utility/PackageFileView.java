package bluej.utility;

import bluej.Config;
import bluej.pkgmgr.Package;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.io.File;

/**
 * A FileView subclass that enables BlueJ packages to be displayed with a
 * distinct icon in a FileChooser.
 *
 * @version $ $
 * @author Michael Kolling
 * @see FileUtility
 */
public class PackageFileView extends FileView {

    static final Icon packageIcon = Config.getImageAsIcon("image.packageIcon");

    /**
     * The name of the file.  Do nothing special here. Let the system file
     * view handle this. (All methods that return null get then handled by
     * the system.)
     */
    public String getName(File f) {
	return null;
    }

    /**
     * A human readable description of the file.
     */
    public String getDescription(File f) {
	return "Unknown file";
    }

    /**
     * A human readable description of the type of the file.
     */
    public String getTypeDescription(File f) {
	return null;
    }

    /**
     * Here we return proper BlueJ package icons for JB packages.
     * Everything else gets handled by the system (by returning null).
     */
    public Icon getIcon(File f) {
	if(Package.isBlueJPackage(f))
	    return packageIcon;
	else
	    return null;
    }

    /**
     * Whether the directory is traversable or not.
     */
    public Boolean isTraversable(File f) {
	if(f.isDirectory()) {
	    return Boolean.TRUE;
	} else {
	    return Boolean.FALSE;
	}
    }

}
