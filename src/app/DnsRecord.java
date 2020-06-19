package app;

public class DnsRecord {
    public String domain;
    public String type;
    public String clasa;
    public String adress;
    public long cacheTime;

    DnsRecord(String Domain, String Type, String Clasa, String Adress, long CacheTime) {
        domain = Domain;
        type = Type;
        cacheTime = CacheTime;
        adress = Adress;
        clasa = Clasa;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name:\t");
        sb.append(domain.substring(0, domain.length() - 1));
        sb.append("\n");
        sb.append("Type:\t");
        sb.append(type);
        sb.append("\n");
        sb.append("Class:\t");
        sb.append(clasa);
        sb.append("\n");
        sb.append("Adress:\t");
        sb.append(adress);
        sb.append("\n");
        sb.append("CacheExpireTime:\t");
        sb.append(Long.toString(cacheTime));
        sb.append("\n");
        return sb.toString();
    }
}