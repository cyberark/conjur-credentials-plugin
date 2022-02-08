package org.conjur.jenkins.configuration;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

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

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public class ConjurConfiguration extends AbstractDescribableImpl<ConjurConfiguration> implements Serializable {

	private static final Logger LOGGER = Logger.getLogger(ConjurConfiguration.class.getName());

	@Extension
	public static class DescriptorImpl extends Descriptor<ConjurConfiguration> {
		public ListBoxModel doFillCertificateCredentialIDItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
			return fillCredentialIDItemsWithClass(item, credentialsId, StandardCertificateCredentials.class);
		}

		public ListBoxModel doFillCredentialIDItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
			return fillCredentialIDItemsWithClass(item, credentialsId, StandardUsernamePasswordCredentials.class);
		}

		@Override
		public String getDisplayName() {
			return "Conjur Configuration";
		}

		@POST
		public FormValidation doObtainJwtToken(@AncestorInPath Item item) throws IOException, ServletException {
			JwtToken token = JwtToken.getUnsignedToken("pluginAction", item);
			return FormValidation.ok("JWT Token: \n" + token.claim.toString(4));
		}

		@POST
		public FormValidation doRefreshCredentialSupplier(@AncestorInPath Item item) throws IOException, ServletException {
						
			if (item != null) {
				String key = String.valueOf(item.hashCode());            
				Supplier<Collection<StandardCredentials>> supplier;
				if (ConjurCredentialStore.getAllStores().containsKey(key)) {
					LOGGER.log(Level.FINEST, "Resetting Credential Supplier : " + item.getClass().getName() + ": " + item.toString() + " => " + item.hashCode());
					supplier = ConjurCredentialProvider.memoizeWithExpiration(CredentialsSupplier.standard(item), Duration.ofSeconds(120));
					ConjurCredentialProvider.getAllCredentialSuppliers().put(key, supplier);
				}
				return FormValidation.ok("Refreshed");
 			} else {
				 return FormValidation.ok();
			 }
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String applianceURL;
	private String account;
	private String credentialID;
	private String certificateCredentialID;
	private String ownerFullName;

	public ConjurConfiguration() {
	}

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

	public String getCertificateCredentialID() {
		return certificateCredentialID;
	}

	public String getCredentialID() {
		return credentialID;
	}

	public String getOwnerFullName() {
		return ownerFullName;
	}


	/**
	 * Together with {@link #getAccount}, binds to entry in {@code config.jelly}.
	 * 
	 * @param account
	 *            the new value of Conjur account
	 */
	@DataBoundSetter
	public void setAccount(String account) {
		this.account = account;
	}

	/**
	 * Together with {@link #getApplianceURL}, binds to entry in
	 * {@code config.jelly}.
	 * 
	 * @param applianceURL
	 *            the new value of Conjur Appliance URL
	 */
	@DataBoundSetter
	public void setApplianceURL(String applianceURL) {
		this.applianceURL = applianceURL;
	}

	@DataBoundSetter
	public void setCertificateCredentialID(String certificateCredentialID) {
		this.certificateCredentialID = certificateCredentialID;
	}

	@DataBoundSetter
	public void setCredentialID(String credentialID) {
		this.credentialID = credentialID;
	}

	public void setOwnerFullName(String ownerFullName) {
		this.ownerFullName = ownerFullName;
	}

	private static ListBoxModel fillCredentialIDItemsWithClass(Item item, String credentialsId, Class<? extends StandardCredentials> credentialClass) {
		StandardListBoxModel result = new StandardListBoxModel();
		if (item == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
			return result.includeCurrentValue(credentialsId);
		} 

		if (item != null
			&& !item.hasPermission(Item.EXTENDED_READ)
			&& !item.hasPermission(CredentialsProvider.USE_ITEM)) {
		return result.includeCurrentValue(credentialsId);
		}

		return result
			.includeEmptyValue()
			.includeAs(ACL.SYSTEM, item, credentialClass, URIRequirementBuilder.fromUri(credentialsId).build())
			.includeCurrentValue(credentialsId);
	}

}
