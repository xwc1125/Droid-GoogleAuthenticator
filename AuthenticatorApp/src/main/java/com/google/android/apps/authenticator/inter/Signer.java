package com.google.android.apps.authenticator.inter;

import java.security.GeneralSecurityException;

/**
 * Using an interface to allow us to inject different signature
 * implementations.
 */
public interface Signer {
    /**
     * @param data Preimage to sign, represented as sequence of arbitrary bytes
     * @return Signature as sequence of bytes.
     * @throws GeneralSecurityException
     */
    byte[] sign(byte[] data) throws GeneralSecurityException;
}
