package ca.uhn.fhir.jpa.starter.interceptor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.http.MediaType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;

public class OEForwardingInterceptor extends InterceptorAdapter {

    private FhirContext fhirContext;

    public OEForwardingInterceptor(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    @Override
    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void incomingRequestPreHandled(RestOperationTypeEnum theOperation,
            ActionRequestDetails theProcessedRequest) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            if ((theProcessedRequest.getResource() != null)) {

                URI uri = new URI(HapiProperties.getOpenELISApiAddress() + theProcessedRequest.getResourceType());
                HttpPost httpPost = new HttpPost(uri);

				// TODO this will need to be secured before it is production ready
//                if (uri.getHost().equals("localhost")) {
                    SSLContextBuilder builder = new SSLContextBuilder();
                    builder.loadTrustMaterial(null, TrustAllStrategy.INSTANCE);
                    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
                    httpClient = HttpClients.custom().setSSLSocketFactory(sslsf)
                            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
//                }

                UsernamePasswordCredentials creds = new UsernamePasswordCredentials(
                        HapiProperties.getOpenELISApiUsername(), HapiProperties.getOpenELISApiPassword());
                httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));
                httpPost.setHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
                httpPost.setHeader("Content-type", MediaType.APPLICATION_JSON_VALUE);

                String resourceAsJson = fhirContext.newJsonParser()
                        .encodeResourceToString(theProcessedRequest.getResource());
                StringEntity entity = new StringEntity(resourceAsJson);
                httpPost.setEntity(entity);

                System.out.println("sending from JPAServer: " + resourceAsJson);

                response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() != 200) {
					System.out.println(uri.toString());
					System.out.println(response.getStatusLine().getStatusCode());
					System.out.println(response.getStatusLine().getReasonPhrase());
                    throw new UnclassifiedServerFailureException(500, "Could not save object in OE Database");
                }
            }
        } catch (URISyntaxException | IOException | AuthenticationException | NoSuchAlgorithmException
                | KeyStoreException | KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new UnclassifiedServerFailureException(500, "Error in JPA Server while reaching OE Server");
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

}
