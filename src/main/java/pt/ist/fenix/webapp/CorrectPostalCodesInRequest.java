package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class CorrectPostalCodesInRequest extends CustomTask {

//    final static String POSTAL_CODE = "062-3535"; //JP
    final static String POSTAL_CODE = "506 84"; //SK

    @Override
    public void runTask() throws Exception {
//        Stream.of("1130916428645340", "847439997175431", "844450699936801", "565965020464023", "565965020464003",
//                "563108867211424", "562975723231032", "562975723230519", "284490043781827", "284490043781826", "284490043771499",
//                "281633890503559", "281633890502968", "281633890502824", "281500746543785").forEach(this::fix);;
        fix((SapRequest) FenixFramework.getDomainObject("1415608335877807"));
    }

    private void fix(String eventID) {
        Event event = FenixFramework.getDomainObject(eventID);
        event.getSapRequestSet().stream().forEach(this::fix);
    }

    private void fix(SapRequest sapRequest) {
        if (!sapRequest.getIntegrated()) {
            JsonObject json = new JsonParser().parse(sapRequest.getRequest()).getAsJsonObject();
            JsonObject clientData = json.get("clientData").getAsJsonObject();

            String oldPostalCode = clientData.get("postalCode").getAsString();
//            String countryCode = clientData.get("fiscalCountry").getAsString();
//            if (!PostalCodeValidator.isValidAreaCode(countryCode, oldPostalCode)) {
//                String examplePostCodeFor = PostalCodeValidator.examplePostCodeFor(countryCode);
//                taskLog("OID: %s - Country: %S - Before: %s - After: %s\n", sapRequest.getExternalId(), countryCode,
//                        oldPostalCode, examplePostCodeFor);
            taskLog("Old postal code: %s\n", oldPostalCode);
            clientData.addProperty("postalCode", POSTAL_CODE);
            sapRequest.setRequest(json.toString());
//            }
        }
    }
}
