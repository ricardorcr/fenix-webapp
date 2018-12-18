package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.datetime.joda.DateTimeFormatterFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.SapRequest;

public class AddMissingPropertyToSapRequest extends CustomTask {

    final DateTimeFormatter formatter = new DateTimeFormatterFactory("yyyy-MM-dd").createDateTimeFormatter();
    final LocalDate now = new LocalDate();

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingEventsSet().stream().flatMap(e -> e.getSapRequestSet().stream())
                .filter(sr -> sr.getRequest().length() > 2).forEach(this::fix);

//        fix(FenixFramework.getDomainObject("852636907890845"));
    }

    private void fix(SapRequest sr) {
        JsonObject json = new JsonParser().parse(sr.getRequest()).getAsJsonObject();
        JsonObject workingDocument = json.get("workingDocument").getAsJsonObject();
        if (workingDocument != null) {
            JsonElement entryDateJson = workingDocument.get("entryDate");
            if (entryDateJson != null) {
                return;
            }

            DateTime entryDate = null;
            if (sr.getEvent().getWhenOccured().getYear() < now.getYear()) {
                entryDate = new DateTime(now.getYear(), 01, 01, 00, 00);
            } else {
                entryDate = sr.getEvent().getWhenOccured();
            }

            taskLog("Vou alterar o request: %s\n", sr.getExternalId());
            workingDocument.addProperty("entryDate", entryDate.toString("yyyy-MM-dd HH:mm:ss"));
            sr.setRequest(json.toString());
        }
    }
}
