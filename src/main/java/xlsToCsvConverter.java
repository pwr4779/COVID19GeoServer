import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static com.google.common.base.Preconditions.checkNotNull;
public class xlsToCsvConverter {
    private List<String> date;
    private List<String> city;
    private List<Double>[] confirmator;
    //Path path
    public void Converter(Path path){
        date = new ArrayList<String>();
        city = new ArrayList<String>();
        try {
            FileInputStream fis= new FileInputStream(path.toAbsolutePath().toString());
            HSSFWorkbook workbook = new HSSFWorkbook(fis);
            int rowindex;
            int columnindex;
            HSSFSheet sheet=workbook.getSheetAt(0);
            int rows=sheet.getLastRowNum();
            confirmator = new ArrayList[rows];
            int tableline = 0;
            for(rowindex=0;rowindex<rows;rowindex++){
                HSSFRow row=sheet.getRow(rowindex);
                confirmator[tableline] = new ArrayList<Double>();
                if(row !=null){
                    int cells=row.getLastCellNum();
                    for(columnindex=0;columnindex<cells;columnindex++){
                        HSSFCell cell=row.getCell(columnindex);
                        String value="";
                        if(cell==null){
                            continue;
                        }else{
                            switch (cell.getCellType()){
                                case FORMULA:
                                    value=cell.getCellFormula();
                                    break;
                                case NUMERIC:
                                    value=cell.getNumericCellValue()+"";
                                    break;
                                case STRING:
                                    value=cell.getStringCellValue()+"";
                                    break;
                                case BLANK:
                                    value=cell.getBooleanCellValue()+"";
                                    break;
                                case ERROR:
                                    value=cell.getErrorCellValue()+"";
                                    break;
                            }
                        }
                        if(rowindex == 0 && columnindex>0 && ! String.valueOf(value).equals("false")){
                            city.add(String.valueOf(value));
                        }
                        if(rowindex > 0 && columnindex == 0 && ! String.valueOf(value).equals("false")){
                            date.add(String.valueOf(value));
                        }
                        if(rowindex > 0 && columnindex > 0 && !String.valueOf(value).equals("false")){
                            confirmator[tableline].add(Double.parseDouble(value));
                        }
                    }
                    tableline++;
                }
            }
            Writer(path, date,city, confirmator);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void Writer(Path path, List date, List city, List[] airinfo){
        try {
            String csvPath = getFileNameExtension(path.toAbsolutePath().toString())+".csv";
            CSVWriter cw = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvPath), "EUC-KR"),',', ' ');
            for(int i = 0; i<date.size(); i++){
                for(int j =0; j<city.size(); j++){
                    cw.writeNext(new String[] { String.valueOf(date.get(i)),String.valueOf(city.get(j)),String.valueOf(airinfo[i+1].get(j))});
                }
            }
            cw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //file name extension
    public String getFileNameExtension(String fullName){
        int pos = fullName .lastIndexOf(".");
        String _fileName = fullName.substring(0, pos);
        return _fileName;
    }

}
