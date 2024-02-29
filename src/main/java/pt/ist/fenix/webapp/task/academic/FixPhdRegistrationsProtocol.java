package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixPhdRegistrationsProtocol extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getRegistrationProtocolsSet().stream()
                .filter(protocol -> protocol.getCode().equals("-"))
                .flatMap(protocol -> protocol.getRegistrationsSet().stream())
                .filter(registration -> registration.getDegree().isDEA())
                .peek(registration -> taskLog("%s\t%s\t%s\t%s%n", registration.getExternalId(), registration.getStartExecutionYear().getName(),
                        registration.getNumber(), registration.getDegreeName()))
                .forEach(registration -> registration.setRegistrationProtocol(RegistrationProtocol.getRegular()));
    }
}
