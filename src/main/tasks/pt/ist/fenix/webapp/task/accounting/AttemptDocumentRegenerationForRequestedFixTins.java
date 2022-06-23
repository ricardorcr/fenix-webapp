package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.PostalCodeValidator;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.admissions.util.RemoteReader;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

public class AttemptDocumentRegenerationForRequestedFixTins extends WriteCustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        Arrays.stream(string("spam_tin_pc.txt").split("\n"))
                .filter(s -> !s.trim().isEmpty())
                .map(s -> (Event) FenixFramework.getDomainObject(s.trim()))
                .filter(event -> event != null)
                .filter(event -> hasMultipleDebtDocuments(event))
                .forEach(event -> taskLog("Multiple debts in event = %s%n", event.getExternalId()));

        Arrays.stream(string("spam_tin_pc.txt").split("\n"))
                .filter(s -> !s.trim().isEmpty())
                .map(s -> (Event) FenixFramework.getDomainObject(s.trim()))
                .filter(event -> event != null)
                .filter(event -> hasErrors(event))
                .forEach(event -> {
                    final Party party = event.getParty();
                    final String tin = ClientMap.uVATNumberFor(party);
                    final String countryCode = tin.substring(0, 2);
                    final PhysicalAddress address = Utils.toAddress(party, tin.substring(0, 2));
                    taskLog("Event: %s = %s : %s : %s%n",
                            event.getExternalId(),
                            tin,
                            address.getCountryOfResidence().getCode(),
                            address.getPostalCode());
                    if (!countryCode.equals(address.getCountryOfResidence().getCode())) {
                        final String examplePostCode = PostalCodeValidator.examplePostCodeFor(countryCode);
                        new PhysicalAddress(party, PartyContactType.PERSONAL, Boolean.FALSE,  "Unknown",
                                examplePostCode, "Unknown", "Unknown", "Unknown",
                                "Unknown", "Unknown",
                                Country.readByTwoLetterCode(countryCode));
                        final PhysicalAddress newAddress = //Utils.toAddress(party, tin.substring(0, 2));
                                toAddress(party, tin.substring(0, 2));
                        taskLog("   New Address = %s : %s%n", newAddress.getCountryOfResidence().getCode(), newAddress.getPostalCode());
                    }

                    event.getSapRequestSet().stream()
                            .filter(sapRequest -> !sapRequest.getIntegrated())
                            .filter(sapRequest -> !sapRequest.getIgnore())
                            .forEach(sapRequest -> {
                                taskLog("   %s = %s%n", sapRequest.getExternalId(), sapRequest.getDocumentNumber());
                                sapRequest.delete();
                            });
                });
//        throw new Error("Abort TX");
    }

    private boolean hasMultipleDebtDocuments(final Event event) {
        return event.getSapRequestSet().stream()
                .filter(sapRequest -> sapRequest.getRequestType() == SapRequestType.DEBT)
                .count() > 1l;
    }

    private boolean hasErrors(final Event event) {
        return event.getSapRequestSet().stream()
                .filter(sapRequest -> !sapRequest.getIntegrated())
                .filter(sapRequest -> !sapRequest.getIgnore())
                .anyMatch(sapRequest -> sapRequest.getSent());
    }

    @Override
    public String baseUrl() {
        return "https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/admissions/test/";
    }

    private static final Comparator<PhysicalAddress> ADDRESS_COMPARATOR = (a1, a2) -> {
        boolean d1 = a1.getDefaultContact();
        boolean d2 = a2.getDefaultContact();
        boolean ac1 = a1.getActive();
        boolean ac2 = a2.getActive();
        // some addresses don't have an associated country and that is relevant for the postal codes
        Country c1 = a1.getCountryOfResidence();
        Country c2 = a2.getCountryOfResidence();
        return (c1 != null && c2 == null) ? -1 :
                (
                        (c2 != null && c1 == null) ? 1 :
                                (
                                        (ac1 && !ac2) ? -1 : (ac2 && !ac1) ? 1 :
                                                (
                                                        (d1 && !d2) ? -1 : (d2 && !d1) ? 1 :
                                                                a1.getExternalId().compareTo(a2.getExternalId())
                                                )
                                )
                );
    };

    private static Stream<PhysicalAddress> addressStream(final Party party) {
        return party.getPartyContactsSet().stream()
                .filter(PhysicalAddress.class::isInstance)
                .map(PhysicalAddress.class::cast)
                .sorted(ADDRESS_COMPARATOR);
    }

    public static PhysicalAddress toAddress(final Party party, final String countryCode) {
        return addressStream(party)
                .filter(a -> a.getCountryOfResidence() != null && a.getCountryOfResidence().getCode().equals(countryCode))
                .findFirst()
                .orElseGet(() -> addressStream(party).findFirst().orElse(null));
    }

}