package pt.ist.fenix.webapp;

import java.util.Collections;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.events.SpecialSeasonEnrolmentEvent;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;

public class GenerateSpecialSeasonEnrolmentPaymentCodesAndEvents extends CustomTask {

    private static final Money AMOUNT_TO_PAY = new Money("20");
    private static final String EVENT_DESCRIPTION = "Inscrição em época especial à disciplina ";
    private static final YearMonthDay END_DATE = new YearMonthDay(2016, 07, 11);

    @Override
    public void runTask() throws Exception {
        final ExecutionYear year = ExecutionYear.readCurrentExecutionYear();
        taskLog("%s%n", year.getQualifiedName());
        year.getExecutionPeriodsSet().stream()

        .flatMap(es -> es.getEnrolmentsSet().stream()).flatMap(e -> e.getEvaluationsSet().stream())
                .filter(ee -> ee.getEvaluationSeason().isSpecial())
                .filter(ee -> !ee.getEnrolmentEvaluationState().equals(EnrolmentEvaluationState.ANNULED_OBJ))
                .filter(this::missingEvent).forEach(this::createEvent);
    }

    private boolean missingEvent(final EnrolmentEvaluation ee) {
        return ee.getSpecialSeasonEnrolmentEvent() == null;
    }

    private void createEvent(final EnrolmentEvaluation ee) {
        final Registration registration = ee.getRegistration();
        final Degree degree = registration.getDegree();
        final AdministrativeOffice office = degree.getAdministrativeOffice();
        final Person person = registration.getPerson();
        final SpecialSeasonEnrolmentEvent event = new SpecialSeasonEnrolmentEvent(office, person, Collections.singleton(ee));

        final YearMonthDay today = new YearMonthDay();
//        final AccountingEventPaymentCode paymentCode =
//                AccountingEventPaymentCode.create(PaymentCodeType.SPECIAL_SEASON_ENROLMENT, today, END_DATE, event,
//                        AMOUNT_TO_PAY, AMOUNT_TO_PAY, event.getPerson());

        final User user = registration.getPerson().getUser();
        final String eventDescription = eventDescription(ee);

//        final SystemSender sender = Bennu.getInstance().getSystemSender();
//        final Recipient recipient = null; //new Recipient(UserGroup.of(user)); UserGroup not public anymore
//
//        taskLog("Generated payment codes for: %s -> %s%n", user.getUsername(), eventDescription);
//
//        final String subject = "Pagamento Inscrição Época Especial - " + eventDescription;
//        final String body =
//                "A inscrição em exames de época especial ou extraordinário está sujeita ao "
//                        + "pagamento de um emolumento de 20 Euros por unidade curricular. " + "\n\n"
//                        + "A sua inscrição só é finalizada após o pagamento do referido emolumento "
//                        + "usando a referência multibanco junta. " + "\n\n"
//                        + "Há uma referência multibanco associada a cada unidade curricular a que " + "pretende inscrever-se. "
//                        + "\n\n" + "Este pagamento deverá ser efectuado no prazo de 48h, sob pena "
//                        + "de a inscrição deixar de ser válida. " + "\n\n"
//                        + "Pode efetuar o pagamento via multibanco com os seguintes dados a partir "
//                        + "das 20:00 do dia de hoje (ou das 20:00 do dia seguinte se esta mensagem "
//                        + "tiver sido enviada depois das 18h00): " + "\n\n" + "Entidade: " + paymentCode.getEntityCode() + "\n"
//                        + "\n" + "Referência: " + paymentCode.getCode() + "\n\n" + "\n" + "Valor: " + AMOUNT_TO_PAY.toString()
//                        + " €" + "\n\n";
//
//        new Message(sender, sender.getReplyTosSet(), recipient.asCollection(), subject, body, "");
    }

    private String eventDescription(final EnrolmentEvaluation ee) {
        return EVENT_DESCRIPTION + ee.getEnrolment().getCurricularCourse().getName(ee.getExecutionPeriod()) + " - "
                + ee.getExecutionPeriod().getQualifiedName();
    }

}