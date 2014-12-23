package au.com.addstar.bc;

import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.SucceededFuture;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.GameProfile.Property;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class SkinLibrary
{
	private Map<UUID, SkinData> mSkins;
	
	public SkinLibrary()
	{
		mSkins = Collections.synchronizedMap(Maps.<UUID, SkinData>newHashMap());
	}
	
	public SkinData getSkin(ProxiedPlayer player)
	{
		SkinData skin = mSkins.get(player.getUniqueId());
		if (skin == null)
		{
			for (Property prop : player.getProfile().getProperties())
			{
				if (prop.getName().equals("textures"))
				{
					skin = new SkinData(prop.getValue(), prop.getSignature());
					mSkins.put(player.getUniqueId(), skin);
					break;
				}
			}
		}
		
		return skin;
	}
	
	public SkinData getSkin(UUID id)
	{
		SkinData skin = mSkins.get(id);
		if (skin == null)
		{
			ProxiedPlayer player = ProxyServer.getInstance().getPlayer(id);
			if (player != null)
			{
				for (Property prop : player.getProfile().getProperties())
				{
					if (prop.getName().equals("textures"))
					{
						skin = new SkinData(prop.getValue(), prop.getSignature());
						mSkins.put(player.getUniqueId(), skin);
						break;
					}
				}
			}
		}
		
		return skin;
	}
	
	public Future<SkinData> getSkinWithLookup(final UUID id)
	{
		SkinData data = getSkin(id);
		if (data != null)
			return new SucceededFuture<SkinData>(null, data);
		
		Callable<SkinData> lookup = new Callable<SkinData>()
		{
			@Override
			public SkinData call() throws Exception
			{
				URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + id.toString().replace("-", "") + "?unsigned=false");
				
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				try
				{
					con.setConnectTimeout(30000);
					con.connect();
				}
				catch(FileNotFoundException e)
				{
					// User does not exist
					return null;
				}
				
				if (con.getResponseCode() != HttpURLConnection.HTTP_OK)
					return null;
				
				Reader reader = null;
				try
				{
					reader = new InputStreamReader(con.getInputStream(), CharsetUtil.UTF_8);
					
					// Parse the response
					JsonParser parser = new JsonParser();
					JsonObject root = parser.parse(reader).getAsJsonObject();
					
					JsonArray properties = root.getAsJsonArray("properties");
					for (JsonElement propertyRaw : properties)
					{
						JsonObject property = propertyRaw.getAsJsonObject();
						
						if (property.get("name").getAsString().equals("textures"))
						{
							SkinData skin = new SkinData(property.get("value").getAsString(), property.get("signature").getAsString());
							mSkins.put(skin.id, skin);
							return skin;
						}
					}
				}
				finally
				{
					Closeables.closeQuietly(reader);
				}
				
				// Somehow the user doesnt have a skin
				return null;
			}
		};
		
		return ProxyServer.getInstance().getScheduler().unsafe().getExecutorService(BungeeChat.instance).submit(lookup);
	}
	
	public SkinData getSkinWithLookupSync(UUID id)
	{
		Future<SkinData> data = getSkinWithLookup(id);
		try
		{
			return data.get();
		}
		catch(ExecutionException e)
		{
			e.getCause().printStackTrace();
		}
		catch(InterruptedException e)
		{
		}
		
		return null;
	}
	
}
