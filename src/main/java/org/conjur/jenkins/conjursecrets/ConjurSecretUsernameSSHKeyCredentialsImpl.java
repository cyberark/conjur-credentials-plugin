package org.conjur.jenkins.conjursecrets;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BaseSSHUser;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

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

	transient ModelObject context;
	transient ModelObject storeContext;

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

		public ListBoxModel doFillCredentialIDItems(@AncestorInPath final Item item, @QueryParameter final String uri) {
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			return new StandardListBoxModel().includeAs(ACL.SYSTEM, item, ConjurSecretCredentials.class,
					URIRequirementBuilder.fromUri(uri).build());
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
	public void setContext(final ModelObject context) {
		LOGGER.log(Level.FINE, "Set Context");
		if (context != null)
			this.context = context;
	}

	@Override
	public void setStoreContext(ModelObject storeContext) {
		LOGGER.log(Level.FINE, "Setting store context");
		this.storeContext = storeContext;
	}

	@Override
	public String getPrivateKey() {
		LOGGER.log(Level.FINE, "Getting SSH Key secret from Conjur");
		final Secret secret = ConjurSecretCredentials.getSecretFromCredentialIDWithConfigAndContext(
				this.getCredentialID(), this.conjurConfiguration, this.context, this.storeContext);
		return secret.getPlainText();
	}

	@Override
	public List<String> getPrivateKeys() {
		final List<String> result = new ArrayList<String>();
		result.add(getPrivateKey());
		return result;
	}

}
