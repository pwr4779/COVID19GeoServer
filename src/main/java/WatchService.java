import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import it.geosolutions.geoserver.rest.encoder.datastore.GSPostGISDatastoreEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import it.geosolutions.geoserver.rest.encoder.metadata.GSDimensionInfoEncoder;
import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;
//import it.geosolutions.geobatch.actions.ds2ds.dao.FeatureConfiguration;

@Data
public  class  WatchService {
    public static File latloncsv;
    //    final DSGeoServerConfiguration conf;
    //프로젝트 경로
    public Properties prop;
    private String dirPath;
    private WatchKey watchKey;
    private Geoserver geoserver;
    private GeoServerRESTManager manager;
    private GeoServerRESTReader reader;
    private GeoServerRESTPublisher publisher;
    private PostgresqlDB postgresqldb;
    private GSPostGISDatastoreEncoder datastoreEncoder;
    //private Shapefile shapefile;
    private String converterPath;
    private String DatastoreName;
    private xlsToCsvConverter xlsConverter;
//    private String dataStore;
//    private String layer;

    public void init() throws IOException {

        //GeoServer Init
        configInit();
        String RESTURL  = geoserver.getURL();
        String RESTUSER = geoserver.getID();
        String RESTPW   = geoserver.getPW();
        manager = new GeoServerRESTManager(new URL(RESTURL), RESTUSER, RESTPW);
        reader = new GeoServerRESTReader(RESTURL, RESTUSER, RESTPW);
        publisher = new GeoServerRESTPublisher(RESTURL, RESTUSER, RESTPW);
        boolean created = manager.getStoreManager().create(geoserver.getWorkspace(), datastoreEncoder); //geoserver - PostGis connect create store
        System.out.println("PostGisDB초기만들기:"+created);
        try {
            postgresqldb.importCSV(latloncsv);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //watchService 생성
        prop = System.getProperties();
        prop.setProperty("user.dir",dirPath);
        String projPath = prop.get("user.dir").toString();

        //FIXME GPFS not supported fileSystem notice
        java.nio.file.WatchService watchService = FileSystems.getDefault().newWatchService();
        //경로 생성
        Path path = Paths.get(projPath);
        //해당 디렉토리 경로에 와치서비스와 이벤트 등록
        path.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.OVERFLOW);

        Thread thread = new Thread(()-> {
            while(true) {
                try {
                    watchKey = watchService.take();//이벤트가 오길 대기(Blocking)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                List<WatchEvent<?>> events = watchKey.pollEvents();//이벤트들을 가져옴
                for(WatchEvent<?> event : events) {
                    //이벤트 종류
                    WatchEvent.Kind<?> kind = event.kind();
                    //경로
                    Path paths = (Path)event.context();
                    System.out.println(paths.toAbsolutePath());
                    if(kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
//                        geoserver.setUrl("http://localhost:8080/geoserver/web");
//                        geoserver.setUser("admin");
//                        geoserver.setPassword("geoserver");
//                        String response = HTTPUtils.get(geoserver.getUrl() + "/rest/about/versions.xml", geoserver.getUser(), geoserver.getPassword());
                        System.out.println("created something in directory");
                        try {
                            fileFormatSwitch(paths);
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }else if(kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        System.out.println("delete something in directory");
                    }else if(kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                        System.out.println("modified something in directory");

                    }else if(kind.equals(StandardWatchEventKinds.OVERFLOW)) {
                        System.out.println("overflow");
                    }else {
                    }
                }
                if(!watchKey.reset()) {
                    try {
                        watchService.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public void configInit() throws FileNotFoundException {
        Yaml yaml = new Yaml();
        Reader yamlFile = new FileReader("config.yml");
        Map<String, Object> yamlMaps = yaml.load(yamlFile);
        Map<String, String> hashMaps = new HashMap<String, String>();

        //Monitoring Dir Setting
        hashMaps = (HashMap<String, String>) yamlMaps.get("Dir");
        dirPath = hashMaps.get("path");

        //Geoserver Setting
        hashMaps = (HashMap<String, String>) yamlMaps.get("Geoserver");
        geoserver = new Geoserver(hashMaps.get("URL"),hashMaps.get("ID"),String.valueOf(hashMaps.get("PW")));
        geoserver.setWorkspace(hashMaps.get("Workspace"));

        //Postgresql Setting
        hashMaps = (HashMap<String, String>) yamlMaps.get("Postgresql");
        postgresqldb = new PostgresqlDB(hashMaps.get("URL"),hashMaps.get("USER"),String.valueOf(hashMaps.get("PW")));
        postgresqldb.Init();

        //Geoserver - Postgis setting
        hashMaps = (HashMap<String, String>) yamlMaps.get("GeoseverPostgis");
        datastoreEncoder = new GSPostGISDatastoreEncoder(hashMaps.get("DataStore"));
        DatastoreName = hashMaps.get("DataStore");
        datastoreEncoder.setHost(hashMaps.get("Host"));
        datastoreEncoder.setPort(Integer.parseInt(String.valueOf(hashMaps.get("Port"))));
        datastoreEncoder.setDatabase(hashMaps.get("Database"));
        datastoreEncoder.setSchema(hashMaps.get("Schema"));
        datastoreEncoder.setUser(hashMaps.get("USER"));
        datastoreEncoder.setPassword(String.valueOf(hashMaps.get("PW")));

        //CSVfile setting
        hashMaps = (HashMap<String, String>) yamlMaps.get("CSVfile");
//        xlsConverter = new xlsToCsvConverter(Integer.parseInt(String.valueOf(hashMaps.get("row"))),Integer.parseInt(String.valueOf(hashMaps.get("col"))));
        //LatLonfile setting
        hashMaps = (HashMap<String, String>) yamlMaps.get("latlon");
        latloncsv = new File(hashMaps.get("path"));

    }

    public void fileFormatSwitch(Path path) throws IOException, InterruptedException, SQLException {

        File file = new File(path.toAbsolutePath().toString());
        System.out.println(file.getName());
        String Workspace = geoserver.getWorkspace();

        if(file.isFile()){
            String format = getFileFormatExtension(path.toAbsolutePath().toString());
            System.out.println("file format:"+format);
            switch(format){
                case "xls":
//                    xlsConverter.Converter(path);
                    break;
                case "csv":
                    String tablename = postgresqldb.importCSV(file);
                    PublishPostGIS(geoserver.getWorkspace(), DatastoreName, tablename);
                    break;
            }
        }else if(file.isDirectory()) {
//            System.out.println(file.getName() + "is Directory");
//            File dir = new File(path.toAbsolutePath().toString());
//            File[] fileList = dir.listFiles();
//            File subfile = null;
//            try{
//                boolean shpcheck = false;
//                boolean geotiffcheck = false;
//                for(int i = 0 ; i < fileList.length ; i++){
//
//                    subfile = fileList[i];
//                    if(subfile.isFile() && (subfile.getName()).contains(".shp")){
//                        // shapefile exist
//                        System.out.println("shp exist");
//                        shpcheck = true;
//                    }
//                    if(subfile.isFile() && (subfile.getName()).contains(".geotiffcheck")){
//                        // shapefile exist
//                        System.out.println("geotiff exist");
//                        shpcheck = true;
//                    }
//                }
//                if(shpcheck && !geotiffcheck) {
//                    boolean result = shapefile.zipshapefile(publisher, Workspace, subfile, null);
//                }
//                return;
//            }catch(IOException e){
//            }
        }
    }


    public void PublishPostGIS(String WorkspaceName, String datastoreName, String tableName) throws MalformedURLException {
        System.out.println("POSTGIS");
        boolean created = manager.getStoreManager().create(geoserver.getWorkspace(), datastoreEncoder); //geoserver - PostGis connect create store
        //boolean ok = manager.getPublisher().publishDBLayer(WorkspaceName, datastoreName,fte,layerEncoder);
       boolean ok = manager.getPublisher().publishDBLayer(geoserver.getWorkspace() , datastoreName, tableName ,"EPSG:4326","point");
        System.out.println("publish:"+ok);
    }


    //file format extension
    public String getFileFormatExtension(String fullName) {
        checkNotNull(fullName);
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    //file name extension
    public String getFileNameExtension(String fullName){
        int pos = fullName .lastIndexOf(".");
        String _fileName = fullName.substring(0, pos);
        return _fileName;
    }


}