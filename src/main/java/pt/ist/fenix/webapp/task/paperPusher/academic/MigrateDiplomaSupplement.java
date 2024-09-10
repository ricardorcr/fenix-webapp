package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonObject;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.documents.GeneratedDocument;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituation;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituationType;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DiplomaSupplementRequest;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.RegistryDiplomaRequest;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.ImmutableJsonElement;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.service.AccountService;
import org.fenixedu.dynamicForms.DynamicForm;
import org.fenixedu.paperPusher.ist.RequestProcessors;
import org.fenixedu.smartFlow.domain.Flow;
import org.fenixedu.smartForms.domain.Request;
import org.fenixedu.smartForms.domain.RequestQueue;
import org.fenixedu.smartForms.domain.RequestType;
import org.fenixedu.smartForms.domain.SmartFormsLog;
import org.fenixedu.smartForms.domain.SmartFormsSystem;
import org.fenixedu.smartForms.domain.SmartFormsVisibility;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MigrateDiplomaSupplement extends CustomTask implements SmartFormsLog.SmartFormsLogger {

    private static final String FALLBACK_USER_FOR_LOGS = "fenix";

    @Override
    public void runTask() throws Exception {
        Stream.of("1979391512940162")
                .map(requestId -> (Request) FenixFramework.getDomainObject(requestId))
                .forEach(request -> {
                    final RegistryDiplomaRequest registryDiplomaRequest = FenixFramework.getDomainObject(
                            request.getData().get().getAsJsonObject().get("oldDocumentRequest").getAsString());
                    migrateDiplomaSupplement(registryDiplomaRequest, request, null, registryDiplomaRequest.getAcademicServiceRequestSituationType());
                });
    }

    private void migrateDiplomaSupplement(final AcademicServiceRequest documentRequest, final Request request, final Flow flow, final AcademicServiceRequestSituationType situationType) {
        if (documentRequest instanceof RegistryDiplomaRequest registryDiplomaRequest) {
            final DiplomaSupplementRequest diplomaSupplementRequest = registryDiplomaRequest.getDiplomaSupplement();
            if (diplomaSupplementRequest != null) {
                Request supplementRequest = null;
                if (flow == null && (situationType == AcademicServiceRequestSituationType.DELIVERED
                        || situationType == AcademicServiceRequestSituationType.CONCLUDED)) {
                    supplementRequest = createRequestFromRequest(JsonUtils.toJson(config -> {
                        config.addProperty("RequestType", "Suplemento ao Diploma");
                        config.addProperty("handle", "create-diploma-supplement-request");
                        config.add("mapper", JsonUtils.toJson(mapper -> {
                            mapper.addProperty("REGISTRATION", "REGISTRATION");
                            mapper.addProperty("PROGRAMME_CONCLUSION_TYPE", "PROGRAMME_CONCLUSION_TYPE");
                            mapper.addProperty("INCLUDE_COMPLEMENTARY_INFORMATION", "INCLUDE_COMPLEMENTARY_INFORMATION");
                            mapper.addProperty("IS_DIGITAL", "IS_DIPLOMA_SUPPLEMENT_DIGITAL");
                        }));
                    }), request);
                    if (supplementRequest != null) {
                        final JsonObject data = request.getData().get();
                        data.addProperty("requestId", supplementRequest.getExternalId());
                        request.setData(ImmutableJsonElement.of(data));
                    }
                } else if (flow != null) {
                    supplementRequest = flow.getAllActionProcessor()
                            .map(json -> (Request) JsonUtils.toDomainObject(json, "requestId"))
                            .filter(Objects::nonNull)
                            .filter(other -> other.getRequestTypeVersion().getRequestTypeFromCurrent().getName().anyMatch(s -> s.equals("Suplemento ao Diploma")))
                            .findAny().orElse(null);
                }
                if (supplementRequest != null) {
                    final GeneratedDocument generatedDocument = diplomaSupplementRequest.getLastGeneratedDocument();
                    if (generatedDocument != null) {
                        supplementRequest.setConcluded(false);
                        final Map<String, MultipartFile> map = new HashMap<>();
                        final byte[] content = generatedDocument.getContent();
                        RequestProcessors.prepareFilesMap(map, "0.1.DIPLOMA_SUPPLEMENT_PORTUGUESE",
                                "DIPLOMA_SUPPLEMENT_PORTUGUESE", "application/pdf", extractPT(content));
                        RequestProcessors.prepareFilesMap(map, "0.1.DIPLOMA_SUPPLEMENT_ENGLISH",
                                "DIPLOMA_SUPPLEMENT_ENGLISH", "application/pdf", extractEN(content));
                        final JsonObject data = supplementRequest.getData().get();
                        if (!map.isEmpty()) {
                            supplementRequest.updateOutcomeForm(data.getAsJsonObject("outcomeForm"), map, true, false);
                        }
                        data.addProperty("oldDocumentRequest", diplomaSupplementRequest.getExternalId());
                        supplementRequest.setData(ImmutableJsonElement.of(data));
                        final AcademicServiceRequestSituation activeSituation = diplomaSupplementRequest.getActiveSituation();
                        final AcademicServiceRequestSituationType suplementSituationType = activeSituation.getAcademicServiceRequestSituationType();
                        final boolean finished = suplementSituationType == AcademicServiceRequestSituationType.CONCLUDED
                                || suplementSituationType == AcademicServiceRequestSituationType.DELIVERED;
                        supplementRequest.setConcluded(finished);
                        supplementRequest.setAccepted(finished);
                        if (finished) {
                            supplementRequest.setLockInstant(documentRequest.getCreationDate());
                        }

                        if (suplementSituationType == AcademicServiceRequestSituationType.DELIVERED) {
                            markOutcomeDocumentDelivery(diplomaSupplementRequest, supplementRequest);
                        }
                    }

                    callChangeQueue(supplementRequest);
                    supplementRequest.setRequester(request.getRequester());
                    supplementRequest.setCreationInstant(diplomaSupplementRequest.getCreationDate());

                    transferLog(supplementRequest, "Criou o pedido", diplomaSupplementRequest.getCreationDate(), diplomaSupplementRequest.getPerson().getUser());
                    transferLog(supplementRequest, "Validou o pedido", diplomaSupplementRequest.getCreationDate(), diplomaSupplementRequest.getPerson().getUser());
                    transferLog(supplementRequest, "Submeteu e lacrou o pedido", registryDiplomaRequest.getCreationDate(), registryDiplomaRequest.getPerson().getUser());
                    supplementRequest.getSmartFormsLogSet().stream()
                            .filter(log -> log.getDescription().anyMatch(s -> s.startsWith("Editou o formulário de resposta"))
                                    || log.getDescription().anyMatch(s -> s.startsWith("Carregou o documento")))
                            .forEach(log -> log.delete());
                }
            }
        }
    }

    private void transferLog(final Request request, final String prefix, final DateTime when, final User user) {
        final SmartFormsLog log = find(request, prefix);
        if (log != null) {
            log.setWhen(when);
            Identity identity = user.getIdentity();
            if (identity == null && user.getAccount() != null) {
                identity = user.getAccount().getIdentity();
            }
            final Account account = ConnectSystem.getMostRelevantAccount(identity);
            log.setAccount(account);
        }
    }
    private Request createRequestFromRequest(final JsonObject config, final Request originalRequest) {
        final String requestTypeName = config.get("RequestType").getAsString();
        RequestQueue requestQueue = null;
        String requestQueueName = JsonUtils.get(config, "RequestQueue");
        if (requestQueueName == null) {
            if (originalRequest != null) {
                requestQueue = originalRequest.getRequestQueue();
            }
        } else {
            requestQueue = SmartFormsSystem.getInstance().findQueueByName(requestQueueName);
        }

        if (requestQueue != null) {
            final RequestType requestType = SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                    .filter(type -> type.getName().anyMatch(typeName -> typeName.equals(requestTypeName)))
                    .findAny().orElse(null);
            if (requestType != null) {
                final JsonObject data = originalRequest.getData().get();

                boolean isToRemoveQueue = false;
                if (!requestType.getRequestQueueSet().contains(requestQueue)) {
                    requestType.addRequestQueue(requestQueue);
                    isToRemoveQueue = true;
                }
                final Request request = Request.createRequest(requestType, requestQueue,
                        AccountService.getLoggedAccount());
                if (isToRemoveQueue) {
                    requestType.removeRequestQueue(requestQueue);
                }
                final JsonObject requestData = request.getData().get();
                requestData.addProperty("requestId", originalRequest.getExternalId());
                requestData.add("keywords", data.getAsJsonArray("keywords"));
                final JsonObject mapper = config.getAsJsonObject("mapper");
                if (mapper != null) {
                    final JsonObject inputForm = originalRequest.inputForm(true).toDataJson();
                    final JsonObject dataObject = originalRequest.getData().get();
                    final JsonObject inputFormData = dataObject.getAsJsonObject("inputForm");


                    final DynamicForm dynamicForm = new DynamicForm(inputForm)
                            .overwriteReadonly(true)
                            .withData(inputFormData);
                    final JsonObject requestInputForm = requestData.getAsJsonObject("inputForm");
                    final DynamicForm requestDynamicForm = new DynamicForm(
                            request.inputForm(true).toDataJson())
                            .withData(requestInputForm);
                    org.fenixedu.smartForms.integration.RequestProcessors.copyAndMap(mapper, dynamicForm, requestDynamicForm);
                    requestData.add("inputForm", requestDynamicForm.toDataJson());
                }
                request.setData(ImmutableJsonElement.of(requestData));

                return request;
            }
        }
        return null;
    }

    private void callChangeQueue(final Request request) {
        Request.REQUEST_PROCESSORS.get("Change Academic Request Queue").accept(new JsonObject(), request);
    }

    private AcademicServiceRequestSituation find(final AcademicServiceRequest documentRequest, final AcademicServiceRequestSituationType type) {
        return documentRequest.getAcademicServiceRequestSituationsSet().stream()
                .filter(situation -> situation.getAcademicServiceRequestSituationType() == type)
                .max(Comparator.comparing(AcademicServiceRequestSituation::getSituationDate))
                .orElse(null);
    }

    private SmartFormsLog find(final Request request, final String prefix) {
        return request.getSmartFormsLogSet().stream()
                .filter(log -> log.getDescription().anyMatch(s -> s.startsWith(prefix)))
                .max(Comparator.comparing(SmartFormsLog::getWhen))
                .orElse(null);
    }

    private void markOutcomeDocumentDelivery(final AcademicServiceRequest documentRequest, final Request request) {
        request.getOutcomeDocumentSet().forEach(requestDocument -> {
            final JsonObject documentData = requestDocument.getData().get();
            final AcademicServiceRequestSituation deliverySituation = find(documentRequest, AcademicServiceRequestSituationType.DELIVERED);
            if (deliverySituation == null) {
                throw new Error("Delivery situation not found for " + documentRequest.getExternalId());
            }
            final SmartFormsLog smartFormsLog = log(SmartFormsVisibility.REQUESTER, request, "log.deliver.physical.outcome.document",
                    requestDocument.getDisplayName());
            smartFormsLog.setAccount(accountFor(deliverySituation.getCreator(), FALLBACK_USER_FOR_LOGS));
            smartFormsLog.setWhen(deliverySituation.getSituationDate());
            documentData.addProperty("deliveryDate", deliverySituation.getSituationDate().toString(ISODateTimeFormat.dateTime()));
            requestDocument.setData(ImmutableJsonElement.of(documentData));
        });
    }

    private Account accountFor(final Person person, final String fallback) {
        final User user = person.getUser();
        return user == null && fallback == null ? null : accountFor(user == null ? User.findByUsername(fallback) : user);
    }

    private Account accountFor(final User user) {
        Identity identity = user.getIdentity();
        if (identity == null && user.getAccount() != null) {
            identity = user.getAccount().getIdentity();
        }
        final Account account = ConnectSystem.getMostRelevantAccount(identity);
        if (account == null) {
            throw new NullPointerException();
        }
        return account;
    }
    private byte[] extractPT(byte[] content) {
        final boolean[] copy = {true};
        return splitPdf(content, pdfPage -> copy[0] = copy[0]
                && PdfTextExtractor.getTextFromPage(pdfPage, new SimpleTextExtractionStrategy()).indexOf("DIPLOMA SUPPLEMENT") < 0);
    }

    private byte[] extractEN(byte[] content) {
        final boolean[] copy = {false};
        return splitPdf(content, pdfPage -> copy[0] = copy[0]
                || PdfTextExtractor.getTextFromPage(pdfPage, new SimpleTextExtractionStrategy()).indexOf("DIPLOMA SUPPLEMENT") >= 0);
    }

    private byte[] splitPdf(final byte[] content, final Predicate<PdfPage> copyPrediacte) {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (final PdfReader pdfReader = new PdfReader(inputStream);
             final PdfDocument pdfDocumentIn = new PdfDocument(pdfReader);
             final PdfWriter pdfWriter = new PdfWriter(outputStream);
             final PdfDocument pdfDocumentOut = new PdfDocument(pdfWriter)) {
            for (int i = 1; i <= pdfDocumentIn.getNumberOfPages(); i++) {
                final PdfPage pdfPageIn = pdfDocumentIn.getPage(i);
                if (copyPrediacte.test(pdfPageIn)) {
                    final PdfPage pdfPageOut = pdfPageIn.copyTo(pdfDocumentOut);
                    pdfDocumentOut.addPage(pdfPageOut);
                }
            }
        } catch (final IOException e) {
            throw new Error(e);
        }
        return outputStream.toByteArray();
    }
}
