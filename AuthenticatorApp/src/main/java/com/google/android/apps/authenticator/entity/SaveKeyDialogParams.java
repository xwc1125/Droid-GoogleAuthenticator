package com.google.android.apps.authenticator.entity;

import com.google.android.apps.authenticator.activity.AuthenticatorActivity;
import com.google.android.apps.authenticator.entity.OtpType;

import java.io.Serializable;

/**
 * Parameters to the {@link AuthenticatorActivity#DIALOG_ID_SAVE_KEY} dialog.
 */
public class SaveKeyDialogParams implements Serializable {
    private String user;
    private String secret;
    private OtpType type;
    private Integer counter;

    public SaveKeyDialogParams(String user, String secret, OtpType type, Integer counter) {
        this.user = user;
        this.secret = secret;
        this.type = type;
        this.counter = counter;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public OtpType getType() {
        return type;
    }

    public void setType(OtpType type) {
        this.type = type;
    }

    public Integer getCounter() {
        return counter;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }
}
