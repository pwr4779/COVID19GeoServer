import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PaserTest {
    public static void main(String[] args) {
        String nowdate = "2020-03-05";
        SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date now = transFormat.parse(nowdate);
            Parser a = new Parser(now, null);
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
