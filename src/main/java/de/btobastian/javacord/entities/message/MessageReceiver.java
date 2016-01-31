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
package de.btobastian.javacord.entities.message;

import com.google.common.util.concurrent.FutureCallback;

import java.util.concurrent.Future;

/**
 * This interface represents a message receiver.
 * The most common message receivers are {@link de.btobastian.javacord.entities.Channel}
 * and {@link de.btobastian.javacord.entities.User}.
 */
public interface MessageReceiver {

    /**
     * Sends a message with the given content.
     *
     * @param content The content of the message.
     * @return The sent message. Canceled if something didn't work (e.g. missing permissions).
     */
    public Future<Message> sendMessage(String content);

    /**
     * Sends a message with the given content.
     *
     * @param content The content of the message.
     * @param tts Whether the message should be tts or not.
     * @return The sent message. Canceled if something didn't work (e.g. missing permissions).
     */
    public Future<Message> sendMessage(String content, boolean tts);

    /**
     * Sends a message with the given content.
     *
     * @param content The content of the message.
     * @param callback The callback which will be informed when the message was sent or sending failed.
     */
    public void sendMessage(String content, FutureCallback<Message> callback);

    /**
     * Sends a message with the given content.
     *
     * @param content The content of the message.
     * @param tts Whether the message should be tts or not.
     * @param callback The callback which will be informed when the message was sent or sending failed.
     */
    public void sendMessage(String content, boolean tts, FutureCallback<Message> callback);

}
