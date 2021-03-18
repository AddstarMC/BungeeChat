package au.com.addstar.bc.config;

import au.com.addstar.bc.sync.SyncSerializable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;


import java.util.HashMap;
import java.util.Map;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 4/06/2019.
 */
@ConfigSerializable
public class PermissionSetting implements SyncSerializable
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

    @Override
    public Map<String, Object> toMap()
    {
        HashMap<String, Object> map = new HashMap<>();
        map.put("fmt", format);
        map.put("c", color);
        map.put("pri", priority);
        map.put("perm", permission);

        return map;
    }
}
