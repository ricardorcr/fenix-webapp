package pt.ist.fenix.webapp;

import org.fenixedu.PostalCodeValidator;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.io.File;
import java.io.FileInputStream;

public class UpdatePostalCodeValidatorFile extends CustomTask {

    @Override
    public void runTask() throws Exception {
        File file = new File("/home/rcro/workspace/PostCodeTools/src/main/resources/postal-codes.json");
        FileInputStream inputStream = new FileInputStream(file);
        PostalCodeValidator.reloadPostCodes(inputStream);
    }
}
