package bluej.debugger.jdi;

import java.util.ArrayList;
import java.util.List;

import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.utility.Debug;
import bluej.utility.JavaNames;

import com.sun.jdi.*;

/**
 * Represents an object running on the user (remote) machine.
 *
 * @author  Michael Kolling
 * @version $Id: JdiObject.java 2554 2004-05-27 08:20:56Z polle $
 */
public class JdiObject extends DebuggerObject
{
    private static final String nullLabel =
		Config.getString("debugger.null");
    
    // boolean - true if our JVM supports generics
    static boolean jvmSupportsGenerics = Config.isJava15();
    
    /**
     *  Factory method that returns instances of JdiObjects.
     *
     *  @param  obj  the remote object this encapsulates.
     *  @return      a new JdiObject or a new JdiArray object if
     *               remote object is an array
     */
    public static JdiObject getDebuggerObject(ObjectReference obj)
    {
        if (obj instanceof ArrayReference) {
            return new JdiArray((ArrayReference) obj);
        } else {
            if( jvmSupportsGenerics )
                return JdiObject15.getDebuggerObject(obj);
            else
                return new JdiObject(obj);
        }
    }

    /**
     * Get a JdiObject from a field. 
     * @param obj    Represents the value of the field.
     * @param field  The field.
     * @param parent The parent object containing the field.
     * @return
     */
    public static JdiObject getDebuggerObject(ObjectReference obj, Field field, JdiObject parent)
    {
        if (obj instanceof ArrayReference) {
            return new JdiArray((ArrayReference) obj);
        } else {
            if( jvmSupportsGenerics )
                return JdiObject15.getDebuggerObject(obj, field, (JdiObject15)parent);
            else
                return new JdiObject(obj);
        }
    }
    
    
    // -- instance methods --

    ObjectReference obj;  // the remote object represented
    List fields;

    protected JdiObject()
    {
    }

    /**
     *  Constructor is private so that instances need to use getJdiObject
     *  factory method.
     *
     *  @param  obj  the remote debugger object (Jdi code) this encapsulates.
     */
    private JdiObject(ObjectReference obj)
    {
        this.obj = obj;
        getRemoteFields();
    }


    /**
     *  Get the (raw) name of the class of this object.
     *
     *  @return    The ClassName value
     */
    public String getClassName()
    {
        if (obj == null)
            return "";
        else
        return obj.referenceType().name();
    }

    /**
     * Get the generic name of the class of the object. All names are fully
     * qualified
     *  (eg. java.util.List&lt;java.lang.Integer&gt;).
     * 
     *  @return    The generic class name
     */
    public String getGenClassName()
    {
        return getClassName();
    }
    
    /**
     * Get the generic name of the class of the object. The base names of types
     * are returned. (eg. List&lt;Integer&gt;).
     */
    public String getStrippedGenClassName()
    {
        return JavaNames.stripPrefix(getClassName());
    }
    
    /**
     *  Get the class of this object.
     *
     *  @return    The class object.
     */
    public DebuggerClass getClassRef()
    {
        if (obj == null)
            return null;
        else
            return new JdiClass(obj.referenceType());
    }

    /**
     *  Is an object of this class assignable to the given fully qualified type?
     *
     *@param  type  Description of Parameter
     *@return       The AssignableTo value
     */
/*    public boolean isAssignableTo(String type)
    {
        if (obj == null) {
            return false;
        }
        if (obj.referenceType() == null) {
            return false;
        }
        if (obj.referenceType().name() != null
                 && type.equals(obj.referenceType().name())) {
            return true;
        }
        if ((obj.referenceType() instanceof ClassType))
        {
            ClassType clst = ((ClassType) obj.referenceType());
            InterfaceType[] intt = ((InterfaceType[]) clst.allInterfaces().toArray(new InterfaceType[0]));
            for (int i = 0; i < intt.length; i++)
            {
                if (type.equals(intt[i].name()))
                {
                    return true;
                }
            }
            clst = clst.superclass();
            while (clst != null)
            {
                if (clst.name().equals(type))
                {
                    return true;
                }
                clst = clst.superclass();
            }
        }
        else if ((obj.referenceType() instanceof ArrayType))
        {
        }
        return false;
    }
*/
    
    /**
     *  Return true if this object is an array. This is always false, since
     *  arrays are wropped in the subclass "JdiArray".
     *
     *@return    The Array value
     */
    public boolean isArray()
    {
        return false;
    }

    public boolean isNullObject()
    {
        return obj == null;
    }

    /**
     *  Return the number of static fields (including inherited fields).
     *
     *@return    The StaticFieldCount value
     */
    public int getStaticFieldCount()
    {
        return getFieldCount(true);
    }

    /**
     *  Return the number of object fields.
     *
     *@return    The InstanceFieldCount value
     */
    public int getInstanceFieldCount()
    {
        return getFieldCount(false);
    }


    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public String getStaticFieldName(int slot)
    {
        return getField(true, slot).name();
    }

    /**
     *  Return the name of the object field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The InstanceFieldName value
     */
    public String getInstanceFieldName(int slot)
    {
        return getField(false, slot).name();
    }


    /**
     *  Return the object in static field 'slot'. Slot must exist and
     *  must be of object type.
     *
     *@param  slot  The slot number to be returned
     *@return       the object at slot
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        Field field = getField(true, slot);
        ObjectReference val = (ObjectReference) obj.getValue(field);
        return getDebuggerObject(val, field, this);
    }

    /**
     *  Return the object in object field 'slot'. Slot must exist and
     *  must be of object type.
     *
     *@param  slot  The slot number to be returned
     *@return       The InstanceFieldObject value
     */
    public DebuggerObject getInstanceFieldObject(int slot)
    {
        Field field = getField(false, slot);
        ObjectReference val = (ObjectReference) obj.getValue(field);
        return getDebuggerObject(val, field, this);
    }

    /**
     *  Return the object in field 'slot'. Slot must exist and
     *  must be of object type.
     *
     *@param  slot  The slot number to be returned
     *@return       The FieldObject value
     */
    public DebuggerObject getFieldObject(int slot)
    {
        Field field = (Field) fields.get(slot);
        ObjectReference val = (ObjectReference) obj.getValue(field);
        return getDebuggerObject(val, field, this);
    }

    public DebuggerObject getFieldObject(String name)
    {
        Field field = obj.referenceType().fieldByName(name);
        ObjectReference val = (ObjectReference) obj.getValue(field);
        return getDebuggerObject(val, field, this);
    }
    
    public String getFieldValueString(int slot) {
    	Field field = (Field) fields.get(slot);
        Value val = obj.getValue(field);
        return getValueString(val); 
    }
    
    public String getFieldValueTypeString(int slot) {
    	Field field = (Field) fields.get(slot);
        Value val = obj.getValue(field);
        
        return val.type().name();  
    }

    public ObjectReference getObjectReference()
    {
        return obj;
    }


    /**
     *  Return an array of strings with the description of each static field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The StaticFields value
     */
    public List getStaticFields(boolean includeModifiers)
    {
        return getFields(false, true, includeModifiers);
    }

    /**
     *  Return a list of strings with the description of each instance field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The InstanceFields value
     */
    public List getInstanceFields(boolean includeModifiers)
    {
        return getFields(false, false, includeModifiers);
    }


    /**
     *  Return a list of strings with the description of each field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The AllFields value
     */
    public List getAllFields(boolean includeModifiers)
    {
        return getFields(true, true, includeModifiers);
    }


    /**
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean staticFieldIsPublic(int slot)
    {
        return getField(true, slot).isPublic();
    }

    /**
     *  Return true if the object field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean instanceFieldIsPublic(int slot)
    {
        return getField(false, slot).isPublic();
    }


    /**
     *  Return true if the static field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean staticFieldIsObject(int slot)
    {
        return checkFieldForObject(true, slot);
    }

    /**
     *  Return true if the object field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean instanceFieldIsObject(int slot)
    {
        return checkFieldForObject(false, slot);
    }

    /**
     *  Return true if the object field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean fieldIsObject(int slot)
    {
        Field field = (Field) fields.get(slot);
        Value val = obj.getValue(field);
        return (val instanceof ObjectReference);
    }

    private int getFieldCount(boolean getStatic)
    {
        int count = 0;

        for (int i = 0; i < fields.size(); i++)
        {
            Field field = (Field) fields.get(i);

            if (checkIgnoreField(field))
                continue;

            if (field.isStatic() == getStatic)
            {
                count++;
            }
        }
        return count;
    }


    /**
     *  Return a list of strings with the description of each field
     *  in the format "<modifier> <type> <name> = <value>".
     *  If 'getAll' is true, both static and instance fields are returned
     *  ('getStatic' is ignored). If 'getAll' is false, then 'getStatic'
     *  determines whether static fields or instance fields are returned.
     *
     *@param  getAll            If true, get static and instance fields
     *@param  getStatic         If 'getAll' is false, determine which fields to get
     *@param  includeModifiers  If true, include the modifier name (public, private)
     *@return                   The Fields value
     */
    private List getFields(boolean getAll, boolean getStatic,
            boolean includeModifiers)
    {
        List fieldStrings = new ArrayList(fields.size());

        if (obj == null)
            return fieldStrings;
            
        ReferenceType cls = obj.referenceType();
        List visible = cls.visibleFields();

        for (int i = 0; i < fields.size(); i++) {
            Field field = (Field) fields.get(i);

            if (checkIgnoreField(field))
                continue;

            if (getAll || (field.isStatic() == getStatic)) {
                Value val = obj.getValue(field);

                String valString = getValueString(val);
                String fieldString = "";

                if (includeModifiers) {
                    if (field.isPrivate()) {
                        fieldString = "private ";
                    }
                    if (field.isProtected()) {
                        fieldString = "protected ";
                    }
                    if (field.isPublic()) {
                        fieldString = "public ";
                    }
                }

                if( jvmSupportsGenerics )
                    fieldString += JdiGenType.fromField(field,this).toString(true);
                else
                    fieldString += JavaNames.stripPrefix(field.typeName());

                if (!visible.contains(field)) {
                    fieldString += " (hidden)";
                }
                
                fieldString += " " + field.name() + " = " +valString;
                
                // the following code adds the word "inherited" to inherited
                // fields - currently unused
                //else if (!field.declaringType().equals(cls)) {
                //    fieldString += " (inherited)";
                //}
                fieldStrings.add(fieldString);
            }
        }
        return fieldStrings;
    }

    private Field getField(boolean getStatic, int slot)
    {
        for (int i = 0; i < fields.size(); i++) {
            Field field = (Field) fields.get(i);

            if (checkIgnoreField(field))
                continue;

            if (field.isStatic() == getStatic) {
                if (slot == 0) {
                    return field;
                }
                else {
                    slot--;
                }
            }
        }
        Debug.reportError("invalid slot in remote object");
        return null;
    }

    private boolean checkIgnoreField(Field f)
    {
        if (f.name().indexOf('$') >= 0)
            return true;
        else
            return false;
    }

    /**
     *  Get the list of fields for this object.
     */
    protected void getRemoteFields()
    {
        if (obj != null) {
        ReferenceType cls = obj.referenceType();

            if (cls != null) {
                fields = cls.allFields();
                return;
            }
        }
        // either null object or unavailable fields
        // lets give them an empty list of fields
        fields = new ArrayList();
    }

    private boolean checkFieldForObject(boolean getStatic, int slot)
    {
        Field field = getField(getStatic, slot);
        Value val = obj.getValue(field);
        return (val instanceof ObjectReference);
    }  // list of fields of the object

    /**
     *  Return the value of a field as as string.
     *
     *@param  val  Description of Parameter
     *@return      The ValueString value
     */
    public static String getValueString(Value val)
    {
        if (val == null)
        {
            return nullLabel;
        }
        else if (val instanceof StringReference)
        {
            return "\"" + ((StringReference) val).value() + "\"";
            // toString should be okay for this as well once the bug is out...
        }
        else if (val instanceof ObjectReference)
        {
            return OBJECT_REFERENCE;
        }

        // the following should not be necessary but it seems like
        // the 1.3 beta jpda has a bug in the toString() method.
        // revisit this code when 1.3 is released
        else if (val instanceof BooleanValue)
        {
            return String.valueOf(((BooleanValue) val).value());
        }
        else if (val instanceof ByteValue)
        {
            return String.valueOf(((ByteValue) val).value());
        }
        else if (val instanceof CharValue)
        {
            return String.valueOf(((CharValue) val).value());
        }
        else if (val instanceof DoubleValue)
        {
            return String.valueOf(((DoubleValue) val).value());
        }
        else if (val instanceof FloatValue)
        {
            return String.valueOf(((FloatValue) val).value());
        }
        else if (val instanceof IntegerValue)
        {
            return String.valueOf(((IntegerValue) val).value());
        }
        else if (val instanceof LongValue)
        {
            return String.valueOf(((LongValue) val).value());
        }
        else if (val instanceof ShortValue)
        {
            return String.valueOf(((ShortValue) val).value());
        }
        else
        {
            return val.toString();
        }
    }

    /**
     * Base our object equality on the object that we are referring
     * to in the remote VM.
     */
    public boolean equals(Object o)
    {
        if(this == o)
            return true;
        if((o == null) || (o.getClass() != this.getClass()))
            return false;

        // object must be JdiObject at this point
        JdiObject test = (JdiObject)o;
        return this.obj.equals(test.obj);
	}
		
    /**
     * Base our hashcode on the hashcode of the object that we are
     * referring to in the remote VM.
     */
	public int hashCode()
	{
        return obj.hashCode();
	}
}
