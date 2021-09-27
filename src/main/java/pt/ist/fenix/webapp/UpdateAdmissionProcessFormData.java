package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class UpdateAdmissionProcessFormData extends CustomTask {

    private static final String DIR = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/";
    //private static final String DIR = "/home/rcro/workspace/data/admissions/";
    private static final String FORM_DATA_FILENAME = DIR + "degreeChangeFormData.json";

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> ap.getTitle().toString().contains("Mudança de Par"))
                .forEach(this::setFormData);
    }

    private void setFormData(AdmissionProcess process) {
        JsonObject formData;
        try {
            formData = JsonParser.parseString(
                    new String(Files.readAllBytes(new File(FORM_DATA_FILENAME).toPath()))
            ).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error(e);
        }
        process.setFormData(formData.toString());
    }
}