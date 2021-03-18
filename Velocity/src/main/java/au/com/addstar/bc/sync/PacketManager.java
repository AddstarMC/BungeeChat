package au.com.addstar.bc.sync;

import com.google.common.collect.HashMultimap;

import au.com.addstar.bc.ProxyChat;
import au.com.addstar.bc.Debugger;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;


import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.AbstractMap.SimpleEntry;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
public class PacketManager implements IDataReceiver, ServerComLink.ConnectionStateNotify
{
    private HashMap<RegisteredServer, PacketCodec> mCodecs;
    private HashMultimap<Class<? extends Packet>, IPacketHandler> mHandlers;
    private ProxyComLink mComLink;
    private ProxyChat plugin;

    // Packets that arrived before the schema did
    private LinkedList<SimpleEntry<ServerInfo, DataInput>> mPendingPackets;

    public PacketManager(ProxyChat plugin)
    {
        this.plugin = plugin;
        mCodecs = new HashMap<>();
        mHandlers = HashMultimap.create();
        mPendingPackets = new LinkedList<>();
        mComLink = this.plugin.getComLink();
        mComLink.listenToChannel("BungeeChat", this);
        mComLink.listenToChannel("BCState", this);
        this.plugin.getComLink().setNotifyHandle(this);
    }

    public void initialize()
    {
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(ostream);
        try
        {
            PacketRegistry.writeSchemaPacket(out);
        }
        catch(IOException e)
        {
            // Cant happen
        }

        this.plugin.getComLink().broadcastMessage("BCState", ostream.toByteArray());
    }


    public void addHandler(IPacketHandler handler, Class<? extends Packet>... packets)
    {
        if(packets == null)
            mHandlers.put(null, handler);
        else
        {
            for(Class<? extends Packet> clazz : packets)
                mHandlers.put(clazz, handler);
        }
    }

    public void send(Packet packet, RegisteredServer server)//should this be serverinfo

    {
        Debugger.logp("Sending to %s: %s", server.getServerInfo().getName(), packet);
        MessageSender sender = new MessageSender() {
            @Override
            public int getId() {
                return server.getServerInfo().getAddress().getPort();
            }

            @Override
            public String getName() {
                return server.getServerInfo().getName();
            }
        };
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try
        {
            PacketRegistry.write(packet, out);
            this.plugin.getComLink().sendMessage("BungeeChat", stream.toByteArray(), sender);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public void broadcast(Packet packet)
    {
        Debugger.logp("Broadcast: %s", packet);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try
        {
            PacketRegistry.write(packet, out);
            this.plugin.getComLink().broadcastMessage("BungeeChat", stream.toByteArray());
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    @Deprecated
    public void broadcastNoQueue( Packet packet )
    {
        broadcast(packet);
    }

    private void handleDataPacket(RegisteredServer server, PacketCodec codec, DataInput in)
    {
        Packet packet = null;
        try
        {
            packet = codec.read(in);
            if(packet == null)
            {
                Debugger.logp("Received packet but decoded to null. %s", server.getServerInfo().getName());
                return;
            }

            Debugger.logp("Received packet from %s: %s", server.getServerInfo().getName(), packet);

            // Handler spec handlers
            for(IPacketHandler handler : mHandlers.get(packet.getClass()))
                handler.handle(packet, server);

            // Handle non spec handlers
            for(IPacketHandler handler : mHandlers.get(null))
                handler.handle(packet, server);
        }
        catch(Throwable e)
        {
            this.plugin.getLogger().error("An error occured handling packet: " + packet + ":");
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive( String channel, DataInput in, MessageSender sender )
    {
        if(channel.equals("BungeeChat"))
        {
            RegisteredServer server = plugin.getServer().getServer(sender.getName()).orElse(null);
            PacketCodec codec = mCodecs.get(server);

            if(codec == null)
            {
                Debugger.logp("Received packet. Pending codec from %s", sender.getName());
                synchronized(mPendingPackets)
                {
                    mPendingPackets.add(new SimpleEntry<>(server.getServerInfo(), in));
                }
                return;
            }

            handleDataPacket(server, codec, in);
        }
        else if(channel.equals("BCState"))
        {
            RegisteredServer server = plugin.getServer().getServer(sender.getName()).orElse(null);
            if(server == null){
                Debugger.logp("Received packet: BCSTATE from unregistered Server:%s ",sender.getName());
                return;
            }
            try
            {
                String type = in.readUTF();
                if(type.equals("Schema"))
                {
                    Debugger.logp("Received schema from %s", server.getServerInfo().getName());
                    PacketCodec codec = PacketCodec.fromSchemaData(in);
                    mCodecs.put(server, codec);
                    doPending(server);
                }
                else if(type.equals("Online"))
                {
                    Debugger.logp("Received online message from %s", server.getServerInfo().getName());
                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(ostream);
                    PacketRegistry.writeSchemaPacket(out);
                    mComLink.sendMessage("BCState", ostream.toByteArray(), sender);
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }


        }
    }

    public void sendSchemas()
    {
        try
        {
            // Send schemas to all servers
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(ostream);
            PacketRegistry.writeSchemaPacket(out);

            mComLink.broadcastMessage("BCState", ostream.toByteArray());

            // Request schemas from all servers
            ostream = new ByteArrayOutputStream();
            out = new DataOutputStream(ostream);
            out.writeUTF("SchemaRequest");

            mComLink.broadcastMessage("BCState", ostream.toByteArray());
        }
        catch ( IOException e )
        {
            // cant happen
            e.printStackTrace();
        }
    }

    private void doPending(RegisteredServer server)
    {
        PacketCodec codec = mCodecs.get(server);
        if(codec == null)
            return;

        synchronized(mPendingPackets)
        {
            Iterator<SimpleEntry<ServerInfo, DataInput>> it = mPendingPackets.iterator();

            while(it.hasNext())
            {
                SimpleEntry<ServerInfo, DataInput> entry = it.next();

                if(entry.getKey().equals(server))
                {
                    it.remove();
                    Debugger.logp("Do pending:");
                    handleDataPacket(server, codec, entry.getValue());
                }
            }
        }
    }

    @Override
    public void onConnectionLost( Throwable e )
    {
    }

    @Override
    public void onConnectionRestored()
    {
        this.plugin.getServer().getScheduler().buildTask(plugin, () -> {
            System.out.println("[BungeeChat] Redis connection restored");
            plugin.getSyncManager().sendConfig("bungeechat");
        });
    }
}
