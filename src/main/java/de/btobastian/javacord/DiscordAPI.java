package de.btobastian.javacord;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

import de.btobastian.javacord.listener.Listener;
import de.btobastian.javacord.listener.ReadyListener;
import de.btobastian.javacord.message.Message;
import de.btobastian.javacord.permissions.PermissionState;
import de.btobastian.javacord.permissions.Permissions;
import de.btobastian.javacord.permissions.PermissionsBuilder;

/**
 * The discord api.
 */
public interface DiscordAPI {

    /**
     * Sets the email address to login.
     * 
     * @param email The email.
     */
    public void setEmail(String email);
    
    /**
     * Sets the password.
     * 
     * @param password The password.
     */
    public void setPassword(String password);
    
    /**
     * Gets the email used to connect.
     * 
     * @return The email.
     */
    public String getEmail();
    
    /**
     * Gets the password used to connect.
     * 
     * @return The password.
     */
    public String getPassword();
    
    /**
     * Attempts to login.
     * 
     * @param listener The listener informs you whether the connection was successful or not.
     */
    public void connect(ReadyListener listener);
    
    /**
     * Checks if the connection if ready.
     * 
     * @return Whether the connection if ready or not.
     */
    public boolean isReady();
    
    /**
     * Sets the encoding (default: UTF-8).
     * 
     * @param encoding The encoding to set.
     * @throws UnsupportedEncodingException if it's an unknown encoding.
     */
    public void setEncoding(String encoding) throws UnsupportedEncodingException;
    
    /**
     * Gets the used encoding.
     * 
     * @return The used encoding.
     */
    public String getEncoding();
    
    /**
     * Sets the current game.
     * This may have a short delay.
     * 
     * @param game The game to set.
     */
    public void setGame(String game);
    
    /**
     * Gets the current game.
     * 
     * @return The current game.
     */
    public String getGame();
    
    /**
     * Gets an user by it's id.
     * 
     * @param id The is of the user.
     * @return The user with the given id. <code>Null</code> if no user with the given id is known.
     */
    public User getUserById(String id);
    
    /**
     * Gets a message by its id.
     * 
     * @param messageId The id of the message.
     * @return The message. May be <code>null</code>, even if a message with the given id exists!
     */
    public Message getMessageById(String messageId);
    
    /**
     * Gets a collection with all known users.
     * 
     * @return A collection with all known users.
     */
    public Collection<User> getUsers();
    
    /**
     * Gets a collection with all known servers.
     * 
     * @return A collection with all known servers.
     */
    public Collection<Server> getServers();
    
    /**
     * Registers a listener.
     * 
     * @param listener The listener to register.
     */
    public void registerListener(Listener listener);
    
    /**
     * Accepts an invite.
     * 
     * @param inviteCode The invite code.
     * @return Whether you were able to join or not.
     */
    public boolean acceptInvite(String inviteCode);
    
    /**
     * Gets the user object of yourself.
     * Sending yourself messages and doing other strange stuff can cause some errors, so don't do it.
     * 
     * @return Yourself.
     */
    public User getYourself();
    
    /**
     * Gets a server by its id.
     * 
     * @param id The id of the server.
     * @return The server with the given id.
     */
    public Server getServerById(String id);
    
    /**
     * Gets a new permissions builder with every type set to {@link PermissionState#NONE}
     * 
     * @return A new permissions builder.
     */
    public PermissionsBuilder getPermissionsBuilder();
    
    /**
     * Gets a new permissions builder.
     * 
     * @param The permissions which should be copied.
     * @return A new permissions builder.
     */
    public PermissionsBuilder getPermissionsBuilder(Permissions permissions);
    
}
