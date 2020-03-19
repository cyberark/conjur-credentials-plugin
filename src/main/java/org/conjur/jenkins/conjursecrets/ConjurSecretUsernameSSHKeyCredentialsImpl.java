package org.conjur.jenkins.conjursecrets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.exceptions.InvalidConjurSecretException;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BaseSSHUser;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
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

	transient Run<?, ?> context;

	@DataBoundConstructor
	public ConjurSecretUsernameSSHKeyCredentialsImpl(CredentialsScope scope, String id, String username, String credentialID,
			ConjurConfiguration conjurConfiguration, Secret passphrase, String description) {
		super(scope, id, username, description);
		this.credentialID = credentialID;
		this.passphrase = passphrase;
		this.conjurConfiguration = conjurConfiguration;
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
		ConjurSecretCredentials credential = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(this.getCredentialID()));

		if(credential == null && context != null) {
            LOGGER.log(Level.INFO, "NOT FOUND at Jenkins Instance Level!");
            Item folder = Jenkins.getInstance().getItemByFullName(context.getParent().getParent().getFullName());
			credential = CredentialsMatchers.firstOrNull(
					CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, folder, ACL.SYSTEM,
							Collections.<DomainRequirement>emptyList()),
					CredentialsMatchers.withId(this.getCredentialID()));
		}

		if (credential != null)
			credential.setConjurConfiguration(conjurConfiguration);

	}
	
    public Secret getPassphrase() {
        return passphrase;
    }
	
	@DataBoundSetter
	public void setPassphrase(Secret passphrase) {
		this.passphrase = passphrase;
	}


	@Extension
	public static class DescriptorImpl extends CredentialsDescriptor {

		@Override
		public String getDisplayName() {
			return "Conjur Secret Username SSHKey Credential";
		}

		public ListBoxModel doFillCredentialIDItems(@AncestorInPath Item item, @QueryParameter String uri) {
			return new StandardListBoxModel().includeAs(ACL.SYSTEM, item, ConjurSecretCredentials.class,
					URIRequirementBuilder.fromUri(uri).build());
		}

	}

	@Override
	public String getDisplayName() {
		return "ConjurSecretUsernameSSHKey:" + this.username;
	}

	@Override
	public void setContext(Run<?, ?> context) {
		LOGGER.log(Level.INFO, "Set Context");
		if (context != null)
			this.context = context;
	}

	public Secret getSecret() {

		LOGGER.log(Level.INFO, "* CredentialID: {0}", this.getCredentialID());
		
		ConjurSecretCredentials credential = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(this.getCredentialID()));
		
        if(credential == null) {
            LOGGER.log(Level.INFO, "NOT FOUND at Jenkins Instance Level!");
            if (context == null) {
                throw new InvalidConjurSecretException("Unable to find credential at Global Instance Level and no current context to determine folder provided");
            }
            Item folder = Jenkins.getInstance().getItemByFullName(context.getParent().getParent().getFullName());
    		credential = CredentialsMatchers.firstOrNull(
    				CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, folder, ACL.SYSTEM,
    						Collections.<DomainRequirement>emptyList()),
    				CredentialsMatchers.withId(this.getCredentialID()));
        }
		
		if (credential == null) {
			LOGGER.log(Level.INFO, "NOT FOUND!");
			return null;
		}

		return credential.secretWithConjurConfigAndContext(conjurConfiguration, context);
	}

	@Override
	public String getPrivateKey() {
		LOGGER.log(Level.INFO, "Getting SSH Key secret from Conjur");
		return getSecret().getPlainText();
	}

	@Override
	public List<String> getPrivateKeys() {
		List<String> result = new ArrayList<String>();
		result.add(getPrivateKey());
		return result;
	}

}
