package app;

import java.net.URL;

public class UrlInformation {
    public URL url;
    public String absPath;
    public int nrOfTries;
    public boolean isIgnored;
    public boolean isVisited;
    public String textBody;

    public UrlInformation(URL Url) {
        url = Url;
        isIgnored = false;
        nrOfTries = 0;
        isVisited = false;
        textBody = "";
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("url:");
        sb.append(url.toString());
        sb.append(" ");
        sb.append("nrOfTries");
        sb.append(nrOfTries);
        sb.append("\n");
        
        return sb.toString();
    }
}
