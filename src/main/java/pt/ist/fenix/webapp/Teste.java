package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

public class Teste extends CustomTask {

    @Override
    public void runTask() throws Exception {

        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE)
                .filter(sr -> {
                    final JsonObject clientData = sr.getRequestAsJson().get("clientData").getAsJsonObject();
                    final String accountID = clientData.get("accountId").getAsString();
                    final String vat = clientData.get("vatNumber").getAsString();
                    final String companyName = clientData.get("companyName").getAsString();
                    return accountID.equals("STUDENT") && vat.equals("PT505002892");
                })
                .forEach(sr -> {
                    final JsonObject clientData = sr.getRequestAsJson().get("clientData").getAsJsonObject();
                    final String companyName = clientData.get("companyName").getAsString();
                    taskLog("%s\t%s\t%s%n", sr.getEvent().getExternalId(), sr.getExternalId(), companyName);
                });
    }
}