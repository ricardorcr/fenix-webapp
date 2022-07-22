package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class DeleteGradeOfExcludedStudents extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess process = FenixFramework.getDomainObject("852907490541645");
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() != null)
                .filter(application -> application.getAccepted() != null && !application.getAccepted().booleanValue())
                .forEach(application -> {
                    final JsonObject data = application.getDataObject();
                    taskLog("%s = %s : %s%n",
                            application.getExternalId(),
                            application.getGrade(),
                            data.get("gradeData"));
                    application.setGrade(null);
                    data.remove("gradeData");
                    application.setData(data.toString());
                });
    }

}