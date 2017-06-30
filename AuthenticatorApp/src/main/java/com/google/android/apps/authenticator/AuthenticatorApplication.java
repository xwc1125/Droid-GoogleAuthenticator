/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.authenticator;

import com.google.android.apps.authenticator.utils.FileUtilities;

import android.app.Application;

import org.xutils.x;

/**
 * Authenticator application which is one of the first things instantiated when our process starts.
 * At the moment the only reason for the existence of this class is to initialize
 * with the application context so that the class can (later) instantiate
 * the various objects it owns.
 * <p>
 * Also restrict UNIX file permissions on application's persistent data directory to owner
 * (this app's UID) only.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
public class AuthenticatorApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        x.Ext.init(this);

        // Try to restrict data dir file permissions to owner (this app's UID) only. This mitigates the
        // security vulnerability where SQLite database transaction journals are world-readable.
        // NOTE: This also prevents all files in the data dir from being world-accessible, which is fine
        // because this application does not need world-accessible files.
        try {
            FileUtilities.restrictAccessToOwnerOnly(
                    getApplicationContext().getApplicationInfo().dataDir);
        } catch (Throwable e) {
            // Ignore this exception and don't log anything to avoid attracting attention to this fix
        }
    }

    /**
     * 当终止应用程序对象时调用，不保证一定被调用，当程序是被内核终止以便为其他应用程序释放资源
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
