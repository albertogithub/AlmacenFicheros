package almacenficheros;

import java.util.List;
import java.util.Map;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Para crear un servidor de almacenamiento de ficheros.                                                     <br>
 * Los ficheros pueden ser escritos y leidos por trozos.                                                     <br>
 * 
 * @author Alberto Bellido de la Cruz
 */
public interface AlmacenInterfaceRemote extends Remote{

    //public String                    solicitarEscribirFicheroEnServidor                      (String password_escritura, String nombre_directorio, String nombre_fichero, String version, int longitud_del_paquete, int cantidad_paquetes)                                 throws RemoteException;    
    //public String                    solicitarContinuarEscribirFicheroEnServidor             (String password_escritura, String nombre_directorio, String nombre_fichero, String version                                                 )                                 throws RemoteException;    
    //public int                       escribiendoFicheroEnServidorEnviandoPaquete             (String password_escritura, String id_de_escritura, int[] paquete, int numero_del_paquete, int cantidad_bytes)                                                                throws RemoteException;
    //public int                       escribiendoFicheroEnServidorEnviandoPaquetes            (String password_escritura, String id_de_escritura, List<FicheroPaquete> paquetes)                                                                                            throws RemoteException;
    //public List<Integer>             escribiendoFicheroEnServidorPaquetesQueFaltan           (String password_escritura, String id_de_escritura)                                                                                                                           throws RemoteException;
    //public int                       escribiendoFicheroEnServidorEnvioFinalizado             (String password_escritura, String id_de_escritura)                                                                                                                           throws RemoteException;
    //public int                       escribiendoFicheroEnServidorCancelar                    (String password_escritura, String id_de_escritura)                                                                                                                           throws RemoteException;    
    //public String                    escribiendoFicheroEnServidorInfoFichero                 (String password_escritura, String id_de_escritura, int dir_o_file)                                                                                                           throws RemoteException;
    //
    //public String                    solicitarLeerFicheroDelServidor                         (String password_lectura, String nombre_directorio, String nombre_fichero, int longitud_del_paquete)                                                                          throws RemoteException;
    //public int[]                     leyendoFicheroDelServidorLeerPaquete                    (String password_lectura, String id_de_lectura, int num_del_paquete)                                                                                                          throws RemoteException;
    //public List<FicheroPaquete>      leyendoFicheroDelServidorLeerPaquetes                   (String password_lectura, String id_de_lectura)                                                                                                                               throws RemoteException;    
    //public int                       leyendoFicheroDelServidorTamanoDelPaquete               (String password_lectura, String id_de_lectura)                                                                                                                               throws RemoteException;
    //public int                       leyendoFicheroDelServidorNumeroDePaquetes               (String password_lectura, String id_de_lectura)                                                                                                                               throws RemoteException;
    //public int                       leyendoFicheroDelServidorBytesDelPaquete                (String password_lectura, String id_de_lectura, int num_del_paquete)                                                                                                          throws RemoteException;
    //public void                      leyendoFicheroDelServidorLecturaFinalizada              (String password_lectura, String id_de_lectura)                                                                                                                               throws RemoteException;
    //
    //public int                       borrarFicheroDelServidor                                (String password_escritura, String nombre_directorio, String nombre_fichero)                                                                                                  throws RemoteException;
    //public int                       existeFicheroEnServidor                                 (String password_lectura,   String nombre_directorio, String nombre_fichero)                                                                                                  throws RemoteException;
    //
    // ------------------------------------------------------------------------ Grupo W. Control de acceso y passwords
    //
    //public int                       adminCambiarPasswordLectura                             (String password_administracion, String nueva_password_lectura)                                                                                                               throws RemoteException;
    //public int                       adminCambiarPasswordEscritura                           (String password_administracion, String nueva_password_escritura)                                                                                                             throws RemoteException;
    //public int                       adminCambiarPasswordProceso                             (String password_administracion, String nueva_password_procesos)                                                                                                              throws RemoteException;
    //public int                       adminCambiarPasswordAdministracion                      (String password_administracion_antigua, String password_administracion_nueva)                                                                                                throws RemoteException;
    //public int                       adminCambiarAccesoPermitido                             (String password_administracion, boolean permitido_lectura, boolean permitido_escritura, boolean permitido_conexion_pasiva, boolean permitido_sincronizar)                    throws RemoteException;
    //
    // ------------------------------------------------------------------------ Grupo X. Ejecucion del run y las tareas de mantenimiento.
    //
    //public int                       apagar                                                  (String password_administracion)                                                                                                                                              throws RemoteException;
    //public int                       finalizarHiloRun                                        (String password_administracion)                                                                                                                                              throws RemoteException;
    //public int                       pausaHiloRun                                            (String password_procesos, boolean valor_pausa)                                                                                                                               throws RemoteException;
    //public int                       permitirEjecutarCaducarSesiones                         (String password_procesos, boolean valor_caducar_sesiones)                                                                                                                    throws RemoteException;
    //public int                       permitirEjecutarComprobarConexiones                     (String password_procesos, boolean valor_comprobar_conexiones)                                                                                                                throws RemoteException;
    //public int                       permitirEjecutarSincronizarFicheros                     (String password_procesos, boolean valor_sincronizar_ficheros)                                                                                                                throws RemoteException;
    //public int                       ejecutarAhoraCaducarSesiones                            (String password_procesos)                                                                                                                                                    throws RemoteException;
    //public int                       ejecutarAhoraComprobarConexiones                        (String password_procesos)                                                                                                                                                    throws RemoteException;
    //public int                       ejecutarAhoraSincronizarFicheros                        (String password_procesos)                                                                                                                                                    throws RemoteException;
    //
    // ------------------------------------------------------------------------ Grupo Y. Conectar con otros almacenes.
    //
    //public List<String>              listaDeAlmacenesOrdenCero                               (String password_administracion)                                                                                                                                              throws RemoteException;
    //public AlmacenInterfaceRemote    almacenOrdenCero                                        (String password_administracion, String id_almacen_remoto)                                                                                                                    throws RemoteException;
    //public boolean                   solicitarConexion                                       (String password_administracion, AlmacenInterfaceRemote solicitante)                                                                                                          throws RemoteException;
    //public boolean                   confirmarConexionActiva                                 (String password_administracion, String  id_solicitante)                                                                                                                      throws RemoteException;
    //public boolean                   confirmarConexionPasiva                                 (String password_administracion, String  id_solicitante)                                                                                                                      throws RemoteException;
    //public int                       informandoBaja                                          (String password_administracion, AlmacenInterfaceRemote almacen_baja)                                                                                                         throws RemoteException;
    //
    // ------------------------------------------------------------------------ Grupo Z. Sincronizar contenido entre almacenes.
    //
    //public Map<String, FicheroInfo>  ficherosDelAlmacen                                      (String password_lectura)                                                                                                                                                     throws RemoteException;
    //public boolean                   sincronizarSolicitarIniciar                             (String password_administracion, String id_almacen)                                                                                                                           throws RemoteException;
    //public int                       sincronizarInformarFinalizar                            (String password_administracion)                                                                                                                                              throws RemoteException;
    //public int                       sincronizarEliminarDeFicherosDeLaEstructura             (String password_administracion, String id_de_fichero)                                                                                                                        throws RemoteException;    
    //public String                    sincronizarSolicitarEscribirFicheroEnServidor           (String password_administracion, String id_almacen, String nombre_directorio, String nombre_fichero, String fecha_version, int longitud_del_paquete, int cantidad_paquetes)   throws RemoteException;
    //public int                       sincronizarEscribiendoFicheroEnServidorEnviandoPaquetes (String password_administracion, String id_almacen, String id_de_escritura, List<FicheroPaquete> lista_paquetes)                                                              throws RemoteException;
    //public int                       sincronizarEscribiendoFicheroEnServidorEnvioFinalizado  (String password_administracion, String id_almacen, String id_de_escritura)                                                                                                   throws RemoteException;
    //public int                       sincronizarEscribiendoFicheroEnServidorCancelar         (String password_administracion, String id_almacen, String id_de_escritura)                                                                                                   throws RemoteException;
    //public int                       sincronizarBorrarFicheroDelServidor                     (String password_administracion, String id_almacen, String nombre_directorio, String nombre_fichero)                                                                          throws RemoteException;
    //public String                    sincronizarSolicitarLeerFicheroDelServidor              (String password_administracion, String id_almacen, String nombre_directorio, String nombre_fichero, int longitud_del_paquete)                                                throws RemoteException;
    //public List<FicheroPaquete>      sincronizarLeyendoFicheroDelServidorLeerPaquetes        (String password_administracion, String id_almacen, String id_de_lectura)                                                                                                     throws RemoteException;
    //public int                       sincronizarLeyendoFicheroDelServidorLecturaFinalizada   (String password_administracion, String id_almacen, String id_de_lectura)                                                                                                     throws RemoteException;

    
    public final int    INFO_FICHERO_YA_EXISTE                                =  -11;
    public final int    INFO_FICHERO_NO_ENCONTRADO                            =  -12;
    public final int    INFO_DIRECTORIO_NO_EXISTE                             =  -13;
    public final int    INFO_FICHERO_EN_USO                                   =  -14;
    public final int    INFO_FICHERO_VACIO                                    =  -15;
    public final int    INFO_ID_NO_EXISTE                                     =  -21;
    public final int    INFO_LIMITE_MAX_IDS_POR_CREAR                         =  -22;
    public final int    INFO_LIMITE_MAX_LECTORES_POR_ID                       =  -23;
    public final int    INFO_LIMITE_MAX_LECTORES                              =  -24;
    public final int    INFO_TAMANO_PAQUETE_ARRAY                             =  -31;
    public final int    INFO_PAQUETE_FUERA_DE_RANGO                           =  -32;
    public final int    INFO_ACCESO_NO_PERMITIDO                              =  -41;
    public final int    INFO_PASSWORD_INVALIDA                                =  -42;
    public final int    INFO_SINCRONIZACION_SIN_PERMISO                       =  -43;
    public final int    WARNING_DURANTE_LA_ESPERA                             =  -91;
    public final int    ERROR_INTERNO_NUM_PAQUETE_LISTA                       = -101;
    public final int    ERROR_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO  = -102;

    
    /**
     * Eco, ecooooo
     * Se usa para comprobar la conexion de una manera sencilla.
     * Se devuelve la misma cadena que se envia como parametro.
     * 
     * @param  str_test        La cadena que se espera recibir devuelta
     * @return String          La misma cadena que se envia como parametro
     * @throws RemoteException 
     */
    public String testConnection(String str_test) throws RemoteException;

    
    /**
     * Devuelve el identificador.
     * 
     * @return String           El identificador de la instancia.
     * @throws RemoteException 
     */
    public String getID() throws RemoteException;
    
    
    /**
     * Para iniciar el envio de un fichero desde el cliente al servidor.                                                                                                                    <br>
     * Se envia el nombre del fichero junto con el directorio para que no se confunda con otros posibles ficheros con el mismo nombre.                                                      <br>
     * El nombre del directorio debe empezar sin barra pero si debe terminar en barra. Aqui no se comprueba que este bien escrito. Fallara si no se envia bien.                             <br>
     * El servidor crea id para esta comunicacion que es lo que le devuelve al cliente.                                                                                                     <br>
     * El cliente usara este id para enviar su fichero y que no se mezcle la informacion de otros ficheros que se esten subiendo a la vez.                                                  <br>
     * El fichero permanecera en memoria del servidor hasta que no llame al metodo que da por finalizado el envio y lo escribe a disco                                                      <br>
     * Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema:                                                                       <br>
     *    INFO_FICHERO_YA_EXISTE         El fichero ya existe, el enviado no es mas nuevo que el almacenado.                                                                                <br>
     *    INFO_LIMITE_MAX_IDS_POR_CREAR  Se ha superado el limite ids de escritura que se pueden crear. Solo puede haber un escritor por archivo pero varios para archivos diferentes.      <br>
     *    WARNIG_DURANTE_LA_ESPERA       Mientras se esperaba a que quedara libre el fichero se produjo un error.                                                                           <br>
     *    INFO_PASSWORD_INVALIDA         La contraseña de escritura suministrada no es valida                                                                                               <br>
     *    INFO_ACCESO_NO_PERMITIDO       Se ha denegado el acceso, no se permite crear nuevas sesiones de escritura en este momento.                                                        <br>
     * 
     * @param  password_escritura    Se debe enviar la contraseña de escritura para crear una sesion para escribir un fichero.        
     * @param  nombre_directorio     Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros. Debe venir sin barra inicial pero con barra final.
     * @param  nombre_fichero        El nombre del fichero. 
     * @param  version               Una cadena para comparar la antiguedad de ficheros en reescrituras. Usa  ver_antigua.compareTo(ver_nueva)
     * @param  longitud_del_paquete  Para indicar cuantos bytes se enviaran cada vez que se mande un paquete
     * @param  cantidad_paquetes     Se debe decir antes de empezar cuantos paquetes seran enviados. Junto con longitud del paquete se sabra el tamaño del fichero.
     * @return String                Una cadena que identifica la comunicacion. Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema,
     * @throws RemoteException 
     */
    public String solicitarEscribirFicheroEnServidor (String password_escritura, String nombre_directorio, String nombre_fichero, String version, int longitud_del_paquete, int cantidad_paquetes) throws RemoteException;

    
    /**
     * Para continuar una escritura que no se termino. Se llamo al metodo escribiendoFicheroEnServidorEscrituraFinalizada y este dijo que quedaban partes por escribir.                   <br>
     * Se envia el nombre del fichero junto con una añadido para que no se confunda con otros posibles ficheros con el mismo nombre.                                                      <br>
     * El servidor crea id para esta comunicacion que es lo que le devuelve al cliente.                                                                                                   <br>
     * El cliente usara este id para enviar su fichero y que no se mezcle la informacion de otros ficheros que se esten subiendo a la vez.                                                <br>
     * El fichero permanecera en memoria del servidor hasta que no llame al metodo que da por finalizado el envio y lo escribe a disco                                                    <br>
     * Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema:                                                                     <br>
     *    INFO_FICHERO_NO_ENCONTRADO      Fichero no encontrado                                                                                                                           <br>
     *    INFO_LIMITE_MAX_IDS_POR_CREAR   Se ha superado el limite ids de escritura que se pueden crear. Solo puede haber un escritor por archivo pero varios para archivos diferentes.   <br>
     *    WARNIG_DURANTE_LA_ESPERA        Mientras se esperaba a que quedara libre el fichero se produjo un error.                                                                        <br>
     *    INFO_PASSWORD_INVALIDA          La contraseña de escritura suministrada no es valida                                                                                            <br>
     *    INFO_ACCESO_NO_PERMITIDO        Se ha denegado el acceso, no se permite crear nuevas sesiones de escritura en este momento.                                                     <br>
     *                                                                                                                                                                                    <br>
     * NOTA: Metodo pendiente de poder almacenar la longitud del paquete.                                                                                                                 <br>
     * 
     * @param  password_escritura    Se debe enviar la contraseña de escritura para crear una sesion para escribir un fichero.        
     * @param  nombre_directorio     Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros.
     * @param  nombre_fichero        El nombre del fichero. 
     * @param  version               Una cadena para comparar la antiguedad de ficheros en reescrituras. Usa  ver_antigua.compareTo(ver_nueva)
     * @return String                Un numero que identifica la comunicacion. Así se puede enviar el fichero a trozos y sin prisas.
     * @throws RemoteException 
     */
    public String solicitarContinuarEscribirFicheroEnServidor (String password_escritura, String nombre_directorio, String nombre_fichero, String version) throws RemoteException;
    
    
    /**
     * Se debe llamar a este metodo para enviar al servidor los trozos del fichero.                                       <br>
     * Permite el envio desordenado de paquetes y se puede enviar dos veces el mismo paquete, se limita a sobrescribir.   <br>
     * Se puede enviar dos veces el mismo paquete, se limita a sobrescribir.                                              <br>
     * Resultados:                                                                                                        <br>
     *    ERROR_SIN_ERRORES            Operacion realizada correctamente                                                  <br>
     *    INFO_ID_NO_EXISTE            No existe el id_de_escritura                                                       <br>
     *    INFO_TAMANO_PAQUETE_ARRAY    El array de bytes no tiene el tamaño correcto.                                     <br>
     *    INFO_PAQUETE_FUERA_DE_RANGO  El numero del paquete enviado esta fuera de rango.                                 <br>
     *    INFO_PASSWORD_INVALIDA       La contraseña de escritura suministrada no es valida                               <br>
     * 
     * @param  password_escritura    Se debe enviar la contraseña de escritura para crear una sesion para escribir un fichero.
     * @param  id_de_escritura       El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @param  paquete               Los bytes de informacion del fichero. No se mira el tamaño pero se ignorara todo lo que exceda de la longitud del paquete que se dijo al solicitar la escritura a la hora de guardar.
     * @param  numero_del_paquete    Podría pasar que los paquetes no llegasen en orden. El primero es el cero.
     * @param  cantidad_bytes        El ultimo paquete no estara completo asi que no se puede leer todo el array
     * @return int                   Resultado de la operacion.
     */
    public int escribiendoFicheroEnServidorEnviandoPaquete  (String password_escritura, String id_de_escritura, int[] paquete, int numero_del_paquete, int cantidad_bytes) throws RemoteException;
    
    
    /**
     * METODO OPCIONAL: Necesita que el Cliente tenga la clase FicheroPaquete y esto es algo que no se puede exigir.      <br>
     *                                                                                                                    <br>
     * Con una única llamada se envían todos los paquetes.                                                                <br>
     * Resultados:                                                                                                        <br>
     *    ERROR_SIN_ERRORES            Operacion realizada correctamente                                                  <br>
     *    INFO_ID_NO_EXISTE            No existe el id_de_escritura                                                       <br>
     *    INFO_TAMANO_PAQUETE_ARRAY    El array de bytes no tiene el tamaño correcto.                                     <br>
     *    INFO_PAQUETE_FUERA_DE_RANGO  El numero del paquete enviado esta fuera de rango.                                 <br>
     * 
     * @param  id_de_escritura  El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @param  lista_paquetes   El contenido integro del fichero en una lista de paquetes.
     * @return int              Resultado de la operacion.
     * @throws RemoteException 
     */
    public int escribiendoFicheroEnServidorEnviandoPaquetes (String password_escritura, String id_de_escritura, List<FicheroPaquete> lista_paquetes) throws RemoteException;


    /**
     * Para saber que paquetes no ha llegado aun al servidor.                                                                                                            <br>
     * Si se devuelve lista vacia es que no falta ningun paquete pero si se devuelve un unico elemento negativo indica un problema.                                      <br>
     * Resultados negativos posibles:                                                                                                                                    <br>
     *    INFO_ID_NO_EXISTE            No existe el id_de_escritura                                                                                                      <br>
     *    INFO_PASSWORD_INVALIDA       La contraseña de escritura suministrada no es valida                                                                              <br>
     * 
     * @param  password_escritura  Se debe enviar la contraseña de escritura para crear una sesion para escribir un fichero.
     * @param  id_de_escritura     El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return List<Integer>       Cada posicion de la lista devuelta indica el numero del paquete que falta. Lista vacia si no falta ningun paquete. Un unico elemento con INFO_ID_NO_EXISTE  indica que no existe el id enviado. O INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     * @throws RemoteException 
     */
    public List<Integer> escribiendoFicheroEnServidorPaquetesQueFaltan(String password_escritura, String id_de_escritura) throws RemoteException;

    
    /**
     * Libera todos los recursos asociados a un fichero que se estaba enviando al servidor.           <br>
     * Es ahora cuando se almacena en disco sin que importe que se hayan enviado todos los paquetes.  <br>
     * Resultados:                                                                                    <br>
     *    ERROR_SIN_ERRORES  Operacion realizada correctamente                                        <br>
     *    INFO_ID_NO_EXISTE  No existe el id_de_escritura                                             <br>

     * @param  id_de_escritura   El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return int               Resultado de la operacion.
     * @throws RemoteException 
     */
    public int escribiendoFicheroEnServidorEnvioFinalizado(String password_escritura, String id_de_escritura) throws RemoteException;

    
    /**
     * Libera todos los recursos asociados a un fichero que se estaba enviando al servidor.           <br>
     * No escribe en disco. Es lo mismo que EnvioFinalizado pero sin escribir en disco.               <br>
     * Resultados:                                                                                    <br>
     *    ERROR_SIN_ERRORES  Operacion realizada correctamente                                        <br>
     *    INFO_ID_NO_EXISTE  No existe el id_de_escritura                                             <br>
     * 
     * @param  id_de_escritura   El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return int               Resultado de la operacion.
     * @throws RemoteException 
     */
    public int escribiendoFicheroEnServidorCancelar(String password_escritura, String id_de_escritura) throws RemoteException;

    
//------------------------------------------------------------------------------
    
    /**
     * Para iniciar la lectura de un fichero que esta almacenado en el servidor.                                                                                                     <br>
     * Se envia el nombre del fichero junto con el directorio para que no se confunda con otros posibles ficheros con el mismo nombre.                                               <br>
     * El nombre del directorio debe empezar sin barra pero si debe terminar en barra. Aqui no se comprueba que este bien escrito. Fallara si no se envia bien.                      <br>
     * El servidor crea id para esta comunicacion que es lo que le devuelve al cliente.                                                                                              <br>
     * El cliente usara este id para solicitar trozos del fichero.                                                                                                                   <br>
     * Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema:                                                                <br>
     *    INFO_FICHERO_NO_ENCONTRADO                           Fichero no encontrado                                                                                                 <br>
     *    INFO_LIMITE_MAX_IDS_POR_CREAR                        Se ha superado el limite de ids que se pueden crear.                                                                  <br>
     *    INFO_LIMITE_MAX_LECTORES_POR_ID                      Se ha superado el limite de lectores por ids.                                                                         <br>
     *    INFO_LIMITE_MAX_LECTORES                             Se ha superado el limite total de lectores que pueden acceder al servidor simultaneamente.                            <br>
     *    WARNIG_DURANTE_LA_ESPERA                             Mientras se esperaba a que quedara libre el fichero se produjo un error.                                              <br>
     *    INFO_PASSWORD_INVALIDA                               La contraseña de lectura suministrada no es valida                                                                    <br>
     *    INFO_ACCESO_NO_PERMITIDO                             Se ha denegado el acceso, no se permite crear nuevas sesiones de lectura en este momento.                             <br>
     *    INFO_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO  No debe estar el fichero en disco a la vez que esta marcado como borrado.                                             <br>
     * 
     * @param  password_lectura     Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  nombre_directorio    Sirve para encontrar el directorio del fichero
     * @param  nombre_fichero       El nombre del fichero. 
     * @param  longitud_del_paquete El cliente establece cual debe ser la longitud de array de bytes
     * @return String               Una cadena que identifica la comunicacion. Así se puede leer el fichero a trozos y sin prisas.
     * @throws RemoteException   
     */
    public String solicitarLeerFicheroDelServidor  (String password_lectura, String nombre_directorio, String nombre_fichero, int longitud_del_paquete) throws RemoteException;

    
    /**
     * Para leer un paquete de informacion del fichero almacenado en el servidor  <br>
     * 
     * @param  password_lectura Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  id_de_lectura    El identificador que creo 'solicitarLeerFicheroDelServidor' antes de empezar la comunicacion  
     * @param  num_del_paquete  El numero del paquete del que se desea leer
     * @return int[]            El paquete con la informacion. null si el id_de_lectura no existe o si la contraseña es invalida.
     * @throws RemoteException 
     */
    public int[] leyendoFicheroDelServidorLeerPaquete(String password_lectura, String id_de_lectura, int num_del_paquete) throws RemoteException;


    /**
     * METODO OPCIONAL: Necesita que el Cliente tenga la clase FicheroPaquete y esto es algo que no se puede exigir.      <br>
     *                                                                                                                    <br>
     * Con una única llamada se devuelven todos los paquetes.                                                             <br>
     * 
     * @param  password_lectura       Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  id_de_lectura          El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return List<FicheroPaquete>   El contenido integro del fichero en una lista de paquetes. Null si no existe el id_de_lectura o la contraseña es invalida.
     * @throws RemoteException 
     */
    public List<FicheroPaquete> leyendoFicheroDelServidorLeerPaquetes(String password_lectura, String id_de_lectura) throws RemoteException;


    /**
     * Devuelve el tamaño del paquete solicitado para la lectura.                                               <br>
     * Resultados:                                                                                              <br>
     *    INFO_ID_NO_EXISTE         El id_de_lectura no existe.                                                 <br>
     *    INFO_PASSWORD_INVALIDA    La contraseña de lectura suministrada no es valida                          <br> 
     * 
     * @param  password_lectura Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  id_de_lectura    El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return int              El tamaño del paquete asociado a la lectura del id proporcionado. 
     */
    public int leyendoFicheroDelServidorTamanoDelPaquete (String password_lectura, String id_de_lectura) throws RemoteException;
    
    /**
     * Devuelve la cantidad de paquetes que componen el fichero.
     * Resultados:                                                                                              <br>
     *    INFO_ID_NO_EXISTE         El id_de_lectura no existe.                                                 <br>
     *    INFO_PASSWORD_INVALIDA    La contraseña de lectura suministrada no es valida                          <br> 
     * 
     * @param  password_lectura Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  id_de_lectura    El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return int              Cantidad de paquetes de los que se componen el fichero. -21 el id_de_lectura no existe.
     * @throws RemoteException 
     */
    public int leyendoFicheroDelServidorNumeroDePaquetes  (String password_lectura, String id_de_lectura) throws RemoteException;

    
    /**
     * Para conocer cuantos bytes son validos del paquete enviado.                                                                                   <br>
     * Para todos los paquetes menos para el ultimo este valor coincidira con la longitud del paquete que se estableciera al solicitar la lectura.   <br>
     * Resultados:                                                                                                                                   <br>
     *    INFO_ID_NO_EXISTE         El id_de_lectura no existe.                                                                                      <br>
     *    INFO_PASSWORD_INVALIDA    La contraseña de lectura suministrada no es valida                                                               <br> 
     * 
     * @param  password_lectura  Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  id_de_lectura     El identificador que creo 'solicitarLeerFicheroDelServidor' antes de empezar la comunicacion  
     * @param  num_del_paquete   El numero del paquete del que se desea conocer 
     * @return int               El numero de bytes que son validos para el numero de paquete solicitado.
     * @throws RemoteException 
     */
    public int leyendoFicheroDelServidorBytesDelPaquete(String password_lectura, String id_de_lectura, int num_del_paquete) throws RemoteException;
    

    /**
     * Para informar al servidor que ya a leido todos los paquetes y que puede liberar todos los recursos asociados a la cumunicacion.                     <br>
     * Resultados:                                                                                                                                         <br>
     *                              Numero positivo da una idea de los lectores que quedan. Aproximado porque otros hilos pueden haber modificado el valor <br>
     *    ERROR_SIN_ERRORES         Cero indica que ya no quedan lectores.                                                                                 <br>
     *    INFO_ID_NO_EXISTE         No existe el id_de_lectura                                                                                             <br>
     *    INFO_PASSWORD_INVALIDA    La contraseña de lectura suministrada no es valida                                                                     <br> 
     * 
     * @param  password_lectura  Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  id_de_lectura     El identificador que creo 'solicitarLeerFicheroDelServidor' antes de empezar la comunicacion 
     * @return int               Un numero que indica el resultado de la operacion.
     * @throws RemoteException 
     */
    public int leyendoFicheroDelServidorLecturaFinalizada (String password_lectura, String id_de_lectura) throws RemoteException;
    

//------------------------------------------------------------------------------
    
    
    /**
     * Borra un fichero que este almacenado en el servidor.                                                                               <br>
     * Se devuelve un codigo informando del resultado.                                                                                    <br>
     *    ERROR_SIN_ERRORES                                    Operacion realizada correctamente.                                         <br>
     *    INFO_FICHERO_NO_ENCONTRADO                           Fichero no encontrado.                                                     <br>
     *    WARNIG_DURANTE_LA_ESPERA                             Mientras se esperaba a que quedara libre el fichero se produjo un error.   <br>
     *    INFO_ACCESO_NO_PERMITIDO                             No se permite realizar la operacion, se han bloqueado los accesos.         <br>
     *    INFO_PASSWORD_INVALIDA                               La contraseña de escritura suministrada no es valida                       <br>
     *    INFO_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO  No debe estar el fichero en disco a la vez que esta marcado como borrado.  <br>
     *
     * @param  password_escritura    Se debe enviar la contraseña de escritura para borrar un fichero.
     * @param  nombre_directorio     Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros. Sin barra al principio para indicar ruta relativa y con barra final
     * @param  nombre_fichero        El nombre del fichero. 
     * @return int                   Codigo para explicar el resultado de la operacion
     * @throws RemoteException 
     */
    public int borrarFicheroDelServidor (String password_escritura, String nombre_directorio, String nombre_fichero) throws RemoteException;


    /**
     * Para saber si existe un fichero almacenado en el servidor.                                                                              <br>
     * Debido a los accesos simultaneos, el resultado de este metodo puede ser efimero.                                                        <br>
     * solicitarLeer es una forma equivalente de conocer si existe un fichero.                                                                 <br>
     * Ya dependera de nivel de intensidad con el que se modifiquen los ficheros del servidor.                                                 <br>
     *                                                                                                                                         <br>
     * Se devuelve un codigo informando del resultado.                                                                                         <br>
     *     Numero Positivo todo correcto:                                                                                                      <br>
     *         1               Existe el fichero                                                                                               <br>
     *         0               No existe el fichero.                                                                                           <br>
     *     Numero Negativo hubo algun problema:                                                                                                <br>
     *         INFO_ACCESO_NO_PERMITIDO   No se permite realizar la operacion, se han bloqueado los accesos.                                   <br>
     *         INFO_PASSWORD_INVALIDA     La contraseña de lectura suministrada no es valida                                                   <br>
     * 
     * @param  password_lectura    Se debe enviar la contraseña de lectura para realizar la pregunta.
     * @param  nombre_directorio   Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros. Sin barra al principio para indicar ruta relativa y con barra final
     * @param  nombre_fichero      El nombre del fichero. 
     * @return int                 Codigo para explicar el resultado de la operacion 
     * @throws RemoteException 
     */
    public int existeFicheroEnServidor (String password_lectura, String nombre_directorio, String nombre_fichero)  throws RemoteException;
    
    
    /**
     * Para conocer el directorio relativo o el fichero que se esta enviando.                                                                              <br>
     * Desde le punto de vista del cliente puede parecer absurdo porque es una informacion que suministra al solicitar la escritura.                       <br>
     * Pero puede haber servidores intermedios. El cliente se comunica con el servidor intermedio y el servidor intermedio con el Almacen de Ficheros.     <br>
     * El servidor intermedio solo reenvia la peticion. Pero en determinadas ocasiones puede necesitar la informacion que el cliente esta enviando.        <br>
     *                                                                                                                                                     <br>
     * Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema:                                      <br>
     *    INFO_ID_NO_EXISTE        No existe el id_de_escritura                                                                                            <br>
     *    INFO_PASSWORD_INVALIDA   La contraseña de escritura suministrada no es valida                                                                    <br>
     * 
     * @param  password_escritura   Se debe enviar la contraseña de escritura pues se esta tratando con un proceso de escritura.
     * @param  id_de_escritura      El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @param  dir_o_file           Numero positivo indica que quiere el directorio. Numero negativo indica que quiere el fichero. Cero devolvera la cadena vacia.
     * @return String               Una cadena que es el nombre del directorio relativo o el nombre del fichero. Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema.
     */
    public String escribiendoFicheroEnServidorInfoFichero(String password_escritura, String id_de_escritura, int dir_o_file) throws RemoteException;
    
    
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------

    
    /**
     * Actualiza el valor de la contraseña de lectura
     * 
     * @param  password_administracion  Se debe proporcionar la contraseña de administracion para realizar esta tarea.
     * @param  nueva_password_lectura   La nueva contraseña de lectura.
     * @return int                      El resultado de la operacion. Cero si todo va bien, INFO_PASSWORD_INVALIDA  si la contraseña de administracion no es corecta.
     * @throws RemoteException 
     */
    public int adminCambiarPasswordLectura(String password_administracion, String nueva_password_lectura) throws RemoteException;
    
    
    /**
     * Actualiza el valor de la contraseña de escritura
     * 
     * @param  password_administracion   Se debe proporcionar la contraseña de administracion para realizar esta tarea.
     * @param  nueva_password_escritura  La nueva contraseña de escritura.
     * @return int                       El resultado de la operacion. Cero si todo va bien, INFO_PASSWORD_INVALIDA  si la contraseña de administracion no es corecta.
     * @throws RemoteException 
     */
    public int adminCambiarPasswordEscritura(String password_administracion, String nueva_password_escritura) throws RemoteException;

    
    /**
     * Actualiza el valor de la contraseña de procesos
     * 
     * @param  password_administracion   Se debe proporcionar la contraseña de administracion para realizar esta tarea.
     * @param  nueva_password_procesos   La nueva contraseña de procesos.
     * @return int                       El resultado de la operacion. Cero si todo va bien, INFO_PASSWORD_INVALIDA  si la contraseña de administracion no es corecta.
     * @throws RemoteException 
     */
    public int adminCambiarPasswordProceso(String password_administracion, String nueva_password_procesos) throws RemoteException;
    

    /**
     * Actualiza el valor de la contraseña de administracion
     * 
     * @param  password_administracion_antigua  Se debe proporcionar la contraseña de administracion para realizar esta tarea.
     * @param  password_administracion_nueva    La nueva contraseña de administracion.
     * @return int                              El resultado de la operacion. Cero si todo va bien, INFO_PASSWORD_INVALIDA  si la contraseña de administracion antigua no es corecta.
     * @throws RemoteException 
     */
    public int adminCambiarPasswordAdministracion(String password_administracion_antigua, String password_administracion_nueva) throws RemoteException;
    
    
    /**
     * Para permitir el acceso o no a los metodos que crean nuevas sesiones.                                <br>
     * Si no se permite el acceso no se podran crear nuevas lecturas o escrituras asi como borrar archivos. <br>
     * Su finalidad es la de apagar el servidor dejando que terminen las tareas en curso.                   <br>
     * 
     * @param  password_administracion                 Se debe proporcionar la contraseña de administracion para realizar esta tarea.
     * @param  valor_acceso_permitido_lectura          true permitira el acceso para crear sesiones de lectura,   false no permitira el acceso para crear sesiones de lectura
     * @param  valor_acceso_permitido_escritura        true permitira el acceso para crear sesiones de escritura, false no permitira el acceso para crear sesiones de escritura
     * @param  valor_acceso_permitido_conexion_pasiva  true permitira aceptar conexiones pasivas,                 false no permitira aceptar conexiones pasivas.                 Grupo Y.
     * @param  valor_acceso_permitido_sincronizar      true permitira iniciar procesos de sincronizacion,         false no permitira iniciar procesos de sincronización.         Grupo Z.
     * @return int                                     El resultado de la operacion. Cero si todo va bien, INFO_PASSWORD_INVALIDA  si la contraseña de administracion antigua no es corecta.
     * @throws RemoteException 
     */
    public int adminCambiarAccesoPermitido(String password_administracion, boolean valor_acceso_permitido_lectura, boolean valor_acceso_permitido_escritura, boolean valor_acceso_permitido_conexion_pasiva, boolean valor_acceso_permitido_sincronizar) throws RemoteException;
    
    
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------

    
    /**
     * Un apagado controlado del servidor consta de las siguientes tareas:                        <br>
     *    1) Finalizar los hilos run de las tareas periodicas.                                    <br>
     *    2) Informar a otros servidores. Opcional debido al procedimiento de comprobarConexiones <br>
     *    3) Desregistrar el objeto remoto de rmiregistry.
     * 
     * @param  password_administracion Se debe enviar la contraseña de administracion para apagar el servidor.
     * @return int                     Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     */
    public int apagar(String password_administracion) throws RemoteException;
            
    /**
     * Da por finalizada la ejecucion del hilo run y no se puede volver a activar, sería necesario volver a crearlo
     * 
     * @param  password_administracion    Se debe enviar la contraseña de administracion para finalizar a run()
     * @return int                        Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     * @throws RemoteException 
     */
    public int  finalizarHiloRun(String password_administracion) throws RemoteException;
    
    
    /**
     * Usando el valor del parametro, se deja en pausa o se reativa la ejecucion del hilo.
     * 
     * @param  password_procesos   Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @param  valor_pausa         true para indicar que debe estar en pausa. false indica que se puede ejecutar el hilo
     * @return int                 Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     * @throws RemoteException 
     */
    public int  pausaHiloRun(String password_procesos, boolean valor_pausa) throws RemoteException;

    
    /**
     * Dice si se debe ejecutar el proceso de Caducar Sesiones
     * 
     * @param  password_procesos      Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @param  valor_caducar_sesiones true permite ejecutar el proceso, false impide que se ejecute el proceso. 
     * @return int                    Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     * @throws RemoteException 
     */    
    public int  permitirEjecutarCaducarSesiones(String password_procesos, boolean valor_caducar_sesiones)  throws RemoteException;
            
            
    /**
     * Dice si se debe ejecutar el proceso de Comprobar Conexiones
     * 
     * @param  password_procesos          Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @param  valor_comprobar_conexiones true permite ejecutar el proceso, false impide que se ejecute el proceso. 
     * @return int                        Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     * @throws RemoteException 
     */
    public int  permitirEjecutarComprobarConexiones(String password_procesos, boolean valor_comprobar_conexiones) throws RemoteException;

    
    /**
     * Dice si se debe ejecutar el proceso de Sincronizar Ficheros.
     * 
     * @param  password_procesos          Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @param  valor_sincronizar_ficheros true permite ejecutar el proceso, false impide que se ejecute el proceso. 
     * @return int                        Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     * @throws RemoteException 
     */
    public int  permitirEjecutarSincronizarFicheros(String password_procesos, boolean valor_sincronizar_ficheros) throws RemoteException;
        
    
    /**
     * Una forma manual de ejecutar el proceso de Caducar Sesiones.
     * 
     * @param  password_procesos  Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @return int                Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida. INFO_ACCESO_NO_PERMITIDO  si no hay permiso para ejecutar el proceso.
     * @throws RemoteException 
     */
    public int  ejecutarAhoraCaducarSesiones(String password_procesos) throws RemoteException;
            

    /**
     * Una forma manual de ejecutar el proceso de Comprobar Conexiones.
     * 
     * @param  password_procesos  Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @return int                Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida. INFO_ACCESO_NO_PERMITIDO  si no hay permiso para ejecutar el proceso.
     * @throws RemoteException 
     */
    public int  ejecutarAhoraComprobarConexiones(String password_procesos) throws RemoteException;
    
    
    /**
     * Una forma manual de ejecutar el proceso de SincronizarFicheros.
     * 
     * @param  password_procesos  Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @return int                Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida. INFO_ACCESO_NO_PERMITIDO  si no hay permiso para ejecutar el proceso.
     * @throws RemoteException 
     */    
    public int  ejecutarAhoraSincronizarFicheros(String password_procesos) throws RemoteException;
            
    
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
    

    /**
     * Devuelve los almacenes conocidos, sus IDs.                                                                                                                          <br>
     * Es el resultado de unir las listas de activos, pasivos y orden1                                                                                                     <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @return List<String>             La lista de IDs de los almacenes conocidos.
     * @throws RemoteException 
     */
    public List<String>  listaDeAlmacenesOrdenCero(String password_administracion) throws RemoteException;

    
    /**
     * Devuelve un Almacen dado su ID. Lo busca en las listas de activos y pasivos           <br>
     * Podría no existir el ID aunque se hubiera obtenido de 'listaDeAlmacenesConocidos()'   <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @param  id_almacen_remoto        El id de un almacen remoto
     * @return AlmacenInterfaceRemote   El almacen remotos solicitado. null si el id no existe. Podría no existir el ID aunque se hubiera obtenido de 'listaDeAlmacenesConocidos()'
     * @throws RemoteException 
     */
    public AlmacenInterfaceRemote almacenOrdenCero(String password_administracion, String id_almacen_remoto) throws RemoteException;

    
    /**
     * Solicita conectarse para sincronizar los fichero.                                    <br>
     * Quedara anotado como servidor pasivo. No podra ser uno que este anotado como activo. <br>
     * 
     * @param   password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @param   solicitante              El almacen que solicita conectarse. 
     * @return  boolean                  true si acepta la conexion, false si no la acepta y debe buscar a otro con quien sincronizarse.
     * @throws  RemoteException 
     */
    public boolean solicitarConexion(String password_administracion, AlmacenInterfaceRemote solicitante) throws RemoteException;
    

    /**
     * Debido a posibles cortes de conexion que producen reoganizacion de las listas, un serviror puede descartar una conexion mientras otro no. <br>
     * Con este metodo, el que no descarto la conexion la vuelve a pedir y asi se reorganizan las listas en ambos lados.                         <br>
     * Se comprueba que se tenga como activo al solicitante. Si no se tiene se rechaza, no se le intentara poner como activo.                    <br>
     * Es decir, solo el llamante debera reajustar sus listas o maps, aqui solo se entra en modo lectura.                                        <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @param  id_solicitante           El id del almacen que informa que tiene con nosotros una conexion establecida
     * @return boolean                  true si confirma que acepta la conexion. false si ahora rechaza la conexion.     
     * @throws RemoteException 
     */
    public boolean confirmarConexionActiva(String password_administracion, String  id_solicitante) throws RemoteException;

    
    /**
     * Debido a posibles cortes de conexion que producen reoganizacion de las listas, un serviror puede descartar una conexion mientras otro no. <br>
     * Con este metodo, el que no descarto la conexion la vuelve a pedir y asi se reorganizan las listas en ambos lados.                         <br>
     * Se comprueba que se tenga como pasivo al solicitante. Si no se tiene se rechaza, no se le intentara poner como pasivo.                    <br>
     * Es decir, solo el llamante debera reajustar sus listas o maps, aqui solo se entra en modo lectura.                                        <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @param  id_solicitante           El id del almacen que informa que tiene con nosotros una conexion establecida
     * @return boolean                  true si confirma que acepta la conexion. false si ahora rechaza la conexion.     
     * @throws RemoteException 
     */
    public boolean confirmarConexionPasiva(String password_administracion, String  id_solicitante) throws RemoteException;
    
    
    /**
     * Un almacen informa de que se va a apagar o desconectar.                                             <br>
     * De esta forma se le saca de las listas y no dara fallos de conexion al intentar contactar con el.   <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @param  almacen_baja             El almacen que informa que se va a apagar o descnoectar, para que lo saquemos de las listas
     * @return int                      Un codigo resultado de la operacion: INFO_PASSWORD_INVALIDA  si la contraseña no es valida.
     * @throws RemoteException 
     */
    public int informandoBaja(String password_administracion, AlmacenInterfaceRemote almacen_baja) throws RemoteException;
    
    
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
//------------------------------------------------------------------------------
    
    
    /**
     * Devuelve todos los fichero que gestiona este almacen.                                                  <br>
     * Ahora mismo devulve una referencia porque se entiendo que es para usarlo con rmi que creara una copia. <br>
     * 
     * @param  password_lectura         Se debe enviar la contraseña de lectura para conocer los ficheros del almacén.
     * @return Map<String, FicheroInfo> Una coleccion de los fichero que hay en el almacen con la version y la marca de borrado. null si la contraseña no es valida.
     * @throws RemoteException 
     */
    public Map<String, FicheroInfo> ficherosDelAlmacen(String password_lectura) throws RemoteException;


    /**
     * Antes de que un servidor puede iniciar la sincronizacion debe solicitarlo.                                                                                                            <br>
     * Se le dara permiso si no se le ha dado a otro. Tambien debe estar en la lista de pasivos.                                                                                             <br>
     * Si durante el control de conexion con otros servidores se perdiera el contacto y se tuviera que sacar de la lista de pasivos se quitaria el permiso se sincronizar.                   <br>
     * Esto es debido a que se trata de un proceso que bloquea lecturas y escrituras y no puede ser ejecutado por todos los servidores a la vez.                                             <br>
     * Si varios servidores inician el proceso de sincronizacion a la vez puede producirse una situacion de interbloqueo entre servidores: todos quieren escribir pero no se dejan escribir. <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_servidor                 El servidor que solicita la sincronizacion debe proporcionar su id.
     * @return boolean                     true indica que se permite iniciar la sincronizacion.
     * @throws RemoteException 
     */
    public boolean sincronizarSolicitarIniciar(String password_administracion, String id_servidor) throws RemoteException;

    
    /**
     * Da por finalizada la sincronizacion. No se permitira el acceso a los metodos de sincronizacion.
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @return int                         Cero si todo va bien. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     * @throws RemoteException 
     */
    public int sincronizarInformarFinalizar(String password_administracion) throws RemoteException;

    
    /**
     * Si id_de_fichero envidado corresponde a un fichero que esta marcado como borrado en la estructura de ficheros_del_almacen, entonces se quitara esa entrada de la estructura. <br>
     * Resultados:                                                                                                                                                                  <br>
     *    INFO_PASSWORD_INVALIDA            La contraseña no es valida                                                                                                              <br>
     *    INFO_SINCRONIZACION_SIN_PERMISO   No tiene permiso para realizar la operacion solicitada.                                                                                 <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen                  El id del servidor que se esta sincronizando.
     * @param  id_de_fichero               El id del fichero a quitar de la estructura si esta marcado como borrado.
     * @return int                         Un codigo resultado de la operacion. 0 si todo correcto.
     * @throws RemoteException 
     */
    public int sincronizarEliminarDeFicherosDeLaEstructura(String password_administracion, String id_almacen, String id_de_fichero) throws RemoteException;
    

    /**
     * Mismo metodo que solicitarEscribirFicheroEnServidor pero para la sincronizacion.                                                                                                     <br>
     * Es necesario que haya metodos especificos para la sincronizacion debido a que se debe proporcionar el id del servidor.                                                               <br>
     * A los códigos de resultado que devuelve el metodo normal se añade INFO_SINCRONIZACION_SIN_PERMISO .                                                                                  <br>
     *                                                                                                                                                                                      <br>
     * Para iniciar el envio de un fichero desde el cliente al servidor.                                                                                                                    <br>
     * Se envia el nombre del fichero junto con el directorio para que no se confunda con otros posibles ficheros con el mismo nombre.                                                      <br>
     * El nombre del directorio debe empezar sin barra pero si debe terminar en barra. Aqui no se comprueba que este bien escrito. Fallara si no se envia bien.                             <br>
     * El servidor crea id para esta comunicacion que es lo que le devuelve al cliente.                                                                                                     <br>
     * El cliente usara este id para enviar su fichero y que no se mezcle la informacion de otros ficheros que se esten subiendo a la vez.                                                  <br>
     * El fichero permanecera en memoria del servidor hasta que no llame al metodo que da por finalizado el envio y lo escribe a disco                                                      <br>
     * Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema:                                                                       <br>
     *    INFO_FICHERO_YA_EXISTE            El fichero ya existe, el enviado no es mas nuevo que el almacenado.                                                                             <br>
     *    INFO_LIMITE_MAX_IDS_POR_CREAR     Se ha superado el limite ids de escritura que se pueden crear. Solo puede haber un escritor por archivo pero varios para archivos diferentes.   <br>
     *    WARNIG_DURANTE_LA_ESPERA          Mientras se esperaba a que quedara libre el fichero se produjo un error.                                                                        <br>
     *    INFO_SINCRONIZACION_SIN_PERMISO   Si el servidor no tiene permiso para sincronizarse.                                                                                             <br>
     *    INFO_PASSWORD_INVALIDA            La contraseña de administracion suministrada no es valida                                                                                       <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen               El id del servidor que se esta sincronizando.
     * @param  nombre_directorio        Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros. Debe venir sin barra inicial pero con barra final.
     * @param  nombre_fichero           El nombre del fichero. 
     * @param  version                  Una cadena para comparar la antiguedad de ficheros en reescrituras. Usa  ver_antigua.compareTo(ver_nueva)
     * @param  longitud_del_paquete     Para indicar cuantos bytes se enviaran cada vez que se mande un paquete
     * @param  cantidad_paquetes        Se debe decir antes de empezar cuantos paquetes seran enviados. Junto con longitud del paquete se sabra el tamaño del fichero.
     * @return String                   Una cadena que identifica la comunicacion. Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema,
     * @throws RemoteException 
     */
    public String sincronizarSolicitarEscribirFicheroEnServidor (String password_administracion, String id_almacen, String nombre_directorio, String nombre_fichero, String fecha_version, int longitud_del_paquete, int cantidad_paquetes) throws RemoteException;


    /**
     * Mismo metodo que escribiendoFicheroEnServidorEnviandoPaquetes pero para la sincronizacion.                              <br>
     * Es necesario que haya metodos especificos para la sincronizacion debido a que se debe proporcionar el id del servidor.  <br>
     * A los códigos de resultado que devuelve el metodo normal se añade INFO_SINCRONIZACION_SIN_PERMISO .                     <br>
     *                                                                                                                         <br>
     * Con una única llamada se envían todos los paquetes.                                                                     <br>
     * Resultados:                                                                                                             <br>
     *    ERROR_SIN_ERRORES                 Operacion realizada correctamente                                                  <br>
     *    INFO_ID_NO_EXISTE                 No existe el id_de_escritura                                                       <br>
     *    INFO_TAMANO_PAQUETE_ARRAY         El array de bytes no tiene el tamaño correcto.                                     <br>
     *    INFO_PAQUETE_FUERA_DE_RANGO       El numero del paquete enviado esta fuera de rango.                                 <br>
     *    INFO_SINCRONIZACION_SIN_PERMISO   Si el servidor no tiene permiso para sincronizarse.                                <br>
     *    INFO_PASSWORD_INVALIDA            La contraseña de administracion suministrada no es valida                          <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen               El id del servidor que se esta sincronizando.
     * @param  id_de_escritura          El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @param  lista_paquetes           El contenido integro del fichero en una lista de paquetes.
     * @return int                      Resultado de la operacion.
     * @throws RemoteException 
     */
    public int sincronizarEscribiendoFicheroEnServidorEnviandoPaquetes (String password_administracion, String id_almacen, String id_de_escritura, List<FicheroPaquete> lista_paquetes) throws RemoteException;


    /**
     * Mismo metodo que escribiendoFicheroEnServidorEnvioFinalizado pero para la sincronizacion.                               <br>
     * Es necesario que haya metodos especificos para la sincronizacion debido a que se debe proporcionar el id del servidor.  <br>
     * A los códigos de resultado que devuelve el metodo normal se añade INFO_SINCRONIZACION_SIN_PERMISO .                     <br>
     *                                                                                                                         <br>
     * Para este metodo esta la duda de si hacerle caso o no cuando no hay permiso pues se podria dejar bloqueado el fichero.  <br>
     * Se puede haber iniciado la sincronizacion y por lo que sea se quito el permiso durante la escritura.                    <br>
     * Con la implementacion de sesiones que caducan no hay problemas.                                                         <br>
     *                                                                                                                         <br>
     * Libera todos los recursos asociados a un fichero que se estaba enviando al servidor.                                    <br>
     * Es ahora cuando se almacena en disco sin que importe que se hayan enviado todos los paquetes.                           <br>
     * Resultados:                                                                                                             <br>
     *    ERROR_SIN_ERRORES                 Operacion realizada correctamente                                                  <br>
     *    INFO_ID_NO_EXISTE                 No existe el id_de_escritura                                                       <br>
     *    INFO_SINCRONIZACION_SIN_PERMISO   Si el servidor no tiene permiso para sincronizarse.                                <br>
     *    INFO_PASSWORD_INVALIDA            La contraseña de administracion suministrada no es valida                          <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen                  El id del servidor que se esta sincronizando.
     * @param  id_de_escritura             El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return int                         Resultado de la operacion.
     * @throws RemoteException 
     */
    public int sincronizarEscribiendoFicheroEnServidorEnvioFinalizado(String password_administracion, String id_almacen, String id_de_escritura) throws RemoteException;

    
    /**
     * Mismo metodo que escribiendoFicheroEnServidorCancelar pero para la sincronizacion.                                      <br>
     * Es necesario que haya metodos especificos para la sincronizacion debido a que se debe proporcionar el id del servidor.  <br>
     * A los códigos de resultado que devuelve el metodo normal se añade INFO_SINCRONIZACION_SIN_PERMISO .                     <br>
     *                                                                                                                         <br>
     * Para este metodo esta la duda de si hacerle caso o no cuando no hay permiso pues se podria dejar bloqueado el fichero.  <br>
     * Se puede haber iniciado la sincronizacion y por lo que sea se quito el permiso durante la escritura.                    <br>
     * Con la implementacion de sesiones que caducan no hay problemas.                                                         <br>
     *                                                                                                                         <br>
     * Libera todos los recursos asociados a un fichero que se estaba enviando al servidor.                                    <br>
     * No escribe en disco. Es lo mismo que EnvioFinalizado pero sin escribir en disco.                                        <br>
     * Resultados:                                                                                                             <br>
     *    ERROR_SIN_ERRORES                 Operacion realizada correctamente                                                  <br>
     *    INFO_ID_NO_EXISTE                 No existe el id_de_escritura                                                       <br>
     *    INFO_SINCRONIZACION_SIN_PERMISO   Si el servidor no tiene permiso para sincronizarse.                                <br>
     *    INFO_PASSWORD_INVALIDA            La contraseña de administracion suministrada no es valida                          <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen                  El id del servidor que se esta sincronizando.
     * @param  id_de_escritura             El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return int                         Resultado de la operacion.
     * @throws RemoteException 
     */
    public int sincronizarEscribiendoFicheroEnServidorCancelar(String password_administracion, String id_almacen, String id_de_escritura) throws RemoteException;

    
    /**
     * Mismo metodo que borrarFicheroDelServidor pero para la sincronizacion.                                                             <br>
     * Es necesario que haya metodos especificos para la sincronizacion debido a que se debe proporcionar el id del servidor.             <br>
     * A los códigos de resultado que devuelve el metodo normal se añade INFO_SINCRONIZACION_SIN_PERMISO .                                <br>
     *                                                                                                                                    <br>
     * Borra un fichero que este almacenado en el servidor.                                                                               <br>
     * Se devuelve un codigo informando del resultado.                                                                                    <br>
     *    ERROR_SIN_ERRORES                                    Operacion realizada correctamente.                                         <br>
     *    INFO_FICHERO_NO_ENCONTRADO                           Fichero no encontrado.                                                     <br>
     *    WARNIG_DURANTE_LA_ESPERA                             Mientras se esperaba a que quedara libre el fichero se produjo un error.   <br>
     *    INFO_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO  No debe estar el fichero en disco a la vez que esta marcado como borrado.  <br>
     *    INFO_SINCRONIZACION_SIN_PERMISO                      Si el servidor no tiene permiso para sincronizarse.                        <br>
     *    INFO_PASSWORD_INVALIDA                               La contraseña de administracion suministrada no es valida                  <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen                  El id del servidor que se esta sincronizando.
     * @param  nombre_directorio           Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros. Sin barra al principio para indicar ruta relativa y con barra final
     * @param  nombre_fichero              El nombre del fichero. 
     * @return int                         Codigo para explicar el resultado de la operacion
     * @throws RemoteException 
     */    
    public int sincronizarBorrarFicheroDelServidor(String password_administracion, String id_almacen, String nombre_directorio, String nombre_fichero) throws RemoteException;


    /**
     * Mismo metodo que borrarFicheroDelServidor pero para la sincronizacion.                                                                                                        <br>
     * Es necesario que haya metodos especificos para la sincronizacion debido a que se debe proporcionar el id del servidor.                                                        <br>
     * A los códigos de resultado que devuelve el metodo normal se añade INFO_SINCRONIZACION_SIN_PERMISO .                                                                           <br>
     *                                                                                                                                                                               <br>
     * Para iniciar la lectura de un fichero que esta almacenado en el servidor.                                                                                                     <br>
     * Se envia el nombre del fichero junto con el directorio para que no se confunda con otros posibles ficheros con el mismo nombre.                                               <br>
     * El nombre del directorio debe empezar sin barra pero si debe terminar en barra. Aqui no se comprueba que este bien escrito. Fallara si no se envia bien.                      <br>
     * El servidor crea id para esta comunicacion que es lo que le devuelve al cliente.                                                                                              <br>
     * El cliente usara este id para solicitar trozos del fichero.                                                                                                                   <br>
     * Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema:                                                                <br>
     *    INFO_FICHERO_NO_ENCONTRADO                           Fichero no encontrado                                                                                                 <br>
     *    INFO_LIMITE_MAX_IDS_POR_CREAR                        Se ha superado el limite de ids que se pueden crear.                                                                  <br>
     *    INFO_LIMITE_MAX_LECTORES_POR_ID                      Se ha superado el limite de lectores por ids.                                                                         <br>
     *    INFO_LIMITE_MAX_LECTORES                             Se ha superado el limite total de lectores que pueden acceder al servidor simultaneamente.                            <br>
     *    WARNIG_DURANTE_LA_ESPERA                             Mientras se esperaba a que quedara libre el fichero se produjo un error.                                              <br>
     *    INFO_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO  No debe estar el fichero en disco a la vez que esta marcado como borrado.                                             <br>
     *    INFO_SINCRONIZACION_SIN_PERMISO                      Si el servidor no tiene permiso para sincronizarse.                                                                   <br>
     *    INFO_PASSWORD_INVALIDA                               La contraseña de administracion suministrada no es valida                                                             <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen                  El id del servidor que se esta sincronizando.
     * @param  nombre_directorio           Sirve para encontrar el directorio del fichero
     * @param  nombre_fichero              El nombre del fichero. 
     * @param  longitud_del_paquete        El cliente establece cual debe ser la longitud de array de bytes
     * @return String                      Una cadena que identifica la comunicacion. Así se puede leer el fichero a trozos y sin prisas.
     * @throws RemoteException   
     */
    public String sincronizarSolicitarLeerFicheroDelServidor  (String password_administracion, String id_almacen, String nombre_directorio, String nombre_fichero, int longitud_del_paquete) throws RemoteException;


    /**
     * Mismo metodo que borrarFicheroDelServidor pero para la sincronizacion.                                                                                                        <br>
     * Es necesario que haya metodos especificos para la sincronizacion debido a que se debe proporcionar el id del servidor.                                                        <br>
     * Esta vez no hay códigos de resultado, no dice INFO_SINCRONIZACION_SIN_PERMISO  pero si lo comprueba y devolvera null si no tiene permiso.                                     <br>
     *                                                                                                                                                                               <br>
     * Con una única llamada se devuelven todos los paquetes.                                                                                                                        <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen                  El id del servidor que se esta sincronizando.
     * @param  id_de_escritura             El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return List<FicheroPaquete>        El contenido integro del fichero en una lista de paquetes. Null si no existe el id_de_lectura o la contraseña no es valida.
     * @throws RemoteException 
     */
    public List<FicheroPaquete> sincronizarLeyendoFicheroDelServidorLeerPaquetes(String password_administracion, String id_almacen, String id_de_lectura) throws RemoteException;


    /**
     * Mismo metodo que leyendoFicheroDelServidorLecturaFinalizada pero para la sincronizacion.                                                                         <br>
     * Es necesario que haya metodos especificos para la sincronizacion debido a que se debe proporcionar el id del servidor.                                           <br>
     * A los códigos de resultado que devuelve el metodo normal se añade INFO_SINCRONIZACION_SIN_PERMISO .                                                              <br>
     *                                                                                                                                                                  <br>
     * Para este metodo esta la duda de si hacerle caso o no cuando no hay permiso pues se podria dejar bloqueado el fichero.                                           <br>
     * Se puede haber iniciado la sincronizacion y por lo que sea se quito el permiso durante la escritura.                                                             <br>
     * Con la implementacion de sesiones que caducan no hay problemas.                                                                                                  <br>
     *                                                                                                                                                                  <br>
     * Para informar al servidor que ya a leido todos los paquetes y que puede liberar todos los recursos asociados a la cumunicacion.                                  <br>
     * Resultados:                                                                                                                                                      <br>
     *                                       Numero positivo da una idea de los lectores que quedan. Aproximado porque otros hilos pueden haber modificado el valor     <br>
     *    ERROR_SIN_ERRORES                  Cero indica que ya no quedan lectores.                                                                                     <br>
     *    INFO_ID_NO_EXISTE                  No existe el id_de_lectura                                                                                                 <br>
     *    INFO_SINCRONIZACION_SIN_PERMISO    Si el servidor no tiene permiso para sincronizarse.                                                                        <br>
     *    INFO_PASSWORD_INVALIDA            La contraseña de administracion suministrada no es valida                                                                   <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen                  El id del servidor que se esta sincronizando.
     * @param  id_de_lectura               El identificador que creo 'solicitarLeerFicheroDelServidor' antes de empezar la comunicacion 
     * @return int                         Un numero que indica el resultado de la operacion.
     * @throws RemoteException 
     */
    public int sincronizarLeyendoFicheroDelServidorLecturaFinalizada (String password_administracion, String id_almacen, String id_de_lectura) throws RemoteException;

}/*END INTERFACE AlmacenInterfaceRemote*/
