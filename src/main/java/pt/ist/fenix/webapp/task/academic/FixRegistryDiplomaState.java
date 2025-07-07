package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.serviceRequests.documentRequests.RegistryDiplomaRequest;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class FixRegistryDiplomaState extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final String filename = "/afs/ist.utl.pt/ciist/fenix/fenix015/fix_registry_diploma_state.txt";
//        final String filename = "/home/rcro/Documents/fenix/academicos/fix_registry_diploma_state.txt";
        List<String> lines = Files.readAllLines(new File(filename).toPath());
        final DateTime dateTime = new DateTime(2025, 02, 25, 20, 35, 00);
        lines.stream()
                .map(requestId -> (RegistryDiplomaRequest) FenixFramework.getDomainObject(requestId))
                .flatMap(request -> request.getAcademicServiceRequestSituationsSet().stream())
                .filter(situation -> situation.getFinalSituationDate().isAfter(dateTime))
                .forEach(situation -> {
                    taskLog("Deleting situation: %s\tfor %s\t%s\t%s%n",
                            situation.getAcademicServiceRequestSituationType(),
                            situation.getAcademicServiceRequest().getPerson().getUsername(),
                            situation.getAcademicServiceRequest().getDescription()
                            , situation.getExternalId());
//                    situation.delete(false);
                });
    }
}