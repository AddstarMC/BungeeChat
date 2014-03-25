package au.com.addstar.bc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.md_5.bungee.api.ChatColor;

import org.yaml.snakeyaml.Yaml;

public class Config
{
	private HashMap<String, String> mFormats = new HashMap<String, String>();
	private String mDefaultFormat = "<{DISPLAYNAME}> {MESSAGE}";
	
	private File mFile;
	private Map<String, Object> mRootMap;
	
	public Config(File file)
	{
		mFile = file;
	}

	@SuppressWarnings( "unchecked" )
	public void load() throws IOException
	{
		Yaml yaml = new Yaml();
		FileInputStream input = new FileInputStream(mFile);
		
		try
		{
			Object obj = yaml.load(input);
			
			if(!(obj instanceof Map<?, ?>))
				throw new IOException("Config is not a valid format");
			
			mRootMap = (Map<String, Object>)obj;
		}
		finally
		{
			input.close();
		}
		
		mDefaultFormat = ChatColor.translateAlternateColorCodes('&', getString("format", "<{DISPLAYNAME}> {MESSAGE}"));
		mFormats = new HashMap<String, String>();
		
		if(isSection("groupFormat"))
		{
			for(String key : getKeys("groupFormat"))
			{
				String format = ChatColor.translateAlternateColorCodes('&', getString("groupFormat." + key, "<{DISPLAYNAME}> {MESSAGE}"));
				mFormats.put(key, format);
			}
		}
	}
	
	public String getDefaultFormat()
	{
		return mDefaultFormat;
	}
	
	public Map<String, String> getGroupFormats()
	{
		return mFormats;
	}
	
	@SuppressWarnings( "unchecked" )
	private Map<String, Object> getMap(String path)
	{
		String[] parts = path.split("\\.");
		Map<String, Object> map = mRootMap;
		
		if(path.isEmpty())
			return map;
		
		for(int i = 0; i < parts.length; ++i)
		{
			Object sub = map.get(parts[0]);
			if(!(sub instanceof Map<?,?>))
				return Collections.emptyMap();
			
			map = (Map<String, Object>)sub;
		}
		
		return map;
	}
	
	private String getParentPath(String path)
	{
		if(path.indexOf('.') != -1)
			return path.substring(0, path.lastIndexOf('.'));
		else
			return "";
	}
	
	private String getKeyPart(String path)
	{
		if(path.indexOf('.') != -1)
			return path.substring(path.lastIndexOf('.')+1);
		else
			return path;
	}
	
	private String getString(String path, String def)
	{
		Map<String, Object> map = getMap(getParentPath(path));
		String key = getKeyPart(path);
		
		if(!map.containsKey(key))
			return def;
		else
			return String.valueOf(map.get(key));
	}
	
	private Set<String> getKeys(String path)
	{
		Map<String, Object> map = getMap(path);
		
		return map.keySet();
	}
	
	private boolean isSection(String path)
	{
		Map<String, Object> map = getMap(getParentPath(path));
		String key = getKeyPart(path);
		
		if(map.containsKey(key) && map.get(key) instanceof Map<?, ?>)
			return true;
		
		return false;
	}
}
