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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyncConfig
{
	private Map<String, Object> mData;
	
	public SyncConfig()
	{
		mData = new HashMap<>();
	}
	
	private SyncConfig(Map<String, Object> data)
	{
		mData = data;
	}
	
	public Object get(String path, Object def)
	{
		if(path.isEmpty())
			return this;
		
		SyncConfig section = this;
		char sep = '.';
		
		int higher = -1, lower;
        while ((higher = path.indexOf(sep, lower = higher + 1)) != -1) 
        {
            section = section.getSection(path.substring(lower, higher));
            if (section == null)
                return def;
        }

        String key = path.substring(lower);
        if (section == this)
        {
            Object result = mData.get(key);
            return (result == null) ? def : result;
        }
		
		return section.get(key, def);
	}
	
	public void set(String path, Object value)
	{
		SyncConfig section = this;
		char sep = '.';
		
		int higher = -1, lower;
        while ((higher = path.indexOf(sep, lower = higher + 1)) != -1) 
        {
            String node = path.substring(lower, higher);
            SyncConfig subSection = section.getSection(node);
            if (subSection == null)
                section = section.createSection(node);
            else
                section = subSection;
        }

        String key = path.substring(lower);
        if (section == this) 
        {
            if (value == null)
                mData.remove(key);
            else
                mData.put(key, value);
        } 
        else
        	section.set(key, value);
	}
	
	public Set<String> getKeys()
	{
        return mData.keySet();
	}
	
	public String getString(String key, String def)
	{
		Object value = get(key, def);
		if(value == null)
			return def;
		
		return String.valueOf(value);
	}
	
	public byte getByte(String key, byte def)
	{
		Object o = get(key, def);
		if(o == null)
			return def;
		
		if(o instanceof Number)
			return ((Number) o).byteValue();
		
		return def;
	}
	
	public short getShort(String key, short def)
	{
		Object o = get(key, def);
		if(o == null)
			return def;
		
		if(o instanceof Number)
			return ((Number) o).shortValue();
		
		return def;
	}
	
	public int getInt(String key, int def)
	{
		Object o = get(key, def);
		if(o == null)
			return def;
		
		if(o instanceof Number)
			return ((Number) o).intValue();
		
		return def;
	}
	
	public long getLong(String key, long def)
	{
		Object o = get(key, def);
		if(o == null)
			return def;
		
		if(o instanceof Number)
			return ((Number) o).longValue();
		
		return def;
	}
	
	public float getFloat(String key, float def)
	{
		Object o = get(key, def);
		if(o == null)
			return def;
		
		if(o instanceof Number)
			return ((Number) o).floatValue();
		
		return def;
	}
	
	public double getDouble(String key, double def)
	{
		Object o = get(key, def);
		if(o == null)
			return def;
		
		if(o instanceof Number)
			return ((Number) o).doubleValue();
		
		return def;
	}
	
	public boolean getBoolean(String key, boolean def)
	{
		Object o = get(key, def);
		if(o == null)
			return def;
		
		if(o instanceof Number)
			return ((Number) o).byteValue() != 0;
		if(o instanceof Boolean)
			return (Boolean)o;
		
		return def;
	}
	
	public List<?> getList(String key, List<?> def)
	{
		Object o = get(key, def);
		if(o == null)
			return def;
		
		if(o instanceof List<?>)
			return (List<?>)o;
		
		return def;
	}
	
	public SyncConfig getSection(String key)
	{
		Object o = get(key, null);
		if(o == null)
			return null;
		
		if(o instanceof SyncConfig)
			return (SyncConfig)o;
		else if(o instanceof Map<?, ?>)
		{
			@SuppressWarnings( "unchecked" )
			SyncConfig config = new SyncConfig((Map<String, Object>)o);
			set(key,config);
			return config;
		}
		
		return null;
	}
	
	public SyncConfig createSection(String path)
	{
		char sep = '.';
		SyncConfig section = this;
		
        int higher = -1, lower;
        while ((higher = path.indexOf(sep, lower = higher + 1)) != -1) 
        {
            String node = path.substring(lower, higher);
            SyncConfig subSection = section.getSection(node);
            if (subSection == null)
                section = section.createSection(node);
            else
                section = subSection;
        }

        String key = path.substring(lower);
        if (section == this) 
        {
        	SyncConfig result = new SyncConfig();
            mData.put(key, result);
            return result;
        }
        
        return section.createSection(key);
	}
	
	public void load(DataInput input) throws IOException
	{
		mData = SyncUtil.readMap(input);
	}
	
	public void write(DataOutput output) throws IOException
	{
		SyncUtil.writeMap(output, mData);
	}
	
	Map<String, Object> getInternalMap()
	{
		return mData;
	}
}
