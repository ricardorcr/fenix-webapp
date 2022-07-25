package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class FixWronglyCreatedRegistrationDatasMobility extends CustomTask {
    @Override
    public void runTask() throws Exception {
        final RegistrationProtocol mobility = FenixFramework.getDomainObject("5295694675972");
        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        Bennu.getInstance().getRegistrationDataByExecutionYearSet().stream()
                .filter(rd -> rd.getRegistration().getRegistrationProtocol() == mobility)
                .filter(rd -> rd.getExecutionYear() == currentYear)
                .filter(rd -> rd.getRegistration().getStartExecutionYear() == currentYear.getNextExecutionYear())
                .filter(rd -> rd.getRegistration().getEnrolments(currentYear).isEmpty())
                .forEach(rd -> taskLog("%s\t%s%n", rd.getRegistration().getNumber(), rd.getRegistration().getDegreeName()));
    }
}