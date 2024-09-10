package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

public class UpdateSurveysCode extends CustomTask {

    @Override
    public void runTask() throws Exception {
        DateTime now = new DateTime();
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> !ap.getArchived())
                .filter(ap -> ap.getEndOutcomePeriod() == null || ap.getEndOutcomePeriod().isAfter(now))
                .filter(ap -> !ap.getResultsPublished())
                .filter(ap -> ap.getTitle().getContent().contains("2024/2025"))
                .filter(ap -> ap.getOutcomeConfigJson().get("type").getAsJsonObject().get("name").getAsString().contains("degree"))
                .filter(ap -> ap.getAdmissionProcessTargetSet().stream().flatMap(target -> target.getApplicationSet().stream())
                        .noneMatch(app -> app.getDataObject().has("registration")))
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .forEach(target -> {
                    final JsonObject outcomeConfigJson = target.getOutcomeConfigJson();
                    if (outcomeConfigJson.has("surveyConcludeBoarding")) {
                        final JsonObject surveyConcludeBoarding = outcomeConfigJson.get("surveyConcludeBoarding").getAsJsonObject();
                        if (surveyConcludeBoarding.has("surveyId")) {
                            if (outcomeConfigJson.get("cycleType").getAsString().equals("SECOND_CYCLE")) {
                                taskLog("Changed surveyId from: %s\tto%s on target:%s%n", surveyConcludeBoarding.get("surveyId").getAsString(), "988217", target.getExternalId());
                                surveyConcludeBoarding.addProperty("surveyId", "988217");
                            } else if (outcomeConfigJson.get("cycleType").getAsString().equals("FIRST_CYCLE")) {
                                taskLog("Changed surveyId from: %s\tto%s on target:%s%n", surveyConcludeBoarding.get("surveyId").getAsString(), "317474", target.getExternalId());
                                surveyConcludeBoarding.addProperty("surveyId", "317474");
                            }
                            target.setOutcomeConfig(outcomeConfigJson.toString());
                        }
                    }
                });
    }
}
