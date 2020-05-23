package app;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class HttpConnection {

    private static final String STATUS_CODE = "Status_Code";
    private static final String PROTOCOL = "Protocol";
    private static final String LOCATION = "Location";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String CONNECTION = "Connection";
    private static final String LAST_MODIFIED = "Last-Modified";
    private static final String DATE = "Date";

    private static final int BUFF_MAX_SIZE = 8 * 30000;

    private final int port = 80;
    private String ipAddress;
    public static final int MAX_TRIES = 5;
    private static Logger logger;
    private static FileHandler fileHandler;

    public HttpConnection(String IpAddress) throws SecurityException, IOException {
        ipAddress = IpAddress;
        fileHandler = new FileHandler("httpConnection.log", true);
        logger = Logger.getLogger("HTTP_CONNECTION");
        logger.addHandler(fileHandler);
    }

    public void saveToFile(UrlInformation urlInformation, byte[] body) throws IOException {

        File domainDir = new File("crawl/" + urlInformation.url.getAuthority());
        if (!domainDir.exists()) {
            domainDir.mkdir();
        }

        File pageFile = new File(domainDir.getAbsolutePath() + urlInformation.url.getPath());
        // daca resursa este director/fisier
        // este fisier
        if (urlInformation.url.getPath().contains(".")) {
            if (pageFile.exists()) {
                urlInformation.absPath = pageFile.getAbsolutePath();
                return;
            }
            pageFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(pageFile);
            fos.write(body);
            fos.close();
            urlInformation.absPath = pageFile.getAbsolutePath();
        } else {

            pageFile.mkdir();
            File pageFileIndex = new File(pageFile.getAbsolutePath() + "\\index.html");
            if (pageFileIndex.exists()) {
                urlInformation.absPath = pageFile.getAbsolutePath() + "\\index.html";
                return;
            }

            pageFileIndex.createNewFile();

            FileOutputStream fos = new FileOutputStream(pageFileIndex);
            fos.write(body);
            fos.close();
            urlInformation.absPath = pageFileIndex.getAbsolutePath();
        }
    }

    // trimite cererea catre un link
    public void sendRequest(UrlInformation UrlInformation) throws IOException {

        StringBuilder sb = new StringBuilder();

        // link format http://domain/resource
        String authority = UrlInformation.url.getAuthority();
        String resource = UrlInformation.url.getPath();

        sb.append("GET");
        sb.append(" ");
        sb.append(resource);
        sb.append(" ");
        sb.append("HTTP/1.1");
        sb.append("\r\n");
        sb.append("HOST: ");
        sb.append(authority);
        sb.append("\r\n");
        sb.append("Accept: text/html");
        sb.append("\r\n");
        sb.append("User-Agent: RIWEB_CRAWLER");
        sb.append("\r\n");
        sb.append("\r\n");

        InetAddress IP = InetAddress.getByName(ipAddress);
        Socket socket = new Socket(IP, port);
        OutputStream outputServer = socket.getOutputStream();
        outputServer.write(sb.toString().getBytes());

        BufferedInputStream bufferedInputStream = new BufferedInputStream(socket.getInputStream());

        // TODO: tratat cazul in care nu primesc niciun raspuns de la server 
        sb.setLength(0);
        // read the response header
        // trebuie sa gasesc 2 \r\n consecutive
        // sadds \r\n
        // \r\n
        int read = bufferedInputStream.read();
        int i=0;
        byte[] header = new byte[512];
        header[0]=(byte)read;
        while (read != -1) {
        
            read = bufferedInputStream.read();
            if(read==13)
            {
                continue;
            }
            header[i] =(byte) read;

            if(header.length>4 && header[i]==0x0A && header[i-1] ==0x0A)
            {
                break;
            }
            ++i;
           
        }


        header[i]=0;
        header[i-1]=0;
        header[i-2]=0;
        header[i-3]=0;
        String responseHeader = new String(header);

        Map<String, String> responseHeaderMap = parseResponseHeader(responseHeader);
        int statusCode = Integer.parseInt(responseHeaderMap.get(STATUS_CODE));


        int messageLength = Integer.parseInt(responseHeaderMap.get(CONTENT_LENGTH));
        byte[] body = new byte[messageLength];
        int readBytes = bufferedInputStream.read(body, 0, messageLength);
        System.out.println("Link:\t" + UrlInformation.url.toString() + "\nBytesRead:\t" + readBytes
                + "\tMessageLength:\t" + messageLength + "\n");

        switch (statusCode) {
            // eroare server
            // incerc din nou intr-o iteratie ulterioare, de maximi 5 ori
            case 500:
                UrlInformation.nrOfTries++;
                if (UrlInformation.nrOfTries == MAX_TRIES) {
                    UrlInformation.isIgnored = true;
                }
                break;

            // eroare client
            // ignor link-ul
            // 401, 403 si 405 le ignor din prima
            case 401: // unauthorized
            case 402: // payment request
            case 403: // forbidden
            case 404: // not dound
            case 405: // method not allowed
                UrlInformation.isIgnored = true;
                break;
            case 400: // bad request
                UrlInformation.nrOfTries++;
                if (UrlInformation.nrOfTries == MAX_TRIES) {
                    UrlInformation.isIgnored = true;
                    logger.info("400 Bad Request" + UrlInformation.toString());
                }
                break;

            case 408: // request timeout
                UrlInformation.nrOfTries++;
                if (UrlInformation.nrOfTries == MAX_TRIES) {
                    UrlInformation.isIgnored = true;
                    logger.info("408 Request Timeout" + UrlInformation.toString());
                }

                // redirect 301 - moved permanently, 302 - Found, temporary redirect, 308
                // se reincearca pe loc
            case 301: // moved permanently
            case 308: // permanent redirect
                UrlInformation.nrOfTries++;
                if (UrlInformation.nrOfTries == MAX_TRIES) {
                    UrlInformation.isIgnored = true;
                    break;
                } {

                UrlInformation.url = new URL(responseHeaderMap.get(LOCATION));
                sendRequest(UrlInformation);
            }
                break;

            //
            case 302: // found
            case 307: // remporary redirect
                UrlInformation.nrOfTries++;
                if (UrlInformation.nrOfTries == MAX_TRIES) {
                    UrlInformation.isIgnored = true;
                    break;
                }

                UrlInformation newUrlInformation = new UrlInformation(new URL(responseHeaderMap.get(LOCATION)));
                sendRequest(newUrlInformation);

                // daca salvarea fisierului s-a realizat cu succes, actualizez datele in
                // urlInformation
                if (newUrlInformation.isVisited) {
                    UrlInformation.isIgnored = newUrlInformation.isVisited;
                    UrlInformation.nrOfTries = newUrlInformation.nrOfTries;
                    UrlInformation.absPath = newUrlInformation.absPath;
                }
                break;

            // e ok, tratez cererea, salvez fisierul si returnez un obiect
            case 200:
                UrlInformation.nrOfTries = 1;
                UrlInformation.isVisited = true;
                saveToFile(UrlInformation, body);
                break;
            default:
                break;
        }

        socket.close();
    }

    private Map<String, String> parseResponseHeader(String responseHeader) {
        Map<String, String> responseHeaderMap = new HashMap<>();
        String[] headerElements = responseHeader.split("\n");

        String protocol = headerElements[0].split(" ")[0];
        responseHeaderMap.put(PROTOCOL, protocol);

        String statusCode = headerElements[0].split(" ")[1];
        responseHeaderMap.put(STATUS_CODE, statusCode);

        for (int i = 1; i < headerElements.length; ++i) {

                String[] headerLine = headerElements[i].split(": ");
                responseHeaderMap.put(headerLine[0], headerLine[1]);
                System.out.println(headerLine[0] + " " + headerLine[1]);
        }

        return responseHeaderMap;
    }

    public static void main(String[] args) {

        HttpConnection httpConnection;
        long startTime = System.currentTimeMillis();
        try {
            httpConnection = new HttpConnection("67.207.88.228");
            try {
                httpConnection
                        .sendRequest(new UrlInformation(new URL("http://riweb.tibeica.com/crawl/pyapi-filter.html")));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (SecurityException | IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        System.out.println("Page time:\t" + (stopTime - startTime) / 1000.0 + " secunde");

    }
}
