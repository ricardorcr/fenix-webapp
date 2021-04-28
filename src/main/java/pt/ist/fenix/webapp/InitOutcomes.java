package pt.ist.fenix.webapp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;

import java.util.Locale;

public class InitOutcomes extends CustomTask {

    private final String PREFIX = "Candidaturas a ";

    private static final Locale PT = new Locale("pt");
    private static final Locale EN = new Locale("en");

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .forEach(this::init);
    }

    private void init(final AdmissionProcessTarget target) {
        final String targetName = target.getName().getContent(PT);
        final String name = targetName.substring(PREFIX.length());
        final Degree degree = degreeFor(name);
//        taskLog("Name: %s -> %s : %s (applications): %s%n",
//                name,
//                degree.getSigla(),
//                target.getApplicationSet().size(),
//                target.getOutcomeConfigJson());

        final CycleType cycleType = name.contains("Licenciatura") || name.contains("1º Ciclo")
                ? CycleType.FIRST_CYCLE : CycleType.SECOND_CYCLE;
        final RegistrationProtocol protocol = Bennu.getInstance().getRegistrationProtocolsSet().stream()
                .filter(p -> p.getCode().equals("ALIEN"))
                .findAny().orElse(null);

        final JsonObject outcome = new JsonObject();
        outcome.addProperty("degree", degree.getExternalId());
        outcome.addProperty("protocol", protocol.getExternalId());
        outcome.addProperty("year", ExecutionYear.readCurrentExecutionYear().getNextExecutionYear().getExternalId());
        outcome.addProperty("cycleType", cycleType.name());
        outcome.add("cost", costFor(degree));
        outcome.add("actionName", ls("Matrícular", "Enroll").json());
//        target.setOutcomeConfig(outcome.toString());
    }

    private JsonElement costFor(final Degree degree) {
        final String value = degree.getSigla().equals("MOTU") ? "3500" : "7000";
        final String firstPayment = degree.getSigla().equals("MOTU") ? "1000" : "2000";
        final JsonObject cost = new JsonObject();
        cost.add("description", ls("Propina: " + value + "€ / ano. Para proceder com o processo de matrícula tem de avançar com o pagamento inicial de " + firstPayment + "€.",
                "Tuition Fee: " + value + "€ / year. To proceed with the registration " + firstPayment + "€ must be payed in advance.").json());
        return cost;
    }

    private Degree degreeFor(final String name) {
        final int i = name.indexOf(" (");
        final String s = i > 0 ? name.substring(0, i) : name;
        return Bennu.getInstance().getDegreesSet().stream()
                .filter(degree -> match(degree, name))
                .findAny().orElseThrow(() -> new Error("No degree found for " + name));
    }

    private boolean match(final Degree degree, final String name) {
        final int i = name.indexOf(" (");
        final String s = i > 0 ? name.substring(0, i) : name;
        return degree.getPresentationName().equals(s)
                && (degree.isFirstCycle() || degree.isSecondCycle());
    }

    private LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(PT, pt).with(EN, en);
    }

}
