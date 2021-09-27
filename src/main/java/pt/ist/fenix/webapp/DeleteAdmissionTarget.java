package pt.ist.fenix.webapp;

import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class DeleteAdmissionTarget extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionProcessTarget target = FenixFramework.getDomainObject("1415977703089016");
        target.getApplicationSet().stream()
                .forEach(app -> {
                    if (app.getEvent() != null) {
                        app.getEvent().cancel(User.findByUsername("ist24616").getPerson(), "Candidatura a MISE não devia ter aberto.");
                        app.setEvent(null);
                    }
                });
        target.getJurySet().clear();
        target.getLogSet().forEach(log -> log.delete());
        target.delete();
    }
}
