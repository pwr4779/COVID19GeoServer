import lombok.Data;
import lombok.NonNull;

@Data
public class Geoserver {
    @NonNull
    private String URL;
    @NonNull
    private String ID;
    @NonNull
    private String PW;
    private String Workspace;
}
