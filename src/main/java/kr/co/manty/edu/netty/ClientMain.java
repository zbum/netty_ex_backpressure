package kr.co.manty.edu.netty;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientMain {
    static boolean needToRead = true;
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 9999);
        try (InputStream is = socket.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            while(needToRead) {
                System.out.println(br.readLine());
            }
        }

        socket.close();

    }
}
