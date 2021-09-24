package org.conjur.jenkins.jwtauth.impl;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import org.conjur.jenkins.jwtauth.JwtAuthenticationService;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;

@Extension
public class JwtAuthenticationServiceImpl extends JwtAuthenticationService {
    private static final Logger LOGGER = Logger.getLogger(JwtAuthenticationServiceImpl.class.getName());

    @Override
    public String getJwkSet() throws HttpRequestMethodNotSupportedException {
        LOGGER.log(Level.FINE, "Getting JwkSet");

        GlobalConjurConfiguration result = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);
        if (result == null || !result.getEnableJWKS()) {
            throw new HttpRequestMethodNotSupportedException("conjur-jwk-set");
        }

        return JwtToken.getJwkset().toString(4);
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Conjur JWT endpoint";
    }

}

