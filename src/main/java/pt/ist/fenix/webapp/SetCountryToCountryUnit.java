package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.organizationalStructure.CountryUnit;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;
 
public class SetCountryToCountryUnit extends CustomTask {
 
    @Override
    public void runTask() throws Exception {
        CountryUnit iraqUnit = FenixFramework.getDomainObject("579821008282");
        Country iraq = FenixFramework.getDomainObject("712964571187");
        iraqUnit.setCountry(iraq);
    }
}
