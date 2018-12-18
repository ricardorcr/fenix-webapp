package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;

public class CheckInvoiceYears extends CustomTask {

    @Override
    public void runTask() throws Exception {

        Bennu.getInstance().getSapRoot().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE)
                .filter(sr -> getDocumentDate(sr).contains("2018"))
                .filter(sr -> sr.getWhenCreated().getYear() == 2019 && sr.getWhenCreated().getMonthOfYear() >= 2)
                .filter(sr -> sr.getEvent().getWhenOccured().getYear() == 2018)
                .forEach(sr ->
                        taskLog("Esta factura devia ter sido enviada o ano passado:" +
                                        " ID: %s\tNº: %s\tDataEvento: %s\tDataCriaçãoRequest: %s\tDataDocSAP: %s %n",
                                sr.getEvent().getExternalId(), sr.getDocumentNumber(), sr.getEvent().getWhenOccured().toString("dd/MM/yyyy HH:mm"),
                                sr.getWhenCreated().toString("dd/MM/yyyy HH:mm"), getDocumentDate(sr)));
    }

    private String getDocumentDate(final SapRequest sr) {
        final JsonObject request = sr.getRequestAsJson();
        final JsonObject workingDocument = request.getAsJsonObject("workingDocument");
        return workingDocument.get("documentDate").getAsString();
    }
}
