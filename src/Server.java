import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class Server {
    public static void main (String[] args) {  
       try (ServerSocket server = new ServerSocket(8000)) {
           System.out.println("Server Started");
           
           while (true) {
               try (Socket socket = server.accept();
                        DataOutputStream writer = 
                            new DataOutputStream(
                                    socket.getOutputStream());
                        DataInputStream reader =
                            new DataInputStream (
                                    socket.getInputStream());) {
                   //��������� �������
                   while (true) {
                       String request = reader.readUTF();

                        //����������� �������
                        String[] split;
                        //���������� ������� �� ����� ��� ����������� ������
                        split = request.split("/");
                        System.out.println("request: " + request);

                        //��������� ������� �� �����������
                        switch (split[0]) {
                            //��������� ������� �� �����������
                            case ("authorize") -> {                                
                                //��������� �����
                                String authResult = CompareHash(split[1], split[2]);
                                                                
                                //�������� ������ �������
                                writer.writeUTF(authResult);
                            }
                            //��������� ������� �� �����������
                            case ("register") -> {
                                //������ ������ ������������ � ����
                                String regResult = UserSerialize(split[1], split[2], split[3]);
                                //�������� ������ �������
                                writer.writeUTF(regResult);
                            }
                            
                            //��������� ������� �� ��������� �����
                            case ("getfile") -> {
                                String fileResult = SendFile(writer, split[1]);
                                
                            }
                            case ("save") -> {
                                //������ ������ ������������ � ����
                                String fileResult = GetFileMessage(writer, reader, split[1]);
                            }
                           
                        }
                   }
               }
           }
                      
       } catch (IOException e) {
           throw new RuntimeException(e);
       }
    }
    
    
    //��������� ������ ������������ � xml-���� � ���������� D://superfiles//memmoth//users
    //���������� 200 - ���� ������� ������; 404 - ���� ��� ����������; 500 - ������
    public static String UserSerialize(String pLogin, String pUsername, String pHash) {
        File file = new File("D://superfiles//memmoth//users//" + pLogin + ".xml");
               
        try {
            System.out.println("file.exists() = " + file.exists());
            if (!file.exists() && file.createNewFile()) {
                try {
                    XMLOutputFactory output = XMLOutputFactory.newInstance();
                    Writer oswriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
                    XMLStreamWriter writer = output.createXMLStreamWriter(oswriter); 
                    writer.writeStartDocument("UTF-8", "1.0"); // ��������� XML-�������� � ����� �������� ������� ServerUsers
                    writer.writeStartElement("ServerUsers"); 

                    writer.writeStartElement("User");  // ���������� User 
                    // ��������� ��� ����                        
                    writer.writeStartElement("Login");
                    writer.writeCharacters(pLogin);
                    writer.writeEndElement();                
                    writer.writeStartElement("Username");
                    writer.writeCharacters(pUsername);
                    writer.writeEndElement();                
                    writer.writeStartElement("Hash");
                    writer.writeCharacters(pHash);
                    writer.writeEndElement();
                    writer.writeEndElement(); // ��������� ��� User

                    writer.writeEndElement();     // ��������� �������� �������            
                    writer.writeEndDocument();    // ��������� XML-��������
                    writer.flush();
                } catch (IOException | XMLStreamException e) {
                    //������ ��� ������
                    e.printStackTrace();   
                    return "500";
                }  
                return "200";
            } else {
                //������������ � ����� ������� ��� ����������
                return "404";
            }
        } catch (IOException e) {
            //������ ��� ��������
            e.printStackTrace();
            return "500";
        }
    }
    
    
    //��������� ��� ������ ������������ �� ����� � ��� �������
    public static String GetHash(Document document) throws DOMException, XPathExpressionException {
        XPathFactory pathFactory = XPathFactory.newInstance();
        XPath xpath = pathFactory.newXPath();
        XPathExpression expr = xpath.compile("ServerUsers/User/Hash/text()");
                
        Node node = (Node) expr.evaluate(document, XPathConstants.NODE);
        return node.getTextContent();
    }
    
    
    //��������� ��� ������������ �� ����� � ��� �������
     public static String GetUsername(Document document) throws DOMException, XPathExpressionException {
        XPathFactory pathFactory = XPathFactory.newInstance();
        XPath xpath = pathFactory.newXPath();
        XPathExpression expr = xpath.compile("ServerUsers/User/Username/text()");
                
        Node node = (Node) expr.evaluate(document, XPathConstants.NODE);
        return node.getTextContent();
    }

    //���������� ��� ������, ��������� ������������� ��� �����������, � �����, ���������� ��� ���� �� �������
    //����������: 200/username - ������ ������, 404 - �������� �����/������, 500 - ������
    public static String CompareHash(String pLogin, String pHash) {
        File file = new File("D://superfiles//memmoth//users//" + pLogin + ".xml");
        
        if (file.exists()) {
            try {
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(file);
                String fileHash = GetHash(document);

                //���� �������, ����������� ������ �������
                if (fileHash.compareTo(pHash) == 0) {
                    return "200/" + GetUsername(document);
                }
                else {
                    //���� �� �������, ����������� �� �������
                    return "404";
                }
            } catch (XPathException | ParserConfigurationException | SAXException | IOException e) {
               e.printStackTrace(System.out);
               return "500";
            }
        } else {
            //������������ �� ������
            return "404";
        }
    }
    
    //���������� ������� ���� �� ��� �������
    private static String SendFile(DataOutputStream writer, String login)
    {
        FileInputStream fileStream;                    //����� ��� �����
        byte[]          bytesFile = new byte[16*1024]; //������ �����, ������� �� ����������
        Long            sizeFile;                      //������ �����, ������� �� ����������
        String          needFile;                      //������, �������� �������� ������� ������� �����
        int             count;                         //���������� ��� �������� ��������

        try {
            //���� ��������� �����
            needFile = "D://superfiles//memmoth//memo//" + login + ".txt";
            
             System.out.println("Start Sending...");

            //����� ��������� �������� ��������
            File sendFile = new File(needFile);
            sizeFile      = sendFile.length();
            fileStream    = new FileInputStream(sendFile);

            //���� ���� ����������
            if (sendFile.exists()) {
               writer.writeBoolean(true);
               writer.writeLong(sizeFile);

                while ((count = fileStream.read(bytesFile)) != -1) {
                    writer.write(bytesFile, 0, count);
                    writer.flush();
                }
                System.out.println("File send");
                fileStream.close();
                return "200";
            }
            //���� ����� �� ����������
            else
            {
                System.out.println("File doesn't exists");
                writer.writeBoolean(false);
                return "500";
            }
        }
        catch(IOException i)
        {
            System.out.println(i);
            return "500";
        }
    }
    
    //��������� ����, ������������ �������
    public static String GetFileMessage(DataOutputStream writer, DataInputStream reader, String login)
    {
        FileOutputStream fileStream;                    //����� ��� �����
        Long             sizeFile;                      //������ �����, ������� �� ����������
        byte[]           bytesFile = new byte[16*1024]; //������ ��� �������� �����
        int              count;                         //���������� ��� �������� ��������
         
        try
        {
            if(reader.readBoolean() == true) //���� ���� � �� �����
            {
                //�������� �������� �����
                sizeFile = reader.readLong();
                fileStream = new FileOutputStream("D://superfiles//memmoth//memo//" + login + ".txt");  //���� ��� ������ �����
                
                while (sizeFile > 0 && (count = reader.read(bytesFile, 0, (int) Math.min(bytesFile.length, sizeFile))) != -1)
                {
                    fileStream.write(bytesFile, 0, count);
                    sizeFile -= count;
                }

                //��������� ��������� �����. �������� ��������� ������.
                fileStream.close();
                System.out.println("File was getted!");
                return "200";
            }
            else //����� ��� � �� �� �����
            {
                return "500";
            }
        }
        catch(IOException i)
        {
            System.out.println(i);
            return "500";
        }
    }
}
