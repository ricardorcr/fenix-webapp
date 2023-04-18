package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class UpdateApplicationCanEditForm extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> Utils.hasAfterOutcomeForm(ap))
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> Utils.registrationFor(app) != null)
                .filter(app -> RegistrationProcessState.BOARDING == Utils.outcomeStateFor(app))
                .forEach(this::changeIfNeeded);
    }

    private void changeIfNeeded(final Application application) {
        FenixFramework.atomic(() -> {
            final JsonObject dataObject = application.getDataObject();
            final JsonObject outcomeState = dataObject.get("outcomeState").getAsJsonObject();
            if (outcomeState.get("canEditPostOutcomeForm").getAsBoolean()) {
                if (dataObject.has("outcomeFormData")) {
                    final JsonObject outcomeFormData = dataObject.get("outcomeFormData").getAsJsonObject();
                    if (outcomeFormData.has("afterOutcome")) {
                        taskLog("Changed canEdit for: %s %s%n", application.getExternalId(), application.getAdmissionProcessTarget().getAdmissionProcess().getTitle().getContent());
                        outcomeState.addProperty("canEditPostOutcomeForm", false);
                        application.setData(dataObject.toString());
                        Signal.emit("fenixedu.admissions.application.boarding.concluded", new DomainObjectEvent<>(application));
                    }
                }
            }
        });
    }
}
