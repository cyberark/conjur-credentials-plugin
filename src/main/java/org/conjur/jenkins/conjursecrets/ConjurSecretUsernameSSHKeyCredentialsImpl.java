package org.conjur.jenkins.conjursecrets;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BaseSSHUser;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;

import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.Run;
import hudson.util.Secret;

public class ConjurSecretUsernameSSHKeyCredentialsImpl extends BaseSSHUser
implements ConjurSecretUsernameSSHKeyCredentials {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(ConjurSecretUsernameSSHKeyCredentialsImpl.class.getName());

	private String credentialID;
	private ConjurConfiguration conjurConfiguration;
	private Secret passphrase;

	transient Run<?, ?> context;

	@DataBoundConstructor
	public ConjurSecretUsernameSSHKeyCredentialsImpl(final CredentialsScope scope, final String id,
			final String username, final String credentialID, final ConjurConfiguration conjurConfiguration,
			final Secret passphrase, final String description) {
		super(scope, id, username, description);
		this.credentialID = credentialID;
		this.passphrase = passphrase;
		this.conjurConfiguration = conjurConfiguration;
	}

	public String getCredentialID() {
		return credentialID;
	}

	@DataBoundSetter
	public void setCredentialID(final String credentialID) {
		this.credentialID = credentialID;
	}

	public ConjurConfiguration getConjurConfiguration() {
		return conjurConfiguration;
	}

	@DataBoundSetter
	public void setConjurConfiguration(final ConjurConfiguration conjurConfiguration) {

		ConjurAPI.logConjurConfiguration(conjurConfiguration);

		this.conjurConfiguration = conjurConfiguration;

		ConjurSecretCredentials.setConjurConfigurationForCredentialWithID(this.getCredentialID(), conjurConfiguration,
				context);

	}

	public Secret getPassphrase() {
		return passphrase;
	}

	@DataBoundSetter
	public void setPassphrase(final Secret passphrase) {
		this.passphrase = passphrase;
	}

	@Extension
	public static class DescriptorImpl extends CredentialsDescriptor {

		@Override
		public String getDisplayName() {
			return ConjurSecretUsernameSSHKeyCredentialsImpl.getDescriptorDisplayName();
		}

	}

	public static String getDescriptorDisplayName() {
		return "Conjur Secret Username SSHKey Credential";
	}

	@Override
	public String getDisplayName() {
		return "ConjurSecretUsernameSSHKey:" + this.username;
	}

	@Override
	public void setContext(final Run<?, ?> context) {
		LOGGER.log(Level.INFO, "Set Context");
		if (context != null)
			this.context = context;
	}

	@Override
	public String getPrivateKey() {
		LOGGER.log(Level.INFO, "Getting SSH Key secret from Conjur");
		final Secret secret = ConjurSecretCredentials.getSecretFromCredentialIDWithConfigAndContext(
				this.getCredentialID(), this.conjurConfiguration, this.context);
		return secret.getPlainText();
	}

	@Override
	public List<String> getPrivateKeys() {
		final List<String> result = new ArrayList<String>();
		result.add(getPrivateKey());
		return result;
	}

}
