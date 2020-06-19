package app;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class DNSResolver {
    private static final byte[] ip = {(byte)192,(byte)168,(byte)0,(byte)1};
    private static final int port = 53;

    static void processError(int errorCode, String url) {
        System.out.println(url);
        switch (errorCode) {
            case 1:
                
                System.out.println("Format error - the name server was unable to interpret the querry");
                break;
            case 2:
                System.out.println(
                        "Server failure - The name server was unable to process this query due to a problem with the name server.");
                break;
            case 3:
                System.out.println("Name error - domain name referenced in the query does not exist.");
                break;
            case 4:
                System.out.println("Not Implemented - The name server does not support the requested kind of query");
                break;
            case 5:
                System.out.println(
                        "Refused - The name server refuses to perform the specified operation for policy reasons");
                break;
            default:
                break;
        }
        System.out.println();
    }

    // afisam request-ul/ response-ul
    static void print(byte[] record) {
        // response are mereu 512 bytes
        int lines = record.length >> 4;
        for (int j = 1; j <= lines; ++j) {
            System.out.print(j + ":\t");
            for (int i = 0; i < 16; ++i) {
                System.out.print(String.format("%02X\t", record[i * j]));
            }
            System.out.println();
        }
    }

    // functie care returneaza numele de domeniu
    // merge pana cand response[idx] == 0
    static String getName(int index, byte[] response) {
        if ((response[index] & 0xFF) == 0x0) // cat timp nu am ajuns la octetul terminator de nume
        {
            return "";
        }

        if ((response[index] & 0xFF) >= 192) // iar am gasit pointer
        {
            // calculam indicele de octet
            index = ((response[index] & 0x3F) << 8) | (response[index + 1] & 0xFF);
            return getName(index, response);
        }

        // am ajuns pe dimensiune de particula, atunci construim sirul de caractere
        int currentNumberOfCharacters = response[index++] & 0xFF;
        StringBuilder currentElement = new StringBuilder();
        for (int i = 0; i < currentNumberOfCharacters; ++i) // preluam cate o parte de particula
        {
            currentElement.append((char) (response[index + i] & 0xFF));
        }

        // trecem la elementul urmator (daca exista)
        index += currentNumberOfCharacters;
        return (currentElement.toString() + "." + getName(index, response));
    }

    // parsam raspunsul primit de la server
    static DnsRecord parseResponse(int idx, byte[] response) {

        // parsam sectiunea anwser
        // formata din Name, Clasa, Tip TTL, RD_LENGTH, RDATA
        List<DnsRecord> rDnsRecords = new LinkedList<DnsRecord>();
        while (response[idx + 1] != 0x00) {
            StringBuilder sb = new StringBuilder();
            sb.append("Name:\t");
            String name = getName(idx + 1, response);

            sb.append(name.substring(0, name.length() - 1));
            sb.append("\n");
            sb.append("Type:\t");
            idx += 2;
            int type = response[idx + 1] << 8 | response[idx + 2];
            idx = idx + 2;
            sb.append(type);
            sb.append("\n");
            sb.append("Clasa:\t");
            int clasa = response[idx + 1] << 8 | response[idx + 2];
            idx += 2;
            sb.append(clasa);
            sb.append("\n");

            sb.append("TTL:\t");
            int ttl = response[idx + 1] << 24 | response[idx + 2] << 16 | response[idx + 3] << 8
                    | (response[idx + 4] & 0xff);
            idx += 4;
            long cacheTime = ttl + System.currentTimeMillis()/1000;
            sb.append(cacheTime);
            sb.append("\n");

            sb.append("RDL:\t");
            int rdl = response[idx + 1] << 8 | response[idx + 2];
            idx += 2;
            sb.append(rdl);
            sb.append("\n");

            sb.append("RDATA:\t");
            StringBuilder adress = new StringBuilder();
            for (int i = 1; i <= rdl; ++i) {
                adress.append((response[idx + i] & 0xff) + ".");
            }
            adress.setLength(adress.length() - 1);
            sb.append(adress.toString());
            idx += rdl;

            sb.append("\n");

            DnsRecord dnsRecord = new DnsRecord(name, Integer.toString(type), Integer.toString(clasa),
                    adress.toString(), cacheTime);
            rDnsRecords.add(dnsRecord);
        }
        if (rDnsRecords.size() > 0) {
            return rDnsRecords.stream().filter(r -> r.type.equals("1")).collect(Collectors.toList()).get(0);
        } else {
            return null;
        }
    }

    static DnsRecord sendRequest(URL url) throws IOException {

        final byte request[] = new byte[100];
        final byte response[] = new byte[512];

        //long startTime = System.currentTimeMillis();
        // dns header - 12 bytes
        // message id 16 biti - este copiat de catre server in raspuns
        request[0] = 0x00;
        request[1] = 0x01;

        // querry-response 1 bit 0 - cerere, 1 - reaspuns
        // opcode - 4 bits - descrie operatiunea, este copiat de catre server in raspuns
        // aa - 1 bit - setat de catre server pentru a indica tipul entitatii care a
        // generat raspunsul autoritate sau nu
        // tc - 1 bit - truncation flag, cam setatde catre server pentru a indica daca
        // mesajul a fost trunchiat sau nu
        // rd - 1 bit - recursion desired - setat de catre client
        request[2] = (byte)0x01;

        // ra - 1 bit - recursion available - setat de catre server
        // z - 3 biti - zero, camp cu valoare implicita zero
        // rc - 4 biti - response code , camp setat pe 0 de catre client
        request[3] = 0x00;

        // qdcount 16 biti - question count, camp setat de catre client, specifica
        // numarul de intrebari
        request[4] = 0x00;
        request[5] = 0x01;

        // ancount 16 biti - answer count, camp setat de catre server, numarul de
        // raspunsuri
        request[6] = 0x00;
        request[7] = 0x00;

        // nscount 16 biti - numarul de intrari pentru sectiunea autoritate din raspuns,
        // setat de server
        request[8] = 0x00;
        request[9] = 0x00;

        // arcount 16 biti - aditional record, setat de server, numarul de intrari
        // corespunzatoare sectiunii aditional din raspuns
        request[10] = 0x00;
        request[11] = 0x00;

        // dns question section
        // marcam urmatorul byte pe care putem scrie
        int idx = 13;

        String domain = url.getAuthority();
        // parsam nnumele domeniului si il punem in request
        int k = 0;
        for (int i = 0; i <= domain.length(); ++i) {
            if (i == domain.length() || domain.charAt(i) == '.') {
                request[idx - k - 1] = (byte) k;
                k = 0;
                idx++;
            } else {
                k++;
                request[idx++] = (byte) domain.charAt(i);
            }
        }
        // marcam finalul numelui de domeniu
        request[idx] = 0x00;

        // qtype 16 biti - tipul de adresa ceruta in raspuns, 1 -> ipv4, 28 -> ipv6
        request[idx++] = 0x00;
        request[idx++] = 0x01;

        // qclass 16 biti - tipul de clasa ceruta internet -> 1, csnet ->2, all ->0xff,
        // any ->0xff
        request[idx++] = 0x0;
        request[idx++] = 0x1;

        DatagramSocket datagramSocket = new DatagramSocket();

        datagramSocket.setSoTimeout(3000);

        InetAddress IP = InetAddress.getByAddress(ip);
        DatagramPacket requestPacket = new DatagramPacket(request, idx, IP, port);

        datagramSocket.send(requestPacket);

        DatagramPacket responsePacket = new DatagramPacket(response, response.length, IP, port);
        datagramSocket.receive(responsePacket);

        datagramSocket.close();

        if ((response[3] & 0x0f) != 0x00) {
            {
                processError(response[3] & 0x0f, url.toString());
                return null;
            }
        } else { // request succesfully processed

            // check anwser count
            int answerCount = (response[6] << 8) | response[7];
            if (answerCount == 0) {
                System.out.println( url.toString() + "\tNo answer from server! End execution\n");
                return null;
            }

            DnsRecord dnsRecord = parseResponse(idx - 1, response);
            System.out.println(dnsRecord.toString());

            //long stopTime = System.currentTimeMillis();
            //System.out.println("DnsTime:\t" + (stopTime - startTime) / 1000.0);

            return dnsRecord;
        }

    }

    public static void main(final String[] args) throws SocketException {

        try {
            sendRequest(new URL("www.tuiasi.ro"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}