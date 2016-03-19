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
package de.btobastian.javacord.utils.handler.message;

import de.btobastian.javacord.ImplDiscordAPI;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.listener.message.TypingStartListener;
import de.btobastian.javacord.utils.PacketHandler;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Handles the typing start packet.
 */
public class TypingStartHandler extends PacketHandler {

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public TypingStartHandler(ImplDiscordAPI api) {
        super(api, true, "TYPING_START");
    }

    @Override
    public void handle(JSONObject packet) {
        Channel channelTemp = null;
        String channelId = packet.getString("channel_id");
        Iterator<Server> serverIterator = api.getServers().iterator();
        while (serverIterator.hasNext()) {
            channelTemp = serverIterator.next().getChannelById(channelId);
            if (channelTemp != null) {
                break;
            }
        }
        final Channel channel = channelTemp;

        String userId = packet.getString("user_id");
        final User user;
        try {
            user = api.getUserById(userId).get();
        } catch (InterruptedException | ExecutionException e) {
            return;
        }
        listenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                List<TypingStartListener> listeners = api.getListeners(TypingStartListener.class);
                synchronized (listeners) {
                    for (TypingStartListener listener : listeners) {
                        listener.onTypingStart(api, user, channel);
                    }
                }
            }
        });
    }

}
