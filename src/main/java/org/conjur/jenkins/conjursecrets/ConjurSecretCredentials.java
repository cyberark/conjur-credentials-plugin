package org.conjur.jenkins.conjursecrets;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.conjur.jenkins.configuration.ConjurConfiguration;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;

@NameWith(value = ConjurSecretCredentials.NameProvider.class, priority = 1)
public interface ConjurSecretCredentials extends StandardCredentials {

	/** Innder class to retrieve the displayName for the job */
	class NameProvider extends CredentialsNameProvider<ConjurSecretCredentials> {
		/**
		 * returns the displayName and description to be displayed along with the Conjur
		 * secret Credential
		 */
		@Override
		public String getName(ConjurSecretCredentials c) {
			return c.getDisplayName() + c.getNameTag() + " (" + c.getDescription() + ")";
		}

	}

	public static final Logger LOGGER = Logger.getLogger(ConjurSecretCredentials.class.getName());

	String getDisplayName();

	String getNameTag();

	Secret getSecret();

	default Secret secretWithConjurConfigAndContext(ConjurConfiguration conjurConfiguration, ModelObject context) {
		setConjurConfiguration(conjurConfiguration);
		setContext(context);
		return getSecret();
	}

	void setConjurConfiguration(ConjurConfiguration conjurConfiguration);

	void setStoreContext(ModelObject storeContext);

	void setContext(ModelObject context);

	/**
	 * static method to fetch the credentials from the Context
	 * 
	 * @param selected    ConjurSecretcredential
	 * @param selected    or incoming CredentialId
	 * @param ModelObject
	 * @return the ConjurSecretCredentials
	 */
	static ConjurSecretCredentials credentialFromContextIfNeeded(ConjurSecretCredentials credential,
			String credentialID, ModelObject context) {

		LOGGER.log(Level.FINE, "Start of credentialFromContextIfNeeded()");
		if (credential == null && context != null) {
			LOGGER.log(Level.FINE, "NOT FOUND at Jenkins Instance Level!");
			Item folder = null;

			if (context instanceof Run) {
				LOGGER.log(Level.FINE, "Inside Conjur Credentials >> {0}", context.getDisplayName());
				folder = Jenkins.get().getItemByFullName(((Run<?, ?>) context).getParent().getParent().getFullName());
			} else {

				LOGGER.log(Level.FINE, "Inside not Conjur Credentials >> {0}", context.getDisplayName());
				folder = Jenkins.get().getItemByFullName((((AbstractItem) context)).getDisplayName());

			}

			LOGGER.log(Level.FINE, "Inside not conjur credentials final folder >> {0}", folder);
			credential = CredentialsMatchers
					.firstOrNull(
							CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, folder, ACL.SYSTEM,
									Collections.<DomainRequirement>emptyList()),
							CredentialsMatchers.withId(credentialID));
			LOGGER.log(Level.FINE, "Returning the Credentials >> {0}", credential);

			return credential;
		}
		LOGGER.log(Level.FINE, "End  of credentialFromContextIfNeeded()... returning credentails");
		return credential;
	}

	static ConjurSecretCredentials credentialWithID(String credentialID, ModelObject context) {
		LOGGER.log(Level.FINE, "Start of credentialWithID()");
		if (context != null) {
			LOGGER.log(Level.FINE, "* Context Id not null >>>: {0}", context.getDisplayName());
		}

		ConjurSecretCredentials credential = null;
		LOGGER.log(Level.FINE, "* Context Id >>> :{0}", Jenkins.get());
		credential = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, Jenkins.get(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(credentialID));

		if (credential == null) {
			if (context != null) {
				LOGGER.log(Level.FINE, "Get all jobs >> {0}", context);
				String[] spiltJob = context.toString().split("/");
				Item childFolder = null;
				Item parentFolder = null;

				ConjurSecretCredentials conjurSecretCredential = null;
				if (context instanceof Run) {
					LOGGER.log(Level.FINE, "Inside Conjur Credentials instance of Run>> {0}", context.getDisplayName());
					childFolder = Jenkins.get()
							.getItemByFullName(((Run<?, ?>) context).getParent().getParent().getFullName());
				} else {

					LOGGER.log(Level.FINE, "Inside not Conjur Credentials >>{0}", context.getDisplayName());
					childFolder = Jenkins.get()
							.getItemByFullName(((AbstractItem) ((AbstractItem) context).getParent()).getFullName());

				}
				parentFolder = childFolder;
				for (int i = 0; i < spiltJob.length; i++) {
					conjurSecretCredential = credentialFromContextIfNeeded(credential, credentialID, parentFolder);
					credential = conjurSecretCredential;
					if (conjurSecretCredential == null) {
						LOGGER.log(Level.FINE, "Inside Credentials not null");
						if(parentFolder !=null)
						{
						parentFolder = Jenkins.get()
								.getItemByFullName((((AbstractItem) parentFolder).getParent()).getFullName());
						}
						
						LOGGER.log(Level.FINE, "Back to the for loop tocheck for the parent level");
					}

				}

			}
		}
		LOGGER.log(Level.FINE, "End of credentialWithID()");
		return credential;
	}

	/**
	 * static method to set the ConjurConfiguration for CredentialWith ID
	 * 
	 * @param credentialID
	 * @param conjurConfiguration
	 * @param context
	 */
	static void setConjurConfigurationForCredentialWithID(String credentialID, ConjurConfiguration conjurConfiguration,
			ModelObject context) {
		LOGGER.log(Level.FINE, "Start of setConjurConfigurationForCredentialWithID()");
		ConjurSecretCredentials credential = credentialWithID(credentialID, context);

		if (credential != null)
			credential.setConjurConfiguration(conjurConfiguration);

		LOGGER.log(Level.FINE, "End of setConjurConfigurationForCredentialWithID()");
	}

	/**
	 * static method to get secretCredentialIDWithConfigAndContext
	 * @param credentialID
	 * @param conjurConfiguration
	 * @param context
	 * @param storeContext
	 * @return
	 */
	static Secret getSecretFromCredentialIDWithConfigAndContext(String credentialID,
			ConjurConfiguration conjurConfiguration, ModelObject context, ModelObject storeContext) {

		ModelObject effectiveContext = context != null ? context : storeContext;

		LOGGER.log(Level.FINE, "Getting Secret with CredentialID: {0},{1}", new Object[] { context, credentialID });
		ConjurSecretCredentials credential = credentialWithID(credentialID, effectiveContext);

		return credential.secretWithConjurConfigAndContext(conjurConfiguration, effectiveContext);
	}

}
