package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.ist.wizard.RemoteReader;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class SetupGradeConfig extends WriteCustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess process = FenixFramework.getDomainObject("571432513830959");
        //process.setGradeConfig(string("teachersPromotionGradeConfig.json"));
        //process.setGradeConfig(null);
        process.setResultsPublished(true);
        process.setHasAdmissionGranted(true);
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .forEach(application -> {
                    application.setGrade(null);
                });
    }

}