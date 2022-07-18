package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonObject;
import org.fenixedu.TINValidator;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.fenixedu.connect.util.AddressUtils;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.geographic.Planet;
import pt.ist.standards.geographic.PostalCode;

public class DebugEventStuff extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Event event = FenixFramework.getDomainObject("1978579764118023");
        final Person person = event.getPerson();
        test(person);
        //ClientMap.uVATNumberFor(person);
    }

    private String test(final Party party) {
        taskLog("1");
        final PersonalInformation personalInformation = personalInformationFor((Person) party);
        taskLog("2");
        if (personalInformation != null) {
            taskLog("3");
            final TaxInformation taxInformation = personalInformation.getTaxInformation();
            taskLog("4");
            if (taxInformation != null && taxInformation.isValid()) {
                taskLog("5");
                final String tin = taxInformation.getTin();
                taskLog("6");
                return tin != null && !tin.isEmpty() ? tin
                        : (AddressUtils.getCountryCodeFor(taxInformation.getAddressData()) + party.getExternalId());
            }
            taskLog("7");
        }

        taskLog("8");
        final String tin = party.getSocialSecurityNumber();
        if (tin != null && !tin.trim().isEmpty()) {
            if (tin.length() > 2
                    && Character.isAlphabetic(tin.charAt(0))
                    && Character.isAlphabetic(tin.charAt(1))
                    && Character.isUpperCase(tin.charAt(0))
                    && Character.isUpperCase(tin.charAt(1))) {
                final String countryCode = tin.substring(0, 2);
                final String code = tin.substring(2);
                if (TINValidator.isValid(countryCode, code)) {
                    // all is ok
                    return tin;
                }
            }
            final Country country = getValidCountry(tin, party.getCountry(), party.getCountryOfResidence(), (party.isPerson() ? ((Person) party).getCountryOfBirth() : null), Country.readByTwoLetterCode("PT"));
            if (country != null) {
                return country.getCode() + tin;
            }
        }
        if (tin != null && tin.length() > 2 && !"PT".equals(tin.substring(0, 2)) && Country.readByTwoLetterCode(tin.substring(0, 2)) != null) {
            return tin;
        }
        final Country country = party.getCountry();
        if (country != null && !country.getCode().equals("PT")) {
            return country.getCode() + party.getExternalId();
        }
        return "PT999999990";
    };

    private PersonalInformation personalInformationFor(final Person person) {
        final User user = person.getUser();
        final Identity identity = user == null ? null : user.getIdentity();
        Account account = identity == null ? null : ConnectSystem.getMostRelevantAccount(identity);
        if (account == null && user != null) {
            account = user.getAccount();
        }
        if (account == null) {
            account = findByEvent(person);
        }
        return account == null ? null : ConnectSystem.getPersonalInformationFor(account);
    }

    private Account findByEvent(final Person person) {
        return person.getEventsSet().stream()
                .flatMap(event -> event.getApplicationSet().stream())
                .filter(application -> application != null)
                .map(application -> application.getAccount())
                .findAny().orElse(null);
    }

    private String districtFor(final String countryCode, final String zipCode) {
        final PostalCode postalCode = Planet.getEarth().getByAlfa2(countryCode).getPostalCode(zipCode);
        final JsonObject details = postalCode == null ? null : postalCode.getDetails();
        return details == null ? null : JsonUtils.get(details, "Distrito");
    }

    private static Country getValidCountry(final String tin, final Country... countries) {
        for (int i = 0; i < countries.length; i++) {
            final Country country = countries[i];
            if (country != null && TINValidator.isValid(country.getCode().toUpperCase(), tin, false)) {
                return country;
            }
        }
        return null;
    }

}