package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.io.File;
import java.nio.file.Files;

public class CreateFormData extends CustomTask {

    private static final String FORM_DATA_FILENAME = "/home/rcro/DocumentsHDD/fenix/candidaturas/process_template_pages.json";

    @Override
    public void runTask() throws Exception {
        final JsonObject formData = new JsonParser().parse(new String(Files.readAllBytes(new File(FORM_DATA_FILENAME).toPath()))).getAsJsonObject();
        AdmissionsSystem.getInstance().getAdmissionProcessSet()
                .forEach(admissionProcess -> admissionProcess.setFormData(formData.toString()));
    }
}