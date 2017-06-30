package com.google.android.apps.authenticator.config;

import org.xutils.DbManager;

/**
 * Class: com.ugchain.wallet.config <br>
 * Description: TODO <br>
 *
 * @author xwc1125 <br>
 * @version V1.0
 * @Copyright: Copyright (c) 2017 <br>
 * @date 2017/6/9  09:16 <br>
 */
public class DBConfig {
    private static DbManager.DaoConfig daoConfig;

    public static DbManager.DaoConfig getDaoConfig() {
        if (daoConfig == null) {
            syncInit();
        }
        return daoConfig;
    }

    private static synchronized void syncInit() {
        if (daoConfig == null) {
            daoConfig = new DbManager.DaoConfig()
                    .setDbName("SafeWallet.db")
                    // 不设置dbDir时, 默认存储在app的私有目录.
                    //.setDbDir(FileUtils.createFolderAuto("/Android/data/com.ugchain.wallet/databases"))
                    .setDbVersion(1)
                    .setDbOpenListener(new DbManager.DbOpenListener() {
                        @Override
                        public void onDbOpened(DbManager db) {
                            // 开启WAL, 对写入加速提升巨大
                            db.getDatabase().enableWriteAheadLogging();
                        }
                    })
                    .setDbUpgradeListener(new DbManager.DbUpgradeListener() {
                        @Override
                        public void onUpgrade(DbManager db, int oldVersion, int newVersion) {
                            // TODO: 升级数据的处理
                        }
                    });
        }
    }
}
