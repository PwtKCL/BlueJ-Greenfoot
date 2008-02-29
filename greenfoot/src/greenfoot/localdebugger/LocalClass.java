package greenfoot.localdebugger;

import greenfoot.Actor;
import greenfoot.World;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;

/**
 * Represent a local class as a DebuggerClass.
 * 
 * @author Davin McCall
 * @version $Id: LocalClass.java 5621 2008-02-29 18:24:46Z polle $
 */
public class LocalClass extends DebuggerClass
{
    private Class cl;
    private static Field [] noFields = new Field[0];
     
    /**
     * Constructor for LocalClass.
     */
    public LocalClass(Class cl)
    {
        this.cl = cl;
    }
    
    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#getName()
     */
    public String getName()
    {
        return cl.getName();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#getStaticFieldCount()
     */
    public int getStaticFieldCount()
    {        
        return getFields().length;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#getStaticFieldName(int)
     */
    public String getStaticFieldName(int slot)
    {
        Field field = getFields()[slot];
        return field.getName();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#getStaticFieldObject(int)
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        Field field = getFields()[slot];
        try {
            return LocalObject.getLocalObject(field.get(null));
        }
        catch (IllegalAccessException iae) {}
        return null;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#getStaticFields(boolean)
     */
    public List getStaticFields(boolean includeModifiers)
    {
        List r = new ArrayList();
        
        Field [] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            // skip non-instance fields
            int mods = fields[i].getModifiers();
            
            String desc = "";
            if (includeModifiers) {
                desc = Modifier.toString(mods) + " ";
            }
            
            desc += fields[i].getName() + " = ";
            try {
                if (fields[i].getType().isPrimitive()) {
                    desc += fields[i].get(null);
                }
                else {
                    Object fieldval = fields[i].get(null);
                    if (fieldval instanceof String)
                        desc += '\"' + fieldval.toString() + '\"';
                    else if (fieldval == null)
                        desc += Config.getString("debugger.null");
                    else
                        desc += DebuggerObject.OBJECT_REFERENCE;
                }
            }
            catch (IllegalAccessException iae) {
                desc += "?";
            }
            
            r.add(desc);
        }
        
        return r;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#staticFieldIsPublic(int)
     */
    public boolean staticFieldIsPublic(int slot)
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#staticFieldIsObject(int)
     */
    public boolean staticFieldIsObject(int slot)
    {
        Field field = getFields()[slot];
        return ! field.getType().isPrimitive()
            && fieldNotNull(field);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#isInterface()
     */
    public boolean isInterface()
    {
        return cl.isInterface();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#isEnum()
     */
    public boolean isEnum()
    {
        // TODO support for enums
        return false;
    }

    /**
     * Convenience method to get static fields, public
     * and private. Except the ones that should be filtered out.
     */
    private Field [] getFields()
    {
        ArrayList allFields = new ArrayList();
        Class c = cl;
        
        while (c != null) {
            Field [] declFields = c.getDeclaredFields();
            ArrayList sfields = new ArrayList();
            for (int i = 0; i < declFields.length; i++) {
                Field field = declFields[i];
                if ((field.getModifiers() & Modifier.STATIC) != 0 && keepField(c, field))
                    sfields.add(field);
            }
            
            declFields = (Field []) sfields.toArray(noFields);
            AccessibleObject.setAccessible(declFields, true);
            allFields.addAll(Arrays.asList(declFields));
            c = c.getSuperclass();

        }

        return (Field []) allFields.toArray(noFields);
    }
    
    /**
     * Check whether a field in this class contains a null reference.
     */
    public boolean fieldNotNull(Field field)
    {
        try {
            Object v = field.get(null);
            return v != null;
        }
        catch (IllegalAccessException iae) { return false; }
    }

    /**
     * Whether a given field should be used.
     * @return True if the field should be used, false if it should be ignored
     */
    private boolean keepField(Class cls, Field field) 
    {
        if(cls.equals(World.class)) {
            return false;
        }
        else if(cls.equals(Actor.class)) {
            return false;
        }
        return true;            
    }
}
