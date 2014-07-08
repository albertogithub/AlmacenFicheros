package almacenficheros;

import java.io.Serializable;

/**
 * Para almacenar la informacion asociada a un fichero que esta guardado en un Almacen.                                                                                                                                   <br>
 * El uso combinado de version y borrado sirve para mantener una buena sincronización de los almacenes.                                                                                                                   <br>
 *                                                                                                                                                                                                                        <br>
 * Por lo que a la escritura se refiere solo necesita el atributo version, que debe ser mas nuevo para poder sobrescribir. Caso 1.                                                                                        <br>
 * O bien si solo esta en uno de los dos se copiara donde no este.                                                         Caso 3.                                                                                        <br>
 *                                                                                                                                                                                                                        <br>
 * En caso de que se borre el fichero el atributo borrado pasa a estar a true y la sincronización pasa a tener varios casos.                                                                                              <br>
 * Dos almacenes comparan sus ficheros y el atributo version es el que manda si está el fichero en los dos almacenes. Al ganar el atributo version se hara lo que diga la version ganadora. Caso 2.                       <br>
 * Si la version ganadora dice borrado el fichero debera ser borrado y marcado como borrado.                                                                                                                              <br>
 * Si la version ganadora dice que no esta borrado es que despues de borrarlo hubo una escritura con un fichero mas reciente y por tanto el que lo tenia marcado como borrado ahora debe coger el fichero nuevo.          <br>
 *                                                                                                                                                                                                                        <br>
 * Como podrian borrarse muchos ficheros podria haber muchas variables con borrado a true cuando ya esta todo sincrinizado.                                                                                               <br>
 * En ese caso un proceso de administración cogería toda la información de todos los almacenes, las compara y veria que todo esta ya sincronizado lo que le permite hacer limpieza y quitar todos los borrados a true     <br>
 * Ahora suponemos que durante esta limpieza hay problemas de comunicación y que algunos de los almacenes ha recibido la estrucutra limpia sin borrados a true y otros aun van a conservar los borrados a true.           <br>
 * En este caso, cuando dos servidores van a sincronizar sus ficheros y uno de ellos tiene marcado el fichero como borrado y el otro no lo tiene entonces el que lo tiene marcado como borrado debe destruir la variable. <br>
 * En este ultimo caso no se estan sincronizando los contenido de los discos duros si no las variables, las estructuras de control.  Caso 4.                                                                              <br>
 *                                                                                                                                                                                                                        <br>
 * Este último caso es casi como resetear el valor de version para las entradas que se quitan. Para evitar problemas, es responsabilidad del usuario enviar siempre el nuevo fichero con la version nueva.                <br>
 * Si lo mandase con una version angitua y todo hubiera salido bien el fichero con la version antigua va a poder replicarse porque no habra memoria de su existencia.                                                     <br>
 * Pero si la parte de la limpieza no salio bien puede que algun servidor acepte el fichero pero mas tarde en la sincronizacion sea borrado.                                                                              <br>
 *                                                                                                                                                                                                                        <br>
 * Resumen:                                                                                                                                                                                                               <br>
 *    Caso 1: En ambos lados. Ninguno como borrado.    Gana la version más nueva.                                                                                                                                         <br>
 *    Caso 2: En ambos lados. Uno como borrado.        Gana la version más nueva. O se borra el otro o se copia fichero quitando la marca de borrado.                                                                     <br>
 *    Caso 3: En un    lado.  No marcado como borrado. Se replica a donde no esta.                                                                                                                                        <br>
 *    Caso 4: En un    lado.  Marcado como borrado.    Se quita la entrada del map que tiene la entrada de borrado.                                                                                                       <br>
 * 
 * @author Alberto Bellido de la Cruz
 */
public class FicheroInfo implements Serializable{

// VARIABLES ----------------------------------------------------------------
    private String  id                            =    "";
    
    private String  directorio_relativo           =    "";                      //Un fichero esta almacenado en un directorio del disco duro. Parte de la ruta la establece Almacen y parte el usuario. Aquí se almacena la parte del usuario.
    private String  nombre_fichero                =    "";                      //El nombre del fichero
    private String  version                       =    "";                      //Una cadena que sirve para saber la antiguedad del fichero y así controlar las reescrituras.
    private boolean borrado                       = false;                      //A true indica que el fichero fue borrado y no se debe buscar en el disco duro.

// CONSTRUCTORES ------------------------------------------------------------
    public FicheroInfo(String id){
        this.id = id;
    }

    
// METODOS ------------------------------------------------------------------
    
    public void setDirectorioRelativo(String dir_rel){
        this.directorio_relativo = dir_rel;
    }
    
    
    public void setNombreFichero(String nom_fich){
        this.nombre_fichero = nom_fich;
    }
    
    
    public void setVersion(String ver){
        this.version = ver;
    }
    
    
    public void setBorrado(boolean valor_borrado){
        this.borrado = valor_borrado;
    }
    
    
//---
    
    public String getDirectorioRelativo(){
        return this.directorio_relativo;
    }
    
    
    public String getNombreFichero(){
        return this.nombre_fichero;
    }
    
    
    public String getVersion(){
        return this.version;
    }
    
    
    public boolean getBorrado(){
        return this.borrado;
    }

    
}/*END CLASS FicheroInfo*/
