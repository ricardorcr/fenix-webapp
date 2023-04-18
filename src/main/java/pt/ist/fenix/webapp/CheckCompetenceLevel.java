package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLevel;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class CheckCompetenceLevel extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Competences");
        ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(ep -> ep.getAssociatedExecutionCoursesSet().stream())
                .flatMap(ec -> ec.getCompetenceCourses().stream())
                .filter(cc -> cc.getCompetenceCourseLevel() == CompetenceCourseLevel.UNKNOWN)
                .forEach(cc -> report(cc, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("nivel_competencias.xlsx", baos.toByteArray());
    }

    private void report(final CompetenceCourse cc, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Disciplina", cc.getName());
        row.setCell("Nível", cc.getCompetenceCourseLevel().name());
        row.setCell("Departamento", cc.getDepartmentUnit().getName());
    }
}
