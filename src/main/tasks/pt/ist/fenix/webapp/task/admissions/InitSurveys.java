package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.collaboration.limesurvey.LimeSurveyClient;

import java.io.IOException;

public class InitSurveys extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final int id1 = 918456;
        final int id2 = 865334;
        try (final LimeSurveyClient client = new LimeSurveyClient()) {
            set(client, id1);
            set(client, id2);
        } catch (final IOException ex) {
            throw new Error(ex);
        }
    }

    private void set(final LimeSurveyClient client, final int surveyId) {
        final JsonObject props = setSurveyProperties(client, surveyId);
        client.activateTokens(surveyId);
    }

    public JsonObject setSurveyProperties(final LimeSurveyClient client, final int surveyId) {
        final JsonObject properties = new JsonObject();
//        properties.addProperty("owner_id", Integer.parseInt(ownerId));
        properties.addProperty("bounce_email", "no-reply@tecnico.ulisboa.pt");
        properties.addProperty("sendconfirmation", "N");
        properties.addProperty("assessments", "Y");
//        properties.addProperty("startdate", start);
//        properties.addProperty("expires", end);
        properties.addProperty("usecookie", "Y");
        properties.addProperty("datestamp", "Y");
        properties.addProperty("savetimings", "Y");
        properties.addProperty("tokenanswerspersistence", "Y");
        properties.addProperty("usetokens", "Y");
        properties.addProperty("tokenlength", 35);
        return client.setSurveyProperties(surveyId, properties);
    }

}