package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class ChangeAcademicOfficeCoordinator extends CustomTask {

    @Override
    public void runTask() throws Exception {
        User oldCoordinator = User.findByUsername("ist23000");
        User newCoordinator = User.findByUsername("ist23978");
        Bennu.getInstance().getAdministrativeOfficesSet().stream()
                .filter(ad -> ad.getCoordinator() == oldCoordinator)
                .forEach(ad -> {
                    taskLog("Vou mudar o coordenador da área %s de: %s para %s%n",
                            ad.getName().getContent(), ad.getCoordinator().getUsername(), newCoordinator.getUsername());
                    ad.setCoordinator(newCoordinator);
                });
    }
}
