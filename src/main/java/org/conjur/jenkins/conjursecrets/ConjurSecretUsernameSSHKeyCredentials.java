package org.conjur.jenkins.conjursecrets;

import javax.annotation.Nonnull;

import org.conjur.jenkins.configuration.ConjurConfiguration;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;

import hudson.model.Run;

@NameWith(value = ConjurSecretUsernameSSHKeyCredentials.NameProvider.class, priority = 1)

public interface ConjurSecretUsernameSSHKeyCredentials extends SSHUserPrivateKey {

	String getDisplayName();

	void setContext(Run<?, ?> context);

	String getPrivateKey();

	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);

	public static class NameProvider extends CredentialsNameProvider<StandardUsernameCredentials> {
		@Nonnull
		@Override
		public String getName(@Nonnull StandardUsernameCredentials c) {
			return "ConjurSecretUsernameSSHKey:" + c.getUsername() + "/*ConjurSecretUsernameSSHKey*" + " (" + c.getDescription() + ")";
		}
	}

}
