package org.conjur.jenkins.jwtauth.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.Authentication;
import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.lang.JoseException;
import org.json.JSONArray;
import org.json.JSONObject;

import hudson.model.AbstractItem;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.User;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

public class JwtToken {
    private static final Logger LOGGER = Logger.getLogger(JwtToken.class.getName());

    private static int DEFAULT_NOT_BEFORE_IN_SEC = 30;

    public static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("MMddkkmmss")
        .withZone(ZoneId.systemDefault());


    private static Queue<JwtRsaDigitalSignatureKey> keysQueue = new LinkedList<JwtRsaDigitalSignatureKey>();

    /**
     * JWT Claim
     */
    public final JSONObject claim = new JSONObject();

    /**
     * Generates base64 representation of JWT token sign using "RS256" algorithm
     *
     * getHeader().toBase64UrlEncode() + "." + getClaim().toBase64UrlEncode() + "." + sign
     *
     * @return base64 representation of JWT token
     */
    public String sign() {
        LOGGER.log(Level.FINE, "Signing Token");
        try {
            JsonWebSignature jsonWebSignature = new JsonWebSignature();
            // RSAPrivateKey k = InstanceIdentity.get().getPrivate();
            JwtRsaDigitalSignatureKey key = getCurrentSigningKey(this);
            jsonWebSignature.setPayload(claim.toString());
            jsonWebSignature.setKey(key.toSigningKey());
            jsonWebSignature.setKeyIdHeaderValue(key.getId());
            jsonWebSignature.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
            jsonWebSignature.setHeader(HeaderParameterNames.TYPE, "JWT");
            // LOGGER.log(Level.FINEST, "Return: " + jsonWebSignature.getCompactSerialization());
            return jsonWebSignature.getCompactSerialization();
        } catch (JoseException e) {
            String msg = "Failed to sign JWT token: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "Failed to sign JWT token", e);
            throw new RuntimeException(msg, e);
        }

    }
    public static String getToken(Object context) {
        return getToken("SecretRetrieval", context);
    }
 
    public static String getToken(String pluginAction, Object context) {
        LOGGER.log(Level.FINE, "***** Getting Token");
        JwtToken unsignedToken = getUnsignedToken(pluginAction, context);
        LOGGER.log(Level.FINEST, "Claims:\n{0}", unsignedToken.claim.toString(4));
        return unsignedToken.sign();
    }

    public static JwtToken getUnsignedToken(String pluginAction, Object context) {
        GlobalConjurConfiguration globalConfig = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
        if (globalConfig == null || !globalConfig.getEnableJWKS()) {
            LOGGER.log(Level.FINE, "No JWT Authentication");
            return null;
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
        String issuer = Jenkins.get().getRootUrl();
        if (issuer.substring(issuer.length() - 1).equals("/")) {
            issuer = issuer.substring(0, issuer.length() - 1);
        }
        LOGGER.log(Level.FINEST, "RootURL => {0}", Jenkins.get().getRootUrl());

        JwtToken jwtToken = new JwtToken();
        jwtToken.claim.put("jti", UUID.randomUUID().toString().replace("-",""));
        jwtToken.claim.put("aud", globalConfig.getJwtAudience());
        jwtToken.claim.put("iss", issuer);
        jwtToken.claim.put("sub", userId);
        jwtToken.claim.put("name", fullName);
        long currentTime = System.currentTimeMillis()/1000;
        jwtToken.claim.put("iat", currentTime);
        jwtToken.claim.put("exp", currentTime + GlobalConjurConfiguration.get().getTokenDurarionInSeconds());
        jwtToken.claim.put("nbf", currentTime - DEFAULT_NOT_BEFORE_IN_SEC);

        LOGGER.log(Level.FINE, "Context => " + context);

        ModelObject contextObject = (ModelObject) context; 

        if (contextObject instanceof Run) {
            Run run = (Run) contextObject;
            jwtToken.claim.put("jenkins_build_number", run.getNumber());
            contextObject = run.getParent();
        }

        if (contextObject instanceof AbstractItem) {

            if (contextObject instanceof Job) {
                Job job = (Job) contextObject;
                jwtToken.claim.put("jenkins_pronoun", job.getPronoun());
            }

            AbstractItem item = (AbstractItem) contextObject;
            jwtToken.claim.put("jenkins_full_name", item.getFullName());
            jwtToken.claim.put("jenkins_name", item.getName());
            jwtToken.claim.put("jenkins_task_noun", item.getTaskNoun());
            if (item instanceof ItemGroup) {
                ItemGroup itemGroup = (ItemGroup) item;
                jwtToken.claim.put("jenkins_url_child_prefix", itemGroup.getUrlChildPrefix());
            }
            if (item instanceof Job) {
                Job job = (Job) item;
                jwtToken.claim.put("jenkins_job_buildir", job.getBuildDir().getAbsolutePath());
            }

            ItemGroup parent = item.getParent();
            if (parent != null && parent instanceof AbstractItem) {
                item =  (AbstractItem) parent;
                jwtToken.claim.put("jenkins_parent_full_name", item.getFullName());
                jwtToken.claim.put("jenkins_parent_name", item.getName());
                jwtToken.claim.put("jenkins_parent_task_noun", item.getTaskNoun());
                if (item instanceof ItemGroup) {
                    ItemGroup itemGroup = (ItemGroup) item;
                    jwtToken.claim.put("jenkins_parent_url_child_prefix", itemGroup.getUrlChildPrefix());
                }
                if (item instanceof Job) {
                    Job job = (Job) item;
                    jwtToken.claim.put("jenkins_parent_pronoun", job.getPronoun());
                }
            }

            // Add identity field
            List<String> identityFields = Arrays.asList(globalConfig.getIdentityFormatFieldsFromToken().split(","));
            String fieldSeparator = globalConfig.getIdentityFieldsSeparator();
            StringBuffer identityValue = new StringBuffer();
            for (String identityField : identityFields) {
                if (jwtToken.claim.has(identityField)) {
                    String fieldValue = jwtToken.claim.getString(identityField);
                    if (identityValue.length() != 0) identityValue.append(fieldSeparator);
                    identityValue.append(fieldValue);
                }
            }
            if (identityValue.length() > 0) jwtToken.claim.put(globalConfig.getidentityFieldName(), identityValue);

        }
        return jwtToken;
    }

    protected static JwtRsaDigitalSignatureKey getCurrentSigningKey(JwtToken jwtToken) {

        JwtRsaDigitalSignatureKey result = null; 
        long currentTime = System.currentTimeMillis()/1000;
        long max_key_time_in_sec = GlobalConjurConfiguration.get().getKeyLifetimeInMinutes() * 60;

        //access via Iterator
        Iterator<JwtRsaDigitalSignatureKey> iterator = keysQueue.iterator();
        while(iterator.hasNext()) {
            JwtRsaDigitalSignatureKey key = iterator.next();
            // LOGGER.log(Level.FINE, "currentTime: " + currentTime + " creationTime: " + key.getCreationTime() + " max_key_time_in_sec: " + max_key_time_in_sec + " exp: " + jwtToken.claim.getLong("exp"));
            if (currentTime - key.getCreationTime() < max_key_time_in_sec) {
                if (key.getCreationTime() + max_key_time_in_sec > jwtToken.claim.getLong("exp")) {
                    result = key;
                    break;
                }
            } else {
                iterator.remove();
            }
        }

        if (result == null) {
            String id = ID_FORMAT.format(Instant.now());
            result = new JwtRsaDigitalSignatureKey(id);
            keysQueue.add(result);
        }

        return result;
    }

    protected static JSONObject getJwkset() {

        // RSAPublicKey k = InstanceIdentity.get().getPublic();

        JSONObject jwks = new JSONObject();
        JSONArray keys = new JSONArray();

        long currentTime = System.currentTimeMillis()/1000;
        long max_key_time_in_sec = GlobalConjurConfiguration.get().getKeyLifetimeInMinutes() * 60;

        //access via Iterator
        Iterator<JwtRsaDigitalSignatureKey> iterator = keysQueue.iterator();
        while(iterator.hasNext()) {
            JwtRsaDigitalSignatureKey key = iterator.next();
            // LOGGER.log(Level.FINE, "currentTime: " + currentTime + " creationTime: " + key.getCreationTime());
            if (currentTime - key.getCreationTime() < max_key_time_in_sec) {
                JSONObject jwk = new JSONObject();
                jwk.put("kty", "RSA");
                jwk.put("alg", AlgorithmIdentifiers.RSA_USING_SHA256);
                jwk.put("kid", key.getId());
                jwk.put("use", "sig");
                jwk.put("key_ops", Collections.singleton("verify"));
                jwk.put("n", Base64.getUrlEncoder().withoutPadding().encodeToString(key.getPublicKey().getModulus().toByteArray()));
                jwk.put("e", Base64.getUrlEncoder().withoutPadding().encodeToString(key.getPublicKey().getPublicExponent().toByteArray()));
                keys.put(jwk);
        
            } else {
                iterator.remove();
            }
        }

        jwks.put("keys", keys);
        
        return jwks;
    }

}
