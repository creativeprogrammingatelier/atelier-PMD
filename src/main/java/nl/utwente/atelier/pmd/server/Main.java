package nl.utwente.atelier.pmd.server;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Main extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("http://linux571.ewi.utwente.nl");
    }
}
