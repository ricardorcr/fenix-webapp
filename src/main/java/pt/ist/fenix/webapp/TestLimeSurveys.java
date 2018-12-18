package pt.ist.fenix.webapp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.io.IOException;

public class TestLimeSurveys extends CustomTask {


    /*
        list_participants(string $sSessionKey, integer $iSurveyID, integer $iStart,
            integer $iLimit = 10,
            boolean $bUnused = false, boolean|array $aAttributes = false, array $aConditions = array()) : array
    */

    @Override
    public void runTask() throws Exception {

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("https://surveys.tecnico.ulisboa.pt/index.php/admin/remotecontrol");
        post.setHeader("Content-type", "application/json");
        post.setEntity( new StringEntity("{\"method\": \"get_session_key\", \"params\": [\"ist24616\", \"22+EU+futuro1\"], \"id\": 1}"));
        try {
            HttpResponse response = client.execute(post);
            if(response.getStatusLine().getStatusCode() == 200){
                taskLog("Response: %s%n", response.getAllHeaders());
                taskLog("Será?: %s%n", response);
                HttpEntity entity = response.getEntity();
                taskLog("entity: %s%n", entity.getContent() != null ? "content" : "sem content");
                String sessionKey = parse(EntityUtils.toString(entity));
                post.setEntity( new StringEntity("{\"method\": \"list_participants\", " +
                        "\"params\": [ \""+sessionKey+"\", \"165313\", \"0\", \"5000\", \"false\", \"false\", \"false\" ]" +
                        ", \"id\": 1}"));
                response = client.execute(post);
                if(response.getStatusLine().getStatusCode() == 200){
                    entity = response.getEntity();
                    System.out.println(EntityUtils.toString(entity));
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String parse(String jsonLine) {
        taskLog(jsonLine);
        JsonElement jelement = new JsonParser().parse(jsonLine);
        JsonObject jobject = jelement.getAsJsonObject();
        String result = jobject.get("result").getAsString();
        return result;
    }
}