package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import model.Libro;

public class ClientHandler implements Runnable {

    private Socket clientSocket; //Es el socket asociado al cliente que está conectado al servidor.
    private List<Libro> libros; //Es una lista de objetos Libro que contiene todos los libros disponibles en el servidor.
    private static final Lock lock = new ReentrantLock(); //Es un bloqueo (lock) que se usa para asegurar que solo un cliente pueda agregar un libro a la vez.

    //El constructor recibe el socket del cliente y la lista de libros del servidor.
    public ClientHandler(Socket clientSocket, List<Libro> books) {
        this.clientSocket = clientSocket;
        this.libros = books;
    }

    @Override
    public void run() { // Este es el método principal que se ejecutará en el hilo del cliente.
        try  {
            receiveAndSendResponse(); // Este método es el encargado de leer las solicitudes del cliente y enviar respuestas.
        } catch (IOException e) {
            System.out.println("Error en la conexión: " + e.getMessage());
        } finally {
            try {
                clientSocket.close(); //Se cierra el socket
                System.out.println("Cliente desconectado.");
            } catch (IOException e) {
                System.out.println("Error al cerrar el cliente con el socket: " + e.getMessage());
            }
        }
    }

    // Este método es el encargado de leer las solicitudes del cliente y enviar respuestas.
    private void receiveAndSendResponse() throws IOException {
        String request;
        boolean salir = false;
        BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);

        //El servidor entra en un bucle donde lee las solicitudes del cliente.
        while ((request = input.readLine()) != null && !(salir = request.startsWith("SALIR"))) {
            if (request.startsWith("CHECK_ISBN:")) {
                findBookByISBN(request, output);
            } else if (request.startsWith("CHECK_TITLE:")) {
                findBookByTitle(request, output);
            } else if (request.startsWith("CHECK_BOOKS_BY_AUTHOR:")) {
                findBooksByAuthor(request, output);
            } else if (request.startsWith("TRY_ADD_BOOK:")) {
                addBook(request, output, input);
            } else {
                output.println("Respuesat invalida.");
            }
            output.println("END_RESPONSE"); //Es el generico para todos.
        }
        if (salir) {
            System.out.println("Saliendo");
        }
    }

    //Este método busca un libro en la lista por su ISBN.
    private void findBookByISBN(String request, PrintWriter output) {
        Libro elegir = null;
        // Se obtiene el ISBN de la solicitud, eliminando la parte "CHECK_ISBN:" de la cadena.
        String isbn = request.substring("CHECK_ISBN:".length());

        // Si el ISBN del libro en la lista coincide con el ISBN proporcionado, se asigna ese libro a 'elegir'.
        for (Libro libro : libros) {
            if (libro.getIsbn().equals(isbn)) {
                elegir = libro;
                break; // Se sale del bucle ya que se ha encontrado el libro.
            }
        }

        // Si se encontró el libro (es decir, 'elegir' no es null), se imprime la información del libro.
        output.println(elegir != null ? elegir.toString() : "Libro no encontrado.");
    }

    private void findBookByTitle(String request, PrintWriter output) {
        Libro elegir = null;
        // Se obtiene el ISBN de la solicitud, eliminando la parte "CHECK_ISBN:" de la cadena.
        String title = request.substring("CHECK_TITLE:".length());

        for (Libro libro : libros) {
            // Si el ISBN del libro en la lista coincide con el ISBN proporcionado, se asigna ese libro a 'elegir'.
            if (libro.getTitulo().equalsIgnoreCase(title)) {
                elegir = libro;
                break; // Se sale del bucle ya que se ha encontrado el libro.
            }
        }
        // Si se encontró el libro (es decir, 'elegir' no es null), se imprime la información del libro.
        output.println(elegir != null ? elegir.toString() : "Book not found.");
    }

    // Este método busca todos los libros de un autor específico.
    private void findBooksByAuthor(String request, PrintWriter output) {
        // Se obtiene el nombre del autor desde la solicitud, eliminando la parte "CHECK_BOOKS_BY_AUTHOR:"
        // y eliminando los espacios extra al principio y al final.
        String autor = request.substring("CHECK_BOOKS_BY_AUTHOR:".length()).trim();

        List<Libro> booksByAuthor = libros.stream()
                .filter(book -> book.getAutor().equalsIgnoreCase(autor))
                .collect(Collectors.toList());
                // Los libros que cumplen la condición se recogen en una lista nueva.
        if (booksByAuthor.isEmpty()) {
            output.println("No hay libros de " + autor);
        } else {
            for (Libro libro : booksByAuthor) {
                output.println(libro.toString());
                // Se imprime la información del libro utilizando el método 'toString()' de la clase 'Libro'.
            }
        }
    }

    private void addBook(String request, PrintWriter output, BufferedReader input) {
        // Se intenta bloquear el acceso al recurso de la lista de libros para evitar que otro cliente modifique la lista al mismo tiempo.
        boolean locked = lock.tryLock();

        // Si el bloqueo no se obtiene (es decir, otro cliente está agregando un libro), se informa al cliente y se termina la operación.
        if (!locked) {
            output.println("El servidor está ocupado agregando un libro por otro cliente. Por favor, espere e intente nuevamente");
            return;
        }

        output.println("Listo para añadir un nuevo libro");
        output.println("END_RESPONSE");
        String bookRequest = null;
        try {
            // El servidor lee la solicitud del cliente con los datos del libro.
            bookRequest = input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            output.println("Error al recibir los datos");
            return;
        }

        // Si la solicitud contiene los datos correctos para agregar un libro, procesamos la información.
        if (bookRequest != null && bookRequest.startsWith("ADD_BOOK_REQUEST:")) {
            try {
                // Se extraen los datos del libro de la solicitud, separando por comas.
                String[] datos = bookRequest.substring("ADD_BOOK_REQUEST:".length()).split(",");

                // Si los datos del libro son correctos, se agrega el libro a la lista.
                if (datos.length == 4) {
                    String isbn = datos[0];
                    String titulo = datos[1];
                    String autor = datos[2];
                    double precio = Double.parseDouble(datos[3]);

                    // Se agrega el libro a la lista de libros.
                    libros.add(new Libro(isbn, titulo, autor, precio));
                    output.println("Libro añadido con éxito");
                } else {
                    output.println("No se puede agregar el libro");
                }
                // Después de procesar la solicitud, liberamos el bloqueo para que otros clientes puedan acceder a la lista de libros.
            } finally {
                lock.unlock();
            }
        }
    }
}