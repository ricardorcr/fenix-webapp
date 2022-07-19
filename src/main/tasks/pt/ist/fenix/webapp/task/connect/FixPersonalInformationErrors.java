package pt.ist.fenix.webapp.task.connect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.geographic.Planet;
import pt.ist.standards.geographic.PostalCode;

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
        });
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
                            taskLog("! No details for postcode = %s on identity %s%n",
                                    taxInformation.getPersonalInformation().getIdentity().getExternalId(),
                                    zipCode);
                        } else {
                            final String Localidade = details.get("Localidade").getAsString();
                            taskLog("Fixing: %s for %s from [%s] to [%s]%n",
                                    taxInformation.getPersonalInformation().getIdentity().getExternalId(),
                                    zipCode,
                                    location,
                                    Localidade);
                            address.addProperty("location", Localidade);
                            taxInformation.setAddressData(addressData.toString());
                        }
                    }
                }
            }
        }
    }

}