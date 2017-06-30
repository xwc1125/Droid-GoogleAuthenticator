package com.google.android.apps.authenticator.entity;

/**
 * 唯一码显示的实体类
 * <p>
 * A tuple of user, OTP value, and type, that represents a particular user.
 *
 * @author adhintz@google.com (Drew Hintz)
 */
public class PinInfo {
    /**
     * 唯一码
     */
    public String pin; // calculated OTP, or a placeholder if not calculated
    /**
     * 用户名
     */
    public String user;
    /**
     * 是否是计数类型的。如果是，那么刷新按钮就会显示
     */
    public boolean isHotp = false; // used to see if button needs to be displayed

    /**
     * 计数类型使用：是否允许生成Code
     * <p>
     * HOTP only: Whether code generation is allowed for this account.
     */
    public boolean hotpCodeGenerationAllowed;
}
