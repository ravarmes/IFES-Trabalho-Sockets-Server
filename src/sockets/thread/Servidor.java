/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sockets.thread;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Servidor {

    // Estruturas de dados em memória
    private static List<ContadorGrupo> contadores = new ArrayList<>();
    private static List<LogGrupo> logs = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        inicializarGrupos();
        
        ServerSocket servidor = new ServerSocket(12345);
        System.out.println("Servidor iniciado na porta 12345");
        System.out.println("Aguardando conexões...");
        
        while (true) {
            Socket socket = servidor.accept();
            System.out.println("Um cliente se conectou");
            ThreadSockets thread = new ThreadSockets(socket);
            thread.start();
        }
    }

    private static void inicializarGrupos() {
        // Inicializar todos os grupos com 0 utilizações
        String[] nomesGrupos = {
            "Financeiro", "Apostas", "Fotografia", "Corrida", "Aulas",
            "Treinamentos", "Pets", "Viagens", "Blackjack", "RPG"
        };
        
        for (int i = 0; i < nomesGrupos.length; i++) {
            contadores.add(new ContadorGrupo(i + 1, nomesGrupos[i], 0));
        }
        
        System.out.println("Grupos inicializados:");
        for (ContadorGrupo contador : contadores) {
            System.out.println(contador);
        }
    }

    private static class ThreadSockets extends Thread {

        private final Socket clienteSocket;

        public ThreadSockets(Socket s) {
            this.clienteSocket = s;
        }

        public void run() {
            System.out.println("Thread iniciada: " + Thread.currentThread().getName());
            
            try {
                // Entrada de Dados no Servidor
                ObjectInputStream entrada = new ObjectInputStream(clienteSocket.getInputStream());
                Integer idGrupo = (Integer) entrada.readObject();
                System.out.println("ID do grupo solicitado: " + idGrupo);

                // Validar ID do grupo
                if (idGrupo < 1 || idGrupo > 10) {
                    System.out.println("ID de grupo inválido: " + idGrupo);
                    clienteSocket.close();
                    return;
                }

                // Incrementar contador do grupo e adicionar log
                synchronized (contadores) {
                    ContadorGrupo contador = contadores.get(idGrupo - 1);
                    contador.incrementarUtilizacoes();
                    
                    // Criar timestamp
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String timestamp = formatter.format(new Date());
                    
                    // Adicionar log
                    LogGrupo log = new LogGrupo(idGrupo, timestamp);
                    logs.add(log);
                    
                    System.out.println("Grupo " + contador.getNomeGrupo() + " utilizado. Total: " + contador.getQuantidadeUtilizacoes());
                    System.out.println("Log adicionado: " + log);
                }

                // Saída de Dados do Servidor
                ObjectOutputStream saida = new ObjectOutputStream(clienteSocket.getOutputStream());
                
                // Enviar lista completa de contadores (ranking)
                List<ContadorGrupo> rankingCompleto = new ArrayList<>(contadores);
                saida.writeObject(rankingCompleto);
                
                // Enviar logs filtrados do grupo solicitante
                List<LogGrupo> logsFiltrados = logs.stream()
                    .filter(log -> log.getIdGrupo() == idGrupo)
                    .collect(Collectors.toList());
                saida.writeObject(logsFiltrados);
                
                System.out.println("Dados enviados para o cliente");
                
                clienteSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Erro na thread: " + e.toString());
                e.printStackTrace();
            }
        }
    }
}
