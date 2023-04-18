package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;

public class CorrectSapRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {
//        SapRequest sapRequest = FenixFramework.getDomainObject("1978558289539971");
//        String newRequest = sapRequest.getRequest().replace("2023-02-01", "2022-12-30");
////        newRequest = newRequest.replace("true", "false");
////        newRequest = newRequest.replace("xpto", "true");
////        newRequest = newRequest.replace("-100", "100");
//        sapRequest.setRequest(newRequest);

        final Event event = FenixFramework.getDomainObject("1695528534410086");
        event.getSapRequestSet().stream()
                .filter(sr -> sr.getWhenCreated().getYear() == 2023)
                .forEach(sr -> {
                    final String newRequest = sr.getRequest().replace("2023-02-09", "2022-12-30");
                    sr.setRequest(newRequest);
                });
//        SapRoot.getInstance().setOpenYear(2022);
//        sapRequest.setSent(true);
//        sapRequest.setIgnore(true);
//        sapRequest.setIntegrated(true);
//        sapRequest.getOriginalRequest().setIgnore(true);
//        sapRequest.setClientId("AO1691156257706760");

//        Arrays.asList("571557067917183", "571557067917186").stream()
//                .map(eventID -> (Event) FenixFramework.getDomainObject(eventID))
//                .flatMap(event -> event.getSapRequestSet().stream())
//                .forEach(sr -> {
//                    sr.setRequest(sr.getRequest().replace("075", "0075"));
//                    sr.setRequest(sr.getRequest().replace("00075", "0075"));
//                });
    }
}