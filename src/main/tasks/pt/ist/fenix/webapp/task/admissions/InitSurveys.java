package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.fenixedu.bennu.LimeSurveySDKConfiguration;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.collaboration.limesurvey.LimeSurveyClient;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

public class InitSurveys extends ReadCustomTask {

    private String basicRemoteCall(final String method, final Consumer<JsonArray> paramSetter) {
        final JsonObject body = new JsonObject();
        final JsonArray params = new JsonArray();
        body.addProperty("jsonrpc", "2.0");
        body.addProperty("method", method);
        body.add("params", params);
        paramSetter.accept(params);
        body.addProperty("id", UUID.randomUUID().toString());

        taskLog("Params: %s%n", body.toString());

        final String url = "https://surveys.tecnico.ulisboa.pt/index.php?r=admin/remotecontrol";
        final HttpResponse<String> response = Unirest.post(url).header("Content-Type", "application/json").body(body).asString();

        taskLog("code: %s%n", response.getStatus());
        taskLog("response: %s%n", response.getBody());
        return response.getBody();
    }

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