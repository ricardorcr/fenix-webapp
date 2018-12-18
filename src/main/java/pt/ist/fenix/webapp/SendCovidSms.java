package pt.ist.fenix.webapp;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.messaging.smsdispatch.SMSMessage;

public class SendCovidSms extends CustomTask {

    @Override
    public void runTask() throws Exception {
        List<String> allLines = Files.readAllLines(new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/mensagens_covid.txt").toPath());
        for (String line : allLines) {
            String[] parts = line.split("\t");
            String number = parts[0].trim();
            String message = parts[1];
            if (!SMSMessage.getInstance().sendSMS(number, message)){
                taskLog("Mensagem NÃO enviada para: %s%n", number);
            }
        }      
    }
}
