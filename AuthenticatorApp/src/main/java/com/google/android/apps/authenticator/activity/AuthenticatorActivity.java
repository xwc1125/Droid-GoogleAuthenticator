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

import com.google.android.apps.authenticator.dao.AccountInfoDao;
import com.google.android.apps.authenticator.engine.AuthEngine;
import com.google.android.apps.authenticator.entity.OtpType;
import com.google.android.apps.authenticator.entity.AccountInfo;
import com.google.android.apps.authenticator.entity.PinInfo;
import com.google.android.apps.authenticator.entity.SaveKeyDialogParams;
import com.google.android.apps.authenticator.engine.OnRefreshPINClickListener;
import com.google.android.apps.authenticator.utils.SignAuthUtils;
import com.google.android.apps.authenticator.other.setting.SettingsActivity;
import com.google.android.apps.authenticator.ui.CountdownIndicator;
import com.google.android.apps.authenticator.engine.OtpSourceException;
import com.google.android.apps.authenticator.engine.TimeotpClock;
import com.google.android.apps.authenticator.engine.TimeotpCountdownTask;
import com.google.android.apps.authenticator.engine.TimeotpCounter;
import com.google.android.apps.authenticator.utils.Utilities;
import com.google.android.apps.authenticator2.R;
import com.xwc1125.droidui.recyclerview.adapter.DroidRecyclerAdapter;
import com.xwc1125.droidui.recyclerview.event.DroidRecylerDecoration;
import com.xwc1125.droidui.recyclerview.listener.DroidItemClickListener;
import com.xwc1125.droidui.recyclerview.listener.DroidItemLongClickListener;
import com.xwc1125.droidui.recyclerview.listener.DroidRecyclerBindViewListener;
import com.xwc1125.droidui.recyclerview.viewholder.DroidRecyclerViewHolder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.ClipboardManager;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.webkit.WebView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.google.android.apps.authenticator.config.AuthenticConfig.PIN_TEXT_SCALEX_NORMAL;
import static com.google.android.apps.authenticator.config.AuthenticConfig.PIN_TEXT_SCALEX_UNDERSCORE;

/**
 * Class: AuthenticatorActivity<br>
 * Description: 密码计算器显示界面
 *
 * @author xwc1125<br>
 * @version V1.0
 * @Copyright: Copyright (c) 2017/6/30<br>
 * @date 2017/6/30 18:26<br>
 */
public class AuthenticatorActivity extends BaseActivity {
    private static final String TAG = AuthenticatorActivity.class.getSimpleName();

    /**
     * 计时器更新的频率（毫秒）
     */
    private static final long TOTP_COUNTDOWN_REFRESH_PERIOD = 100;

    // @VisibleForTesting
    public static final int DIALOG_ID_SAVE_KEY = 13;

    private View mContentNoAccounts;
    private View mContentAccountsPresent;
    private TextView mEnterPinPrompt;
    private RecyclerView mUserList;
    private List<PinInfo> itemList = new ArrayList<>();

    /**
     * 用于生成TOTP验证码计数器
     */
    private TimeotpCounter mTimeotpCounter;

    /**
     * 用于生成TOTP验证码时钟
     */
    private TimeotpClock mTimeotpClock;
    /**
     * 任务：用于更新UI
     * <p>
     * 任务，定期通知这个活动保持直到TOTP代码刷新时间。任务还通知这个活动时，TOTP码刷新。
     */
    private TimeotpCountdownTask mTimeotpCountdownTask;
    /**
     * 倒计时指数，值在0到1之间
     * <p>
     * Phase of TOTP countdown indicators. The phase is in {@code [0, 1]} with {@code 1} meaning
     * full time step remaining until the code refreshes, and {@code 0} meaning the code is refreshing
     * right now.
     */
    private double mTotpCountdownPhase;

    /**
     * Key under which the {@link #mSaveKeyDialogParams} is stored in the instance state
     * {@link Bundle}.
     */
    private static final String KEY_SAVE_KEY_DIALOG_PARAMS = "saveKeyDialogParams";

    /**
     * Parameters to the save key dialog (DIALOG_ID_SAVE_KEY).
     * <p>
     * <p>
     * Note: this field is persisted in the instance state {@link Bundle}. We need to resolve to this
     * error-prone mechanism because showDialog on Eclair doesn't take parameters. Once Froyo is
     * the minimum targetted SDK, this contrived code can be removed.
     */
    private SaveKeyDialogParams mSaveKeyDialogParams;

    /**
     * Whether this activity is currently displaying a confirmation prompt in response to the
     * "save key" Intent.
     */
    private boolean mSaveKeyIntentConfirmationInProgress;

    private static final String OTP_SCHEME = "otpauth";
    private static final String TOTP = "totp"; // time-based
    private static final String HOTP = "hotp"; // counter-based
    private static final String SECRET_PARAM = "secret";
    private static final String COUNTER_PARAM = "counter";
    // @VisibleForTesting
    public static final int CHECK_KEY_VALUE_ID = 0;
    // @VisibleForTesting
    public static final int RENAME_ID = 1;
    // @VisibleForTesting
    public static final int REMOVE_ID = 2;
    // @VisibleForTesting
    public static final int COPY_TO_CLIPBOARD_ID = 3;
    // @VisibleForTesting
    public static final int SCAN_REQUEST = 31337;

    private static AccountInfoDao accountInfoDao;


    @Override
    protected void setContentView(Bundle savedInstanceState) {
        setContentView(R.layout.main);
        // restore state on screen rotation
        Object savedState = getLastNonConfigurationInstance();//横竖屏切换数据保存

        if (savedState != null) {
            itemList = (List<PinInfo>) savedState;
            // Re-enable the Get Code buttons on all HOTP accounts, otherwise they'll stay disabled.
            for (PinInfo account : itemList) {
                if (account.isHotp) {
                    account.hotpCodeGenerationAllowed = true;
                }
            }
        }

        if (savedInstanceState != null) {
            mSaveKeyDialogParams =
                    (SaveKeyDialogParams) savedInstanceState.getSerializable(KEY_SAVE_KEY_DIALOG_PARAMS);
        }
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void getBundleExtra() {
        accountInfoDao = new AccountInfoDao();
        //初始化数据
        List<AccountInfo> list = accountInfoDao.getAccountList();
        if (list != null && list.size() > 0) {
            for (int i = 0, len = list.size(); i < len; i++) {
                AccountInfo authAccountEntity = list.get(i);
                PinInfo pinInfo = new PinInfo();
                pinInfo.user = authAccountEntity.getEmail();
                pinInfo.pin = getString(R.string.empty_pin);
                OtpType type = OtpType.getEnum(authAccountEntity.getType());
                pinInfo.isHotp = (type == OtpType.HOTP);
                if (pinInfo.isHotp) {
                    pinInfo.hotpCodeGenerationAllowed = true;
                }
                itemList.add(pinInfo);
            }
        }

    }

    @Override
    protected void initViews() {
        setTitle(R.string.app_name);
        mUserList = (RecyclerView) findViewById(R.id.user_list);
        mContentNoAccounts = findViewById(R.id.content_no_accounts);
        mContentAccountsPresent = findViewById(R.id.content_accounts_present);
        mContentNoAccounts.setVisibility((itemList.size() > 0) ? View.GONE : View.VISIBLE);
        mContentAccountsPresent.setVisibility((itemList.size() > 0) ? View.VISIBLE : View.GONE);
        TextView noAccountsPromptDetails = (TextView) findViewById(R.id.details);
        noAccountsPromptDetails.setText(
                Html.fromHtml(getString(R.string.welcome_page_details)));
        findViewById(R.id.add_account_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAccount();
            }
        });
        mEnterPinPrompt = (TextView) findViewById(R.id.enter_pin_prompt);
    }

    @Override
    protected void initListeners() {
        mTimeotpCounter = TimeotpCounter.getInstance(30);
        mTimeotpClock = TimeotpClock.getInstance(activity);
    }

    @Override
    protected void initData() {
        bindAdapter(mUserList);
        mUserList.setVisibility(View.GONE);
        mUserList.setAdapter(adapter);
    }

    /**
     * Reacts to the {@link Intent} that started this activity or arrived to this activity without
     * restarting it (i.e., arrived via {@link #onNewIntent(Intent)}). Does nothing if the provided
     * intent is {@code null}.
     */
    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (intent.getData() != null) {
            interpretScanResult(intent.getData(), true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_SAVE_KEY_DIALOG_PARAMS, mSaveKeyDialogParams);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return itemList;  // save state of users and currently displayed PINs
    }

    // Because this activity is marked as singleTop, new launch intents will be
    // delivered via this API instead of onResume().
    // Override here to catch otpauth:// URL being opened from QR code reader.
    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(getString(R.string.app_name), TAG + ": onNewIntent");
        handleIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateCodesAndStartTotpCountdownTask();
    }

    @Override
    protected void onStop() {
        stopTotpCountdownTask();
        super.onStop();
    }

    /**
     * 启动计数器
     */
    private void updateCodesAndStartTotpCountdownTask() {
        stopTotpCountdownTask();

        //启动任务
        mTimeotpCountdownTask =
                new TimeotpCountdownTask(mTimeotpCounter, mTimeotpClock, TOTP_COUNTDOWN_REFRESH_PERIOD);
        mTimeotpCountdownTask.setListener(new TimeotpCountdownTask.Listener() {
            @Override
            public void onTotpCountdown(long millisRemaining) {
                if (isFinishing()) {
                    // No need to reach to this even because the Activity is finishing anyway
                    return;
                }
                //计数方式
                setTotpCountdownPhaseFromTimeTillNextValue(millisRemaining);
            }

            @Override
            public void onTotpCounterValueChanged() {
                if (isFinishing()) {
                    // No need to reach to this even because the Activity is finishing anyway
                    return;
                }
                //计时
                refreshVerificationCodes();
            }
        });

        mTimeotpCountdownTask.startAndNotifyListener();
    }

    private void stopTotpCountdownTask() {
        if (mTimeotpCountdownTask != null) {
            mTimeotpCountdownTask.stop();
            mTimeotpCountdownTask = null;
        }
    }

    /**
     * Display list of user emails and updated pin codes.
     */
    protected void refreshUserList() {
        refreshUserList(false);
    }

    /**
     * 设置计时器
     *
     * @param phase
     */
    private void setTotpCountdownPhase(double phase) {
        mTotpCountdownPhase = phase;
        updateCountdownIndicators();
    }

    /**
     * 下一次出现新密码的时间间隔
     *
     * @param millisRemaining
     */
    private void setTotpCountdownPhaseFromTimeTillNextValue(long millisRemaining) {
        setTotpCountdownPhase(
                ((double) millisRemaining) / Utilities.secondsToMillis(mTimeotpCounter.getTimeStep()));
    }

    private void refreshVerificationCodes() {
        refreshUserList();//更新用户列表
        setTotpCountdownPhase(1.0);
    }

    /**
     * 更新计时器
     */
    private void updateCountdownIndicators() {
        for (int i = 0, len = mUserList.getChildCount(); i < len; i++) {
            View listEntry = mUserList.getChildAt(i);
            //计时器
            CountdownIndicator indicator =
                    (CountdownIndicator) listEntry.findViewById(R.id.countdown_icon);
            if (indicator != null) {
                indicator.setPhase(mTotpCountdownPhase);
            }
        }
    }

    /**
     * Display list of user emails and updated pin codes.
     *
     * @param isAccountModified if true, force full refresh
     */
    // @VisibleForTesting
    public void refreshUserList(boolean isAccountModified) {
        List<AccountInfo> entities = accountInfoDao.getAccountList();
        if (entities == null || entities.size() == 0) {
            return;
        }
        int userCount = entities.size();

        if (userCount > 0) {
            boolean newListRequired = isAccountModified || itemList.size() != userCount;

            for (int i = 0; i < userCount; ++i) {
                String user = entities.get(i).getEmail();
                try {
                    AuthEngine.computeAndDisplayPin(AuthenticatorActivity.this, itemList, user, i, false);
                } catch (OtpSourceException ignored) {
                }
            }

            if (newListRequired) {
                bindAdapter(mUserList);
            }
            adapter.notifyDataSetChanged();

            if (mUserList.getVisibility() != View.VISIBLE) {
                mUserList.setVisibility(View.VISIBLE);
                registerForContextMenu(mUserList);
            }
        } else {
            itemList = new ArrayList<>();
            mUserList.setVisibility(View.GONE);
        }

        // Display the list of accounts if there are accounts, otherwise display a
        // different layout explaining the user how this app works and providing the user with an easy
        // way to add an account.
        mContentNoAccounts.setVisibility((itemList.size() > 0) ? View.GONE : View.VISIBLE);
        mContentAccountsPresent.setVisibility((itemList.size() > 0) ? View.VISIBLE : View.GONE);
    }

    DroidRecyclerAdapter adapter;

    private void bindAdapter(RecyclerView recyclerView) {
        adapter = new DroidRecyclerAdapter<PinInfo>(this, itemList, R.layout.user_row, new DroidRecyclerBindViewListener() {
            @Override
            public void onBindViewHolder(DroidRecyclerViewHolder holder, View itemView, List list, int position) {
                PinInfo currentPin = itemList.get(position);
                TextView pinView = (TextView) itemView.findViewById(R.id.pin_value);
                TextView userView = (TextView) itemView.findViewById(R.id.current_user);
                View buttonView = itemView.findViewById(R.id.next_otp);
                CountdownIndicator countdownIndicator =
                        (CountdownIndicator) itemView.findViewById(R.id.countdown_icon);

                if (currentPin.isHotp) {
                    //计数
                    buttonView.setVisibility(View.VISIBLE);
                    buttonView.setEnabled(currentPin.hotpCodeGenerationAllowed);
                    ((ViewGroup) itemView).setDescendantFocusability(
                            ViewGroup.FOCUS_BLOCK_DESCENDANTS); // makes long press work
                    OnRefreshPINClickListener clickListener = new OnRefreshPINClickListener(activity, currentPin, itemList, adapter);
                    buttonView.setOnClickListener(clickListener);
                    itemView.setTag(clickListener);

                    countdownIndicator.setVisibility(View.GONE);
                } else {
                    // TOTP, so no button needed
                    //计时
                    buttonView.setVisibility(View.GONE);
                    buttonView.setOnClickListener(null);
                    itemView.setTag(null);

                    countdownIndicator.setVisibility(View.VISIBLE);
                    countdownIndicator.setPhase(mTotpCountdownPhase);
                }

                if (activity.getString(R.string.empty_pin).equals(currentPin.pin)) {
                    pinView.setTextScaleX(PIN_TEXT_SCALEX_UNDERSCORE); // smaller gap between underscores
                } else {
                    pinView.setTextScaleX(PIN_TEXT_SCALEX_NORMAL);
                }
                pinView.setText(currentPin.pin);
                userView.setText(currentPin.user);
            }
        });

//        if (isListViewLOG) {
        //【xwc1125】如果设置成LinearLayoutManager，那么所有的item都放在一个LinearLayout中
        LinearLayoutManager nav_mg = new LinearLayoutManager(this);
        //水平或垂直摆放，可以不用 HorizontalScrollView
        nav_mg.setOrientation(LinearLayoutManager.VERTICAL);
        mUserList.setLayoutManager(nav_mg);
        mUserList.setAdapter(adapter);
//        } else {
//            //【xwc1125】所有的item放在GridLayout中
//            GridLayoutManager mg = new GridLayoutManager(this, 3);//格子摆放
//            //交错性的摆放，有点win8那种格子风格，最好使用CardView作为item，有边框和圆角
//            //StaggeredGridLayoutManager mg = new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL);
//            nav_list.setLayoutManager(mg);
//            nav_list.setAdapter(adapter);
//        }
//
        //设置事件
        RecyclerView.ItemDecoration decoration = new DroidRecylerDecoration(this);
        mUserList.addItemDecoration(decoration);
        adapter.setOnItemClickListener(new DroidItemClickListener() {
            @Override
            public void onItemClick(View view, int postion) {
                OnRefreshPINClickListener clickListener = (OnRefreshPINClickListener) view.getTag();
                View nextOtp = view.findViewById(R.id.next_otp);
                if ((clickListener != null) && nextOtp.isEnabled()) {
                    clickListener.onClick(view);
                }
                mUserList.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
            }
        });
        adapter.setOnItemLongClickListener(new DroidItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int postion) {
                mUserList.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

                    }
                });
            }
        });
    }


    /**
     * Parses a secret value from a URI. The format will be:
     * <p>
     * otpauth://totp/user@example.com?secret=FFF...
     * otpauth://hotp/user@example.com?secret=FFF...&counter=123
     *
     * @param uri               The URI containing the secret key
     * @param confirmBeforeSave a boolean to indicate if the user should be
     *                          prompted for confirmation before updating the otp
     *                          account information.
     */
    private void parseSecret(Uri uri, boolean confirmBeforeSave) {
        final String scheme = uri.getScheme().toLowerCase();
        final String path = uri.getPath();
        final String authority = uri.getAuthority();
        final String user;
        final String secret;
        final OtpType type;
        final Integer counter;

        if (!OTP_SCHEME.equals(scheme)) {
            Log.e(getString(R.string.app_name), TAG + ": Invalid or missing scheme in uri");
            showDialog(Utilities.INVALID_QR_CODE);
            return;
        }

        if (TOTP.equals(authority)) {
            type = OtpType.TOTP;
            counter = 0; // only interesting for HOTP
        } else if (HOTP.equals(authority)) {
            type = OtpType.HOTP;
            String counterParameter = uri.getQueryParameter(COUNTER_PARAM);
            if (counterParameter != null) {
                try {
                    counter = Integer.parseInt(counterParameter);
                } catch (NumberFormatException e) {
                    Log.e(getString(R.string.app_name), TAG + ": Invalid counter in uri");
                    showDialog(Utilities.INVALID_QR_CODE);
                    return;
                }
            } else {
                counter = 0;
            }
        } else {
            Log.e(getString(R.string.app_name), TAG + ": Invalid or missing authority in uri");
            showDialog(Utilities.INVALID_QR_CODE);
            return;
        }

        user = validateAndGetUserInPath(path);
        if (user == null) {
            Log.e(getString(R.string.app_name), TAG + ": Missing user id in uri");
            showDialog(Utilities.INVALID_QR_CODE);
            return;
        }

        secret = uri.getQueryParameter(SECRET_PARAM);

        if (secret == null || secret.length() == 0) {
            Log.e(getString(R.string.app_name), TAG +
                    ": Secret key not found in URI");
            showDialog(Utilities.INVALID_SECRET_IN_QR_CODE);
            return;
        }

        if (SignAuthUtils.getSigningOracle(secret) == null) {
            Log.e(getString(R.string.app_name), TAG + ": Invalid secret key");
            showDialog(Utilities.INVALID_SECRET_IN_QR_CODE);
            return;
        }
        AccountInfo accountInfo = accountInfoDao.getAccount(user);
        if (secret.equals(accountInfoDao.getSecret(user)) &&
                counter == accountInfo.getCounter() &&
                type == OtpType.getEnum(accountInfo.getType())) {
            return;  // nothing to update.
        }

        if (confirmBeforeSave) {
            mSaveKeyDialogParams = new SaveKeyDialogParams(user, secret, type, counter);
            showDialog(DIALOG_ID_SAVE_KEY);
        } else {
            saveSecretAndRefreshUserList(user, secret, null, type, counter);
        }
    }

    private static String validateAndGetUserInPath(String path) {
        if (path == null || !path.startsWith("/")) {
            return null;
        }
        // path is "/user", so remove leading "/", and trailing white spaces
        String user = path.substring(1).trim();
        if (user.length() == 0) {
            return null; // only white spaces.
        }
        return user;
    }

    /**
     * Saves the secret key to local storage on the phone and updates the displayed account list.
     *
     * @param user         the user email address. When editing, the new user email.
     * @param secret       the secret key
     * @param originalUser If editing, the original user email, otherwise null.
     * @param type         hotp vs totp
     * @param counter      only important for the hotp type
     */
    private void saveSecretAndRefreshUserList(String user, String secret,
                                              String originalUser, OtpType type, Integer counter) {
        if (AuthEngine.saveSecret(this, user, secret, originalUser, type, counter)) {
            refreshUserList(true);
        }
    }

    /**
     * Converts user list ordinal id to user email
     */
    private String idToEmail(long id) {
        return itemList.get((int) id).user;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        String user = idToEmail(info.id);
        OtpType type = accountInfoDao.getType(user);
        menu.setHeaderTitle(user);
        menu.add(0, COPY_TO_CLIPBOARD_ID, 0, R.string.copy_to_clipboard);
        // Option to display the check-code is only available for HOTP accounts.
        if (type == OtpType.HOTP) {
            menu.add(0, CHECK_KEY_VALUE_ID, 0, R.string.check_code_menu_item);
        }
        menu.add(0, RENAME_ID, 0, R.string.rename);
        menu.add(0, REMOVE_ID, 0, R.string.context_menu_remove_account);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Intent intent;
        final String user = idToEmail(info.id); // final so listener can see value
        switch (item.getItemId()) {
            //复制
            case COPY_TO_CLIPBOARD_ID:
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(itemList.get((int) info.id).pin);
                return true;
            case CHECK_KEY_VALUE_ID:
                //生成随机吗
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(this, CheckCodeActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
                return true;
            case RENAME_ID:
                //重命名id
                final Context context = this; // final so listener can see value
                final View frame = getLayoutInflater().inflate(R.layout.rename,
                        (ViewGroup) findViewById(R.id.rename_root));
                final EditText nameEdit = (EditText) frame.findViewById(R.id.rename_edittext);
                nameEdit.setText(user);
                new AlertDialog.Builder(this)
                        .setTitle(String.format(getString(R.string.rename_message), user))
                        .setView(frame)
                        .setPositiveButton(R.string.submit,
                                this.getRenameClickListener(context, user, nameEdit))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            case REMOVE_ID:
                //移除id
                // Use a WebView to display the prompt because it contains non-trivial markup, such as list
                View promptContentView =
                        getLayoutInflater().inflate(R.layout.remove_account_prompt, null, false);
                WebView webView = (WebView) promptContentView.findViewById(R.id.web_view);
                webView.setBackgroundColor(Color.TRANSPARENT);
                // Make the WebView use the same font size as for the mEnterPinPrompt field
                double pixelsPerDip =
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()) / 10d;
                webView.getSettings().setDefaultFontSize(
                        (int) (mEnterPinPrompt.getTextSize() / pixelsPerDip));
                AccountInfo accountInfo = accountInfoDao.getAccount(user);
                boolean isGoogleAccount = false;
                if (accountInfo.getProvider() == 1) {
                    isGoogleAccount = true;
                }

                Utilities.setWebViewHtml(
                        webView,
                        "<html><body style=\"background-color: transparent;\" text=\"white\">"
                                + getString(
                                isGoogleAccount
                                        ? R.string.remove_google_account_dialog_message
                                        : R.string.remove_account_dialog_message)
                                + "</body></html>");

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.remove_account_dialog_title, user))
                        .setView(promptContentView)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.remove_account_dialog_button_remove,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        accountInfoDao.delete(user);
                                        refreshUserList(true);
                                    }
                                }
                        )
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private DialogInterface.OnClickListener getRenameClickListener(final Context context,
                                                                   final String user, final EditText nameEdit) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String newName = nameEdit.getText().toString();
                if (newName != user) {
                    AccountInfo currentAccountInfo = accountInfoDao.getAccount(user);
                    AccountInfo accountInfo = accountInfoDao.getAccount(newName);
                    if (accountInfo != null) {
                        Toast.makeText(context, R.string.error_exists, Toast.LENGTH_LONG).show();
                    } else {

                        saveSecretAndRefreshUserList(newName,
                                currentAccountInfo.getSecret(), user, OtpType.getEnum(currentAccountInfo.getType()),
                                currentAccountInfo.getCounter());
                    }
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_account:
                addAccount();
                return true;
            case R.id.settings:
                showSettings();
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SCAN_REQUEST && resultCode == Activity.RESULT_OK) {
            // Grab the scan results and convert it into a URI
            String scanResult = (intent != null) ? intent.getStringExtra("SCAN_RESULT") : null;
            Uri uri = (scanResult != null) ? Uri.parse(scanResult) : null;
            //扫码的回调数据
            interpretScanResult(uri, false);
        }
    }

    private void addAccount() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(activity, EnterKeyActivity.class);
        startActivity(intent);
    }

    private void showSettings() {
        Intent intent = new Intent();
        intent.setClass(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Interprets the QR code that was scanned by the user.  Decides whether to
     * launch the key provisioning sequence or the OTP seed setting sequence.
     *
     * @param scanResult        a URI holding the contents of the QR scan result
     * @param confirmBeforeSave a boolean to indicate if the user should be
     *                          prompted for confirmation before updating the otp
     *                          account information.
     */
    private void interpretScanResult(Uri scanResult, boolean confirmBeforeSave) {
        // The scan result is expected to be a URL that adds an account.

        // If confirmBeforeSave is true, the user has to confirm/reject the action.
        // We need to ensure that new results are accepted only if the previous ones have been
        // confirmed/rejected by the user. This is to prevent the attacker from sending multiple results
        // in sequence to confuse/DoS the user.
        if (confirmBeforeSave) {
            if (mSaveKeyIntentConfirmationInProgress) {
                Log.w(TAG, "Ignoring save key Intent: previous Intent not yet confirmed by user");
                return;
            }
            // No matter what happens below, we'll show a prompt which, once dismissed, will reset the
            // flag below.
            mSaveKeyIntentConfirmationInProgress = true;
        }

        // Sanity check
        if (scanResult == null) {
            showDialog(Utilities.INVALID_QR_CODE);
            return;
        }

        // See if the URL is an account setup URL containing a shared secret
        if (OTP_SCHEME.equals(scanResult.getScheme()) && scanResult.getAuthority() != null) {
            parseSecret(scanResult, confirmBeforeSave);
        } else {
            showDialog(Utilities.INVALID_QR_CODE);
        }
    }

    /**
     * This method is deprecated in SDK level 8, but we have to use it because the
     * new method, which replaces this one, does not exist before SDK level 8
     */
    @Override
    protected Dialog onCreateDialog(final int id) {
        Dialog dialog = null;
        switch (id) {
            case DIALOG_ID_SAVE_KEY:
                final SaveKeyDialogParams saveKeyDialogParams = mSaveKeyDialogParams;
                dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.save_key_message)
                        .setMessage(saveKeyDialogParams.getUser())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        saveSecretAndRefreshUserList(
                                                saveKeyDialogParams.getUser(),
                                                saveKeyDialogParams.getSecret(),
                                                null,
                                                saveKeyDialogParams.getType(),
                                                saveKeyDialogParams.getCounter());
                                    }
                                })
                        .setNegativeButton(R.string.cancel, null)
                        .create();
                // Ensure that whenever this dialog is to be displayed via showDialog, it displays the
                // correct (latest) user/account name. If this dialog is not explicitly removed after it's
                // been dismissed, then next time showDialog is invoked, onCreateDialog will not be invoked
                // and the dialog will display the previous user/account name instead of the current one.
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeDialog(id);
                        onSaveKeyIntentConfirmationPromptDismissed();
                    }
                });
                break;

            case Utilities.INVALID_QR_CODE:
                dialog = createOkAlertDialog(R.string.error_title, R.string.error_qr,
                        android.R.drawable.ic_dialog_alert);
                markDialogAsResultOfSaveKeyIntent(dialog);
                break;

            case Utilities.INVALID_SECRET_IN_QR_CODE:
                dialog = createOkAlertDialog(
                        R.string.error_title, R.string.error_uri, android.R.drawable.ic_dialog_alert);
                markDialogAsResultOfSaveKeyIntent(dialog);
                break;
            default:
                break;
        }
        return dialog;
    }

    private void markDialogAsResultOfSaveKeyIntent(Dialog dialog) {
        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                onSaveKeyIntentConfirmationPromptDismissed();
            }
        });
    }

    /**
     * Invoked when a user-visible confirmation prompt for the Intent to add a new account has been
     * dimissed.
     */
    private void onSaveKeyIntentConfirmationPromptDismissed() {
        mSaveKeyIntentConfirmationInProgress = false;
    }

    /**
     * Create dialog with supplied ids; icon is not set if iconId is 0.
     */
    private Dialog createOkAlertDialog(int titleId, int messageId, int iconId) {
        return new AlertDialog.Builder(this)
                .setTitle(titleId)
                .setMessage(messageId)
                .setIcon(iconId)
                .setPositiveButton(R.string.ok, null)
                .create();
    }
}
