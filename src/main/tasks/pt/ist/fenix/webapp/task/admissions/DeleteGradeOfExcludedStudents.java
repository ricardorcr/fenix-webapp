package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.util.DynamicForm;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.Identity;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

public class DeleteGradeOfExcludedStudents extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess process = FenixFramework.getDomainObject("852907490541645");
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() != null)
                .filter(application -> application.getAccepted() != null && !application.getAccepted().booleanValue())
                .forEach(application -> {
                    taskLog("%s = %s : %s%n",
                            application.getExternalId(),
                            application.getGrade(),
                            application.getDataObject().get("gradeData"));
                });
    }

}