package pt.ist.fenix.webapp.task.academic;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.admissions.ist.util.SheetUtils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.io.File;
import java.nio.file.Files;
import java.util.stream.Stream;

public class FixNAGrades extends CustomTask implements SheetUtils {

    @Override
    public void runTask() throws Exception {
//        final String filename = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/fix_NA_grades.xlsx";
        final String filename = "/home/rcro/external/DocumentsHDD/fenix/academicos/fix_NA_grades.xlsx";
        final byte[] content = Files.readAllBytes(new File(filename).toPath());

        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        final Stream<Row> alunos = xlsxRowStream(content, "Alunos");
        alunos
                .skip(1L)
                .forEach(row -> {
                    final String username = row.getCell(0).getStringCellValue();
                    final String degreeSigla = row.getCell(1).getStringCellValue();
                    final String courseName = row.getCell(2).getStringCellValue();
                    final int semester = (int) row.getCell(3).getNumericCellValue();

                    final User user = User.findByUsername(username);
                    user.getPerson().getStudent().getRegistrationsSet().stream()
                            .filter(r -> r.getDegree().getSigla().equals(degreeSigla))
                            .flatMap(r -> r.getEnrolments(currentYear.getExecutionSemesterFor(semester)).stream())
                            .filter(enrolment -> enrolment.getCurricularCourse().getName().equals(courseName))
                            .forEach(enrolment -> enrolment.setEnrollmentState(EnrollmentState.ENROLLED));
                });
    }
}
