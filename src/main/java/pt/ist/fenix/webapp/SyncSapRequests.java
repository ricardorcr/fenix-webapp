package pt.ist.fenix.webapp;

import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;

public class SyncSapRequests extends SapCustomTask {

    int count = 0;

//    private static void logError(final ErrorLogConsumer errorLog, final EventLogger elogger, final SapRequest sapRequest, final Throwable e) {
//        final String errorMessage = e.getMessage();
//
//        BigDecimal amount;
//        DebtCycleType cycleType;
//
//        try {
//            amount = sapRequest.getEvent().getOriginalAmountToPay().getAmount();
//            cycleType = Utils.cycleType(sapRequest.getEvent());
//        } catch (Exception ex) {
//            amount = null;
//            cycleType = null;
//        }
//
//        errorLog.accept(sapRequest.getEvent().getExternalId(), Utils.getUserIdentifier(sapRequest.getEvent().getParty()), sapRequest.getEvent().getParty().getName(),
//                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), errorMessage,
//                "", "", "", "", "", "", "", "", "", "", "");
//        elogger.log("%s: %s%n", sapRequest.getEvent().getExternalId(), errorMessage);
//        elogger.log(
//                "Unhandled error for event " + sapRequest.getEvent().getExternalId() + " : " + e.getClass().getName() + " - " + errorMessage);
//        e.printStackTrace();
//    }
//
//    @Override
//    public TxMode getTxMode() {
//        return TxMode.READ;
//    }
//
    @Override
    protected void runTask(final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
//        count = 0;
//        Bennu.getInstance().getAccountingEventsSet().stream().parallel().forEach(e -> process(e, errorLogConsumer, elogger));
//        taskLog(String.valueOf(count));
    }
//
//    private void process(final Event e, final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
//        try {
//            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {
//
//                @Override
//                public Void call() {
//                    e.getSapRequestSet().stream()
//                            .filter(sr -> !sr.getIntegrated() && isToSend(sr))
//                            .sorted(SapRequest.COMPARATOR_BY_EVENT_AND_ORDER)
//                            .forEach(sr -> syncRequestWithSap(sr, errorLogConsumer, elogger));
//                    return null;
//                }
//            }, new AtomicInstance(TxMode.READ, false));
//        } catch (Exception ex) {
//            throw new Error("Bad things happen", ex);
//        }
//    }
//
//    private boolean isToSend(final SapRequest sr) {
//        JsonObject jsonRequest = sr.getRequestAsJson();
//        if (jsonRequest.has("workingDocument")) {
//            return jsonRequest.getAsJsonObject("workingDocument")
//                    .get("documentDate").getAsString().contains("2018");
//        } else if (jsonRequest.has("paymentDocument")) {
//            return jsonRequest.getAsJsonObject("paymentDocument")
//                    .get("paymentDate").getAsString().contains("2018");
//        }
//        return false;
//    }
//
//    public void syncRequestWithSap(final SapRequest sapRequest, final ErrorLogConsumer errorLog, final EventLogger elogger) {
//        try {
//            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {
//
//                @Override
//                public Void call() {
//                    sendRequest(sapRequest, errorLog, elogger);
//                    count++;
//                    return null;
//                }
//            }, new AtomicInstance(TxMode.SPECULATIVE_READ, false));
//        } catch (Exception e) {
//            logError(errorLog, elogger, sapRequest, e);
//            e.printStackTrace();
//        }
//    }
//
//    private void sendRequest(final SapRequest sr, final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
//        try {
//            if (!sr.getIntegrated()) {
//                JsonParser jsonParser = new JsonParser();
//                JsonObject data = (JsonObject) jsonParser.parse(sr.getRequest());
//
//                JsonObject result = sendDataToSap(sr, data);
//
//                boolean isIntegrated = checkAndRegisterIntegration(sr.getEvent(), errorLogConsumer, elogger, data, sr.getDocumentNumber(), sr,
//                        result, sr.getRequestType().toString(), sr.getRequestType().isToGetDocument());
//                if (!isIntegrated) {
//                    return;
//                }
//
//                final SapRequest originalRequest = sr.getOriginalRequest();
//                if (originalRequest != null) {
//                    originalRequest.setIgnore(true);
//                }
//            }
//        } catch (Exception e) {
//            logError(errorLogConsumer, elogger, sr, e);
//        }
//    }
//
//    private boolean checkAndRegisterIntegration(Event event, ErrorLogConsumer errorLog, EventLogger elogger, JsonObject data,
//                                                String documentNumber, SapRequest sapRequest, JsonObject result, String action, boolean getDocument) {
//        if (result.get("exception") == null) {
//            boolean docIsIntegrated = checkDocumentsStatus(result, sapRequest, event, errorLog, elogger, action);
//            boolean clientStatus = checkClientStatus(result, event, errorLog, elogger, action, data, sapRequest);
//            if (docIsIntegrated) {
//                String sapDocumentNumber = getSapDocumentNumber(result, documentNumber);
//
//                if (getDocument) {
//                    JsonObject docResult =
//                            SapFinantialClient.getDocument(sapDocumentNumber, data.get("taxRegistrationNumber").getAsString());
//                    if (docResult.get("status").getAsString().equalsIgnoreCase("S")) {
//                        sapRequest.setSapDocumentFile(new SapDocumentFile(sanitize(sapDocumentNumber) + ".pdf",
//                                Base64.getDecoder().decode(docResult.get("documentBase64").getAsString())));
//                    }
//                }
//
//                sapRequest.setSapDocumentNumber(sapDocumentNumber);
//                sapRequest.setIntegrated(true);
//                if (clientStatus) {
//                    sapRequest.setIntegrationMessage("{}");
//                } else {
//                    //there are still error messages regarding the client
//                    sapRequest.removeIntegrationMessage("Documento");
//                }
//                return true;
//            } else {
//                return false;
//            }
//        } else {
//            logError(event, errorLog, elogger, result.get("exception").getAsString(), documentNumber, action, sapRequest);
//            return false;
//        }
//    }
//
//    private boolean checkDocumentsStatus(JsonObject result, SapRequest sapRequest, Event event, ErrorLogConsumer errorLog,
//                                         EventLogger elogger, String action) {
//        JsonArray jsonArray = result.getAsJsonArray("documents");
//        boolean checkStatus = true;
//        for (int iter = 0; iter < jsonArray.size(); iter++) {
//            JsonObject json = jsonArray.get(iter).getAsJsonObject();
//            if (!"S".equals(json.get("status").getAsString())) {
//                checkStatus = false;
//                String errorMessage = json.get("errorDescription").getAsString();
//                logError(event, errorLog, elogger, errorMessage, json.get("documentNumber").getAsString(), action, sapRequest);
//            }
//        }
//        return checkStatus;
//    }
//
//    private boolean checkClientStatus(JsonObject result, Event event, ErrorLogConsumer errorLog, EventLogger elogger,
//                                      String action, JsonObject sentData, SapRequest sr) {
//        JsonArray jsonArray = result.getAsJsonArray("customers");
//        for (int iter = 0; iter < jsonArray.size(); iter++) {
//            JsonObject json = jsonArray.get(iter).getAsJsonObject();
//            if (!"S".equals(json.get("status").getAsString())) {
//                logError(event, json.get("customerId").getAsString(), errorLog, elogger, json.get("returnMessage").getAsString(),
//                        action, sentData, sr);
//                return false;
//            }
//        }
//        return true;
//    }
//
//    private String getSapDocumentNumber(JsonObject result, String docNumber) {
//        JsonArray jsonArray = result.getAsJsonArray("documents");
//        for (int iter = 0; iter < jsonArray.size(); iter++) {
//            JsonObject json = jsonArray.get(iter).getAsJsonObject();
//            if (json.get("documentNumber").getAsString().equals(docNumber) && "S".equals(json.get("status").getAsString())) {
//                return json.get("sapDocumentNumber").getAsString();
//            }
//        }
//        return null;
//    }
//
//    private String sanitize(final String s) {
//        return s.replace('/', '_').replace('\\', '_');
//    }
//
//    /**
//     * Sends the data to SAP
//     *
//     * @param sapRequest - the domain representation of the request
//     * @param data       - the necessary data to invoke the service for the specified operation
//     * @return The result of the SAP service invocation, with the status of the documents and clients and also the xml request
//     * sent. In case of an unexpected exception returns the exception message
//     */
//    private JsonObject sendDataToSap(SapRequest sapRequest, JsonObject data) {
//        JsonObject result = null;
//        sapRequest.setWhenSent(new DateTime());
//        sapRequest.setSent(true);
//        try {
//            result = SapFinantialClient.comunicate(data);
//        } catch (Exception e) {
//            e.printStackTrace();
//            result = new JsonObject();
//            result.addProperty("exception", responseFromException(e));
//            return result;
//        }
//        return result;
//    }
//
//    private String responseFromException(final Throwable t) {
//        final Throwable cause = t.getCause();
//        final String message = t.getMessage();
//        return cause == null ? message : message + '\n' + responseFromException(cause);
//    }
//
//    private void logError(Event event, String clientId, ErrorLogConsumer errorLog, EventLogger elogger, String returnMessage,
//                          String action, JsonObject sentData, SapRequest sr) {
//        final Party party = event.getParty();
//        errorLog.accept(event.getExternalId(), clientId, party.getName(), "", "", returnMessage, "", "",
//                sentData.get("clientData").getAsJsonObject().get("fiscalCountry").getAsString(), clientId,
//                sentData.get("clientData").getAsJsonObject().get("street").getAsString(), "",
//                sentData.get("clientData").getAsJsonObject().get("postalCode").getAsString(), "", "", "", action);
//        elogger.log("Pessoa %s (%s): evento: %s %s %s %s %n", party.getExternalId(), Utils.getUserIdentifier(party),
//                event.getExternalId(), clientId, returnMessage, action);
//
//        //Write to SapRequest in json format
//        JsonObject errorMessage = new JsonObject();
//        errorMessage.addProperty("ID Evento", event.getExternalId());
//        errorMessage.addProperty("Utilizador", Utils.getUserIdentifier(party));
//        errorMessage.addProperty("Nº Contribuinte", clientId);
//        errorMessage.addProperty("Nome", party.getName());
//        errorMessage.addProperty("Mensagem", returnMessage);
//        errorMessage.addProperty("País Fiscal", sentData.get("clientData").getAsJsonObject().get("fiscalCountry").getAsString());
//        errorMessage.addProperty("Morada", sentData.get("clientData").getAsJsonObject().get("street").getAsString());
//        errorMessage.addProperty("Código Postal", sentData.get("clientData").getAsJsonObject().get("postalCode").getAsString());
//        errorMessage.addProperty("Tipo Documento", action);
//
//        sr.addIntegrationMessage("Cliente", errorMessage);
//    }
//
//    private void logError(Event event, ErrorLogConsumer errorLog, EventLogger elogger, String errorMessage, String documentNumber,
//                          String action, SapRequest sr) {
//        BigDecimal amount = null;
//        DebtCycleType cycleType = Utils.cycleType(event);
//        final Party party = event.getParty();
//
//        errorLog.accept(event.getExternalId(), Utils.getUserIdentifier(party), party.getName(),
//                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), errorMessage,
//                "", "", "", "", "", "", "", "", "", documentNumber, action);
//        elogger.log("%s: %s %s %s %n", event.getExternalId(), errorMessage, documentNumber, action);
//
//        //Write to SapRequest in json format
//        JsonObject returnMessage = new JsonObject();
//        returnMessage.addProperty("ID Evento", event.getExternalId());
//        returnMessage.addProperty("Utilizador", Utils.getUserIdentifier(party));
//        returnMessage.addProperty("Nome", party.getName());
//        returnMessage.addProperty("Ciclo", cycleType != null ? cycleType.getDescription() : "");
//        returnMessage.addProperty("Mensagem", errorMessage);
//        returnMessage.addProperty("Nº Documento", documentNumber);
//        returnMessage.addProperty("Tipo Documento", action);
//
//        sr.addIntegrationMessage("Documento", returnMessage);
//    }

}
