/*
 * Copyright 2009 Google Inc. All Rights Reserved.
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

package com.google.android.apps.authenticator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.android.apps.authenticator.dao.AccountInfoDao;
import com.google.android.apps.authenticator.entity.OtpType;
import com.google.android.apps.authenticator.entity.AccountInfo;
import com.google.android.apps.authenticator.utils.Base32Utils;
import com.google.android.apps.authenticator.utils.Base32Utils.DecodingException;
import com.google.android.apps.authenticator.engine.PasscodeGenerator;
import com.google.android.apps.authenticator2.R;

import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The activity that displays the integrity check value for a key.
 * The user is passed in via the extra bundle in "user".
 *
 * @author sweis@google.com (Steve Weis)
 */
public class CheckCodeActivity extends BaseActivity {
    private TextView mCheckCodeTextView;
    private TextView mCodeTextView;
    private TextView mCounterValue;
    AccountInfoDao accountInfoDao;

    @Override
    protected void setContentView(Bundle savedInstanceState) {
        setContentView(R.layout.check_code);
    }

    private String user;

    @Override
    protected void getBundleExtra() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        //传过来的用户
        user = extras.getString("user");
    }

    @Override
    protected void initViews() {
        mCodeTextView = (TextView) findViewById(R.id.code_value);
        mCheckCodeTextView = (TextView) findViewById(R.id.check_code);
        mCounterValue = (TextView) findViewById(R.id.counter_value);
        findViewById(R.id.code_area).setVisibility(View.VISIBLE);
    }

    @Override
    protected void initListeners() {
        accountInfoDao = new AccountInfoDao();
        OtpType type = accountInfoDao.getType(user);

        AccountInfo accountInfo = accountInfoDao.getAccount(user);
        if (type == OtpType.HOTP) {
            mCounterValue.setText(accountInfo.getCounter() + "");
            findViewById(R.id.counter_area).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.counter_area).setVisibility(View.GONE);
        }
        //密码
        String secret = accountInfo.getSecret();
        String checkCode = null;
        String errorMessage = null;
        try {
            //生成随机码
            checkCode = getCheckCode(secret);
        } catch (GeneralSecurityException e) {
            errorMessage = getString(R.string.general_security_exception);
        } catch (DecodingException e) {
            errorMessage = getString(R.string.decoding_exception);
        }
        if (errorMessage != null) {
            mCheckCodeTextView.setText(errorMessage);
            return;
        }
        mCodeTextView.setText(checkCode);
        String checkCodeMessage = String.format(getString(R.string.check_code),
                TextUtils.htmlEncode(user));
        CharSequence styledCheckCode = Html.fromHtml(checkCodeMessage);
        mCheckCodeTextView.setText(styledCheckCode);
        mCheckCodeTextView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initData() {

    }

    /**
     * 生成随机吗
     *
     * @param secret
     * @return
     * @throws GeneralSecurityException
     * @throws DecodingException
     */
    private static String getCheckCode(String secret) throws GeneralSecurityException,
            DecodingException {
        final byte[] keyBytes = Base32Utils.decode(secret);
        Mac mac = Mac.getInstance("HMACSHA1");
        mac.init(new SecretKeySpec(keyBytes, ""));
        PasscodeGenerator pcg = new PasscodeGenerator(mac);
        return pcg.generateResponseCode(0L);
    }

}
