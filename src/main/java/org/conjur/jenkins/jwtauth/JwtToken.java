package org.conjur.jenkins.jwtauth;

// import io.jenkins.blueocean.commons.ServiceException;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import net.sf.json.JSONObject;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.lang.JoseException;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.Run;
import hudson.model.User;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import com.google.gson.Gson;
import hudson.model.Job;
import hudson.model.Item;
import hudson.model.ItemGroup;

import javax.servlet.ServletException;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates JWT token
 *
 * @author Vivek Pandey
 */
public class JwtToken implements HttpResponse {
    private static final Logger LOGGER = Logger.getLogger(JwtToken.class.getName());

    private static int DEFAULT_EXPIRY_IN_SEC = 1800;
    private static int DEFAULT_MAX_EXPIRY_TIME_IN_MIN = 480;
    private static int DEFAULT_NOT_BEFORE_IN_SEC = 30;

    /**
     * {@link JwtToken} is sent as HTTP header of name.
     */
    public static final String X_CONJUR_JWT="X-CONJUR-JWT";

    /**
     * JWT header
     */
    public final JSONObject header = new JSONObject();


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
        LOGGER.log(Level.INFO, "Signing Token");
        try {
            JsonWebSignature jsonWebSignature = new JsonWebSignature();
            RSAPrivateKey k = InstanceIdentity.get().getPrivate();
            jsonWebSignature.setPayload(claim.toString());
            jsonWebSignature.setKey(k);
            jsonWebSignature.setKeyIdHeaderValue("1234567890");
            jsonWebSignature.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
            jsonWebSignature.setHeader(HeaderParameterNames.TYPE, "JWT");
            LOGGER.log(Level.INFO, "Return: " + jsonWebSignature.getCompactSerialization());
            return jsonWebSignature.getCompactSerialization();
        } catch (JoseException e) {
            String msg = "Failed to sign JWT token: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "Failed to sign JWT token", e);
            throw new RuntimeException(msg, e);
        }

    }

    /**
     * Writes the token as an HTTP response.
     */
    @Override
    public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
        rsp.setStatus(204);
        /* https://httpstatuses.com/204 No Content
        The server has successfully fulfilled the request and
        that there is no additional content to send in the response payload body */
        rsp.addHeader(X_CONJUR_JWT, sign());
    }

    public static String getToken(Run<?, ?> context) {
        return getToken(context, null, null);
    }

    public static String getToken(Run<?, ?> context, Integer expiryTimeInMins, Integer maxExpiryTimeInMins) {
        LOGGER.log(Level.INFO, "***** Getting Token");

        GlobalConjurConfiguration globalConfig = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
        LOGGER.log(Level.INFO, "**** GlobalConjurConfiguration ==> " + globalConfig);
        if (globalConfig == null || !globalConfig.getEnableJWKS()) {
            LOGGER.log(Level.INFO, "No JWT Authentication");
            return null;
        }

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
        jwtToken.claim.put("aud", globalConfig.getJwtAudience());
        jwtToken.claim.put("iss", issuer);
        jwtToken.claim.put("sub", userId);
        jwtToken.claim.put("name", fullName);
        long currentTime = System.currentTimeMillis()/1000;
        jwtToken.claim.put("iat", currentTime);
        jwtToken.claim.put("exp", currentTime+expiryTime);
        jwtToken.claim.put("nbf", currentTime - DEFAULT_NOT_BEFORE_IN_SEC);

        if (context != null) {
            Job<?, ?> parent = (Job<?, ?>) context.getParent();
            LOGGER.log(Level.INFO, "To print value of context");
            LOGGER.log(Level.INFO, "Context: " + parent.getFullName());
            jwtToken.claim.put("jenkins_name", parent.getName());
            jwtToken.claim.put("jenkins_task_noun", parent.getTaskNoun());
            jwtToken.claim.put("jenkins_pronoun", parent.getPronoun());
            jwtToken.claim.put("jenkins_folder", parent.getParent().getFullName());
            jwtToken.claim.put("jenkins_full_name", parent.getFullName());
            jwtToken.claim.put("jenkins_build_directory_path", parent.getBuildDir().getAbsolutePath());
            jwtToken.claim.put("jenkins_build_number", context.getNumber());
            
            Item item = Jenkins.get().getItemByFullName(parent.getParent().getFullName());
            if (item != null) {
                jwtToken.claim.put("jenkins_url_child_prefix", ((ItemGroup<?>) item).getUrlChildPrefix());
            }
            // LOGGER.log(Level.INFO, "Context.parent: " + parent.getDescription() + " => " + gson.toJson(parent));
        } else {
            LOGGER.log(Level.INFO, "Context is NULL");
        }

        LOGGER.log(Level.INFO, "returning TOKEN ");
        return jwtToken.sign();
    }

}
