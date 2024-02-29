package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.domain.accounting.events.gratuity.exemption.penalty.FixedAmountInterestExemption;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class FixTuitionCustomEvents extends ReadCustomTask {

    private final DateTime now = new DateTime();
    private final Map<Student, BigDecimal> studentMap = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void runTask() throws Exception {

        Bennu.getInstance().getAccountingEventsSet().stream()
                .parallel()
                .filter(CustomEvent.class::isInstance)
                .map(CustomEvent.class::cast)
                .forEach(this::fix);

        final Spreadsheet spreadsheet = new Spreadsheet("JurosEmFalta");
        studentMap.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .forEach(e -> {
                    spreadsheet.addRow()
                            .setCell("User", e.getKey().getPerson().getUsername())
                            .setCell("Value", e.getValue().toPlainString());
                });

        taskLog("Total debt = %s%n", studentMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString());
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("juros_em_falta.xlsx", stream.toByteArray());

//        fix(FenixFramework.getDomainObject("571557067892220"));
//        fix(FenixFramework.getDomainObject("571557067899219"));
    }

    private void fix(final CustomEvent event) {
        try {
            FenixFramework.atomic(() -> {
                if (EventTemplate.Type.TUITION.isType(event)) {
                    final JsonObject config = event.getConfigObject();
                    if (!event.isToApplyInterest() && event.isOpen()) {
                        final DebtInterestCalculator calculator1 = event.getDebtInterestCalculator(now);
                        final BigDecimal paiedDebt = calculator1.getPaidDebtAmount();

                        config.addProperty("applyInterest", true);
                        event.setConfig(config.toString());

                        final DebtInterestCalculator calculator2 = event.getDebtInterestCalculator(now);
                        final BigDecimal correctPayedDebt = calculator2.getPaidDebtAmount();

                        final BigDecimal diff = paiedDebt.subtract(correctPayedDebt);
                        if (diff.signum() == 0) {
                            // all is ok
                        } else {

                            taskLog("Values don't match for event %s : %s != %s%n",
                                    event.getExternalId(),
                                    paiedDebt.toPlainString(),
                                    correctPayedDebt.toPlainString());

                            studentMap.compute(event.getPerson().getStudent(), (s, v) -> v == null ? diff : v.add(diff));

                            calculator2.getAccountingEntries().stream()
                                    .forEach(accountingEntry -> {

//                                        taskLog("   %s = %s = %s = %s%n",
//                                                accountingEntry.getCreated().toString(),
//                                                accountingEntry.getTypeDescription().getContent(),
//                                                accountingEntry.getDescription(),
//                                                accountingEntry.getAmount().toPlainString());

                                        if (accountingEntry instanceof Payment payment) {
                                            final BigDecimal usedAmountInInterests = payment.getUsedAmountInInterests();
                                            if (usedAmountInInterests.signum() > 0) {
                                                taskLog("      > %s%n", usedAmountInInterests.toPlainString());
                                                final DateTime when = payment.getCreated().minusMinutes(1);
                                                if (LocalDate.now().getYear() != when.getYear() && !SapRoot.getInstance().yearIsOpen(when.getYear())) {
                                                    taskLog("      > Isenção com data anterior a 2023: %s%n", when.toString());
                                                }
                                                final Exemption exemption = new FixedAmountInterestExemption(event,
                                                        event.getPerson(),
                                                        new Money(usedAmountInInterests),
                                                        EventExemptionJustificationType.DIRECTIVE_COUNCIL_AUTHORIZATION,
                                                        when,
                                                        "Problema na configuração do plano de pagamentos");
                                                exemption.setWhenCreated(when);
                                            }
                                        }
                                    });
                        }
                    }
                }
            });
        } catch (final Throwable t) {
            // Keep going...
            throw t;
        }
    }
}