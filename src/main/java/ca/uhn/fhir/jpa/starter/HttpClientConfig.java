package ca.uhn.fhir.jpa.starter;

import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.to.TesterConfig;

@Configuration
@PropertySource("file:/etc/openelis-global/common_ssl.properties")
public class HttpClientConfig {

	@Value("${server.ssl.trust-store}")
	private Resource trustStore;

	@Value("${server.ssl.trust-store-password}")
	private String trustStorePassword;

	@Value("${server.ssl.key-store}")
	private Resource keyStore;

	@Value("${server.ssl.key-store-password}")
	private String keyStorePassword;

	@Value("${server.ssl.key-password}")
	private String keyPassword;

	@Autowired
	private FhirContext fhirContext;
	@Autowired
	private Optional<TesterConfig> testerConfig;

	public SSLConnectionSocketFactory sslConnectionSocketFactory() throws Exception {
		return new SSLConnectionSocketFactory(sslContext());
	}

	public SSLContext sslContext() throws Exception {
		return SSLContextBuilder.create()
				.loadKeyMaterial(keyStore.getFile(), keyStorePassword.toCharArray(), keyPassword.toCharArray())
				.loadTrustMaterial(trustStore.getFile(), trustStorePassword.toCharArray()).build();
	}

	@Bean
	public HttpClient httpClient() throws Exception {
		HttpClient httpClient = HttpClientBuilder.create()//
				.setSSLSocketFactory(sslConnectionSocketFactory())//
				.build();//

		IRestfulClientFactory clientFactory = new ApacheRestfulClientFactory(fhirContext);
		clientFactory.setHttpClient(httpClient);
		fhirContext.setRestfulClientFactory(clientFactory);
		if (testerConfig.isPresent()) {
			testerConfig.get().setClientFactory(new CustomTestingUiClientFactory(fhirContext));
		}
		return httpClient;
	}
}
