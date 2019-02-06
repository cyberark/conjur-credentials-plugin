package org.conjur.jenkins.configuration;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
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

public class ConjurConfiguration extends AbstractDescribableImpl<ConjurConfiguration> implements Serializable {

	@Extension
	public static class DescriptorImpl extends Descriptor<ConjurConfiguration> {
		public ListBoxModel doFillCertificateCredentialIDItems(@AncestorInPath Item item, @QueryParameter String uri) {
			return new StandardListBoxModel().includeEmptyValue().includeAs(ACL.SYSTEM, item,
					StandardCertificateCredentials.class, URIRequirementBuilder.fromUri(uri).build());
		}

		public ListBoxModel doFillCredentialIDItems(@AncestorInPath Item item, @QueryParameter String uri) {
			return new StandardListBoxModel().includeEmptyValue().includeAs(ACL.SYSTEM, item,
					StandardUsernamePasswordCredentials.class, URIRequirementBuilder.fromUri(uri).build());
		}

		@Override
		public String getDisplayName() {
			return "Conjur Configuration";
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

	public ConjurConfiguration() {
	}

	@DataBoundConstructor
	public ConjurConfiguration(String applianceURL, String account) {
		this.applianceURL = applianceURL;
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

}
