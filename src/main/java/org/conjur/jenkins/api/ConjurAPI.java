package org.conjur.jenkins.api;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.hudson.plugins.folder.AbstractFolder;

import org.conjur.jenkins.configuration.ConjurConfiguration;
import org.conjur.jenkins.configuration.ConjurJITJobProperty;
import org.conjur.jenkins.configuration.GlobalConjurConfiguration;
import org.conjur.jenkins.configuration.FolderConjurConfiguration;
import org.conjur.jenkins.jwtauth.impl.JwtToken;

import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.remoting.Channel;
import hudson.security.ACL;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConjurAPI {

	public static class ConjurAuthnInfo {
		public String applianceUrl;
		public String authnPath;
		public String account;
		public String login;
		public String apiKey;
	}

	private static final Logger LOGGER = Logger.getLogger(ConjurAPI.class.getName());

	static Logger getLogger() {
		return Logger.getLogger(ConjurAPI.class.getName());
	}

	private static void defaultToEnvironment(ConjurAuthnInfo conjurAuthn) {
		Map<String, String> env = System.getenv();
		if (conjurAuthn.applianceUrl == null && env.containsKey("CONJUR_APPLIANCE_URL"))
			conjurAuthn.applianceUrl = env.get("CONJUR_APPLIANCE_URL");
		if (conjurAuthn.account == null && env.containsKey("CONJUR_ACCOUNT"))
			conjurAuthn.account = env.get("CONJUR_ACCOUNT");
		if (conjurAuthn.login == null && env.containsKey("CONJUR_AUTHN_LOGIN"))
			conjurAuthn.login = env.get("CONJUR_AUTHN_LOGIN");
		if (conjurAuthn.apiKey == null && env.containsKey("CONJUR_AUTHN_API_KEY"))
			conjurAuthn.apiKey = env.get("CONJUR_AUTHN_API_KEY");
	}

	public static String getAuthorizationToken(OkHttpClient client, ConjurConfiguration configuration,
			ModelObject context) throws IOException {

		String resultingToken = null;

		Channel channel = Channel.current();

		List<UsernamePasswordCredentials> availableCredentials = null;

		if (channel == null) {
			availableCredentials = CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
					Jenkins.get(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList());

			if (context != null) {
				if (context instanceof Run) {
					availableCredentials.addAll(CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
					((Run) context).getParent(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()));
				} else if (context instanceof Item) {
					availableCredentials.addAll(CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
					(Item) context, ACL.SYSTEM, Collections.<DomainRequirement>emptyList()));
				}
			}
		} else {
			try {
				availableCredentials = channel.call(new ConjurAPIUtils.NewAvailableCredentials());
			} catch (InterruptedException e) {
				getLogger().log(Level.INFO, "Exception getting available credentials", e);
				e.printStackTrace();
			}
		}

		ConjurAuthnInfo conjurAuthn = getConjurAuthnInfo(configuration, availableCredentials, context);

		Request request = null;
		if (conjurAuthn.login != null && conjurAuthn.apiKey != null) {
			LOGGER.log(Level.INFO, "Authenticating with Conjur (authn)");
			request = new Request.Builder()
				.url(String.format("%s/%s/%s/%s/authenticate", conjurAuthn.applianceUrl, conjurAuthn.authnPath,
						conjurAuthn.account, URLEncoder.encode(conjurAuthn.login, "utf-8")))
				.post(RequestBody.create(MediaType.parse("text/plain"), conjurAuthn.apiKey)).build();
		} else if (conjurAuthn.authnPath != null & conjurAuthn.apiKey != null) {
			LOGGER.log(Level.INFO, "Authenticating with Conjur (JWT)");
			request = new Request.Builder()
				.url(String.format("%s/%s/%s/authenticate", conjurAuthn.applianceUrl, conjurAuthn.authnPath,
						conjurAuthn.account))
				.post(RequestBody.create(MediaType.parse("text/plain"), conjurAuthn.apiKey)).build();

		}

		if (request != null) {
			Response response = client.newCall(request).execute();
			resultingToken = Base64.getEncoder().withoutPadding()
					.encodeToString(response.body().string().getBytes("UTF-8"));
			LOGGER.log(Level.INFO,
					() -> "Conjur Authenticate response " + response.code() + " - " + response.message());
			if (response.code() != 200) {
				throw new IOException("Error authenticating to Conjur [" + response.code() + " - " + response.message()
						+ "\n" + resultingToken);
			}
		} else {
			LOGGER.log(Level.INFO, "Failed to find credentials for conjur authentication");
		}

		return resultingToken;
	}

	public static ConjurAuthnInfo getConjurAuthnInfo(ConjurConfiguration configuration,
			List<UsernamePasswordCredentials> availableCredentials, ModelObject context) {
		// Conjur variables
		LOGGER.log(Level.INFO, "getting conjut authentication info getConjurAuthnInfo");
		ConjurAuthnInfo conjurAuthn = new ConjurAuthnInfo();

		if (configuration != null) {

			if (availableCredentials != null) {
				initializeWithCredential(conjurAuthn, configuration.getCredentialID(), availableCredentials);
			}

			String applianceUrl = configuration.getApplianceURL();
			if (applianceUrl != null && !applianceUrl.isEmpty()) {
				conjurAuthn.applianceUrl = applianceUrl;
			}
			String account = configuration.getAccount();
			if (account != null && !account.isEmpty()) {
				conjurAuthn.account = account;
			}
			// Default authentication will be authn
			conjurAuthn.authnPath = "authn";
		}

		// Default to Environment variables if not values present
		defaultToEnvironment(conjurAuthn);

		// Check for Just-In-time Credential Access
		if (context != null) {
			LOGGER.log(Level.INFO, "calling setConjurAuthnForJITCredentialAccess");
			setConjurAuthnForJITCredentialAccess(context, conjurAuthn);
		}

		LOGGER.log(Level.INFO, "Returning conjurAuthn=" + conjurAuthn);

		return conjurAuthn;
	}

	// private static String signatureForRequest(String challenge, RSAPrivateKey privateKey) {
	// 	// sign using the private key
	// 	LOGGER.log(Level.INFO, "Challenge: {0}", challenge);
	// 	try {
	// 		Signature sig = Signature.getInstance("SHA256withRSA");
	// 		sig.initSign(privateKey);
	// 		sig.update(challenge.getBytes("UTF8"));
	// 		String signatureString = Base64.getEncoder().encodeToString(sig.sign());
	// 		LOGGER.log(Level.INFO, "*** SignatureString: {0}", signatureString);
	// 		return signatureString;
	// 	} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
	// 		e.printStackTrace();
	// 	} catch (UnsupportedEncodingException e) {
	// 		e.printStackTrace();
	// 	}
	// 	return null;
	// }

	// private static String apiKeyForAuthentication(String prefix, String buildNumber, String signature,
	// 		String keyAlgorithm) {
	// 	// Build the response Body
	// 	Map<String, String> body = new HashMap<String, String>();
	// 	body.put("buildNumber", buildNumber);
	// 	body.put("signature", signature);
	// 	body.put("keyAlgorithm", keyAlgorithm);
	// 	if (prefix != null && prefix.length() > 0) {
	// 		body.put("jobProperty_hostPrefix", prefix);
	// 	}

	// 	ObjectMapper objectMapper = new ObjectMapper();

	// 	try {
	// 		return objectMapper.writeValueAsString(body);
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 	}
	// 	return null;
	// }

	private static void setConjurAuthnForJITCredentialAccess(ModelObject context, ConjurAuthnInfo conjurAuthn) {

		String token = JwtToken.getToken(context);
		GlobalConjurConfiguration globalconfig = GlobalConfiguration.all().get(GlobalConjurConfiguration.class);

		if (token != null && globalconfig != null) {
			conjurAuthn.login = null;
			conjurAuthn.authnPath = globalconfig.getAuthWebServiceId();
			conjurAuthn.apiKey = "jwt=" + token;
		}
		
		// ConjurJITJobProperty conjurJobConfig = context.getParent().getProperty(ConjurJITJobProperty.class);
		// if (conjurJobConfig != null && conjurJobConfig.getUseJustInTime()) {
		// 	String jobName = context.getParent().getFullName();
		// 	int buildNumber = context.getNumber();
		// 	LOGGER.log(Level.INFO, "++++++ JobName: " + jobName + "  Build Number: " + buildNumber);
		// 	String prefix = conjurJobConfig.getHostPrefix();
		// 	LOGGER.log(Level.INFO, "PREFIX: {0}", prefix);
		// 	RSAPrivateKey privateKey = InstanceIdentity.get().getPrivate();
		// 	LOGGER.log(Level.INFO, privateKey.getAlgorithm());
		// 	conjurAuthn.login = "host/" + (prefix != null && prefix.length() > 0 ? prefix + "/" : "") + jobName;
		// 	conjurAuthn.authnPath = "authn-jenkins/" + conjurJobConfig.getAuthWebServiceId();
		// 	conjurAuthn.apiKey = apiKeyForAuthentication(prefix, String.valueOf(buildNumber),
		// 			signatureForRequest(jobName + "-" + buildNumber, privateKey), privateKey.getAlgorithm());
		// 	LOGGER.log(Level.INFO, "*** passwordBody: {0}", conjurAuthn.apiKey);
		// }
	}

	public static String getSecret(OkHttpClient client, ConjurConfiguration configuration, String authToken,
		String variablePath) throws IOException {
		String result = null;

		ConjurAuthnInfo conjurAuthn = getConjurAuthnInfo(configuration, null, null);

		LOGGER.log(Level.INFO, "Fetching secret from Conjur");
		Request request = new Request.Builder().url(
				String.format("%s/secrets/%s/variable/%s", conjurAuthn.applianceUrl, conjurAuthn.account, variablePath))
				.get().addHeader("Authorization", "Token token=\"" + authToken + "\"").build();

		Response response = client.newCall(request).execute();
		result = response.body().string();
		LOGGER.log(Level.INFO, () -> "Fetch secret [" + variablePath + "] from Conjur response " + response.code()
				+ " - " + response.message());
		if (response.code() != 200) {
			throw new IOException("Error fetching secret from Conjur [" + response.code() + " - " + response.message()
					+ "\n" + result);
		}

		return result;
	}

	public static ConjurConfiguration logConjurConfiguration(ConjurConfiguration conjurConfiguration) {
		if (conjurConfiguration != null) {
			LOGGER.log(Level.INFO, "Conjur configuration provided");
			LOGGER.log(Level.INFO, "Conjur Configuration Appliance Url: " + conjurConfiguration.getApplianceURL());
			LOGGER.log(Level.INFO, "Conjur Configuration Account: " + conjurConfiguration.getAccount());
			LOGGER.log(Level.INFO, "Conjur Configuration credential ID: " + conjurConfiguration.getCredentialID());
		}
		return conjurConfiguration;
	}

	private static void initializeWithCredential(ConjurAuthnInfo conjurAuthn, String credentialID,
			List<UsernamePasswordCredentials> availableCredentials) {
		if (credentialID != null && !credentialID.isEmpty()) {
			LOGGER.log(Level.INFO, "Retrieving Conjur credential stored in Jenkins");
			UsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(availableCredentials,
					CredentialsMatchers.withId(credentialID));
			if (credential != null) {
				conjurAuthn.login = credential.getUsername();
				conjurAuthn.apiKey = credential.getPassword().getPlainText();
			}
		}
	}

	public static ConjurConfiguration getConfigurationFromContext(ModelObject context) {
		LOGGER.log(Level.INFO, "Getting Configuration from Context");

        Item contextObject = null;
		ConjurJITJobProperty conjurJobConfig = null;


        if (context instanceof Run) {
            Run run = (Run) context;
			conjurJobConfig = (ConjurJITJobProperty) run.getParent().getProperty(ConjurJITJobProperty.class);
            contextObject = run.getParent();
        } else if (context instanceof AbstractItem) {
			contextObject = (Item) context;
		}


		ConjurConfiguration conjurConfig = GlobalConjurConfiguration.get().getConjurConfiguration();

		if (context == null) {
			return ConjurAPI.logConjurConfiguration(conjurConfig);
		}

		if (conjurJobConfig != null && !conjurJobConfig.getInheritFromParent()) {
			// Taking the configuration from the Job
			return ConjurAPI.logConjurConfiguration(conjurJobConfig.getConjurConfiguration());
		}

		ConjurConfiguration inheritedConfig = inheritedConjurConfiguration(contextObject);
		if (inheritedConfig != null) {
			return ConjurAPI.logConjurConfiguration(inheritedConfig);
		}

		return ConjurAPI.logConjurConfiguration(conjurConfig);

	}

	@SuppressWarnings("unchecked")
	private static ConjurConfiguration inheritedConjurConfiguration(Item job) {
		for (ItemGroup<? extends Item> g = job
				.getParent(); g instanceof AbstractFolder; g = ((AbstractFolder<? extends Item>) g).getParent()) {
			FolderConjurConfiguration fconf = ((AbstractFolder<?>) g).getProperties()
					.get(FolderConjurConfiguration.class);
			if (!(fconf == null || fconf.getInheritFromParent())) {
				// take the folder Conjur Configuration
				return fconf.getConjurConfiguration();
			}
		}
		return null;
	}


	private ConjurAPI() {
		super();
	}

}
