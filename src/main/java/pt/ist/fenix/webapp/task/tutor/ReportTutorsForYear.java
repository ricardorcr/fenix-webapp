package pt.ist.fenix.webapp.task.tutor;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.domain.TutorshipIntention;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ReportTutorsForYear extends CustomTask {

    ExecutionYear currentYear = null;
    @Override
    public void runTask() throws Exception {
        currentYear = ExecutionYear.readCurrentExecutionYear();
        Spreadsheet spreadsheet = new Spreadsheet("Tutores");
        currentYear.getExecutionDegreesSet().stream()
                .flatMap(ed -> TutorshipIntention.getTutorshipIntentions(ed).stream())
                .flatMap(ti -> getTutorships(ti).stream())
                .forEach(tutorship -> report(spreadsheet, tutorship));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("tutores.xlsx", baos.toByteArray());
    }

    private void report(final Spreadsheet spreadsheet, final Tutorship tutorship) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Tutor User", tutorship.getPerson().getUsername());
        row.setCell("Tutor", tutorship.getPerson().getName());
        row.setCell("Curso", tutorship.getStudentCurricularPlan().getDegreeCurricularPlan().getName());
        row.setCell("Aluno", tutorship.getStudent().getPerson().getName());
        row.setCell("Aluno User", tutorship.getStudent().getPerson().getUsername());
    }

    public List<Tutorship> getTutorships(final TutorshipIntention ti) {
        List<Tutorship> result = new ArrayList<Tutorship>();
        for (Tutorship tutorship : ti.getTutorships()) {
            if (tutorship.getStudentCurricularPlan().getDegreeCurricularPlan().equals(ti.getDegreeCurricularPlan())) {
                if (ti.getAcademicInterval().equals(currentYear.getAcademicInterval()))
                    result.add(tutorship);
            }
        }
        return result;
    }
}
