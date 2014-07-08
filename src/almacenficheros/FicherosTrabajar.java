package almacenficheros;

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * Metodos de clase para trabajar con ficheros usando 
 * 
 * @author Alberto Bellido de la Cruz
 */
public class FicherosTrabajar {
    
    /**
     * Dice si existe un fichero en disco.
     * 
     * @param  directorio El directorio del fichero. Se espera sin barra al principio y con barra al final. 
     * @param  fichero    El nombre del fichero
     * @return boolean    true si el fichero con su directorio es encotrado en el disco
     */
    public static boolean exiteFichero(String directorio, String fichero){
    /*VAR*/
        File    file            =  null;
        String  ruta_completa   =    "";
        boolean existe_fichero  = false; 
    /*BEGIN*/
        ruta_completa  = directorio + fichero;   //Ahora mismo no se hacen comprobaciones.
        file           = new File(ruta_completa);
        existe_fichero = file.exists() &&  file.isFile();
        return existe_fichero;
    }/*END exiteFichero*/

    
    /**
     * Dice si existe un directorio en el disco
     * 
     * @param  directorio El directorio a comprobar
     * @return boolean    true si el directorio es encotrado en el disco
     */
    public static boolean existeDirectorio(String directorio){
    /*VAR*/
        File    file               =  null;
        boolean existe_directorio  = false; 
    /*BEGIN*/
        file           = new File(directorio);
        existe_directorio = file.exists() &&  file.isDirectory();
        return existe_directorio;
    }/*END existeDirectorio*/
    
    
    /**
     * Crea la ruta de directorio con los subdirectorios.
     * 
     * @param  directorio Los directorios a crear. Pueden ser varios uno dentro de otro.
     * @return boolean    true si se crean los directorios.
     */
    public static boolean crearDirectorio(String directorio){
    /*VAR*/
        File    file               =  null;
        boolean creado_directorio  = false; 
    /*BEGIN*/
        file              = new File(directorio);
        creado_directorio = file.mkdirs();
        return creado_directorio;
    }/*END crearDirectorio*/

    
    /**
     * Crea un fichero vacio si no existe
     * 
     * @param  directorio El directorio del fichero. Se espera sin barra al principio y con barra al final. 
     * @param  fichero    El nombre del fichero
     * @return boolean    true si el fichero se pudo crear.
     */
    public static boolean crearFichero(String directorio, String fichero){
    /*VAR*/
        File    file            =  null;
        String  ruta_completa   =    "";
        boolean creado_fichero  = false; 
    /*BEGIN*/
        ruta_completa  = directorio + fichero;   //Ahora mismo no se hacen comprobaciones.
        file           = new File(ruta_completa);
        try{
            creado_fichero = file.createNewFile();
        }catch (IOException ex){
            creado_fichero = false;
        }/*try-catch*/
        return creado_fichero;
    }/*END crearFichero*/
    
    
    /**
     * Lee un fichero y lo mete en una lista de paquetes. Asume que el fichero existe. <br>
     * Devolvera null si hay problemas con la lectura.                                 <br>
     * Devolvera una lista vacia si el fichero esta vacio.                             <br>
     * 
     * @param  directorio           El directorio del fichero. Se espera sin barra al principio y con barra al final. 
     * @param  fichero              El nombre del fichero
     * @param  tamano_paquete       Se debe especificar el tamaño de los paquetes en los que se debe trocear la lectura
     * @return List<FicheroPaquete> El contenido del fichero en una lista de paquetes. null si hay problemas con la lectura. Lista vacia si el fichero esta vacio.
     */
    public static List<FicheroPaquete> leerFichero(String directorio, String fichero, int tamano_paquete){
    /*VAR*/
        List<FicheroPaquete>  paquetes               =  new  ArrayList<FicheroPaquete>();
        FicheroPaquete        fichero_paquete        =  null;
        File                  file                   =  null;
        FileInputStream       file_input_stream      =  null;
        String                ruta_completa          =    "";
        int[]                 array_de_bytes         =  null;
        int                   numero_del_byte_a_leer =     0;
        int                   numero_de_bytes_leidos =     0;
        int                   numero_de_paquete      =    -1;
        int                   byte_actual            =     0;
        boolean               exception              = false;
    /*BEIGN*/
        ruta_completa = directorio + fichero;   //Ahora mismo no se hacen comprobaciones.
        file          = new File(ruta_completa);
        try{
            file_input_stream  = new FileInputStream(file);
        }catch (FileNotFoundException ex) {
            exception = true;
            paquetes  = null;
            //Logger.getLogger(AlmacenObjeto.class.getName()).log(Level.SEVERE, null, ex);
        }/*try-catch*/
        if (!exception){
            try{
                while (byte_actual != -1){                                                             //Bucle externo crea la lista de paquetes.
                    numero_del_byte_a_leer = 0;
                    array_de_bytes         = new int[tamano_paquete];
                    while ( (numero_del_byte_a_leer < tamano_paquete) && (byte_actual != -1) ){        //Bucle interno intenta leer bytes para luego crear un paquete. 
                        byte_actual = file_input_stream.read();
                        if (byte_actual != -1){                                                        //Problema de dos condiciones y se debe evitar leer un byte de más si ya se han leido todos los que caben en un paquete.
                            array_de_bytes[numero_del_byte_a_leer] = byte_actual;
                            numero_del_byte_a_leer++;
                        }/*if*/
                    }/*while*/
                    numero_de_bytes_leidos = numero_del_byte_a_leer;
                    if (numero_de_bytes_leidos > 0){                                                   //Se creara un paquete si se ha leido algo. Debe ser mayor que cero para que haya leido algo.
                        numero_de_paquete++;                                                           //Se inicializa a -1, el primer paquete sera el numero cero.
                        fichero_paquete = new FicheroPaquete(ruta_completa + "-" + numero_de_paquete); //El nombre o id importa poco.
                        fichero_paquete.setBytes                  (array_de_bytes          );
                        fichero_paquete.setCantidadDeBytesValidos (numero_de_bytes_leidos  );
                        fichero_paquete.setNumeroDePaquete        (numero_de_paquete       );          //Se inicializa a -1, el primer paquete sera el numero cero.
                        fichero_paquete.setTamPaquete             (tamano_paquete          );
                        paquetes.add(numero_de_paquete, fichero_paquete);
                    }/*if*/
                }/*while*/
                file_input_stream.close();
            }catch(IOException e){
                exception = true;
                paquetes  = null;
            }/*try-catch*/
        }/*if*/
        return paquetes;
    }/*END leerFichero*/

    
    /**
     * Escribe a disco la informacion contennida en una lista de paquetes.                 <br>
     * El fichero es borrado si existe.                                                    <br>
     * Resultado:                                                                          <br>
     *    0 Todo correcto.                                                                 <br>
     *  -12 Problema de escritura o cerrando el fichero.                                   <br>
     *  -31 El array de bytes no tiene el tamaño correcto.                                 <br>
     * -101 Error interno. No concuerda el numero del paquete con la posicion en la lista. <br>
     * 
     * @param  directorio El directorio del fichero. Se espera sin barra al principio y con barra al final. 
     * @param  fichero    El nombre del fichero
     * @param  paquetes   El contenido del fichero en una lista de paquetes.
     * @return int        Cero si todo va bien. Negativo si sale algo mal.
     */
    public static int escribirFichero(String directorio, String fichero, List<FicheroPaquete> paquetes){
    /*VAR*/ 
        FicheroPaquete        fichero_paquete         =  null;
        FileOutputStream      file_output_stream      =  null;
        File                  dir                     =  null;
        File                  file                    =  null;
        String                ruta_completa           =    "";
        int[]                 array_de_bytes          =  null;
        int                   numero_de_paquetes      =     0;        
        int                   numero_de_paquete       =     0;
        int                   tamano_paquete          =     0;
        int                   cantidad_bytes_validos  =     0;
        int                   byte_actual             =     0;
        int                   resultado               =     0;
        int                   i, j                    =     0;
    /*BEGIN*/
        if ( FicherosTrabajar.exiteFichero(directorio, fichero) ){
            FicherosTrabajar.borrarFichero(directorio, fichero);
        }/*if*/
        try {
            ruta_completa      = directorio + fichero;   //Ahora mismo no se hacen comprobaciones.
            dir                = new File(directorio);
            file               = new File(ruta_completa);
            dir.mkdirs();
            file.createNewFile();            
            file_output_stream = new FileOutputStream(file);
        } catch (IOException ex) {
            resultado = -12;
        }/*try-catch*/
        if (resultado == 0){
            i                  = 0;
            numero_de_paquetes = paquetes.size();
            while ( (i<numero_de_paquetes) && (resultado == 0) ){
                fichero_paquete        = paquetes.get(i);
                array_de_bytes         = fichero_paquete.getBytes();
                cantidad_bytes_validos = fichero_paquete.getCantidadDeBytesValidos();
                numero_de_paquete      = fichero_paquete.getNumeroDePaquete();
                tamano_paquete         = fichero_paquete.getTamPaquete();
                if (numero_de_paquete != i){
                    resultado = -101;
                }/*if*/
                if (tamano_paquete != array_de_bytes.length){
                    resultado = -31;
                }/*if*/
                if (resultado == 0){
                    j=0;
                    while ( (j<cantidad_bytes_validos) && (resultado == 0) ){
                        byte_actual = array_de_bytes[j];
                        try {
                            file_output_stream.write(byte_actual);
                        } catch (IOException ex) {
                            resultado = -12;
                        }/*try-catch*/
                        j++;
                    }/*while*/
                }/*if*/
                i++;
            }/*while*/
            try {
                file_output_stream.close();
            } catch (IOException ex) {
                resultado = -12;
            }/*try-catch*/
        }/*if*/
        return resultado;
    }/*END escribirFichero*/

    
    /**
     * Borra un fichero que debe exsitir, no hace comprbacione.
     * 
     * @param  directorio El directorio del fichero. Se espera sin barra al principio y con barra al final. 
     * @param  fichero    El nombre del fichero
     * @return boolean    true si fue borrado.
     */    
    public static boolean borrarFichero(String directorio, String fichero){
    /*VAR*/
        File    file            =  null;
        String  ruta_completa   =    "";        
        boolean resultado       = false;
    /*BEGIN*/
        ruta_completa  = directorio + fichero;   //Ahora mismo no se hacen comprobaciones.
        file           = new File(ruta_completa);
        resultado      = file.delete();
        return resultado;
    }/*END boorarFichero*/
    
    
}/*END CLASS FicherosTrabajar*/
