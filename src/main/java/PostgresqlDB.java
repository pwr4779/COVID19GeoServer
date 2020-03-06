import clojure.main;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import it.geosolutions.geoserver.rest.encoder.metadata.GSDimensionInfoEncoder;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.*;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PostgresqlDB {
    private Connection con = null;
    private Statement st = null;
    private int update;
    private String url;
    private String user;
    private String password;

    public PostgresqlDB(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void Init() {
        ResultSet rs = null;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            con = DriverManager.getConnection(url, user, password);
            st = con.createStatement();
            rs = st.executeQuery("SELECT VERSION()");

            if (rs.next()) {
                System.out.println(rs.getString(1));
            }

        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(main.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);

        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (con != null) {
                    con.close();
                }

            } catch (SQLException ex) {
                Logger lgr = Logger.getLogger(main.class.getName());
                lgr.log(Level.WARNING, ex.getMessage(), ex);
            }
        }

    }

    public void createTable(String tableName) {
        try {
            con = DriverManager.getConnection(url, user, password);
            DatabaseMetaData dbm = null;
            dbm = con.getMetaData();
            // check if "employee" table is there
            ResultSet tables = dbm.getTables(null, null, tableName, new String[]{"TABLE"});
            if (tables.next()) {
                // Table exists
                return;
            } else {
                // Table does not exist
                StringBuilder sb = new StringBuilder();
                String sql = sb.append("CREATE TABLE ")
                        .append(tableName)
                        .append("(covid19date date, city character varying(50) not null, confirmator double precision);").toString();
                executeSQL(sql);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return;

    }


    public String importCSV(File file) throws SQLException {
        String tableName = file.getName().substring(0, file.getName().lastIndexOf(".csv"));
        if (file.equals(WatchService.latloncsv)) {
            createLatLonTable(tableName);
        } else {
            createTable(tableName);
        }

        String fileName = file.getName();
        String sql = "copy " + tableName + " FROM stdin DELIMITER ',' CSV header  encoding 'UTF-8'";
        BaseConnection pgcon = (BaseConnection) con;
        CopyManager mgr = new CopyManager(pgcon);
        try {
            Reader in = new BufferedReader(new FileReader(file));
            long rowsaffected = mgr.copyIn(sql, in);
            System.out.println("Rows copied: " + rowsaffected);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

            if (file.equals(WatchService.latloncsv)) {
                CreateGeomtry(tableName);
                return "latlon";
            } else {
                CreateView(tableName);
                return "vw_" + tableName;
            }

    }

    private void createLatLonTable(String tableName) {
        try {
            con = DriverManager.getConnection(url, user, password);
            DatabaseMetaData dbm = null;
            dbm = con.getMetaData();
            // check if "employee" table is there
            ResultSet tables = dbm.getTables(null, null, tableName, new String[]{"TABLE"});
            if (tables.next()) {
                // Table exists
                return;
            } else {
                // Table does not exist
                StringBuilder sb = new StringBuilder();
                String sql = sb.append("CREATE TABLE ")
                        .append(tableName)
                        .append("(city character varying(50) not null, lat double precision, lon double precision);").toString();
                executeSQL(sql);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return;
    }

    public void CreateGeomtry(String tableName){
        StringBuilder sb = new StringBuilder();
       String sql = sb.append("ALTER TABLE ")
               .append(tableName)
                 .append(" ADD column the_geom geometry;")
                .append("update ")
                .append(tableName)
                .append(" set the_geom = ST_GeomFromText('POINT('||lon||' '||lat||')',4326);").toString();
        executeSQL(sql);
    }

    public void CreateView(String tableName){
        StringBuilder sb = new StringBuilder();
        String sql = sb.append("create view ")
                .append("public.vw_")
                .append(tableName)
                .append(" as select * from ")
                .append(tableName)
                .append(" left outer join latlon using(city);").toString();
        executeSQL(sql);
    }

    private void executeSQL(String sql) {
        try {
            con = DriverManager.getConnection(url, user, password);
            st = con.createStatement();
            update = st.executeUpdate(sql);
        } catch (
                SQLException e) {
            System.out.println("query execution failed...");
            e.printStackTrace();
            return;
        }
    }




}

