package au.com.addstar.bc.sync;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

public class SyncUtil
{
	private static HashMap<Class<?>, String> mClassMappings = new HashMap<>();
	private static HashMap<String, Class<?>> mClassMappingsInverse = new HashMap<>();
	
	public static void addSerializer(Class<? extends SyncSerializable> clazz, String typename)
	{
		mClassMappings.put(clazz, typename);
		mClassMappingsInverse.put(typename, clazz);
	}
	
	public static void writeObject(DataOutput output, Object value) throws IOException
	{
		if(value == null)
		{
			output.writeByte(-1);
		}
		else if(value instanceof Byte)
		{
			output.writeByte(0);
			output.writeByte((Byte)value);
		}
		else if(value instanceof Short)
		{
			output.writeByte(1);
			output.writeShort((Short)value);
		}
		else if(value instanceof Integer)
		{
			output.writeByte(2);
			output.writeInt((Integer)value);
		}
		else if(value instanceof Long)
		{
			output.writeByte(3);
			output.writeLong((Long)value);
		}
		else if(value instanceof Float)
		{
			output.writeByte(4);
			output.writeFloat((Float)value);
		}
		else if(value instanceof Double)
		{
			output.writeByte(5);
			output.writeDouble((Double)value);
		}
		else if(value instanceof Boolean)
		{
			output.writeByte(6);
			output.writeBoolean((Boolean)value);
		}
		else if(value instanceof String)
		{
			output.writeByte(7);
			output.writeUTF((String)value);
		}
		else if(value instanceof Character)
		{
			output.writeByte(8);
			output.writeChar((Character)value);
		}
		else if(value instanceof List<?>)
		{
			output.writeByte(9);
			List<?> list = (List<?>)value;
			output.writeShort(list.size());
			for(Object obj : list)
				writeObject(output, obj);
		}
		else if(value instanceof Map<?, ?>)
		{
			output.writeByte(10);
			writeMap(output, (Map<String, Object>)value);
		}
		else if(value instanceof SyncConfig)
		{
			output.writeByte(10);
			writeMap(output, ((SyncConfig)value).getInternalMap());
		}
		else if(value instanceof SyncSerializable)
		{
			output.writeByte(11);
			writeSerializable(output, (SyncSerializable)value);
		}
		else if(value instanceof UUID)
		{
			output.writeByte(12);
			output.writeUTF(value.toString());
		}
		else
			throw new IllegalArgumentException("Unable to use type " + value.getClass().getName() + ". Make it a SyncSerializable if you want to use it directly.");
	}
	
	public static void writeMap(DataOutput output, Map<String, Object> data) throws IOException
	{
		output.writeShort(data.size());
		for(Entry<String, Object> entry : data.entrySet())
		{
			output.writeUTF(entry.getKey());
			writeObject(output, entry.getValue());
		}
	}
	
	public static void writeSerializable(DataOutput output, SyncSerializable serializable) throws IOException
	{
		String typeName = mClassMappings.get(serializable.getClass());
		if(typeName == null)
			typeName = serializable.getClass().getName();
		
		output.writeUTF(typeName);
		writeMap(output, serializable.toMap());
	}
	
	public static Object readObject(DataInput input) throws IOException
	{
		byte type = input.readByte();
		switch(type)
		{
		case -1:
			return null;
		case 0:
			return input.readByte();
		case 1:
			return input.readShort();
		case 2:
			return input.readInt();
		case 3:
			return input.readLong();
		case 4:
			return input.readFloat();
		case 5:
			return input.readDouble();
		case 6:
			return input.readBoolean();
		case 7:
			return input.readUTF();
		case 8:
			return input.readChar();
		case 9:
		{
			int count = input.readShort();
			ArrayList<Object> list = new ArrayList<>(count);
			for(int i = 0; i < count; ++i)
				list.add(readObject(input));
			
			return list;
		}
		case 10:
			return readMap(input);
		case 11:
			return readSerializable(input);
		case 12:
			return UUID.fromString(input.readUTF());
		default:
			throw new AssertionError("Encountered unknown type " + type + " when reading object");
		}
	}
	
	public static HashMap<String, Object> readMap(DataInput input) throws IOException
	{
		int count = input.readShort();
		HashMap<String, Object> map = new HashMap<>(count);
		
		for(int i = 0; i < count; ++i)
			map.put(input.readUTF(), readObject(input));
		
		return map;
	}
	
	public static SyncSerializable readSerializable(DataInput input) throws IOException
	{
		String className = input.readUTF();
		HashMap<String, Object> map = readMap(input);
		
		Class<?> clazz = mClassMappingsInverse.get(className);
		
		try
		{
			if(clazz == null)
				clazz = Class.forName(className);
			
			Method method = clazz.getMethod("fromMap", Map.class);
			return (SyncSerializable)method.invoke(null, map);
		}
		catch(ClassNotFoundException e)
		{
			System.out.println("WARNING: Unable to find class '" + className + "' required for deserialization");
		}
		catch ( NoSuchMethodException e )
		{
			System.out.println("WARNING: Unable to find fromMap method in class '" + className + "' required for deserialization");
		}
		catch ( SecurityException | IllegalAccessException e )
		{
			throw new RuntimeException(e);
		} catch ( InvocationTargetException e )
		{
			if(e.getCause() instanceof RuntimeException)
				throw (RuntimeException)e.getCause();
			
			throw new RuntimeException(e.getCause());
		}
		
		return null;
	}
}
