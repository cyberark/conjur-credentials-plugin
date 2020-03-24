package org.conjur.jenkins.conjursecrets;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;

@NameWith(value = ConjurSecretCredentials.NameProvider.class, priority = 1)

public class ConjurSecretUsernameCredentialsImpl extends BaseStandardCredentials
		implements ConjurSecretUsernameCredentials {

	private static final Logger LOGGER = Logger.getLogger(ConjurSecretUsernameCredentialsImpl.class.getName());

	private String username;
	private String credentialID;
	private ConjurConfiguration conjurConfiguration;

	transient Run<?, ?> context;

	@DataBoundConstructor
	public ConjurSecretUsernameCredentialsImpl(CredentialsScope scope, String id, String username, String credentialID,
			ConjurConfiguration conjurConfiguration, String description) {
		super(scope, id, description);
		this.username = username;
		this.credentialID = credentialID;
		this.conjurConfiguration = conjurConfiguration;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getUsername() {
		LOGGER.log(Level.INFO, "Get UserName => {0}", this.username);
		return this.username;
	}

	@DataBoundSetter
	public void setUserName(String username) {
		this.username = username;
	}

	public String getCredentialID() {
		return credentialID;
	}

	@DataBoundSetter
	public void setCredentialID(String credentialID) {
		this.credentialID = credentialID;
	}

	public ConjurConfiguration getConjurConfiguration() {
		return conjurConfiguration;
	}

	@DataBoundSetter
	public void setConjurConfiguration(ConjurConfiguration conjurConfiguration) {

		ConjurAPI.logConjurConfiguration(conjurConfiguration);

		this.conjurConfiguration = conjurConfiguration;

		ConjurSecretCredentials.setConjurConfigurationForCredentialWithID(this.getCredentialID(), conjurConfiguration, context);

	}

	@Extension
	public static class DescriptorImpl extends ConjurSecretCredentialsDescriptor {

		@Override
		public String getDisplayName() {
			return ConjurSecretUsernameCredentialsImpl.getDescriptorDisplayName();
		}

	}

	public static String getDescriptorDisplayName() {
		return "Conjur Secret Username Credential";
	}

	@Override
	public String getDisplayName() {
		return "ConjurSecretUsername:" + this.username;
	}

	@Override
	public void setContext(Run<?, ?> context) {
		LOGGER.log(Level.INFO, "Set Context");
		if (context != null)
			this.context = context;
	}

	@Override
	public Secret getSecret() {
		return getPassword();
	}

	@Override
	public Secret getPassword() {
		LOGGER.log(Level.INFO, "Getting Password");
		return ConjurSecretCredentials.getSecretFromCredentialIDWithConfigAndContext(this.getCredentialID(), this.conjurConfiguration, this.context);
	}

	@Override
	public String getNameTag() {
		return "/*ConjurSecretUsername*";
	}

}
