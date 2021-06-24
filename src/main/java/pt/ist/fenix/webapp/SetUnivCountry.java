package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.organizationalStructure.CountryUnit;
import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;
 
public class SetUnivCountry extends CustomTask {
 
    @Override
    public void runTask() throws Exception {
        UniversityUnit univ = FenixFramework.getDomainObject("1126007281025173");
        univ.setCountry(FenixFramework.getDomainObject("712964571147")); //France
        CountryUnit countryUnit = FenixFramework.getDomainObject("579820660799"); //France
//        countryUnit.getChildsSet()
    }
}