package org.conjur.jenkins.conjursecrets;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.exceptions.InvalidConjurSecretException;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

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
	public static class DescriptorImpl extends CredentialsDescriptor {

		@Override
		public String getDisplayName() {
			return "Conjur Secret Username Credential";
		}

		public ListBoxModel doFillCredentialIDItems(@AncestorInPath Item item, @QueryParameter String uri) {
			return new StandardListBoxModel().includeAs(ACL.SYSTEM, item, ConjurSecretCredentials.class,
					URIRequirementBuilder.fromUri(uri).build());
		}

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

}
