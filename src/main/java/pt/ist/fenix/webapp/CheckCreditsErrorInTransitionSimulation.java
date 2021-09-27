package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.transitions.domain.DegreeCurricularTransitionPlan;
import org.fenixedu.academic.transitions.domain.StudentDegreeCurricularTransitionPlan;
import org.fenixedu.academic.transitions.service.TransitionService;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CheckCreditsErrorInTransitionSimulation extends CustomTask {

    private Map<Registration, Set<StudentDegreeCurricularTransitionPlan>> studentMap = null;

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        User user = Authenticate.getUser();
        Authenticate.mock(User.findByUsername("ist24439"), "Transition Script Check");
        final Spreadsheet spreadsheet = new Spreadsheet("Dismissal Errors");

        studentMap = new HashMap<>();
        DegreeCurricularPlan.readBolonhaDegreeCurricularPlans().stream()
                .flatMap(dcp -> dcp.getDestinationTransitionPlanSet().stream())
                .filter(transitionPlan -> !transitionPlan.getDestinationDegreeCurricularPlan().getName().equals("LMAC 2021"))
                .flatMap(transitionPlan -> transitionPlan.getStudentDegreeCurricularTransitionPlanSet().stream())
                .filter(studentPlan -> !isConcluded(studentPlan))
//                .filter(studentPlan -> studentPlan.getStudent().getPerson().getUsername().equals("ist426311"))
                .filter(studentPlan -> !exclude(studentPlan.getStudent().getPerson().getUsername()))
                .filter(studentPlan -> !hasDestination(studentPlan.getDegreeCurricularTransitionPlan()
                        .getDestinationDegreeCurricularPlan(), studentPlan.getStudent()))
                .forEach(studentPlan -> {
                    final Student student = studentPlan.getStudent();
                    try {
                        TransitionService.run(studentPlan.getDegreeCurricularTransitionPlan(), student.getPerson().getUser(), true, false, true, true);
                    } catch (Throwable e) {
                        if ("error.dismissal.invalid.curricular.course.to.dismissal".equals(e.getMessage())) {
                            report(studentPlan, spreadsheet);
                            FenixFramework.atomic(() -> {
                               studentPlan.delete();
                            });
                        }
                    }
                });

        Authenticate.mock(user, "Restore User Transition Script");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Alunos_erros_dismissal.xls",baos.toByteArray());
    }

    private void report(final StudentDegreeCurricularTransitionPlan studentPlan, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Aluno", studentPlan.getStudent().getPerson().getUsername());
        row.setCell("Curso Destino", studentPlan.getDegreeCurricularTransitionPlan().getDestinationDegreeCurricularPlan().getName());
        row.setCell("Congelado", Boolean.toString(studentPlan.getFreezeInstant() != null));
        row.setCell("Confirmado", Boolean.toString(studentPlan.getConfirmTransitionInstant() != null));
    }

    private boolean isConcluded(final StudentDegreeCurricularTransitionPlan studentPlan) {
        final DegreeCurricularTransitionPlan degreePlan = studentPlan.getDegreeCurricularTransitionPlan();
        final DegreeCurricularPlan destinationPlan = degreePlan.getDestinationDegreeCurricularPlan();
        final Set<CycleType> cycleTypes = destinationPlan.getRoot().getCycleCourseGroups().stream()
                .map(group -> group.getCycleType())
                .collect(Collectors.toSet());
        final Student student = studentPlan.getStudent();
        return (!destinationPlan.getDegreeType().isIntegratedMasterDegree()) && student.getRegistrationsSet().stream()
                .flatMap(registration -> registration.getStudentCurricularPlansSet().stream())
                .flatMap(scp -> scp.getCycleCurriculumGroups().stream())
                .filter(group -> cycleTypes.contains(group.getCycleType()))
                .filter(group -> group.getDegreeModule().getDegree() == degreePlan.getOriginDegreeCurricularPlan().getDegree())
                .anyMatch(group -> group.isConcluded());
    }

    private static boolean hasDestination(final DegreeCurricularPlan destinationPlan, final Student student) {
        return student.getRegistrationsSet().stream()
                .flatMap(registration -> registration.getStudentCurricularPlansSet().stream())
                .anyMatch(scp -> scp.getCycleCurriculumGroups().stream()
                        .anyMatch(ccg -> ccg.getDegreeCurricularPlanOfDegreeModule() == destinationPlan));
    }

    private static final String[] exclusions = new String[]{
            "ist190709",
            "ist189516",
            "ist186978",
            "ist423305",
            "ist1100693",
            "ist196252",
            "ist194150",
            "ist196295",
            "ist196147",
            "ist1100073",
            "ist199945",
            "ist196265",
            "ist196289",
            "ist1100059",
            "ist199969",
            "ist199881",
            "ist199987",
            "ist199911",
            "ist1100003",
            "ist1100000",
            "ist199980",
            "ist199937",
            "ist199942",
            "ist1100035",
            "ist196319",
            "ist179130",
            "ist189895",
            "ist163240",
            "ist169749"
    };

    private static boolean exclude(final String username) {
        for (final String s : exclusions) {
            if (s.equals(username)) {
                return true;
            }
        }
        return false;
    }

}
