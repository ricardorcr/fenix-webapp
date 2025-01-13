package pt.ist.fenix.webapp;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.sap.client.SapFinantialClient;

public class SendXMLRequestToSap extends SapCustomTask {

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger eventLogger) {
        final File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/NP1373900_estorno.xml");
//        final File file = new File("/home/rcro/DocumentsHDD/fenix/sap/NP590985.xml");
        String fileContent = null;
        try {
            fileContent = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonObject data = new JsonObject();
        data.addProperty("finantialInstitution","IST");

        byte[] bytes = null;
        try {
            bytes = fileContent.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JsonObject result = null;
        try {
            result = SapFinantialClient.sendInfoOnline(bytes, data);
            taskLog(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            result = new JsonObject();
            result.addProperty("exception", responseFromException(e));
            taskLog(responseFromException(e));
        }

        final SapRequest sr = FenixFramework.getDomainObject("2260033266017151");
        SapEvent sapEvent = new SapEvent(sr.getEvent());
        if (!sapEvent.checkAndRegisterIntegration(sr.getEvent(), errorLogConsumer, eventLogger, data, sr.getDocumentNumber(), sr,
                result, sr.getRequestType().toString(), sr.getRequestType().isToGetDocument())) {
            return;
        }

        final SapRequest originalRequest = sr.getOriginalRequest();
        if (originalRequest != null) {
            originalRequest.setIgnore(true);
            // If it was cancelled it means that the refund is going to be cancelled and the object will be deleted
            // and it needs this relation to be free.
            // If a new request is generated for the same refund object this relation is no longer needed as well
            originalRequest.setRefund(null);
        }
    }

    private String responseFromException(final Throwable t) {
        final Throwable cause = t.getCause();
        final String message = t.getMessage();
        return cause == null ? message : message + '\n' + responseFromException(cause);
    }
}
