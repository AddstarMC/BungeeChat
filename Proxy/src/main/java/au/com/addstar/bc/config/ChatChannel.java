package au.com.addstar.bc.config;

import java.util.HashMap;
import java.util.Map;

import au.com.addstar.bc.sync.SyncSerializable;
import net.cubespace.Yamler.Config.YamlConfig;

public class ChatChannel extends YamlConfig implements SyncSerializable {
    public String command;
    public String format;
    public String permission;
    public String listenPermission;
    private Boolean subscribe;
    private Boolean isRp;

    public ChatChannel() {
        subscribe = false;
        isRp = false;
    }

    public ChatChannel(String cmd, String prefix, String perm, String listenPerm) {
        this(cmd, prefix, perm, listenPerm, false, false);
    }

    public ChatChannel(String cmd, String prefix, String perm, String listenPerm, Boolean sub, Boolean rp) {
        this.command = cmd;
        this.format = prefix;
        this.permission = perm;
        this.listenPermission = listenPerm;
        this.subscribe = sub != null && sub;
        this.isRp = rp != null && rp;
    }

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        checkConfig();
        map.put("cmd", command);
        map.put("fmt", format);
        map.put("perm", permission);
        map.put("lperm", listenPermission);
        map.put("sub", subscribe);
        map.put("rp", isRp);

        return map;
    }

    private void checkConfig() {
        if (subscribe == null) {
            subscribe = false;
        }
        if (isRp == null) {
            isRp = false;
        }
    }
}