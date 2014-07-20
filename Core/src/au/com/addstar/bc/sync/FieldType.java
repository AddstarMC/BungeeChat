package au.com.addstar.bc.sync;

public enum FieldType
{
	String(String.class, false),
	Long(Long.class, false),
	Integer(Integer.class, false),
	Short(Short.class, false),
	Byte(Byte.class, false),
	Double(Double.class, false),
	Float(Float.class, false),
	Boolean(Boolean.class, false),
	Char(Character.class, false),
	UUID(java.util.UUID.class, false),
	Object(Object.class, false),
	List(java.util.List.class, true),
	SyncConfig(SyncConfig.class, false);
	
	private Class<?> mClass;
	private boolean mHasSubType;
	
	private FieldType(Class<?> clazz, boolean hasSubType)
	{
		mClass = clazz;
		mHasSubType = hasSubType;
	}
	
	public boolean isCorrectType(Object object)
	{
		if(object == null)
			return false;
		
		return mClass.isInstance(object);
	}
	
	public boolean hasSubType()
	{
		return mHasSubType;
	}
	
	public static FieldType getByName(String name)
	{
		for(FieldType type : values())
		{
			if(type.name().equalsIgnoreCase(name))
				return type;
		}
		
		return null;
	}
}
