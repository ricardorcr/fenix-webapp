package pt.ist.fenix.webapp.sap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;

import java.util.Arrays;
import java.util.List;

public class FixOldDebtCreditToResend extends SapCustomTask {

    final String COMPLETE_DATE = "2022-12-30 17:25:02";
    final String DATE = "2022-12-30";

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger eventLogger) {
        final List<String> debtCredutsNumbers = Arrays.asList(/*"NJ848173", */"NJ848165", "NJ848120", "NJ848221", "NJ850602", "NJ848174", "NJ1006707", "NJ903677");

        debtCredutsNumbers.stream()
                .forEach(docNumber -> process(docNumber, eventLogger, errorLogConsumer));
    }

    private void process(final String docNumber, EventLogger eventLogger, ErrorLogConsumer errorLogConsumer) {
        final SapRequest debtCredit = SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .filter(sr -> sr.getDocumentNumber().equals(docNumber))
                .findAny().get();

        final JsonObject requestAsJson = debtCredit.getRequestAsJson();
        final JsonObject workingDocument = requestAsJson.get("workingDocument").getAsJsonObject();
        workingDocument.addProperty("documentDate", COMPLETE_DATE);
        workingDocument.addProperty("entryDate", COMPLETE_DATE);

        String metadata = workingDocument.get("metadata").getAsString();
        String stripMetadata = metadata.replace("\\", "");
        JsonObject metadataJson = new JsonParser().parse(stripMetadata).getAsJsonObject();
        metadata = String.format("{\"ANO_LECTIVO\":\"%s\", \"START_DATE\":\"%s\", \"END_DATE\":\"%s\", \"Despacho\":\"%s\"}",
                metadataJson.get("ANO_LECTIVO").getAsString(), metadataJson.get("START_DATE").getAsString(),
                metadataJson.get("END_DATE").getAsString(), DATE);

        workingDocument.addProperty("metadata", metadata);
        debtCredit.setRequest(requestAsJson.toString());
        debtCredit.setSent(false);
        debtCredit.setIntegrated(false);

        final SapEvent sapEvent = new SapEvent(debtCredit.getEvent());
        sapEvent.processPendingRequests(debtCredit, errorLogConsumer, eventLogger);
    }
}
