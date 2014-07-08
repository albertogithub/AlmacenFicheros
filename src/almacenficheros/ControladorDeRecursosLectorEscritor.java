package almacenficheros;

/**
 * A esta clase se le da un recurso que permite usarlo como lectura y escritura.
 * Proporciona el recuros a un numero ilimitado de lectores pero solo a un escritor.
 * Cuando el escritor pide el recurso debe esperar a que terminen los lectores.
 * Cuando el escritor pide el recurso no se deja pasar a ningún lector
 * 
 * Esta clase depende del buen hacer del que la use. 
 * Es decir, no se debera acceder al recurso una vez que se haya llamado al metodo que informa de que ya no lo va a usar más.
 * 
 * @author Alberto
 */
public class ControladorDeRecursosLectorEscritor {
    
    // ATRIBUTOS ---------------------------------------------------------------
    private Object recurso        = null;
    private int    num_lectores   =    0;
    private int    num_escritores =    0;
    
    private final Object lock_recurso        = new Object();
    private final Object lock_num_lectores   = new Object();                    //Se usa como cola de dormidos para escritores que estan esperando a que el recurso quede vacio.
    private final Object lock_num_escritores = new Object();                    //Se usa como cola de dormidos para lectores   que estan esperando a que un escritor termine.     Tambien se usa como cola de dormidos para escritores que estan esperando a que otro escritor termine.
    
    // CONSTRUCTORES -----------------------------------------------------------
    
    /**
     * Crea un controlador para un recurso.
     * Realmente no hace falta que esta clase tenga una referencia al recurso pues el objetivo es bloquear o dejar pasar.
     * Una vez que se deja pasar lo que se haga ya no depende de esta clase.
     * 
     * @param recurso El recurso a gestionar, el que puede ser usado por mucho en modo lectura pero solo por uno en modo escritura.
     */
    public ControladorDeRecursosLectorEscritor(Object recurso){
    /*BEGIN*/
        this.recurso = recurso;
    }/*EMD ControladorDeRecrusoLectorEscritor*/

    
    /**
     * Lo primero es esperar en la puerta de escritores (lock_num_escritores), por si hay un escritor accediendo o queriendo acceder.
     * Haber pasado la puerta de los escritores (lock_num_escritores) no quiere decir que no haya escritores trabajando, solo quiere decir que no esta en uno de los metodos de esta clase.
     * Pero el escritor puede estar trabajando sobre el recuros. En ese caso num_escritores sera mayor que cero y los lectores deberan esperar a que el escritor termine.
     * 
     * Luego debe esperar en la puerta de los lectores porque otro hilo puede estar modificando la varialbe num_lectores
     * Se incrementa la variable num_lectores y se devuelve el recurso.
     * 
     * Luego, lo primero que sucede es que se libera el mutex de lock_num_lectores, la puerta donde habra otros lectores esperando para incrementar la variable y recibir el recurso.
     * Pero claro, hasta 
     * 
     * @return Object El recurso al que se quiere acceder
     */
    public Object quieroLeer(){
    /*VAR*/
        Object obj_aux = null;
    /*BEGIN*/
        try{
            synchronized(this.lock_num_escritores){                             //Si hay un hilo que quiere escribir debera esperar. Tambien bloquea a otros lectores. Este es el que tendra la cola, el synchronized interno no tendra cola porque estaran aqui esperando.
                while (this.num_escritores > 0){                                //Si hay un hilo escribiendo debera esperar
                    this.lock_num_escritores.wait();
                }/*while*/
                synchronized(this.lock_num_lectores){                           //Modificar la variable requiere un control. Pero nadie espera aqui, esperan en el anterior.
                    this.num_lectores++;
                }/*synchronized*/
            }/*synchronized*/
            obj_aux = this.recurso;                                             //Al ser num_lectores > 0 se esta en modo lectura con lo que se puede colocar esta linea fuera de los synchronized
        }catch (InterruptedException e){
            System.out.println("--- INI ERROR quieroLeer ---");
            System.out.println(e);
            System.out.println("--- FIN ERROR quieroLeer ---");
        }/*try-catch*/
        return obj_aux;
    }/*END quieroLeer*/
            

    /**
     * Un escritor se quedara bloqueado en la puerta (lock_num_escritores) si hay un lector o un escritor haciendo una peticion, ejecutando codigo de alguno de los metodos de esta clase. 
     * Luego debera esperar en la puerta de los lectores (lock_num_lectores) si hay alguien modificando num_lectores.
     * Cuando ya nadie este modificando num_lectores podra mirar su valor y si es mayor que cero debera dormir y esperar a que termien todos los que estaban leyendo antes de que el realizara su peticion de escritura.
     * Cuando ya no haya lectores podra acceder al recurso.
     * 
     * @return Object El recuros al que se quiere acceder
     */
    public Object quieroEscribir(){
    /*VAR*/
        Object obj_aux = null;
    /*BEGIN*/
        try{
            synchronized(this.lock_num_escritores){
                while (this.num_escritores > 0){                                //Si hay un hilo escribiendo debera esperar
                    this.lock_num_escritores.wait();
                }/*while*/
                this.num_escritores++;                                          //Incrementa y así ya no deja leer.
            }/*synchronized*/
            synchronized(this.lock_num_lectores){                               //Para poder mirar el valor de la variable no puede haber un hilo modificandolo.
                while (this.num_lectores > 0){
                    this.lock_num_lectores.wait();                              //Pero debe esperar a que los lectores terminen
                }/*while*/
                obj_aux = this.recurso;                                         //Esta asignacion se realiza cuando ya no hay nadie accediendo al recuros. Ya no hay lectores y el escritor aun no ha empezado.
            }/*synchronized*/
        }catch (InterruptedException e){
            System.out.println("--- INI ERROR quieroEscribir ---");
            System.out.println(e);
            System.out.println("--- FIN ERROR quieroEscribir ---");
        }/*try-catch*/
        return obj_aux;
    }/*END quieroEscribir*/
    

    /**
     * Actualiza el numero de lectores y despierta a todos.
     */
    public void dejoDeLeer(){
    /*BEGIN*/
        synchronized (this.lock_num_escritores){
        synchronized (this.lock_num_lectores  ){
            if (this.num_lectores > 0){                                         //Para evitar que llamadas sin control a dejoDeLeer() creen numero negativos que no tendrian sentido.
                this.num_lectores--;
            }/*if*/
            this.lock_num_lectores.notifyAll();                                 //Esta cola solo tiene a escritores esperando.       Primero despierta a los que hay esperando aqui pues solo son escritores que estaban a que el recurso quedara vacio.
            this.lock_num_escritores.notifyAll();                               //Esta cola tiene a lectores y escritores esperando. Luego despierta a los que esperan en la puerta de escritores, que no quiere decir que sean todos escritores pues en quieroLeer
        }/*synchronized*/
        }/*synchronized*/
    }/*END dejoDeLeer*/
    
    
    /**
     * Actualiza el numero de escritores y despierta a todos
     */
    public void dejoDeEscribir(){
    /*BEGIN*/
        synchronized (this.lock_num_escritores){
        synchronized (this.lock_num_lectores  ){
            if (this.num_escritores > 0){                                       //Para evitar que llamadas sin control a dejoDeLeer() creen numero negativos que no tendrian sentido.
                this.num_escritores--;
            }/*if*/
            this.lock_num_lectores.notifyAll();                                 //Esta cola solo tiene a escritores esperando.       Primero despierta a los que hay esperando aqui pues solo son escritores que estaban a que el recurso quedara vacio.
            this.lock_num_escritores.notifyAll();                               //Esta cola tiene a lectores y escritores esperando. Luego despierta a los que esperan en la puerta de escritores, que no quiere decir que sean todos escritores pues en quieroLeer            
        }/*synchronized*/
        }/*synchronized*/
    }/*END dejoDeEscribir*/

    
}/*END ControladorDeRecrusoLectorEscritor*/
