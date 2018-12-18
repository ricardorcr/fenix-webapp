package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class CopySapRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {
        //SapRequest requestToCopy = FenixFramework.getDomainObject("289708429017602");//TAXAS DE MATRICULA ist167012
        //SapRequest requestToCopy = FenixFramework.getDomainObject("289708429022272");//SEGURO ESCOLAR ist186222
        //SapRequest requestToCopy = FenixFramework.getDomainObject("289708429018934");//PROPINA ist154821
//        SapRequest requestToCopy = FenixFramework.getDomainObject("289708429034030");//Divida PROPINA ist192310        
//        JsonObject jsonToCopy = new JsonParser().parse(requestToCopy.getRequest()).getAsJsonObject();
//        JsonObject workDocument = jsonToCopy.get("workingDocument").getAsJsonObject();
//        //workDocument.addProperty("documentDate", new DateTime(2017, 12, 13, 23, 59, 00).toString("yyyy-MM-dd HH:mm:ss"));
//        String documentNumber = "NG" + SapRoot.getInstance().getAndSetNextDocumentNumber();
//        workDocument.addProperty("workingDocumentNumber", documentNumber);
//
//        SapRequest sapRequestCopied =
//                new SapRequest(requestToCopy.getEvent(), requestToCopy.getClientId(), requestToCopy.getValue(), documentNumber,
//                        requestToCopy.getRequestType(), requestToCopy.getAdvancement(), jsonToCopy);
//        //sapRequestCopied.setIgnore(true);
//
//        sendRequestToSap(sapRequestCopied);
//    }
//
//    private void sendRequestToSap(SapRequest sapRequestCopied) {
//        JsonObject result = sendDataToSap(sapRequestCopied, (JsonObject) new JsonParser().parse(sapRequestCopied.getRequest()));
//
//        taskLog("A resposta foi: %s\n", result.toString());
//
//        boolean isIntegrated = checkAndRegisterIntegration("", sapRequestCopied, result, false);
//        taskLog("Resultado da integração: %s\n", isIntegrated);
//        if (!isIntegrated) {
//            final SapRequest originalRequest = sapRequestCopied.getOriginalRequest();
//            if (originalRequest != null) {
//                originalRequest.setIgnore(true);
//            }
//        }
//    }
//
//    private boolean checkAndRegisterIntegration(String tecnicoNIF, SapRequest sapRequest, JsonObject result,
//            boolean getDocument) {
//        if (result.get("exception") == null) {
//            boolean docIsIntregrated = checkDocumentsStatus(result);
//            boolean clientStatus = checkClientStatus(result);
//            if (docIsIntregrated) {
//                String sapDocumentNumber = getSapDocumentNumber(result, sapRequest.getDocumentNumber());
//                taskLog("O nº de documento é: %s\n", sapDocumentNumber);
//
//                if (getDocument) {
//                    JsonObject docResult = SapFinantialClient.getDocument(sapDocumentNumber, tecnicoNIF);
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
//            taskLog("Deu excepção!! %s\n", result.get("exception").getAsString());
//            return false;
//        }
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
//    private boolean checkClientStatus(JsonObject result) {
//        JsonArray jsonArray = result.getAsJsonArray("customers");
//        for (int iter = 0; iter < jsonArray.size(); iter++) {
//            JsonObject json = jsonArray.get(iter).getAsJsonObject();
//            if (!"S".equals(json.get("status").getAsString())) {
//                taskLog("Deu erro no cliente: %s\n", json.toString());
//                return false;
//            }
//        }
//        return true;
//    }
//
//    private boolean checkDocumentsStatus(JsonObject result) {
//        JsonArray jsonArray = result.getAsJsonArray("documents");
//        boolean checkStatus = true;
//        for (int iter = 0; iter < jsonArray.size(); iter++) {
//            JsonObject json = jsonArray.get(iter).getAsJsonObject();
//            if (!"S".equals(json.get("status").getAsString())) {
//                checkStatus = false;
//                taskLog("Deu erro no documento: %s\n", json.toString());
//            }
//        }
//        return checkStatus;
//    }
//
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
//    private String sanitize(final String s) {
//        return s.replace('/', '_').replace('\\', '_');
    }
}
