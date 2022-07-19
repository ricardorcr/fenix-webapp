package pt.ist.fenix.webapp.task.connect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.PostalCodeValidator;
import org.fenixedu.TINValidator;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.geographic.Planet;
import pt.ist.standards.geographic.PostalCode;

import java.util.Comparator;
import java.util.stream.Stream;

public class FixPersonalInformationErrors extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        ConnectSystem.getInstance().getIdentitySet().stream().parallel()
                .forEach(this::process);
    }

    private void process(final Identity identity) {
        FenixFramework.atomic(() -> {
            final PersonalInformation personalInformation = identity.getPersonalInformation();
            final TaxInformation taxInformation = personalInformation.getTaxInformation();
            if (taxInformation != null) {
                process(taxInformation);
            }

            final String ssn = ssnFor(identity);
            if (ssn != null && ssn.length() > 2) {
                final String countryCode = ssn.substring(0, 2);
                final String number = ssn.substring(2);
                if (TINValidator.isValid(countryCode, number, true)) {
                    final PhysicalAddress physicalAddress = addressFor(identity, countryCode);
                    if (physicalAddress != null && (physicalAddress.getCountryOfResidence() == null
                            || physicalAddress.getCountryOfResidence().getCode().equals(countryCode))) {
                        if (taxInformation == null) {
                            taskLog("Can fill empty tax info for: %s%n", identity.getUser().getUsername());
                        }
                    }
                }
            }
/*
            if (taxInformation == null || !pt(taxInformation)) {

            }
 */
        });
    }

    private PhysicalAddress addressFor(final Identity identity, final String countryCode) {
        final User user = identity.getUser();
        final Person person = user == null ? null : user.getPerson();
        return addressStream(person)
                //.filter(physicalAddress -> physicalAddress.getCountryOfResidence() != null && countryCode.equals(physicalAddress.getCountryOfResidence().getCode()))
                .filter(physicalAddress -> physicalAddress.getAreaCode() != null)
                .filter(physicalAddress -> PostalCodeValidator.isValidAreaCode(countryCode, physicalAddress.getAreaCode()))
                .findAny().orElse(null);
    }

    private static Stream<PhysicalAddress> addressStream(final Party party) {
        return party.getPartyContactsSet().stream()
                .filter(PhysicalAddress.class::isInstance)
                .map(PhysicalAddress.class::cast)
                .sorted(ADDRESS_COMPARATOR);
    }

    private String ssnFor(final Identity identity) {
        final User user = identity.getUser();
        final Person person = user == null ? null : user.getPerson();
        return person.getSocialSecurityNumber();
    }

    private boolean pt(final TaxInformation taxInformation) {
        final String addressData = taxInformation.getAddressData();
        if (addressData != null && !addressData.isEmpty()) {
            final JsonObject address = new JsonParser().parse(addressData).getAsJsonObject();
            final String countryCode = JsonUtils.get(address, "countryCode");
            return "PT".equals(countryCode);
        }
        return false;
    }

    private void process(final TaxInformation taxInformation) {
        final String addressData = taxInformation.getAddressData();
        if (addressData != null && !addressData.isEmpty()) {
            final JsonObject address = new JsonParser().parse(addressData).getAsJsonObject();
            final String countryCode = JsonUtils.get(address, "countryCode");
            if ("PT".equals(countryCode)) {
                final String zipCode = JsonUtils.get(address, "zipCode");
                final String location = JsonUtils.get(address, "location");
                if (zipCode != null && (location == null || location.trim().isEmpty())) {
                    final PostalCode postalCode = Planet.getEarth().getByAlfa2(countryCode).getPostalCode(zipCode);
                    if (postalCode != null) {
                        final JsonObject details = postalCode.getDetails();
                        if (details == null) {
                            if (zipCode.equals("2005-336")) {
                                address.addProperty("location", "SANTARÉM");
                                taxInformation.setAddressData(address.toString());
                            } else {
                                taskLog("! No details for postcode = %s on identity %s%n",
                                        taxInformation.getPersonalInformation().getIdentity().getExternalId(),
                                        zipCode);
                            }
                        } else {
                            final String Localidade = details.get("Localidade").getAsString();
                            taskLog("Fixing: %s for %s from [%s] to [%s]%n",
                                    taxInformation.getPersonalInformation().getIdentity().getExternalId(),
                                    zipCode,
                                    location,
                                    Localidade);
                            address.addProperty("location", Localidade);
                            taxInformation.setAddressData(address.toString());
                        }
                    }
                }
            }
        }
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

}