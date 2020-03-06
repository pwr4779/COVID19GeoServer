import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Parser {
    private Date nowDate;
    private String dateStr;
    private String  dirPath;
    public Parser(Date nowDate, String dirPath){
        try {
            this. nowDate = nowDate;
            this.dirPath = dirPath;
            getList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getList() throws Exception{
        Document doc = Jsoup.connect("https://www.cdc.go.kr/board.es?mid=a20501000000&bid=0015&nPage=1").get();
        Elements contents = doc.select("#listView a");
        //System.out.println(contents);
        for(int i =0; i<contents.size(); i++) {
            String url = contents.get(i).attr("href");
            String title = contents.get(i).attr("title");
            checkCOVID19data(url, title);
        }
    }

    public void checkCOVID19data(String url, String title) throws ParseException, IOException {
        String date = null;
        if (title.contains("코로나바이러스감염증-19") && title.contains("0시 기준")) {
            String[] split = title.split("\\(|,");
            date = split[1];
            date =date.replace("월","");
            date = date.replace("일", "");

            String[] day = date.split(" ");
            date = "2020-" +String.format("%02d", Integer.parseInt(day[0])) +"-"+ String.format("%02d", Integer.parseInt(day[1]));
            dateStr = date;
            //System.out.println(date);
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
            Date coviddate = dt.parse(date);
            //System.out.print(coviddate);
            //System.out.print(nowDate);
            //System.out.println(coviddate.compareTo(nowDate));
            if (coviddate.compareTo(nowDate) == 0) {
                getCOVID19data("https://www.cdc.go.kr"+url);
            }
        }
    }

    public void getCOVID19data(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Element tbody = doc.select("tbody").last();
        //System.out.println(contents);
        Element tr = tbody.select("tr").get(2);
        Elements td = tr.select("td");
        System.out.println(td.text());
        createCSV(td.text());
    }

    public void createCSV(String data){
        //출력 스트림 생성
        String[] info = data.split(" ");
        BufferedWriter bufWriter = null;
        String filedate = dateStr.replace("-","");
        try{
            bufWriter = Files.newBufferedWriter(Paths.get(dirPath+"COVID19_"+filedate+".csv"), Charset.forName("UTF-8"));
            //bufWriter = Files.newBufferedWriter(Paths.get("D:\\test.csv"), Charset.forName("UTF-8"));
            bufWriter.write("날짜,지역,확진환자수");
            bufWriter.newLine();

            for(int i = 0; i<17; i++){
                StringBuilder sb = new StringBuilder();
                sb.append(dateStr);
                sb.append(",");
                sb.append(city(i));
                sb.append(",");
                sb.append(info[i+3]);
                bufWriter.write(sb.toString());
                bufWriter.newLine();
            }


        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            try{
                if(bufWriter != null){
                    bufWriter.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public String city(int i){
        String cityname = null;
        switch(i){
            case 0:
                cityname = "서울";
                break;
            case 1:
                cityname = "부산";
                break;
            case 2:
                cityname = "대구";
                break;
            case 3:
                cityname = "인천";
                break;
            case 4:
                cityname = "광주";
                break;
            case 5:
                cityname = "대전";
                break;
            case 6:
                cityname = "울산";
                break;
            case 7:
                cityname = "세종";
                break;
            case 8:
                cityname = "경기";
                break;
            case 9:
                cityname = "강원";
                break;
            case 10:
                cityname = "충북";
                break;
            case 11:
                cityname = "충남";
                break;
            case 12:
                cityname = "전북";
                break;
            case 13:
                cityname = "전남";
                break;
            case 14:
                cityname = "경북";
                break;
            case 15:
                cityname = "경남";
                break;
            case 16:
                cityname = "제주";
                break;
        }
        return   cityname;
    }
}