package pt.ist.fenix.webapp;

import java.util.Arrays;
import java.util.List;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ResendSapDocuments extends SapCustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        final StringBuilder errors = new StringBuilder();
        List<String> documentNumbers = null;
//		try {
//			documentNumbers = Files.readAllLines(
//					new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/reenvio_documentos_lote2_15_11_2020_NAs.txt").toPath());
        documentNumbers = Arrays.asList("NP468793");
//		} catch (IOException e) {
//			throw new Error("Erro a ler o ficheiro.");
//		}
        for (String documentNumber : documentNumbers) {
//        final String documentNumber = "NP456194";
            try {
//            	taskLog("Processing: %s%n", documentNumber);
                resend(documentNumber, errorLogConsumer, elogger);
            } catch (Throwable t) {
                t.printStackTrace();
                taskLog(t.getMessage() + " - " + documentNumber);
                errors.append(t.getMessage()).append(" - ").append(documentNumber).append("\n");
            }
        }
        output("erros_processar_documentos.csv", errors.toString().getBytes());
    }

    private void resend(final String documentNumber, final ErrorLogConsumer errorLogConsumer,
                        final EventLogger elogger) {
        FenixFramework.atomic(() -> resendTx(documentNumber, errorLogConsumer, elogger));
    }

    private boolean wasAlreadySent(SapRequest sr) {
        return sr.getWhenSent().getYear() == 2020 && sr.getWhenSent().getMonthOfYear() == 3
                && sr.getWhenSent().getDayOfMonth() == 26 && sr.getWhenSent().getHourOfDay() == 15;
    }

    private void resendTx(final String documentNumber, final ErrorLogConsumer errorLogConsumer,
                          final EventLogger elogger) {
        final SapRequest sapRequest = SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equals(documentNumber))
                .peek(sr -> {
                    if (sr.getAnulledRequest() != null || sr.getOriginalRequest() != null) {
                        throw new Error("Document " + documentNumber + " is annulled for event " + sr.getEvent().getExternalId());
                    }
                })
                .peek(sr -> {
                    if (sr.getIgnore() && sr.getAnulledRequest() == null && sr.getOriginalRequest() == null) {
                        throw new Error("Este documento não devia estar a ser enviado: " + sr.getDocumentNumber() + " Evento: " + sr.getEvent().getExternalId());
                    }
                })
                .peek(sr -> {
                    if (sr.getEvent().getSapRequestSet().stream().filter(osr -> osr != sr)
                            .filter(osr -> osr.getIntegrated())
                            .filter(osr -> osr.getRequestType() != SapRequestType.REIMBURSEMENT)
                            .anyMatch(osr -> osr.refersToDocument(documentNumber))) {
                        throw new Error("Document " + documentNumber + " is refered by other integrated documents");
                    }
                }).findAny().orElseThrow(() -> new Error("Document " + documentNumber + " not found."));

        //this is in the case some of the requests of the file were sent and integrated but others didn't
        //and due to the large data of the file we don't want to resend the ones already sent and integrated
//		if (wasAlreadySent(sapRequest)) {
//			return;
//		}
//		taskLog("Going to send: %s%n", documentNumber);
        final SapEvent sapEvent = new SapEvent(sapRequest.getEvent());
        sapRequest.setIntegrated(false);
        if (sapRequest.getSapDocumentFile() != null) {
            sapRequest.getSapDocumentFile().delete();
        }
//		taskLog("Sending: %s%n", documentNumber);
//		Integer openYear = SapRoot.getInstance().getOpenYear();
//		try {
//			SapRoot.getInstance().setOpenYear(sapRequest.getDocumentDate().getYear());
        sapEvent.processPendingRequests(sapRequest, errorLogConsumer, elogger);
//		} finally {
//			SapRoot.getInstance().setOpenYear(openYear);
//		}
    }
}