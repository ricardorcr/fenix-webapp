package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class AddOnboardingFlowTemplateConfig extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> isIsolated(admissionProcess))
                .forEach(admissionProcess -> {
                    final JsonObject config = admissionProcess.getOutcomeConfigJson();
                    config.addProperty("onBoardingFlow", "student-registration-flow");
                    admissionProcess.setOutcomeConfig(config.toString());

                    admissionProcess.getAdmissionProcessTargetSet().stream()
                            .flatMap(admissionProcessTarget -> admissionProcessTarget.getApplicationSet().stream())
                            .filter(application -> application.isBoarding())
                            .filter(application -> !hasFlow(application))
                            .forEach(application -> {
                                final User user = application.getAccount().getIdentity().getUser();
                                final Thread t = new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            Authenticate.mock(user, "Script");
                                            FenixFramework.atomic(() -> {
                                                taskLog("Running init for: %s%n", user.getUsername());
                                                Signal.emit(Application.BOARDING, new DomainObjectEvent<>(application));
                                            });
                                        } finally {
                                            Authenticate.unmock();
                                        }
                                    }
                                };
                                t.start();
                                try {
                                    t.join();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                });
    }

    private boolean hasFlow(final Application application) {
        final JsonObject applicationData = application.getDataObject();
        return JsonUtils.toDomainObject(applicationData, "flow") != null;
    }

    private boolean isIsolated(AdmissionProcess admissionProcess) {
        return admissionProcess.getTitle().anyMatch(s -> s.indexOf("2023/2024") > 0
                && s.indexOf("Unidades Curriculares Isoladas") > 0);
    }

}