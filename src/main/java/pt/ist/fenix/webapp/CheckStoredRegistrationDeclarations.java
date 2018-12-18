package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.registration.process.domain.RegistrationDeclarationFileState;

public class CheckStoredRegistrationDeclarations extends CustomTask {

    @Override
    public void runTask() throws Exception {

        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        final DateTime yesterday = new DateTime().withHourOfDay(03).minusDays(2);

        Bennu.getInstance().getRegistrationsSet().stream()
                //.filter(r -> r.getStartExecutionYear() == currentYear)
                .filter(r -> r.getRegistrationDeclarationFileSet().stream()
                        .filter(rd -> rd.getCreationDate().isBefore(yesterday))
                        .anyMatch(rd -> rd.getState() != RegistrationDeclarationFileState.STORED))
                .forEach(r -> taskLog("O aluno %s tem documentos não stored\n", r.getNumber()));
    }
}