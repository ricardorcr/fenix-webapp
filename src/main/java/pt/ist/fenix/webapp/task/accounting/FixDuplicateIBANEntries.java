package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.IBANPayment;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FixDuplicateIBANEntries extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Map<LocalDate, Map<String, Set<IBANPayment>>> map = new TreeMap<>();
        Bennu.getInstance().getIBANGroupSet().stream()
                .flatMap(ibanGroup -> ibanGroup.getIBANSet().stream())
                .flatMap(iban -> iban.getIBANPaymentSet().stream())
                .filter(iban -> iban.getSettlementDate().getYear() == 2025)
                .forEach(iban -> {
                    final String settlement = iban.getSettlement();
                    map.computeIfAbsent(iban.getSettlementDate(), d -> new TreeMap<>())
                            .computeIfAbsent(settlement, k -> new HashSet<>())
                            .add(iban);
                });

        map.forEach((date, m) -> {
            m.values().stream()
                    .filter(set -> set.size() > 1)
        });
    }

}