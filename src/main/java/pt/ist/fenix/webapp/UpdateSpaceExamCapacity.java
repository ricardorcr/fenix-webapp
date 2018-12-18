package pt.ist.fenix.webapp;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.spaces.domain.Space;
import org.fenixedu.spaces.ui.InformationBean;
import org.joda.time.DateTime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.fenixframework.FenixFramework;

public class UpdateSpaceExamCapacity extends CustomTask {

    @Override
    public void runTask() throws Exception {
        List<String> allLines = Files.readAllLines(new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/salas_capacidade_exame.txt").toPath());
        for (String line : allLines) {
            String[] parts = line.split("\t");
            Space space = FenixFramework.getDomainObject(parts[0]);
            String examCapacity = parts[1];            
            InformationBean bean = space.bean();
            JsonElement rawMetadata = bean.getRawMetadata();
            JsonObject metadata = rawMetadata.getAsJsonObject();
            metadata.addProperty("examCapacity", examCapacity);
            DateTime validFrom = new DateTime(2020,10,9,23,59);
            InformationBean newInformationBean = new InformationBean(bean.getExternalId(), bean.getAllocatableCapacity(), bean.getBlueprintNumber(),
                    bean.getArea(), bean.getName(), bean.getIdentification(), validFrom, null, metadata, bean.getClassification(),
                    bean.getBlueprint(), bean.getSpacePhotoSet(), User.findByUsername("ist24616"));
            space.bean(newInformationBean);
        }      
    }
}
