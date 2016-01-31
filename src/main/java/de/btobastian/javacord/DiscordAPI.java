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

import de.btobastian.javacord.listener.ReadyListener;

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
     * @param readyListener The listener which informs you whether the connection succeeded or failed.
     */
    public void connect(ReadyListener readyListener);

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

}
