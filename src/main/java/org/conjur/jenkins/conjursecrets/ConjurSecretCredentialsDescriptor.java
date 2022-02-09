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

//@Extension
public class ConjurSecretCredentialsDescriptor extends CredentialsDescriptor {

	@Override
	public String getDisplayName() {
		return "Generic Conjur Secret Credential";
	}

	public ListBoxModel doFillCredentialIDItems(@AncestorInPath final Item item, @QueryParameter final String uri) {
		Jenkins.get().checkPermission(Jenkins.ADMINISTER);
		return new StandardListBoxModel().includeAs(ACL.SYSTEM, item, ConjurSecretCredentials.class,
				URIRequirementBuilder.fromUri(uri).build());
	}

}