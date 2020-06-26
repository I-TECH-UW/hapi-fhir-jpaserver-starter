package ca.uhn.fhir.jpa.starter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.subscription.model.ResourceDeliveryMessage;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@Interceptor
public class SubscriptionRetryInterceptor {

	private FhirContext ctx;
	private final Logger ourLog = LoggerFactory.getLogger(SubscriptionRetryInterceptor.class);

	private Map<String, List<ResourceDeliveryMessage>> failedRestHooks = new ConcurrentHashMap<>();

	public SubscriptionRetryInterceptor(FhirContext ctx) {
		this.ctx = ctx;
	}

	@Hook(Pointcut.SUBSCRIPTION_BEFORE_DELIVERY)
	public boolean retryFailedHooksFirst(ResourceDeliveryMessage resourceDeliveryMessage) {
		String endpointUrl = resourceDeliveryMessage.getSubscription().getEndpointUrl();

		synchronized (failedRestHooks) {
			List<ResourceDeliveryMessage> failedPayloads = failedRestHooks.get(endpointUrl);

			if (failedPayloads == null || failedPayloads.isEmpty()) {
				return true;
			} else {
				boolean success = sendFailedNotifications(failedPayloads);
				if (!success) {
					saveFailedHook(resourceDeliveryMessage);
				}
				return success;
			}
		}

	}

	private boolean sendFailedNotifications(List<ResourceDeliveryMessage> failedPayloads) {
		for (ResourceDeliveryMessage failedPayload : failedPayloads) {
			if (!retryPayload(failedPayload)) {
				return false;
			}
		}
		return true;
	}

	private boolean retryPayload(ResourceDeliveryMessage failedPayload) {
		IBaseResource resource = failedPayload.getPayload(ctx);
		String endpointUrl = failedPayload.getSubscription().getEndpointUrl();
		try {
			IGenericClient fhirClient = ctx.newRestfulGenericClient(endpointUrl);

			switch (failedPayload.getOperationType()) {
			case UPDATE:
			case CREATE:
				fhirClient.update().resource(resource).execute();
			case DELETE:
			default:
				break;
			}
			return true;
		} catch (RuntimeException e) {
			ourLog.error("error communicating resource to " + endpointUrl);
			return false;
		}
	}

	@Hook(Pointcut.SUBSCRIPTION_AFTER_DELIVERY_FAILED)
	public void saveFailedHook(ResourceDeliveryMessage resourceDeliveryMessage) {
		String endpointUrl = resourceDeliveryMessage.getSubscription().getEndpointUrl();

		synchronized (failedRestHooks) {
			List<ResourceDeliveryMessage> failedPayloads = failedRestHooks.get(endpointUrl);

			if (failedPayloads == null) {
				failedPayloads = new ArrayList<>();
				failedRestHooks.put(endpointUrl, failedPayloads);
			}
			failedPayloads.add(resourceDeliveryMessage);
		}

	}

}
