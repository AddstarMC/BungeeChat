#!/bin/bash

SERVERS=(main hub)

mvn clean install

for i in ${SERVERS[@]}
do
SERVERFOLDER="/D/Servers/$i/plugins"
cp Bukkit/target/BungeeChat-Bukkit*.jar "$SERVERFOLDER/BungeeChat-Bukkit.jar"
echo "Coppying jar to $SERVERFOLDER"
done

cp Proxy/target/BungeeChat*.jar "/D/Servers/bungee/plugins/BungeeChat-Proxy.jar"
echo "Coppying proxy plugin"

echo "Done" 
read