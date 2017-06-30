/*
 * Copyright 2010 Google Inc. All Rights Reserved.
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

package com.google.android.apps.authenticator.engine;

/**
 * Class containing implementation of HOTP/TOTP.
 * Generates OTP codes for one or more accounts.
 *
 * @author Steve Weis (sweis@google.com)
 * @author Cem Paya (cemp@google.com)
 */
public class OtpProvider implements OtpSource {

    /**
     * Default passcode timeout period (in seconds)
     */
    private static final int DEFAULT_INTERVAL = 30;//默认30秒更改一次

    /**
     * Counter for time-based OTPs (TOTP).
     */
    private final TimeotpCounter mTimeotpCounter;

    /**
     * Clock input for time-based OTPs (TOTP).
     */
    private final TimeotpClock mTimeotpClock;

    public OtpProvider(TimeotpClock timeotpClock) {
        this(DEFAULT_INTERVAL, timeotpClock);
    }

    public OtpProvider(int interval, TimeotpClock timeotpClock) {
        mTimeotpCounter = new TimeotpCounter(interval);//初始化事件间隔（30）
        mTimeotpClock = timeotpClock;
    }

    @Override
    public TimeotpCounter getTotpCounter() {
        return mTimeotpCounter;
    }

    @Override
    public TimeotpClock getTotpClock() {
        return mTimeotpClock;
    }

}
