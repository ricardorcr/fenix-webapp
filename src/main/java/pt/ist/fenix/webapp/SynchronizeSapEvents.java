package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SynchronizeSapEvents extends SapCustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        List<String> eventIDs = null;
		try {
            eventIDs = Files.readAllLines(
					new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/events_imputacoes_internas.txt").toPath());
		} catch (IOException e) {
			throw new Error("Erro a ler o ficheiro.");
		}

        Set<String> ids = new HashSet<>(eventIDs);
        for (String eventId: ids) {
            Event event = FenixFramework.getDomainObject(eventId);
            SapEvent sapEvent = new SapEvent(event);
            try {
                FenixFramework.atomic(() -> sapEvent.processPendingRequests(event, errorLogConsumer, elogger));
            } catch (Exception e) {
                taskLog("Error processing event: %s %s%n", event.getExternalId(), e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
