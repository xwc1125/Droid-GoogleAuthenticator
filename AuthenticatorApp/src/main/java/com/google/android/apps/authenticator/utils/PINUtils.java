package com.google.android.apps.authenticator.utils;

import android.content.Context;

import com.google.android.apps.authenticator.dao.AccountInfoDao;
import com.google.android.apps.authenticator.entity.OtpType;
import com.google.android.apps.authenticator.entity.AccountInfo;
import com.google.android.apps.authenticator.engine.PasscodeGenerator;
import com.google.android.apps.authenticator.inter.Signer;
import com.google.android.apps.authenticator.engine.OtpProvider;
import com.google.android.apps.authenticator.engine.OtpSourceException;
import com.google.android.apps.authenticator.engine.TimeotpClock;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

/**
 * Created by xwc1125 on 2017/6/29.
 */
public class PINUtils {
    private static final int PIN_LENGTH = 6; // HOTP or TOTP长度
    private static final int REFLECTIVE_PIN_LENGTH = 9; // ROTP

    /**
     * 获取随机码的核心
     * <p>
     * Computes the one-time PIN given the secret key.
     *
     * @param secret    the secret key
     * @param otp_state current token state (counter or time-interval)
     * @param challenge optional challenge bytes to include when computing passcode.
     * @return the PIN
     */
    public static String computePin(String secret, long otp_state, byte[] challenge)
            throws OtpSourceException {
        if (secret == null || secret.length() == 0) {
            throw new OtpSourceException("Null or empty secret");
        }

        try {
            Signer signer = SignAuthUtils.getSigningOracle(secret);
            PasscodeGenerator pcg = new PasscodeGenerator(signer,
                    (challenge == null) ? PIN_LENGTH : REFLECTIVE_PIN_LENGTH);

            return (challenge == null) ?
                    pcg.generateResponseCode(otp_state) :
                    pcg.generateResponseCode(otp_state, challenge);
        } catch (GeneralSecurityException e) {
            throw new OtpSourceException("Crypto failure", e);
        }
    }

    public static String getNextCode(Context context, String accountName) throws OtpSourceException {
        return getCurrentCode(context, accountName, null);
    }

    // This variant is used when an additional challenge, such as URL or
    // transaction details, are included in the OTP request.
    // The additional string is appended to standard HOTP/TOTP state before
    // applying the MAC function.
    public static String respondToChallenge(Context context, String accountName, String challenge) throws OtpSourceException {
        if (challenge == null) {
            return getCurrentCode(context, accountName, null);
        }
        try {
            byte[] challengeBytes = challenge.getBytes("UTF-8");
            return getCurrentCode(context, accountName, challengeBytes);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static String getCurrentCode(Context context, String username, byte[] challenge) throws OtpSourceException {
        // Account name is required.
        if (username == null) {
            throw new OtpSourceException("No account name");
        }

        AccountInfoDao accountInfoDao = new AccountInfoDao();

        OtpType type = accountInfoDao.getType(username);
        String secret = accountInfoDao.getSecret(username);

        long otp_state = 0;

        if (type == OtpType.TOTP) {
            // For time-based OTP, the state is derived from clock.
            OtpProvider otpProvider = new OtpProvider(TimeotpClock.getInstance(context));
            otp_state =
                    otpProvider.getTotpCounter().getValueAtTime(Utilities.millisToSeconds(otpProvider.getTotpClock().currentTimeMillis()));
        } else if (type == OtpType.HOTP) {
            // For counter-based OTP, the state is obtained by incrementing stored counter.
            AccountInfo accountInfo = accountInfoDao.getAccount(username);
            Integer counter = accountInfo.getCounter() + 1;
            accountInfo.setCounter(counter);
            accountInfoDao.save(accountInfo);
            otp_state = counter.longValue();
        }

        return PINUtils.computePin(secret, otp_state, challenge);
    }
}
