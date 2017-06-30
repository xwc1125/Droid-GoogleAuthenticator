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

import com.google.android.apps.authenticator.engine.AuthEngine;
import com.google.android.apps.authenticator.entity.OtpType;
import com.google.android.apps.authenticator.utils.Base32Utils;
import com.google.android.apps.authenticator.utils.Base32Utils.DecodingException;
import com.google.android.apps.authenticator2.R;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * 手动输入密码和账户
 */
public class EnterKeyActivity extends BaseActivity {
    /**
     * 密码长度
     */
    private static final int MIN_KEY_BYTES = 10;
    /**
     * 密码
     */
    private EditText mKeyEntryField;
    /**
     * 用户名
     */
    private EditText mAccountName;
    /**
     * 密码类型选择
     */
    private Spinner mType;

    private Button btn_save;

    @Override
    protected void setContentView(Bundle savedInstanceState) {
        setContentView(R.layout.enter_key);
    }

    @Override
    protected void getBundleExtra() {

    }

    @Override
    protected void initViews() {
        mKeyEntryField = (EditText) findViewById(R.id.key_value);
        mAccountName = (EditText) findViewById(R.id.account_name);
        mType = (Spinner) findViewById(R.id.type_choice);
        btn_save = (Button) findViewById(R.id.btn_save);

        ArrayAdapter<CharSequence> types = ArrayAdapter.createFromResource(this,
                R.array.type, android.R.layout.simple_spinner_item);
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mType.setAdapter(types);
    }

    @Override
    protected void initListeners() {
        // Set listeners
        mKeyEntryField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validateKeyAndUpdateStatus(mKeyEntryField, false, MIN_KEY_BYTES);
            }
        });
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取选择的密码生成类型，并调用生成方法
                OtpType mode = mType.getSelectedItemPosition() == OtpType.TOTP.value ?
                        OtpType.TOTP :
                        OtpType.HOTP;
                if (validateKeyAndUpdateStatus(mKeyEntryField, true, MIN_KEY_BYTES)) {
                    AuthEngine.saveSecret(activity,
                            mAccountName.getText().toString(),
                            getEnteredKey(mKeyEntryField.getText().toString()),
                            null,
                            mode,
                            0);
                }
                EnterKeyActivity.this.finish();
            }

        });
    }

    @Override
    protected void initData() {

    }

    /**
     * 判断密码是否符合要求
     *
     * @param ev_key        密码输入框
     * @param isSubmit      是否是提交事件
     * @param MIN_KEY_BYTES 密码最小长度
     * @return
     */
    private boolean validateKeyAndUpdateStatus(EditText ev_key, boolean isSubmit, int MIN_KEY_BYTES) {
        String userEnteredKey = getEnteredKey(ev_key.getText().toString());
        try {
            byte[] decoded = Base32Utils.decode(userEnteredKey);
            if (decoded.length < MIN_KEY_BYTES) {
                // If the user is trying to submit a key that's too short, then
                // display a message saying it's too short.
                ev_key.setError(isSubmit ? getString(R.string.enter_key_too_short) : null);
                return false;
            } else {
                ev_key.setError(null);
                return true;
            }
        } catch (DecodingException e) {
            ev_key.setError(getString(R.string.enter_key_illegal_char));
            return false;
        }
    }

    /**
     * 获取输入的密码。将易认错的1和0全部替换成I和O。
     *
     * @param enteredKey 输入的密码。
     * @return
     */
    private String getEnteredKey(String enteredKey) {
        return enteredKey.replace('1', 'I').replace('0', 'O');
    }
}
