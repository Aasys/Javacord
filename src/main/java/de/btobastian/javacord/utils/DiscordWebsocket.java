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
import de.btobastian.javacord.entities.VoiceChannel;
import de.btobastian.javacord.utils.handler.ReadyHandler;
import de.btobastian.javacord.utils.handler.ReadyReconnectHandler;
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
import de.btobastian.javacord.utils.handler.voice.VoiceServerUpdateHandler;
import de.btobastian.javacord.utils.handler.voice.VoiceStateUpdateHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * The websocket which is used to connect to discord.
 */
public class DiscordWebsocket extends WebSocketClient {

    private final SettableFuture<Boolean> ready;
    private final ImplDiscordAPI api;
    private final HashMap<String, PacketHandler> handlers = new HashMap<>();
    private final boolean isReconnect;
    private volatile boolean isClosed = false;

    // the voice websocket
    private DiscordVoiceWebsocket voiceWebsocket = null;
    // params needed for voice
    private ReentrantLock voiceLock = new ReentrantLock();
    private String voiceToken;
    private String voiceEndpoint;
    private String voiceSessionId;
    private VoiceChannel voiceChannel;


    // received in packet with op = 7
    private String urlForReconnect = null;

    /**
     * Creates a new instance of this class.
     *
     * @param serverURI The uri of the gateway the socket should connect to.
     * @param api The api.
     * @param reconnect Whether it's a reconnect or not.
     */
    public DiscordWebsocket(URI serverURI, ImplDiscordAPI api, boolean reconnect) {
        super(serverURI);
        this.api = api;
        this.ready = SettableFuture.create();
        registerHandlers();
        this.isReconnect = reconnect;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // I don't know why, but sometimes we get an error with close code 1006 (connection closed abnormally (locally))
        // The really strange thing is: Everything works fine after this error. The socket sometimes is still connected
        // TODO find the reason for this behaviour
        System.out.println("Websocket closed with reason " + reason + " and code " + code);
        isClosed = true;
        if (remote && urlForReconnect != null) {
            System.out.println("Trying to reconnect (we received op 7 before)...");
            api.reconnectBlocking(urlForReconnect);
            System.out.println("Reconnected!");
        } else if (remote && api.isAutoReconnectEnabled()) {
            System.out.println("Trying to auto-reconnect...");
            api.reconnectBlocking();
            System.out.println("Reconnected!");
        }
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

        int op = obj.getInt("op");
        if (op == 7) {
            String url = obj.getJSONObject("d").getString("url");
        }

        JSONObject packet = obj.getJSONObject("d");
        String type = obj.getString("t");

        if (type.equals("READY") && isReconnect) {
            // we would get some errors if we do not handle the missed data
            handlers.get("READY_RECONNECT").handlePacket(packet);
            ready.set(true);
            updateStatus();
            return; // do not handle the ready packet twice
        }

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

    @Override
    public void close() {
        isClosed = true;
        super.close();
    }

    @Override
    public void closeBlocking() throws InterruptedException {
        isClosed = true;
        super.closeBlocking();
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
                while (!isClosed) {
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
     * Sends the payload which is the first step of connecting to a voice channel.
     *
     * @param channel The channel the bot should connect to.
     */
    public void payloadVoice(VoiceChannel channel) {
        JSONObject payload = new JSONObject()
                .put("op", 4)
                .put("d", new JSONObject()
                        .put("guild_id", channel.getServer().getId())
                        .put("channel_id", channel.getId())
                        .put("self_mute", false)
                        .put("self_deaf", false)
                );
        voiceChannel = channel;
        voiceWebsocket = null;
        voiceEndpoint = null;
        voiceToken = null;
        voiceSessionId = null;
        send(payload.toString());
    }

    /**
     * Sets the voice token and connects the voice socket if the session id was set, too.
     *
     * @param token The voice token to set.
     * @return Whether the socket connects now or not.
     */
    public boolean setVoiceTokenAndEndpoint(String token, String endpoint) {
        try {
            // #setToken and #setSessionId should be called sync, but this may change in the future
            // so this method is thread safe even if it's not necessary atm
            voiceLock.lock();
            this.voiceToken = token;
            this.voiceEndpoint = endpoint;
            if (voiceSessionId != null && voiceWebsocket == null) {
                voiceWebsocket = connectVoice();
                return true;
            }
        } finally {
            voiceLock.unlock();
        }
        return false;
    }

    /**
     * Sets the voice session id and connects the socket if the token was set, too.
     *
     * @param sessionId The voice session id to set.
     */
    public boolean setVoiceSessionId(String sessionId) {
        try {
            // #setToken and #setSessionId should be called sync, but this may change in the future
            // so this method is thread safe even if it's not necessary atm
            voiceLock.lock();
            this.voiceSessionId = sessionId;
            if (voiceToken != null && voiceWebsocket == null) {
                voiceWebsocket = connectVoice();
                return true;
            }
        } finally {
            voiceLock.unlock();
        }
        return false;
    }

    /**
     * Creates a new voice websocket and connects.
     *
     * @return The voice websocket and connects.
     */
    private DiscordVoiceWebsocket connectVoice() {
        try {
            DiscordVoiceWebsocket socket = new DiscordVoiceWebsocket(
                    new URI(voiceEndpoint), api, voiceChannel, voiceToken, voiceSessionId);
            socket.connect();
            return socket;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Registers all handlers.
     */
    private void registerHandlers() {
        // general
        addHandler(new ReadyHandler(api));
        addHandler(new ReadyReconnectHandler(api));

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

        // voice
        addHandler(new VoiceServerUpdateHandler(api));
        addHandler(new VoiceStateUpdateHandler(api));
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
