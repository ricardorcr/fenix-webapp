package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonElement;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.serviceRequests.documentRequests.PhdRegistryDiplomaRequest;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.RegistryDiplomaRequest;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.dynamicForms.DynamicForm;
import org.fenixedu.smartForms.domain.RequestState;
import org.fenixedu.smartForms.domain.SmartFormsSystem;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;

public class ReportMissingDiplomaSupplement extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Suplementos em falta");
        SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                .filter(rt -> rt.getName().getContent().contains("Certidão de Registo"))
                .flatMap(rt -> rt.getCurrentRequestTypeVersion().getRequestSet().stream())
                .forEach(request -> {
                    if (request.getData().get().getAsJsonObject().has("oldDocumentRequest")) {
                        final String oldRequestOID = request.getData().get().getAsJsonObject().get("oldDocumentRequest").getAsString();
                        final AcademicServiceRequest academicRequest = FenixFramework.getDomainObject(oldRequestOID);
                        /*if (academicRequest instanceof RegistryDiplomaRequest registryRequest) {
                            if (registryRequest.getDiplomaSupplement() != null) {
                                final JsonElement requestId = request.getData().get().getAsJsonObject().get("requestId");
                                final JsonElement flowId = request.getData().get().getAsJsonObject().get("flowId");
                                boolean isSameFlow = false;
                                if (flowId != null && !flowId.isJsonNull()) {
                                    isSameFlow = request.getRequester().getRequestSet().stream()
                                            .filter(req -> req.getRequestType().getName().getContent().equals("Suplemento ao Diploma"))
                                            .anyMatch(req -> {
                                                final JsonElement flowIdSupp = req.getData().get().getAsJsonObject().get("flowId");
                                                return flowIdSupp != null && !flowIdSupp.isJsonNull() && flowIdSupp.getAsString().equals(flowId.getAsString());
                                            });
                                }
                                if (!isSameFlow && (requestId == null || requestId.isJsonNull())) {
                                    final DynamicForm dynamicForm = request.inputForm();
                                    final Registration registration = registryRequest.getRegistration();
                                    final PhdIndividualProgramProcess phdIndividualProgramProcess = registration.getPhdIndividualProgramProcess();
                                    final ProgramConclusion programConclusion = FenixFramework.getDomainObject(
                                            dynamicForm.get("PROGRAMME_CONCLUSION_TYPE").value());

                                    final RegistrationConclusionBean registrationConclusionBean = new RegistrationConclusionBean(registration, programConclusion);
                                    final boolean isConfirmed = phdIndividualProgramProcess == null
                                            ? registrationConclusionBean.getCurriculumGroup() != null && registrationConclusionBean.isConclusionProcessed()
                                            : phdIndividualProgramProcess.getLastConclusionProcess() != null;

                                    final Spreadsheet.Row row = spreadsheet.addRow();
                                    row.setCell("Aluno", registryRequest.getRegistration().getPerson().getUsername());
                                    row.setCell("Estado pedido", academicRequest.getActiveSituation().getAcademicServiceRequestSituationType().getName());
                                    row.setCell("Apuramento", isConfirmed ? "Sim" : "Não");
                                    row.setCell("Novo Pedido", request.getExternalId());
                                    final RequestState.RequestStatePlug statePlug = RequestState.PLUG.apply(request);
                                    row.setCell("Estado", statePlug.getDescription().getContent());
                                    row.setCell("Descrição", request.keywordStream().map(LocalizedString::getContent).collect(Collectors.joining(",")));
                                }
                            }
                        } else */if (academicRequest instanceof PhdRegistryDiplomaRequest phdRequest) {
                            if (phdRequest.getDiplomaSupplement() != null) {
                                final JsonElement requestId = request.getData().get().getAsJsonObject().get("requestId");
                                final JsonElement flowId = request.getData().get().getAsJsonObject().get("flowId");
                                boolean isSameFlow = false;
                                if (flowId != null && !flowId.isJsonNull()) {
                                    isSameFlow = request.getRequester().getRequestSet().stream()
                                            .filter(req -> req.getRequestTypeVersion().getRequestType().getName().getContent().equals("Suplemento ao Diploma"))
                                            .anyMatch(req -> {
                                                final JsonElement flowIdSupp = req.getData().get().getAsJsonObject().get("flowId");
                                                return flowIdSupp != null && !flowIdSupp.isJsonNull() && flowIdSupp.getAsString().equals(flowId.getAsString());
                                            });
                                }
                                if (!isSameFlow && (requestId == null || requestId.isJsonNull())) {
                                    final DynamicForm dynamicForm = request.inputForm(true);
                                    final Registration registration = phdRequest.getPhdIndividualProgramProcess().getRegistration();
                                    final PhdIndividualProgramProcess phdIndividualProgramProcess = registration.getPhdIndividualProgramProcess();
                                    final ProgramConclusion programConclusion = FenixFramework.getDomainObject(
                                            dynamicForm.get("PROGRAMME_CONCLUSION_TYPE").value());

                                    final RegistrationConclusionBean registrationConclusionBean = new RegistrationConclusionBean(registration, programConclusion);
                                    final boolean isConfirmed = phdIndividualProgramProcess == null
                                            ? registrationConclusionBean.getCurriculumGroup() != null && registrationConclusionBean.isConclusionProcessed()
                                            : phdIndividualProgramProcess.getLastConclusionProcess() != null;

                                    final Spreadsheet.Row row = spreadsheet.addRow();
                                    row.setCell("Aluno", phdRequest.getPhdIndividualProgramProcess().getRegistration().getPerson().getUsername());
                                    row.setCell("Estado pedido", academicRequest.getActiveSituation().getAcademicServiceRequestSituationType().getName());
                                    row.setCell("Apuramento", isConfirmed ? "Sim" : "Não");
                                    row.setCell("Novo Pedido", request.getExternalId());
                                    final RequestState.RequestStatePlug statePlug = RequestState.PLUG.apply(request);
                                    row.setCell("Estado", statePlug.getDescription().getContent());
                                    row.setCell("Descrição", request.keywordStream().map(LocalizedString::getContent).collect(Collectors.joining(",")));
                                }
                            }
                        }
                    }
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSXSheet(baos);
        output("missing_diploma_suplement.xlsx", baos.toByteArray());
    }
}

