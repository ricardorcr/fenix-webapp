package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.SapRequest;

public class CorrectSapRequestCountry extends CustomTask {

    @Override
    public void runTask() throws Exception {

        Bennu.getInstance().getAccountingEventsSet().stream().filter(e -> !e.getSapRequestSet().isEmpty())
                .flatMap(e -> e.getSapRequestSet().stream()).filter(sr -> sr.getRequest().length() > 2).forEach(this::fix);

    }

    private void fix(SapRequest sr) {
        JsonObject json = new JsonParser().parse(sr.getRequest()).getAsJsonObject();
        JsonObject clientData = json.get("clientData").getAsJsonObject();

        String vatNumber = clientData.get("vatNumber").getAsString();
        clientData.addProperty("country", vatNumber.substring(0, 2));
        
        sr.setRequest(json.toString());
    }
}
