import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SampleClient {

    public static void main(String[] theArgs) {

        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));

        // Search for Patient resources
        Bundle response = client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value("SMITH"))
                .returnBundle(Bundle.class)
                .execute();

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
