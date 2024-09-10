package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FixProductCodeInAdvancements extends SapCustomTask {

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger eventLogger) {
        final DateTime newDate  = new DateTime(2024,06,01,0,0);
        final String dateString = newDate.toString("yyyy-MM-dd HH:mm:ss");
        final Spreadsheet spreadsheet = new Spreadsheet("Advancements");
        final List<String> requestsToFix = Arrays.asList("852658383014795","2260033266009089","289708429405919","1697083313309319","1697083313307973","1134133360208333",
                "571183405818276","1697083313316904","852658383015525","1134133360210115","3104458196128781","571183405818271");

        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> requestsToFix.contains(sr.getExternalId()))
//                .filter(sr -> sr.getDocumentNumber().equals("NP1238928"))
                .forEach(sr -> {
                    final JsonObject requestAsJson = sr.getRequestAsJson();
                    final String productCode = requestAsJson.get("productCode").getAsString();
//                    if (!productCode.equals("0056")) {
                        final Spreadsheet.Row row = spreadsheet.addRow();
                        row.setCell("Event", sr.getEvent().getExternalId());
                        row.setCell("Doc Number", sr.getDocumentNumber());
                        row.setCell("SAP Number", sr.getSapDocumentNumber());
                        row.setCell("SapRequest", sr.getExternalId());
                        row.setCell("Advancement", sr.getDocumentNumberForType("NA"));
                        row.setCell("Data Doc", sr.getDocumentDate().toString());

//                        requestAsJson.addProperty("productCode", "0056");
//                        final String productDescription = requestAsJson.get("productDescription").getAsString();
//                        final String[] split = productDescription.split(":");
//                        requestAsJson.addProperty("productDescription", "ADIANTAMENTO " + ":" + split[1]);
//                        requestAsJson.addProperty("fromDate", dateString);
//                        requestAsJson.addProperty("toDate", dateString);
//
//                        final JsonObject workingDocument = requestAsJson.get("workingDocument").getAsJsonObject();
//                        workingDocument.addProperty("documentDate", dateString);
//                        workingDocument.addProperty("entryDate", dateString);
//                        workingDocument.addProperty("dueDate", dateString);
//
//                        final JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
//                        paymentDocument.addProperty("paymentDate", dateString);
//
//                        sr.setRequest(requestAsJson.toString());
                        final SapEvent sapEvent = new SapEvent(sr.getEvent());
                        sapEvent.resendSapRequest(errorLogConsumer, eventLogger, sr);
//                    }
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            spreadsheet.exportToXLSXSheet(baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        output("advancements_fixed_prod.xlsx", baos.toByteArray());
    }
}
