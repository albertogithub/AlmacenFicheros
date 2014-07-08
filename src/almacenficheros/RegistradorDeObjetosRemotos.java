package almacenficheros;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.RMISecurityManager;

import java.util.Properties;
import java.io.FileReader;
import java.io.IOException;

/**
 * Crea objetos remotos y los registra en rmiregistry
 * Usa un fichero de propiedades para cargar informacion en vez de usar los argumentos de main
 * 
 * -----------------------------------------------------------------------------
 * COMO EJECTUAR:
 * Se ha creado un enlace como los de linux (juntion en windows) con linkd.exe en C:\NetBeansProjects apuntando a esa misma carpeta que esta dentro de Mis Documentos para evitar rutas muy largas y con espacios.
 * Tambien es conveniente colocar en el path el directorio de java: set path=%path%;C:\Archivos de programa\Java\jdk1.7.0\bin;
 * 
 * -----
 * 
 * 1 Creacion del Stub
 * No parece que el codebase para el Stub sea necesario, tal vez por ser la version 7. 
 * Para 1.1 dice que no encuentra AlmacenObjetoRemote
 * 1.1 NO C:\NetBeansProjects\AlmacenFicheros\build\classes\almacenficheros>rmic AlmacenObjetoRemote
 * 1.2 SI C:\NetBeansProjects\AlmacenFicheros\build\classes>rmic almacenficheros.AlmacenObjetoRemote
 * 
 * -----
 * 
 * 2 Arrancando el registro
 * Antes de ejecutar el registro hay que establecer la variable de entorno CLASSPATH con la ruta donde esten los .class
 * La ruta dada en CLASSPATH debe existir antes de ejecutar rmiregistry. No se puede compilar y crear las carpetas despues porque parece que lo busca al iniciar rmiregistry
 * 
 *  C:\>set path=%path%;C:\Archivos de programa\Java\jdk1.7.0\bin;
 *  C:\>set CLASSPATH=C:\NetBeansProjects\AlmacenFicheros\build\classes;
 *  C:\>rmiregistry
 * 
 * -----
 * 
 * 3 Ejecucion nº1 normal
 * Para 3.1 dice que no encuentra RegistradorDeObjetosRemotos
 * 3.1 NO C:\NetBeansProjects\AlmacenFicheros\build\classes\almacenficheros>java -Djava.security.policy=/Compartida/almacenficheros/policy.java RegistradorDeObjetosRemotos
 * 3.2 SI C:\NetBeansProjects\AlmacenFicheros\build\classes>java -Djava.security.policy=/Compartida/almacenficheros/policy.java almacenficheros.RegistradorDeObjetosRemotos
 * 
 *  C:\>set path=%path%;C:\Archivos de programa\Java\jdk1.7.0\bin;
 *  C:\>cd \NetBeansProjects\AlmacenFicheros\build\classes
 *  C:\NetBeansProjects\AlmacenFicheros\build\classes>java -Djava.security.policy=/Compartida/almacenficheros/policy.java almacenficheros.RegistradorDeObjetosRemotos /Compartida/almacenficheros/properties.rmiregistry.conf /Compartida/almacenficheros/properties.almacen.conf
 * 
 * -----
 * 
 * 4 Ejecucion nº2 distribuida
 * El primer servidor se arranca de forma normal, al segundo y sucesivos hay que darles la ip y el nombre del servicio de uno en ejecucion.
 * Se llego a la conclusión de que por cada almacen deberia haber un rmiregistry o al menos un rmiregistry por maquina (podría haber varios almacenes en la misma maquina) 
 * Por ahora se piensa que lo mejor es que el nombre del servicio que se registre en rmiregistry sea el mismo que el del ID del almacen pues evita colisiones y no parece que suponga un problema para contactar.
 * 
 *  C:\>set path=%path%;C:\Archivos de programa\Java\jdk1.7.0\bin;
 *  C:\>cd \NetBeansProjects\AlmacenFicheros\build\classes
 *  C:\NetBeansProjects\AlmacenFicheros\build\classes>java -Djava.security.policy=/Compartida/almacenficheros/policy.java almacenficheros.RegistradorDeObjetosRemotos /Compartida/almacenficheros/properties.rmiregistry.conf /Compartida/almacenficheros/properties.almacen.conf /Compartida/almacenficheros/properties.distribuida.conf
 * 
 * @author Alberto Bellido de la Cruz
 */
public class RegistradorDeObjetosRemotos {

    // CONSTANTES  -------------------------------------------------------------
    private final static String SERVIDOR_RMIREGISTRY_IP  = "ip_rmiregistry";
    private final static String NOMBRE_DEL_SERVICIO      = "nombre_servicio_en_rmiregistry";
    private final static String PREFIJO_MENSAJES         = "REGISTRADOR:   ";

    
    /**
     * Crea un objeto AlmacenObjetoRemote y lo registra en rmiregistry
     * 
     * @param argumentos Los argumentos de la linea de comando
     */
    public static void main (String[] argumentos){
    /*VAR*/
        Properties                  prop                              = null;
        Registry                    registro                          = null;
        AlmacenObjetoRemote         objeto                            = null;
        String                      fich_conf_rmiregistry_registrar   =   "";   //Lugar desde el que cargar la configuracion para registrar el objeto remoto en rmiregistry
        String                      fich_conf_almacen                 =   "";   //Lugar desde el que cargar la configuracion para crear el objeto remoto.        
        String                      fich_conf_rmiregistry_distribuida =   "";   //Lugar desde el que cargar la configuracion para conectar con otros servidores almacenes y crear una red distribuida
        String                      ip_rmiregistry                    =   "";
        String                      servicio_nombre                   =   "";
        int                         tipo_arranque                     =    0;
        int                         resultado_operacion               =    0;
    /*BEGIN*/
//      if (System.getSecurityManager() == null){                               //Estas lineas deben estar también en el Cliente ¿?
//          System.setSecurityManager( new RMISecurityManager() );
//      }/*if*/
        
        if (argumentos.length == 2){                                            //Arranque en solitario.
            tipo_arranque                     = 1;
            fich_conf_rmiregistry_registrar   = argumentos[0];
            fich_conf_almacen                 = argumentos[1];                
        }else if (argumentos.length == 3){                                      //Arranque version distribuida
            tipo_arranque                     = 2;
            fich_conf_rmiregistry_registrar   = argumentos[0];
            fich_conf_almacen                 = argumentos[1];
            fich_conf_rmiregistry_distribuida = argumentos[2];
        }else{
            System.out.println(PREFIJO_MENSAJES + "Faltan los ficheros de configuracion (2 parametros):   properties.rmiregistry.conf  y  properties.almacen.conf");
            System.out.println(PREFIJO_MENSAJES + "o tambien, para el arranque de la version distribuida");
            System.out.println(PREFIJO_MENSAJES + "Falta la informacion de otro servidor (3 parametros):  properties.rmiregistry.conf  y  properties.almacen.conf    properties.distribuida.conf");
        }/*if*/
        
        if ( (tipo_arranque == 1) || (tipo_arranque == 2) ){
            
            //Arranca la aplicación.
            try{
                //Carga lo referente a rmiregistry desde un fichero.
                prop            = new Properties();
                prop.load( new FileReader(fich_conf_rmiregistry_registrar) ); 
                ip_rmiregistry  = prop.getProperty(SERVIDOR_RMIREGISTRY_IP);
                servicio_nombre = prop.getProperty(NOMBRE_DEL_SERVICIO    );
                System.out.println(PREFIJO_MENSAJES + "SERVIDOR_RMI:         " + ip_rmiregistry );
                System.out.println(PREFIJO_MENSAJES + "Nombre del servicio:  " + servicio_nombre);
                System.out.println("");

                //Para no tener que poner java -Djava.rmi.server.hostname al ejecutar Registrador.
                //Para que funcione debe ejecutarse antes que la creacion de los objetos remotos.
                //Esto es asi porque los UnicastRemoteObject usan esta propiedad. PruebaRMIRemoteObject() hereda de UnicastRemoteObject
                System.setProperty("java.rmi.server.hostname", ip_rmiregistry);

                objeto = AlmacenObjetoRemote.crearAlmacenObjetoRemote (fich_conf_almacen, fich_conf_rmiregistry_distribuida, ip_rmiregistry, servicio_nombre);
                if (objeto != null){
                    System.out.println(PREFIJO_MENSAJES + "Objeto remoto creado.");
                    registro = LocateRegistry.getRegistry(ip_rmiregistry);
                    registro.rebind(servicio_nombre, objeto);
                    System.out.println(PREFIJO_MENSAJES + "Servicio " + servicio_nombre + " registrado en rmiregistry de " + ip_rmiregistry);
                }else{
                    System.out.println(PREFIJO_MENSAJES + "No se ha podido crear el objeto remoto");
                }/*if*/
            }catch (IOException e){
                System.out.println(PREFIJO_MENSAJES + "ERROR leyendo el fichero: " + fich_conf_rmiregistry_registrar);
                System.out.println(e);
            }/*try-catch*/
            
            //Apaga la aplicación
/*          try{
                if (objeto != null){
                    Thread.sleep(60000);
                    System.out.println("");
                    System.out.println(PREFIJO_MENSAJES + "Antes de apagar.");
                    resultado_operacion = objeto.apagar("admin");
                    System.out.println(PREFIJO_MENSAJES + "Despues de apagar. Resultado: " + resultado_operacion);
                      try {
                        System.out.println(PREFIJO_MENSAJES + "Antes de lookup.");
                        objeto = (AlmacenObjetoRemote)registro.lookup(servicio_nombre);
                        if (objeto != null){
                            System.out.println(PREFIJO_MENSAJES + "Despues de lookup. El objeto NO es null");
                        }else{
                            System.out.println(PREFIJO_MENSAJES + "Despues de lookup. El objeto es null");
                        }
                    } catch (RemoteException ex) {
                        System.out.println(PREFIJO_MENSAJES + "ERROR RemoteException. Invocando a lookup. ");
                        System.out.println(ex);
                    } catch (NotBoundException ex) {
                        System.out.println(PREFIJO_MENSAJES + "ERROR NotBoundException. Invocando a lookup. ");
                        System.out.println(ex);
                    }
                }
            }catch (InterruptedException ex){
                System.out.println(PREFIJO_MENSAJES + "ERROR InterruptedException. Esperando para desperar. ");
                System.out.println(ex);
            }
*/
            
        }/*if*/
    }/*END main*/

    
}/*END CLASS RegistradorDeObjetosRemotos*/