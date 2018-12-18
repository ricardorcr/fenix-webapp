package pt.ist.fenix.webapp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class PreEnrolmentRESTServiceClient extends CustomTask {

    //ist426659
    //group 281882998603999 tronco comum
    //course 1690378868621324 portfolio pessoal

    //group 281882998604018 dissertacao
    //course 283003985068038 projecto de mestrado 

    //ist426125
    //group 281882998604001 engenharia software
    //course 283003985068040 arquictectura software

    //group 281882998604004 tecnologia dos sistemas informáticos
    //course 1529008512950 desenvolvimento de aplicações distribuidas

    @Override
    public void runTask() throws Exception {
        StringBuilder builder = new StringBuilder();
        try {

            // Step1: Let's 1st read file from fileSystem
            InputStream inputStream =
                    new FileInputStream("/home/rcro/Documents/fenix/inscricoes/preInscricoes/preEnrolments_test_deic.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line).append("\n");
            }

            // Step2: Now pass JSON File Data to REST Service
            try {
                //Authenticate.mock(User.findByUsername("ist130598"));
                JsonParser jParser = new JsonParser();
                JsonElement jsonElement = jParser.parse(builder.toString());
//                FenixAPIv1 api = new FenixAPIv1();
//                Response response = api.defaultEnrolments("2761663971585", "1ºSemestre 2016/2017", jsonElement.getAsJsonObject());
//                taskLog(response.getEntity().toString());
//
                //academic term 2%C2%BA%20Semestre%202015/2016
                URL url = new URL(
                        "http://localhost:8080/fenix/api/fenix/v1/degrees/2761663971585/preEnrolmentsCurricularGroups?academicTerm=2016/2017&"
                                + "access_token=MTk3NzM5ODY0ODEyNzUxNzphNTM2NjE4ZGYxNjk5YjA4NGIwYjc5ZWU4MzY2NzRkMzAyYjc1YTgxMDMyOTVkMGNjOGNhNWY3YmRkMjc1M2JkODMxYTdjMjc5YTVmYmJhYTgwYmY3MmI0ZWVkMDJkNTJiZjIzZmQ4NTViYTMzNDM0ODFlMDk2MzdjZWIwZjUwNw");

//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//
//                connection.setDoInput(true);
//                connection.setDoOutput(true);
//                connection.setRequestMethod("POST");
//                connection.setRequestProperty("Accept", "application/json");
//                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
//                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
//                writer.write(jsonElement.toString());
//                writer.close();

                URLConnection connection = url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("requestBody",
                        "{\"defaultEnrolments\":[{\"student\":\"ist175741\",\"preEnrolments\":[{\"group\":\"281882998604011\",\"course\":\"283003985068051\"},{\"group\":\"281882998604005\",\"course\":\"283003985068052\"},{\"group\":\"281882998604011\",\"course\":\"283003985068075\"},{\"group\":\"281882998604011\",\"course\":\"283003985068076\"}]}]}");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
//                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
//                out.write(
//                        "requestBody:{\"defaultEnrolments\":[{\"student\":\"ist175741\",\"preEnrolments\":[{\"group\":\"281882998604011\",\"course\":\"283003985068051\"},{\"group\":\"281882998604005\",\"course\":\"283003985068052\"},{\"group\":\"281882998604011\",\"course\":\"283003985068075\"},{\"group\":\"281882998604011\",\"course\":\"283003985068076\"}]}]}");
//                out.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String readLine = null;
                while ((readLine = in.readLine()) != null) {
                    taskLog(readLine);
                }

                taskLog("\nREST Service Invoked Successfully..");
//                in.close();
            } catch (Exception e) {
                taskLog("\nError while calling REST Service");
                taskLog(e.getMessage());
                e.printStackTrace();
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
