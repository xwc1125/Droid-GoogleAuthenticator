package com.google.android.apps.authenticator.engine;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.google.android.apps.authenticator.config.AuthenticConfig;
import com.google.android.apps.authenticator.entity.PinInfo;
import com.google.android.apps.authenticator2.R;
import com.xwc1125.droidui.recyclerview.adapter.DroidRecyclerAdapter;

import java.util.List;

/**
 * Listener for the Button that generates the next OTP value.
 *
 * @author adhintz@google.com (Drew Hintz)
 */
public class OnRefreshPINClickListener implements View.OnClickListener {
    private static final String TAG = OnRefreshPINClickListener.class.getSimpleName();
    private final Handler mHandler = new Handler();
    private final PinInfo mAccount;
    private DroidRecyclerAdapter adapter;
    private Context context;
    private List<PinInfo> mUsers;


    public OnRefreshPINClickListener(Context context, PinInfo account, List<PinInfo> mUsers, DroidRecyclerAdapter adapter) {
        mAccount = account;
        this.adapter = adapter;
        this.context = context;
        this.mUsers = mUsers;
    }

    @Override
    public void onClick(View v) {
        int position = findAccountPositionInList();
        if (position == -1) {
            Log.e(TAG, "Account not in list: " + mAccount);
            return;
        }

        try {
            AuthEngine.computeAndDisplayPin(context, mUsers, mAccount.user, position, true);
        } catch (OtpSourceException e) {
            e.printStackTrace();
            return;
        }

        final String pin = mAccount.pin;

        // Temporarily disable code generation for this account
        mAccount.hotpCodeGenerationAllowed = false;
        adapter.notifyDataSetChanged();
        // The delayed operation below will be invoked once code generation is yet again allowed for
        // this account. The delay is in wall clock time (monotonically increasing) and is thus not
        // susceptible to system time jumps.
        mHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        mAccount.hotpCodeGenerationAllowed = true;
                        adapter.notifyDataSetChanged();
                    }
                },
                AuthenticConfig.HOTP_MIN_TIME_INTERVAL_BETWEEN_CODES);
        // The delayed operation below will hide this OTP to prevent the user from seeing this OTP
        // long after it's been generated (and thus hopefully used).
        mHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!pin.equals(mAccount.pin)) {
                            return;
                        }
                        mAccount.pin = context.getString(R.string.empty_pin);
                        adapter.notifyDataSetChanged();
                    }
                },
                AuthenticConfig.HOTP_DISPLAY_TIMEOUT);
    }

    /**
     * Gets the position in the account list of the account this listener is associated with.
     *
     * @return {@code 0}-based position or {@code -1} if the account is not in the list.
     */
    private int findAccountPositionInList() {
        for (int i = 0, len = mUsers.size(); i < len; i++) {
            if (mUsers.get(i) == mAccount) {
                return i;
            }
        }

        return -1;
    }
}

