package pt.ist.fenix.webapp.task.connect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.identification.TaxInformation;
import pt.ist.fenixframework.FenixFramework;

public class HackAccounts extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Account account = FenixFramework.getDomainObject("1697512810074933");
        final TaxInformation taxInformation = account.getIdentity().getPersonalInformation().getTaxInformation();
        final JsonObject address = new JsonParser().parse(taxInformation.getAddressData()).getAsJsonObject();
        address.addProperty("zipCode", "83324-050");
        taxInformation.setAddressData(address.toString());
    }

}