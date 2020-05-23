package app;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jsoup.Jsoup;

public class WebCrawlerSecvential {

    public static void main(String[] args) throws IOException, URISyntaxException {

        URL urlSeed = new URL("http://riweb.tibeica.com/crawl/");

        DnsRecord dnsSeed = DNSResolver.sendRequest(urlSeed);

        if (dnsSeed == null) {
            System.out.println("DnsRecord is NULL!");
            return;
        }

        HttpConnection httpConnection = new HttpConnection(dnsSeed.adress);

        UrlInformation urlInfRobots = new UrlInformation(new URL("http://riweb.tibeica.com/robots.txt"));
        httpConnection.sendRequest(urlInfRobots);

        UrlInformation urlInfSeed = new UrlInformation(urlSeed);
        Map<String, WebsiteInfo> domainMap = new HashMap<>();
        Queue<UrlInformation> urlFontier = new LinkedList<>();

        if (urlInfRobots.absPath != null) {
            WebsiteInfo websiteInfo = new WebsiteInfo(urlInfSeed.absPath, urlInfSeed.url.getAuthority());
            websiteInfo.hasRobots = true;
            domainMap.put(urlInfSeed.url.getAuthority(), websiteInfo);

            urlFontier.add(urlInfSeed);

            // la inceput presupun ca am voie pe toate linkurile din domeniu
            while (!urlFontier.isEmpty()) {
                UrlInformation urlInf = urlFontier.remove();

                // verifica daca exista vreo intrare pentru domeniul url-ului in domainMap
                if (domainMap.get(urlInf.url.getAuthority()) == null) {
                    try {
                    UrlInformation urlRobotsInf = new UrlInformation(new URL(urlInf.url.getProtocol(), urlInf.url.getHost(), "/robots.txt"));
                    httpConnection.sendRequest(urlRobotsInf);

                    // daca avem doar un url in url frontier si inca nu am epuizat incercarile
                    // il adaugam in coada si asteptam 2 secunde
                    if (!urlRobotsInf.isVisited && !urlRobotsInf.isIgnored && urlFontier.isEmpty()) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            // TODO: handle exception
                        }
                        urlFontier.add(urlInf);
                        continue;
                    }

                    // avem robots.txt
                    if (urlRobotsInf.absPath != null) {
                        WebsiteInfo wInfo = new WebsiteInfo(urlRobotsInf.absPath, urlRobotsInf.url.getAuthority());
                        wInfo.hasRobots = true;
                        domainMap.put(urlRobotsInf.url.getAuthority(), wInfo);
                    } else {
                        // nu avem robots.txt
                        WebsiteInfo wInfo = new WebsiteInfo(urlRobotsInf.absPath, urlRobotsInf.url.getAuthority());
                        wInfo.hasRobots = false;
                        domainMap.put(urlRobotsInf.url.getAuthority(), wInfo);
                    }
                    } catch (Exception e) {
                        //TODO: handle exception
                        System.out.println(urlInf.url.getHost() + "/robots.txt");
                        e.printStackTrace();
                    }
                   
                }
                
                WebsiteInfo wInfo = domainMap.get(urlInf.url.getAuthority());
                if (wInfo.hasRobots) {
                    // daca avem acces la fisierul respectiv
                    if (wInfo.allows(urlInf.url.getFile())) {
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

                        if (urlInf.isVisited && UrlSet.visitedUrls.size() < 100) {
                            UrlSet.visitedUrls.add(urlInf.url.toString());

                            Set<String> pageUrls = wInfo
                                    .getLinks(Jsoup.parse(new File(urlInf.absPath), "UTF-8", urlInf.url.toString()));

                            for (String urlString : pageUrls) {
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
                            }
                        }

                    }
                } else {
                    // nu avem robots.txt
                    // totul este permis
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

                    if (urlInf.isVisited && UrlSet.visitedUrls.size() < 100) {
                        UrlSet.visitedUrls.add(urlInf.url.toString());

                        Set<String> pageUrls = wInfo
                                .getLinks(Jsoup.parse(new File(urlInf.absPath), "UTF-8", urlInf.url.toString()));

                        for (String urlString : pageUrls) {
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
                        }
                    }

                }

            }

        }

    }
}


//TODO: rezolvat problema cu inputStreamReader
// salvat in mongo dnsRecords - documente cu numele fisierelor si cu timpul cache-ului
// incarcat acestea cand pornim aplicatia
// protocol rep la nivel de pagina web

// web crawler - un fel de browser automat a carui sarcina este sa exploreze si
// sa indexeze link-uri de pe web
// pornind cu o structura de tip coada
// extragem rand pe rand cate un url din coada
// verificam daca link-ul a fost sau nu vizitat intr-o iteratie anterioara
// descarcam continutul paginii respectiva
// verificam rep protocol
// all - am voie sa extrag tot
// none - n-am voie sa extrag nimic
// index - pot extrage continutul pentru a-l pregati pentru functia de indexare
// noindex - nu pot salva local continutul acelei pagini
// follow - am voie sa extrag link-ruile din pagina si sa le procesez
// nofollow - nu am voie sa extrag link-urile din pagina curenta si sa le
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