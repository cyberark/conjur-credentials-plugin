package org.conjur.jenkins.credentials;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardCredentials;

import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.api.ConjurAPIUtils;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.conjursecrets.ConjurSecretCredentials;
import org.conjur.jenkins.conjursecrets.ConjurSecretCredentialsImpl;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameCredentials;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameCredentialsImpl;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameSSHKeyCredentials;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameSSHKeyCredentialsImpl;
import org.json.JSONArray;
import org.json.JSONObject;

import hudson.model.ModelObject;
// import net.sf.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CredentialsSupplier implements Supplier<Collection<StandardCredentials>> {

    private static final Logger LOGGER = Logger.getLogger(CredentialsSupplier.class.getName());

    private ModelObject context;

    private CredentialsSupplier(ModelObject context) {
        super();
        this.context = context;
    }

    public static Supplier<Collection<StandardCredentials>> standard(ModelObject context) {
        return new CredentialsSupplier(context);
    }

    @Override
    public Collection<StandardCredentials> get() {

        LOGGER.log(Level.FINEST,"Retrieve variables from CyberArk Conjur -- Context => " + getContext());
        LOGGER.log(Level.FINEST,"Retrieve variables from CyberArk Conjur ==> " + getContext().getClass().getName() + ": " + getContext().toString() + " => " + getContext().hashCode());
        final Collection<StandardCredentials> allCredentials = new ArrayList<>();


		String result = "";
		try {
            ConjurConfiguration conjurConfiguration = ConjurAPI.getConfigurationFromContext(getContext(), null);
			// Get Http Client
			OkHttpClient client = ConjurAPIUtils.getHttpClient(conjurConfiguration);
			// Authenticate to Conjur
			String authToken = ConjurAPI.getAuthorizationToken(client, conjurConfiguration, getContext());
			// // Retrieve secret from Conjur
			// String secretString = ConjurAPI.getSecret(client, conjurConfiguration, authToken, this.variablePath);
			// result = secretString;
            // LOGGER.log(Level.FINEST, "authToken=" + authToken);

            ConjurAPI.ConjurAuthnInfo conjurAuthn = ConjurAPI.getConjurAuthnInfo(conjurConfiguration, null, getContext());

            LOGGER.log(Level.FINE, "Fetching variables from Conjur");
            Request request = new Request.Builder().url(
                    String.format("%s/resources/%s?kind=variable&limit=1000", conjurAuthn.applianceUrl, conjurAuthn.account))
                    .get().addHeader("Authorization", "Token token=\"" + authToken + "\"").build();
    
            Response response = client.newCall(request).execute();
            result = response.body().string();
            LOGGER.log(Level.FINEST, "RESULT => " + result);
            if (response.code() != 200) {
                LOGGER.log(Level.FINE, "Error fetching variables from Conjur [" + response.code() + " - " + response.message()
                + "\n" + result);
                throw new IOException("Error fetching variables from Conjur [" + response.code() + " - " + response.message()
                        + "\n" + result);
            }

            JSONArray resultResources = new JSONArray(result);
            for (int i = 0; i < resultResources.length(); i++) {
                JSONObject resource = resultResources.getJSONObject(i);
                LOGGER.log(Level.FINEST, "resource => {0}", resource.toString(4));

                String variablePath = resource.getString("id").split(":")[2];
                JSONArray annotations = resource.getJSONArray("annotations");
                String userName = null;
                String credentialType = null;
                for (int j = 0; j < annotations.length(); j++) {
                    JSONObject annotation = annotations.getJSONObject(j);
                    switch (annotation.getString("name").toLowerCase()) {
                        case "jenkins_credential_username":
                            userName = annotation.getString("value");
                            break;
                        case "jenkins_credential_type":
                            credentialType = annotation.getString("value").toLowerCase();
                            break;
                        default:
                            break;
                    }
                }

                if (credentialType == null) {
                    if (userName == null) {
                        credentialType = "credential";
                    } else {
                        credentialType = "usernamecredential";
                    }
                }

                ConjurSecretCredentials credential = (ConjurSecretCredentials) new ConjurSecretCredentialsImpl(CredentialsScope.GLOBAL, variablePath.replace("/", "-"), variablePath, "CyberArk Conjur Provided");
                credential.setStoreContext(getContext());
                allCredentials.add(credential);
                switch (credentialType) {
                    case "usernamecredential":
                        ConjurSecretUsernameCredentials usernameCredential = (ConjurSecretUsernameCredentials) new ConjurSecretUsernameCredentialsImpl(CredentialsScope.GLOBAL, "username-" + variablePath.replace("/", "-"), userName, variablePath.replace("/", "-"), conjurConfiguration, "CyberArk Conjur Provided");
                        usernameCredential.setStoreContext(getContext());
                        allCredentials.add(usernameCredential);
                        break;
                    case "usernamesshkeycredential":
                        ConjurSecretUsernameSSHKeyCredentials usernameSSHKeyCredential = (ConjurSecretUsernameSSHKeyCredentials) new ConjurSecretUsernameSSHKeyCredentialsImpl(CredentialsScope.GLOBAL, "usernamesshkey-" + variablePath.replace("/", "-"), userName, variablePath.replace("/", "-"), conjurConfiguration, null /* no passphrase yet */, "CyberArk Conjur Provided");
                        usernameSSHKeyCredential.setStoreContext(getContext());
                        allCredentials.add(usernameSSHKeyCredential);
                    break;
                    default:
                        break;
                }

                LOGGER.log(Level.FINEST, String.format("*** Variable Path: %s  userName:[%s]  credentialType:[%s]", variablePath, userName, credentialType));

            }

		} catch (IOException e) {
			LOGGER.log(Level.FINE, "EXCEPTION: CredentialSuplier => " + e.getMessage());
			// throw new InvalidConjurSecretException(e.getMessage(), e);
		}

        return allCredentials.stream()
                .map( cred -> {
                    return cred;
                })
                .collect(Collectors.toList());
    }

    private ModelObject getContext() {
        return this.context;
    }

}