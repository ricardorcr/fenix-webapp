package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.smartForms.domain.Request;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

public class CancelNotPayedRequestsFromAcademicRequestsMigration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final List<String> toCancel = Arrays.asList("1971158060630028","1971609032196101","1971158060630022","286641822370975","849591775792690","286641822370962",
                "286641822370961","3382866566185078","1131066752502915","1131066752502914","2538441636053121","2256966659342631","2256966659342613","1412541729212471",
                "2538441636053106","1412541729212467","1412541729212466","2256966659342605","1694016705924349","286641822370899","286641822370893","286641822370892",
                "1694016705924312","1694016705924298","1694016705924293","1694016705924271","1694016705924247","1412541729212425","1412541729212424","1975491682635010",
                "1131066752502846","286641822370792","286641822370782","286641822370781","2819916612763859","2819916612763755","1694016705924109","286641822370568",
                "1131066752502449","1131066752502448","1131066752502198","1131066752502197","568116799081995","1694016705923674","1131066752501600","1694016705922949",
                "568116799080668");

        final Person responsible = User.findByUsername("ist24616").getPerson();
        final Spreadsheet spreadsheet = new Spreadsheet("Eventos e pedidos cancelados");
        toCancel.stream()
                .map(eventID -> (Event) FenixFramework.getDomainObject(eventID))
                .filter(event -> event.getRequestCost() != null)
                .forEach(event -> {
                    final Request request = event.getRequestCost().getRequest();
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Aluno", event.getPerson().getUsername());
                    row.setCell("Evento", event.getExternalId());
                    row.setCell("Data", event.getWhenOccured().toString("dd/MM/yyyy HH:mm:ss"));
                    row.setCell("Pedido", request.getExternalId());
                    row.setCell("Descrição", event.getDescription().toLocalizedString().getContent());
                    request.reject("Falta de pagamento");
                    try {
                        event.cancel(responsible, "Pedido académico rejeitado por falta de pagamento");
                    } catch (Exception e) {
                        taskLog(event.getExternalId());
                        throw e;
                    }
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSXSheet(baos);
        output("eventos_pedidos_cancelados.xlsx", baos.toByteArray());
    }
}
