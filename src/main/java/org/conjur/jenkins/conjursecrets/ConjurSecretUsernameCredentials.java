package org.conjur.jenkins.conjursecrets;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import org.conjur.jenkins.configuration.ConjurConfiguration;

import hudson.model.ModelObject;
import hudson.util.Secret;


public interface ConjurSecretUsernameCredentials extends StandardUsernamePasswordCredentials, ConjurSecretCredentials {

	String getDisplayName();

	void setContext(ModelObject context);

	Secret getSecret();

	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);

}
