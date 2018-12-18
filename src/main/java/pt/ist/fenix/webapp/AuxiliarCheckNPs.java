package pt.ist.fenix.webapp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.io.domain.GroupBasedFile;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AuxiliarCheckNPs extends CustomTask {

    @Override
    public void runTask() throws Exception {


        final Map<String, Money> npsSAP = new HashMap<>();
//        GroupBasedFile file = FenixFramework.getDomainObject("563568428782572"); //NPs que são de NAs
//        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(file.getContent())));
        BufferedReader readerSAP = new BufferedReader(new FileReader("/home/rcro/DocumentsSSD/fenix/sap/SAP_NPs_com_valor.txt"));
        String np = readerSAP.readLine();
        StringBuilder sb = new StringBuilder();
        try {
            while (np != null) {
                String[] line = np.split("\t");
                //if (!line[0].endsWith("_")) {
                //if (line[2].equalsIgnoreCase("ID")) {
//                    if (npsSAP.get(line[0]) != null) {
//                        if (!npsSAP.get(line[0]).abs().equals(new Money(line[1]).abs())) {
//                            taskLog("Afinal temos repetidos com valores diferentes: %s %s %s%n", line[0], npsSAP.get(line[0]), line[1]);
//                        }
//                    }
                    npsSAP.merge(line[0], new Money(line[1]), (value1, value2) -> value1.add(value2));
//                }
                //}
                np = readerSAP.readLine();
            }
        } finally {
            readerSAP.close();
        }


        final Map<String, Money> npsFenix = new HashMap<>();
        BufferedReader readerFenix = new BufferedReader(new FileReader("/home/rcro/DocumentsSSD/fenix/sap/NPsFenixComValor.txt"));
        np = readerFenix.readLine();
        try {
            while (np != null) {
                String[] line = np.split("\t");
                npsFenix.put(line[0], new Money(line[1]).add(new Money(line[2])));
                np = readerFenix.readLine();
            }
        } finally {
            readerFenix.close();
        }


        npsSAP.keySet().stream().forEach(npSap ->
        {
            Money npFenixValue = npsFenix.get(npSap);
            Money npSAPValue = npsSAP.get(npSap);

//            if (!npSAPValue.isZero()) {
//                taskLog("%s %s%n", npSap, npSAPValue);
//            }

            if (npFenixValue == null) {
                if (!npSAPValue.isZero() && !npSap.endsWith("_")) {
                    taskLog("Nas contas do Fénix este NP não entrou: %s%n", npSap);
                }
            } else {
                if (!npFenixValue.equals(npSAPValue.abs())) {
                    taskLog("O NP: %s tem valores diferentes: Fénix: %s - SAP: %s%n", npSap, npFenixValue, npSAPValue);
                }
            }
        });

        Money fenixValue = npsFenix.values().stream().reduce(Money.ZERO, Money::add);
        taskLog("Fénix value: %s%n", fenixValue);

        Money sapValue = npsSAP.values().stream().reduce(Money.ZERO, Money::add);
        taskLog("Sap value: %s%n", sapValue);
    }
}