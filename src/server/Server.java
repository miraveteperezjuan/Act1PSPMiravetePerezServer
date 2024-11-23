package server;

import java.io.*;
import java.net.*;
import java.util.*;
import model.Libro;

public class Server {
    private static List<Libro> books = new ArrayList<>();
    //En esta lista se almacena los objetos de tipo libro que son inicializados por el servidor

    Server(){ //Este es el constructor de la clase Server
        initializeBooks();
        // Cuando se crea un objeto de tipo Server, se llama al// método initializeBooks() para inicializar la lista de libros
    }

    public void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(9000);
            //Se crea un objeto ServerSocket que se encargará de escuchar las conexiones entrantes de los clientes en el puerto 8080
            System.out.println("Servidor escuchando el puerto 9000...");

            while (true) { //Mientras escuche las conexiones...
                Socket clientSocket = serverSocket.accept();
                // Cuando un cliente se conecta, este método bloquea la ejecución hasta que una conexión es aceptada.
                System.out.println("Cliente conectado.");

                //Para manejar a cada cliente de forma independiente y no bloquear el servidor, se crea un hilo (Thread) para cada cliente.
                Thread clientThread = new Thread(new ClientHandler(clientSocket, books));
                clientThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeBooks() { //Este es el método encargado de inicializar la lista de libros.
        books.add(new Libro("978-123", "Harry Potter y la piedra filosofal", "J.K. Rowling", 15.99));
        books.add(new Libro("978-456", "Harry Potter y la cámara secreta", "J.K. Rowling", 16.99));
        books.add(new Libro("978-789", "Juego de Tronos", "George R.R. Martin", 22.00));
        books.add(new Libro("978-321", "El nombre del viento", "Patrick Rothfuss", 20.00));
        books.add(new Libro("978-654", "El código Da Vinci", "Dan Brown", 18.00));
    }

}
