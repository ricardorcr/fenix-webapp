package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.ist.util.SheetUtils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.nio.file.Files;

public class ManualImportMinorPlacements extends ReadCustomTask implements SheetUtils {

    @Override
    public void runTask() throws Exception {
        final String filename = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/minor-results_852907490541640ARv2.xlsx";
        final byte[] content = Files.readAllBytes(new File(filename).toPath());

        final AdmissionProcess admissionProcess = FenixFramework.getDomainObject("852907490541640");
/*
        if ((!Utils.isHACS(admissionProcess) || Utils.isHACSWithFirstComeFirstServe(admissionProcess))
                || admissionProcess.getAdmissionProcessTargetSet().size() != 1
                || admissionProcess.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() != null)
                .anyMatch(application -> (application.getLockInstant() != null && application.getAccepted() == null)
                        || (application.getAccepted() != null && application.getAccepted() && application.getGrade() == null))) {
            return;
        }
*/
        xlsxRowStream(content, "Results")
                .skip(1)
                .forEach(row -> {
                    final String applicationID = getCellValue(row.getCell(1));
                    final String grade = getCellValue(row.getCell(7));
                    final String minorPlacement = getCellValue(row.getCell(9));

                    taskLog("%s : %s : %s%n", applicationID, grade, minorPlacement);
                });
    }

}