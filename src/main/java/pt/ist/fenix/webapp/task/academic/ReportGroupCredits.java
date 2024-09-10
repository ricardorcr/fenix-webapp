package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportGroupCredits extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionYear actualYear = ExecutionYear.readCurrentExecutionYear();
        final ExecutionYear previousYear = actualYear.getPreviousExecutionYear();

        Arrays.asList("MEIC-A", "MEIC-T").forEach(sigla -> {
            final Degree degree = Degree.find(sigla);
            //fazer colunas com grupos do DCP
            final Spreadsheet spreadsheet = new Spreadsheet("Alunos");
            final DegreeCurricularPlan lastDCP = degree.getLastActiveDegreeCurricularPlan();
            final List<String> headers = new ArrayList<>();
            lastDCP.getRoot().getChildContextsSet().forEach(context -> {
                fillInHeaders(context, headers);
            });
            spreadsheet.setHeader(0, "Aluno");
            spreadsheet.setHeader(1, "Estado");
            spreadsheet.setHeader(2, "Acordo");
            for (int iter = 0; iter < headers.size(); iter++) {
                spreadsheet.setHeader(iter+3, headers.get(iter));
            }
            lastDCP.getRegistrations().stream()
                    .filter(r -> r.hasAnyEnrolmentsIn(actualYear) || r.hasAnyEnrolmentsIn(previousYear))
                    .forEach(r -> report(r, spreadsheet, headers));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                spreadsheet.exportToXLSSheet(baos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            output(degree.getSigla() + "_" + previousYear.getName().replace("/", "_") + "_"
                    + actualYear.getName().replace("/", "_") + ".xlsx", baos.toByteArray());
        });
    }

    private void fillInHeaders(final Context context, final List<String> headers) {
        final DegreeModule degreeModule = context.getChildDegreeModule();
        if (degreeModule.isCourseGroup()) {
            headers.add(degreeModule.getName());
            ((CourseGroup) degreeModule).getChildContextsSet().forEach(childContext -> fillInHeaders(childContext, headers));
        }
    }

    private void report(final Registration registration, final Spreadsheet spreadsheet, final List<String> headers) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
        row.setCell(0, registration.getPerson().getUsername());
        row.setCell(1, registration.getLastActiveState().getStateType().getDescription());
        row.setCell(2, registration.getRegistrationProtocol().getDescription().getContent());
        for (CurriculumGroup group : scp.getAllCurriculumGroups()) {
            int index = getIndex(headers, group);
            if (index != -1) {
                row.setCell(index+3, String.valueOf(group.getAprovedEctsCredits()));
            }
        }
    }

    private int getIndex(final List<String> headers, final CurriculumGroup group) {
        for (int iter = 0; iter < headers.size(); iter++) {
            if (headers.get(iter).equals(group.getName().getContent())) {
                return iter;
            }
        }
        return -1;
    }
}
