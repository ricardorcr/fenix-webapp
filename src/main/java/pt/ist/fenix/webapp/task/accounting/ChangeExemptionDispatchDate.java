package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.events.EventExemptionJustification;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class ChangeExemptionDispatchDate extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final EventExemptionJustification justification = FenixFramework.getDomainObject("3104509735731551");

        new EventExemptionJustification(justification.getExemption(), justification.getJustificationType(), new LocalDate(2024,01,02),
                justification.getReason() + " - Data de Despacho modificada de 29-12-2023 para 02-01-2024 pois foi inserida depois do ano fiscal de 2023 já ter sido fechado em SAP");

        justification.delete();

        final SapRequest creditDebt = FenixFramework.getDomainObject("3104458196131566");
        creditDebt.setRequest(creditDebt.getRequest().replace("2023-12-29", "2024-01-02"));
    }
}
