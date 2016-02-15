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
package de.btobastian.javacord.entities;

import com.google.common.util.concurrent.FutureCallback;
import de.btobastian.javacord.entities.permissions.Role;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * This interface represents a discord server (also known as guild).
 */
public interface Server {

    /**
     * Gets the unique id of the server.
     *
     * @return The unique id of the server.
     */
    public String getId();

    /**
     * Gets the name of the server.
     *
     * @return The name of the server.
     */
    public String getName();

    /**
     * Deletes or leaves the server.
     *
     * @return A future which tells us if the deletion was successful or not.
     *         If the exception is <code>null</code> the deletion was successful.
     */
    public Future<Exception> deleteOrLeave();

    /**
     * Gets a channel by its id.
     *
     * @param id The id of the channel.
     * @return The channel with the given id.
     *         <code>Null</code> if the server has no channel with the given id.
     */
    public Channel getChannelById(String id);

    /**
     * Gets a collection with all channels of the server.
     *
     * @return A collection with all channels of the server.
     */
    public Collection<Channel> getChannels();

    /**
     * Gets an user by its id.
     *
     * @param id The id of the user.
     * @return The user with the given id.
     *         <code>Null</code> if the user is no member of this server.
     */
    public User getMemberById(String id);

    /**
     * Gets a collection with all members on this server.
     *
     * @return A collection with all members on this server.
     */
    public Collection<User> getMembers();

    /**
     * Checks if an user is a member of this server.
     *
     * @param user The user to check.
     * @return Whether the user is a member or not.
     */
    public boolean isMember(User user);

    /**
     * Checks if an user is a member of this server.
     *
     * @param userId The id of the user to check.
     * @return Whether the user is a member or not.
     */
    public boolean isMember(String userId);

    /**
     * Gets a collection with all roles of this server.
     *
     * @return A collection with all roles of this server.
     */
    public Collection<Role> getRoles();

    /**
     * Gets a role by its id.
     *
     * @param id The id of the role.
     * @return The role with the given id.
     *         <code>Null</code> if the role does not exist on this server.
     */
    public Role getRoleById(String id);

    /**
     * Creates a new channel.
     *
     * @param name The name of the channel.
     * @return The created channel.
     */
    public Future<Channel> createChannel(String name);

    /**
     * Creates a new channel.
     *
     * @param name The name of the channel.
     * @param callback The callback which will be informed when the server was created.
     * @return The created channel.
     */
    public Future<Channel> createChannel(String name, FutureCallback<Channel> callback);

    /**
     * Gets an array with all invites.
     *
     * @return An array with all invites.
     */
    public Future<Invite[]> getInvites();

    /**
     * Gets an array with all invites.
     *
     * @param callback The callback which will be informed when the request has finished.
     * @return An array with all invites.
     */
    public Future<Invite[]> getInvites(FutureCallback<Invite[]> callback);

    /**
     * Updates the roles of a user.
     *
     * @param user The user.
     * @param roles The roles to set. This will override the existing roles of the user.
     * @return A future which tells us whether the update was successful or not.
     *         If the exception is <code>null</code> the update was successful.
     */
    public Future<Exception> updateRoles(User user, Role[] roles);

    /**
     * Bans the given user from the server.
     *
     * @param user The user to ban.
     * @return A future which tells us whether the ban was successful or not.
     *         If the exception is <code>null</code> the ban was successful.
     */
    public Future<Exception> banUser(User user);

    /**
     * Bans the given user from the server.
     *
     * @param userId The id of the user to ban.
     * @return A future which tells us whether the ban was successful or not.
     *         If the exception is <code>null</code> the ban was successful.
     */
    public Future<Exception> banUser(String userId);

    /**
     * Bans the given user from the server.
     *
     * @param user The user to ban.
     * @param deleteDays Deletes all messages of the user which are younger than <code>deleteDays</code> days.
     * @return A future which tells us whether the ban was successful or not.
     *         If the exception is <code>null</code> the ban was successful.
     */
    public Future<Exception> banUser(User user, int deleteDays);

    /**
     * Bans the given user from the server.
     *
     * @param userId The id of the user to ban.
     * @param deleteDays Deletes all messages of the user which are younger than <code>deleteDays</code> days.
     * @return A future which tells us whether the ban was successful or not.
     *         If the exception is <code>null</code> the ban was successful.
     */
    public Future<Exception> banUser(String userId, int deleteDays);

    /**
     * Unbans the user from the server.
     *
     * @param userId The id of the user to unban.
     * @return A future which tells us whether the unban was successful or not.
     *         If the exception is <code>null</code> the unban was successful.
     */
    public Future<Exception> unbanUser(String userId);

    /**
     * Gets an array with all banned users.
     *
     * @return An array with all banned users.
     */
    public Future<User[]> getBans();

    /**
     * Gets an array with all banned users.
     *
     * @param callback The callback which will be informed when the request finished.
     * @return An array with all banned users.
     */
    public Future<User[]> getBans(FutureCallback<User[]> callback);

}
