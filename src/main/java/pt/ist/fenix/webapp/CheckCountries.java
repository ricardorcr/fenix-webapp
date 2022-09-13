package pt.ist.fenix.webapp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.PersonalInformation_Base;

import java.util.Arrays;
import java.util.List;

public class CheckCountries extends CustomTask {
    @Override
    public void runTask() throws Exception {

        final List<String> removedCountries = Arrays.asList("AN", "CS", "CT", "FQ", "JT", "MI", "NQ", "NT", "PC", "PU", "PZ", "SU", "WK", "YU");
        ConnectSystem.getInstance().getAccountSet().stream()
                .filter(ac -> ac.getPersonalInformation() != null)
                .map(Account::getPersonalInformation)
                .filter(pi -> pi.getNationalityCountryCode() != null)
                .filter(pi -> removedCountries.contains(pi.getNationalityCountryCode()))
                .forEach(pi -> taskLog("Account: %s\tCountry: %s%n", pi.getAccount().getExternalId(), pi.getNationalityCountryCode()));

        ConnectSystem.getInstance().getAccountSet().stream()
                .filter(ac -> ac.getPersonalInformation() != null)
                .map(Account::getPersonalInformation)
                .filter(pi -> pi.getTaxInformation() != null)
                .map(PersonalInformation::getTaxInformation)
                .filter(ti -> ti.getTin() != null)
                .filter(ti -> removedCountries.contains(ti.getTin().substring(0,2)))
                .forEach(ti -> taskLog("Account TIN: %s\tCountry: %s%n", ti.getPersonalInformation().getAccount().getExternalId(), ti.getTin().substring(0,2)));

        ConnectSystem.getInstance().getAccountSet().stream()
                .filter(ac -> ac.getPersonalInformation() != null)
                .map(Account::getPersonalInformation)
                .filter(pi -> pi.getTaxInformation() != null)
                .map(PersonalInformation::getTaxInformation)
                .filter(ti -> ti.getAddressData() != null)
                .filter(ti -> {
                    final JsonObject address = JsonParser.parseString(ti.getAddressData()).getAsJsonObject();
                    if (address.has("countryCode")) {
                        return removedCountries.contains(address.get("countryCode").getAsString());
                    } else {
                        return false;
                    }
                })
                .forEach(ti -> taskLog("Account Address: %s\tCountry: %s%n", ti.getPersonalInformation().getAccount().getExternalId(), "have to check"));

        ConnectSystem.getInstance().getIdentitySet().stream()
                .filter(id -> id.getPersonalInformation() != null)
                .map(Identity::getPersonalInformation)
                .filter(pi -> pi.getNationalityCountryCode() != null)
                .filter(pi -> removedCountries.contains(pi.getNationalityCountryCode()))
                .forEach(pi -> taskLog("Identity: %s\tCountry: %s%n", pi.getIdentity().getExternalId(), pi.getNationalityCountryCode()));

        ConnectSystem.getInstance().getIdentitySet().stream()
                .filter(id -> id.getPersonalInformation() != null)
                .map(Identity::getPersonalInformation)
                .filter(pi -> pi.getTaxInformation() != null)
                .map(PersonalInformation::getTaxInformation)
                .filter(ti -> ti.getTin() != null)
                .filter(ti -> removedCountries.contains(ti.getTin().substring(0,2)))
                .forEach(ti -> taskLog("Identity TIN: %s\tCountry: %s%n", ti.getPersonalInformation().getIdentity().getExternalId(), ti.getTin().substring(0,2)));

        ConnectSystem.getInstance().getIdentitySet().stream()
                .filter(id -> id.getPersonalInformation() != null)
                .map(Identity::getPersonalInformation)
                .filter(pi -> pi.getTaxInformation() != null)
                .map(PersonalInformation::getTaxInformation)
                .filter(ti -> ti.getAddressData() != null)
                .filter(ti -> {
                    final JsonObject address = JsonParser.parseString(ti.getAddressData()).getAsJsonObject();
                    if (address.has("countryCode")) {
                        return removedCountries.contains(address.get("countryCode").getAsString());
                    } else {
                        return false;
                    }
                })
                .forEach(ti -> taskLog("Identity Address: %s\tCountry: %s%n", ti.getPersonalInformation().getIdentity().getExternalId(), "have to check"));
    }
}
