package org.conjur.jenkins.credentials;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConjurCredentialProvider.class })
public class ConjurCredentialProviderTest {
	
	@Mock
	public ConjurCredentialProvider provider;


	@Test
	public void getStoreTest() {
		ConjurCredentialStore store = null;
		 when(provider.getStore(any())).thenReturn(store);
		 assertFalse(provider.getStore(any()) instanceof ConjurCredentialStore);
	}

	@Test(expected = RuntimeException.class)
	public void getCredentialsTest() throws Exception {
		ConjurCredentialProvider classUnderTest = PowerMockito.spy(new ConjurCredentialProvider());
		PowerMockito.when(classUnderTest, "getCredentialsFromSupplier", any(), any(), any())
				.thenReturn(Collections.emptyList());

	}
}
