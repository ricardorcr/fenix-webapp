package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FixDuplicateMEQGratuityEvent extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Map<Person, Set<Event>> map = Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(Event::isGratuity)
                .filter(event -> event.getDescriptionI18N().toString().contains("Propina (MEQ21 2024/2025)"))
                .collect(Collectors.toMap(Event::getPerson, this::toSet, this::merge));

        final Spreadsheet spreadsheet = new Spreadsheet("Propinas duplicadas");
        map.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .filter(entry -> entry.getValue().stream().anyMatch(Event::isOpen))
                .filter(entry -> entry.getValue().stream().anyMatch(Event::isCancelled))
                .filter(entry -> entry.getValue().stream().anyMatch(event -> !event.isCancelled() && event.isClosed()))
                .forEach(entry -> report(entry.getValue(), spreadsheet));

        output("propinas_MEQ21_duplicadas.xlsx", spreadsheet.exportToXLSXSheet());
    }

    private void report(final Set<Event> events, final Spreadsheet spreadsheet) {
        events.forEach( event -> {
            Spreadsheet.Row row = spreadsheet.addRow();
            row.setCell("OID", event.getExternalId());
            row.setCell("IstID", event.getPerson().getUsername());
            row.setCell("Nome", event.getDescriptionI18N().getContent());
            row.setCell("Estado", event.getEventState().toString());
            row.setCell("Inicial", event.getOriginalAmountToPay().toString());
            row.setCell("Actual", event.getAmountToPay().toString());
        });
    }

    private Set<Event> merge(final Set<Event> s1, final Set<Event> s2) {
        s1.addAll(s2);
        return s1;
    }

    private Set<Event> toSet(final Event event) {
        final Set<Event> set = new HashSet<>();
        set.add(event);
        return set;
    }
}
