package pt.ist.fenix.webapp;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.common.base.CharMatcher;

import pt.ist.fenixedu.giaf.invoices.EventWrapper;

public class FixPostalCodesPT extends CustomTask {

    Set<Party> processedPersons = null;

    @Override
    public void runTask() throws Exception {
        processedPersons = new HashSet<Party>();
        getEventsToProcess().forEach(this::process);
        throw new Error("Just testing");
    }

    private Stream<Event> getEventsToProcess() {
        return Bennu.getInstance().getAccountingEventsSet().stream().filter(e -> !e.isCancelled())
                .filter(e -> (EventWrapper.needsProcessingSap(e)));
    }

    private void process(Event event) {
        Party party = event.getParty();
        if (!processedPersons.contains(party)) {
            party.getPartyContactsSet().stream().filter(pc -> pc instanceof PhysicalAddress).map(pc -> (PhysicalAddress) pc)
                    .forEach(this::fix);
            processedPersons.add(party);
        }
    }

    private void fix(PhysicalAddress address) {
        if (address.getCountryOfResidence() != null && address.getCountryOfResidence().getCode().equals("PT")) {
            String areaCode = address.getAreaCode();
//            if (areaCode != null) {
//                areaCode = areaCode.replaceAll(" ", "");
//                if (areaCode.length() > 8 && areaCode.charAt(4) == '-' && CharMatcher.DIGIT.matchesAllOf(areaCode.substring(0, 4))
//                        && CharMatcher.DIGIT.matchesAllOf(areaCode.substring(5, 8))) {
//                    taskLog("OID: %s - Before: '%s' - After: %s\n", address.getExternalId(), address.getAreaCode(),
//                            areaCode.substring(0, 8));
//                    address.setAreaCode(areaCode.substring(0, 8));
//                } else if (areaCode.length() == 7 && CharMatcher.DIGIT.matchesAllOf(areaCode)) {
//                    taskLog("OID: %s - Before: '%s' - After: %s\n", address.getExternalId(), address.getAreaCode(),
//                            areaCode.substring(0, 4) + "-" + areaCode.substring(4));
//                    address.setAreaCode(areaCode.substring(0, 4) + "-" + areaCode.substring(4));
//                } else if (!address.getAreaCode().equals(areaCode) && areaCode.length() == 8 && areaCode.charAt(4) == '-'
//                        && CharMatcher.DIGIT.matchesAllOf(areaCode.substring(0, 4))
//                        && CharMatcher.DIGIT.matchesAllOf(areaCode.substring(5, 8))) {
//                    taskLog("OID: %s - Before: '%s' - After: %s\n", address.getExternalId(), address.getAreaCode(),
//                            areaCode.substring(0, 8));
//                    address.setAreaCode(areaCode.substring(0, 8));
//                }
//            }
        }
    }
}
