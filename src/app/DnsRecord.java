package app;

public class DnsRecord {
    public String name;
    public String type;
    public String clasa;
    public String adress;
    public long cacheTime;

    DnsRecord(String Name, String Type, String Clasa, String Adress, long CacheTime) {
        name = Name;
        type = Type;
        cacheTime = CacheTime;
        adress = Adress;
        clasa = Clasa;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name:\t");
        sb.append(name.substring(0, name.length() - 1));
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
        sb.append("CacheTime:\t");
        sb.append(Long.toString(cacheTime));
        sb.append("\n");
        return sb.toString();
    }
}