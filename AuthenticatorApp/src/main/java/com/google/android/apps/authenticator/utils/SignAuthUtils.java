package com.google.android.apps.authenticator.utils;

import android.util.Log;

import com.google.android.apps.authenticator.inter.Signer;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by xwc1125 on 2017/6/29.
 */
public class SignAuthUtils {
    private final static String TAG = SignAuthUtils.class.getSimpleName();

    /**
     * 获取签名
     */
    public static Signer getSigningOracle(String secret) {
        try {
            byte[] keyBytes = decodeKey(secret);
            final Mac mac = Mac.getInstance("HMACSHA1");
            mac.init(new SecretKeySpec(keyBytes, ""));

            // Create a signer object out of the standard Java MAC implementation.
            return new Signer() {
                @Override
                public byte[] sign(byte[] data) {
                    return mac.doFinal(data);
                }
            };
        } catch (Base32Utils.DecodingException error) {
            Log.e(TAG, error.getMessage());
        } catch (NoSuchAlgorithmException error) {
            Log.e(TAG, error.getMessage());
        } catch (InvalidKeyException error) {
            Log.e(TAG, error.getMessage());
        }

        return null;
    }

    /**
     * base32签名
     */
    private static byte[] decodeKey(String secret) throws Base32Utils.DecodingException {
        return Base32Utils.decode(secret);
    }
}
