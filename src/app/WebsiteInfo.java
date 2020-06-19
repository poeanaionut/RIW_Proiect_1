package app;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class WebsiteInfo {
    private String websiteFolder;
    private String baseUri;
    public boolean hasRobots;
    public String ipAddress;
    public long dnsCacheExpireTime;

    WebsiteInfo(String websiteFolder, String baseUri, String ipAddress, long dnsCacheExpireTime) {
        this.websiteFolder = websiteFolder;
        this.baseUri = baseUri;
        this.ipAddress = ipAddress;
        this.dnsCacheExpireTime = dnsCacheExpireTime;
    }

    public WebsiteInfo(WebsiteInfo wInfo) {
        websiteFolder = wInfo.websiteFolder;
        baseUri = wInfo.baseUri;
        hasRobots = wInfo.hasRobots;
        ipAddress = wInfo.ipAddress;
        dnsCacheExpireTime = wInfo.dnsCacheExpireTime;
	}

	public String getWebsiteFolder() {
        return websiteFolder;
    }

    public String getBaseUri() {
        return baseUri;
    }
    
    public String getRobots(Document doc) // preia lista de robots
    {
        Element robots = doc.selectFirst("meta[name=robots]");
        String robotsString = "";
        if (robots == null) {
            // System.out.println("Nu exista tag-ul <meta name=\"robots\">!");
        } else {
            robotsString = robots.attr("content");
            // System.out.println("Lista de robots a site-ului a fost preluata!");
        }
        return robotsString;
    }

    public Set<String> getLinks(Document doc) throws IOException // preia link-urile de pe site (ancorele)
    {
        Elements links = doc.select("a[href]");
        Set<String> URLs = new HashSet<String>();
        for (Element link : links) {
            String absoluteLink = link.absUrl("href"); // facem link-urile relative sa fie absolute

            // cautam eventuale ancore in link-uri
            int anchorPosition = absoluteLink.indexOf('#');
            if (anchorPosition != -1) // daca exista o ancora (un #)
            {
                // stergem partea cu ancora din link
                StringBuilder tempLink = new StringBuilder(absoluteLink);
                tempLink.replace(anchorPosition, tempLink.length(), "");
                absoluteLink = tempLink.toString();
            }

            // nu vrem sa adaugam duplicate, asa incat folosim o colectie de tip Set
            URLs.add(absoluteLink);
        }
        // System.out.println("Link-urile de pe site au fost preluate!");
        return URLs;
    }

    public boolean allows(String file) {

        File robotsFile = new File(websiteFolder + "/robots.txt");

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(robotsFile));
            String line;

            while ((line = bufferedReader.readLine()) != null) {

                if (line.length() > 0) {
                    if (line.length() > 0) {
                        String[] rElms = line.split(": ");
                        if (rElms[0].equals("User-agent") && rElms[1].equals("RIWEB_CRAWLER")) {
                            line = bufferedReader.readLine();
                            rElms = line.split(":");
                            if (rElms[0].equals("Disallow") && rElms.length == 1) {
                                return true;
                            } else if (rElms[0].equals("Disallow") && rElms[1].equals("/")) {
                                return false;
                            }else if(rElms[0].equals("Disallow") && rElms[1].contains(file))
                            {
                                return false;
                            }
                        }
                    }
                }
            }

            //System.out.println(line);

            bufferedReader.close();
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println("Nu se poate gasi calea:\t" + robotsFile.getAbsolutePath());
        }

        // should never get here
        return false;
    }

    public boolean cacheHasExpired() {

        return System.currentTimeMillis() / 1000 > dnsCacheExpireTime;
    }

    public static void main(String[] args) throws MalformedURLException {

        URL urlSeed = new URL("http://riweb.tibeica.com/crawl/");
        UrlInformation urlInfSeed = new UrlInformation(urlSeed);
        WebsiteInfo websiteInfo = new WebsiteInfo("crawl/" + urlSeed.getHost(), urlInfSeed.url.getHost(), "",
                System.currentTimeMillis());
        websiteInfo.hasRobots = true;

        boolean allows = websiteInfo.allows("/crawl");
    }
}
