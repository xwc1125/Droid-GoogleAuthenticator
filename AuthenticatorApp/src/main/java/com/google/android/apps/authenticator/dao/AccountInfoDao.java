package com.google.android.apps.authenticator.dao;

import com.google.android.apps.authenticator.entity.AccountInfo;
import com.google.android.apps.authenticator.config.DBConfig;
import com.google.android.apps.authenticator.entity.OtpType;

import org.xutils.DbManager;
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.util.List;

/**
 * Created by xwc1125 on 2017/6/28.
 */
public class AccountInfoDao {
    private final static String TAG = AccountInfoDao.class.getSimpleName();
    private static boolean isDebug = true;
    public static final int PROVIDER_UNKNOWN = 0;
    public static final int PROVIDER_GOOGLE = 1;

    /**
     * @param email
     * @param secret
     * @param oldEmail
     * @param type
     * @param counter
     * @param googleAccount
     */
    public void update(String email, String secret, String oldEmail,
                       OtpType type, Integer counter, Boolean googleAccount) throws DbException {
        AccountInfo entity = null;
        DbManager db = x.getDb(DBConfig.getDaoConfig());
        entity = db.selector(AccountInfo.class)
                .where("email", "=", oldEmail)
                .findFirst();
        if (entity == null) {
            entity = new AccountInfo();
        }
        entity.setEmail(email);
        entity.setSecret(secret);
        entity.setType(type.ordinal());
        entity.setCounter(counter);
        if (googleAccount != null) {
            entity.setProvider(googleAccount.booleanValue() ? PROVIDER_GOOGLE : PROVIDER_UNKNOWN);
        }
        db.saveOrUpdate(entity);
    }

    /**
     * 获取验证的类型
     *
     * @param email
     * @return
     */
    public OtpType getType(String email) {
        try {
            AccountInfo entity = null;
            DbManager db = x.getDb(DBConfig.getDaoConfig());
            entity = db.selector(AccountInfo.class)
                    .where("email", "=", email)
                    .findFirst();
            if (entity != null) {
                return OtpType.getEnum(entity.getType());
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取所有的信息
     *
     * @return
     */
    public List<AccountInfo> getAccountList() {
        try {
            List<AccountInfo> list;
            DbManager db = x.getDb(DBConfig.getDaoConfig());
            list = db.selector(AccountInfo.class)
                    .where("email", "is not", null)
                    .findAll();
            return list;
        } catch (DbException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getSecret(String email) {
        try {
            AccountInfo entity = null;
            DbManager db = x.getDb(DBConfig.getDaoConfig());
            entity = db.selector(AccountInfo.class)
                    .where("email", "=", email)
                    .findFirst();
            if (entity != null) {
                return entity.getSecret();
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AccountInfo getAccount(String email) {
        try {
            AccountInfo entity = null;
            DbManager db = x.getDb(DBConfig.getDaoConfig());
            entity = db.selector(AccountInfo.class)
                    .where("email", "=", email)
                    .findFirst();
            return entity;
        } catch (DbException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void save(AccountInfo accountInfo) {
        try {
            DbManager db = x.getDb(DBConfig.getDaoConfig());
            db.saveOrUpdate(accountInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(String email) {
        try {
            DbManager db = x.getDb(DBConfig.getDaoConfig());
            WhereBuilder b = WhereBuilder.b();
            b.and("email", "=", email); //构造修改的条件
            db.delete(AccountInfo.class, b);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
