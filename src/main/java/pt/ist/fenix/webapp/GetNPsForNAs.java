package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.util.Arrays;
import java.util.List;

public class GetNPsForNAs extends CustomTask {

    @Override
    public void runTask() throws Exception {

        final List<String> nas = Arrays.asList("NA1200521","NA1200512","NA1200488","NA1200475","NA1200458","NA1200450","NA1200435","NA1200424","NA1200414",
                "NA1200406","NA1200401","NA1200395","NA1200382","NA1200362","NA1200354","NA1200336","NA1200320","NA1200292","NA1200271","NA1200264","NA1200254",
                "NA1200246","NA1200232","NA1200171","NA1200163","NA1200140","NA1200116","NA1200086","NA1200080","NA1200073");
        
        for (final String naNumber : nas) {
            String npNumber = SapRoot.getInstance().getSapRequestSet().stream()
                    .filter(sr -> sr.getRequestType() == SapRequestType.ADVANCEMENT)
                    .filter(sr -> sr.getRequest().contains(naNumber))
                    .findAny().get().getDocumentNumber();
            taskLog("%s - %s%n", npNumber, naNumber);
        }
    }
}