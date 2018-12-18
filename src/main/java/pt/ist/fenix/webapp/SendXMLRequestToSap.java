package pt.ist.fenix.webapp;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonObject;

import pt.ist.sap.client.SapFinantialClient;

public class SendXMLRequestToSap extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/NP482886.xml");
//        final File file = new File("/home/rcro/DocumentsHDD/fenix/sap/NP590985.xml");
        String fileContent = new String(Files.readAllBytes(file.toPath()));
        JsonObject data = new JsonObject();
        data.addProperty("finantialInstitution","IST");

        byte[] bytes = null;
        try {
            bytes = fileContent.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JsonObject result = SapFinantialClient.sendInfoOnline(bytes, data);
        taskLog(result.toString());
    }
}
