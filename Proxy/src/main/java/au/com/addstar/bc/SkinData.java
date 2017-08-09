package au.com.addstar.bc;

import java.util.UUID;

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SkinData
{
	public final String name;
	public final UUID id;
	public final long timestamp;
	
	public final String skinURL;
	public final String capeURL;
	
	public final String value;
	public final String signature;
	
	public SkinData(String data, String signature)
	{
		String json = Base64Coder.decodeString(data);
		JsonObject root = new JsonParser().parse(json).getAsJsonObject();
		
		name = root.get("profileName").getAsString();
		String rawId = root.get("profileId").getAsString();
		id = UUID.fromString(String.format("%s-%s-%s-%s-%s", rawId.substring(0, 8), rawId.substring(8, 12), rawId.substring(12, 16), rawId.substring(16, 20), rawId.substring(20)));
		timestamp = root.get("timestamp").getAsLong();
		
		JsonObject textures = root.getAsJsonObject("textures");
		
		if (textures.has("SKIN"))
			skinURL = textures.getAsJsonObject("SKIN").get("url").getAsString();
		else
			skinURL = null;
		
		if (textures.has("CAPE"))
			capeURL = textures.getAsJsonObject("CAPE").get("url").getAsString();
		else
			capeURL = null;
		
		value = data;
		this.signature = signature;
	}
}
