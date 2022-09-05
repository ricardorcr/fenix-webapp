package pt.ist.fenix.webapp;

import com.google.gson.JsonElement;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.ist.service.RegistrationService;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class ChangeOutcomeState extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2022/2023");
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> Utils.isToChangeOutcomeState(ap))
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .filter(target -> {
                    final JsonElement year = target.getOutcomeConfigJson().get("year");
                    if (year != null) {
                        ExecutionYear targetYear = FenixFramework.getDomainObject(year.getAsString());
                        return targetYear.isBefore(executionYear);
                    }
                    return false;
                })
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> {
                    final Registration registration = Utils.registrationFor(app);
                    return registration != null && registration.getLastState().isActive() && registration.hasAnyEnrolments();
                })
                .forEach(app -> changeState(app, RegistrationProcessState.CONFIRMED));
    }

    private void changeState(final Application app, final RegistrationProcessState state) {
        FenixFramework.atomic(() -> {
            RegistrationService.setOutcomeState(app, state);
        });
    }
}