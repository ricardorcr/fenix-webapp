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
import org.fenixedu.connect.util.AddressUtils;
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
                if (TINValidator.isValid(countryCode, number, true) && isPersonalNumber(ssn)) {
                    final PhysicalAddress physicalAddress = addressFor(identity, countryCode);
                    if (physicalAddress != null && (physicalAddress.getCountryOfResidence() == null
                            || physicalAddress.getCountryOfResidence().getCode().equals(countryCode))) {
                        final JsonObject address = addressFor(identity, ssn, countryCode, physicalAddress);
                        if (address != null) {
                            if (taxInformation == null) {
                                taskLog("Filled empty tax info for: %s%n", identity.getUser().getUsername());
                                new TaxInformation(identity.getPersonalInformation(), ssn, address.toString());
                            } else if (!taxInformation.isValid()) {
                                taskLog("Fix invalid tax info for: %s from [%s] to [%s]%n",
                                        identity.getUser().getUsername(),
                                        taxInformation.getTin(),
                                        ssn);
                                taxInformation.delete();
                                new TaxInformation(identity.getPersonalInformation(), ssn, address.toString());
                            } else if ("PT".equals(countryCode) && !pt(taxInformation)) {
                                taskLog("Possible tax info improvement for: %s from [%s] to [%s]%n",
                                        identity.getUser().getUsername(),
                                        taxInformation.getTin(),
                                        ssn);
                                taxInformation.delete();
                                new TaxInformation(identity.getPersonalInformation(), ssn, address.toString());
                            }
                        }
                    }
                }
            }
        });
    }

    private JsonObject addressFor(final Identity identity, final String ssn, final String countryCode, final PhysicalAddress physicalAddress) {
        final JsonObject address = new JsonObject();
        address.addProperty("firstLine", physicalAddress.getAddress());
        address.addProperty("zipCode", physicalAddress.getAreaCode());
        final String area = physicalAddress.getArea();
        final String location = (area == null || area.trim().isEmpty()) ? fixAreaCode(countryCode, physicalAddress.getAreaCode()) : area;
        address.addProperty("location", location);
        address.addProperty("countryCode", countryCode);

        return AddressUtils.isValid(address.toString()) ? address : null;
    }

    private String fixAreaCode(final String countryCode, final String areaCode) {
        if ("PT".equals(countryCode)) {
            final PostalCode postalCode = Planet.getEarth().getByAlfa2(countryCode).getPostalCode(areaCode);
            if (postalCode != null) {
                final JsonObject details = postalCode.getDetails();
                if (details != null) {
                    return details.get("Localidade").getAsString();
                }
            }
        }
        return "";
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
        return person == null ? null : person.getSocialSecurityNumber();
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

    private boolean isPersonalNumber(final String tin) {
        if (tin != null && "PT".equals(tin.substring(0, 2))) {
            final String number = tin.substring(2);
            if (allNinesAndZeros(number)) {
                return false;
            }
            for (int i = 0; i < number.length(); i++) {
                final char c = number.charAt(i);
                final char nc = i + 1 < number.length() ? number.charAt(i + 1) : 'X';
                if (c == '0') {
                    // skip;
                } else if (c == '1' || c == '2' || c == '3') {
                    return true;
                } else if (c == '4' && nc == '5') {
                    return true;
                } else if (c == '7' && (nc == '0' || nc == '4' || nc == '5')) {
                    return true;
                } else if (c == '7' && nc == '7') {
                    return true;
                } else if (c == '7' && nc == '8') {
                    return true;
                } else if (c == '9' && nc == '8') {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
        return true;
    }

    private boolean allNinesAndZeros(final String number) {
        for (int i = 0; i < number.length(); i++) {
            final char c = number.charAt(i);
            if (c != '0' && c != '9') {
                return false;
            }
        }
        return true;
    }

}