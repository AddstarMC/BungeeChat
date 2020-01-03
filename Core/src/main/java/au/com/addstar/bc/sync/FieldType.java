/*
 * BungeeChat
 *
 * Copyright (c) 2015 - 2020.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy   of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is *
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
