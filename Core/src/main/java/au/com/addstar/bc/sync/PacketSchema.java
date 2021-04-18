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

/*-
 * #%L
 * BungeeChat-Core
 * %%
 * Copyright (C) 2015 - 2020 AddstarMC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

public class PacketSchema
{
	private ArrayList<FieldDefinition> mFields;
	private HashMap<String, Integer> mFieldMap;
	
	private PacketSchema()
	{
		mFields = new ArrayList<>();
		mFieldMap = new HashMap<>();
	}
	
	private void addField(String name, FieldType type, FieldType subType, boolean required)
	{
		mFields.add(new FieldDefinition(name, type, subType, required));
		mFieldMap.put(name, mFields.size()-1);
	}
	
	/**
	 * Encodes a list of data into a byte stream using this schema
	 */
	public void encode(Object[] data, DataOutput output) throws IOException
	{
		if(data.length != mFields.size())
			throw new IllegalArgumentException("Input data does not match schema");
		
		for(int i = 0; i < mFields.size(); ++i)
		{
			FieldDefinition field = mFields.get(i);
			Object object = data[i];

			if(!write(field.type, field.subType, object, output))
				throw new IllegalArgumentException("Type " + field.type + " did not match on field " + field.name);
		}
	}
	
	/**
	 * Decodes a byte stream into a list of data using this schema
	 */
	public Object[] decode(DataInput input) throws IOException
	{
		Object[] data = new Object[mFields.size()];
		
		for(int i = 0; i < mFields.size(); ++i)
		{
			FieldDefinition field = mFields.get(i);
			data[i] = read(field.type, field.subType, input);
		}
		return data;
	}
	
	/**
	 * Translates from one schema to another.
	 * @param data The source data
	 * @param schema The schema of the source data
	 * @return A list matching this schemas format
	 * @throws IllegalArgumentException Thrown if it is impossible to translate (missing fields, incompatible types, etc)
	 */
	public Object[] translate(Object[] data, PacketSchema schema) throws IllegalArgumentException
	{
		if(schema.equals(this))
			return data;
		
		Object[] translated = new Object[mFields.size()];
		for(int i = 0; i < mFields.size(); ++i)
		{
			FieldDefinition field = mFields.get(i);
			
			Integer theirId = schema.mFieldMap.get(field.name);
			if(theirId == null)
			{
				if(field.required)
					throw new IllegalArgumentException("Missing field " + field.name);
				continue;
			}
			
			FieldDefinition theirField = schema.mFields.get(theirId);
			
			if(field.type != theirField.type)
				throw new IllegalArgumentException("Incompatible field " + field.name);
			
			translated[i] = data[theirId];
		}
		
		return translated;
	}
	
	private void writeString(String string, DataOutput output) throws IOException
	{
		if(string == null)
			output.writeShort(-1);
		else
		{
			byte[] data = string.getBytes("UTF-8");
			output.writeShort(data.length);
			output.write(data);
		}
	}
	
	private String readString(DataInput input) throws IOException
	{
		short count = input.readShort();
		if(count == -1)
			return null;
		
		byte[] data = new byte[count];
		input.readFully(data);
		return new String(data, "UTF-8");
	}

	private void writeComponent(Component input, DataOutput output) throws IOException
	{
		if(input == null)
			output.writeShort(-1);
		else
		{
			String string = GsonComponentSerializer.gson().serialize(input);
			byte[] data = string.getBytes("UTF-8");
			output.writeShort(data.length);
			output.write(data);
		}
	}

	private Component readComponent(DataInput input) throws IOException
	{
		short count = input.readShort();
		if(count == -1)
			return null;

		byte[] data = new byte[count];
		input.readFully(data);
		return GsonComponentSerializer.gson().deserialize(new String(data, "UTF-8"));
	}

	private boolean write(FieldType type, FieldType subType, Object object, DataOutput output) throws IOException
	{
		if(!type.isCorrectType(object)) {
			System.out.println("write incorrect type: " + type + " (object " + object.getClass().getName() + ")");
			return false;
		}
		
		switch(type)
		{
		case Boolean:
			output.writeBoolean((Boolean)object);
			break;
		case Byte:
			output.writeByte((Byte)object);
			break;
		case Char:
			output.writeChar((Character)object);
			break;
		case Double:
			output.writeDouble((Double)object);
			break;
		case Float:
			output.writeFloat((Float)object);
			break;
		case Integer:
			output.writeInt((Integer)object);
			break;
		case Long:
			output.writeLong((Long)object);
			break;
		case Short:
			output.writeShort((Short)object);
			break;
		case String:
			writeString((String)object, output);
			break;
		case UUID:
			if(object == null)
				output.writeShort(-1);
			else
				writeString(object.toString(), output);
			break;
		case Object:
			SyncUtil.writeObject(output, object);
			break;
		case SyncConfig:
			((SyncConfig)object).write(output);
			break;
		case List:
			writeList((List<?>)object, subType, output);
			break;
		case Component:
			writeComponent((Component)object, output);
			break;
		default:
			throw new AssertionError("Invalid type \"" + type.name() + "\"");
		}
		
		return true;
	}
	
	private void writeList(List<?> list, FieldType subType, DataOutput output) throws IOException
	{
		output.writeShort(list.size());
		for(Object object : list)
			write(subType, null, object, output);
	}
	
	private Object read(FieldType type, FieldType subType, DataInput input) throws IOException
	{
		switch(type)
		{
		case Boolean:
			return input.readBoolean();
		case Byte:
			return input.readByte();
		case Char:
			return input.readChar();
		case Double:
			return input.readDouble();
		case Float:
			return input.readFloat();
		case Integer:
			return input.readInt();
		case Long:
			return input.readLong();
		case Short:
			return input.readShort();
		case String:
			return readString(input);
		case UUID:
		{
			String str = readString(input);
			if(str == null)
				return null;
			return UUID.fromString(str);
		}
		case Object:
			return SyncUtil.readObject(input);
		case SyncConfig:
		{
			SyncConfig config = new SyncConfig();
			config.load(input);
			return config;
		}
		case List:
			return readList(subType, input);
		case Component:
			return readComponent(input);
		default:
			System.out.println("Type: " + type.name());
			System.out.println("class: " + type.getClass());
			System.out.println("Declaring class: " + type.getDeclaringClass());
			throw new AssertionError("Invalid type \"" + type.name() + "\"");
		}
	}
	
	private List<?> readList(FieldType subType, DataInput input) throws IOException
	{
		int count = input.readUnsignedShort();
		ArrayList<Object> list = new ArrayList<>(count);
		for(int i = 0; i < count; ++i)
			list.add(read(subType, null, input));
		
		return list;
	}
	
	public boolean isCompatible( PacketSchema readSchema )
	{
		// It is compatible if the readSchema has every required field in this schema and vice versa
		for(int i = 0; i < mFields.size(); ++i)
		{
			FieldDefinition field = mFields.get(i);
			
			Integer theirId = readSchema.mFieldMap.get(field.name);
			if(theirId == null)
			{
				if(field.required)
				{
					System.out.println("field " + field.name + " is missing");
					return false;
				}
				continue;
			}
			
			FieldDefinition theirField = readSchema.mFields.get(theirId);
			
			if(!field.type.equals(theirField.type))
			{
				System.out.println("field " + field.name + " type mismatch " + field.type + " to " + theirField.type);
				return false;
			}
		}
		
		for(int i = 0; i < readSchema.mFields.size(); ++i)
		{
			FieldDefinition field = readSchema.mFields.get(i);
			
			Integer theirId = mFieldMap.get(field.name);
			if(theirId == null)
			{
				if(field.required)
				{
					System.out.println("myfield " + field.name + " is missing");
					return false;
				}
				continue;
			}
			
			FieldDefinition theirField = mFields.get(theirId);
			
			if(!field.type.equals(theirField.type))
			{
				System.out.println("myfield " + field.name + " type mismatch");
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if(!(obj instanceof PacketSchema))
			return false;
		
		PacketSchema other = (PacketSchema)obj;
		return other.mFields.equals(mFields);
	}
	
	@Override
	public int hashCode()
	{
		return mFields.hashCode();
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		for(FieldDefinition field : mFields)
		{
			if(builder.length() != 0)
				builder.append(",");
			
			builder.append(field.name);
			if(field.required)
				builder.append("=");
			else
				builder.append(":=");
			
			builder.append(field.type.name());
			if(field.subType != null)
			{
				builder.append("<");
				builder.append(field.subType.name());
				builder.append(">");
			}
		}
		
		return builder.toString();
	}
	
	private static Pattern mPattern = Pattern.compile("([a-zA-Z_0-9\\-]+)(\\:?=)([a-zA-Z_]+)(?:\\<([a-zA-Z_]+)\\>)?");
	/**
	 * Creates a schema from the string.
	 * @param definition The definition to use. Format is 'name:=type,name=type,name=type&lt;subtype&gt;...' = Is a required field, := is a field that can be omitted during translation
	 * @return A schema
	 */
	public static PacketSchema from(String definition) throws IllegalArgumentException
	{
		String[] fields = definition.split(",");
		
		PacketSchema schema = new PacketSchema();
		if(definition.isEmpty())
			return schema;
		
		for(String field : fields)
		{
			Matcher matcher = mPattern.matcher(field);
			if(!matcher.matches())
				throw new IllegalArgumentException("Syntax error in field definition '" + field + "'. Must be in the format of: name=type.");
			
			String name = matcher.group(1);
			boolean required = (matcher.group(2).equals("="));
			FieldType type = FieldType.getByName(matcher.group(3));
			FieldType subType = null;
			
			if(type == null)
				throw new IllegalArgumentException("Type " + matcher.group(3) + " in " + field + " is not a valid type for use here.");
			
			if(matcher.group(4) != null)
			{
				if(!type.hasSubType())
					throw new IllegalArgumentException("Type " + type.name() + " does not take a subtype. " + field);
				subType = FieldType.getByName(matcher.group(4));
				if(subType == null)
					throw new IllegalArgumentException("Unknown type " + matcher.group(4) + ". Subtype of " + type.name() + " in " + field);
				if(subType.hasSubType())
					throw new IllegalArgumentException("Type " + subType.name() + " requires a subtype but is used as one. in" + field);
			}
			
			schema.addField(name, type, subType, required);
		}
		
		return schema;
	}
	
	private static class FieldDefinition
	{
		public String name;
		public FieldType type;
		public FieldType subType;
		public boolean required;
		
		public FieldDefinition(String name, FieldType type, FieldType subType, boolean required)
		{
			this.name = name;
			this.type = type;
			this.subType = subType;
			this.required = required;
		}
		
		@Override
		public boolean equals( Object obj )
		{
			if(!(obj instanceof FieldDefinition))
				return false;
			
			FieldDefinition other = (FieldDefinition)obj;
			return name.equals(other.name) && type == other.type && subType == other.subType && required == other.required;
		}
	}
}
