import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class SampleClientTest {

    @Test
    public void testParseFamilyNamesFromFilename_WhenParsingValidFileOf10Names_ShouldProcessWithoutErrorsAnd10Results()
            throws IOException {
        String filename = "SampleClientTest-10FamilyNames.txt";
        List<String> familyNames = getSampleClient().parseFamilyNamesFromFilename(filename);
        assertNotNull(familyNames);
        assertEquals("Did not get expected number of results!", 10, familyNames.size());
    }

    @Test
    public void testParseFamilyNamesFromFilename_WhenParsingEmptyOrBlankFile_ShouldProcessWithoutErrorsAndNoResults()
            throws IOException {
        String zeroFilename = "SampleClientTest-0FamilyNames.txt";
        String blankFilename = "SampleClientTest-BlankFamilyNames.txt";
        List<String> familyNames = getSampleClient().parseFamilyNamesFromFilename(zeroFilename);
        assertNotNull(familyNames);
        assertEquals("Did not get expected number of results!", 0, familyNames.size());
        familyNames = getSampleClient().parseFamilyNamesFromFilename(blankFilename);
        assertNotNull(familyNames);
        assertEquals("Did not get expected number of results!", 0, familyNames.size());
    }

    @Test(expected = NullPointerException.class)
    public void testParseFamilyNamesFromFilename_WhenParsingInvalidFile_ShouldThrowNullPointerException()
            throws IOException {
        String invalidFilename = "ThisFileDoesNotExist-123.txt";
        List<String> familyNames = getSampleClient().parseFamilyNamesFromFilename(invalidFilename);
    }

    @Test
    public void testExecuteFHIRSearchForPatientByFamilyName_WhenPassingEmptyStringForName_GetNoResults() {
        SampleClient sampleClient = getSampleClient();
        Bundle bundle = sampleClient.executeFHIRSearchForPatientByFamilyName(SampleClient.createFHIRClient(),
                "", false);
        assertNotNull(bundle);
        assertTrue("Did not get 0 results when searching with empty FamilyName!", bundle.getTotal() == 0);
    }

    @Test
    public void testCalculateSearchTermResponseTimes_WhenPassingItNoNames_ReturnsEmptyResponseTimes() {
        List<String> familyNames = new ArrayList<>();
        Map<String, Long> responseTimes = getSampleClient()
                .calculateSearchTermResponseTimes(familyNames, true);
        assertNotNull(responseTimes);
        assertEquals("Did not get expected number of results!", 0, responseTimes.size());
    }

    @Test
    public void testExecuteFHIRSearchForPatientByFamilyName_WhenPassingNameSmith_GetAtLeastOneResult() {
        SampleClient sampleClient = getSampleClient();
        Bundle bundle = sampleClient.executeFHIRSearchForPatientByFamilyName(SampleClient.createFHIRClient(),
                "Smith", false);
        assertNotNull(bundle);
        assertTrue(!bundle.isEmpty());
        assertTrue("Did not get any results!", bundle.getTotal() > 0);
    }

    @Test
    public void testCalculateSearchTermResponseTimes_WhenPassingItThreeNames_ReturnsThreeResponseTimes() {
        List<String> familyNames = new ArrayList<>();
        familyNames.add("Peart");
        familyNames.add("Lee");
        familyNames.add("Lifeson");
        Map<String, Long> responseTimes = getSampleClient()
                .calculateSearchTermResponseTimes(familyNames, true);
        assertNotNull(responseTimes);
        assertEquals("Did not get expected number of results!", 3, responseTimes.size());
    }

    @Test
    public void testCalculateSearchTermResponseTimes_WhenSettingNoCache_GetsSlowerFinalSearch() {
        SampleClient sampleClient = getSampleClient();
        List<String> familyNames = new ArrayList<>();
        familyNames.add("Peart");
        familyNames.add("Lee");
        familyNames.add("Lifeson");
        Map<String, Long> responseTimes = getSampleClient()
                .calculateSearchTermResponseTimes(familyNames, true);
        assertNotNull(responseTimes);
        assertEquals("Did not get expected number of results!", 3, responseTimes.size());
        long secondLast = 0;
        long lastOne = 0;
        for (long responseTime : responseTimes.values()) {
            secondLast = lastOne;
            lastOne = responseTime;
        }
        assertTrue(secondLast < lastOne);
    }

    @Test
    public void testCreateFHIRClient_WhenAddingAnInterceptor_FHIRClientIsCreatedWithThatInterceptor() {
        IGenericClient fhirClient = getSampleClient().createFHIRClient(new SampleClientRequestTimingInterceptor());
        Optional<Object> timingInterceptor =
                fhirClient.getInterceptorService().getAllRegisteredInterceptors().stream()
                        .filter(SampleClientRequestTimingInterceptor.class::isInstance)
                        .findFirst();
        assertTrue(timingInterceptor.isPresent());
        IClientInterceptor myTimingInterceptor = (SampleClientRequestTimingInterceptor)timingInterceptor.get();
        assertNotNull(myTimingInterceptor);
    }

    // Helper Methods
    private SampleClient getSampleClient() {
        return new SampleClient("SMITH", "",
                1, true);
    }
}
