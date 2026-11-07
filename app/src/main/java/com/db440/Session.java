package com.db440;

/**
 * Remembers who is logged in, so any screen can look up the current user
 * instead of having it passed along on every page change.
 */
public final class Session {

    private static String username;

    private Session() {
    }

    public static void login(String user) {
        username = user;
    }

    public static void logout() {
        username = null;
    }

    public static String getUsername() {
        return username;
    }

}
