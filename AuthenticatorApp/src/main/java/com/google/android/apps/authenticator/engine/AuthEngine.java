package com.google.android.apps.authenticator.engine;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.authenticator.config.AuthenticConfig;
import com.google.android.apps.authenticator.dao.AccountInfoDao;
import com.google.android.apps.authenticator.entity.AccountInfo;
import com.google.android.apps.authenticator.entity.OtpType;
import com.google.android.apps.authenticator.entity.PinInfo;
import com.google.android.apps.authenticator.utils.PINUtils;
import com.google.android.apps.authenticator2.R;

import java.util.List;

/**
 * Description: TODO <br>
 *
 * @author xwc1125 <br>
 * @version V1.0
 * @Copyright: Copyright (c) 2017 <br>
 * @date 2017/6/30  09:34 <br>
 */
public class AuthEngine {
    private final static String TAG = AuthEngine.class.getSimpleName();

    /**
     * Computes the PIN and saves it in mUsers. This currently runs in the UI
     * thread so it should not take more than a second or so. If necessary, we can
     * move the computation to a background thread.
     *
     * @param user        the user email to display with the PIN
     * @param position    the index for the screen of this user and PIN
     * @param computeHotp true if we should increment counter and display new hotp
     */
    public static void computeAndDisplayPin(Context context, List<PinInfo> mUsers, String user, int position,
                                            boolean computeHotp) throws OtpSourceException {
        AccountInfoDao accountInfoDao = new AccountInfoDao();
        PinInfo currentPin;
        if (mUsers == null || mUsers.size() == 0) {
            return;
        }
        if (position > mUsers.size()) {
            // 数据未更新
            Log.e("authengine:", "数据未更新");
            return;
        }
        if (mUsers.get(position) != null) {
            currentPin = mUsers.get(position); // existing PinInfo, so we'll update it
        } else {
            currentPin = new PinInfo();
            currentPin.pin = context.getString(R.string.empty_pin);
            currentPin.hotpCodeGenerationAllowed = true;
        }

        OtpType type = accountInfoDao.getType(user);
        currentPin.isHotp = (type == OtpType.HOTP);

        currentPin.user = user;

        if (!currentPin.isHotp || computeHotp) {
            // Always safe to recompute, because this code path is only
            // reached if the account is:
            // - Time-based, in which case getNextCode() does not change state.
            // - Counter-based (HOTP) and computeHotp is true.
            currentPin.pin = PINUtils.getNextCode(context, user);
            currentPin.hotpCodeGenerationAllowed = true;
        }
        mUsers.remove(position);
        mUsers.add(position, currentPin);
    }

    /**
     * Saves the secret key to local storage on the phone.
     *
     * @param user         the user email address. When editing, the new user email.
     * @param secret       the secret key
     * @param originalUser If editing, the original user email, otherwise null.
     * @param type         hotp vs totp
     * @param counter      only important for the hotp type
     * @return {@code true} if the secret was saved, {@code false} otherwise.
     */
    public static boolean saveSecret(Context context, String user, String secret,
                                     String originalUser, OtpType type, Integer counter) {
        AccountInfoDao accountInfoDao = new AccountInfoDao();
        if (originalUser == null) {  // new user account
            originalUser = user;
        }
        if (secret != null) {
            AccountInfo accountInfo = accountInfoDao.getAccount(user);
            if (accountInfo == null) {
                accountInfo = new AccountInfo();
            }
            accountInfo.setEmail(user);
            accountInfo.setSecret(secret);
            accountInfo.setType(type.value);
            accountInfo.setCounter(counter);
            accountInfoDao.save(accountInfo);

            Toast.makeText(context, R.string.secret_saved, Toast.LENGTH_LONG).show();
            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                    .vibrate(AuthenticConfig.VIBRATE_DURATION);
            return true;
        } else {
            Log.e(TAG, "Trying to save an empty secret key");
            Toast.makeText(context, R.string.error_empty_secret, Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
