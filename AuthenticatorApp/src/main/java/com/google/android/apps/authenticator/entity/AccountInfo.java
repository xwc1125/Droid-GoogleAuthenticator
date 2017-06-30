package com.google.android.apps.authenticator.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * Created by xwc1125 on 2017/6/28.
 */
@Table(name = "AuthAccount")
public class AccountInfo {
    @Column(name = "id", isId = true, autoGen = true)
    private int id;//ID自动生成
    @Column(name = "email")
    private String email;//邮箱
    @Column(name = "secret")
    private String secret;//密码
    @Column(name = "counter")
    private Integer counter = 0;
    @Column(name = "type")
    private Integer type;
    @Column(name = "provider")
    private Integer provider;

    public AccountInfo() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Integer getCounter() {
        return counter;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getProvider() {
        return provider;
    }

    public void setProvider(Integer provider) {
        this.provider = provider;
    }
}
