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
package de.btobastian.javacord;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.btobastian.javacord.utils.DiscordWebsocket;
import de.btobastian.javacord.utils.ThreadPool;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * The implementation of {@link DiscordAPI}.
 */
public class ImplDiscordAPI implements DiscordAPI {

    private final ThreadPool pool;

    private String email =  null;
    private String password = null;
    private String token = null;
    private String game = "";

    private DiscordWebsocket socket = null;

    /**
     * Creates a new instance of this class.
     *
     * @param pool The used pool of the library.
     */
    public ImplDiscordAPI(ThreadPool pool) {
        this.pool = pool;
    }

    @Override
    public void connect(FutureCallback<DiscordAPI> callback) {
        final DiscordAPI api = this;
        Futures.addCallback(pool.getListeningExecutorService().submit(new Callable<DiscordAPI>() {
            @Override
            public DiscordAPI call() throws Exception {
                connectBlocking();
                return api;
            }
        }), callback);
    }

    @Override
    public void connectBlocking() {
        token = requestTokenBlocking();
        String gateway = requestGatewayBlocking();
        try {
            socket = new DiscordWebsocket(new URI(gateway), this);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null); // using defaults
            socket.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
            socket.connect();
        } catch (URISyntaxException | KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        try {
            if (!socket.isReady().get()) {
                throw new IllegalStateException("Socket closed before ready packet was received!");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void setGame(String game) {
        this.game = game;
    }

    @Override
    public String getGame() {
        return game;
    }

    /**
     * Gets the token. May be null if not connected.
     *
     * @return The token.
     */
    public String getToken() {
        return token;
    }

    /**
     * Gets the ThreadPool used by this api.
     *
     * @return The used ThreadPool.
     */
    public ThreadPool getThreadPool() {
        return pool;
    }

    /**
     * Gets the used websocket.
     *
     * @return The websocket.
     */
    public DiscordWebsocket getSocket() {
        return socket;
    }

    /**
     * Requests a new token.
     *
     * @return The requested token.
     */
    public String requestTokenBlocking() {
        try {
            HttpResponse<JsonNode> response = Unirest.post("https://discordapp.com/api/auth/login")
                    .field("email", email)
                    .field("password", password)
                    .asJson();
            JSONObject jsonResponse = response.getBody().getObject();
            if (jsonResponse.has("password") || jsonResponse.has("email")) {
                throw new IllegalArgumentException("Wrong email or password!");
            }
            return jsonResponse.getString("token");
        } catch (UnirestException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Requests the gateway.
     *
     * @return The requested gateway.
     */
    public String requestGatewayBlocking() {
        try {
            HttpResponse<JsonNode> response = Unirest.get("https://discordapp.com/api/gateway")
                    .header("authorization", token)
                    .asJson();
            if (response.getStatus() == 401) {
                throw new IllegalStateException("Cannot request gateway! Invalid token?");
            }
            return response.getBody().getObject().getString("url");
        } catch (UnirestException e) {
            e.printStackTrace();
            return null;
        }
    }

}
