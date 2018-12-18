package pt.ist.fenix.webapp;

import java.util.Locale;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class CreateCandidacyLinkForAdmission extends CustomTask {

    private final Locale PT = new Locale("pt");
    private final Locale EN = new Locale("en");
    
    @Override
    public void runTask() throws Exception {
//        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
//            .filter(ap -> ap.getTitle().getContent(PT).contains("Concurso"))
//            .flatMap(ap -> ap.getApplicationSet().stream())
//            .filter(app -> app.get("candidacyId") != null)
//            .forEach(this::addLink);        
//    }
//
//    private void addLink(Application application) {
//        JsonObject data = new JsonParser().parse(application.getData()).getAsJsonObject();        
////        final JsonArray operatorLinks = new JsonArray();
////        data.add("operatorLinks", operatorLinks);
////        
////        addLink(operatorLinks, "Detalhes da Candidatura", "Candidacy Details", //fenix.tecnico.ulisboa.pt
////                "https://fenix.tecnico.ulisboa.pt/admissions/candidate/academicCandidacyDetails/" + application.get("candidacyId"));
////        
//        final JsonArray candidateLinks = new JsonArray();
//        data.add("candidateLinks", candidateLinks);
//        
//        addLink(candidateLinks, "Iniciar Processo de Matrícula", "Begin Registration Process", "http://localhost:8080/candidate/");
//        application.setData(data.toString());
    }
    
    private void addLink(final JsonArray links, final String pt, final String en, final String link) {
        final JsonObject result = new JsonObject();
        result.add("title", new LocalizedString(PT, pt).with(EN, en).json());
        result.addProperty("link", link);
        links.add(result);
    }
}
