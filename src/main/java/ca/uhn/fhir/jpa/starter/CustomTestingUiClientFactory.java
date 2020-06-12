package ca.uhn.fhir.jpa.starter;

import javax.servlet.http.HttpServletRequest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.util.ITestingUiClientFactory;

public class CustomTestingUiClientFactory implements ITestingUiClientFactory {

	FhirContext theFhirContext;

	public CustomTestingUiClientFactory(FhirContext theFhirContext) {
		this.theFhirContext = theFhirContext;
	}


	@Override
	public IGenericClient newClient(FhirContext theFhirContext, HttpServletRequest theRequest,
			String theServerBaseUrl) {
		// Create a client
		return this.theFhirContext.newRestfulGenericClient(theServerBaseUrl);
	}

}
