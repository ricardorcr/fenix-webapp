package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.stream.Collectors;

public class AddMobilityCoordinatorsToJury extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> {
                    final JsonObject outcomeJson = ap.getOutcomeConfigJson();
                    final String type = outcomeJson.get("type").getAsJsonObject().get("name").getAsString();
                    return type.equals("mobilityInboundDoubleDegree") || type.equals("mobilityInbound");
                })
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .forEach(this::addJury);
    }

    private void addJury(AdmissionProcessTarget target) {
        Degree degree = FenixFramework.getDomainObject(target.getOutcomeConfigJson().get("degree").getAsString());
        target.getJurySet().addAll(degree.getMobilityCoordinatorsSet().stream()
                                    .map(user -> user.getAccount())
                                    .collect(Collectors.toSet()));
    }
}
