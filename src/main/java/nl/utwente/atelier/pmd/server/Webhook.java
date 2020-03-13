package nl.utwente.atelier.pmd.server;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Webhook extends HttpServlet {
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO: Implement Atelier webhook parsing
        response.sendRedirect("http://linux571.ewi.utwente.nl");
    }
}