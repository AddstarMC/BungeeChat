package au.com.addstar.bc.sync;

public enum FieldType
{
	String(String.class, false, true),
	Long(Long.class, false, false),
	Integer(Integer.class, false, false),
	Short(Short.class, false, false),
	Byte(Byte.class, false, false),
	Double(Double.class, false, false),
	Float(Float.class, false, false),
	Boolean(Boolean.class, false, false),
	Char(Character.class, false, false),
	UUID(java.util.UUID.class, false, true),
	Object(Object.class, false, true),
	List(java.util.List.class, true, false),
	SyncConfig(SyncConfig.class, false, false);
	
	private Class<?> mClass;
	private boolean mHasSubType;
	private boolean mNullable;
	
	FieldType(Class<?> clazz, boolean hasSubType, boolean nullable)
	{
		mClass = clazz;
		mHasSubType = hasSubType;
		mNullable = nullable;
	}
	
	public boolean isCorrectType(Object object)
	{
		if(object == null)
			return mNullable;
		
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
