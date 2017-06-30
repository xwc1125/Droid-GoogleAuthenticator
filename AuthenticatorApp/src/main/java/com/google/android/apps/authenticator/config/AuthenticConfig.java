package com.google.android.apps.authenticator.config;

/**
 * Description: TODO <br>
 *
 * @author xwc1125 <br>
 * @version V1.0
 * @Copyright: Copyright (c) 2017 <br>
 * @date 2017/6/28  12:05 <br>
 */
public class AuthenticConfig {
    /**
     * 默认30秒更改一次
     * <p>
     * Default passcode timeout period (in seconds)
     */
    public static final int DEFAULT_INTERVAL = 30;

    /**
     * The maximum amount of time (milliseconds) for which a HOTP code is displayed after it's been
     * generated.
     */
    public static final long HOTP_DISPLAY_TIMEOUT = 2 * 60 * 1000;
    /**
     * Minimum amount of time (milliseconds) that has to elapse from the moment a HOTP code is
     * generated for an account until the moment the next code can be generated for the account.
     * This is to prevent the user from generating too many HOTP codes in a short period of time.
     */
    public static final long HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES = 5000;


    /**
     * Scale to use for the text displaying the PIN numbers.
     */
    public static final float PIN_TEXT_SCALEX_NORMAL = 1.0f;
    /**
     * Underscores are shown slightly smaller.
     */
    public static final float PIN_TEXT_SCALEX_UNDERSCORE = 0.87f;
    /**
     * Frequency (milliseconds) with which TOTP countdown indicators are updated.
     */
    public static final long TOTP_COUNTDOWN_REFRESH_PERIOD = 100;

    /**
     * 震动的频率
     */
    public static final long VIBRATE_DURATION = 200L;
}
