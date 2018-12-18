package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerateFinalPaymentThatHasAdvancement extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/advancements_with_final_payment.csv");
        Files.readAllLines(file.toPath()).stream()
                .forEach(this::processLine);
    }

    private void processLine(final String line) {
        final LocalDate today = new LocalDate();
        String[] lineSplit = line.split("\t");
        String eventID = lineSplit[0];
        String advancementNumber = lineSplit[1];

        if (eventID.startsWith("#")) {
            taskLog("Evento %s não existe SapRequest com este nº %s%n", eventID, advancementNumber);
            return;
        }
        Event event = FenixFramework.getDomainObject(eventID);
        SapRequest advancement = event.getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equals(advancementNumber))
                .findAny().orElseThrow(() -> new Error("Isto não era suposto acontecer! " + eventID + " - " + advancementNumber));
//        advancement.setEvent(null);

        boolean hasInterest = advancement.getPayment().getSapRequestSet().stream()
                .anyMatch(sr -> sr.getRequestType() == SapRequestType.INVOICE_INTEREST);

        if (hasInterest) {
            Set<SapRequest> original =
                    advancement.getPayment().getSapRequestSet().stream().peek(sr -> sr.setEvent(null)).collect(Collectors.toSet());
            EventProcessor.calculate(() -> event);
            event.getSapRequestSet().stream()
                    .filter(sr -> !sr.getSent())
                    .filter((sr -> sr.getWhenCreated().toLocalDate().equals(today)))
                    .filter(sr -> sr.getRequestType().equals(SapRequestType.ADVANCEMENT) || sr.getRequestType().equals(SapRequestType.INVOICE_INTEREST)
                            || sr.getRequestType().equals(SapRequestType.PAYMENT_INTEREST))
                    .filter(sr -> sr.getPayment() == advancement.getPayment())
                    .forEach(sr -> sr.delete());

            original.forEach(sr -> sr.setEvent(event));
        }
    }
}
