package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class ShowLogs extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess process =  FenixFramework.getDomainObject("3104707304226830");
        process.getLogSet().stream()
                .filter(log -> log.getApplication() == null)
//                .filter(log -> !log.getDescription().getContent().contains("Calculou"))
//                .filter(log -> !log.getDescription().getContent().contains("Adicionou"))
//                .filter(log -> !log.getDescription().getContent().contains("Exportou"))
//                .filter(log -> !log.getDescription().getContent().contains("Criou o âmbito"))
//                .filter(log -> !log.getDescription().getContent().contains("Alterou o número de vagas"))
//                .filter(log -> !log.getDescription().getContent().contains("Definiu o template de email"))
                .filter(log -> log.getWhen().getDayOfMonth() == 30)
                .forEach(log -> taskLog("%s\t%s\t%s%n", log.getExternalId(), log.getDescription().getContent(), log.getWhen()));
    }
}
