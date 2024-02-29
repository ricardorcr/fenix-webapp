package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.CreditsLimit;
import org.fenixedu.academic.domain.curricularRules.CurricularRule;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Optional;

public class CheckGroupsCreditLimit extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Alunos");
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        executionYear.getRegistrationDataByExecutionYearSet().stream()
                .filter(rdey -> rdey.getEnrolmentDate() != null)
                .map(rdey -> rdey.getRegistration().getLastStudentCurricularPlan())
                .forEach(scp -> {
                    scp.getAllCurriculumGroups().stream()
                            .filter(group -> group.getDegreeModule() != null)
//                            .filter(group -> !group.isConcluded())
                            .filter(group -> group.getEnrolmentsSet().stream()
                                    .filter(e -> e.getExecutionPeriod().getExecutionYear() == executionYear)
                                    .anyMatch(Enrolment::isEnroled))
                            .forEach(group -> {
                                final Optional<CreditsLimit> limitRule = group.getDegreeModule().getCurricularRulesSet().stream()
                                        .filter(rule -> rule instanceof CreditsLimit)
                                        .filter(CurricularRule::isActive)
                                        .map(CreditsLimit.class::cast)
                                        .findAny();
                                if (limitRule.isPresent()) {
                                    final Double maximumCredits = limitRule.get().getMaximumCredits();
                                    final Double ectsCredits = getEctsCredits(group);
                                    if (ectsCredits > maximumCredits) {
                                        final Spreadsheet.Row row = spreadsheet.addRow();
                                        row.setCell("Aluno", group.getStudent().getPerson().getUsername());
                                        row.setCell("Curso", scp.getName());
                                        row.setCell("Grupo", group.getName().getContent());
                                        row.setCell("Máximo", maximumCredits);
                                        row.setCell("Inscrito", ectsCredits);
                                    }
                                }
                            });

                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("alunos_grupos_excesso.xlsx", baos.toByteArray());
    }

    final public Double getEctsCredits(final CurriculumGroup group) {
        BigDecimal bigDecimal = BigDecimal.ZERO;
        for (final CurriculumModule curriculumModule : group.getCurriculumModulesSet()) {
            final Double aprovedEctsCredits = curriculumModule.getAprovedEctsCredits();
            bigDecimal = bigDecimal.add(new BigDecimal(aprovedEctsCredits));
            if (aprovedEctsCredits == 0 && curriculumModule.isEnrolment() && ((Enrolment)curriculumModule).isEnroled()) {
                bigDecimal = bigDecimal.add(BigDecimal.valueOf(curriculumModule.getEctsCredits()));
            }
        }
        return Double.valueOf(bigDecimal.doubleValue());
    }
}
