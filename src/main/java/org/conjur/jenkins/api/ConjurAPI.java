package org.conjur.jenkins.api;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.conjur.jenkins.configuration.ConjurConfiguration;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.model.Run;
import hudson.security.ACL;
import io.github.openunirest.http.HttpResponse;
import io.github.openunirest.http.Unirest;
import io.github.openunirest.http.exceptions.UnirestException;
import io.github.openunirest.request.HttpRequestWithBody;
import jenkins.model.Jenkins;


public class ConjurAPI {

	private static final Logger LOGGER = Logger.getLogger( ConjurAPI.class.getName());

	public static String getAuthorizationToken(ConjurConfiguration configuration, Run<?, ?> context) {
		
		String resultingToken = null;
		
		UsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(
	            CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
	                    Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
	            CredentialsMatchers.withId(configuration.getCredentialID())
	    );
				
		try {
			LOGGER.log(Level.INFO, "Authenticating with Conjur");
			HttpResponse<String> response = Unirest.post(String.format("%s/authn/{account}/{username}/authenticate", configuration.getApplianceURL()))
					  .routeParam("account", configuration.getAccount())
					  .routeParam("username", credential.getUsername())
					  .body(credential.getPassword().getPlainText())
					  .asString();
			resultingToken = Base64.getEncoder().withoutPadding().encodeToString(response.getBody().getBytes("UTF-8"));
			LOGGER.log(Level.INFO,  "Conjur Authenticate response " + response.getStatus() + " - " + resultingToken);
		} catch (NullPointerException | UnirestException | UnsupportedEncodingException e) {
			LOGGER.log(Level.SEVERE, "Error during Conjur Authentication: " + e.getMessage());
			e.printStackTrace();
		}
		
		return resultingToken;
	}
	
	public static String getSecret(ConjurConfiguration configuration, String authToken, String variablePath) {
		String result = null;
		
		HttpResponse<String> response = Unirest.get(String.format("%s/secrets/{account}/variable/{variablePath}", configuration.getApplianceURL()))
				  .routeParam("account", configuration.getAccount())
				  .routeParam("variablePath", variablePath)
				  .header("Authorization", "Token token=\"" + authToken +"\"")
				  .asString();
		
		result = response.getBody();
		
		return result;
	}
	

}
