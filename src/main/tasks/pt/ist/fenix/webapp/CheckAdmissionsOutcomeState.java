package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.service.AuthorizePersonalDataAccessService;
import org.fenixedu.admissions.ist.service.RegistrationService;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Survey;
import org.fenixedu.admissions.ist.domain.UserAccountInfo;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Identity;
import pt.ist.fenixedu.integration.domain.SantanderCard;
import pt.ist.fenixframework.FenixFramework;

public class CheckAdmissionsOutcomeState extends CustomTask {

    @Override
    public void runTask() throws Exception {
//        final Application application = FenixFramework.getDomainObject("1978790217538900"); //571415333968005
//        final boolean mandatoryActivitiesDone = allMandatoryActivitiesDone(application);
//        taskLog("All activities done: %s%n", mandatoryActivitiesDone);
//        checkNeededChangeOutcomeState(application);

        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> !Utils.isDges(ap))
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .filter(target -> {
                    if (target.getOutcomeConfigJson() != null) {
                        final ExecutionYear executionYear = Utils.yearFor(target);
                        return executionYear != null && executionYear.getName().contains("2023");
                    } else {
                        return false;
                    }
                })
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> Utils.registrationFor(app) != null)
                .forEach(this::checkNeededChangeOutcomeState);
    }

    public boolean allMandatoryActivitiesDone(final Application application) {
        final Identity identity = application.getAccount().getIdentity();
        if (identity != null) {
            taskLog("identity");
            if (identity.getUser() != null) {
                taskLog("user");
                final Person person = identity.getUser().getPerson();
                if (person.getInstitutionalEmailAddress() != null || true /*ONLY LOCAL*/) {
                    taskLog("mail");
                    final UserAccountInfo userInfo = person.getUser().getUserAccountInfo();
                    if (true || (userInfo != null && userInfo.isPasswordSet())) {
                        taskLog("password");
                        if (AuthorizePersonalDataAccessService.hasCompletedAllDataAccessResponses(identity.getUser())) {
                            taskLog("cedencia dados");
                            if (!Survey.pendingResponse(application)) {
                                taskLog("survey");
                                final JsonObject processAfterOutcomeForm = application.getAdmissionProcessTarget().getAdmissionProcess().getAfterOutcomeFormDataJson();
                                if (processAfterOutcomeForm != null) {
                                    final JsonObject dataObject = application.getDataObject();
                                    if (dataObject.has("outcomeState")) {
                                        taskLog("outcomeState");
                                        return !dataObject.get("outcomeState").getAsJsonObject().get("canEditPostOutcomeForm").getAsBoolean();
                                    }
                                } else {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void checkNeededChangeOutcomeState(final Application application) {
        final Enum outcomeState = Utils.outcomeStateFor(application);
        if (outcomeState == RegistrationProcessState.BOARDING) {
            if (Utils.allMandatoryActivitiesDone(application)) {
                final AdmissionProcess admissionProcess = application.getAdmissionProcessTarget().getAdmissionProcess();
                if (Utils.isToChangeOutcomeState(admissionProcess)) {
                    if (Utils.needsDocumentConfirmation(admissionProcess)) {
                        taskLog("Should change for REGISTERED - %s - %s%n", application.getExternalId(), application.getAdmissionProcessTarget().getAdmissionProcess().getTitle().getContent());
//                        RegistrationService.setOutcomeState(application, RegistrationProcessState.REGISTERED);
//                        RegistrationService.addToConfirmationQueueIfNeeded(application);
                    } else {
                        taskLog("Should change for CONFIRMED - %s - %s%n", application.getExternalId(), application.getAdmissionProcessTarget().getAdmissionProcess().getTitle().getContent());
//                        RegistrationService.setOutcomeState(application, RegistrationProcessState.CONFIRMED);
//                        Signal.emit(RegistrationService.REGISTRATION_CONFIRMED, new DomainObjectEvent<>(application));
                    }
                }
            }
        }
    }

}