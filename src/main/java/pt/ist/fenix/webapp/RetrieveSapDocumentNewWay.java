package pt.ist.fenix.webapp;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.SapDocumentFile;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.sap.client.SapFinantialClient;

public class RetrieveSapDocumentNewWay extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Map<String, String> sapDocNumberMap = new HashMap<>();
        sapDocNumberMap.put("NA290011","3230003998/2019");
        sapDocNumberMap.forEach((k, v) -> {
            final String sapDocumentNumber = v;
            final SapRequest sapRequest = getSapRequest(k);
            JsonObject result = SapFinantialClient.getDocument(sapDocumentNumber, "501507930");
            if (result.get("documentBase64") != null) {
                output("documento_sap.pdf", Base64.getDecoder().decode(result.get("documentBase64").getAsString()));
                new SapDocumentFile(sapRequest,sapDocumentNumber + ".pdf",
                        Base64.getDecoder().decode(result.get("documentBase64").getAsString()));
                sapRequest.setSapDocumentNumber(sapDocumentNumber);
                sapRequest.setIntegrated(true);
            }
            taskLog(result.get("status").getAsString() + " - " + result.get("errorDescription").getAsString());
        });
    }

    private SapRequest getSapRequest(String documentNumber) {
        final SapRequest sapRequest = SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equals(documentNumber))
                .findAny().get();
        return sapRequest;
    }
}