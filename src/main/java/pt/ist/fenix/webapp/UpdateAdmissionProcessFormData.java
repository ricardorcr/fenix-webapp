package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class UpdateAdmissionProcessFormData extends CustomTask {

//    private static final String DIR = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/";
    private static final String DIR = "/home/rcro/workspace/data/admissions/";
    private static final String FORM_DATA_FILENAME = DIR + "2ndCycleFormData.json";

    @Override
    public void runTask() throws Exception {
        AdmissionProcess admissionProcess = FenixFramework.getDomainObject("1978807397384194");
        setFormData(admissionProcess);
    }

    private void setFormData(AdmissionProcess process) {
        JsonObject formData;
        try {
            final String json = new String(Files.readAllBytes(new File(FORM_DATA_FILENAME).toPath()));
            formData = JsonParser.parseString(json.replaceAll("https://fenix.tecnico.ulisboa.pt", CoreConfiguration.getConfiguration().applicationUrl()))
                    .getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error(e);
        }
        process.setFormData(formData.toString());
    }
}