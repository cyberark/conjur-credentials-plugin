package org.conjur.jenkins.conjursecrets;

import org.conjur.jenkins.configuration.ConjurConfiguration;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import hudson.model.ModelObject;
import hudson.util.Secret;

/**
 *Interface to get the DispalyName,Context, secret
 */

public interface ConjurSecretUsernameCredentials extends StandardUsernamePasswordCredentials, ConjurSecretCredentials {

	String getDisplayName();

	void setContext(ModelObject context);

	Secret getSecret();

	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);

}
