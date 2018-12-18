
package pt.ist.fenix.webapp;
 
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.organizationalStructure.CountryUnit;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
 
import pt.ist.fenixframework.FenixFramework;
 
public class SetCountryToCountryUnit extends CustomTask {
 
    @Override
    public void runTask() throws Exception {
        CountryUnit paraguayUnit = FenixFramework.getDomainObject("579821008163");
        Country paraguay = FenixFramework.getDomainObject("712964571372");
        paraguayUnit.setCountry(paraguay);
    }
}
