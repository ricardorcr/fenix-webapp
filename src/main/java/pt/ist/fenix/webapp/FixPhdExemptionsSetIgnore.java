package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.HashMap;
import java.util.Map;

public class FixPhdExemptionsSetIgnore extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Map<String, String> exemptionsMap = new HashMap<>();
        exemptionsMap.put("1132149084259687",	"NA894200");
        exemptionsMap.put("1132149084259770",	"NA893958");
        exemptionsMap.put("1132149084259773",	"NA893504");
        exemptionsMap.put("1132149084259908",	"NA893496");
        exemptionsMap.put("1132149084259939",	"NR892658");
        exemptionsMap.put("1413624060969672",	"NA892625");
        exemptionsMap.put("1695099037680493",	"NA892612");
        exemptionsMap.put("1132149084259890",	"NA892604");
        exemptionsMap.put("1132149084259810",	"NA892600");
        exemptionsMap.put("1132149084259860",	"NA892592");
        exemptionsMap.put("1132149084259876",	"NA892588");
        exemptionsMap.put("1132149084259844",	"NA892586");
        exemptionsMap.put("1695099037680491",	"NA892557");
        exemptionsMap.put("1132149084259471",	"NA892555");
        exemptionsMap.put("1132149084259899",	"NA892553");
        exemptionsMap.put("1132149084259444",	"NA892551");
        exemptionsMap.put("287724154127668",  "NA892549");
        exemptionsMap.put("287724154127751",  "NA888001");
        exemptionsMap.put("287724154127762",  "NA887990");
        exemptionsMap.put("1132149084259481",	"NA887562");
        exemptionsMap.put("1132149084259523",	"NA887552");
        exemptionsMap.put("1132149084259567",	"NA887539");
        exemptionsMap.put("1132149084259931",	"NA887447");
        exemptionsMap.put("1132149084260016",	"NA887431");
        exemptionsMap.put("1695099037680629",	"NA886029");
        exemptionsMap.put("1976574014390873",	"NA886004");

        exemptionsMap.entrySet().stream()
                .forEach(entry -> {
                    final Event event = FenixFramework.getDomainObject(entry.getKey());
                    event.getSapRequestSet().stream()
                            .filter(sr -> sr.getDocumentNumber().equals(entry.getValue()))
                            .forEach(sr -> sr.setIgnore(true));
                });
    }
}
