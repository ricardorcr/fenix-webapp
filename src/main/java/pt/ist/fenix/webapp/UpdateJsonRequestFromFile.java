package pt.ist.fenix.webapp;

import java.io.File;
import java.nio.file.Files;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class UpdateJsonRequestFromFile extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final File file = new File("/home/rcro/DocumentsSSD/fenix/sap/updateRequest.json");
        String fileContent = new String(Files.readAllBytes(file.toPath()));

        SapRequest sr = FenixFramework.getDomainObject("1978558289281102");
        sr.setRequest(fileContent);

        sr.setIntegrated(false);
    }
}
