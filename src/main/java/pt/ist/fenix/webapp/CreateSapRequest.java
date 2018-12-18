package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class CreateSapRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRequest sapRequest = FenixFramework.getDomainObject("852658382520005");
        String request = sapRequest.getRequest();
        request = request.replace("NA343439", "NA301202");
        request = request.replace("NP343440", "NP301203");
        SapRequest newSapRequest = new SapRequest(sapRequest.getEvent(), sapRequest.getClientId(), sapRequest.getValue(),
                "NA301202", sapRequest.getRequestType(), sapRequest.getAdvancement(), new JsonParser().parse(request).getAsJsonObject());     
        newSapRequest.setCreditId(sapRequest.getCreditId());
    }
}
