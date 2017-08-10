package au.com.addstar.bc.config;

import net.cubespace.Yamler.Config.YamlConfig;

public class RedisSettings extends YamlConfig
{
	public String host = "localhost";
	public int port = 6379;
	public String password = "";
}
