package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;

public class FixRegistrationStart extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        Arrays.asList("1972583989819836").stream()
            .map(oid -> (Registration) FenixFramework.getDomainObject(oid))
            .forEach(r -> {
                r.setStartDate(currentExecutionYear.getBeginDateYearMonthDay());
                r.getLastState().setStateDate(currentExecutionYear.getBeginDateYearMonthDay());
                r.getLastStudentCurricularPlan().setStartDate(currentExecutionYear.getBeginDateYearMonthDay());
            });
    }
}
