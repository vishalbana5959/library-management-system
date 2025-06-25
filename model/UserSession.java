package com.library.model;

public class UserSession {
    private static final ThreadLocal<Integer> userId = ThreadLocal.withInitial(() -> -1);
    private static final ThreadLocal<String> userName = ThreadLocal.withInitial(() -> "");

    public static void setUser(int id, String name) {
        userId.set(id);
        userName.set(name);
    }

    public static int getUserId() {
        return userId.get();
    }

    public static String getUserName() {
        return userName.get();
    }

    public static boolean isLoggedIn() {
        return userId.get() != -1;
    }

    public static void clearSession() {
        userId.set(-1);
        userName.set("");
    }
}