package org.thesandbox.core.data;

import java.util.List;

public final class PlayerDataKeys {

    // player
    public static final String FIRST_TIME_JOINING = "FirstTimeJoining";
    public static final String COINS = "coins";
    public static final String LAST_LOCATION = "LastLocation";

    // stuff
    public static final String JUMPPADS_MODE = "jumppadmode";
    public static final String LOGIN_MESSAGE = "LoginMessage";
    public static final String LIGHTNING_ROD = "Lightning Rod";
    public static final String LOGIN_MESSAGES_STATE = "Login Messages";
    public static final String CLOWN_FISH = "Clownfish";
    public static final String RIDEABLE_ENDER_PEARL = "Rideable Ender Pearl";
    public static final String GRAPPLING_HOOK = "Grappling Hook";
    public static final String Stacking_Potato = "Stacking Potato";
    public static final String FLOAT_BOAT = "Float Boat";
    public static final String WIND_ROD = "Wind Rod";
    public static final String MARRIAGE_STATUS = "MarriageStatus";

    // marriage
    public static final String MARRIAGE_SPOUSE = "MarriageSpouse";
    public static final String GENDER = "male";
    // STAFF
    public static final String VANISHED = "Vanished";
    // CHATFILTER
    public static final String CHATFILTER = "ChatFilterStatus";
    // IP TRACKING
    public static final String LAST_IP = "LastIp";
    // RANK TRACKING
    public static final String LAST_RANK = "LastRank";

    public static List<String> getAllKeys() {
        return List.of(
                FIRST_TIME_JOINING,
                COINS,
                LAST_LOCATION,
                JUMPPADS_MODE,
                LOGIN_MESSAGE,
                LIGHTNING_ROD,
                LOGIN_MESSAGES_STATE,
                CLOWN_FISH,
                RIDEABLE_ENDER_PEARL,
                GRAPPLING_HOOK,
                Stacking_Potato,
                FLOAT_BOAT,
                WIND_ROD,
                MARRIAGE_STATUS,
                MARRIAGE_SPOUSE,
                GENDER,
                VANISHED,
                CHATFILTER,
                LAST_IP,
                LAST_RANK
        );
    }

    private PlayerDataKeys() {
    }
}
