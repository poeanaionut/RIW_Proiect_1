package app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.sound.sampled.Port;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class WebCrawlerSecvential {
    private static volatile Queue<UrlInformation> urlFontier = new LinkedList<>();
    private static volatile Map<String, WebsiteInfo> domainMap = new HashMap<>();
    private static volatile HttpConnection httpConnection;
        

    public static void saveToFile(UrlInformation urlInformation) throws IOException {

        File domainDir = new File("crawl/" + urlInformation.url.getHost());
        if (!domainDir.exists()) {
            domainDir.mkdirs();
        }

        File pageFile = new File(domainDir.getAbsolutePath() + urlInformation.url.getPath());
        // este fisier
        if (urlInformation.url.getPath().contains(".")) {
            File dir = pageFile.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (pageFile.exists()) {
                urlInformation.absPath = pageFile.getAbsolutePath();
                return;
            }
            pageFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(pageFile);
            fos.write(urlInformation.textBody.getBytes());
            fos.close();
            urlInformation.absPath = pageFile.getAbsolutePath();
        } else {

            pageFile.mkdirs();
            File pageFileIndex = new File(pageFile.getAbsolutePath() + "\\index.html");
            if (pageFileIndex.exists()) {
                urlInformation.absPath = pageFile.getAbsolutePath() + "\\index.html";
                return;
            }

            pageFileIndex.createNewFile();

            FileOutputStream fos = new FileOutputStream(pageFileIndex);
            fos.write(urlInformation.textBody.getBytes());
            fos.close();
            urlInformation.absPath = pageFileIndex.getAbsolutePath();
        }
    }

    public static void getUrls(WebsiteInfo wInfo, Document doc, Queue<UrlInformation> urlFontier) throws IOException{
        Set<String> pageUrls = wInfo.getLinks(doc);

        for (String urlString : pageUrls) {
            try {
                URL url = new URL(urlString);
                // daca url-ul curent este deja vizitat
                // sau in lista de url-uri ignorate
                // sau nu foloseste protocolul http
                // il ignoram
                if (UrlSet.ignoredUrls.contains(urlString) || UrlSet.visitedUrls.contains(urlString)
                        || !url.getProtocol().equals("http")) {
                    continue;
                }
                UrlInformation currentUrlInf = new UrlInformation(url);
                urlFontier.add(currentUrlInf);
            } catch (Exception e) {
                //TODO: handle exception
            }
          
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {

        File clearFile = new File("crawl/");
        if (clearFile.exists()) {
            clearFile.delete();
        }

        long startTime = System.currentTimeMillis();
        URL urlSeed = new URL("http://riweb.tibeica.com/crawl/");

        DnsRecord dnsSeed = DNSResolver.sendRequest(urlSeed);

        if (dnsSeed == null) {
            System.out.println("DnsRecord is NULL!");
            return;
        }

        httpConnection = new HttpConnection(dnsSeed.adress, 80);

        UrlInformation urlInfRobots = new UrlInformation(new URL("http://riweb.tibeica.com/robots.txt"));
        httpConnection.sendRequest(urlInfRobots);
        saveToFile(urlInfRobots);


        UrlInformation urlInfSeed = new UrlInformation(urlSeed);

       

        if (urlInfRobots.absPath != null) {
            
            WebsiteInfo websiteInfo = new WebsiteInfo("C:\\Users\\ionut\\source\\repos\\RIW\\Proiect_1\\crawl\\riweb.tibeica.com\\", urlInfSeed.url.getHost(),
                    httpConnection.getIpAddress(), dnsSeed.cacheTime);
            websiteInfo.hasRobots = true;
            domainMap.put(urlInfSeed.url.getHost(), websiteInfo);

            urlFontier.add(urlInfSeed);


            // la inceput presupun ca am voie pe toate linkurile din domeniu
            while (!urlFontier.isEmpty() && UrlSet.visitedUrls.size() < 100) {
                UrlInformation urlInf = urlFontier.remove();

                // verific daca domeniul respectiv n-a mai fost vizitat intr-o iteratie
                // anterioara si ignorat
                if (UrlSet.ignoredUrls.contains(urlInf.url.getHost())) {
                    continue;
                }

                // verifica daca nu exista vreo intrare pentru domeniul url-ului in domainMap
                if (!domainMap.containsKey(urlInf.url.getHost())  || domainMap.get(urlInf.url.getHost()).cacheHasExpired()) {

                    if(domainMap.containsKey(urlInf.url.getHost()))
                    {
                        domainMap.remove(urlInf.url.getHost());
                    }

                    URL newDomain = new URL(urlInf.url.getProtocol(), urlInf.url.getHost(), "");
                    DnsRecord dnsRecord = DNSResolver.sendRequest(newDomain);

                    // daca serverul dns nu poate gasi domeniul
                    if (dnsRecord == null) {
                        UrlSet.ignoredUrls.add(newDomain.getHost());
                        continue;
                    }
                    UrlInformation urlRobotsInf = new UrlInformation(
                            new URL(urlInf.url.getProtocol(), urlInf.url.getHost(), "/robots.txt"));
                    // System.out.println("Url "+urlInf.url.toString());
                    // System.out.println("host " + urlInf.url.getHost());
                    // System.out.println("robotsUrl "+ urlRobotsInf.url.toString());
                    httpConnection.setIpAddress( dnsRecord.adress);
                    httpConnection.sendRequest(urlRobotsInf);
                    saveToFile(urlInfRobots);

                    // daca avem doar un url in url frontier si inca nu am epuizat incercarile
                    // il adaugam in coada si asteptam 2 secunde
                    if (!urlRobotsInf.isVisited && !urlRobotsInf.isIgnored && urlFontier.isEmpty()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // TODO: handle exception
                        }
                        urlFontier.add(urlInf);
                        continue;
                    }

                    // avem robots.txt
                    if (urlRobotsInf.absPath != null) {
                        WebsiteInfo wInfo = new WebsiteInfo(urlRobotsInf.absPath, urlRobotsInf.url.getHost(),
                                dnsRecord.adress, dnsRecord.cacheTime);
                        wInfo.hasRobots = true;
                        domainMap.put(urlRobotsInf.url.getHost(), wInfo);
                    } else {
                        // nu avem robots.txt
                        WebsiteInfo wInfo = new WebsiteInfo(urlRobotsInf.absPath, urlRobotsInf.url.getHost(),
                                dnsRecord.adress, dnsRecord.cacheTime);
                        wInfo.hasRobots = false;
                        domainMap.put(urlRobotsInf.url.getHost(), wInfo);
                        domainMap.put(urlInf.url.getHost(), wInfo);
                    }

                }

                WebsiteInfo wInfo = domainMap.get(urlInf.url.getHost());

                if (wInfo.hasRobots) {
                    // daca avem acces la fisierul respectiv
                    if (wInfo.allows(urlInf.url.getFile())) {

                        httpConnection.setIpAddress(wInfo.ipAddress);
                        httpConnection.sendRequest(urlInf);

                        // daca nu reusim sa descarcam contentul url-ului acum, incercam intr-o iteratie
                        // viitoare
                        if (urlInf.nrOfTries != 1 && !urlInf.isIgnored) {
                            urlFontier.add(urlInf);
                            continue;
                        }

                        // daca url-ul nu poate fi accesat si este ignorat
                        // il adaugam la lista de url-uri ignorate
                        if (urlInf.isIgnored) {
                            UrlSet.ignoredUrls.add(urlInf.url.toString());
                            continue;
                        }

                        // daca url-ul este vizitat si nr de url-uri vizitate este sub 100,
                        // il adaugam la lista de url-uri vizitate

                        if (urlInf.isVisited) {
                            UrlSet.visitedUrls.add(urlInf.url.toString());

                            Document doc = Jsoup.parse(urlInf.textBody,urlInf.url.toURI().toString());


                            // all - am voie sa extrag tot
                            // none - n-am voie sa extrag nimic
                            // index - pot extrage continutul pentru a-l pregati pentru functia de indexare
                            // noindex - nu pot salva local continutul acelei pagini
                            // follow - am voie sa extrag link-ruile din pagina si sa le procesez
                            // nofollow - nu am voie sa extrag link-urile din pagina curenta si sa le
                            String robots =  wInfo.getRobots(doc);
                            if(robots.contains("none"))
                            {
                                continue;
                            }else if(robots.contains("noindex"))
                            {
                                if(robots.contains("nofollow"))
                                {
                                    continue;
                                }
                                getUrls(wInfo,doc,urlFontier);     
                            }
                            else if(robots.contains("index"))
                            {
                                saveToFile(urlInf);
                            }
                            else
                            {
                                saveToFile(urlInf);
                                getUrls(wInfo, doc, urlFontier); 
                            }
                        }

                    }
                } else {
                    // nu avem robots.txt
                    // totul este permis
                    httpConnection.setIpAddress( wInfo.ipAddress);
                    httpConnection.sendRequest(urlInf);

                    // daca nu reusim sa descarcam contentul url-ului acum, incercam intr-o iteratie
                    // viitoare
                    if (urlInf.nrOfTries != 1 && !urlInf.isIgnored) {
                        urlFontier.add(urlInf);
                        continue;
                    }

                    // daca url-ul nu poate fi accesat si este ignorat
                    // il adaugam la lista de url-uri ignorate
                    if (urlInf.isIgnored) {
                        UrlSet.ignoredUrls.add(urlInf.url.toString());
                        continue;
                    }

                    // daca url-ul este vizitat si nr de url-uri vizitate este sub 100,
                    // il adaugam la lista de url-uri vizitate

                    if (urlInf.isVisited) {
                        UrlSet.visitedUrls.add(urlInf.url.toString());

                       saveToFile(urlInf);
                       Document doc = Jsoup.parse(urlInf.textBody, urlInf.url.toURI().toString());
                       getUrls(wInfo, doc, urlFontier);
                    }

                }

            }

        }
        long stopTime = System.currentTimeMillis();
        System.out.println("Timpul de procesare a 100 de fisiere:\t" + (stopTime - startTime) / 1000.0 + " secunde");

    }

   
}

// TODO: rezolvat problema cu inputStreamReader
// salvat in mongo dnsRecords - documente cu numele fisierelor si cu timpul
// cache-ului
// incarcat acestea cand pornim aplicatia
// protocol rep la nivel de pagina web

// web crawler - un fel de browser automat a carui sarcina este sa exploreze si
// sa indexeze link-uri de pe web
// pornind cu o structura de tip coada
// extragem rand pe rand cate un url din coada
// verificam daca link-ul a fost sau nu vizitat intr-o iteratie anterioara
// descarcam continutul paginii respectiva
// verificam rep protocol

// procesez

// select("a[abs:href]") link-urile in valori absolut

// daca exista tag-urile meta name = robots - urmam instructiunile
// if din pseudocod liniile 9 - 12
// daca nu exista am voie sa fac orice

// prima data se verifica robots.txt
// apoi se verifica tag-urile meta

// in extragerea link-urile
// extragem link-urile noi in format aboslut
// protocol://domeniu/cale/locala?query#anchor
// prima analiza o facem la nivel de protocol :
// daca nu este http - ignoram
// daca este http - retin tot ce este prezent de la inceputul link-ului pana la
// anchora locala, retinem si query-ul, prin query se indica pagini noi

// din set-ul de link-uri luam doar ce este nou - sa nu fi fost vizitata candva
// in istoric si sa nu fi fost vizitata in prezenta rulare

// marcarea paginilor vizitate -
// printr-un hashset care stocheaza link-urile vizitate
// creez o structura arborescenta si daca NU exista fisierul respectiv descarc
// continutul

// pentru laborator
// un contor pentru a explora 100 de pagini
// ignoram din link-uri tot ce nu contine riw.tibeica.com
// robots.txt - clauzele care se aplica pentru semnaturile robotilor web
// tipuri de clauze user-agent:name user-agent:*
// in cazul nostru user-agent:* user-agent:RIW_CRAWLER
// doua clauze suplimentare
// disallow - prefixul de cale inseamna interzis in explorare
// allow - inseamna permis in explorare
// exemplu
// User-agent: RIWEB_CRAWLER Disallow: - avem avoie peste tot
// User-agent: * Disallow: / - ceilalti roboti nu au voie deloc

// daca exista robots.txt se verifica permisiunile disallow
// pentru url-ul curent
// daca nu se permite accesul se trece la urmatorul url
// daca se permite accesul, se descarca si indexeaza url-ul
// daca se primeste un cod de eroare 301 Moved Permanently,
// se reface cererea pentru noul domeniu si se actualizeaza
// datele deja salvate
// se analizeaza tag-ul meta name=robots, pentru a vedea daca
// se pot extrage link-urilor incluse in document,
// se extrag link-urile in format aboslut si se elimina cele
// care nu respecta protocolul rep