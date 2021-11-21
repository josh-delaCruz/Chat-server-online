package it.fi.itismeucci.berisha;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Server della chat che servirà gli utenti(client) che si connettono
 * @author Dardan Matias Berisha
 */
public class ChatServer {
    
    private ServerSocket server;
    private ArrayList<ClientChatThread> clients;
    private int port;
    private int clientLimit;
    
    public ChatServer(int port) {
        this.port = port;
        clients = new ArrayList<>();
        this.clientLimit = -1;
    }
    
    public ChatServer(int port, int clientsLimit) {
        this.port = port;
        clients = new ArrayList<>();
        this.clientLimit = clientsLimit;
    }
    
    public ArrayList<ClientChatThread> startServer(){
        try {
            System.out.println("Inizializzazione server");
            server = new ServerSocket(port);
        } catch (IOException ioe) {System.out.println(">Errore durante l'inizializzazione del server socket sulla porta " + port);}
        
        System.out.println("In attesa di client...");
        waitForClients(clientLimit);
        
        return clients;
    }
    
    public void waitForClients(int clientsLimit){
        if(clientsLimit < 0){
            while(true){
                
                addClient();
                System.out.println("Arrivato client");
            }
        }else{
            for(int i = 0; i < clientsLimit; i++){
                addClient();
                System.out.println("Arrivato client");
            }
        }
    }
    
    public void addClient(){
        try {
            clients.add(new ClientChatThread(server.accept(), this));
        } catch (IOException ex) { System.out.println(">Errore durante la tentata connessione di un client");}
        
        clients.get(clients.size()-1).start();
    }

    public void avvisoConnessioneUtente(String nomeUtente){
        messaggioGlobale(ClientChatThread.COMMAND_USER_CONNECTED + " " + ClientChatThread.PREFISSO_UTENTE + nomeUtente);
    }

    public void AvvisoDisconnessioneUtente(String nomeUtente){
        messaggioGlobale(ClientChatThread.COMMAND_USER_DISCONNECTED + " " + ClientChatThread.PREFISSO_UTENTE + nomeUtente);
    }

    public void messaggioGlobale(String messaggio){
        String mittente = trovaNomeUtente(messaggio);
        
        for(int i = 0; i < clients.size(); i++){
            //se il client non è il mittente
            if(!clients.get(i).getNomeClient().equals(mittente)){
                
                clients.get(i).aggiungiMessaggioDaInviare(messaggio);
            }
            
        }
    }
    
    public void messaggioPrivato(String messaggio, ClientChatThread mittente){
        String nome = trovaNomeUtente(messaggio);

        ClientChatThread destinatario = trovaUtente(nome);

        if(destinatario != null) destinatario.aggiungiMessaggioDaInviare(messaggio);
        else mittente.aggiungiMessaggioDaInviare(ClientChatThread.COMMAND_USER_MESSAGE_OFFLINE + " " + ClientChatThread.PREFISSO_UTENTE + mittente.getNomeClient());
    }
    
    /**
     * Il ClientChatThread invierà il comando per la lista degli user al proprio client che sta servendo
     * @param user Thread che invierà la lista degli utenti al proprio client
     */
    public void inviaListaUtenti(ClientChatThread user){
        
        String lista = ClientChatThread.COMMAND_LIST_USERS + " ";
        
        for (ClientChatThread c : clients) {
            lista += c.getNomeClient()+ ",";
        }
        
        user.aggiungiMessaggioDaInviare(lista);
    }
    
    /**
     * Cerca il thread che server l'utente col nome passato per parametro
     * @param nome Stringa nome del client che si sta cercando
     * @return ClintChatThread che server il client cercato
     */
    public ClientChatThread trovaUtente(String nome){
        for(int i = 0; i < clients.size(); i++){
            if(clients.get(i).getNomeClient().equals(nome)) return clients.get(i);
        }
        return null;
    }

    public boolean isUsernameUsed(String name){
        for (ClientChatThread c : clients) {
            if(c.getNomeClient() != null){  //se il nome è null significa che non è stato settato
                if(c.getNomeClient().equals(name)){
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Trova il nome utente in un messaggio
     * @param messaggio messaggio in cui si trova un nome utente
     * @return nome utente
     */
    public String trovaNomeUtente(String messaggio){
        return messaggio.split(" ")[1].substring(1);
    }
    
    public ServerSocket getServer() {
        return server;
    }

    public ArrayList<ClientChatThread> getClients() {
        return clients;
    }

    public int getClientLimit() {
        return clientLimit;
    }
}