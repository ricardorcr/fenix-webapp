package pt.ist.fenix.webapp.task;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.smartFlow.domain.ActionNode;
import org.fenixedu.smartFlow.domain.FlowNode;
import org.fenixedu.smartFlow.domain.FlowQueue;
import org.fenixedu.smartFlow.domain.FlowState;
import org.fenixedu.smartFlow.domain.Node;
import org.fenixedu.smartFlow.domain.SmartFlowSystem;
import pt.ist.fenixframework.FenixFramework;

import java.util.HashMap;
import java.util.Map;

public class TerminatePendingFlowsWhenPossible extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Map<FlowQueue, Map<LocalizedString, Map<LocalizedString, Integer>>> mapFlow = new HashMap<>();
        final Table<User, AdmissionProcess, String> table = HashBasedTable.create();
        Spreadsheet spreadsheet = new Spreadsheet("Flows pendentes");
        SmartFlowSystem.getInstance().getFlowTemplateSet().stream()
                .flatMap(flowTemplate -> flowTemplate.getFlowSet().stream())
                .filter(flow -> flow.getState() == FlowState.RUNNING)
                .forEach(flow -> {
                    final Node node = flow.nodeBy(flow.getCurrentFlowNode().getNodeName());
                    if (node instanceof ActionNode actionNode) {
                        final FlowNode currentFlowNode = flow.getCurrentFlowNode();
                        final FlowQueue flowQueue = flow.getCurrentFlowQueue();
                        final LocalizedString flowTemplateTitle = flow.getFlowTemplate().getTitle();
                        final LocalizedString actionTitle = actionNode.description;

                        if (flow.getData().get().has("application")) {
                            final Application application = JsonUtils.toDomainObject(flow.getData().get(),
                                    "application");
                            final Identity identity = application.getAccount().getIdentity();
                            final User user = identity.getUser();
                            if (flowQueue.isMember(user)) {
                                Spreadsheet.Row row = spreadsheet.addRow();
                                row.setCell("User", user.getUsername());
                                row.setCell("Processo",
                                        application.getAdmissionProcessTarget().getAdmissionProcess().getTitle().getContent());
                                row.setCell("Flow", flowTemplateTitle.getContent());
                                row.setCell("Nó", actionTitle.getContent());
                                final Registration registration = JsonUtils.toDomainObject(application.getDataObject(),
                                        "registration");
                                row.setCell("Estado Matrícula", registration != null ?
                                        registration.getLastState().getStateType().getName() : "-");
                                final boolean isRegistrationActive = registration != null && registration.isActive();
                                row.setCell("Matrícula Activa", registration != null ?
                                        String.valueOf(registration.isActive()) : "não tem");
                                row.setCell("Flow terminado", String.valueOf(!isRegistrationActive));

                                if (!isRegistrationActive) {
                                    terminate(currentFlowNode);
                                }
                            }
                        }
                    }
                });

        output("flows_pendentes.xlsx", spreadsheet.exportToXLSXSheet());
    }

    private void terminate(final FlowNode currentNode) {
        FenixFramework.atomic(() -> {
            FlowNode cancel = new FlowNode(currentNode.getPrev(), "cancel");
            currentNode.delete();
            cancel.flow().doAction("CANCEL", new JsonObject().toString(), false, false);
        });
    }
}
