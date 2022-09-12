package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Survey;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.util.RemoteReader;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.messaging.core.domain.Message;
import pt.ist.fenixframework.FenixFramework;

public class SetupULisboaSurveyes extends ReadCustomTask implements RemoteReader {

    final JsonObject surveyCycle1 = object("cycle1.json");
    final JsonObject surveyCycle2 = object("cycle2.json");

    @Override
    public void runTask() throws Exception {

        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(this::needToApplySurvey)
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .filter(this::init)
                .flatMap(admissionProcessTarget -> admissionProcessTarget.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() != null)
                .filter(application -> Utils.registrationFor(application) != null)
                .forEach(application -> {
                    final AdmissionProcessTarget admissionProcessTarget = application.getAdmissionProcessTarget();
                    final JsonObject config = admissionProcessTarget.getOutcomeConfigJson();
                    final CycleType cycleType = cycleTypeFor(config);
                    final JsonObject survey = cycleType == CycleType.FIRST_CYCLE ? surveyCycle1
                            : cycleType == CycleType.SECOND_CYCLE ? surveyCycle2
                            : null;
                    if (survey != null) {
                        taskLog("app = %s%n", application.getAccount().getEmail());
                        add(application, survey);
                    }
                });
        ;
    }

    private void add(final Application application, final JsonObject survey) {
        try {
            FenixFramework.atomic(() -> {
                if (Survey.survey(application, survey.get("surveyId").getAsString()) == null) {
                    Survey.addSurvey(application, survey);
                    Message.fromSystem().singleTos(application.getAccount().getIdentity().getAccountSet().stream()
                            .map(account -> account.getEmail())
                            .filter(e -> !e.startsWith("deges")))
                            .subject("Inquérito ULisboa")
                            .textBody("Para completar a sua matrícula deverá aceder a https://fenix.tecnico.ulisboa.pt/fenixedu-connect/ " +
                                    "e responder ao inquérito da Universidade de Lisboa")
                            .send();
                }
            });
        } catch (final Throwable t) {
            taskLog("   Failled to add survey to %s%n", application.getExternalId());
        }
    }

    private boolean init(final AdmissionProcessTarget admissionProcessTarget) {
        final JsonObject config = admissionProcessTarget.getOutcomeConfigJson();
        final CycleType cycleType = cycleTypeFor(config);
        final JsonObject survey = cycleType == CycleType.FIRST_CYCLE ? surveyCycle1
                : cycleType == CycleType.SECOND_CYCLE ? surveyCycle2
                : null;
        if (survey == null) {
            taskLog("Unable to init: %s > %s%n",
                    admissionProcessTarget.getAdmissionProcess().getTitle().getContent(),
                    admissionProcessTarget.getName().getContent());
            return false;
        } else {
            config.add("surveyConcludeBoarding", survey);
            admissionProcessTarget.setOutcomeConfig(config.toString());
            return true;
        }
    }

    private boolean needToApplySurvey(final AdmissionProcess admissionProcess) {
        return admissionProcess.getTitle().getContent().indexOf("2023") > 0 && (
                Utils.isDegreeType(admissionProcess)
                || Utils.isDegreeSpecificRegimentType(admissionProcess)
                || Utils.isReinstatement(admissionProcess)
                || Utils.isDges(admissionProcess)
                );
    }

    private CycleType cycleTypeFor(final JsonObject config) {
        final String cycleType = JsonUtils.get(config, "cycleType");
        return cycleType == null ? null : CycleType.valueOf(cycleType);
    }

    @Override
    public String baseUrl() {
        return "https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/admissions/surveys/";
    }

}