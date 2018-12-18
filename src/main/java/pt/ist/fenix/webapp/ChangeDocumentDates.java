package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class ChangeDocumentDates extends CustomTask {

    private final DateTime endYear = new DateTime(2020,12,31,13,50);
    private final String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    public void runTask() throws Exception {
        List<String> eventIDs = null;
//        try {
//            eventIDs = Files.readAllLines(
//                    new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/events_to_sync.txt").toPath());
<<<<<<< HEAD
            eventIDs = Arrays.asList("1855427088123");
=======
            eventIDs = Arrays.asList("1125925676656740");
>>>>>>> 65a03bc... Several scripts
//        } catch (IOException e) {
//            throw new Error("Erro a ler o ficheiro.");
//        }
        for (String eventId : eventIDs) {
            Event event = FenixFramework.getDomainObject(eventId);
            event.getSapRequestSet().stream()
//                    .filter(sr -> !sr.getIntegrated() && !sr.getIgnore())
//                    .filter(sr -> sr.getDocumentDate().getYear() == 2020)
                    .filter(sr -> sr.getRequestType() == SapRequestType.ADVANCEMENT)
//                    .filter(sr -> sr.getDocumentNumber().equals("ND635244"))
                    .forEach(this::changeDocumentDate);
        }
    }

    private void changeDocumentDate(SapRequest sapRequest) {
        final JsonObject request = sapRequest.getRequestAsJson();
//        request.addProperty("fromDate", endYear.toString(DT_FORMAT));
        request.addProperty("toDate", endYear.toString(DT_FORMAT));
        if (request.get("workingDocument") != null && !request.get("workingDocument").isJsonNull()) {
            final JsonObject workingDocument = request.get("workingDocument").getAsJsonObject();
//            workingDocument.addProperty("documentDate", workingDocument.get("entryDate").getAsString());
//            workingDocument.addProperty("dueDate", workingDocument.get("entryDate").getAsString());
            workingDocument.addProperty("entryDate", endYear.toString(DT_FORMAT));
        }
//        if (request.get("paymentDocument") != null && !request.get("paymentDocument").isJsonNull()) {
//            final JsonObject paymentDocument = request.get("paymentDocument").getAsJsonObject();
//            paymentDocument.addProperty("paymentDate", endYear.toString(DT_FORMAT));
//        }
        sapRequest.setRequest(request.toString());
    }
}
