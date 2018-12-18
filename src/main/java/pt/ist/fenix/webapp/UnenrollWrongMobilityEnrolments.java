package pt.ist.fenix.webapp;

import com.google.common.collect.Lists;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;

public class UnenrollWrongMobilityEnrolments extends CustomTask {

    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Disciplinas desinscritas");
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> Utils.isMobilityType(ap))
                .filter(ap -> !Utils.isMobilityDoubleDegreeType(ap))
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> app.getDataObject().has("registration"))
                .map(app -> (Registration) FenixFramework.getDomainObject(app.getDataObject().get("registration").getAsString()))
                .forEach(r -> fixEnrolments(r, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("desinscricoes_mobilidade_no_curriculo.xls", baos.toByteArray());
    }

    private void fixEnrolments(final Registration registration, final Spreadsheet spreadsheet) {
        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
        final ExecutionSemester currentExecutionSemester = ExecutionSemester.readActualExecutionSemester();
        scp.getCycleCurriculumGroups().stream()
                .flatMap(ccg -> ccg.getEnrolmentsBy(currentExecutionSemester).stream())
                .forEach(enrolment -> {
                    report(enrolment, spreadsheet);
                    scp.enrol(currentExecutionSemester, new HashSet<>(), Lists.newArrayList(enrolment), CurricularRuleLevel.ENROLMENT_NO_RULES);
                });
    }

    private void report(final Enrolment enrolment, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        final Person student = enrolment.getRegistration().getPerson();
        row.setCell("IstID", student.getUsername());
        row.setCell("Nome", student.getName());
        row.setCell("Curso Aluno", enrolment.getStudentCurricularPlan().getDegreeCurricularPlan().getName());
        row.setCell("Disciplina", enrolment.getName().getContent());
        row.setCell("Protocolo", enrolment.getRegistration().getRegistrationProtocol().getDescription().getContent());
        row.setCell("Caminho completo", enrolment.getFullPath());
    }
}
