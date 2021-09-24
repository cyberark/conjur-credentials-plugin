package org.conjur.jenkins.jwtauth.impl;

import java.security.interfaces.RSAPrivateKey;

import jenkins.security.RSADigitalSignatureConfidentialKey;

/**
 * RSA key pair used to sign JWT tokens.
 *
 */
public final class JwtRsaDigitalSignatureKey extends RSADigitalSignatureConfidentialKey {
    private final String id;
    private final long creationTime;


    public JwtRsaDigitalSignatureKey(String id) {
        super("conjurJWT-" + id);
        this.id = id;
        this.creationTime = System.currentTimeMillis()/1000;

    }

    @Override
    public String getId() {
        return id;
    }

    protected long getCreationTime() {
        return creationTime;
    }

    protected RSAPrivateKey toSigningKey() {
        return getPrivateKey();
    }

}