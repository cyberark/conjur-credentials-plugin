package org.conjur.jenkins.conjursecrets;

import javax.annotation.Nonnull;

import org.conjur.jenkins.configuration.ConjurConfiguration;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.model.Run;

@NameWith(value = ConjurSecretUsernameSSHKeyCredentials.NameProvider.class, priority = 1)

public interface ConjurSecretUsernameSSHKeyCredentials extends StandardUsernameCredentials {

	String getDisplayName();

	void setContext(Run<?, ?> context);

	String getPrivateKey();

	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);

	class NameProvider extends CredentialsNameProvider<ConjurSecretUsernameSSHKeyCredentials> {
		@Nonnull
		@Override
		public String getName(@Nonnull ConjurSecretUsernameSSHKeyCredentials c) {
			return c.getDisplayName() + "/*ConjurSecretUsernameSSHKey*" + " (" + c.getDescription() + ")";
		}
	}

}
