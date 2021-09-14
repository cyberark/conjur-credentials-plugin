package org.conjur.jenkins.jwtauth.impl;


import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.kohsuke.stapler.QueryParameter;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.acegisecurity.Authentication;

import hudson.Extension;
import hudson.model.User;

import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import org.conjur.jenkins.jwtauth.JwtAuthenticationService;
import org.conjur.jenkins.jwtauth.JwtToken;

import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.json.JSONObject;
import org.json.JSONArray;

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
    public JwtToken getToken(@Nullable @QueryParameter("expiryTimeInMins") Integer expiryTimeInMins, @Nullable @QueryParameter("maxExpiryTimeInMins") Integer maxExpiryTimeInMins) {
        LOGGER.log(Level.INFO, "Getting Token");

        long expiryTime= Long.getLong("EXPIRY_TIME_IN_MINS",DEFAULT_EXPIRY_IN_SEC);

        int maxExpiryTime = Integer.getInteger("MAX_EXPIRY_TIME_IN_MINS",DEFAULT_MAX_EXPIRY_TIME_IN_MIN);

        if(maxExpiryTimeInMins != null){
            maxExpiryTime = maxExpiryTimeInMins;
        }
        if(expiryTimeInMins != null){
            if(expiryTimeInMins > maxExpiryTime) {
                throw new RuntimeException(
                    String.format("expiryTimeInMins %s can't be greater than %s", expiryTimeInMins, maxExpiryTime));
            }
            expiryTime = expiryTimeInMins * 60;
        }

        Authentication authentication = Jenkins.getAuthentication();

        String userId = authentication.getName();

        User user = User.get(userId, false, Collections.emptyMap());
        String fullName = null;
        if(user != null) {
            fullName = user.getFullName();
            userId = user.getId();
            // Mailer.UserProperty p = user.getProperty(Mailer.UserProperty.class);
            // if(p!=null)
            //     email = p.getAddress();
        }
        // Plugin plugin = Jenkins.get().getPlugin("blueocean-jwt");
        String issuer = "conjur-jwt";

        JwtToken jwtToken = new JwtToken();
        jwtToken.claim.put("jti", UUID.randomUUID().toString().replace("-",""));
        jwtToken.claim.put("iss", issuer);
        jwtToken.claim.put("sub", userId);
        jwtToken.claim.put("name", fullName);
        long currentTime = System.currentTimeMillis()/1000;
        jwtToken.claim.put("iat", currentTime);
        jwtToken.claim.put("exp", currentTime+expiryTime);
        jwtToken.claim.put("nbf", currentTime - DEFAULT_NOT_BEFORE_IN_SEC);

        //set claim
        // JSONObject userObject = new JSONObject();
        // userObject.put("id", userId);
        // userObject.put("fullName", fullName);
        // userObject.put("email", email);

        // JwtAuthenticationStore authenticationStore = getJwtStore(authentication);

        // authenticationStore.store(authentication, context);

        // context.put("user", userObject);
        jwtToken.claim.put("folder", "test");
        jwtToken.claim.put("jobName", "job01");
        jwtToken.claim.put("project_id", "26768846");
        jwtToken.claim.put("ref", "master");
        jwtToken.claim.put("project_path", "namespace1/jwt-example");

        return jwtToken;
    }

    @Override
    public String getJwkSet() throws HttpRequestMethodNotSupportedException {
        LOGGER.log(Level.INFO, "Getting JwkSet");

        GlobalConjurConfiguration result = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
        if (result == null || !result.getEnableJWKS()) {
            throw new HttpRequestMethodNotSupportedException("conjur-jwk-set");
        }

        RSAPublicKey k = InstanceIdentity.get().getPublic();

        JSONObject jwks = new JSONObject();
        JSONArray keys = new JSONArray();

        JSONObject jwk = new JSONObject();
        jwk.put("kty", "RSA");
        jwk.put("alg", AlgorithmIdentifiers.RSA_USING_SHA256);
        jwk.put("kid", "1234567890");
        jwk.put("use", "sig");
        jwk.put("key_ops", Collections.singleton("verify"));
        jwk.put("n", Base64.getUrlEncoder().withoutPadding().encodeToString(k.getModulus().toByteArray()));
        jwk.put("e", Base64.getUrlEncoder().withoutPadding().encodeToString(k.getPublicExponent().toByteArray()));

        keys.put(jwk);

        jwks.put("keys", keys);
        LOGGER.log(Level.INFO, "returning " + jwk.toString(4));
        return jwks.toString(4);
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

