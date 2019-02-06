package org.conjur.jenkins.conjursecrets;

import javax.annotation.Nonnull;

import org.conjur.jenkins.configuration.ConjurConfiguration;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import hudson.model.Run;
import hudson.util.Secret;

@NameWith(value = ConjurSecretUsernameCredentials.NameProvider.class, priority = 1)

public interface ConjurSecretUsernameCredentials extends StandardUsernamePasswordCredentials {

	String getDisplayName();

	void setContext(Run<?, ?> context);

	Secret getSecret();

	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);

	class NameProvider extends CredentialsNameProvider<ConjurSecretUsernameCredentials> {
		@Nonnull
		@Override
		public String getName(@Nonnull ConjurSecretUsernameCredentials c) {
			return c.getUsername() + "/*ConjurSecretUsername*" + " (" + c.getDescription() + ")";
		}
	}

}
