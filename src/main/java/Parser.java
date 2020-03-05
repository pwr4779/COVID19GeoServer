import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Parser {
    private Date nowDate;

    public Parser(Date nowDate){
        try {
            this. nowDate = nowDate;
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
    }
}