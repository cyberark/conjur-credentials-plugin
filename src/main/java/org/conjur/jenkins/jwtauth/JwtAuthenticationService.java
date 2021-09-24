package org.conjur.jenkins.jwtauth;

import java.security.interfaces.RSAPublicKey;

import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.verb.GET;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import hudson.model.UnprotectedRootAction;

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
    @WebMethod(name = "conjur-jwk-set")
    public abstract String getJwkSet() throws HttpRequestMethodNotSupportedException;
}