package au.com.addstar.bc;

public class ChatChannel extends net.cubespace.Yamler.Config.Config
{
	public String command;
	public String format;
	public String permission;
	public String listenPermission;
	
	public ChatChannel() {}
	public ChatChannel(String cmd, String prefix, String perm, String listenPerm)
	{
		this.command = cmd;
		this.format = prefix;
		this.permission = perm;
		this.listenPermission = listenPerm;
	}
}
