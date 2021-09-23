package org.conjur.jenkins.credentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardCredentials;

import org.conjur.jenkins.api.ConjurAPI;
import org.conjur.jenkins.api.ConjurAPIUtils;
import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.conjursecrets.ConjurSecretCredentials;
import org.conjur.jenkins.conjursecrets.ConjurSecretCredentialsImpl;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameCredentials;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameCredentialsImpl;
import org.conjur.jenkins.conjursecrets.ConjurSecretUsernameSSHKeyCredentialsImpl;
import org.conjur.jenkins.exceptions.InvalidConjurSecretException;
import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


import hudson.model.ModelObject;
// import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
        LOGGER.log(Level.INFO,"Retrieve secrets from CyberArk Conjur -- Context => " + context);
        final Collection<StandardCredentials> allCredentials = new ArrayList<>();


		String result = "";
		try {
            ConjurConfiguration conjurConfiguration = ConjurAPI.getConfigurationFromContext(context);
			// Get Http Client
			OkHttpClient client = ConjurAPIUtils.getHttpClient(conjurConfiguration);
			// Authenticate to Conjur
			String authToken = ConjurAPI.getAuthorizationToken(client, conjurConfiguration, context);
			// // Retrieve secret from Conjur
			// String secretString = ConjurAPI.getSecret(client, conjurConfiguration, authToken, this.variablePath);
			// result = secretString;
            LOGGER.log(Level.INFO, "authToken=" + authToken);




            ConjurAPI.ConjurAuthnInfo conjurAuthn = ConjurAPI.getConjurAuthnInfo(conjurConfiguration, null, null);

            LOGGER.log(Level.INFO, "Fetching secret from Conjur");
            Request request = new Request.Builder().url(
                    String.format("%s/resources/%s?kind=variable&limit=1000", conjurAuthn.applianceUrl, conjurAuthn.account))
                    .get().addHeader("Authorization", "Token token=\"" + authToken + "\"").build();
    
            Response response = client.newCall(request).execute();
            result = response.body().string();
            // LOGGER.log(Level.INFO, () -> "Fetch secret [" + variablePath + "] from Conjur response " + response.code()
            //         + " - " + response.message());
            LOGGER.log(Level.INFO, "RESULT => " + result);
            if (response.code() != 200) {
                throw new IOException("Error fetching secret from Conjur [" + response.code() + " - " + response.message()
                        + "\n" + result);
            }

            JSONArray resultResources = new JSONArray(result);
            for (int i = 0; i < resultResources.length(); i++) {
                JSONObject resource = resultResources.getJSONObject(i);
                LOGGER.log(Level.INFO, "resource => " + resource.toString(4));


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



                ConjurSecretCredentialsImpl credential = new ConjurSecretCredentialsImpl(CredentialsScope.GLOBAL, variablePath.replace("/", "-"), variablePath, "CyberArk Conjur Provided");
                allCredentials.add(credential);
                switch (credentialType) {
                    case "usernamecredential":
                        // public ConjurSecretUsernameCredentialsImpl(CredentialsScope scope, String id, String username, String credentialID,
                        // ConjurConfiguration conjurConfiguration, String description) {
                        // public ConjurSecretCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id,
                        // @CheckForNull String variablePath, @CheckForNull String description) {
                        ConjurSecretUsernameCredentialsImpl usernameCredential = new ConjurSecretUsernameCredentialsImpl(CredentialsScope.GLOBAL, "username-" + variablePath.replace("/", "-"), userName, variablePath.replace("/", "-"), conjurConfiguration, "CyberArk Conjur Provided");
                            
                        // CredentialsScope.SYSTEM, "conjur-username-" + variablePath, variablePath, "CyberArk Conjur Provided");
                        allCredentials.add(usernameCredential);
                        break;
                    case "usernamesshkeycredential":
                    // ConjurSecretCredentialsImpl credential = new ConjurSecretCredentialsImpl(CredentialsScope.SYSTEM, variablePath, variablePath, "CyberArk Conjur Provided");

                    // // public ConjurSecretUsernameSSHKeyCredentialsImpl(final CredentialsScope scope, final String id,
                    // // final String username, final String credentialID, final ConjurConfiguration conjurConfiguration,
                    // // final Secret passphrase, final String description) {

                        ConjurSecretUsernameSSHKeyCredentialsImpl usernameSSHKeyCredential = new ConjurSecretUsernameSSHKeyCredentialsImpl(CredentialsScope.GLOBAL, "usernamesshkey-" + variablePath.replace("/", "-"), userName, variablePath.replace("/", "-"), conjurConfiguration, null /* no passphrase yet */, "CyberArk Conjur Provided");
                        
                    // // CredentialsScope.SYSTEM, "conjur-username-" + variablePath, variablePath, "CyberArk Conjur Provided");
                    // allCredentials.add(credential);
                    allCredentials.add(usernameSSHKeyCredential);
                    break;
                    default:
                        break;
                }

                LOGGER.log(Level.INFO, String.format("*** Variable Path: %s  userName:[%s]  credentialType:[%s]", variablePath, userName, credentialType));

            }



		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "EXCEPTION: CredentialSuplier => " + e.getMessage());
			// throw new InvalidConjurSecretException(e.getMessage(), e);
		}





        // final PluginConfiguration config = PluginConfiguration.getInstance();

        // final Collection<Filter> filters = createListSecretsFilters(config);

        // final AWSSecretsManager client = createClient(config);

        // final ListSecretsOperation listSecretsOperation = new ListSecretsOperation(client, filters);

        // final Collection<SecretListEntry> secretList = listSecretsOperation.get();

        // return secretList.stream()
        //         .flatMap(secretListEntry -> {
        //             final String name = secretListEntry.getName();
        //             final String description = Optional.ofNullable(secretListEntry.getDescription()).orElse("");
        //             final Map<String, String> tags = Lists.toMap(secretListEntry.getTags(), Tag::getKey, Tag::getValue);
        //             final Optional<StandardCredentials> cred = CredentialsFactory.create(name, description, tags, client);
        //             return Optionals.stream(cred);
        //         })
        //         .collect(Collectors.toList());

        // return allCredentials.stream()
        // .flatMap(record -> {
            
        // })
        // // .flatMap(record -> toStream(record.getJSONArray("Resources"))
        // //         .filter(resource -> ! resource.getString("name").equals("Credit"))
        // //         .map(resource -> resource.put("id", record.getString("id")))
        // .collect(Collectors.toList());

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