///A Simple Web Server (WebServer.java)

package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


public class WebServer {

    protected static String EMPTY_RESSOURCE = "";
    protected static String RESOURCE_PATH = "doc/";
    protected static String INDEX_PATH = "doc/index.html";

    /**
     * WebServer constructor.
     */
    protected void start() {
        ServerSocket s;
        List<String> HTTP_METHODS = new ArrayList<>(Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD"));

        System.out.println("Webserver starting up on port 80");
        System.out.println("(press ctrl-c to exit)");

        try {
            // create the main server socket
            s = new ServerSocket(3000);
        } catch (Exception e) {
            System.out.println("Error: " + e);
            return;
        }

        System.out.println("Waiting for connection");
        for (; ; ) {
            try {
                // wait for a connection
                Socket remote = s.accept();
                // remote is now the connected socket
                System.out.println("Connection, sending data.");
                //Opening input/output binary stream of client socket
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        remote.getInputStream()));
                PrintWriter out = new PrintWriter(remote.getOutputStream());

                // read the data sent. We basically ignore it,
                // stop reading once a blank line is hit. This
                // blank line signals the end of the client HTTP
                // headers.
                // Header must finish with CR LF CR LF sequence (e.q \r\n\r\n in java)
                String header = "";
                String str = ".";

                /*
                 * Condition to check header (MUST BE COMPLETED)
                 *  warning: when using postman or executing the 1st time a request to the server on a web browser
                 *  Client will do 2 requests:
                 * - favicon : returns null
                 * - URL : returns GET /URL HTTP....
                 * THIS LOOP DOESN'T FETCH DATA THAT MAY BE CONTAIN IN BODY's REQUEST
                 */
                while (str != null && !str.equals("")) {
                    str = in.readLine();
                    header += str + '\n';
                }
                System.out.println("REQUEST: \n" + header);
                String[] header_decomposed = header.split(" ");
                String http_method = header_decomposed[0];

                if (!HTTP_METHODS.contains(http_method)) {
                    out.println(buildResponseHeader("501", "Not Implemented"));
                    out.flush();
                    remote.close();
                } else {
                    String resource = header_decomposed[1];
                    resource = resource.substring(1); // To get rid of '/' character
                    System.out.println(http_method + " " + resource);
                    if (EMPTY_RESSOURCE.equals(resource)) {
                        resource = INDEX_PATH;
                    }
                    if (!resource.startsWith(RESOURCE_PATH)) {
                        out.println(buildResponseHeader("403", "Forbidden"));
                    } else {
                        switch (http_method) {
                            case "GET":
                                httpGET(out, resource);
                                break;
                            case "PUT":
                                httpPUT(in, out, resource);
                                break;
                            case "POST":
                                httpPOST(in, out, resource);
                                break;
                            case "DELETE":
                                httpDELETE(out, resource);
                                break;
                            case "HEAD":
                                httpHEAD(out, resource);
                                break;
                        }
                    }
                    out.flush();
                    remote.close();
                }
            } catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }


    protected String buildResponseHeader(String status, String message) {
        String header = "HTTP/1.0 " + status + ' ' + message + "\r\n";
        header += "Server: Bot\r\n";
        header += "\r\n";
        return header;
    }

    protected String buildResponseHeader(String status, String message, String filename, long length) {
        String header = "HTTP/1.0 " + status + ' ' + message + "\r\n";
        if (filename.endsWith(".html") || filename.endsWith(".htm"))
            header += "Content-Type: text/html";
        else if (filename.endsWith(".mp4"))
            header += "Content-Type: video/mp4";
        else if (filename.endsWith(".jpeg") || filename.endsWith(".png"))
            header += "Content-Type: image/jpg";
        else if (filename.endsWith(".mp3"))
            header += "Content-Type: audio/mp3";
        else if (filename.endsWith(".avi"))
            header += "Content-Type: video/x-msvideo";
        else if (filename.endsWith(".css"))
            header += "Content-Type: text/css";
        else if (filename.endsWith(".pdf"))
            header += "Content-Type: application/pdf";
        else if (filename.endsWith(".odt"))
            header += "Content-Type: application/vnd.oasis.opendocument.text";
        header += "\r\n";
        header += "Content-Length: " + length + "\r\n";
        header += "Server: Bot\r\n";
        header += "\r\n";
        System.out.println(header);
        return header;
    }

    /**
     * Method that returns resource
     * filename doesn't exist -> 404 Not found
     * otherwise -> 200 OK
     * exception -> 500 Internal error
     *
     * @param out,      output flux to client socker
     * @param filename, filepath
     */
    protected void httpGET(PrintWriter out, String filename) {
        try {
            File file = new File(filename);
            if (!file.exists() || !file.isFile()) {
                out.println(buildResponseHeader("404", "Not Found", filename, file.length()));
            } else {
                out.println(buildResponseHeader("200", "OK", filename, file.length()));
                //Read file
                Scanner reader = new Scanner(file);
                while (reader.hasNextLine()) {
                    String data = reader.nextLine();
                    out.write(data);
                }
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(buildResponseHeader("500", "Internal Server Error"));
                out.flush();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * Method to create a resource (or replace an existing one).
     * If the file is new, creates it using data from request's body message.
     * If the file is old, replaces it using data from request's body message.
     * File is old -> 200, OK
     * File is new -> 201, Created
     * exception -> 500, Internal error
     *
     * @param in,       client socket input stream, to read body message
     * @param out,      client socket output stream, to write response
     * @param filename, filepath
     */
    protected void httpPUT(BufferedReader in, PrintWriter out, String filename) {
        try {
            File file = new File(filename);
            boolean has_existed = file.exists();
            PrintWriter pw = new PrintWriter(file);
            pw.close();
            PrintWriter fileOut = new PrintWriter(new FileOutputStream(file));

            /**
             * @// TODO: 18/10/2020  doesn't entirely work
             */
            int charRead;
            while ((charRead = in.read()) > 0) {
                System.out.println(charRead);
                fileOut.write(charRead);
            }
            fileOut.flush();
            fileOut.close();
            if (has_existed) {
                out.println(buildResponseHeader("200", "OK", filename, file.length()));
            } else {
                out.println(buildResponseHeader("201", "Created",filename, file.length()));
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(buildResponseHeader("500", "Internal Server Error"));
                out.flush();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * Method to replace an existing resource (or create one)
     * If file exists -> add at the end request's body message -> 200, OK
     * If file is new -> create a new file -> 201, Created
     * exception -> 500, Internal error
     *
     * @param in,       client socket input stream, to read body message
     * @param out,      client socket output stream, to write response
     * @param filename, filepath
     */
    protected void httpPOST(BufferedReader in, PrintWriter out, String filename) {
        try {
            File file = new File(filename);
            boolean has_existed = file.exists();
            PrintWriter fileOut = new PrintWriter(new FileOutputStream(file, has_existed));
            /**
             * @// TODO: 18/10/2020  doesn't entirely work
             */
            int charRead;
            while ((charRead = in.read()) > 0) {
                System.out.println(charRead);
                fileOut.write(charRead);
            }
            fileOut.flush();
            fileOut.close();
            if (has_existed) {
                out.println(buildResponseHeader("200", "OK", filename, file.length()));
            } else {
                out.println(buildResponseHeader("201", "Created", filename, file.length()));
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(buildResponseHeader("500", "Internal Server Error"));
                out.flush();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * Method that delete a file given its filepath.
     * If deleted -> 204, No Content
     * If can't delete -> 403, Forbidden
     * If not found -> 404, Not Found
     * exception -> 500, Internal error
     *
     * @param out,      client socket output stream, to return header
     * @param filename, filepath
     */
    protected void httpDELETE(PrintWriter out, String filename) {
        try {
            File file = new File(filename);
            if (!file.exists() || !file.isFile()) {
                out.println(buildResponseHeader("404", "Not Found"));
            } else {
                boolean deleted = file.delete();
                if (deleted) {
                    out.println(buildResponseHeader("204", "No Content",filename, file.length()));
                } else {
                    out.println(buildResponseHeader("403", "Forbidden", filename, file.length()));
                }
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(buildResponseHeader("500", "Internal Server Error"));
                out.flush();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * Method that verify the existence of a resource.
     * @param out, client socket output flux, to send header
     * @param filename, filepath
     */
    protected void httpHEAD(PrintWriter out, String filename){
        try{
            File file = new File(filename);
            if (!file.exists() || !file.isFile()) {
                out.println(buildResponseHeader("404", "Not Found", filename, file.length()));
            }else{
                out.println(buildResponseHeader("200", "OK", filename, file.length()));
            }
            out.flush();
            }catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(buildResponseHeader("500", "Internal Server Error"));
                out.flush();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * Start the application.
     *
     * @param args Command line parameters are not used.
     */
    public static void main(String[] args) {
        WebServer ws = new WebServer();
        ws.start();
    }
}
