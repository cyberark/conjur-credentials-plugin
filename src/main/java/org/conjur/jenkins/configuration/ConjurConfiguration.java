package org.conjur.jenkins.configuration;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.conjur.jenkins.credentials.ConjurCredentialProvider;
import org.conjur.jenkins.credentials.ConjurCredentialStore;
import org.conjur.jenkins.credentials.CredentialsSupplier;
import org.conjur.jenkins.jwtauth.impl.JwtToken;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * ConjurConfiguration class extends Jenkins AbstractDescribableImpl class and
 * implements Serializable Retrieves the Conjur configuration details and assign
 * to Configuration parameters
 * 
 * @author Jaleela.FaizurRahman
 *
 */

public class ConjurConfiguration extends AbstractDescribableImpl<ConjurConfiguration> implements Serializable {

	private static final Logger LOGGER = Logger.getLogger(ConjurConfiguration.class.getName());

	/**
	 * Inner static class to retrieve the configuration details from Jenkins
	 * 
	 * @author Jaleela.FaizurRahman
	 *
	 */
	@Extension
	public static class DescriptorImpl extends Descriptor<ConjurConfiguration> {
		/**
		 * Retrieve the conjur credentials and populate back to the ListBox based on the
		 * CertificateCredentialIDItems.
		 * 
		 * @param Jenkins  Item Object for the pipeline
		 * @param selected credentialsId
		 * @return Jenkins ListBoxModel
		 */
		public ListBoxModel doFillCertificateCredentialIDItems(@AncestorInPath Item item,
				@QueryParameter String credentialsId) {
			LOGGER.log(Level.FINE, "Inside doFillCertificateCredentialIDItems()");
			return fillCredentialIDItemsWithClass(item, credentialsId, StandardCertificateCredentials.class);
		}

		/**
		 * Retrieve the conjur credentials and populate back to the ListBox based on the
		 * CredentialIDItems.
		 * 
		 * @param Jenkins  Item Object for the pipeline
		 * @param selected credentialsId
		 * @return Jenkins ListBoxModel
		 */

		public ListBoxModel doFillCredentialIDItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
			LOGGER.log(Level.FINE, "Inside doFillCredentialIDItems()");
			return fillCredentialIDItemsWithClass(item, credentialsId, StandardUsernamePasswordCredentials.class);
		}

		/**
		 * Overriden method to display name
		 * 
		 * @return the name to be displayed
		 */
		@Override
		public String getDisplayName() {
			LOGGER.log(Level.FINE, "Inside getDisplayName()");
			return "Conjur Configuration";
		}

		/**
		 * POST method to obtain the JWTtoken for the Item
		 * 
		 * @param Jenkins ITem item
		 * @return status ok based on the FormValidation
		 */

		@POST
		public FormValidation doObtainJwtToken(@AncestorInPath Item item) {
			LOGGER.log(Level.FINE, "Inside doObtainJwtToken()");
			JwtToken token = JwtToken.getUnsignedToken("pluginAction", item);
			return FormValidation.ok("JWT Token: \n" + token.claim.toString(4));
		}

		/**
		 * POST method to refresh the Credential supplier
		 * 
		 * @param Jenkins Itme item
		 * @return status ok based on the Form Validation
		 */

		@POST
		public FormValidation doRefreshCredentialSupplier(@AncestorInPath Item item) {

			if (item != null) {
				String key = String.valueOf(item.hashCode());
				Supplier<Collection<StandardCredentials>> supplier;
				if (ConjurCredentialStore.getAllStores().containsKey(key)) {
					LOGGER.log(Level.FINE, "Resetting Credential Supplier : {0},{1},{2}",
							new Object[] { item.getClass().getName(), item, item.hashCode() });

					supplier = ConjurCredentialProvider.memoizeWithExpiration(CredentialsSupplier.standard(item),
							Duration.ofSeconds(120));
					ConjurCredentialProvider.getAllCredentialSuppliers().put(key, supplier);
				}
				return FormValidation.ok("Refreshed");
			} else {
				return FormValidation.ok();
			}
		}

		private static ListBoxModel fillCredentialIDItemsWithClass(Item item, String credentialsId,
				Class<? extends StandardCredentials> credentialClass) {
			StandardListBoxModel result = new StandardListBoxModel();
			if (item == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
				return result.includeCurrentValue(credentialsId);
			}

			if (item != null && !item.hasPermission(Item.EXTENDED_READ)
					&& !item.hasPermission(CredentialsProvider.USE_ITEM)) {
				return result.includeCurrentValue(credentialsId);
			}

			return result.includeEmptyValue()
					.includeAs(ACL.SYSTEM, item, credentialClass, URIRequirementBuilder.fromUri(credentialsId).build())
					.includeCurrentValue(credentialsId);
		}
	}

	private static final long serialVersionUID = 1L;
	private String applianceURL;
	private String account;
	private String credentialID;
	private String certificateCredentialID;
	private String ownerFullName;

	public ConjurConfiguration() {
	}

	/**
	 * DataBoundConstructor to bind the configuration
	 * 
	 * @param host url applianceURL
	 * @param host account
	 */
	@DataBoundConstructor
	public ConjurConfiguration(String applianceURL, String account) {
		if (applianceURL.endsWith("/")) {
			// Remove trailing slash from appliance URL
			this.applianceURL = applianceURL.substring(0, applianceURL.length() - 1);
		} else {
			this.applianceURL = applianceURL;
		}
		this.account = account;
	}

	/**
	 * To check the account is empty
	 * 
	 * @param host account value
	 * @return status ok based on the Account value
	 */

	public FormValidation doCheckAccount(@QueryParameter String value) {
		if (StringUtils.isEmpty(value)) {
			return FormValidation.warning("Please specify Account.");
		}
		return FormValidation.ok();
	}

	/** @return the currently configured Account, if any */
	public String getAccount() {
		return account;
	}

	/** @return the currently appliance URL, if any */
	public String getApplianceURL() {
		return applianceURL;
	}

	/** @return the currently certification credentail Id, if any */
	public String getCertificateCredentialID() {
		return certificateCredentialID;
	}

	/** @return the currently credentail Id, if any */
	public String getCredentialID() {
		return credentialID;
	}

	/** @return the currently Owner full name, if any */
	public String getOwnerFullName() {
		return ownerFullName;
	}

	/**
	 * Together with {@link #getAccount}, binds to entry in {@code config.jelly}.
	 * 
	 * @param account the new value of Conjur account
	 */
	@DataBoundSetter
	public void setAccount(String account) {
		this.account = account;
	}

	/**
	 * Together with {@link #getApplianceURL}, binds to entry in
	 * {@code config.jelly}.
	 * 
	 * @param applianceURL the new value of Conjur Appliance URL
	 */
	@DataBoundSetter
	public void setApplianceURL(String applianceURL) {
		this.applianceURL = applianceURL;
	}

	/**
	 * Together with {@link #getCertificateCredentialID}, binds to entry in
	 * {@code config.jelly}.
	 * 
	 * @param certificateCredentialID the new value of Conjur
	 *                                CertificateCredentialID
	 */

	@DataBoundSetter
	public void setCertificateCredentialID(String certificateCredentialID) {
		this.certificateCredentialID = certificateCredentialID;
	}

	/**
	 * Together with {@link #getCredentialID}, binds to entry in
	 * {@code config.jelly}.
	 * 
	 * @param credentialID the new value of Conjur credentialID
	 */

	@DataBoundSetter
	public void setCredentialID(String credentialID) {
		this.credentialID = credentialID;
	}

	/**
	 * Together with {@link #getOwnerFullName}, binds to entry in
	 * {@code config.jelly}.
	 * 
	 * @param ownerFullName the new value of Conjur OwnerFullname
	 */

	public void setOwnerFullName(String ownerFullName) {
		this.ownerFullName = ownerFullName;
	}

}
