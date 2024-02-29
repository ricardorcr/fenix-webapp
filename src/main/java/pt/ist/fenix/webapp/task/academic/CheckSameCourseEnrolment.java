package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckSameCourseEnrolment extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Alunos");
        Bennu.getInstance().getRegistrationsSet()
                .forEach(registration -> {
                    Map<CurricularCourse, List<Enrolment>> map = new HashMap<>();
                    registration.getStudentCurricularPlansSet().stream()
                            .flatMap(scp -> scp.getEnrolmentsSet().stream())
                            .forEach(enrolment -> {
                                map.computeIfAbsent(enrolment.getCurricularCourse(), v -> new ArrayList<>()).add(enrolment);
                            });

                    map.forEach((k,v) -> {
                        if (v.size() > 1) {
                            final long approved = v.stream().filter(Enrolment::isApproved).count();
                            final long openEnrollments = v.stream().filter(e -> e.isEnroled() || e.isTemporarilyEnroled()).count();
                            final long duplicated = v.stream().filter(enrolment -> enrolment.getExecutionYear().isCurrent()).count();
                            if (approved > 1 || duplicated > 1 || openEnrollments > 1) {
                                final Spreadsheet.Row row = spreadsheet.addRow();
                                row.setCell("Aluno", registration.getPerson().getUsername());
                                row.setCell("Curso", registration.getDegree().getSigla());
                                row.setCell("Plano", v.iterator().next().getStudentCurricularPlan().getName());
                                row.setCell("Disciplina", k.getName());
                                row.setCell("Múltiplas Aprovações", String.valueOf(approved));
                                row.setCell("Dupla inscrição", String.valueOf(duplicated));
                                row.setCell("Inscrições abertas", String.valueOf(openEnrollments));
                            }
                        }
                    });
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("inscricoes_duplas.xlsx", baos.toByteArray());
    }
}
