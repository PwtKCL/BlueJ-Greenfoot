package greenfoot.gui;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;
import greenfoot.ImageVisitor;
import greenfoot.World;
import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.GProject;
import greenfoot.event.ValidityEvent;
import greenfoot.event.ValidityListener;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GraphicsUtilities;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

/**
 * A dialog for selecting a class image. The image can be selected from either the
 * project image library, or the greenfoot library, or an external location.
 * 
 * @author Davin McCall
 * @version $Id: ImageLibFrame.java 5500 2008-01-29 00:22:23Z polle $
 */
public class ImageLibFrame extends EscapeDialog implements ListSelectionListener
{
    /** label displaying the currently selected image */
    private JLabel imageLabel;
    private JLabel imageTextLabel;
    private GClass gclass;
    /** The default image icon - either the greenfoot logo or autogenerated image */
    private Icon defaultIcon;
    
    private ImageLibList projImageList;
    private ImageLibList greenfootImageList;
    private Action okAction;
    
    private File selectedImageFile;
    private File projImagesDir;
    
    public static int OK = 0;
    public static int CANCEL = 1;
    private int result = CANCEL;
    
    private Image generatedImage;
    private boolean showingGeneratedImage;
    
    private int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
    private JTextField classNameField;

    
    /**
     * Construct an ImageLibFrame for changing the image of an existing class.
     * 
     * @param owner      The parent frame
     * @param classView  The ClassView of the existing class
     */
    public ImageLibFrame(JFrame owner, ClassView classView)
    {
        super(owner, Config.getString("imagelib.title") + classView.getClassName(), true);
        
        this.gclass = classView.getGClass();
        generatedImage = renderImage();
        if (generatedImage != null) {
            showingGeneratedImage = true;
            defaultIcon = new ImageIcon(GreenfootUtil.getScaledImage(generatedImage, dpi/2, dpi/2));
        }
        else {
            showingGeneratedImage = false;
            defaultIcon = getPreviewIcon(new File(GreenfootUtil.getGreenfootLogoPath()));
        }
        
        try {
        	buildUI(classView.getGClass().getPackage().getProject(), false);
        }
        catch (ProjectNotOpenException pnoe) {}
        catch (RemoteException re) {
        	re.printStackTrace();
        }
    }
    
    /**
     * Construct an ImageLibFrame to be used for creating a new class.
     * 
     * @param owner        The parent frame
     * @param superClass   The superclass of the new class
     */
    public ImageLibFrame(JFrame owner, GClass superClass)
    {
        super(owner, Config.getString("imagelib.newClass"), true);
        defaultIcon = getClassIcon(superClass, getPreviewIcon(new File(GreenfootUtil.getGreenfootLogoPath())));
        showingGeneratedImage = false;
        
        // this.classView = new ClassView()
        try {
        	buildUI(superClass.getPackage().getProject(), true);
        }
        catch (ProjectNotOpenException pnoe) {}
        catch (RemoteException re) {
        	re.printStackTrace();
        }
    }
    
    private void buildUI(GProject project, boolean includeClassNameField)
    {
        JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        contentPane.setBorder(BlueJTheme.dialogBorder);
        
        int spacingLarge = BlueJTheme.componentSpacingLarge;
        int spacingSmall = BlueJTheme.componentSpacingSmall;
        
        okAction = getOkAction();
        
        // Class details - name, current icon
        try {
            contentPane.add(buildClassDetailsPanel(includeClassNameField, project.getDefaultPackage()));
        }
        catch (ProjectNotOpenException e1) {
            e1.printStackTrace();
        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }
        
        // Image selection panels - project and greenfoot image library
        {
            //JPanel imageSelPanels = new JPanel();
            //imageSelPanels.setLayout(new GridLayout(1, 2, BlueJTheme.componentSpacingSmall, 0));
            Box imageSelPanels = new Box(BoxLayout.X_AXIS);
            
            // Project images panel
            {
                Box piPanel = new Box(BoxLayout.Y_AXIS);
                
                JLabel piLabel = new JLabel(Config.getString("imagelib.projectImages"));
                piLabel.setAlignmentX(0.0f);
                piPanel.add(piLabel);
                
                JScrollPane jsp = new JScrollPane();
                
                try {
                    File projDir = project.getDir();
                    projImagesDir = new File(projDir, "images");
                    projImageList = new ImageLibList(projImagesDir);
                    jsp.getViewport().setView(projImageList);
                }
                catch (ProjectNotOpenException pnoe) {}
                catch (RemoteException re) { re.printStackTrace(); }
                
                jsp.setBorder(Config.normalBorder);
                jsp.setViewportBorder(BorderFactory.createLineBorder(projImageList.getBackground(), 4));
                jsp.setAlignmentX(0.0f);
                
                piPanel.add(jsp);
                imageSelPanels.add(piPanel);
            }
            
            imageSelPanels.add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, spacingLarge));
            
            // Category selection panel
            ImageCategorySelector imageCategorySelector;
            {
                Box piPanel = new Box(BoxLayout.Y_AXIS);
                
                JLabel piLabel = new JLabel(Config.getString("imagelib.categories"));
                piLabel.setAlignmentX(0.0f);
                piPanel.add(piLabel);
                
                File imageDir = Config.getGreenfootLibDir();
                imageDir = new File(imageDir, "imagelib");
                imageCategorySelector = new ImageCategorySelector(imageDir);
                
                JScrollPane jsp = new JScrollPane(imageCategorySelector);
                
                jsp.setBorder(Config.normalBorder);
                jsp.setViewportBorder(BorderFactory.createLineBorder(imageCategorySelector.getBackground(), 4));
                jsp.setAlignmentX(0.0f);
                
                piPanel.add(jsp);
                imageSelPanels.add(piPanel);
            }

            imageSelPanels.add(GreenfootUtil.createSpacer(GreenfootUtil.X_AXIS, spacingSmall));

            // Greenfoot images panel
            {
                Box piPanel = new Box(BoxLayout.Y_AXIS);
                
                JLabel piLabel = new JLabel(Config.getString("imagelib.images"));
                piLabel.setAlignmentX(0.0f);
                piPanel.add(piLabel);
                
                JScrollPane jsp = new JScrollPane();
                
                greenfootImageList = new ImageLibList();
                jsp.getViewport().setView(greenfootImageList);
                
                jsp.setBorder(Config.normalBorder);
                jsp.setViewportBorder(BorderFactory.createLineBorder(greenfootImageList.getBackground(), 4));
                jsp.setAlignmentX(0.0f);
                
                piPanel.add(jsp);
                imageSelPanels.add(piPanel);
            }
            
            imageSelPanels.setAlignmentX(0.0f);
            contentPane.add(imageSelPanels);
            
            projImageList.addListSelectionListener(this);
            greenfootImageList.addListSelectionListener(this);
            imageCategorySelector.setImageLibList(greenfootImageList);
        }

        // Browse button. Select image file from arbitrary location.
        JButton browseButton = new JButton(Config.getString("imagelib.browse.button"));
        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser chooser = new JFileChooser();
                new ImageFilePreview(chooser);
                int choice = chooser.showDialog(ImageLibFrame.this, Config.getString("imagelib.choose.button"));
                if (choice == JFileChooser.APPROVE_OPTION) {
                    selectedImageFile = chooser.getSelectedFile();
                    imageLabel.setIcon(getPreviewIcon(selectedImageFile));
                }
            }
        });
        browseButton.setAlignmentX(0.0f);
        contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
        contentPane.add(fixHeight(browseButton));
        
        contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
        contentPane.add(fixHeight(new JSeparator()));
        
        // Ok and cancel buttons
        {
            JPanel okCancelPanel = new JPanel();
            okCancelPanel.setLayout(new BoxLayout(okCancelPanel, BoxLayout.X_AXIS));

            JButton okButton = BlueJTheme.getOkButton();
            okButton.setAction(okAction);
            
            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.setVerifyInputWhenFocusTarget(false);
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    result = CANCEL;
                    selectedImageFile = null;
                    setVisible(false);
                    dispose();
                }
            });
            
            okCancelPanel.add(Box.createHorizontalGlue());
            okCancelPanel.add(okButton);
            okCancelPanel.add(Box.createHorizontalStrut(spacingLarge));
            okCancelPanel.add(cancelButton);
            okCancelPanel.setAlignmentX(0.0f);
            okCancelPanel.validate();
            contentPane.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
            contentPane.add(fixHeight(okCancelPanel));
            
            getRootPane().setDefaultButton(okButton);
        }
        
        pack();
        DialogManager.centreDialog(this);
        setVisible(true);
    }
    
    /**
     * Build the class details panel.
     * 
     * @param includeClassNameField  Whether to include a field for
     *                              specifying the class name.
     * @param pkg 
     */
    private JPanel buildClassDetailsPanel(boolean includeClassNameField, GPackage pkg)
    {
        JPanel classDetailsPanel = new JPanel();
        classDetailsPanel.setLayout(new BoxLayout(classDetailsPanel, BoxLayout.Y_AXIS));
        
        int spacingLarge = BlueJTheme.componentSpacingLarge;
        int spacingSmall = BlueJTheme.componentSpacingSmall;
        
        // Show current image
        {
            JPanel currentImagePanel = new JPanel();
            currentImagePanel.setLayout(new BoxLayout(currentImagePanel, BoxLayout.X_AXIS));
            
            if (includeClassNameField) {
                Box b = new Box(BoxLayout.X_AXIS);
                JLabel classNameLabel = new JLabel(Config.getString("imagelib.className"));
                b.add(classNameLabel);
                
                // "ok" button should be disabled until class name entered
                okAction.setEnabled(false);
                
                classNameField = new JTextField(12);
                final JLabel errorMsgLabel = new JLabel();
                errorMsgLabel.setVisible(false);
                errorMsgLabel.setForeground(Color.RED);
                
                final ClassNameVerifier classNameVerifier = new ClassNameVerifier(classNameField, pkg);
                classNameVerifier.addValidityListener(new ValidityListener(){
                    public void changedToInvalid(ValidityEvent e)
                    {
                        errorMsgLabel.setText(e.getReason());
                        errorMsgLabel.setVisible(true);
                        okAction.setEnabled(false);
                    }

                    public void changedToValid(ValidityEvent e)
                    {
                        errorMsgLabel.setVisible(false);
                        okAction.setEnabled(true);
                    }});
                
                
                b.add(Box.createHorizontalStrut(spacingLarge));
                
                b.add(fixHeight(classNameField));
                b.setAlignmentX(0.0f);
                classDetailsPanel.add(b);
                
                classDetailsPanel.add(errorMsgLabel);

                classDetailsPanel.add(Box.createVerticalStrut(spacingLarge));
            }
            
            // help label
            JLabel helpLabel = new JLabel();
            if (showingGeneratedImage) {
                helpLabel.setText(Config.getString("imagelib.help.autoImage"));
            }
            else {
                helpLabel.setText(Config.getString("imagelib.help.selectImage"));
            }
            Font smallFont = helpLabel.getFont().deriveFont(Font.ITALIC, 11.0f);
            helpLabel.setFont(smallFont);
            classDetailsPanel.add(fixHeight(helpLabel));
            
            classDetailsPanel.add(fixHeight(Box.createVerticalStrut(spacingLarge)));
            
            classDetailsPanel.add(fixHeight(new JSeparator()));
            classDetailsPanel.add(Box.createVerticalStrut(spacingSmall));
            
            // new class image display 
            JLabel classImageLabel = new JLabel(Config.getString("imagelib.newClass.image"));
            currentImagePanel.add(classImageLabel);
            
            Icon icon;
            if (showingGeneratedImage) {
                icon = defaultIcon;
            }
            else {
                icon = getClassIcon(gclass, defaultIcon);
            }
            currentImagePanel.add(Box.createHorizontalStrut(spacingSmall));
            imageLabel = new JLabel(icon) {
                // We don't want changing the image to re-layout the
                // whole frame
                public boolean isValidateRoot()
                {
                    return true;
                }
            };
            currentImagePanel.add(imageLabel);
            currentImagePanel.add(Box.createHorizontalStrut(spacingSmall));
            imageTextLabel = new JLabel() {
                // We don't want changing the text to re-layout the
                // whole frame
                public boolean isValidateRoot()
                {
                    return true;
                }
            };
            currentImagePanel.add(imageTextLabel);
            if (showingGeneratedImage) {
                imageTextLabel.setText(Config.getString("imagelib.image.autoGenerated"));
            }
            currentImagePanel.setAlignmentX(0.0f);
            
            classDetailsPanel.add(fixHeight(currentImagePanel));
        }
        
        classDetailsPanel.setAlignmentX(0.0f);
        return classDetailsPanel;
    }
    
    /*
     * A new image was selected in one of the ImageLibLists
     */
    public void valueChanged(ListSelectionEvent lse)
    {
        Object source = lse.getSource();
        if (! lse.getValueIsAdjusting() && source instanceof ImageLibList) {
            imageTextLabel.setText("");
            ImageLibList sourceList = (ImageLibList) source;
            ImageLibList.ImageListEntry ile = sourceList.getSelectedEntry();
            
            if (ile != null) {
                showingGeneratedImage = false;
                imageLabel.setIcon(getPreviewIcon(ile.imageFile));
                selectedImageFile = ile.imageFile;
            }
        }
    }
    
    /**
     * Get a preview icon for a class. This is a fixed size image. The
     * user-specified image is normally used; if none exists, the class
     * hierarchy is searched.
     * 
     * @param gclass   The class whose icon to get
     * @param defaultIcon  The icon to return if none can be found
     */
    private static Icon getClassIcon(GClass gclass, Icon defaultIcon)
    {
        String imageName = null;
        
        if (gclass == null) {
            return defaultIcon;
        }
        
        while (gclass != null) {
            imageName = gclass.getClassProperty("image");
            
            // If an image is specified for this class, and we can read it, return
            if (imageName != null) {
                File imageFile = new File(new File("images"), imageName);
                if (imageFile.canRead()) {
                    return getPreviewIcon(imageFile);
                }
            }
            
            gclass = gclass.getSuperclass();
        }
        
        return defaultIcon;
    }
    
    /**
     * Load an image from a file and scale it to preview size.
     * @param fname  The file to load the image from
     */
    private static Icon getPreviewIcon(File fname)
    {
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        
        try {
            BufferedImage bi = ImageIO.read(fname);
            return new ImageIcon(GreenfootUtil.getScaledImage(bi, dpi/2, dpi/2));
        }
        catch (IOException ioe) {
            BufferedImage bi = GraphicsUtilities.createCompatibleTranslucentImage(dpi/2, dpi/2);
            return new ImageIcon(bi);
        }
    }
    
    /**
     * Fix the maxiumum height of the component equal to its preferred size, and
     * return the component.
     */
    private static Component fixHeight(Component src)
    {
        Dimension d = src.getMaximumSize();
        d.height = src.getPreferredSize().height;
        src.setMaximumSize(d);
        return src;
    }
    
    /**
     * Get the selected image file (null if dialog was canceled)
     */
    public File getSelectedImageFile()
    {
        if (result == OK) {
            return selectedImageFile;
        }
        else {
            return null;
        }
    }
    
    /**
     * Get the result from the dialog: OK or CANCEL
     */
    public int getResult()
    {
        return result;
    }
    
    /**
     * Get the name of the class as entered in the dialog.
     */
    public String getClassName()
    {
        return classNameField.getText();
    }
    
    /**
     * Try to get an image for the class by instantiating it, and grabbing the image from
     * the resulting object. Returns null if unsuccessful (or if the "generated" image is
     * really the same as the current class image).
     */
    private Image renderImage()
    {
        Object object = null;
        Class<?> cls = gclass.getJavaClass();
        
        if (cls == null) {
            return null;
        }
        try {
            object = cls.newInstance();
        }
        catch (LinkageError le) { }
        catch (InstantiationException e) {
            // No default constructor, or abstract class, or
            // other instantiation failure
        }
        catch (Throwable t) {
            // *Whatever* is thrown by user code, we want to catch it.
            t.printStackTrace();
        }
            
        if (object == null) {
            return null;
        }
        else if (object instanceof Actor) {
            Actor so = (Actor) object;
            GreenfootImage image = ActorVisitor.getDisplayImage(so);

            if (image != null) {
                //Image awtImage = image.getAwtImage();
                //rotate it.
                int rotation = so.getRotation();
                if (image != null && rotation != 0) {
                    BufferedImage bImg = GraphicsUtilities.createCompatibleTranslucentImage(image.getWidth(), image.getHeight());
                    Graphics2D g2 = (Graphics2D) bImg.getGraphics();

                    double rotateX = image.getWidth() / 2.;
                    double rotateY = image.getHeight() / 2.;
                    g2.rotate(Math.toRadians(so.getRotation()), rotateX, rotateY);

                    ImageVisitor.drawImage(image, g2, 0, 0, this);

                    return bImg;
                }
                
                // If the actor got clever and added itself to a world,
                // remove it again.
                World world = so.getWorld();
                if(world != null) {
                    world.removeObject(so);
                } 

                try {
                	GProject project = gclass.getPackage().getProject();
                	GreenfootImage classImage = project.getProjectProperties().getImage(gclass.getQualifiedName());
                	if (ImageVisitor.equal(classImage, image)) {
                		// "generated" image is actually just the class image
                		return null;
                	}
                	else {
                		return image.getAwtImage();
                	}
                }
                catch (RemoteException re) {
                	re.printStackTrace();
                }
                catch (ProjectNotOpenException pnoe) {}
            }
        }
        return null;
    }
    
    /**
     * Write the generate image to a file, and return the filename used.
     * 
     * @return The filename the image was written to, or null if cancelled.
     * @throws IOException
     */
    private File writeGeneratedImage()
        throws IOException
    {
        File f = new File(new File("images"), gclass.getName() + ".png");
        if (f.exists()) {
            int r = JOptionPane.showOptionDialog(this, 
                                                 Config.getString("imagelib.write.exists.part1") + f 
                                                    + Config.getString("imagelib.write.exists.part2"),
                                                 Config.getString("imagelib.write.exists.title"),
                                                 JOptionPane.OK_CANCEL_OPTION, 
                                                 JOptionPane.WARNING_MESSAGE,
                                                 null, null, null);
            
            if (r != JOptionPane.OK_OPTION) {
                return null;
            }
        }
        
        ImageIO.write((RenderedImage) generatedImage, "png", new FileOutputStream(f));
        
        return f;
    }

    /**
     * Get the action for the "ok" button.
     */
    private AbstractAction getOkAction()
    {
        return new AbstractAction(Config.getString("okay")) {
            public void actionPerformed(ActionEvent e)
            {
                result = OK;
                if (showingGeneratedImage) {
                    try {
                        selectedImageFile = writeGeneratedImage();
                        if (selectedImageFile == null) {
                            // cancelled by user.
                            return;
                        }
                    }
                    catch (IOException ioe) {
                        // TODO: report with dialog
                        ioe.printStackTrace();
                    }
                }
                setVisible(false);
                dispose();
            }
        };
    }
}
