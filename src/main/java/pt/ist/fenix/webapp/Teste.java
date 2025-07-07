package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRoot;

public class Teste extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AcademicInterval.getAcademicIntervalFromString("773094117287:3277060046849");
    }
}
