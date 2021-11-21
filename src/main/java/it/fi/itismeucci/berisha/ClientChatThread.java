package it.fi.itismeucci.berisha;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 *
 * @author Dardan Matias Berisha
 */
class ClientChatThread extends Thread{

    private Socket client;
    private ChatServer server;
    
    private BufferedReader inClient;
    private DataOutputStream outClient;
    
    private String messaggioRicevuto;
    private ArrayList<String> messaggiDaInviare;
    private String nomeClient;
    
    private boolean aperto;
    
    //COMANDI
    public static final String PREFISSO = "/";
    public static final String PREFISSO_UTENTE = "@";

    public static final String SET_NAME = "name";                     //per settare il nome dell'utente
    public static final String USER_CONNECTED = "usr_con";            //per avvertire della connessione di un utente
    public static final String USER_DISCONNECTED = "usr_dsc";         //per avvertire della disconnessione di un utente
    public static final String LIST_USERS = "list";                   //per inviare all'utente una stringa della lista degli utenti
    public static final String QUIT_CHAT = "quit";                    //per quando l'utente si vuole disconnettere
    public static final String USER_MESSAGE = "msg";                  //per inviare ad un utente un singolo messaggio(ci deve essere anche il nome utente con questo formato: /msg @nomeDestinatario)
    public static final String USER_GLOBAL_MESSAGE = "g";             //per inviare un messaggio a tutti gli utenti
    public static final String USER_MESSAGE_OFFLINE = "msg_offline";  //utilizzato solo nel caso in cui l'utente del messaggio privato non è connesso alla chat
    public static final String NAME_VALIDITY = "name_validity";
    
    public static final String COMMAND_SET_NAME =               PREFISSO + SET_NAME;                //utilizzato solo all'avvio
    public static final String COMMAND_USER_CONNECTED =         PREFISSO + USER_CONNECTED;
    public static final String COMMAND_USER_DISCONNECTED =      PREFISSO + USER_DISCONNECTED;   
    public static final String COMMAND_LIST_USERS =             PREFISSO + LIST_USERS;             
    public static final String COMMAND_QUIT_CHAT =              PREFISSO + QUIT_CHAT;              
    public static final String COMMAND_USER_MESSAGE =           PREFISSO + USER_MESSAGE;            
    public static final String COMMAND_USER_GLOBAL_MESSAGE =    PREFISSO + USER_GLOBAL_MESSAGE;  
    public static final String COMMAND_USER_MESSAGE_OFFLINE =   PREFISSO + USER_MESSAGE_OFFLINE;
    public static final String COMMAND_NAME_VALIDITY =          PREFISSO + NAME_VALIDITY;

    public ClientChatThread(Socket client, ChatServer server) {
        super();
        nomeClient = null;
        this.client = client;
        this.server = server;        
        aperto = true;
        
        messaggiDaInviare = new ArrayList<>();

        try{
            inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
        }catch(IOException ioe){System.out.println(">Thread: Errore durante la creazione del client input");}
        
        try{
            outClient = new DataOutputStream(client.getOutputStream());
        }catch(IOException ioe){System.out.println(">Thread: Errore durante la creazione del client output");}
    }

    @Override
    public void run() {
        
        while(aperto){   
            //verifica se il nome del client è settato
            if(nomeClient == null){
                try {
                    //si aspetta di riceve un comando per settare il nome
                    System.out.println("In attesa del nome");
                    String msg = inClient.readLine();
                    
                    System.out.println("Messaggio arrivato: " + msg);
                    
                    //verifica se il comando ricevuto è per settare il nome 
                    //          -si poteva anche usare il metodo per eseguire i comandi 
                    //              però potrebbe creare confusione nel caso in cui il comando 
                    //              ricevuto non è per settare il nome.
                    
                    System.out.println("Il nome è valido? " + isSetNameValid(msg));
                    
                    if(isSetNameValid(msg)){
                        
                        System.out.println("Invio nome valido all'utente " + server.trovaNomeUtente(msg));
                        
                        nomeClient = server.trovaNomeUtente(msg);
                        outClient.writeBytes(COMMAND_NAME_VALIDITY + " true" + '\n');
                    }else{
                        
                        System.out.println("Invio nome non valido");
                        
                        outClient.writeBytes(COMMAND_NAME_VALIDITY + " false" + '\n');
                    }
                    
                    
                } catch (IOException e) { System.out.println(">Thread: Errore nell'acquisizione del nome del client, chiusura..."); close();}

                //operazioni da svolgere se il nome viene settato
                if(nomeClient != null){
                    server.avvisoConnessioneUtente(nomeClient); //si avvisa gli altri utenti della connessione
                    eseguiComando(COMMAND_LIST_USERS);          //si invia la lista degli utenti al clint
                }
                
            }else{
                //invio i messaggi nella lista di messaggi da inviare
                controlloInvioMessaggio();
                
                //resta in attesa finché non arriva un messaggio
                eseguiComando(controlloArrivoMessaggio());
            }
        }
    }
    
    public boolean isSetNameValid(String msg){
        if(msg.split(" ")[0].equals(COMMAND_SET_NAME)){
            String name = msg.split(PREFISSO_UTENTE)[1];
            if(!server.isUsernameUsed(name)){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Metodo che restituisce il nome del client(o dell'user) che sta venendo servito da questo ClientChatThread.
     * @return Il nome del client
     */
    public String getNomeClient(){
        return nomeClient;
    }

    /**
     * Esegue il comando passato come parametro, se il comando non viene trovano non succede niente
     * @param msg il comando da eseguire
     */
    public void eseguiComando(String msg){
        if(msg != null){
            String comando = msg.split(" ")[0];

            switch(comando){
                case COMMAND_USER_GLOBAL_MESSAGE:   //CASO messaggio globale
                    server.messaggioGlobale(msg);
                    break;
                    
                case COMMAND_USER_MESSAGE:          //CASO messaggio verso un singolo utente
                    server.messaggioPrivato(msg, this);
                    break;
                    
                case COMMAND_LIST_USERS:            //CASO richiesta lista utenti
                    server.inviaListaUtenti(this);
                    break;

                case COMMAND_QUIT_CHAT:             //CASO client si disconnette
                    server.AvvisoDisconnessioneUtente(nomeClient);
                    close();
                    break;
                    
                default:
                    break;
            }
        }
    }

    public void close(){
        aperto = false;
        try {
            inClient.close();
            outClient.close();
            client.close();
        } catch (IOException ex) { System.out.println(">Thread: Errore nella chiusura del socket");}
    }
    
    public void controlloInvioMessaggio(){
        if(messaggiDaInviare.size() > 0){
            for(int i = 0; i < messaggiDaInviare.size(); i++){

                try {
                    outClient.writeBytes(messaggiDaInviare.remove(0) + '\n');
                } catch (IOException e) { System.out.println(">Thread: Errore nella rimozione dalla lista di messaggi, chiusura..."); close();}
            }
        }
    }

    public String controlloArrivoMessaggio(){
        try {
            return inClient.readLine();
        } catch (IOException e) { System.out.println(">Thread: Errore nell'acquisizione del messaggio, chiusura..."); close();}
        return null;
    }

    public DataOutputStream getOutClient(){
        return outClient;
    }

    public void inviaMessaggio(String messaggio){
        messaggiDaInviare.add(messaggio);
    }

    public boolean aggiungiMessaggioDaInviare(String messaggio){
        return messaggiDaInviare.add(messaggio);
    }
}