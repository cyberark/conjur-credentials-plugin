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

/**
 * ConjurSecretUsernameSSHKeyCredentialsImpl sets the passphrase and private key
 * details based on SSHKeyCredential
 * 
 * @author Jaleela.FaizurRahman
 *
 */
public class ConjurSecretUsernameSSHKeyCredentialsImpl extends BaseSSHUser
		implements ConjurSecretUsernameSSHKeyCredentials {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(ConjurSecretUsernameSSHKeyCredentialsImpl.class.getName());

	private String credentialID;
	private ConjurConfiguration conjurConfiguration;
	private Secret passphrase;

	transient ModelObject context;
	transient ModelObject storeContext;

	/**
	 * Constructor to set the
	 * scope,id,username,credentialID,ConjurConfiguration,Secret,description
	 * 
	 * @param CredentialsScope
	 * @param String              id
	 * @param String              username
	 * @param cString             redentialID
	 * @param ConjurConfiguration
	 * @param Secret              passphrase
	 * @param String              description
	 */
	@DataBoundConstructor
	public ConjurSecretUsernameSSHKeyCredentialsImpl(final CredentialsScope scope, final String id,
			final String username, final String credentialID, final ConjurConfiguration conjurConfiguration,
			final Secret passphrase, final String description) {
		super(scope, id, username, description);
		this.credentialID = credentialID;
		this.passphrase = passphrase;
		this.conjurConfiguration = conjurConfiguration;
	}

	/**
	 * 
	 * @return credentialID
	 */

	public String getCredentialID() {
		return credentialID;
	}

	/**
	 * set the credentialID
	 * 
	 * @param credentialID
	 */

	@DataBoundSetter
	public void setCredentialID(final String credentialID) {
		this.credentialID = credentialID;
	}

	/**
	 * 
	 * @return ConjurConfiguration
	 */

	public ConjurConfiguration getConjurConfiguration() {
		return conjurConfiguration;
	}

	/**
	 * set the ConjurConfiguration params
	 */

	@DataBoundSetter
	public void setConjurConfiguration(final ConjurConfiguration conjurConfiguration) {

		ConjurAPI.logConjurConfiguration(conjurConfiguration);

		this.conjurConfiguration = conjurConfiguration;

		ConjurSecretCredentials.setConjurConfigurationForCredentialWithID(this.getCredentialID(), conjurConfiguration,
				context);

	}

	/**
	 * @return Secret
	 */

	public Secret getPassphrase() {
		return passphrase;
	}

	/**
	 * set the secret
	 * 
	 * @param passphrase
	 */
	@DataBoundSetter
	public void setPassphrase(final Secret passphrase) {
		this.passphrase = passphrase;
	}

	/**
	 * To fill the Jenkins listbox with CredentialItems for
	 * ConjurSecretUsernameSSHKeyCredentials
	 * 
	 * @author Jaleela.FaizurRahman
	 *
	 */
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

	/**
	 * 
	 * @return DescriptorDisplayName
	 */

	public static String getDescriptorDisplayName() {
		return "Conjur Secret Username SSHKey Credential";
	}

	/**
	 * @return DisplayName
	 */

	@Override
	public String getDisplayName() {
		return "ConjurSecretUsernameSSHKey:" + this.username;
	}

	/**
	 * set the Context for ModelObject
	 */

	@Override
	public void setContext(final ModelObject context) {
		LOGGER.log(Level.FINE, "Set Context");
		if (context != null)
			this.context = context;
	}

	/**
	 * set the store context for ModelObject
	 */

	@Override
	public void setStoreContext(ModelObject storeContext) {
		LOGGER.log(Level.FINE, "Setting store context");
		this.storeContext = storeContext;
	}

	/**
	 * @return the SSHKey secret
	 */
	@Override
	public String getPrivateKey() {
		LOGGER.log(Level.FINE, "Getting SSH Key secret from Conjur");
		final Secret secret = ConjurSecretCredentials.getSecretFromCredentialIDWithConfigAndContext(
				this.getCredentialID(), this.conjurConfiguration, this.context, this.storeContext);
		return secret.getPlainText();
	}

	/**
	 * @return List of PrivateKey
	 */
	@Override
	public List<String> getPrivateKeys() {
		final List<String> result = new ArrayList<String>();
		result.add(getPrivateKey());
		return result;
	}

}
