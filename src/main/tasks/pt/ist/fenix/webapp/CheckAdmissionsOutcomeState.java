package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Survey;
import org.fenixedu.admissions.ist.domain.UserAccountInfo;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Identity;
import pt.ist.fenixedu.integration.domain.SantanderCard;
import pt.ist.fenixframework.FenixFramework;

public class CheckAdmissionsOutcomeState extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Application application = FenixFramework.getDomainObject("852890310675974"); //571415333968005
        final boolean mandatoryActivitiesDone = allMandatoryActivitiesDone(application);
        taskLog("All activities done: %s%n", mandatoryActivitiesDone);

        checkNeededChangeOutcomeState(application);
    }

    public boolean allMandatoryActivitiesDone(final Application application) {
        final Identity identity = application.getAccount().getIdentity();
        if (identity != null) {
            taskLog("identity");
            if (identity.getUser() != null) {
                taskLog("user");
                final Person person = identity.getUser().getPerson();
                if (person.getInstitutionalEmailAddress() != null) {
                    taskLog("mail");
                    final UserAccountInfo userInfo = person.getUser().getUserAccountInfo();
                    if (userInfo != null && userInfo.isPasswordSet()) {
                        taskLog("password");
                        final SantanderCard santanderCard = identity.getUser().getSantanderCard();
                        if (santanderCard != null && person.getStudent() != null && person.getStudent().getPersonalDataAuthorization() != null) {
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
                        taskLog("Should change for REGISTERED");
                    } else {
                        taskLog("Should change for CONFIRMED");
                    }
                }
            }
        }
    }

}