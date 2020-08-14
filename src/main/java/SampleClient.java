import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class SampleClient {
    private static final Logger logger = LoggerFactory.getLogger(SampleClient.class);
    static final String FHIR_RESTFULAPI_BASER4_URL = "http://hapi.fhir.org/baseR4";
    static String BASICTASK_FAMILYNAME = "SMITH";
    static String DEFAULT_FILENAME = "SampleClient-FamilyNames.txt";
    static int NUM_ITERATIONS = 3;
    static boolean DISABLE_CACHING_ON_FINAL_ITERATION = true;

    public SampleClient(final String basicTaskFamilyName, final String defaultFilename,
                        final int numIterations, final boolean disableCachingOnFinalIteration) {
        SampleClient.BASICTASK_FAMILYNAME = basicTaskFamilyName;
        SampleClient.DEFAULT_FILENAME = defaultFilename;
        SampleClient.NUM_ITERATIONS = numIterations;
        SampleClient.DISABLE_CACHING_ON_FINAL_ITERATION = disableCachingOnFinalIteration;
    }

    /*
    *  The main() method accepts an optional single String argument which is the name
    *  of a file that contains a list of FamilyName Search Terms. (Intermediate Task)
    *  If no args are passed in, this method will perform a hard-coded search for
    *  the FamilyName "SMITH" and print the results to System.out. (Basic Task)
     */
    public static void main(String[] theArgs) {
        try {
            if (theArgs != null && theArgs.length > 0) {
                executeIntermediateTask(theArgs[0]);
            } else {
                // Fall back to the previous behaviour if no args found
                executeBasicTask();
            }
        } catch(Throwable t) {
            System.out.println("Throwable caught in main(): " + t.getMessage());
            t.printStackTrace();
            System.exit(9);
        }
    }

    private static void executeIntermediateTask(String inputFilename) throws IOException {
        List<String> familyNames = null;

        // This is a bit of overkill but I want to filter out invalid filenames like "   "
        if (inputFilename != null && !inputFilename.trim().isEmpty()) {
            inputFilename = DEFAULT_FILENAME;
        }
        familyNames = parseFamilyNamesFromFilename(inputFilename.trim());
        Objects.requireNonNull(familyNames,
                "NULL familyNames List returned rom call to parseFamilyNamesFromFilename('"
                        + inputFilename.trim() + "'");
        double[] averageTimingPerExecutionRun = new double[NUM_ITERATIONS];
        for (int i = 1; i <= NUM_ITERATIONS; i++) {
            boolean setNoCache = false;
            if (i == NUM_ITERATIONS && DISABLE_CACHING_ON_FINAL_ITERATION) {
                setNoCache = true;
            }
            Map<String, Long> responseTimesPerSearchTerm =
                    calculateSearchTermResponseTimes(familyNames, setNoCache);
            Objects.requireNonNull(responseTimesPerSearchTerm);
            OptionalDouble averageResponseTime = responseTimesPerSearchTerm.values().stream().mapToLong(a -> a).average();
            averageTimingPerExecutionRun[i-1] = (averageResponseTime.isPresent() ? averageResponseTime.getAsDouble() : 0);
        }

        // Print out the average response time results
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            System.out.println("Average Response Time for Execution iteration " + (i+1) + " is "
                    + averageTimingPerExecutionRun[i] + "ms");
        }

    }

    public static List<String> parseFamilyNamesFromFilename(final String filename) throws IOException {
        List<String> familyNames = new ArrayList<>();
        logger.debug("Current Directory: " + new File(".").getAbsolutePath());
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(filename);
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr);) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    familyNames.add(line.trim());
                }
            }
        } catch (FileNotFoundException fnfe) {
            System.out.println("File Not Found! " + fnfe.getMessage());
            throw fnfe;
        } catch (IOException ioe) {
            System.out.println("IO Exception! " + ioe.getMessage());
            throw ioe;
        }
        return familyNames;
    }

    // Package visibility so it can be accessed from a Test Class in the exact same package
    public static Map<String, Long> calculateSearchTermResponseTimes(final List<String> familyNames,
                                                              final boolean setNoCache) {
        Map<String, Long> responseTimes = new HashMap<>();

        for (String familyName : familyNames) {
            logger.debug("Processing familyName '" + familyName + "'");
            IGenericClient client = createFHIRClient(new SampleClientRequestTimingInterceptor());
            Bundle response = executeFHIRSearchForPatientByFamilyName(client, familyName, setNoCache);
            Optional<Object> timingInterceptor =
                    client.getInterceptorService().getAllRegisteredInterceptors().stream()
                    .filter(SampleClientRequestTimingInterceptor.class::isInstance)
                    .findFirst();
            if (timingInterceptor.isPresent()) {
                responseTimes.put(familyName,
                        ((SampleClientRequestTimingInterceptor)timingInterceptor.get()).getTimingResult());
            }
        }
        return responseTimes;
    }

    // Overloaded function showing how to implement the same functionality with different numbers of parameters
    public static IGenericClient createFHIRClient() {
        return createFHIRClient(null);
    }

    // Overloaded method that accepts an IClientInterceptor parameter
    public static IGenericClient createFHIRClient(final IClientInterceptor interceptor) {
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient(FHIR_RESTFULAPI_BASER4_URL);
        client.registerInterceptor(new LoggingInterceptor(false));
        if (interceptor != null) {
            client.registerInterceptor(interceptor);
        }
        return client;
    }


    public static Bundle executeFHIRSearchForPatientByFamilyName(final IGenericClient client,
                                                          final String familyName) {
        return executeFHIRSearchForPatientByFamilyName(client, familyName, false);
    }

    public static Bundle executeFHIRSearchForPatientByFamilyName(final IGenericClient client,
                                                          final String familyName,
                                                          final boolean setNoCache) {
        // I don't like using hard-coded Strings so I'm passing Patient.class to forResource()
        return client
                .search()
                .forResource(Patient.class)
                .where(Patient.FAMILY.matches().value(familyName))
                .cacheControl(new CacheControlDirective().setNoCache(setNoCache))
                .returnBundle(Bundle.class)
                .execute();
    }

    private static void executeBasicTask() {
        IGenericClient client = createFHIRClient();

        // Search for Patient resources
        Bundle response = executeFHIRSearchForPatientByFamilyName(client, BASICTASK_FAMILYNAME);

        // Process the results and produce the output
        if (response.getEntry() != null && !response.getEntry().isEmpty()) {
            List<String> namesList = new ArrayList<>();
            Iterator<Bundle.BundleEntryComponent> iter = response.getEntry().listIterator();
            while (iter.hasNext()) {
                Bundle.BundleEntryComponent entry = iter.next();
                Resource resource = entry.getResource();
                if (resource instanceof Patient) {
                    Patient patient = ((Patient)resource);
                    String birthDateString = "";
                    if (patient.getBirthDateElement() != null &&
                            patient.getBirthDateElement().asStringValue() != null) {
                        birthDateString = patient.getBirthDateElement().asStringValue();
                    }
                    List<HumanName> humanNames = patient.getName();
                    // Write out all the Names for this Person
                    for (HumanName humanName : humanNames) {
                        String firstName = "";
                        if (humanName.getGiven() != null && !humanName.getGiven().isEmpty()) {
                            // Use the first name found since any additional ones are middle names
                            firstName = humanName.getGiven().listIterator().next().toString();
                        }
                        namesList.add(firstName + " " + humanName.getFamily() + " " + birthDateString);
                    }
                }
            }
            namesList.stream().sorted().forEach(n -> System.out.println(n));
        }
    }
}
