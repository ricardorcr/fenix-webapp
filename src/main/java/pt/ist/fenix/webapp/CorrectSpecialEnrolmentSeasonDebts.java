package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.events.EnrolmentEvaluationEvent;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CorrectSpecialEnrolmentSeasonDebts extends CustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    final static String CANCELATION_JUSTIFICATION = "Dívida duplicada e/ou inscrição já não existe";
    final static String SPECIAL_STATUTE_JUSTIFICATION = "Estatuto especial isenta dívida época especial";

    Person responsible = null;
    StatuteType SPECIAL_NEEDS_STATUTE = null;
    ExecutionYear year20182019 = null;

    @Override
    public void runTask() throws Exception {
        responsible = User.findByUsername("ist24616").getPerson();
        SPECIAL_NEEDS_STATUTE = FenixFramework.getDomainObject("1978004238499841");

        year20182019 = ExecutionYear.readCurrentExecutionYear().getPreviousExecutionYear();
        Map<Student, List<Event>> debts = new HashMap<>();
        Map<Student, Integer> enrolments = new HashMap<>();

        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(EnrolmentEvaluationEvent.class::isInstance)
                .map(e -> (EnrolmentEvaluationEvent) e)
                .filter(e -> !e.isCancelled())
                .filter(e -> e.getExemptionsSet().isEmpty())
                .filter(e -> e.getEventType().equals(EventType.SPECIAL_SEASON_ENROLMENT))
                .filter(e -> e.getExecutionPeriodName().equalsIgnoreCase("2ºSemestre 2018/2019")
                        || e.getExecutionPeriodName().equalsIgnoreCase("1ºSemestre 2018/2019"))
                .forEach(e -> {
                    Student student = e.getPerson().getStudent();
                    debts.putIfAbsent(student, new ArrayList<>());
                    debts.get(student).add(e);
                });

        debts.keySet().stream().filter(s -> doesNotMatch(s, debts, enrolments, year20182019)).forEach(s -> fixDebts(s, debts, enrolments));
    }

    private boolean doesNotMatch(final Student student, final Map<Student, List<Event>> debts, final Map<Student, Integer> enrolments,
                                 final ExecutionYear year) {
        long numberSpecialSeasonEnrolments = student.getRegistrationsSet().stream()
                .map(r -> r.getEnrolments(year).stream().filter(e -> e.isSpecialSeason()).count())
                .reduce((long) 0, Long::sum);

        enrolments.put(student, (int) numberSpecialSeasonEnrolments);
        return debts.get(student).size() != numberSpecialSeasonEnrolments;
    }

    private void fixDebts(final Student student, final Map<Student, List<Event>> debts, final Map<Student, Integer> enrolments) {
        List<Event> payedEvents = debts.get(student).stream()
                .filter(e -> !e.getAccountingTransactionsSet().isEmpty() && e.isClosed()).collect(Collectors.toList());

        if (payedEvents.size() > 0) {
            if (payedEvents.size() >= enrolments.get(student)) {
                if (payedEvents.size() == enrolments.get(student)) {
                    taskLog("Cancelados os restantes eventos de época especial: %s\tinscrições: %s\teventos: %s%n",
                            student.getNumber(), enrolments.get(student), debts.get(student).size());
//                    debts.get(student).removeAll(payedEvents);
//                    cancel(debts.get(student), CANCELATION_JUSTIFICATION);
                    return;
                }
                /*taskLog("Cancelados os restantes eventos de época especial: %s\tinscrições: %s\teventos: %s%n",
                        student.getNumber(), enrolments.get(student), debts.get(student).size());
                debts.get(student).removeAll(payedEvents);
                cancel(debts.get(student), CANCELATION_JUSTIFICATION);*/
            } else {
                /*boolean isToExempt = isSpecialStatute(student);

                //já pagou alguma coisa mas como tem estatuto especial vai-se isentar o resto
                if (isToExempt) {
                    taskLog("Tem estatuto especial - Cancelados os restantes eventos de época especial: %s\tinscrições: %s\teventos: %s%n",
                            student.getNumber(), enrolments.get(student), debts.get(student).size());
                    debts.get(student).removeAll(payedEvents);
                    cancel(debts.get(student), SPECIAL_STATUTE_JUSTIFICATION);
                } else {
                    taskLog("Aluno %s tem %s inscrições e %s eventos mas só %s é que estão pagos, não fazer nada%n",
                            student.getNumber(), enrolments.get(student), debts.get(student).size(), payedEvents.size());
                }*/
            }
        }/* else {
            if (enrolments.get(student) == 0) {
                taskLog("Cancelados os restantes eventos de época especial: %s\tinscrições: %s\teventos: %s%n",
                        student.getNumber(), enrolments.get(student), debts.get(student).size());
                cancel(debts.get(student), CANCELATION_JUSTIFICATION);
            } else {
                //Validar se tem o estatuto de serviços de acção social ou necessidades educativas especiais, porque estes não pagam
                boolean isToExempt = isSpecialStatute(student);

                if (isToExempt) {
                    taskLog("Tem estatuto especial - Cancelados os restantes eventos de época especial: %s\tinscrições: %s\teventos: %s%n",
                            student.getNumber(), enrolments.get(student), debts.get(student).size());
                    cancel(debts.get(student), SPECIAL_STATUTE_JUSTIFICATION);
                } else {
                    taskLog("Nenhum dos eventos está pago, não fazer nada: %s\tinscrições: %s\teventos: %s%n",
                            student.getNumber(), enrolments.get(student), debts.get(student).size());
                }
            }
        }*/
    }

    private boolean isSpecialStatute(final Student student) {
        //Validar se tem o estatuto de serviços de acção social ou necessidades educativas especiais, porque estes não pagam
        return student.getAllStatutes().stream()
                .filter(st -> st.getStatuteType().equals(SPECIAL_NEEDS_STATUTE) || st.getStatuteType().isGrantOwnerStatute())
                .filter(st -> st.getStudentStatute().isValidInExecutionPeriod(year20182019.getLastExecutionPeriod()))
                .findAny().isPresent();
    }

    private void cancel(final List<Event> eventsToCancel, final String justification) {
        eventsToCancel.stream().forEach(e -> {
            if (e.isClosed() || !e.getExemptionsSet().isEmpty() || !e.getAccountingTransactionsSet().isEmpty()) {
                taskLog("Este evento tem coisas acontecer: %s%n", e.getExternalId());
            } else {
                taskLog("Canceling event: %s%n", e.getExternalId());
//                e.cancel(responsible, justification);
            }
        });
    }
}
