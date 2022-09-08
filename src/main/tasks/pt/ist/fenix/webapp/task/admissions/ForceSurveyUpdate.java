package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Survey;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.collaboration.limesurvey.LimeSurveySDK;
import org.fenixedu.commons.stream.StreamUtils;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class ForceSurveyUpdate extends WriteCustomTask {
    @Override
    public void runTask() throws Exception {
        final Application application = FenixFramework.getDomainObject("571415333968005");
        Survey.surveys(application)
                .filter(Survey::pendingResponse)
                .filter(survey -> shouldMarkAsCompleted(survey))
                .forEach(survey -> markAsCompleted(application, Survey.id(survey)));

        Survey.updateResponseStatus(application);
    }

    private boolean shouldMarkAsCompleted(final JsonObject survey) {
        if (CoreConfiguration.getConfiguration().developmentMode()) {
            final DateTime now = new DateTime();
            final String min = Integer.toString(now.getMinuteOfHour());
            return min.endsWith("1") || min.endsWith("3") || min.endsWith("5") || min.endsWith("7") || min.endsWith("9");
        }
        taskLog("Should " + Survey.email(survey) + " " + Survey.id(survey));
        return hasCompletedSurvey(Survey.id(survey), Survey.email(survey));
    }

    public boolean hasCompletedSurvey(final String surveyId, final String email) {
        final JsonObject params = new JsonObject();
        params.addProperty("email", email);
        final JsonObject result = LimeSurveySDK.getParticipantProperties(surveyId, params);
        taskLog("Lime response: %s%n", result.toString());
        final String completed = JsonUtils.get(result, "completed");
        return completed != null && completed.length() > 15;
    }

    private void markAsCompleted(final Application application, final String surveyId) {
        taskLog("Makr");
        final JsonObject data = application.getDataObject();
        final JsonArray surveys = data.getAsJsonArray("surveys");
        StreamUtils.of(surveys).map(e -> e.getAsJsonObject())
                .filter(survey -> surveyId.equals(Survey.id(survey)))
                .forEach(survey -> survey.addProperty("completed", new DateTime().toString("yyyy-MM-dd HH:mm:ss")));
        application.setData(data.toString());
        //Signal.emit(SURVEY_COMPLETED, new DomainObjectEvent<>(application));
    }

}