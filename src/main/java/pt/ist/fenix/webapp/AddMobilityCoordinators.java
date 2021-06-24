package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class AddMobilityCoordinators extends CustomTask {
    @Override
    public void runTask() throws Exception {

        List<String> mobilityCoordinators = null;
		try {
            mobilityCoordinators = Files.readAllLines(
                    //
                    ///afs/ist.utl.pt/ciist/fenix/fenix015/ist
					new File("/home/rcro/DocumentsHDD/fenix/candidaturas/coordenadores_mobilidade_2021_2022.csv").toPath());
		} catch (IOException e) {
			throw new Error("Erro a ler o ficheiro.");
		}
        for (String line : mobilityCoordinators) {
            final String[] strings = line.split("\t");
            taskLog(line);
            Degree.readBySigla(strings[1]).getMobilityCoordinatorsSet().add(User.findByUsername(strings[0]));
        }
    }
}
