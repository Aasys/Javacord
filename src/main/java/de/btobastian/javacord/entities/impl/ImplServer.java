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
package de.btobastian.javacord.entities.impl;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import de.btobastian.javacord.ImplDiscordAPI;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.permissions.Role;
import de.btobastian.javacord.entities.permissions.impl.ImplRole;
import de.btobastian.javacord.exceptions.PermissionsException;
import de.btobastian.javacord.listener.Listener;
import de.btobastian.javacord.listener.server.ServerLeaveListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The implementation of the server interface.
 */
public class ImplServer implements Server {

    private final ImplDiscordAPI api;

    private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> members = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Role> roles = new ConcurrentHashMap<>();

    private final String id;
    private String name;

    /**
     * Creates a new instance of this class.
     *
     * @param data A JSONObject containing all necessary data.
     * @param api The api of this server.
     */
    public ImplServer(JSONObject data, ImplDiscordAPI api) {
        this.api = api;

        name = data.getString("name");
        id = data.getString("id");

        JSONArray roles = data.getJSONArray("roles");
        for (int i = 0; i < roles.length(); i++) {
            new ImplRole(roles.getJSONObject(i), this, api);
        }

        JSONArray channels = data.getJSONArray("channels");
        for (int i = 0; i < channels.length(); i++) {
            JSONObject channelJson = channels.getJSONObject(i);
            String type = channelJson.getString("type");
            if (type.equals("text")) {
                new ImplChannel(channels.getJSONObject(i), this, api);
            }
        }

        JSONArray members = new JSONArray();
        if (data.has("members")) {
            members = data.getJSONArray("members");
        }
        for (int i = 0; i < members.length(); i++) {
            User member = api.getOrCreateUser(members.getJSONObject(i).getJSONObject("user"));
            this.members.put(member.getId(), member);

            JSONArray memberRoles = members.getJSONObject(i).getJSONArray("roles");
            for (int j = 0; j < memberRoles.length(); j++) {
                Role role = getRoleById(memberRoles.getString(j));
                if (role != null) {
                    ((ImplRole) role).addUser(member);
                }
            }
        }

        JSONArray presences = new JSONArray();
        if (data.has("presences")) {
            presences = data.getJSONArray("presences");
        }
        for (int i = 0; i < presences.length(); i++) {
            JSONObject presence = presences.getJSONObject(i);
            User user;
            try {
                user = api.getUserById(presence.getJSONObject("user").getString("id")).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                continue;
            }
            if (presence.has("game") && !presence.isNull("game")) {
                if (presence.getJSONObject("game").has("name") && !presence.getJSONObject("game").isNull("name")) {
                    ((ImplUser) user).setGame(presence.getJSONObject("game").getString("name"));
                }
            }
        }

        api.getServerMap().put(id, this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Future<Exception> deleteOrLeave() {
        return api.getThreadPool().getExecutorService().submit(new Callable<Exception>() {
            @Override
            public Exception call() throws Exception {
                try {
                    HttpResponse<JsonNode> response = Unirest.delete("https://discordapp.com/api/guilds/" + id)
                            .header("authorization", api.getToken())
                            .asJson();
                    if (response.getStatus() == 403) {
                        throw new PermissionsException("Missing permissions!");
                    }
                    if (response.getStatus() < 200 || response.getStatus() > 299) {
                        throw new Exception("Received http status code " + response.getStatus()
                                + " with message " + response.getStatusText());
                    }
                    api.getServerMap().remove(id);
                    api.getThreadPool().getSingleThreadExecutorService("handlers").submit(new Runnable() {
                        @Override
                        public void run() {
                            List<Listener> listeners =  api.getListeners(ServerLeaveListener.class);
                            synchronized (listeners) {
                                for (Listener listener : listeners) {
                                    ((ServerLeaveListener) listener).onServerLeave(api, ImplServer.this);
                                }
                            }
                        }
                    });
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }
        });
    }

    @Override
    public Channel getChannelById(String id) {
        return channels.get(id);
    }

    @Override
    public Collection<Channel> getChannels() {
        return channels.values();
    }

    @Override
    public User getMemberById(String id) {
        return members.get(id);
    }

    @Override
    public Collection<User> getMembers() {
        return members.values();
    }

    @Override
    public boolean isMember(User user) {
        return isMember(user.getId());
    }

    @Override
    public boolean isMember(String userId) {
        return members.containsKey(userId);
    }

    @Override
    public Collection<Role> getRoles() {
        return roles.values();
    }

    @Override
    public Role getRoleById(String id) {
        return roles.get(id);
    }

    /**
     * Adds a user to the server.
     *
     * @param user The user to add.
     */
    public void addMember(User user) {
        members.put(user.getId(), user);
    }

    /**
     * Removes a user from the server.
     *
     * @param user The user to remove.
     */
    public void removeMember(User user) {
        members.remove(user.getId());
    }

    /**
     * Adds a channel to the server.
     *
     * @param channel The channel to add.
     */
    public void addChannel(Channel channel) {
        channels.put(channel.getId(), channel);
    }

    /**
     * Adds a role to the server.
     *
     * @param role The role to add.
     */
    public void addRole(Role role) {
        roles.put(role.getId(), role);
    }

    /**
     * Removes a role from the server.
     *
     * @param role The role to remove.
     */
    public void removeRole(Role role) {
        roles.remove(role.getId());
    }

    /**
     * Removes a channel from the server.
     *
     * @param channel The channel to remove.
     */
    public void removeChannel(Channel channel) {
        channels.remove(channel.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
