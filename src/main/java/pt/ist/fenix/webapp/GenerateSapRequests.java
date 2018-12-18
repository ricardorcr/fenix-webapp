package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.Bennu;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GenerateSapRequests extends SapCustomTask {


    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
//        List<String> eventIDs = null;
//        try {
//            eventIDs = Files.readAllLines(
//                    new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/events_imputacoes_internas.txt").toPath());
//        } catch (IOException e) {
//            throw new Error("Erro a ler o ficheiro.");
//        }
//        Set<String> ids = new HashSet<>(eventIDs);
//        for (String eventId: ids) {
//            Event event = FenixFramework.getDomainObject(eventId);
//            EventProcessor.registerEventSapRequests(errorLogConsumer,elogger, event, false);
//        }

        Bennu.getInstance().getAccountingEventsSet().stream().parallel()
                .forEach(e -> EventProcessor.registerEventSapRequests(errorLogConsumer, elogger, e, false));
    }
}
