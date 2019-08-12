package main.webapp.classes;

import main.webapp.PMDTool.main.java.nl.utwente.pmdcreate.main.AtelierPMD;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.server.ManagedAsync;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

@Path("/resource")
public class PMDResource {
    //https://medium.com/techinpieces/how-to-upload-files-using-rest-service-with-java-jersey-jax-rs-on-tomcat-847dc0e6a179
    @POST
    @Produces(MediaType.TEXT_XML)
    @ManagedAsync
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void post(
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileMetaData,
            @Suspended final AsyncResponse asyncResponse
    ) {
        System.out.println("End point Test Reached");

//        AtelierPMD pmd = new AtelierPMD("/home/andrew/Desktop/test-processing-files");
//        String result = pmd.main();
//        asyncResponse.resume(result);
    }


}