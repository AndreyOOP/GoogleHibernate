package Tst;

import com.google.appengine.api.utils.SystemProperty;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class CloudSqlServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        String path = req.getRequestURI();

        ignoreFavicon(path);

        PrintWriter out = resp.getWriter();
        resp.setContentType("text/plain");

        String url = chooseDriverGAEorLocalAndReturnURL();

        out.print( "Get From testuserauth table (Hibernate): " + hibernateDbAccess() + "\n");

        try (Connection conn = DriverManager.getConnection(url)) {

            out.print( "Get From testuserauth table: " + dbAccess(conn) + "\n");

        } catch (SQLException e) {
            throw new ServletException("SQL error", e);
        }
    }

    private String chooseDriverGAEorLocalAndReturnURL(){

        if (System.getProperty("com.google.appengine.runtime.version").startsWith("Google App Engine/")) {// Check the System properties to determine if we are running on appengine or not

            try {
                Class.forName("com.mysql.jdbc.GoogleDriver"); // Load the class that provides the new "jdbc:google:mysql://" prefix.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();}

            return System.getProperty("cloudsql.url");

        } else {
            return System.getProperty("cloudsql.url.dev"); // Set the url with the local MySQL database connection url when running locally
        }
    }

    private String dbAccess(Connection connection){

        try {

            ResultSet resultSet = connection.prepareStatement("SELECT * FROM testuserauth").executeQuery();

            resultSet.next();
            resultSet.next();

            return resultSet.getString("login");

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "";
    }

    private String hibernateDbAccess(){

        Map<String, String> properties = new HashMap();
        if (SystemProperty.environment.value() ==
                SystemProperty.Environment.Value.Production) {
            properties.put("javax.persistence.jdbc.driver",
                    "com.mysql.jdbc.GoogleDriver");
            properties.put("javax.persistence.jdbc.url",
                    System.getProperty("cloudsql.url"));
        } else {
            properties.put("javax.persistence.jdbc.driver",
                    "com.mysql.jdbc.Driver");
            properties.put("javax.persistence.jdbc.url",
                    System.getProperty("cloudsql.url.dev"));
        }

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("Demo", properties);
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();

        TableEntity te = em.find(TableEntity.class, "test3");

        return te.getPassword() + " " + te.getLogin();
    }

    private void ignoreFavicon(String path){

        if (path.startsWith("/favicon.ico")) { // ignore the request for favicon.ico
            return;
        }
    }
}
