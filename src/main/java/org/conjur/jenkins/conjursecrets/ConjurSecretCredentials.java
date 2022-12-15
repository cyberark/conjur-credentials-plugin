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

	static Logger getLogger() {
		return Logger.getLogger(ConjurSecretCredentials.class.getName());
	}

	class NameProvider extends CredentialsNameProvider<ConjurSecretCredentials> {

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

	static ConjurSecretCredentials credentialFromContextIfNeeded(ConjurSecretCredentials credential,
			String credentialID, ModelObject context) {
		if (credential == null && context != null) {
			getLogger().log(Level.FINE, "NOT FOUND at Jenkins Instance Level!");
			Item folder = null;

			if (context instanceof Run) {
				LOGGER.log(Level.FINE, "Inside Conjur Credentials>>" + context.getDisplayName());
				folder = Jenkins.get().getItemByFullName(((Run<?, ?>) context).getParent().getParent().getFullName());
			} else {

				LOGGER.log(Level.FINE, "Inside not Conjur Credentials>>" + context.getDisplayName());
				folder = Jenkins.get().getItemByFullName((((AbstractItem)context)).getDisplayName());

				// LOGGER.log(Level.FINE, "Inside not conjur credentials" + (((AbstractItem)
				// context)).getFullName());
				// folder = Jenkins.get().getItemByFullName(
				// ((AbstractItem) ((AbstractItem)
				// context).getParent()).getParent().getFullName());
				// LOGGER.log(Level.FINE, "Inside not conjur credentials" + folder);

				/*
				 * if (folder == null) { LOGGER.log(Level.FINE, "Folder value is null"); folder
				 * = Jenkins.get() .getItemByFullName(((AbstractItem) ((AbstractItem)
				 * context).getParent()).getFullName());
				 * 
				 * } else { folder = Jenkins.get().getItemByFullName( ((AbstractItem)
				 * ((AbstractItem) context).getParent()).getParent().getFullName());
				 * LOGGER.log(Level.FINE, "Inside not conjur credentials parent folder" +
				 * folder); }
				 */

			}

			//folder = (Item) ((Item) context).getParent();
			LOGGER.log(Level.FINE, "Inside not conjur credentials final folder" + folder);
			credential = CredentialsMatchers
					.firstOrNull(
							CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, folder, ACL.SYSTEM,
									Collections.<DomainRequirement>emptyList()),
							CredentialsMatchers.withId(credentialID));
			LOGGER.log(Level.FINE, "Returning the Credentials" + credential);
			LOGGER.log(Level.FINE, "printing value");

			/*
			 * return CredentialsMatchers .firstOrNull(
			 * CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, folder,
			 * ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
			 * CredentialsMatchers.withId(credentialID));
			 */
			return credential;
		}
		LOGGER.log(Level.FINE, "Returning the Credentials" + credential);
		return credential;
	}

	static ConjurSecretCredentials credentialWithID(String credentialID, ModelObject context) {

		//getLogger().log(Level.FINE, "* CredentialID: {0}", credentialID);
		if (context != null) {
			//getLogger().log(Level.FINE, "* Context Id: {0}", context.getDisplayName());
			LOGGER.log(Level.FINE, "* Context Id not null>>>:" + context.getDisplayName());

		}

		ConjurSecretCredentials credential = null;

		LOGGER.log(Level.FINE, "* Context Id >>>:" + Jenkins.get());

		credential = CredentialsMatchers.firstOrNull(
				CredentialsProvider.lookupCredentials(ConjurSecretCredentials.class, Jenkins.get(), ACL.SYSTEM,
						Collections.<DomainRequirement>emptyList()),
				CredentialsMatchers.withId(credentialID));

		//credential = credentialFromContextIfNeeded(credential, credentialID, context);

		/*
		 * if (credential == null) {
		 * 
		 * String contextLevel =
		 * String.format("Unable to find credential at %s",context.getDisplayName()); //
		 * (context != null? context.getDisplayName() : "Global Instance Level")); throw
		 * new InvalidConjurSecretException(contextLevel); }
		 */

		if (credential == null) {
			if (context != null) {
				LOGGER.log(Level.FINE, "Get all jobs" + context.toString());
				// String allJobs = toCheck.getAllJobs().toString();

				String[] spiltJob = context.toString().split("/");
				Item childFolder = null;
				Item parentFolder = null;

				ConjurSecretCredentials conjurSecretCredential = null;

				if (context instanceof Run) {
					LOGGER.log(Level.FINE, "Inside Conjur Credentials>>" + context.getDisplayName());
					childFolder = Jenkins.get().getItemByFullName(((Run<?, ?>) context).getParent().getParent().getFullName());
				} else {

					LOGGER.log(Level.FINE, "Inside not Conjur Credentials>>" + context.getDisplayName());

					childFolder = Jenkins.get().getItemByFullName(((AbstractItem)((AbstractItem)context).getParent()).getFullName());
					//Jenkins.get().getItemByFullName((((AbstractItem) context).getParent()).getFullName());

				}

				 //childFolder = Jenkins.get().getItemByFullName(((Job<?, ?>)context).getParent().getFullName());
				parentFolder =  childFolder;
				LOGGER.log(Level.FINE, "Child Folder" + childFolder + ">>>>>>" + spiltJob.length);

				for (int i = 0; i < spiltJob.length; i++) {

					LOGGER.log(Level.FINE, "From Binding Credential to Jenkins",parentFolder + "Level" + i);

					conjurSecretCredential = credentialFromContextIfNeeded(credential, credentialID, parentFolder);

					LOGGER.log(Level.FINE, "From Binding Credential" + conjurSecretCredential);
					credential = conjurSecretCredential;
					if (conjurSecretCredential == null) {
						LOGGER.log(Level.FINE,"Inside Credentials not null");
						// parentFolder = (Item) ((Item) parentFolder).getParent();
						parentFolder = Jenkins.get()
								.getItemByFullName((((AbstractItem) parentFolder).getParent()).getFullName());

						LOGGER.log(Level.FINE, "Back to the for loop tocheck for the parent level");
					}

				}

			}
		}

		return credential;
	}

	static void setConjurConfigurationForCredentialWithID(String credentialID, ConjurConfiguration conjurConfiguration,
			ModelObject context) {

		ConjurSecretCredentials credential = credentialWithID(credentialID, context);

		if (credential != null)
			credential.setConjurConfiguration(conjurConfiguration);

	}

	static Secret getSecretFromCredentialIDWithConfigAndContext(String credentialID,
			ConjurConfiguration conjurConfiguration, ModelObject context, ModelObject storeContext) {

		ModelObject effectiveContext = context != null ? context : storeContext;

		getLogger().log(Level.FINE,
				"Getting Secret with CredentialID: {0}  context: " + context + " storeContext: " + storeContext,
				credentialID);
		ConjurSecretCredentials credential = credentialWithID(credentialID, effectiveContext);

		return credential.secretWithConjurConfigAndContext(conjurConfiguration, effectiveContext);
	}

}
