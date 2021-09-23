package org.conjur.jenkins.jwtauth.impl;


import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import org.conjur.jenkins.jwtauth.JwtAuthenticationService;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;

/**
 * Default implementation of {@link JwtAuthenticationService}
 *
 * @author Vivek Pandey
 */
@Extension
public class JwtAuthenticationServiceImpl extends JwtAuthenticationService {
    private static final Logger LOGGER = Logger.getLogger(JwtAuthenticationServiceImpl.class.getName());

    private static int DEFAULT_EXPIRY_IN_SEC = 1800;
    private static int DEFAULT_MAX_EXPIRY_TIME_IN_MIN = 480;
    private static int DEFAULT_NOT_BEFORE_IN_SEC = 30;

    @Override
    public String getJwkSet() throws HttpRequestMethodNotSupportedException {
        LOGGER.log(Level.INFO, "Getting JwkSet");

        GlobalConjurConfiguration result = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
        if (result == null || !result.getEnableJWKS()) {
            throw new HttpRequestMethodNotSupportedException("conjur-jwk-set");
        }

        // RSAPublicKey k = InstanceIdentity.get().getPublic();

        // JSONObject jwks = new JSONObject();
        // JSONArray keys = new JSONArray();

        // JSONObject jwk = new JSONObject();
        // jwk.put("kty", "RSA");
        // jwk.put("alg", AlgorithmIdentifiers.RSA_USING_SHA256);
        // jwk.put("kid", "1234567890");
        // jwk.put("use", "sig");
        // jwk.put("key_ops", Collections.singleton("verify"));
        // jwk.put("n", Base64.getUrlEncoder().withoutPadding().encodeToString(k.getModulus().toByteArray()));
        // jwk.put("e", Base64.getUrlEncoder().withoutPadding().encodeToString(k.getPublicExponent().toByteArray()));

        // keys.put(jwk);

        // jwks.put("keys", keys);
        // LOGGER.log(Level.INFO, "returning " + jwk.toString(4));
        return JwtToken.getJwkset().toString(4);
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Conjur Jwt endpoint";
    }

}

