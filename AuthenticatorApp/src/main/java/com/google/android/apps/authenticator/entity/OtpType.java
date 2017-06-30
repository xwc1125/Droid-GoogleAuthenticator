package com.google.android.apps.authenticator.entity;

/**
 * 密钥的类型
 * <p>
 * Types of secret keys.
 */
public enum OtpType {  // must be the same as in res/values/strings.xml:type
    /**
     * 基于时间
     */
    TOTP(0),  // time based
    /**
     * 基于计数
     */
    HOTP(1);  // counter based

    public final Integer value;  // value as stored in SQLite database

    OtpType(Integer value) {
        this.value = value;
    }

    /**
     * 通过值获取类型
     *
     * @param i
     * @return
     */
    public static OtpType getEnum(Integer i) {
        for (OtpType type : OtpType.values()) {
            if (type.value.equals(i)) {
                return type;
            }
        }

        return null;
    }

}