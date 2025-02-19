package pt.ist.fenix.webapp;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fenixedu.academic.domain.organizationalStructure.CountryUnit;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.academic.domain.raides.DegreeClassification;
import org.fenixedu.academic.domain.raides.DegreeDesignation;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.util.Locale;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CheckUpdateRaidesDegreesAndDesignations extends CustomTask {

    private static final Locale PT = new Locale("pt", "PT");

    @Override
    public void runTask() throws Exception {

        final File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/MatrizCesRamos_2025_Connect.xlsx");
//        final File file = new File("/home/rcro/Documents/fenix/gep/raides/MatrizCesRamos_2025_Connect.xlsx");
        final Workbook workbook = new XSSFWorkbook(file);
        final Sheet sheet = workbook.getSheet("Estabelecimento_Curso_Ramo");
        final Stream<Row> rowStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(sheet.rowIterator(), Spliterator.ORDERED), false);

        final CountryUnit parent = FenixFramework.getDomainObject("579820660538");
        rowStream.forEach(row -> {
            if (row.getRowNum() != 0) {
                final String univCode = row.getCell(0).getStringCellValue();

                final Unit unit = findUnit(univCode);
                final String univName = row.getCell(1).getStringCellValue();
                if (unit != null) {
                    final String degreeCode = row.getCell(2).getStringCellValue();
                    final String degreeName = row.getCell(3).getStringCellValue();
                    final String degreeLevel = row.getCell(5).getStringCellValue();
                    DegreeDesignation degreeDesignation = findDegreeDesignation(degreeCode);
                    if (degreeDesignation == null) {
                        final DegreeClassification degreeClassification = DegreeClassification.readByCode(degreeLevel);
                        degreeDesignation = new DegreeDesignation(degreeCode, degreeName, degreeClassification);
                        unit.addDegreeDesignation(degreeDesignation);
                        taskLog("Creating DD:\t%s\t%s%n", degreeCode, degreeName);
                    } else {
                        if (!unit.getDegreeDesignationSet().contains(degreeDesignation)) {
                            taskLog("Updating:\t%s\t%s\t%s%n", degreeCode, degreeDesignation.getDescription(), univName);
                            unit.addDegreeDesignation(degreeDesignation);
                        }
                    }
                } else {
                    taskLog("Univ não existia: %s\t%s%n", row.getCell(0).getStringCellValue(), univName);
                    LocalizedString name = new LocalizedString(PT, univName);
                    UniversityUnit.createNewUniversityUnit(name, parent, true, univCode, null);
                }
            }
        });
    }

    private DegreeDesignation findDegreeDesignation(final String degreeCode) {
        return Bennu.getInstance().getDegreeDesignationsSet().stream()
                .filter(dd -> degreeCode.equals(dd.getCode()))
                .findAny().orElse(null);
    }

    private Unit findUnit(final String univCode) {
        return Bennu.getInstance().getUnitNameSet().stream()
                .map(unitName -> unitName.getUnit())
                .filter(unit -> univCode.equals(unit.getCode()))
                .findAny().orElse(null);
    }
}
