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
public class ChatChannel implements SyncSerializable {

    public String command;
    public String format;
    public String permission;
    public String listenPermission;
    private boolean subscribe;
    private boolean isRp;

    public ChatChannel() {
        subscribe = false;
        isRp = false;
    }

    public ChatChannel(String cmd, String prefix, String perm, String listenPerm) {
        this(cmd, prefix, perm, listenPerm, false, false);
    }

    public ChatChannel(String cmd, String prefix, String perm, String listenPerm, boolean sub, boolean rp) {
        this.command = cmd;
        this.format = prefix;
        this.permission = perm;
        this.listenPermission = listenPerm;
        this.subscribe = sub;
        this.isRp = rp;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("cmd", command);
        map.put("fmt", format);
        map.put("perm", permission);
        map.put("lperm", listenPermission);
        map.put("sub", subscribe);
        map.put("rp", isRp);
        return map;
    }
}
