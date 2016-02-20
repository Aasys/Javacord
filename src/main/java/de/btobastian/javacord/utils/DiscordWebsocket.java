/*
 * Copyright (C) 2016 Bastian Oppermann
 * 
 * This file is part of Javacord.
 * 
 * Javacord is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser general Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Javacord is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.btobastian.javacord.utils;

import com.google.common.util.concurrent.SettableFuture;
import de.btobastian.javacord.ImplDiscordAPI;
import de.btobastian.javacord.utils.handler.ReadyHandler;
import de.btobastian.javacord.utils.handler.channel.ChannelCreateHandler;
import de.btobastian.javacord.utils.handler.channel.ChannelDeleteHandler;
import de.btobastian.javacord.utils.handler.channel.ChannelUpdateHandler;
import de.btobastian.javacord.utils.handler.message.MessageCreateHandler;
import de.btobastian.javacord.utils.handler.message.MessageDeleteHandler;
import de.btobastian.javacord.utils.handler.message.MessageUpdateHandler;
import de.btobastian.javacord.utils.handler.message.TypingStartHandler;
import de.btobastian.javacord.utils.handler.server.*;
import de.btobastian.javacord.utils.handler.server.role.GuildRoleCreateHandler;
import de.btobastian.javacord.utils.handler.server.role.GuildRoleDeleteHandler;
import de.btobastian.javacord.utils.handler.server.role.GuildRoleUpdateHandler;
import de.btobastian.javacord.utils.handler.user.PresenceUpdateHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * The websocket which is used to connect to discord.
 */
public class DiscordWebsocket extends WebSocketClient {

    private SettableFuture<Boolean> ready = null;
    private ImplDiscordAPI api = null;
    private HashMap<String, PacketHandler> handlers = new HashMap<>();

    /**
     * Creates a new instance of this class.
     *
     * @param serverURI The uri of the gateway the socket should connect to.
     * @param api The api.
     */
    public DiscordWebsocket(URI serverURI, ImplDiscordAPI api) {
        super(serverURI);
        this.api = api;
        ready = SettableFuture.create();
        registerHandlers();
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        System.out.println("Websocket closed with reason " + s + " and code " + i);
        if (!ready.isDone()) {
            ready.set(false);
        }
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onMessage(String message) {
        JSONObject obj = new JSONObject(message);
        JSONObject packet = obj.getJSONObject("d");
        String type = obj.getString("t");

        PacketHandler handler = handlers.get(type);
        if (handler != null) {
            handler.handlePacket(packet);
        }
        if (type.equals("READY")) {
            ready.set(true);
            updateStatus();
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        JSONObject connectPacket = new JSONObject()
                .put("op", 2)
                .put("d", new JSONObject()
                    .put("token", api.getToken())
                    .put("v", 3)
                    .put("properties", new JSONObject()
                        .put("$os", System.getProperty("os.name"))
                        .put("$browser", "None")
                        .put("$device", "")
                        .put("$referrer", "https://discordapp.com/@me")
                        .put("$referring_domain", "discordapp.com"))
                    .put("large_threshold", 250)
                    .put("compress", true));
        send(connectPacket.toString());
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        byte[] compressedData = bytes.array();
        Inflater decompressor = new Inflater();
        decompressor.setInput(compressedData);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);
        byte[] buf = new byte[1024];
        while (!decompressor.finished()) {
            int count;
            try {
                count = decompressor.inflate(buf);
            } catch (DataFormatException e) {
                e.printStackTrace();
                System.exit(-1);
                return;
            }
            bos.write(buf, 0, count);

        }
        try {
            bos.close();
        } catch (IOException ignored) { }
        byte[] decompressedData = bos.toByteArray();
        try {
            onMessage(new String(decompressedData, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the Future which tells whether the connection is ready or failed.
     *
     * @return The Future.
     */
    public Future<Boolean> isReady() {
        return ready;
    }

    /**
     * Starts to send the heartbeat.
     *
     * @param heartbeatInterval The heartbeat interval received in the ready packet.
     */
    public void startHeartbeat(final long heartbeatInterval) {
        api.getThreadPool().getExecutorService().submit(new Runnable() {
            @Override
            public void run() {
                long timer = System.currentTimeMillis();
                for (;;) {
                    if ((System.currentTimeMillis() - timer) >= heartbeatInterval - 10) {
                        Object nullObject = null;
                        JSONObject heartbeat = new JSONObject()
                                .put("op", 1)
                                .put("d", System.currentTimeMillis());
                        send(heartbeat.toString());
                        timer = System.currentTimeMillis();
                        if (Math.random() < 0.1) {
                            // some random status updates to ensure the game and idle status is updated correctly
                            // (discord only accept 5 of these packets per minute and ignores more
                            //  so some might get lost).
                            updateStatus();
                        }
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Sens the update status packet
     */
    public void updateStatus() {
        JSONObject updateStatus = new JSONObject()
                .put("op", 3)
                .put("d", new JSONObject()
                        .put("game", new JSONObject()
                                .put("name", api.getGame() == null ? JSONObject.NULL : api.getGame()))
                        .put("idle_since", api.isIdle() ? 1 : JSONObject.NULL));
        send(updateStatus.toString());
    }

    /**
     * Registers all handlers.
     */
    private void registerHandlers() {
        // general
        addHandler(new ReadyHandler(api));

        // channel
        addHandler(new ChannelCreateHandler(api));
        addHandler(new ChannelDeleteHandler(api));
        addHandler(new ChannelUpdateHandler(api));

        // message
        addHandler(new MessageCreateHandler(api));
        addHandler(new MessageDeleteHandler(api));
        addHandler(new MessageUpdateHandler(api));
        addHandler(new TypingStartHandler(api));

        // server
        addHandler(new GuildBanAddHandler(api));
        addHandler(new GuildBanRemoveHandler(api));
        addHandler(new GuildCreateHandler(api));
        addHandler(new GuildDeleteHandler(api));
        addHandler(new GuildMemberAddHandler(api));
        addHandler(new GuildMemberRemoveHandler(api));
        addHandler(new GuildMemberUpdateHandler(api));
        addHandler(new GuildUpdateHandler(api));

        // role
        addHandler(new GuildRoleCreateHandler(api));
        addHandler(new GuildRoleDeleteHandler(api));
        addHandler(new GuildRoleUpdateHandler(api));

        // user
        addHandler(new PresenceUpdateHandler(api));
    }

    /**
     * Adds a packet handler.
     *
     * @param handler The handler to add.
     */
    private void addHandler(PacketHandler handler) {
        handlers.put(handler.getType(), handler);
    }

}
