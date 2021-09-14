package org.conjur.jenkins.jwtauth;

import hudson.model.UnprotectedRootAction;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.verb.GET;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.security.interfaces.RSAPublicKey;

import javax.annotation.Nullable;

/**
 * JWT endpoint resource. Provides functionality to get JWT token and also provides JWK endpoint to get
 * public key using keyId.
 *
 */
public abstract class JwtAuthenticationService implements UnprotectedRootAction {

    @Override
    public String getUrlName() {
        return "jwtauth";
    }


    /**
     * Gives JWT token for authenticated user. See https://tools.ietf.org/html/rfc7519.
     *
     * @param expiryTimeInMins token expiry time. Default 30 min.
     * @param maxExpiryTimeInMins max token expiry time. Default expiry time is 8 hours (480 mins)
     *
     * @return JWT if there is authenticated user or if  anonymous user has at least READ permission, otherwise 401
     *         error code is returned
     */
    @GET
    @WebMethod(name = "token")
    public abstract JwtToken getToken(@Nullable @QueryParameter("expiryTimeInMins") Integer expiryTimeInMins,
                                          @Nullable  @QueryParameter("maxExpiryTimeInMins") Integer maxExpiryTimeInMins);

    /**
     * Binds Json web key to the URL space.
     *
     * @param keyId keyId of the key
     * @return 
     *
     * @return JWK response
     * @see <a href="https://tools.ietf.org/html/rfc7517">the spec</a>
     */
    @GET
    public  RSAPublicKey getJwks(String keyId) {
        if (keyId.equals("1234567890")) {
            return InstanceIdentity.get().getPublic();
        }
        return null;
    }

    /**
     * Binds Json web keys to the URL space.
     *
     * @return a JWKS
     * @throws HttpRequestMethodNotSupportedException
     * @see <a href="https://tools.ietf.org/html/rfc7517#page-10">the JWK Set Format spec</a>
     */
    @GET
    @WebMethod(name = "conjur-jwk-set") // we could not name this endpoint /jwks as it would be shadowing the pre-existing one
    public abstract String getJwkSet() throws HttpRequestMethodNotSupportedException;
}