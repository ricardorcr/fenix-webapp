
package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.organizationalStructure.CountryUnit;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;
 
public class SetCountryToCountryUnit extends CustomTask {
 
    @Override
    public void runTask() throws Exception {
        CountryUnit malawiUnit = FenixFramework.getDomainObject("579821008183");
        Country malawi = FenixFramework.getDomainObject("712964571352");
        malawiUnit.setCountry(malawi);
    }
}
