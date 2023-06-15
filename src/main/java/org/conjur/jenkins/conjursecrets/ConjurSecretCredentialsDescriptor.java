package org.conjur.jenkins.conjursecrets;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * ConjurSecretCredentialDescriptior to populate the listbox with the
 * credentialss
 * 
 * @author Jaleela.FaizurRahman
 *
 */
//@Extension
public class ConjurSecretCredentialsDescriptor extends CredentialsDescriptor {

	/** @return the DisplayName */

	@Override
	public String getDisplayName() {
		return "Generic Conjur Secret Credential";
	}

	/** @return the ListBoxModel with the populated CredentialIDItems based on item and uri details*/
	public ListBoxModel doFillCredentialIDItems(@AncestorInPath final Item item, @QueryParameter final String uri) {
		Jenkins.get().checkPermission(Jenkins.ADMINISTER);
		return new StandardListBoxModel().includeAs(ACL.SYSTEM, item, ConjurSecretCredentials.class,
				URIRequirementBuilder.fromUri(uri).build());
	}

}