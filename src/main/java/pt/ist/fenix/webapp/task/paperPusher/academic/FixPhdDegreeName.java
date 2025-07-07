package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonParser;
import org.fenixedu.bennu.core.json.ImmutableJsonElement;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.smartForms.domain.SmartFormsSystem;

public class FixPhdDegreeName extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                .filter(type -> type.getName().toString().contains("Candidatura a Doutoramento"))
                .flatMap(type -> type.getRequestTypeVersionSet().stream())
                .flatMap(type -> type.getRequestSet().stream())
                .forEach(request -> {
                    String data = request.getData().get().toString();
                    data = data.replace("Diploma de Estudos Avançados em ", "");
                    data = data.replace("Advanced Studies Diploma in ", "");
                    request.setData(ImmutableJsonElement.of(JsonParser.parseString(data).getAsJsonObject()));
                });
    }
}
