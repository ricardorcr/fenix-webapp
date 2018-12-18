package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ChangeFirstTimeDocumentConfiguration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("adminProcessSheet", "no");
        json.addProperty("registrationDeclaration", "no");
        json.addProperty("timeTable", "no");
        json.addProperty("gratuityPaymentCodes", "no");

        JsonArray classFilters = new JsonArray();
        classFilters.add(new JsonPrimitive("org.fenixedu.idcards.ui.candidacydocfiller.BPIPdfFiller"));
        //classFilters.add(new JsonPrimitive("org.fenixedu.idcards.ui.candidacydocfiller.SantanderPdfFiller"));
        json.add("classFilters", classFilters);

        //FirstTimeDocumentsConfiguration.getInstance().setConfigurationProperties(json);
    }
}
