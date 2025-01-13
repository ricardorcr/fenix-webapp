package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.nio.file.Files;

public class FixEventCreatedAfterYear extends CustomTask {

    final String PATH = "/home/rcro/Documents/fenix/sap/";
    @Override
    public void runTask() throws Exception {
        final Event event = FenixFramework.getDomainObject("1697456974727432");

        final String debtJson = Files.readString(new File(PATH + "debt_ist193221.json").toPath());
        Long documentNumber = SapRoot.getInstance().getAndSetNextDocumentNumber();
        new SapRequest(event, "PT276048008", new Money(412.5), "NG" + documentNumber,
                SapRequestType.DEBT, Money.ZERO, JsonParser.parseString(debtJson).getAsJsonObject());

        final String invoiceJson = Files.readString(new File(PATH + "invoice_ist193221.json").toPath());
        documentNumber = SapRoot.getInstance().getAndSetNextDocumentNumber();
        new SapRequest(event, "PT276048008", new Money(412.5), "ND" + documentNumber,
                SapRequestType.INVOICE, Money.ZERO, JsonParser.parseString(invoiceJson).getAsJsonObject());
    }
}
