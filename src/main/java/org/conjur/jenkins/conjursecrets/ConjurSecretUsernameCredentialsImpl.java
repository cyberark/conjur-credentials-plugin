package org.conjur.jenkins.conjursecrets;

import java.util.logging.Level;
import java.util.logging.Logger;

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
import hudson.model.ModelObject;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;

@NameWith(value = ConjurSecretCredentials.NameProvider.class, priority = 1)

public class ConjurSecretUsernameCredentialsImpl extends BaseStandardCredentials
		implements ConjurSecretUsernameCredentials {

	private static final Logger LOGGER = Logger.getLogger(ConjurSecretUsernameCredentialsImpl.class.getName());

	private String username;
	private String credentialID;
	private ConjurConfiguration conjurConfiguration;

	private transient ModelObject context;
	private transient ModelObject storeContext;

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
	public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

		@Override
		public String getDisplayName() {
			return ConjurSecretUsernameCredentialsImpl.getDescriptorDisplayName();
		}

		public ListBoxModel doFillCredentialIDItems(@AncestorInPath final Item item, @QueryParameter final String uri) {
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			return new StandardListBoxModel().includeAs(ACL.SYSTEM, item, ConjurSecretCredentials.class,
					URIRequirementBuilder.fromUri(uri).build());
		}

	}

	public static String getDescriptorDisplayName() {
		return "Conjur Secret Username Credential";
	}

	@Override
	public String getDisplayName() {
		return "ConjurSecretUsername:" + this.getCredentialID();
	}

	@Override
	public void setContext(ModelObject context) {
		LOGGER.log(Level.FINE, "Set Context");
		if (context != null)
			this.context = context;
	}
	@Override
	public void setStoreContext(ModelObject storeContext) {
		LOGGER.log(Level.FINE, "Set Store Context");
		if (storeContext != null)
			this.storeContext = storeContext;
	}

	@Override
	public Secret getSecret() {
		return getPassword();
	}

	@Override
	public Secret getPassword() {
		LOGGER.log(Level.FINE, "Getting Password");
		return ConjurSecretCredentials.getSecretFromCredentialIDWithConfigAndContext(this.getCredentialID(), this.conjurConfiguration, this.context, this.storeContext);
	}

	@Override
	public String getNameTag() {
		return "";
	}

}
