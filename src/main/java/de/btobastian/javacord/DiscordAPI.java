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
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.Listener;
import de.btobastian.javacord.utils.ThreadPool;

import java.util.Collection;

/**
 * This is the most important class of the api.
 *
 * Every instance represents an account.
 * If you want to connect to more than one discord account you have to use more instances.
 */
public interface DiscordAPI {

    /**
     * Connects to the account with the given email and password.
     *
     * This method is non-blocking.
     *
     * @param callback The callback will inform you when the connection is ready.
     *                 The connection is ready as soon as the ready packet was received.
     */
    public void connect(FutureCallback<DiscordAPI> callback);

    /**
     * Connects to the account with the given email and password.
     *
     * This method is blocking! It's recommended to use the non-blocking version which
     * uses a thread from the internal used thread pool to connect.
     */
    public void connectBlocking();

    /**
     * Sets the email address which should be used to connect to the account.
     *
     * @param email The email address to set.
     */
    public void setEmail(String email);

    /**
     * Sets the password which should be used to connect to the account.
     *
     * @param password The password to set.
     */
    public void setPassword(String password);

    /**
     * Sets the game shown in the user list.
     *
     * @param game The game to set.
     */
    public void setGame(String game);

    /**
     * Gets the game shown in the user list.
     *
     * @return The game.
     */
    public String getGame();

    /**
     * Gets a server by its id.
     *
     * @param id The id of the server.
     * @return The server with the given id. <code>Null</code> if no server with the id was found.
     */
    public Server getServerById(String id);

    /**
     * Gets a collection with all known servers.
     *
     * @return A collection with all known servers.
     */
    public Collection<Server> getServers();

    /**
     * Gets an user by its id.
     *
     * @param id The id of the user.
     * @return The user with the given id. <code>Null</code> if no user with the id was found.
     */
    public User getUserById(String id);

    /**
     * Gets a collection with all known users.
     *
     * @return A collection with all known users.
     */
    public Collection<User> getUsers();

    /**
     * Registers a listener.
     *
     * @param listener The listener to register.
     */
    public void registerListener(Listener listener);

    /**
     * Gets a message by its id.
     * This method may return <ocde>null</ocde> even if the message exists!
     *
     * @param id The id of the message.
     * @return The message with the given id or <code>null</code> no message was found.
     */
    public Message getMessageById(String id);

    /**
     * Gets the used thread pool of this plugin.
     *
     * The {@link ThreadPool} contains the used thread pool(s) of this api.
     * Don't use multi-threading if you don't known how to made things thread-safe
     * or how to prevent stuff like deadlocks!
     *
     * @return The used thread pool.
     */
    public ThreadPool getThreadPool();

    /**
     * Sets the idle state of the bot.
     *
     * @param idle Whether the bot is idle or not.
     */
    public void setIdle(boolean idle);

    /**
     * Checks if the bot is idle.
     *
     * @return Whether the bot is idle or not.
     */
    public boolean isIdle();

}
