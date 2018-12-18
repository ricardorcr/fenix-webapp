package pt.ist.fenix.webapp;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Discount;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.AnnualEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.util.LabelFormatter;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.gson.JsonObject;

import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
public class CheckEventTask extends CustomTask {
    String getStudentNumber(Event event) {
        return event.getParty() instanceof Person ? event.getPerson().getUsername() : "-";
    }

    String getStudentName(Event event) {
        return event.getParty() instanceof Person ? event.getPerson().getName() : "Unit:" + event.getParty().getName();
    }

    String getSocialSecurityNumber(Event event) {
        return event.getParty() instanceof Person ? event.getPerson().getSocialSecurityNumber() : event.getParty().getSocialSecurityNumber();
    }

    String getGratuityExecutionYearName(Event event) {
        if (event instanceof AnnualEvent) {
            return ((AnnualEvent) event).getExecutionYear().getName();
        }
        return event.getWhenOccured().toString("yyyy");
    }

    String getDegreeName(Event event) {
        if (event instanceof GratuityEvent) {
            return ((GratuityEvent) event).getDegree().getNameI18N().getContent();
        }
        return "N/A";
    }

    String getDegreeTypeName(Event event) {
        if (event instanceof GratuityEvent) {
            return ((GratuityEvent) event).getDegree().getDegreeType().getName().getContent();
        }
        return "N/A";
    }

    String getWhenOccured(Event event) {
        return event.getWhenOccured().toString("dd/MM/yyyy");
    }
    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        DateTime when = new DateTime(2017, 12, 31, 23, 59, 59, 999);
        Event event = FenixFramework.getDomainObject("4213364125422");
        Spreadsheet spreadsheet = new Spreadsheet(getClass().getSimpleName());
            LabelFormatter description = event.getDescription();
            String studentName = getStudentName(event);
            String studentNumber = getStudentNumber(event);
            String gratuityExecutionYearName = getGratuityExecutionYearName(event);
            String degreeName = getDegreeName(event);
            String degreeTypeName = getDegreeTypeName(event);
            String whenOccured = getWhenOccured(event);
            //taskLog("%s %s %s%n", event.getExternalId(), event.getDescription(), studentNumber);

            Money originalAmountToPay = event.getOriginalAmountToPay();
            Money amountToPay = event.calculateAmountToPay(when);
            Money totalAmountToPay = event.getTotalAmountToPay(when);
            Money amountPayed = event.getPayedAmount(when);

            if (!event.isOpen()) {
                final JsonObject jsonEvent = new JsonObject();
                jsonEvent.addProperty("id", event.getExternalId());
                jsonEvent.addProperty("originalAmountToPay", originalAmountToPay.toPlainString());
//                closedEvents.add(jsonEvent);
            }
            Row row = spreadsheet.addRow();
            row.setCell("id", event.getExternalId());
            row.setCell("Name", studentName);
            row.setCell("Number", studentNumber);
            row.setCell("year", gratuityExecutionYearName);
            row.setCell("description", description.toString());
//                row.setCell("degree", degreeName);
//                row.setCell("degreeType", degreeTypeName);
            row.setCell("whenOccured", whenOccured);
            row.setCell("originalDebt", originalAmountToPay.toPlainString());
            row.setCell("dueDateAmountMap", getDueDateAmountMap(event));
            row.setCell("exemptionAmountMap", getExemptionAmountMap(event));
            row.setCell("discounts", toString(getDiscounts(event)));
            row.setCell("amountToPay", totalAmountToPay.toPlainString());
            row.setCell("amountToPayInterestDescription", getAmountToPayTodayInterestDescription(when, event));
            row.setCell("amountPayed", amountPayed.toPlainString());
            row.setCell("amountPayedDescription", getPaymentsDescription(event));
            row.setCell("amountInDebt", amountToPay.toPlainString());
            row.setCell("isOpen", Boolean.toString(event.isOpen()));
    }

    private static String toString(Map<LocalDate, Money> map) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> String.format("%s -> %s", e.getKey(), e.getValue().toPlainString())).collect(Collectors.joining("\n"));
    }

    private static Map<LocalDate, Money> getPayments(Event event) {
        return event.getSortedNonAdjustingTransactions().stream().collect(Collectors.toMap(t -> t.getWhenRegistered().toLocalDate(),
                                                                                           AccountingTransaction::getAmountWithAdjustment, Money::add));
    }

    private Map<LocalDate, Money> getDiscounts(Event event) {
        return event.getDiscountsSet().stream().sorted(Comparator.comparing(Discount::getWhenCreated)).collect(Collectors.toMap(d -> d.getWhenCreated().toLocalDate(), Discount::getAmount,
                                                                                                                                Money::add));
    }

    private static String getPaymentsDescription(Event event) {
        return toString(getPayments(event));
    }

    private String getExemptionAmountMap(Event event) {
        return "-";
    }

    private String getDueDateAmountMap(Event event) {
        return "-";
    }

    private String getAmountToPayTodayInterestDescription(DateTime when, Event event) {
        return "-";
    }

    private String getOriginalDebtInterestDescription(Event event) {
        return "-";
    }

    private boolean isGratuity(Event event) {
        return event.isGratuity() || event instanceof PhdGratuityEvent;
    }

    private ExecutionYear executionYearOf(final Event event) {
        return event instanceof AnnualEvent ? ((AnnualEvent) event).getExecutionYear() : ExecutionYear.readByDateTime(event.getWhenOccured());
    }

    private String computeHostName() {
//        try {
//            return InetAddress.getLocalHost().getHostName();
//        } catch (UnknownHostException e) {
//            return "<unknown-host>";
//        }
        return CoreConfiguration.getConfiguration().applicationUrl();
    }

    protected boolean isNotBefore(ExecutionYear year, Event e) {
        return !executionYearOf(e).isBefore(year);
    }

}