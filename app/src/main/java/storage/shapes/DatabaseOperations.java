package storage.shapes;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;

/**
 * Created by Fritz on 4/7/2016.
 */
public abstract class DatabaseOperations  {
    static Context context;
    static LocalDatabaseOperations local;
    static RemoteDatabaseOperations remote;
    private static long lastTopScore;

    public static void DatabaseOperationsInit(Context context) {
        DatabaseOperations.context = context;
        local = new LocalDatabaseOperations(context);
        remote = new RemoteDatabaseOperations();
        lastTopScore = 0;
    }

    /* Begin private helpler methods */
    /**
     * Determine whether a network connection is available
     * @return true if the network is availabe, otherwise false.
     */
    private static boolean isNetworkConnected()
    {
        boolean ret = false;
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        ret = (networkInfo != null && networkInfo.isConnected());

        if (!ret)
            System.out.println("Sorry, the network is not connected.");

        return ret;
    }
    /* End private helper methods */

    /* Begin shared database operations */
    /**
     * Log the user out of the given database.
     *
     * @param username the user's username
     */
    public static void logout(String username) {
        local.logout(username);

        if (isNetworkConnected())
            remote.logout(username);
    }

    /**
     * Log the user into the given database.
     *
     * @param username the user's username
     * @param password the given user's password
     * @return true if the user has been logged in; otherwise false.
     */
    public static boolean login(String username, String password)
    {
        if (!local.login(username, password)) {
             return false;
        }

        if (isNetworkConnected() && !remote.login(username, password)) {
                System.out.println("Play with friends is not available." +
                        "  Please check your credentials.");
        } else {
            System.out.println("Failed to contact remote database.");
        }

        return true;
    }

    /**
     * Set the highScore for the given user.
     *
     * @param username the user's username.
     * @param score    the new highscore.
     */
    public static void setHighScore(String username, long score)
    {
        local.setHighScore(username, score);

        if (isNetworkConnected())
            remote.setHighScore(username, score);
    }

    /**
     * Get the highscore for the given user.
     *
     * @param username the user's username.
     * @return the highscore, or -1 on error.
     */
    public static long getHighScore(String username)
    {
        if (isNetworkConnected() && remote.getLoginStatus(username))
            return remote.getHighScore(username);
        else
            return local.getHighScore(username);

    }

    /**
     * Add the given user to the given database.
     *
     * @param username the desired username
     * @param password the desired password
     * @return true if the user is added to the database; otherwise false
     */
    public static boolean addUser(String username, String password) {
        boolean ret;
        // Ensure username only contains valid characters
        if (username.contains("\\"))
            System.out.println("usernames may not contain \"\\\"");
        if (username.contains("'"))
            System.out.println("usernames may not contain \"'\"");

        if (!(ret = local.addUser(username, password))) {
            System.out.println("user '"+ username +"' could not be added to the local database");
        }

        if (isNetworkConnected() && !remote.addUser(username, password)) {
            System.out.println("user '"+ username +"' could not be added to the remote database");
        }

        return ret;
    }

    /**
     * delete the user from the database.
     *
     * @param username the user's username
     * @param password the associated password for the given username
     * @return true if the user is deleted; otherwise false.
     */
    public static boolean deleteUser(String username, String password) {
        boolean ret = true;

        if (local.deleteUser(username, password)) {
            if (!isNetworkConnected() || !remote.deleteUser(username, password)) {
                local.addUser(username, password);
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Updated the given users password prodived that the oldPassword is correct.
     *
     * @param username    the user's username
     * @param oldPassword the user's old password
     * @param newPassword the user's new password
     * @return true if the passwoard was updated correctly; otherwise false.
     */
    public static boolean setPassword(String username, String oldPassword, String newPassword)
    {
        if (isNetworkConnected() && remote.setPassword(username, oldPassword, newPassword))
            return true;

        return false;
    }

    /**
     * Add a new friend to the give user's friend list.
     * @param usernameOwner the local user.
     * @param usernameFriend the friend's username
     * @return true if the friend is added; otherwise false
     */
    public static boolean addNewFriend(String usernameOwner, String usernameFriend) {
        if (isNetworkConnected()) {
            if (!remote.findUser(usernameFriend)) {
                System.out.println("Sorry, we can't find that friend.");
                return false;
            }

            if (!local.addNewFriend(usernameOwner, usernameFriend)) {
                System.out.println("Sorry, we were unable to add your friend to the local database.");
                return false;
            }

            if (remote.addNewFriend(usernameOwner, usernameFriend)) {
                return true;
            } else {
                System.out.println("Sorry, we were unable to add your friend to the remote database.");
                local.deleteFriend(usernameOwner, usernameFriend);
            }
        } else {
            System.out.println("Sorry, you need to connect to a network to use this feature.");
        }

        return false;
    }

    /**
     * Delete a friend from the given user's friend table.
     *
     * @param usernameOwner the local user.
     * @param usernameFriend the friend to delete.
     * @return true if deleted; otherwise false.
     */
    public static boolean deleteFriend(String usernameOwner, String usernameFriend) {
        if (!local.deleteFriend(usernameOwner, usernameFriend))
            return false;

        if (isNetworkConnected() && remote.deleteFriend(usernameOwner, usernameFriend))
            return true;

        return local.addNewFriend(usernameOwner, usernameFriend);
    }
    /* End shared database operations */

    /* Begin remote database operations */
    /**
     * Return the daily challenge block seed.
     *
     *
     * @return the block seed or -1 otherwise.
     */
    public static long getDailyChallengeSeed()
    {
        return remote.getDailyChallengeSeed();
    }

    /**
     * Return the block seed for the given user.
     *
     * @param username the username of the user.
     *
     * @return the block seed or -1 if not block seed exists for this user.
     */
    public static long getBlockSeed(String username)
    {
        if (isNetworkConnected())
            return remote.getBlockSeed(username);

        return -1;
    }

    /**
     * Set the block seed for the given user.
     *
     * @param username the username of the user.
     * @param seed     the value of the new seed.
     */
    public static void setBlockSeed(String username, long seed)
    {
        if (isNetworkConnected()) {
            remote.setBlockSeed(username, seed);
        }
    }

    /**
     * Get the username and score of the friend with the highest score.
     *
     * @param username the local user's username
     * @return name,score of the top friend on success otherwise null
     */
    public static String[] getTopFriend(String username)
    {
        String name_score = "-1";

        if (isNetworkConnected()) {
            name_score = remote.getTopFriend(username);
        }

        return name_score.split(",");
    }

    /**
     * Returns true if the user is connected to the remote database and
     * their password matches that of the password stored on the remote
     * database.
     *
     * @param username the username of the user
     * @return true if the user is logged in; otherwise return false
     */
    public static boolean getLoginStatus(String username)
    {
        if (isNetworkConnected()) {
            return remote.getLoginStatus(username);
        }

        return false;
    }
    /* End remote database operations */

    /* Begin local database operations */
    /**
     * Get the given user's list of friends
     * @param username the user's username
     * @return an array list of friends on success; otherwise null.
     */
    public static ArrayList<String> getFriendsList(String username)
    {
        return local.getFriendsList(username);
    }

    /**
     * Get the local logged in user.
     *
     * @return the username of the logged in user, otherwise null
     */
    public static String getLocalLoggedInUser()
    {
        return local.getLocalLoggedInUser();
    }

    /**
     * Set the token for the given username.
     *
     * @param username the user's username
     * @param tok the associated token
     * @return true if the token was set; otherwise false.
     */
    public static boolean setToken(String username, String tok)
    {
        return local.setToken(username, tok);
    }

    /**
     * Get the token for the given username
     * @param username the user's username
     * @return the token on success, otherwise null
     */
    public static String getToken(String username)
    {
        return local.getToken(username);
    }
    /* End local database operations */
}
