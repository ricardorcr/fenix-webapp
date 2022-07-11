package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class CorrectSapRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRequest sapRequest = FenixFramework.getDomainObject("852658382927267");
        final String date = new DateTime().toString("yyyy-MM-dd HH:mm:ss");
        changeDate(sapRequest, date);
    }

    private void changeDate(final SapRequest sr, final String date) {
        final JsonObject request = sr.getRequestAsJson();
        final JsonObject workingDocument = request.getAsJsonObject("workingDocument");
        workingDocument.addProperty("documentDate", date);
        workingDocument.addProperty("dueDate", date);
        workingDocument.addProperty("entryDate", date);
        final JsonObject paymentDocument = request.getAsJsonObject("paymentDocument");
        paymentDocument.addProperty("paymentDate", date);
        sr.setRequest(request.toString());
    }
}