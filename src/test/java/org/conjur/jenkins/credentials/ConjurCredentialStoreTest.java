package org.conjur.jenkins.credentials;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.conjur.jenkins.conjursecrets.ConjurSecretCredentialsImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConjurCredentialStore.class,ConjurSecretCredentialsImpl.class })
@PowerMockIgnore({ "javax.management.", "com.sun.org.apache.xerces.", "javax.xml.", "org.xml.", "org.w3c.dom.","com.sun.org.apache.xalan.", "javax.activation.*", "jdk.internal.reflect.*,java.util.logging.*" ,"javax.xml.*","org.xml.*", "javax.management.*, org.mockito.*"})
public class ConjurCredentialStoreTest {

	
	
	
	@Test
	public void mockAddCredential() throws IOException {
		PowerMockito.mockStatic(ConjurCredentialStore.class);
	    ConjurCredentialStore conjurCredentialStore= mock(ConjurCredentialStore.class);
	     
		ConjurSecretCredentialsImpl conjurSecretCredentialsImplAdd = new ConjurSecretCredentialsImpl(CredentialsScope.GLOBAL, "DB_SECRET",
				"db/db_password", "Conjur Secret");
		PowerMockito.when(conjurCredentialStore.addCredentials(Domain.global(), conjurSecretCredentialsImplAdd)).thenReturn(true);
		assertTrue(conjurCredentialStore.addCredentials(Domain.global(), conjurSecretCredentialsImplAdd));

	}
	
	
	@Test
	public void mockRemoveCredential() throws IOException {
		 PowerMockito.mockStatic(ConjurCredentialStore.class);
	     ConjurCredentialStore conjurCredentialStore1Remove= mock(ConjurCredentialStore.class);
	     
		 ConjurSecretCredentialsImpl conjurSecretCredentialsImplRemove = new ConjurSecretCredentialsImpl(CredentialsScope.GLOBAL, "DB_SECRET",
				"db/db_password", "Conjur Secret");
		 conjurCredentialStore1Remove.removeCredentials(Domain.global(), conjurSecretCredentialsImplRemove);
	}
	
	
	@Test
	public void mockUpdateCredential() throws IOException {
		 PowerMockito.mockStatic(ConjurCredentialStore.class);
	     ConjurCredentialStore conjurCredentialStoreUpdate= mock(ConjurCredentialStore.class);
	     
		 ConjurSecretCredentialsImpl conjurSecretCredentialsImplUpdate = new ConjurSecretCredentialsImpl(CredentialsScope.GLOBAL, "DB_SECRET",
				"db/db_password", "Conjur Secret");
		 ConjurSecretCredentialsImpl conjurSecretCredentialsImplUpdate2= new ConjurSecretCredentialsImpl(CredentialsScope.GLOBAL, "DB_SECRET1",
				"db/db_password", "Conjur Secret");
	     conjurCredentialStoreUpdate.updateCredentials(Domain.global(), conjurSecretCredentialsImplUpdate, conjurSecretCredentialsImplUpdate2);
		 

	}

}
