package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.FenixFramework;

import java.util.stream.Stream;

public class DeleteDuplicateDebtCredit extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Stream.of("569199130837935", "850674107548199", "850674107548203", "850674107548206", "850674107548207", "850674107548246", "1132149084258834",
                "1132149084258836", "1132149084258839", "1132149084258933", "1413624060969225", "1976574014390586", "1976574014390590", "1976574014390690",
                "1976574014390721")
                .map(s -> (Event) FenixFramework.getDomainObject(s))
                .forEach(e -> {
                    SapRequest sapRequest = e.getSapRequestSet().stream().filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT).findFirst().get();
                    sapRequest.delete();
                });
    }
}
