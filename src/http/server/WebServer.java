

package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Integer.parseInt;



/**
 * HTTP server implementing GET, PUT, POST, DELETE and HEAD requests
 * @author GALERZA Javier, BEL Corentin, KERMANI Benjamin
 */

public class WebServer {

    protected static final String EMPTY_RESSOURCE = "";
    protected static final String RESOURCE_PATH = "doc/";
    protected static final String INDEX_PATH = "doc/index.html";
    protected static final String GET = "GET";
    protected static final String POST = "POST";
    protected static final String PUT = "PUT";
    protected static final String DELETE = "DELETE";
    protected static final String HEAD = "HEAD";
    protected static final List<String> HTTP_METHODS = new ArrayList<>(Arrays.asList(GET, POST, PUT, DELETE, HEAD));


    /**
     * WebServer constructor
     * @param port , port number on which ServerSocket will be created
     */
    protected void start(int port) {
        ServerSocket s;

        System.out.println("Webserver starting up on port " + port);
        System.out.println("(press ctrl-c to exit)");

        try {
            // create the main server socket
            s = new ServerSocket(port);
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
                BufferedInputStream in_bis = new BufferedInputStream(remote.getInputStream());
                BufferedOutputStream out = new BufferedOutputStream(remote.getOutputStream());
                // Header must finish with CR LF CR LF sequence (e.q \r\n\r\n in java)
                String header = "";
                boolean right_syntax = false;
                while (in_bis.available() > 0) {
                    int cur = in_bis.read();
                    header += (char) cur;
                    if (header.contains("\r\n\r\n")) {
                        right_syntax = true;
                        break;
                    }
                }
                System.out.println("REQUEST: \n" + header);
                if (right_syntax) {
                    String[] header_decomposed = header.split(" ");
                    String http_method = header_decomposed[0];

                    if (!HTTP_METHODS.contains(http_method)) {
                        out.write(buildResponseHeader("501", "Not Implemented").getBytes());
                    } else {
                        String resource = header_decomposed[1];
                        // To get rid of '/' character
                        resource = resource.substring(1);
                        if (EMPTY_RESSOURCE.equals(resource)) {
                            resource = INDEX_PATH;
                        }
                        if (!resource.startsWith(RESOURCE_PATH)) {
                            out.write(buildResponseHeader("403", "Forbidden").getBytes());
                        } else {
                            switch (http_method) {
                                case GET:
                                    httpGET(out, resource);
                                    break;
                                case PUT:
                                    httpPUT(in_bis, out, resource);
                                    break;
                                case POST:
                                    httpPOST(in_bis, out, resource);
                                    break;
                                case DELETE:
                                    httpDELETE(out, resource);
                                    break;
                                case HEAD:
                                    httpHEAD(out, resource);
                                    break;
                            }
                        }
                    }
                    out.flush();
                    remote.close();
                } else {
                    out.write(buildResponseHeader("400", "Bad Request").getBytes());
                    out.flush();
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error: " + e);
            }
        }
    }


    /**
     * Build simple response header that will be returned to client
     * @param status HTTP code status
     * @param message message explaining HTTP code status
     * @return response header as string
     */
    protected String buildResponseHeader(String status, String message) {
        String header = "HTTP/1.0 " + status + ' ' + message + "\r\n";
        header += "Server: Bot\r\n";
        header += "\r\n";
        System.out.println(header);
        return header;
    }


    /**
     * Build complex response header that will be returned to client,
     * allowing him to interpret response depending on content-type
     * @param status HTTP code status
     * @param message message explaining HTTP code status
     * @param filename filename to find corresponding extension (file type)
     * @param length file length (in bytes)
     * @return response header as string
     */
    protected String buildResponseHeader(String status, String message, String filename, long length) {
        System.out.println("REPONSE : ");
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
        else if (filename.endsWith(".pdf")) {
            header += "Content-Type: application/pdf";
        } else if (filename.endsWith(".odt"))
            header += "Content-Type: application/vnd.oasis.opendocument.text";
        header += "\r\n";
        header += "Content-Length: " + length + "\r\n";
        header += "Server: Bot\r\n";
        header += "\r\n";
        System.out.println(header);
        return header;
    }

    /**
     * HTTP method that returns resource to client, allowing him to visualize the resource he asked for. (
     * filename doesn't exist -> 404 Not found
     * otherwise -> 200 OK
     * exception -> 500 Internal error
     * @param out output flux to client socket
     * @param filename filepath
     */
    protected void httpGET(BufferedOutputStream out, String filename) {
        try {
            File file = new File(filename);
            if (!file.exists() || !file.isFile()) {
                out.write(buildResponseHeader("404", "Not Found", filename, file.length()).getBytes());
            } else {
                out.write(buildResponseHeader("200", "OK", filename, file.length()).getBytes());
                //Read file
                byte[] bytes = Files.readAllBytes(file.toPath());
                out.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(buildResponseHeader("500", "Internal Server Error").getBytes());
                out.flush();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * HTTP method that creates a resource (or replace an existing one).
     * If file doesn't exist, creates it using data from request's body message.
     * If file is old, replaces it (i.e, erase previous data) using data from request's body message.
     * File is old -> 200, OK
     * File is new -> 201, Created
     * exception -> 500, Internal error
     *
     * @param in client socket input stream, to read body message
     * @param out client socket output stream, to write response
     * @param filename filepath
     */
    protected void httpPUT(BufferedInputStream in, BufferedOutputStream out, String filename) {
        try {
            File file = new File(filename);
            boolean has_existed = file.exists();
            PrintWriter pw = new PrintWriter(file);
            pw.close();
            BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[256];
            while (in.available() > 0) {
                int nbRead = in.read(buffer);
                fileOut.write(buffer, 0, nbRead);
            }
            fileOut.flush();
            fileOut.close();
            if (has_existed) {
                out.write(buildResponseHeader("200", "OK", filename, file.length()).getBytes());
            } else {
                out.write(buildResponseHeader("201", "Created", filename, file.length()).getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(buildResponseHeader("500", "Internal Server Error").getBytes());
                out.flush();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }


    /**
     * HTTP method that replaces an existing resource (or creates one )
     * If file exists -> append at the end of file's data request's body message -> 200, OK
     * If file doesn't exist-> creates a new file -> 201, Created
     * exception -> 500, Internal error
     * @param in client socket input stream, to read body message
     * @param out client socket output stream, to write response
     * @param filename filepath
     */
    protected void httpPOST(BufferedInputStream in, BufferedOutputStream out, String filename) {
        try {
            File file = new File(filename);
            boolean has_existed = file.exists();
            BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file, has_existed));
            byte[] buffer = new byte[256];
            while (in.available() > 0) {
                int nbRead = in.read(buffer);
                fileOut.write(buffer, 0, nbRead);
            }
            fileOut.flush();
            fileOut.close();
            if (has_existed) {
                out.write(buildResponseHeader("200", "OK", filename, file.length()).getBytes());
            } else {
                out.write(buildResponseHeader("201", "Created", filename, file.length()).getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(buildResponseHeader("500", "Internal Server Error").getBytes());
                out.flush();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }


    /**
     * HTTP method that deletes a file given its filepath.
     * If deleted -> 204, No Content
     * If can't delete -> 403, Forbidden
     * If not found -> 404, Not Found
     * exception -> 500, Internal error
     * @param out, client socket output stream, to return header
     * @param filename, filepath
     */
    protected void httpDELETE(BufferedOutputStream out, String filename) {
        try {
            File file = new File(filename);
            if (!file.exists() || !file.isFile()) {
                out.write(buildResponseHeader("404", "Not Found").getBytes());
            } else {
                boolean deleted = file.delete();
                if (deleted) {
                    out.write(buildResponseHeader("204", "No Content", filename, file.length()).getBytes());
                } else {
                    out.write(buildResponseHeader("403", "Forbidden", filename, file.length()).getBytes());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(buildResponseHeader("500", "Internal Server Error").getBytes());
                out.flush();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }


    /**
     * HTTP method returns headers that would be returned if specified resource was requested with HTTP GET.
     * @param out, client socket output flux, to send header
     * @param filename, filepath
     */
    protected void httpHEAD(BufferedOutputStream out, String filename) {
        try {
            File file = new File(filename);
            if (!file.exists() || !file.isFile()) {
                out.write(buildResponseHeader("404", "Not Found", filename, file.length()).getBytes());
            } else {
                out.write(buildResponseHeader("200", "OK", filename, file.length()).getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write(buildResponseHeader("500", "Internal Server Error").getBytes());
                out.flush();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }


    /**
     * Start the application.
     * @param args, port number
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("WebServer <serverPort>");
        } else {
            try {
                int port = parseInt(args[0]);
                WebServer ws = new WebServer();
                ws.start(port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
