package almacenficheros;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.RMISecurityManager;

/**
 * Envia la señal de apagado al objeto remoto creado por RegistradorDeObjetosRemotos
 * Puede apagar remotamente porque se le da la IP de rmiregistry y el nombre del servidio.
 * 
 * Ejecucion
 *  C:\>set path=%path%;C:\Archivos de programa\Java\jdk1.7.0\bin;
 *  C:\>cd \NetBeansProjects\AlmacenFicheros\build\classes
 *  C:\NetBeansProjects\AlmacenFicheros\build\classes>java -Djava.security.policy=/Compartida/almacenficheros/policy.java almacenficheros.RegistradorDeObjetosRemotosApagar 192.168.176.41 Almacen_041
 * 
 * @author Alberto Bellido de la Cruz
 */
public class RegistradorDeObjetosRemotosApagar {

    // CONSTANTES  -------------------------------------------------------------
    private final static String PASSWORD_ADMINISTRADOR = "admin";

    
    /**
     * Apaga un objeto AlmacenObjetoRemote
     * 
     * @param argumentos Los argumentos de la linea de comando
     */
    public static void main (String[] argumentos){
    /*VAR*/
        Registry                   registry        =  null;
        AlmacenInterfaceRemote     almacen_remoto  =  null;
        String                     ip_servidor     =    "";
        String                     nombre_servicio =    "";
        boolean                    error           = false;
    /*BEGIN*/
        if (System.getSecurityManager() == null){                               //Estas lineas deben estar también en el Cliente ¿?
            System.setSecurityManager( new RMISecurityManager() );
        }/*if*/
        
        if (argumentos.length >=2){
            ip_servidor     = argumentos[0];
            nombre_servicio = argumentos[1];
            try {
                registry       = LocateRegistry.getRegistry(ip_servidor);                //La IP donde se ejecuto rmiregistry
                almacen_remoto = (AlmacenInterfaceRemote)registry.lookup(nombre_servicio);
                almacen_remoto.apagar(PASSWORD_ADMINISTRADOR);
            }catch (RemoteException re){
                error = true;
                System.out.println("RemoteException");
                System.out.println(re);
            }catch (NotBoundException nbe){                
                error = true;
                System.out.println("NotBoundException");
                System.out.println(nbe);                
            }/*try-catch*/
        }else{
            System.out.println("Debe llamar con: IP y nombre del servicio");
        }/*if*/
    }/*END main*/

    
}/*END CLASS RegistradorDeObjetosRemotos*/