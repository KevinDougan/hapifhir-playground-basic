import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

import java.io.IOException;

public class SampleClientRequestTimingInterceptor implements IClientInterceptor {
    private long timingResult;

    public SampleClientRequestTimingInterceptor() {
        super();
    }

    @Override
    public void interceptRequest(IHttpRequest iHttpRequest) {
        // Do nothing
    }

    @Override
    public void interceptResponse(IHttpResponse iHttpResponse) throws IOException {
        timingResult = iHttpResponse.getRequestStopWatch().getMillis();
    }

    public long getTimingResult() {
        return timingResult;
    }
}
