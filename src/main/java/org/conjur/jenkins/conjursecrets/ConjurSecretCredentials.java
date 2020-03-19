package org.conjur.jenkins.conjursecrets;

import org.conjur.jenkins.configuration.ConjurConfiguration;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;

import hudson.model.Run;
import hudson.util.Secret;

@NameWith(value = ConjurSecretCredentials.NameProvider.class, priority = 1)

public interface ConjurSecretCredentials extends StandardCredentials {

	class NameProvider extends CredentialsNameProvider<ConjurSecretCredentials> {

		@Override
		public String getName(ConjurSecretCredentials conjurSecretCredential) {
			String description = conjurSecretCredential.getDescription();
			return conjurSecretCredential.getDisplayName() + "/*Conjur*" + " (" + description + ")";
		}

	}

	String getDisplayName();

	Secret getSecret();

	Secret secretWithConjurConfigAndContext(ConjurConfiguration conjurConfiguration, Run<?, ?> context);

	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);

	void setContext(Run<?, ?> context);

}
