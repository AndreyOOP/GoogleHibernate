package Tst;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class CloudSqlServlet extends HttpServlet {

    final String createTableSql = "CREATE TABLE IF NOT EXISTS visits ( visit_id INT NOT NULL AUTO_INCREMENT, user_ip VARCHAR(46) NOT NULL," +
                                  " timestamp DATETIME NOT NULL, PRIMARY KEY (visit_id) )";
    final String createVisitSql = "INSERT INTO visits (user_ip, timestamp) VALUES (?, ?)";
    final String selectSql      = "SELECT user_ip, timestamp FROM visits ORDER BY timestamp DESC LIMIT 10";


    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        String path = req.getRequestURI();

        ignoreFavicon(path);

        String userIp = storeIP( req.getRemoteAddr());

        PrintWriter out = resp.getWriter();
        resp.setContentType("text/plain");

        String url = chooseDriverGAEorLocalAndReturnURL();

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement statementCreateVisit = conn.prepareStatement(createVisitSql)) {

            conn.createStatement().executeUpdate(createTableSql);
            statementCreateVisit.setString(1, userIp);
            statementCreateVisit.setTimestamp(2, new Timestamp(new Date().getTime()));
            statementCreateVisit.executeUpdate();

            try (ResultSet rs = conn.prepareStatement(selectSql).executeQuery()) {

                out.print("Last 10 visits:\n");

                while (rs.next()) {

                    String savedIp = rs.getString("user_ip");
                    String timeStamp = rs.getString("timestamp");
                    out.print("TimeN: " + timeStamp + " Addr: " + savedIp + "\n");
                }
            }

            out.print( "Get From testuserauth table: " + dbAccess(conn));

        } catch (SQLException e) {
            throw new ServletException("SQL error", e);
        }
    }

    private String storeIP(String userIp){

        InetAddress address = null;

        try {
            address = InetAddress.getByName(userIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if (address instanceof Inet6Address) {
            return userIp.substring(0, userIp.indexOf(":", userIp.indexOf(":") + 1)) + ":*:*:*:*:*:*";

        } else if (address instanceof Inet4Address) {
            return userIp.substring(0, userIp.indexOf(".", userIp.indexOf(".") + 1)) + ".*.*";
        }

        return "error";
    }

    private void ignoreFavicon(String path){

        if (path.startsWith("/favicon.ico")) { // ignore the request for favicon.ico
            return;
        }
    }

    private String chooseDriverGAEorLocalAndReturnURL(){

        if (System.getProperty("com.google.appengine.runtime.version").startsWith("Google App Engine/")) {// Check the System properties to determine if we are running on appengine or not

            try {
                Class.forName("com.mysql.jdbc.GoogleDriver"); // Load the class that provides the new "jdbc:google:mysql://" prefix.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();}

            return System.getProperty("ae-cloudsql.cloudsql-database-url");

        } else {
            return System.getProperty("ae-cloudsql.local-database-url"); // Set the url with the local MySQL database connection url when running locally
        }
    }

    private String dbAccess(Connection connection){

        try {

            ResultSet resultSet = connection.prepareStatement("SELECT * FROM testuserauth").executeQuery();
            resultSet.next();

            return resultSet.getString("login");

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "";
    }
}
