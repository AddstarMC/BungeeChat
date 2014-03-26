package au.com.addstar.bc;

public class PermissionSetting extends net.cubespace.Yamler.Config.Config
{
	public PermissionSetting() {}
	public PermissionSetting(String format, String color, int priority)
	{
		this.format = format;
		this.color = color;
		this.priority = priority;
	}
	
	public String format;
	public String color;
	public int priority;
	public String permission;
	
	@Override
	public String toString()
	{
		return "Permission Setting: " + String.format("perm:%s pri:%d col:%s fmt:%s", permission, priority, color, format);
	}
}
