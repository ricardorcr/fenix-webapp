package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.messaging.core.domain.Message;

public class SpamStudentsForOpenNextCycle extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(p -> p.getEnrolmentsSet().stream())
                .map(e -> e.getStudentCurricularPlan())
                .filter(this::consider)
                .forEach(scp -> {
                    final Person person = scp.getPerson();
                    final boolean female = person.isFemale();
                    taskLog("%s%n", person.getUsername());
                    final String message = "Car" + (female ? "a" : "o") + " " + person.getName() + 
                            "\n\nDe modo a poder efetuar a matrícula no segundo ciclo e prosseguir com as inscrições " +
                            "em 2022/2023 foi disponibilizao uma funcionalidade na primeira página do portal de estudante " +
                            "do Fénix e também na primeira página do portal do alumni para quem entretanto tenha ficado " +
                            "com a matrícula da licenciatura no estado concuído. Esta funcionalidade permite que seja " +
                            "efetuada a matrícula no segundo ciclo. Após a matrícula poderá prosseguir com a inscrição " +
                            "habitual em disciplinas já no curso de mestrado." +
                            "\n\nCaso já tenha procedido à abertura do segundo ciclo para se poder inscrever em 2022/2023 " +
                            "por favor ignore esta mensagem.\n\n" +
                            "\n\n" +
                            "Votos de um bom novo ano letivo." +
                            "\n\n" +
                            "Os melhore cumprimentos," +
                            "\nA Equipa FenixEdi";
                    taskLog("%s%n", message);
                    if (female) {
                        return;
                    }
/*
                    Message.fromSystem()
                            .to(Group.users(person.getUser()))
                            .subject("Matrícula 2022/2023 - Abertura Mestrado")
                            .textBody(message)
                            .send();

 */
                });
                ;
    }

    private boolean consider(final StudentCurricularPlan studentCurricularPlan) {
        final CycleCurriculumGroup first = studentCurricularPlan.getCycle(CycleType.FIRST_CYCLE);
        if (first != null && first.isConcluded()) {
            if (studentCurricularPlan.getCycle(CycleType.SECOND_CYCLE) == null) {
                return studentCurricularPlan.getRegistration().getStudent().getRegistrationsSet().stream()
                        .filter(r -> r != studentCurricularPlan.getRegistration())
                        .noneMatch(r -> r.getStartExecutionYear().isCurrent() || r.getStartExecutionYear().getPreviousExecutionYear().isCurrent());
            }
        }
        return false;
    }

}