package almacenficheros;

import java.util.Date;
import java.text.SimpleDateFormat;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.util.concurrent.Semaphore;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import java.util.Properties;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 * Para almacenar ficheros en un servidor asi como realizar sus lecturas.                                                                                                                         <br>
 * Se hace por paquetes y los paquetes pueden ser escritos o leidos en cualquier orden, incluso repetirlos.                                                                                       <br>
 * Se permiten varias escrituras simulataneas pero a ficheros diferentes.                                                                                                                         <br>
 * Esta version mantiene todo el fichero en memoria hasta finalizar la lectura o la escritura. No esta pensado para ficheros grandes.                                                             <br>
 *                                                                                                                                                                                                <br>
 * El lector y el escritor podrian solicitar el tamaño del paquete.                                                                                                                               <br>
 * Pero para continuar una escritura pendiente el tamaño del paquete debe ser memorizado porque este no puede cambiar                                                                             <br>
 *                                                                                                                                                                                                <br>
 * En las variables existe algunas que son casi constantes, se les da un valor y no se espera que cambien durante la ejecucion.                                                                   <br>
 * Por este motivo se podría pensar en sacarlas del grupo al que pertenecen para asi poder acceder a ellas sin bloqueos.                                                                          <br>
 * Pero mejor se deja como esta ya que permitira crear metodos en un futuro que modifiquen el valor durante la ejecucion.                                                                         <br>
 *                                                                                                                                                                                                <br>
 * Codigo de errores (algunos pueden no devolverse debido a cambios en el funcionamiento de los metodos)                                                                                          <br>
 *  INFO_FICHERO_YA_EXISTE                                El fichero ya existe. No se permite sobrescribir un fichero con una version mas nueva.                                                  <br>
 *  INFO_FICHERO_NO_ENCONTRADO                            Fichero no encontrado. Error de lectura o escritura. Error cerrando el fichero.                                                         <br>
 *  INFO_DIRECTORIO_NO_EXISTE                             El directorio enviado no existe, no es un directorio.                                                                                   <br>
 *  INFO_FICHERO_EN_USO                                   En algunos casos como en la sincronizacion no se realiza una espera si el fichero esta en uso.                                          <br>
 *                                                                                                                                                                                                <br>
 *  INFO_ID_NO_EXISTE                                     El id enviado no existe o no existe como lectura si se solicita una lectura y viceversa.                                                <br>
 *  INFO_LIMITE_MAX_IDS_POR_CREAR                         Se ha superado el numero maximo de ids que se pueden crear.                                 Podría esperar durmiendo.                   <br>
 *  INFO_LIMITE_MAX_LECTORES_POR_ID                       Se ha superado el limite de lectores por ids.                                               Podría esperar durmiendo.                   <br>
 *  INFO_LIMITE_MAX_LECTORES                              Se ha superado el limite total de lectores que pueden acceder al servidor simultaneamente.  Podría esperar durmiendo.                   <br>
 *                                                                                                                                                                                                <br>
 *  INFO_TAMANO_PAQUETE_ARRAY                             En el envio de un paquete, el array de bytes no tiene el tamaño correcto, debe ser el que se establecio en la solicitud de escritrua.   <br>
 *  INFO_PAQUETE_FUERA_DE_RANGO                           El numero del paquete enviado esta fuera de rango.                                                                                      <br>
 *                                                                                                                                                                                                <br>
 *  INFO_ACCESO_NO_PERMITIDO                              Hay momentos en los que no se permite crear sesiones para leer o escribir como cuando se quiere apagar de forma controlada el servidor. <br>
 *  INFO_PASSWORD_INVALIDA                                Para algunos metodos hay que proporcionar una contraseña.                                                                               <br>
 *  INFO_SINCRONIZACION_SIN_PERMISO                       Se debe solicitar permiso y no debe caducar para poder realizar el proceso de sincronizacion entre servidores.                          <br>
 *                                                                                                                                                                                                <br>
 *  WARNING_DURANTE_LA_ESPERA                             Mientras se esperaba a que quedara libre el fichero se produjo un error.                                                                <br>
 *                                                                                                                                                                                                <br>
 *  ERROR_INTERNO_NUM_PAQUETE_LISTA                       Error interno. No concuerda el numero del paquete con la posicion en la lista. (YA NO SE USA)                                           <br>
 *  ERROR_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO  Existe un fichero en el disco duro del servidor pero en las variables del control esta marcado como borrado.                            <br>
 */
public class AlmacenObjetoRemote extends UnicastRemoteObject implements AlmacenInterfaceRemote, Runnable{

// CONSTANTES ---------------------------------------------------------------
    private final String FICHERO_PROPERTIES_ID_INSTANCIA                         = "id_para_la_instancia";
    private final String FICHERO_PROPERTIES_DIRECTORIO_BASE                      = "directorio_base";
    
    private final String FICHERO_PROPERTIES_NUMERO_MAXIMO_LECTORES_TOTALES       = "num_lectores_totales";                       //No se calcula multiplicando los dos siguientes porque podría querese que fuera menor.     
    private final String FICHERO_PROPERTIES_NUMERO_MAXIMO_LECTORES_POR_ID        = "num_lectores_por_id";
    private final String FICHERO_PROPERTIES_NUMERO_MAXIMO_DE_IDS_PARA_LECTORES   = "num_de_ids_para_lectores";
    private final String FICHERO_PROPERTIES_TIEMPO_MAX_ABS_LECTURA               = "tiempo_max_abs_lectura";
    private final String FICHERO_PROPERTIES_TIEMPO_MAX_REL_LECTURA               = "tiempo_max_rel_lectura";
    
    private final String FICHERO_PROPERTIES_NUMERO_MAXIMO_DE_IDS_PARA_ESCRITORES = "num_de_ids_para_escritores";
    private final String FICHERO_PROPERTIES_TIEMPO_MAX_ABS_ESCRITURA             = "tiempo_max_abs_escritura";
    private final String FICHERO_PROPERTIES_TIEMPO_MAX_REL_ESCRITURA             = "tiempo_max_rel_escritura";
   
    private final String FICHERO_PROPERTIES_TIEMPO_DORMIR_CADUCAR_SESIONES       = "tiempo_dormir_caducar_sesiones";
    private final String FICHERO_PROPERTIES_TIEMPO_DORMIR_COMPROBAR_CONEXIONES   = "tiempo_dormir_comprobar_conexiones";
    private final String FICHERO_PROPERTIES_TIEMPO_DORMIR_SINCRONIZAR_FICHEROS   = "tiempo_dormir_sincronizar_ficheros";
    private final String FICHERO_PROPERTIES_EJECUTAR_AUTO_CADUCAR_SESIONES       = "ejecutar_automatico_caducar_sesiones";
    private final String FICHERO_PROPERTIES_EJECUTAR_AUTO_COMPROBAR_CONEXIONES   = "ejecutar_automatico_comprobar_conexiones";
    private final String FICHERO_PROPERTIES_EJECUTAR_AUTO_SINCRONIZAR_FICHEROS   = "ejecutar_automatico_sincronizar_ficheros";
    private final String FICHERO_PROPERTIES_TIEMPO_DORMIR_ESPERAR_FINALIZAR      = "tiempo_dormir_esperar_finalizar";            //Cuando se activa apagar hay que esperar a que terminen las tareas y se hace con un bucle que compruba si han terminado y va a dormir el tiempo que aqui de indique.
    private final String FICHERO_PROPERTIES_TIEMPO_DORMIR_ESPERAR_INTERRUMPIR    = "tiempo_dormir_esperar_interrumpir";          //Existe una minima posibilidad de que trabajando==false && durmiendo==true sin estar durmiendo. Durmiendo se pone a true justo antes de ir a dormir. Con una espera de 1 segundo se entiende que esa posibilidad desaparece casi por completo.
    private final String FICHERO_PROPERTIES_TIEMPO_DORMIR_ESPERAR_JOIN           = "tiempo_dormir_esperar_join";                 //Se espera un tiempo a que el proceso termine por si mismo

    private final String FICHERO_PROPERTIES_NUMERO_MAXIMO_COMUNICACION_ACTIVA    = "num_max_comunicacion_activa";
    private final String FICHERO_PROPERTIES_NUMERO_MAXIMO_COMUNICACION_PASIVA    = "num_max_comunicacion_pasiva";

    private final String FICHERO_PROPERTIES_SINCRONIZACION_TAMANO_PAQUETE_ENVIAR = "tamano_paquete_enviar_sincronizacion";
    private final String FICHERO_PROPERTIES_SINCRONIZACION_TAMANO_PAQUETE_TRAER  = "tamano_paquete_traer_sincronizacion";
    private final String FICHERO_PROPERTIES_SINCRONIZACION_TIEMPO_MAX            = "tiempo_max_abs_sincronizacion";
    
    private final String FICHERO_PROPERTIES_ADMIN_PASSWORD_LEECTURA              = "password_lectura";
    private final String FICHERO_PROPERTIES_ADMIN_PASSWORD_ESCRITURA             = "password_escritura";
    private final String FICHERO_PROPERTIES_ADMIN_PASSWORD_PROCESOS              = "password_procesos";
    private final String FICHERO_PROPERTIES_ADMIN_PASSWORD_ADMINISTRACION        = "password_administracion";
    private final String FICHERO_PROPERTIES_ADMIN_ACCESO_PERMITIDO_LECTURA       = "acceso_permitido_lectura";
    private final String FICHERO_PROPERTIES_ADMIN_ACCESO_PERMITIDO_ESCRITURA     = "acceso_permitido_escritura";
    
    private final String FICHERO_PROPERTIES_LOG_NIVEL                            = "log_nivel";
    
    private final String FICHERO_PROP_RMIREGISTRY_CADENA_TEST       = "ping_test";
    private final String FICHERO_PROP_RMIREGISTRY_NUM_DE_SERVIDORES = "num_de_servidores";
    private final String FICHERO_PROP_RMIREGISTRY_IP                = "ip_rmiregistry";
    private final String FICHERO_PROP_RMIREGISTRY_NOMBRE_SERVICIO   = "nombre_servicio_en_rmiregistry";
    
    private final String NOMBRE_FICHERO_VERSIONES = "___versiones__de__los__ficheros___.txt";                                    //El nombre del fichero donde se almacenara la informacion asociada a la version que tiene cada fichero alojado en el servidor. Con caracteres de mas para que no coincida con uno que un usuario guarde pues se almacena en el mismo directorio.
    
    private final boolean[] MOSTAR_SERVIDOR = {true, true, false, false};
    private final boolean[] MOSTAR_FECHA    = {true, true, false, true };
    

// VARIABLES ----------------------------------------------------------------
    private String                                           id                                    =    "";  //Un identificador para la instancia de Almacen
    private String                                           directorio_base                       =    "";  //La ruta del disco dentro de la que se guardaran los ficheros.
    private String                                           rmiregistry_ip                        =    "";  //La IP del servidor rmiregistry donde se registro el objeto remoto. Necesario para desregistrar al apagar.
    private String                                           rmiregistry_nombre_servicio           =    "";  //El nombre del servicio con el que fue registrado el objeto remoto en el servidor rmiregistry. Necesario para desregistrar al apagar.
    private int                                              log_nivel                             =     3;  //Para indicar si se deben mostrar mensajes por pantalla segun el valor. 0 no muestra. 1 solo errores, 2 errores y avisos importantes, 3 muestra todo.
    
    //Grupo w: Administracion, gestion de accesos.
    private String                                           w_password_lectura                    =    "";  //Los metodos de lectura    necesitan recibir una contraseña para permitir el acceso.
    private String                                           w_password_escritura                  =    "";  //Los metodos de escritura  necesitan recibir una contraseña para permitir el acceso.
    private String                                           w_password_procesos                   =    "";  //Los metodos de run y control de procesos. Una contraseña independiente porque ni es leer ni escribir y puede ser requerida por un cliente por lo que no se le puede dar la de administracion.
    private String                                           w_password_administracion             =    "";  //Cierto metodos especiales necesitan recibir una contraseña para permitir el acceso. Debe ser interna de los servidores y conocida por los administradores de los servidores, no se le debe dar a los clientes.
    private boolean                                          w_acceso_permitido_lectura            = false;  //A false no permite la ejecucion de ningun metodo de lectura o escritura. Se usa para dejar que las sesiones existentes terminen sin que se creen nuevas y así poder apagar de forma controlada.
    private boolean                                          w_acceso_permitido_escritura          = false;  //A false no permite la ejecucion de ningun metodo de lectura o escritura. Se usa para dejar que las sesiones existentes terminen sin que se creen nuevas y así poder apagar de forma controlada.
    private boolean                                          w_acceso_permitido_conexion_pasiva    =  true;  //Se inicializa aqui porque no se hace con el fichero de configuracion. Para apagar controladamente. Se pone a false para no aceptar a nadie más. 
    private boolean                                          w_acceso_permitido_sincronizar        =  true;  //Se inicializa aqui porque no se hace con el fichero de configuracion. Para apagar controladamente. Se pone a false para no aceptar a nadie más. 
    

    //Grupo a: Gestionar el acceso tanto de lectura como escrituras. Puerta de entrada. Aunque ahora creo los Controladores se podrian poner en sus grupos, de hecho, sólo parece que lo necesiten los escritores...
    private Map<String, Integer>                             a_lector_o_escritor                   = null;   //-1 indica que es escritor. Positivo indica cuantos lectores hay para ese id.
    private Map<String, Semaphore>                           a_hoteles_dormir                      = null;   //Para dejar durmiendo a los hilos que intentan leer o escribir y no pueden porque el fichero que solicitan esta siendo usado. Se trata de un Semaphore porque si fuera wait-notify en ciertos caso podria producirse primero el notify y luego el wait lo que dejaria  bloqueado para siempre al que hizo el wait. Lo del nombre de hotel es para dar a entender que su funcion es la de cola de espera.
    private Map<String, Integer>                             a_cantidad_durmientes                 = null;   //Pueden llegar muchas peticiones a solicitarXxx. Cada peticion es un hilo que se ira a dormir menos el primero. Entonces, el primero llamara a EnvioFinalizado y debe despertarlos a todos, deben realizarse tantos release() como acquire se hicieran pues el hotel es sacado del Map y ya nadie podra realizar mas release sobre el. 
//  private Map<String, String>                              a_ids_para_lector_escritor            = null;   //La key en el grupo a no debe ser la misma que en los grupo b y c. El valor de este map es la key de los grupo b y c. De esta forma y si no se reptien los ids de los grupo b y c, una vez que se borra del grupo a es como si se borrase del grupo b y c tambien al quedar inaccesible. Sin este control, volver a crear un id igual para un mismo fichero permite acceder al grupo b y c. Es decir un hilo borra el grupo a y luego quiere borrar el grupo b. Otro hilo llega despues al grupo a y ve que ya no hay así que lo crea. Se adelante y sobrescribe lo del grupo b. Luego, el otro hilo que aun no había borrado el grupo b borra el grupo b y lo que hace es borrar lo que el otro acaba de crear. Uno pisa el trabajo del otro. 
            
    //Grupo b: Para gestionar las lecturas
    private int                                              b_numero_maximo_lectores_totales      =    0;   //Cte. Para establecer un numero maximo de lectores totales. No se calcula multiplicando los dos siguientes porque podría quererse que fuera menor.     
    private int                                              b_numero_maximo_lectores_por_id       =    0;   //Cte. Para establecer un numero maximo de lectores por fichero.
    private int                                              b_numero_maximo_ids_para_leer         =    0;   //Cte. Para establecer un numero maximo de identificadores a crear. No se permitira que los Maps de lectura tengan mas elementos que los indicados por esta variable.
    private int                                              b_lectores_actuales_totales           =    0;   //Cada vez que entra un nuevo lector esta variable se incrementa independientemente del id que solicite.  
    private Map<String, Integer>                             b_lectores_por_id                     = null;   //Cada vez que entra un nuevo lector esta variable se incrementa para un unico id.
    private Map<String, List>                                b_lecturas                            = null;   //List<FicheroPaquete> para tener toda la informacion del fichero en memoria. Se supone que manejara ficheros pequeños
    private Map<String, Integer>                             b_tamano_paquetes                     = null;   //Se permite que cada lectura tenga un tamaño diferente. Puede ser informacion redundante pues en FicheroPaquete tambien se coloca esta informacion. Pero acceder a FicheroPaquete requiere más bloqueos que acceder a este valor.
    private Map<String, Integer>                             b_cantidad_paquetes                   = null;   //Para saber cuanto paquetes corresponde a cada fichero.
    private long                                             b_tiempo_max_abs_para_leer            =    0;   //Cte. Establece el tiempo maximo absoluto para leer un fichero. Se usara este valor si 'b_tiempo_max_rel_para_leer' es cero, si 'b_tiempo_max_rel_para_leer' no es cero se usara 'b_tiempo_max_rel_para_leer'
    private float                                            b_tiempo_max_rel_para_leer            =    0;   //Cte. Establece el tiempo maximo relativo para leer un fichero. Este numero se multiplica por el numero de paquetes y el tamaño del paquete. Ej: 0.1ms * 1000 paquetes * 256 bytes el paquete = 25600 milisegundos para leer el fichero de 256 kb. 
    private Map<String, Date>                                b_hora_de_inicio_lectura              = null;   //Servira para finalizar sesiones que se hayan pasado de un cierto tiempo. Se actualizara cada vez que entre un nuevo lector.
    
    //Grupo c: Para gestionar las escrituras.
    private int                                              c_numero_maximo_ids_para_escribir     =     0;  //Cte. Para establecer un numero maximo de identificadores a crear. No se permitira que los Maps de escritura tengan mas elementos que los indicados por esta variable.
    private Map<String, List>                                c_escrituras                          =  null;  //List<FicheroPaquete> para tener toda la informacion del fichero en memoria. Se supone que manejara ficheros peque�os
    private Map<String, ControladorDeRecursosLectorEscritor> c_controlador_de_accesos              =  null;  //Gestiona las lecturas-escrituras simultaneas a cada fichero. Diría que solo hace falta para las escrituras, en las lecturas se lee el fichero a la vez y ya no se modifica, no necesita ningún control. Se debe quitar del grupo a_ y colocarlo en el grupo b        
    private Map<String, FicheroInfo>                         c_fichero_info                        =  null;  //Tiene el directorio relativo, el nombre del fichero, la version y si esta marcado como borrado. Tiene todo su significado en el grupo_z, aqui solo es para mantener un seguimiento antes de colocarlo en el grupo_z al finalizar la escritura. Para la lectura se coge del grupo_z.    
    private Map<String, Integer>                             c_tamano_paquetes                     =  null;  //Se permite que cada escritura tenga un tamaño diferente. Puede ser informacion redundante pues en FicheroPaquete tambien se coloca esta informacion. Pero mientras no se haya enviado el primer paquete es la unica forma de tener esta informacion.
    private Map<String, Integer>                             c_cantidad_paquetes                   =  null;  //Para saber cuanto paquetes corresponde a cada fichero.
    private long                                             c_tiempo_max_abs_para_escribir        =     0;  //Cte. Establece el tiempo maximo absoluto para leer un fichero. Se usara este valor si 'c_tiempo_max_rel_para_escribir' es cero, si 'c_tiempo_max_rel_para_escribir' no es cero se usara 'c_tiempo_max_rel_para_escribir'
    private float                                            c_tiempo_max_rel_para_escribir        =     0;  //Cte. Establece el tiempo maximo relativo para leer un fichero. Este numero se multiplica por el numero de paquetes y el tamaño del paquete. Ej: 0.1ms * 1000 paquetes * 256 bytes el paquete = 25600 milisegundos para leer el fichero de 256 kb. 
    private Map<String, Date>                                c_hora_de_inicio_escritura            =  null;  //Servira para finalizar sesiones que se hayan pasado de un cierto tiempo. Se actualizara cada vez que entre un nuevo escritor.
    private int                                              c_borrando                            =     0;  //Para saber cuantos hilos hay borrando. Es para esperar a que terminen cuando se va a apagar.

    //Grupo x: estas variables controlan el hilo run()
    private boolean                                          x_fin                                 = false;  //A true indica que hay que finalizar la ejecucion del hilo
    private boolean                                          x_pausa                               = false;  //A true indica que hay que parar la ejecucion del hilo momentaneamente
    private int                                              x_tiempo_dormir_caducar_sesiones      =     0;  //Cada proceso tiene su tiempo de ejecucion.
    private int                                              x_tiempo_dormir_comprobar_conexiones  =     0;  //Cada proceso tiene su tiempo de ejecucion.
    private int                                              x_tiempo_dormir_sincronizar_ficheros  =     0;  //Cada proceso tiene su tiempo de ejecucion.
    private int                                              x_tiempo_dormir_esperar_finalizar     =     0;  //Se debe comprobar cada cierto tiempo el valor de ciertas variables para saber si han terminado las operaciones de lectura y escritura
    private int                                              x_tiempo_dormir_esperar_interrumpir   =     0;  //
    private int                                              x_tiempo_dormir_esperar_join          =     0;  //
    private boolean                                          x_ejecutar_caducar_sesiones           = false;  //Para saber si se debe ejecutar la tarea en el run()
    private boolean                                          x_ejecutar_comprobar_conexiones       = false;  //Para saber si se debe ejecutar la tarea en el run()
    private boolean                                          x_ejecutar_sincronizar_ficheros       = false;  //Para saber si se debe ejecutar la tarea en el run()
    private int                                              x_proceso_a_ejecutar_cont             =     0;  //Como a run() no se le puede pasar parametros, esta variable le indica que proceso debe ejecutar. Se la copiara a una local.
    private int                                              x_proceso_a_ejecutar_max              =     0;  //Como a run() no se le puede pasar parametros, esta variable le indica cuantos procesos debe crear. Es el caso base de la recursividad en la creacion de hilos.
    private boolean[]                                        x_procesos_informar_finalizados       =  null;  //Cuando se de la orde de finalizar cada run debe informar que ha salido y que termina su ejecución para asi realizar un apagado controlado.
    private boolean[]                                        x_procesos_durmiendo                  =  null;  //Para indicar que estan durmiendo. 
    private boolean[]                                        x_procesos_trabajando                 =  null;  //Para indicar que estan trabajando. 
    private Map<Integer, Thread>                             x_procesos_run                        =  null;  //Una manera de tener localizados a los hilos que ejecutan las tareas de mantenimiento. Quien crea el hilo debe introducirlos en este array.
    
    //Grupo y: Para gestionar la red de almacenes distribuidos.
    private Map<String, AlmacenInterfaceRemote>              y_almacenes_comunicacion_activa       =  null;  //Los almacenes con los que se comunica activamente, le pregunta por sus ficheros, lee los que no tenga y le envia los que el otro almacen no tenga.
    private Map<String, AlmacenInterfaceRemote>              y_almacenes_comunicacion_pasiva       =  null;  //Los almacenes que se comunican con este, los que le piden la lista de los ficheros. Si uno está en activo no puede estar en pasivo para evitar comunicaciones redundantes.
    private Map<String, AlmacenInterfaceRemote>              y_almacenes_orden_0                   =  null;  //Los almacenes de orden cero son {activos U pasivos}.
    private Map<String, AlmacenInterfaceRemote>              y_almacenes_orden_1                   =  null;  //Nuestros almacenes de orden uno son los de orden cero de los almacenes que hay en nuestro conjunto de orden cero.
    private String                                           y_ruta_fichero_distribuida            =    "";  //Cte. Establece la ruta donde esta el fichero con la informacion para contactar con otros servidores. 
    private int                                              y_num_max_comunicaciones_activas      =     0;  //Cte. Para establecer el numero maximo de almacenes con los que se puede establecer una comunicacion activa. 1 parece un numero aceptable con 2 para pasivo y crear topología de arbol binario.
    private int                                              y_num_max_comunicaciones_pasivas      =     0;  //Cte. Para establecer el numero maximo de almacenes a los que se le permiten pedir la lista de ficheros y así realicen peticiones de lectura y escritura sobre este almacen. 2 parece un numero aceptable para pasivo y 1 para activo y crear topología en arbol binario.
    private int                                              y_num_comunicaciones_activas          =     0;  //Para saber cuantas conexiones activas hay establecidas
    private int                                              y_num_comunicaciones_pasivas          =     0;  //Para saber cuantas conexiones pasivas hay establecidas

    //Grupo z: Relacionadas con la sincronizacion de ficheros.
    private Map<String, FicheroInfo>                         z_ficheros_del_almacen                =  null;  //Tiene el directorio relativo, el nombre del fichero, la version y si esta marcado como borrado. Sirve para sincronizar los almacenes.
    private String                                           z_id_almacen_pasivo                   =    "";  //Un control de acceso similar al de los grupos a,b,c pero más simple. Solo se aceptara que un unico servidor se sincronice con nosotros y debe ser de la lista de pasivos
    private boolean                                          z_sincronizacion_aceptada             = false;  //Somos nosotros quienes aceptamos.      Para que el otro servidor inicie la sincronizacion con nosotros debemos aceptarlo. Con esta variable a true no podemos iniciar le proceso de sincronización.
    private boolean                                          z_sincronizacion_rechazada            = false;  //Es el otro servidor quien nos rechaza. Para iniciar la tarea de sincronizacion debemos se aceptados, durante la tarea se puede quitar el permiso. Si esta variabla se pone a true debemos dejar de trabajar con el servidor y pasar al siguiente.
    private int                                              z_tamano_paquete_enviar               =     0;  //En la sincronizacion se deben enviar ficheros a  otros servidores lo que requiere un tamaño de paquete.
    private int                                              z_tamano_paquete_traer                =     0;  //En la sincronizacion se deben traer  ficheros de otros servidores lo que requiere un tamaño de paquete.    
    private long                                             z_tiempo_max_abs_para_sincronizar     =     0;  //Establece el tiempo maximo para sincronizarse. Pasado este tiempo se quitara el permiso de sincronizacion. Importante si el otro servidor se cae.
    private Date                                             z_hora_de_inicio_sincronizar          =  null;  //Servira para finalizar la sesion cuando haya superado el maximo permitido.
    
    //Para el acceso sincronizado a las variables
    private ControladorDeRecursosLectorEscritor lock_grupo_a = new ControladorDeRecursosLectorEscritor(""); //Así se permitira accesos simultaneos a las variables del grupo a cuando sean operaciones de lectura.
    private ControladorDeRecursosLectorEscritor lock_grupo_b = new ControladorDeRecursosLectorEscritor("");
    private ControladorDeRecursosLectorEscritor lock_grupo_c = new ControladorDeRecursosLectorEscritor("");
    private ControladorDeRecursosLectorEscritor lock_grupo_w = new ControladorDeRecursosLectorEscritor("");
    private ControladorDeRecursosLectorEscritor lock_grupo_x = new ControladorDeRecursosLectorEscritor("");
    private ControladorDeRecursosLectorEscritor lock_grupo_y = new ControladorDeRecursosLectorEscritor("");
    private ControladorDeRecursosLectorEscritor lock_grupo_z = new ControladorDeRecursosLectorEscritor("");

    
// CONSTRUCTORES ---------------------------------------------------------------

    /**
     * Crea una instancia de un AlmacenObjeto para guardar y leer fichero en un servidor.
     * 
     * @param  ruta_fich_propiedades        La ruta de un directorio y fichero para leer las propiedades sin tener que pasarlas por parametro
     * @param  ruta_fich_distribuida        La ruta de un directorio y fichero para leer las ips y nombre de servicio de otros almacenes con los que contactar y crear una red de servidores.
     * @param  rmi_registry_ip              La IP donde esta el servidor rmiregistry donde se va a registrar este objeto que ahora se esta creando (this). Necesario para apagar.
     * @param  rmi_registry_nombre_servicio El nombre del servicio con el que se va a registrar en rmiregistry este objeto que ahora se esta creando (this). Necesario para apagar.
     * @throws RemoteException              Al ser un objeto remoto se podría producir esta excepcion al intentar crear el objeto si hubiera algún problema en la red.
     * @throws IOException                  Si se produce algún problema accediendo al fichero.
     */
    private AlmacenObjetoRemote(String ruta_fich_propiedades, String ruta_fich_distribuida, String rmi_registry_ip, String rmi_registry_nombre_servicio) throws RemoteException, IOException, NumberFormatException{
    /*SUPER*/
        super();
    /*VAR*/
        Properties                   prop_01                        =  null;    //Para el fichero de propiedades.
        Properties                   prop_02                        =  null;    //Para el fichero de las versiones.
        List<AlmacenInterfaceRemote> lista_almacenes_para_contactar =  null;
        List<String>                 lista_conexiones_realizadas    =  null;
        int                          num_conexiones_realizadas      =     0;
        int                          i                              =     0;
    /*BEGIN*/
        prop_01 = new Properties();
        prop_02 = new Properties();
        prop_01.load( new FileReader(ruta_fich_propiedades) ); 

        //Generales
        this.id                                   =                   prop_01.getProperty(FICHERO_PROPERTIES_ID_INSTANCIA   )  ;
        this.directorio_base                      =                   prop_01.getProperty(FICHERO_PROPERTIES_DIRECTORIO_BASE)  ;
        this.log_nivel                            = Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_LOG_NIVEL      ) );
        this.rmiregistry_ip                       = rmi_registry_ip;
        this.rmiregistry_nombre_servicio          = rmi_registry_nombre_servicio;
        

        //Grupo w - Control de accesos, password
        this.w_password_lectura                   =                    prop_01.getProperty(FICHERO_PROPERTIES_ADMIN_PASSWORD_LEECTURA         );
        this.w_password_escritura                 =                    prop_01.getProperty(FICHERO_PROPERTIES_ADMIN_PASSWORD_ESCRITURA        );
        this.w_password_procesos                  =                    prop_01.getProperty(FICHERO_PROPERTIES_ADMIN_PASSWORD_PROCESOS         );
        this.w_password_administracion            =                    prop_01.getProperty(FICHERO_PROPERTIES_ADMIN_PASSWORD_ADMINISTRACION   );
        this.w_acceso_permitido_lectura           = (Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_ADMIN_ACCESO_PERMITIDO_LECTURA  ) ) ==  1);
        this.w_acceso_permitido_escritura         = (Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_ADMIN_ACCESO_PERMITIDO_ESCRITURA) ) ==  1);
        
        //Grupo a - Lectura y escritura
        this.a_lector_o_escritor                  = new HashMap<String, Integer>();
        this.a_hoteles_dormir                     = new HashMap<String, Semaphore>();
        this.a_cantidad_durmientes                = new HashMap<String, Integer>();

        //Grupo b - Lectura
        this.b_numero_maximo_lectores_totales     = Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_NUMERO_MAXIMO_LECTORES_TOTALES    ) );
        this.b_numero_maximo_lectores_por_id      = Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_NUMERO_MAXIMO_LECTORES_POR_ID     ) );
        this.b_numero_maximo_ids_para_leer        = Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_NUMERO_MAXIMO_DE_IDS_PARA_LECTORES) );
        this.b_tiempo_max_abs_para_leer           = Long.parseLong  ( prop_01.getProperty(FICHERO_PROPERTIES_TIEMPO_MAX_ABS_LECTURA            ) );
        this.b_tiempo_max_rel_para_leer           = Float.parseFloat( prop_01.getProperty(FICHERO_PROPERTIES_TIEMPO_MAX_REL_LECTURA            ) );
        this.b_lectores_actuales_totales          = 0;
        this.b_lectores_por_id                    = new HashMap<String, Integer>();
        this.b_lecturas                           = new HashMap<String, List   >();
        this.b_tamano_paquetes                    = new HashMap<String, Integer>();
        this.b_cantidad_paquetes                  = new HashMap<String, Integer>(); 
        this.b_hora_de_inicio_lectura             = new HashMap<String, Date   >();

        //Grupo c - Escritura
        this.c_numero_maximo_ids_para_escribir    = Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_NUMERO_MAXIMO_DE_IDS_PARA_ESCRITORES) );
        this.c_tiempo_max_abs_para_escribir       = Long.parseLong  ( prop_01.getProperty(FICHERO_PROPERTIES_TIEMPO_MAX_ABS_ESCRITURA            ) );
        this.c_tiempo_max_rel_para_escribir       = Float.parseFloat( prop_01.getProperty(FICHERO_PROPERTIES_TIEMPO_MAX_REL_ESCRITURA            ) );
        this.c_escrituras                         = new HashMap<String, List   >                            ();
        this.c_controlador_de_accesos             = new HashMap<String, ControladorDeRecursosLectorEscritor>();
        this.c_fichero_info                       = new HashMap<String, FicheroInfo                        >();
        this.c_tamano_paquetes                    = new HashMap<String, Integer>                            ();
        this.c_cantidad_paquetes                  = new HashMap<String, Integer>                            ();
        this.c_hora_de_inicio_escritura           = new HashMap<String, Date   >                            ();

        //Grupo x - Para la ejecucion del run().
        this.x_ejecutar_caducar_sesiones          = (Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_EJECUTAR_AUTO_CADUCAR_SESIONES    )   )  ==  1);
        this.x_ejecutar_comprobar_conexiones      = (Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_EJECUTAR_AUTO_COMPROBAR_CONEXIONES)   )  ==  1);
        this.x_ejecutar_sincronizar_ficheros      = (Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_EJECUTAR_AUTO_SINCRONIZAR_FICHEROS)   )  ==  1);
        this.x_tiempo_dormir_caducar_sesiones     =  Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_TIEMPO_DORMIR_CADUCAR_SESIONES    )   );
        this.x_tiempo_dormir_comprobar_conexiones =  Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_TIEMPO_DORMIR_COMPROBAR_CONEXIONES)   );
        this.x_tiempo_dormir_sincronizar_ficheros =  Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_TIEMPO_DORMIR_SINCRONIZAR_FICHEROS)   );
        
        //Grupo x - Relacionado con el inicio y parada de los hilos run, para que pueda crear un hilo para cada proceso y luego informar de que han terminado.
        this.x_tiempo_dormir_esperar_finalizar    =  Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_TIEMPO_DORMIR_ESPERAR_FINALIZAR   )   );
        this.x_tiempo_dormir_esperar_interrumpir  =  Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_TIEMPO_DORMIR_ESPERAR_INTERRUMPIR )   );
        this.x_tiempo_dormir_esperar_join         =  Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_TIEMPO_DORMIR_ESPERAR_JOIN        )   );
        this.x_proceso_a_ejecutar_cont            = 0;                                             //Cada hilo creara uno nuevo, es recursivo.
        this.x_proceso_a_ejecutar_max             = 3;                                             //Indica cuantos hilos crear. Se establece aqui porque si hay que cambiarlo tambien hay que cambiar las lineas anteriores.
        this.x_procesos_informar_finalizados      = new boolean[this.x_proceso_a_ejecutar_max];    //Si importa el valor inicial.
        this.x_procesos_durmiendo                 = new boolean[this.x_proceso_a_ejecutar_max];    //No importa el valor inicial.
        this.x_procesos_trabajando                = new boolean[this.x_proceso_a_ejecutar_max];    //No importa el valor inicial.
        this.x_procesos_run                       = new HashMap<Integer, Thread>();
        for (i=0; i<x_proceso_a_ejecutar_max; i++){
            this.x_procesos_informar_finalizados[i] = false;
        }/*for*/
        
        //Grupo y - Almacenes con los que conectarse.
        this.y_almacenes_comunicacion_activa      = new HashMap<String, AlmacenInterfaceRemote>();
        this.y_almacenes_comunicacion_pasiva      = new HashMap<String, AlmacenInterfaceRemote>();
        this.y_almacenes_orden_0                  = new HashMap<String, AlmacenInterfaceRemote>();
        this.y_almacenes_orden_1                  = new HashMap<String, AlmacenInterfaceRemote>();
        this.y_num_max_comunicaciones_activas     = Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_NUMERO_MAXIMO_COMUNICACION_ACTIVA) );
        this.y_num_max_comunicaciones_pasivas     = Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_NUMERO_MAXIMO_COMUNICACION_PASIVA) );
        this.y_ruta_fichero_distribuida           = ruta_fich_distribuida;
        if ( !ruta_fich_distribuida.equals("") ){
            System.out.println("-----");
            System.out.println("Buscando servidores con los que conectar...");
            lista_almacenes_para_contactar = this.listaAlmacenesParaContactar(ruta_fich_distribuida);
            lista_conexiones_realizadas    = this.conectarConServidoresA(this.w_password_administracion, lista_almacenes_para_contactar);
            num_conexiones_realizadas      = lista_conexiones_realizadas.size();
            System.out.println("");
        }/*if*/
        
        //Grupo z - Para la sincronizacion
//      this.z_ficheros_del_almacen              = new HashMap<String, FicheroInfo>();        
        if ( FicherosTrabajar.exiteFichero( this.directorio_base, NOMBRE_FICHERO_VERSIONES) ){
            prop_02.load( new FileReader(this.directorio_base + NOMBRE_FICHERO_VERSIONES) ); 
        }/*if*/
        this.z_ficheros_del_almacen              = this.ficherosEnDirectorioB(prop_02, this.directorio_base, "");                                               //CUIDADO: La version que carga es la fecha a la que se grabo el fichero. No es la version que mando el usuairo que escribio el fichero.
        this.z_tamano_paquete_enviar             = Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_SINCRONIZACION_TAMANO_PAQUETE_ENVIAR)  );
        this.z_tamano_paquete_traer              = Integer.parseInt( prop_01.getProperty(FICHERO_PROPERTIES_SINCRONIZACION_TAMANO_PAQUETE_TRAER)   );
        this.z_tiempo_max_abs_para_sincronizar   = Long.parseLong  ( prop_01.getProperty(FICHERO_PROPERTIES_SINCRONIZACION_TIEMPO_MAX)             );
        
        //Muestra por pantalla un resumen de las propiedades cargadas
        System.out.println("----- RESUMEN "                                                                                    );
        System.out.println("Id del servicio:                                     " + this.id                                   );
        System.out.println("Directorio_base:                                     " + this.directorio_base                      );
        System.out.println("Nivel de log:                                        " + this.log_nivel                            );
        System.out.println("IP de RmiRegistry:                                   " + this.rmiregistry_ip                       );
        System.out.println("Nombre del servicio en RmiRegistry:                  " + this.rmiregistry_nombre_servicio          );
        System.out.println("-----"                                                                                             );
        System.out.println("Password Lectura:                                    " + this.w_password_lectura                   );
        System.out.println("Password Escritura:                                  " + this.w_password_escritura                 );
        System.out.println("Password Procesos:                                   " + this.w_password_procesos                  );
        System.out.println("Password Administracion:                             " + this.w_password_administracion            );
        System.out.println("Acceso permitido lectura:                            " + this.w_acceso_permitido_lectura           );
        System.out.println("Acceso permitido escritura:                          " + this.w_acceso_permitido_escritura         );
        System.out.println("-----"                                                                                             );
        System.out.println("Numero maximo de lectores leyendo a la vez:          " + this.b_numero_maximo_lectores_totales     );
        System.out.println("Numero maximo de lectores por fichero:               " + this.b_numero_maximo_lectores_por_id      );
        System.out.println("Numero maximo de ficheros abiertos para leer:        " + this.b_numero_maximo_ids_para_leer        );
        System.out.println("Tiempo maximo absoluto para leer un fichero:         " + this.b_tiempo_max_abs_para_leer           );
        System.out.println("Tiempo maximo relativo para leer un fichero:         " + this.b_tiempo_max_rel_para_leer           );
        System.out.println("-----"                                                                                             );        
        System.out.println("Numero maximo de ficheros abiertos para escribir:    " + this.c_numero_maximo_ids_para_escribir    );
        System.out.println("Tiempo maximo absoluto para escribir un fichero:     " + this.c_tiempo_max_abs_para_escribir       );
        System.out.println("Tiempo maximo relativo para escribir un fichero:     " + this.c_tiempo_max_rel_para_escribir       );
        System.out.println("-----"                                                                                             );
        System.out.println("Tiempo entre iteraciones para caducar sesiones:      " + this.x_tiempo_dormir_caducar_sesiones     );
        System.out.println("Tiempo entre iteraciones para comprobar conexiones:  " + this.x_tiempo_dormir_comprobar_conexiones );
        System.out.println("Tiempo entre iteraciones para sincronizar ficheros:  " + this.x_tiempo_dormir_sincronizar_ficheros );
        System.out.println("-----"                                                                                             );        
        System.out.println("Numero maximo de comunicaciones activas:             " + this.y_num_max_comunicaciones_activas     );
        System.out.println("Numero maximo de comunicaciones pasivas:             " + this.y_num_max_comunicaciones_pasivas     );
        System.out.println("-----"                                                                                             );
        System.out.println("Numero de conexiones activas realizadas:             " + num_conexiones_realizadas                 );
        for (i=0; i<num_conexiones_realizadas; i++){
            System.out.println("    ID del servidor :                                " + lista_conexiones_realizadas.get(i)        );
        }/*for*/
        System.out.println("-----"                                                                                             );
        System.out.println("Sincronizacion. Tamano paquete a enviar:             " + this.z_tamano_paquete_enviar              );
        System.out.println("Sincronizacion. Tamano paquete a traer:              " + this.z_tamano_paquete_traer               );
        System.out.println("Sincronizacion. Tiempo para sincronizarse:           " + this.z_tiempo_max_abs_para_sincronizar    );
        System.out.println("Sincronizacion. Numero de ficheros cargados:         " + this.z_ficheros_del_almacen.size()        );
        System.out.println("Sincronizacion. Nombre del fichero de versiones:     " + NOMBRE_FICHERO_VERSIONES                  );
        System.out.println("");
    }/*END AlmacenObjetoRemote*/

    
// PSEUDO-CONSTRUCTORES --------------------------------------------------------
    
    /**
     * Crea un objeto AlmacenObjeto usando un fichero de propiedades.                 <br>
     * Se devolvera null si hay algun problema con el fichero de propiedades enviado. <br>
     * 
     * @param  ruta_fich_propiedades        La ruta de un directorio y fichero para leer las propiedades sin tener que pasarlas por parametro
     * @param  ruta_fich_distribuida        La ruta de un directorio y fichero para leer las ips y nombre de servicio de otros almacenes con los que contactar y crear una red de servidores.
     * @param  rmi_registry_ip              La IP donde esta el servidor rmiregistry donde se va a registrar este objeto que ahora se esta creando (this).   Necesario para apagar.
     * @param  rmi_registry_nombre_servicio El nombre del servicio con el que se va a registrar en rmiregistry este objeto que ahora se esta creando (this). Necesario para apagar.
     * @return AlmacenObjeto                El objeto creado. Null si hubo problemas leyendo el fichero de propiedades.
     * @throws RemoteException              Al ser un objeto remoto se podría producir esta excepcion al intentar crear el objeto si hubiera algún problema en la red.
     */
    public static AlmacenObjetoRemote crearAlmacenObjetoRemote(String ruta_fich_propiedades, String ruta_fich_distribuida, String rmi_registry_ip, String rmi_registry_nombre_servicio) throws RemoteException{
    /*VAR*/
        Thread              hilo           =  null;        
        AlmacenObjetoRemote almacen_objeto =  null;
    /*BEGIN*/
        try{
            almacen_objeto = new AlmacenObjetoRemote (ruta_fich_propiedades, ruta_fich_distribuida, rmi_registry_ip, rmi_registry_nombre_servicio);
            if (almacen_objeto != null){                                        //Ejecuta el run() que hay implementando en este fichero. Da igual que no se haya pasado un fichero, más adelante otros podrían unirse a el.
                hilo = new Thread(almacen_objeto);
                almacen_objeto.anotarHiloRun(hilo, 0);                          //Cero porque es el primero. Debe anotarlo antes de ejecutarlo debido a que el metodo no bloquea la variable donde escribe. Mientras le hilo no se arranque y no se devuelva el objeto no habra nadie accediendo.
                hilo.start();
            }/*if*/
        }catch(IOException e){
            System.out.println(e);
            almacen_objeto = null;
        }catch (NumberFormatException nfe){
            System.out.println(nfe);
            almacen_objeto = null;
        }/*try-catch*/
        return almacen_objeto;
    }/*END crearAlmacenObjetoRemote*/
    
        
// METODOS PUBLICOS ------------------------------------------------------------
    
    /**
     * Eco, ecooooo                                               <br>
     * Se usa para comprobar la conexion de una manera sencilla.  <br>
     * Se devuelve la misma cadena que se envia como parametro.   <br>
     * 
     * @param  str_test La cadena que se espera recibir devuelta
     * @return String   La misma cadena que se envia como parametro
     */
    @Override
    public String testConnection(String str_test){
        return str_test;
    }/*END testConnection*/
    

    /**
     * Devuelve el identificador.
     * 
     * @return String  El identificador de la instancia.
     */
    @Override
    public String getID(){
    /*VAR*/
        return this.id;
    }/*END getID*/

    
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
     *    WARNING_DURANTE_LA_ESPERA      Mientras se esperaba a que quedara libre el fichero se produjo un error.                                                                           <br>
     *    INFO_PASSWORD_INVALIDA         La contraseña de escritura suministrada no es valida                                                                                               <br>
     *    INFO_ACCESO_NO_PERMITIDO       Se ha denegado el acceso, no se permite crear nuevas sesiones de escritura en este momento.                                                        <br>
     *                                                                                                                                                                                      <br>
     * No parece que haya riesgo de interbloqueo. En todo el codigo el anidamiento es siempre fuera el bloque a y dentro el b o el c.                                                       <br>
     * Dentro de los bloques b o c no se realiza esperas de ningun tipo.                                                                                                                    <br>
     * Se debe hacer el anidamiento porque para crear el nuevo id no puede estar creado que es una condicion a mirar en el grupo a.                                                         <br>
     * Tambien hay que comprobar que no se sobrepase un numero de ids para escritura y esto es una condicion a mirar en el grupo c.                                                         <br>
     * Una cosa parece bastante clara, una vez que se esta dentro del grupo a no se debe salir si hacer lo que se deba.                                                                     <br>
     * Salir del grupo a permitiria al otro hilo solicitar la misma operacion... no se, no se....                                                                                           <br>
     * 
     * @param  password_escritura    Se debe enviar la contraseña de escritura para crear una sesion para escribir un fichero.        
     * @param  nombre_directorio     Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros. Debe venir sin barra inicial pero con barra final.
     * @param  nombre_fichero        El nombre del fichero. 
     * @param  version               Una cadena para comparar la antiguedad de ficheros en reescrituras. Usa  ver_antigua.compareTo(ver_nueva)
     * @param  longitud_del_paquete  Para indicar cuantos bytes se enviaran cada vez que se mande un paquete
     * @param  cantidad_paquetes     Se debe decir antes de empezar cuantos paquetes seran enviados. Junto con longitud del paquete se sabra el tamaño del fichero.
     * @return String                Una cadena que identifica la comunicacion. Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema.
     */
    @Override
    public String solicitarEscribirFicheroEnServidor (String password_escritura, String nombre_directorio, String nombre_fichero, String version, int longitud_del_paquete, int cantidad_paquetes){
    /*VAR*/
        ControladorDeRecursosLectorEscritor controlador_de_accesos_aux =  null;
        List<FicheroPaquete>                lista_de_fichero_paquete   =  null;
        FicheroPaquete                      fichero_paquete_aux        =  null;
        FicheroInfo                         fichero_info               =  null;
        Semaphore                           hotel_dormir               =  null;
        String                              id_escritura_lectura       =    "";
        String                              password_escritura_actual  =    "";
        Integer                             lector_o_escritor          =     0;
        int                                 dormidos_actuales          =     0;
        int                                 resultado                  =     0;
        int                                 i                          =     0;
        boolean                             hay_que_dormir             = false;
        boolean                             es_nuevo                   = false;
        boolean                             password_valida            = false;
        boolean                             acceso_permitido           = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_escritura_actual = this.w_password_escritura;
            acceso_permitido          = this.w_acceso_permitido_escritura;
        this.lock_grupo_w.dejoDeLeer();
        
        password_valida = password_escritura_actual.equals(password_escritura);
        if ( acceso_permitido &&  password_valida){
            id_escritura_lectura = this.idParaFichero(nombre_directorio, nombre_fichero);            
            this.lock_grupo_z.quieroLeer();
                es_nuevo = this.nuevoFicheroVersion(id_escritura_lectura, version);
                if (!es_nuevo){
                    resultado = INFO_FICHERO_YA_EXISTE;
                    this.log("solicitarEscribirFicheroEnServidor. INFO_FICHERO_YA_EXISTE. (!es_nuevo). "  + nombre_directorio + nombre_fichero, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                }/*if*/
            this.lock_grupo_z.dejoDeLeer();
        
            while ( (lector_o_escritor != -1) && (resultado == 0) ){                                                        //Se espera mientras no se tenga el id de escritura y no haya errores. Al cosguirlo deja valores en los Maps que impide que se les de acceso a otros escritores.
                hay_que_dormir = false;
                this.lock_grupo_a.quieroEscribir();                                                                         //En modo escritura porque si no existe necesita crearlo
                    lector_o_escritor = this.a_lector_o_escritor.get(id_escritura_lectura);                                 //Si existe podria ser de lectura o escritura. Se debe esperar a que quede libre.
                    if (lector_o_escritor == null){                                                                         //Si no es null es que esta siendo enviado o leido. Debe esperar.
                        this.lock_grupo_c.quieroLeer();
                            resultado = this.sePermiteNuevoEscritor(id_escritura_lectura);                                  //No pasa nada por realizar la llamada en modo lectura y luego permitir usar el grupo c porque al estar dentro del bloque a y no existir el id_de_escritura ningun otro hilo puede crear un nuevo escritor y modificar el resultado.
                        this.lock_grupo_c.dejoDeLeer();
                        if (resultado == 0){                                                                                //Para poder crear el id ni debe existir uno ya y además no se debe haber sobrepasado la cantidad de escritores que pueden existir.
                            lector_o_escritor = -1;                                                                         //Ya si se acepta la solicitud y se marca con -1 para indicar que es de escritura.
                            hotel_dormir      = new Semaphore(0, false);                                                    //Cero para que el primero en hacer acquiere() sea lo mismo a hacer wait(). false porque a la hora de despertar no importa quien se durmio primero.                    
                            this.a_lector_o_escritor.put      (id_escritura_lectura, lector_o_escritor);                    //-1 indica que el id es para escritura
                            this.a_hoteles_dormir.put         (id_escritura_lectura, hotel_dormir     );                    //En este objeto esperaran todos los hilos que esten asociados a un id concreto.
                            this.a_cantidad_durmientes.put    (id_escritura_lectura, 0                );                    //Ahora mismo no hay ningun hilo durmiendo.
                            
                            controlador_de_accesos_aux = new ControladorDeRecursosLectorEscritor(id_escritura_lectura);     //Para gestionar las distintas escrituras simultaneas pero en varios ficheros diferentes a la vez.
                            fichero_info               = new FicheroInfo(id_escritura_lectura);
                            fichero_info.setDirectorioRelativo(nombre_directorio);
                            fichero_info.setNombreFichero     (nombre_fichero);
                            fichero_info.setVersion           (version);
                            fichero_info.setBorrado           (false);
                        
                            lista_de_fichero_paquete = new ArrayList<FicheroPaquete>();                                     //Como es una escritura se debe crear una lista de paquetes nueva para lo que se va a recibir
                            for (i=0; i<cantidad_paquetes; i++){                                                            //Se crean ya todos los paquetes en memoria para que solo tengan que ser rellenados con la informacion del paquete.
                                fichero_paquete_aux = new FicheroPaquete(id_escritura_lectura + i);                         //No parece tener mucha importancia el formato del identificador para este objeto, sera suficiente con que no se repitan.
                                fichero_paquete_aux.setNumeroDePaquete  (i);                                                //En la lista el primero es el cero.
                                fichero_paquete_aux.setTamPaquete       (longitud_del_paquete);                             //El array de bytes que contenga el paquete debe ser de esta longitud. 
                                lista_de_fichero_paquete.add (i, fichero_paquete_aux);                                      //Se supone que el orden de la lista coincidira con el numero del paquete. Es decir 'paquete = lista.get(n)' debe implicar 'n = paquete.getNumeroDePaquete()'
                            }/*for*/
                            this.lock_grupo_c.quieroEscribir();                                                             //Bloqueador general del grupo c porque necesita modificar los Maps, no se trata solo de acceder a informacion de un id en concreto.                        
                                this.c_escrituras.put               (id_escritura_lectura, lista_de_fichero_paquete  );
                                this.c_controlador_de_accesos.put   (id_escritura_lectura, controlador_de_accesos_aux);     //Para gestionar las distintas escrituras simultaneas pero en varios ficheros diferentes a la vez.
                                this.c_fichero_info.put             (id_escritura_lectura, fichero_info              );     //La informacion del fichero que se va a enviar.
                                this.c_tamano_paquetes.put          (id_escritura_lectura, longitud_del_paquete      );
                                this.c_cantidad_paquetes.put        (id_escritura_lectura, cantidad_paquetes         );
                                this.c_hora_de_inicio_escritura.put (id_escritura_lectura, new Date()                );     //Para controlar el tiempo que dura cada sesion y poder cancelarlas si se sobrepasa.
                            this.lock_grupo_c.dejoDeEscribir();
                            this.log("solicitarEscribirFicheroEnServidor. Solicitud aceptada para id: " + id_escritura_lectura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                        }else{
                            resultado = INFO_LIMITE_MAX_IDS_POR_CREAR;                                                     //Solo puede haber cierto numero de escritores a la vez aunque sea a ficheros diferentes.
                            this.log("solicitarEscribirFicheroEnServidor. INFO_LIMITE_MAX_IDS_POR_CREAR. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                        }/*if*/
                    }else{
                        lector_o_escritor =    0;                                                                           //Pudo entrar porque habia otro escritor. Pero aun no se le ha dado el permiso. Si se deja -1 se interpretaria que si tiene el permiso.
                        hay_que_dormir    = true;                                                                           //Si existe un id entonces cuando se creo tambien se creo el hotel
                        hotel_dormir      = this.a_hoteles_dormir.get      (id_escritura_lectura);                          //Si controlador_de_accesos_aux no es null hoteles_dormir tampoco lo seran.                    
                        dormidos_actuales = this.a_cantidad_durmientes.get (id_escritura_lectura);
                        this.a_cantidad_durmientes.put (id_escritura_lectura, dormidos_actuales + 1);                       //EnvioFinalizado debe despertar a todos los que esten antes de eliminar la referencia del Map hoteles_dormir.
                    }/*if*/
                this.lock_grupo_a.dejoDeEscribir();
                
                if (hay_que_dormir){                                                                                        //Espera fuera del synchronized para no bloquear. Se busca que el despertar solo sea para los hilos que lo necesitan, despertar a hilos de otro id es sobrecargar.
                    try{
                        hotel_dormir.acquire();                                                                             //Puede que EnvioFinalizado, aunque ejecute su bloque lock_a despues, llegase a hacer el notify primero. Es por esto que no se puede usar wait-notify y hace falta llevar la cuenta de los notify que se hacen usando un semaphore
                        this.lock_grupo_z.quieroLeer();                                                                     //Al despertar podria haber un fichero mas reciente.
                            es_nuevo = this.nuevoFicheroVersion(id_escritura_lectura, version);
                            if (!es_nuevo){
                                resultado = INFO_FICHERO_YA_EXISTE;
                                this.log("solicitarEscribirFicheroEnServidor. INFO_FICHERO_YA_EXISTE. (!es_nuevo). "  + nombre_directorio + nombre_fichero, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                            }/*if*/
                        this.lock_grupo_z.dejoDeLeer();
                    }catch (InterruptedException e){
                        resultado = WARNING_DURANTE_LA_ESPERA;
                        this.log("solicitarEscribirFicheroEnServidor. WARNING_DURANTE_LA_ESPERA. ", "", MOSTAR_SERVIDOR[2-1], MOSTAR_FECHA[2-1], 2);
                    }/*try-catch*/
                }/*if*/
            }/*while*/
        }else if ( !acceso_permitido ){
            resultado = INFO_ACCESO_NO_PERMITIDO;
            this.log("solicitarEscribirFicheroEnServidor. INFO_ACCESO_NO_PERMITIDO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if( !password_valida ){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("solicitarEscribirFicheroEnServidor. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        if (resultado < 0){
            id_escritura_lectura = "" + resultado + "";
        }/*if*/
        return id_escritura_lectura;
    }/*END solicitarEscribirFicheroEnServidor*/

    
    /**
     * Para continuar una escritura que no se termino. Se llamo al metodo escribiendoFicheroEnServidorEscrituraFinalizada y este dijo que quedaban partes por escribir.                   <br>
     * Se envia el nombre del fichero junto con una añadido para que no se confunda con otros posibles ficheros con el mismo nombre.                                                      <br>
     * El servidor crea id para esta comunicacion que es lo que le devuelve al cliente.                                                                                                   <br>
     * El cliente usara este id para enviar su fichero y que no se mezcle la informacion de otros ficheros que se esten subiendo a la vez.                                                <br>
     * El fichero permanecera en memoria del servidor hasta se llame a escribiendoFicheroEnServidorEscrituraFinalizada.                                                                   <br>
     * Si el id devuelto es negativo es que hubo algun problema:                                                                                                                          <br>
     *    INFO_FICHERO_NO_ENCONTRADO      Fichero no encontrado                                                                                                                           <br>
     *    INFO_LIMITE_MAX_IDS_POR_CREAR   Se ha superado el limite ids de escritura que se pueden crear. Solo puede haber un escritor por archivo pero varios para archivos diferentes.   <br>
     *    WARNING_DURANTE_LA_ESPERA       Mientras se esperaba a que quedara libre el fichero se produjo un error.                                                                        <br>
     *    INFO_PASSWORD_INVALIDA          La contraseña de escritura suministrada no es valida                                                                                            <br>
     *    INFO_ACCESO_NO_PERMITIDO        Se ha denegado el acceso, no se permite crear nuevas sesiones de escritura en este momento.                                                     <br>
     *                                                                                                                                                                                    <br>
     * No parece que haya riesgo de interbloqueo. En todo el codigo el anidamiento es siempre fuera el bloque a y dentro el b o el c.                                                     <br>
     * Dentro de los bloques b o c no se realiza esperas de ningun tipo.                                                                                                                  <br>
     * Se debe hacer el anidamiento porque para crear el nuevo id no puede estar creado que es una condicion a mirar en el grupo a.                                                       <br>
     * Tambien hay que comprobar que no se sobrepase un numero de ids para escritura y esto es una condicion a mirar en el grupo c.                                                       <br>
     * Una cosa parece bastante clara, una vez que se esta dentro del grupo a no se debe salir si hacer lo que se deba.                                                                   <br>
     * Salir del grupo a permitiria al otro hilo solicitar la misma operacion... no se, no se....                                                                                         <br>
     *                                                                                                                                                                                    <br>
     * NOTA: Metodo pendiente de poder almacenar la longitud del paquete.                                                                                                                 <br>
     *       Requiere revision completa, solicitarEscribir ha sido acutalizado y este no.                                                                                                 <br>
     *       
     * @param  password_escritura    Se debe enviar la contraseña de escritura para crear una sesion para escribir un fichero.        
     * @param  nombre_directorio     Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros.
     * @param  nombre_fichero        El nombre del fichero. 
     * @param  version               Una cadena para comparar la antiguedad de ficheros en reescrituras. Usa  ver_antigua.compareTo(ver_nueva)  (NO SE ESTA TENIENDO EN CUENTA)
     * @return String                Un numero que identifica la comunicacion. Así se puede enviar el fichero a trozos y sin prisas.
     * @throws RemoteException 
     */
    @Override
    public String solicitarContinuarEscribirFicheroEnServidor (String password_escritura, String nombre_directorio, String nombre_fichero, String version){
    /*VAR*/
        ControladorDeRecursosLectorEscritor controlador_de_accesos_aux =  null;
        List<FicheroPaquete>                lista_de_fichero_paquete   =  null;
        FicheroInfo                         fichero_info               =  null;
        Semaphore                           hotel_dormir               =  null;        
        String                              id_escritura_lectura       =    "";
        String                              password_escritura_actual  =    "";
        Integer                             lector_o_escritor          =     0;
        int                                 num_paquetes_en_la_lista   =     0;
        int                                 dormidos_actuales          =     0;
        int                                 resultado                  =     0;
        boolean                             hay_que_dormir             = false;
        boolean                             password_valida            = false;
        boolean                             acceso_permitido           = false;        
    /*BEGIN*/
if (false){
        this.lock_grupo_w.quieroLeer();
            password_escritura_actual = this.w_password_escritura;
            acceso_permitido          = this.w_acceso_permitido_escritura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_escritura_actual.equals(password_escritura);
        if ( acceso_permitido &&  password_valida){
            id_escritura_lectura = this.idParaFichero(nombre_directorio, nombre_fichero);
            if ( FicherosTrabajar.exiteFichero(this.directorio_base + nombre_directorio, nombre_fichero) ){
                while ( (lector_o_escritor != -1) && (resultado == 0) ){                                                       //Se espera mientras no se tenga el id de escritura y no haya errores. Al cosguirlo deja valores en los Maps que impide que se les de acceso a otros escritores.
                    hay_que_dormir = false;
                    this.lock_grupo_a.quieroEscribir();                                                                        //En modo escritura porque si no existe necesita crearlo
                        lector_o_escritor = this.a_lector_o_escritor.get(id_escritura_lectura);                                //Si existe podria ser de lectura o escritura. Se debe esperar a que quede libre.   
                        if (lector_o_escritor == null){                                                                        //Si no es null es que esta siendo enviado o leido. Debe esperar.
                            this.lock_grupo_c.quieroLeer();
                                resultado = this.sePermiteNuevoEscritor(id_escritura_lectura);                                 //No pasa nada por realizar la llamada en modo lectura y luego permitir usar el grupo c porque al estar dentro del bloque a y no existir el id_de_escritura ningun otro hilo puede crear un nuevo escritor y modificar el resultado.
                            this.lock_grupo_c.dejoDeLeer();
                            if (resultado == 0){                                                                               //Para poder crear el id ni debe existir uno ya y además no se debe haber sobrepasado la cantidad de escritores que pueden existir.
                                lector_o_escritor = -1;
                                hotel_dormir      = new Semaphore(0, false);                                                   //Cero para que el primero en hacer acquiere() sea lo mismo a hacer wait(). false porque a la hora de despertar no importa quien se durmio primero.                    
                                this.a_lector_o_escritor.put      (id_escritura_lectura, lector_o_escritor);                   //-1 indica que el id es para escritura
                                this.a_hoteles_dormir.put         (id_escritura_lectura, hotel_dormir     );                   //En este objeto esperaran todos los hilos que esten asociados a un id concreto.
                                this.a_cantidad_durmientes.put    (id_escritura_lectura, 0                );                   //Ahora mismo no hay ningun hilo durmiendo.
                                
                                controlador_de_accesos_aux = new ControladorDeRecursosLectorEscritor(id_escritura_lectura);                                
                                fichero_info               = new FicheroInfo(id_escritura_lectura);
                                fichero_info.setDirectorioRelativo(nombre_directorio);
                                fichero_info.setNombreFichero     (nombre_fichero);
                                fichero_info.setVersion           (version);
                                fichero_info.setBorrado           (false);
//                              lista_de_fichero_paquete   = leerFichero(id_escritura_lectura, longitud_del_paquete);          //Mejor leerlo aqui por si al salir de lock_a otro hilo decide borrarlo.                                                    
                                num_paquetes_en_la_lista   = lista_de_fichero_paquete.size();                            
                            
                                this.lock_grupo_c.quieroEscribir();                                                            //Bloqueador general del grupo c porque necesita modificar los Maps, no se trata solo de acceder a informacion de un id en concreto.                        
                                    this.c_escrituras.put               (id_escritura_lectura, lista_de_fichero_paquete  );
                                    this.c_controlador_de_accesos.put   (id_escritura_lectura, controlador_de_accesos_aux);    //Al colocar -1 este controlador es para escribir el fichero enviado.
                                    this.c_fichero_info.put             (id_escritura_lectura, fichero_info              );    //La informacion del fichero que se va a enviar.                            
//                                  this.c_tamano_paquetes.put          (id_escritura_lectura, longitud_del_paquete      );
                                    this.c_cantidad_paquetes.put        (id_escritura_lectura, num_paquetes_en_la_lista  );                
                                    this.c_hora_de_inicio_escritura.put (id_escritura_lectura, new Date()                );
                                this.lock_grupo_c.dejoDeEscribir();
                                this.log("solicitarContinuarEscribirFicheroEnServidor. Solicitud aceptada para id: " + id_escritura_lectura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                            }else{
                                resultado = INFO_LIMITE_MAX_IDS_POR_CREAR;                                                     //Solo puede haber cierto numero de escritores a la vez aunque sea a ficheros diferentes.
                                this.log("solicitarContinuarEscribirFicheroEnServidor. INFO_LIMITE_MAX_IDS_POR_CREAR. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                            }/*if*/
                        }else{
                            hay_que_dormir    = true;                                                                          //Si existe un id entonces cuando se creo tambien se creo el hotel
                            hotel_dormir      = this.a_hoteles_dormir.get      (id_escritura_lectura);                         //Si controlador_de_accesos_aux no es null hoteles_dormir tampoco lo seran.                    
                            dormidos_actuales = this.a_cantidad_durmientes.get (id_escritura_lectura);
                            this.a_cantidad_durmientes.put (id_escritura_lectura, dormidos_actuales + 1);                      //EnvioFinalizado debe despertar a todos los que esten antes de eliminar la referencia del Map hoteles_dormir.
                        }/*if*/
                    this.lock_grupo_a.dejoDeEscribir();
                    if (hay_que_dormir){                                                                                       //Espera fuera del synchronized para no bloquear. Se busca que el despertar solo sea para los hilos que lo necesitan, despertar a hilos de otro id es sobrecargar.
                        try {
                            hotel_dormir.acquire();                                                                            //Puede que EnvioFinalizado, aunque ejecute su bloque lock_a despues, llegase a hacer el notify primero. Es por esto que no se puede usar wait-notify y hace falta llevar la cuenta de los notify que se hacen usando un semaphore
                        } catch (InterruptedException e) {
                            resultado = WARNING_DURANTE_LA_ESPERA;
                            this.log("solicitarContinuarEscribirFicheroEnServidor. WARNING_DURANTE_LA_ESPERA. ", "", MOSTAR_SERVIDOR[2-1], MOSTAR_FECHA[2-1], 2);
                        }/*try-catch*/
                    }/*if*/
                }/*while*/
            }else{
                resultado = INFO_FICHERO_NO_ENCONTRADO;
                this.log("solicitarContinuarEscribirFicheroEnServidor. INFO_FICHERO_NO_ENCONTRADO. "  + nombre_directorio + "   " + nombre_fichero, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
        }else if ( !acceso_permitido ){
            resultado = INFO_ACCESO_NO_PERMITIDO;
            this.log("solicitarContinuarEscribirFicheroEnServidor. INFO_ACCESO_NO_PERMITIDO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if( !password_valida ){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("solicitarContinuarEscribirFicheroEnServidor. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        if (resultado < 0){
            id_escritura_lectura = "" + resultado + "";
        }/*if*/
}
        return id_escritura_lectura;
    }/*END solicitarContinuarEscribirFicheroEnServidor*/
    
    
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
    @Override
    public int escribiendoFicheroEnServidorEnviandoPaquete  (String password_escritura, String id_de_escritura, int[] paquete, int numero_del_paquete, int cantidad_bytes){
    /*VAR*/
        ControladorDeRecursosLectorEscritor controlador_de_accesos_aux  =  null;
        List<FicheroPaquete>                lista_de_fichero_paquete    =  null;
        FicheroPaquete                      fichero_paquete_aux         =  null;
        Integer                             lector_o_escritor           =  null;
        String                              password_escritura_actual   =    "";
        int                                 resultado                   =     0;
        int                                 tam_paquete_para_un_id      =     0;
        int                                 tam_paquete                 =     0;
        boolean                             password_valida             = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_escritura_actual = this.w_password_escritura;
        this.lock_grupo_w.dejoDeLeer();
        
        password_valida = password_escritura_actual.equals(password_escritura);
        if (password_valida){
            this.lock_grupo_a.quieroLeer();                                                            //Este bloqueo permite acceder a otros hilos a variables del grupo a para leerlas.
                lector_o_escritor = this.a_lector_o_escritor.get(id_de_escritura);                     //Si hay controlador se debe comprobar que sea de escritura
            this.lock_grupo_a.dejoDeLeer();
            if ( (lector_o_escritor != null) && (lector_o_escritor == -1) ){                           //Debe haber id y que sea de escritura.
                this.lock_grupo_c.quieroLeer();                                                        //Bloqueo general para grupo c en modo lectura. Se recoge la informacion concreta de un id
                    lista_de_fichero_paquete   = this.c_escrituras.get            (id_de_escritura);   //Si existe el id no devolvera null. Pero EnvioFinalizado podria haberle adelantado y haber borrado.            
                    controlador_de_accesos_aux = this.c_controlador_de_accesos.get(id_de_escritura);   //Para controlar el acceso simultaneo a un fichero concreto.
                    tam_paquete_para_un_id     = this.c_tamano_paquetes.get       (id_de_escritura);
                this.lock_grupo_c.dejoDeLeer();
                if (lista_de_fichero_paquete != null){                                                 //Podría ser null porque entre las lecturas anteriores del grupo a_ la del grupo c_ un hilo podria haber llamado a EnvioFinalizado
                    controlador_de_accesos_aux.quieroEscribir();                                       //Se bloquea solo lo relativo a un id. Se podrían haber creado varios hilos para que cada uno mandase un paquete.
                        tam_paquete = paquete.length;
                        if (tam_paquete == tam_paquete_para_un_id ){                                   //El paquete enviado debe tener el tamaño que se espera. 
                            fichero_paquete_aux = lista_de_fichero_paquete.get(numero_del_paquete);
                            if (fichero_paquete_aux != null){                                          //El numero del paquete debe existir. Siempre existira a no ser que sea mayor que lo establecido en solicitarEscritura
                                fichero_paquete_aux.setBytes                  (paquete);
                                fichero_paquete_aux.setCantidadDeBytesValidos (cantidad_bytes);
                                this.log("escribiendoFicheroEnServidorEnviandoPaquete. Paquete aceptado para id: " + id_de_escritura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                            }else{
                                resultado = INFO_PAQUETE_FUERA_DE_RANGO;
                                this.log("escribiendoFicheroEnServidorEnviandoPaquete. INFO_PAQUETE_FUERA_DE_RANGO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                            }/*if*/
                        }else{
                            resultado = INFO_TAMANO_PAQUETE_ARRAY;
                            this.log("escribiendoFicheroEnServidorEnviandoPaquete. INFO_TAMANO_PAQUETE_ARRAY. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                        }/*if*/
                    controlador_de_accesos_aux.dejoDeEscribir();
                }else{
                    resultado = INFO_ID_NO_EXISTE;
                    this.log("escribiendoFicheroEnServidorEnviandoPaquete. INFO_ID_NO_EXISTE. (lista_de_fichero_paquete == null). Existio pero alguien pudo finalizar la escritura. "  + id_de_escritura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                }/*if*/
            }else{
                resultado = INFO_ID_NO_EXISTE;                                                        //No existe el id enviado como id_de_escritura
                this.log("escribiendoFicheroEnServidorEnviandoPaquete. INFO_ID_NO_EXISTE. ( (lector_o_escritor == null) || (lector_o_escritor != -1) ). No existe o no es de escritura. "  + id_de_escritura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
        }else if( !password_valida ){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("escribiendoFicheroEnServidorEnviandoPaquete. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END escribiendoFicheroEnServidorEnviandoPaquete*/
    
    
    /**
     * METODO OPCIONAL: Necesita que el Cliente tenga la clase FicheroPaquete y esto es algo que no se puede exigir.      <br>
     *                                                                                                                    <br>
     * Con una única llamada se envían todos los paquetes.                                                                <br>
     * Resultados:                                                                                                        <br>
     *    ERROR_SIN_ERRORES            Operacion realizada correctamente                                                  <br>
     *    INFO_ID_NO_EXISTE            No existe el id_de_escritura                                                       <br>
     *    INFO_TAMANO_PAQUETE_ARRAY    El array de bytes no tiene el tamaño correcto.                                     <br>
     *    INFO_PAQUETE_FUERA_DE_RANGO  El numero del paquete enviado esta fuera de rango.                                 <br>
     *    INFO_PASSWORD_INVALIDA       La contraseña de escritura suministrada no es valida                               <br>
     * 
     * @param  password_escritura  Se debe enviar la contraseña de escritura para crear una sesion para escribir un fichero.
     * @param  id_de_escritura     El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @param  lista_paquetes      El contenido integro del fichero en una lista de paquetes.
     * @return int                 Resultado de la operacion.
     */
    @Override
    public int escribiendoFicheroEnServidorEnviandoPaquetes (String password_escritura, String id_de_escritura, List<FicheroPaquete> lista_paquetes_origen){
    /*VAR*/
        ControladorDeRecursosLectorEscritor controlador_de_accesos_aux  = null;
        List<FicheroPaquete>                lista_paquetes_destino      = null;
        FicheroPaquete                      paquete_origen              = null;
        FicheroPaquete                      paquete_destino             = null;
        String                              password_escritura_actual   =    "";
        Integer                             lector_o_escritor           = null;
        int[]                               array_de_bytes_orgien       = null;
        int                                 resultado                   =    0;
        int                                 tam_paquete_para_un_id      =    0;
        int                                 cantidad_bytes              =    0;
        int                                 i                           =    0;
        boolean                             password_valida             = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_escritura_actual = this.w_password_escritura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_escritura_actual.equals(password_escritura);
        if (password_valida){
            this.lock_grupo_a.quieroLeer();                                                            //Este bloqueo permite acceder a otros hilos a variables del grupo a para leerlas.
                lector_o_escritor = this.a_lector_o_escritor.get(id_de_escritura);                     //Si hay controlador se debe comprobar que sea de escritura
            this.lock_grupo_a.dejoDeLeer();
        
            if ( (lector_o_escritor != null) && (lector_o_escritor == -1) ){                           //Debe haber id y que sea de escritura.
                this.lock_grupo_c.quieroLeer();                                                        //Bloqueo general para grupo c en modo lectura. Se recoge la informacion concreta de un id
                    lista_paquetes_destino     = this.c_escrituras.get            (id_de_escritura);   //Si existe el id no devolvera null. Pero EnvioFinalizado podria haberle adelantado y haber borrado.            
                    controlador_de_accesos_aux = this.c_controlador_de_accesos.get(id_de_escritura);   //Para controlar el acceso simultaneo a un fichero concreto.
                    tam_paquete_para_un_id     = this.c_tamano_paquetes.get       (id_de_escritura);
                this.lock_grupo_c.dejoDeLeer();
            
                if (lista_paquetes_destino != null){                                                   //Podría ser null porque entre las lecturas anteriores del grupo a_ la del grupo c_ un hilo podria haber llamado a EnvioFinalizado
                    controlador_de_accesos_aux.quieroEscribir();                                       //Se bloquea solo lo relativo a un id. Se podrían haber creado varios hilos para que cada uno mandase un paquete.    
                        while( (i<lista_paquetes_origen.size()) && (resultado == 0) ){                 //No se sustituye la lista ni los paquete directamente para evitar errores
                            paquete_origen  = lista_paquetes_origen.get (i);
                            paquete_destino = lista_paquetes_destino.get(i);
                            if (paquete_destino != null){
                                array_de_bytes_orgien = paquete_origen.getBytes();
                                cantidad_bytes        = paquete_origen.getCantidadDeBytesValidos();
                                if (array_de_bytes_orgien.length == tam_paquete_para_un_id ){          //El paquete enviado debe tener el tamaño que se espera.
                                    paquete_destino.setBytes                  (array_de_bytes_orgien);
                                    paquete_destino.setCantidadDeBytesValidos (cantidad_bytes);
                                    this.log("escribiendoFicheroEnServidorEnviandoPaquetes. Paquete aceptado para id: " + id_de_escritura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                                }else{
                                    resultado = INFO_TAMANO_PAQUETE_ARRAY;
                                    this.log("escribiendoFicheroEnServidorEnviandoPaquetes. INFO_TAMANO_PAQUETE_ARRAY. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                                }/*if*/
                            }else{
                                resultado = INFO_PAQUETE_FUERA_DE_RANGO;
                                this.log("escribiendoFicheroEnServidorEnviandoPaquetes. INFO_PAQUETE_FUERA_DE_RANGO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                            }/*if*/
                            i++;
                        }/*while*/
                    controlador_de_accesos_aux.dejoDeEscribir();
                }else{
                    resultado = INFO_ID_NO_EXISTE;
                    this.log("escribiendoFicheroEnServidorEnviandoPaquetes. INFO_ID_NO_EXISTE. (lista_de_fichero_paquete == null). Existio pero alguien pudo finalizar la escritura. "  + id_de_escritura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                }/*if*/
            }else{
                resultado = INFO_ID_NO_EXISTE;
                this.log("escribiendoFicheroEnServidorEnviandoPaquetes. INFO_ID_NO_EXISTE. ( (lector_o_escritor == null) || (lector_o_escritor != -1) ). No existe o no es de escritura. "  + id_de_escritura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
        }else if( !password_valida ){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("escribiendoFicheroEnServidorEnviandoPaquetes. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END escribiendoFicheroEnServidorEnviandoPaquetes*/
    
    
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
     */
    @Override
    public List<Integer> escribiendoFicheroEnServidorPaquetesQueFaltan(String password_escritura, String id_de_escritura){
    /*VAR*/
        ControladorDeRecursosLectorEscritor controlador_de_accesos_aux = null;
        List<FicheroPaquete>               lista_de_fichero_paquete    = null;
        FicheroPaquete                     fichero_paquete_aux         = null;        
        List<Integer>                      paquetes_que_faltan         = new ArrayList<Integer>();
        String                             password_escritura_actual   =   "";
        Integer                            lector_o_escritor           = null;
        int                                num_de_paquetes             =    0;
        int                                i                           =    0;
        boolean                            password_valida             = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_escritura_actual = this.w_password_escritura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_escritura_actual.equals(password_escritura);
        if (password_valida){
            this.lock_grupo_a.quieroLeer();                                                                           //Este bloqueo permite acceder a otros hilos a variables del grupo a para leerlas.
                lector_o_escritor = this.a_lector_o_escritor.get(id_de_escritura);                                    //Si hay controlador se debe comprobar que sea de escritura
            this.lock_grupo_a.dejoDeLeer();
        
            if ( (lector_o_escritor != null) && (lector_o_escritor == -1) ){                                          //Debe haber id y que sea de escritura.
                this.lock_grupo_c.quieroLeer();
                    lista_de_fichero_paquete   = this.c_escrituras.get(id_de_escritura);                              //Si existe el id no devolvera null. Pero escribiendoFicheroEnServidorEnvioFinalizado podria haberle adelantado y haber borrado.            
                    controlador_de_accesos_aux = this.c_controlador_de_accesos.get(id_de_escritura);                  //Para controlar el acceso simultaneo a un fichero concreto.
                this.lock_grupo_c.dejoDeLeer();
            
                if (lista_de_fichero_paquete != null){
                    controlador_de_accesos_aux.quieroLeer();                                                          //Es necesario bloquear a otro hilos aunque solo se permita un escritor pues se podrían haber creado varios hilos y que cada uno mandase un paquete. Se permite que varios llamen a este metodo u otros que no modifiquen las variables comunes accedidas aqui, las del grupo c_
                        num_de_paquetes = lista_de_fichero_paquete.size();
                        for (i=0; i<num_de_paquetes; i++){                                                            //Se supone que 'i' debe coincidir con getNumeroDePaquete
                            fichero_paquete_aux = lista_de_fichero_paquete.get(i);                                    //Se asume que no devuelve null si existe el id
                            if ( !fichero_paquete_aux.getPaqueteValido() ){                                           //No es valido si no se le ha dado un byte[]
                                paquetes_que_faltan.add( new Integer(fichero_paquete_aux.getNumeroDePaquete()) );
                            }/*if*/
                        }/*for*/
                    controlador_de_accesos_aux.dejoDeLeer();                                                          //No se puede liberar antes, hay que tener en cuenta que el cliente puede llamar con varios hilos a metodos que accedan al mismo id y mientas esta leyendo no se puede modificar            
                    this.log("escribiendoFicheroEnServidorPaquetesQueFaltan. Faltan " + paquetes_que_faltan + " para id: " + id_de_escritura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                }else{
                    paquetes_que_faltan.add( INFO_ID_NO_EXISTE );                                                     //Ya no existe. El hilo de este metodo entro en lock_controlador_de_accesos primero pero un hilo en el metodo escribiendoFicheroEnServidorEnvioFinalizado entro antes quieroEscribir escribiendo el contenido de los paquetes a disco y borrando el id
                    this.log("escribiendoFicheroEnServidorPaquetesQueFaltan. INFO_ID_NO_EXISTE. (lista_de_fichero_paquete == null). Existio pero alguien pudo finalizar la escritura. "  + id_de_escritura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                }/*if*/
            }else{
                paquetes_que_faltan.add( INFO_ID_NO_EXISTE );                                                        //No existe el id enviado
                this.log("escribiendoFicheroEnServidorPaquetesQueFaltan. INFO_ID_NO_EXISTE. ( (lector_o_escritor == null) || (lector_o_escritor != -1) ). No existe o no es de escritura. "  + id_de_escritura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
        }else if( !password_valida ){
            paquetes_que_faltan.add( INFO_PASSWORD_INVALIDA );
            this.log("escribiendoFicheroEnServidorPaquetesQueFaltan. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return paquetes_que_faltan;
    }/*END escribiendoFicheroEnServidorPaquetesQueFaltan*/

    
    /**
     * Libera todos los recursos asociados a un fichero que se estaba enviando al servidor.           <br>
     * Es ahora cuando se almacena en disco sin que importe que se hayan enviado todos los paquetes.  <br>
     * Resultados:                                                                                    <br>
     *    ERROR_SIN_ERRORES        Operacion realizada correctamente                                  <br>
     *    INFO_ID_NO_EXISTE        No existe el id_de_escritura                                       <br>
     *    INFO_PASSWORD_INVALIDA   La contraseña de escritura suministrada no es valida               <br>
     * 
     * @param  password_escritura  Se debe enviar la contraseña de escritura para crear una sesion para escribir un fichero.
     * @param  id_de_escritura     El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return int                 Resultado de la operacion. 0 Todo correcto. INFO_ID_NO_EXISTE  No existe el id_de_escritura
     */
    @Override
    public int escribiendoFicheroEnServidorEnvioFinalizado(String password_escritura, String id_de_escritura){
    /*VAR*/
        FicheroInfo                         fichero_info               =  null;
        List<FicheroPaquete>                lista_de_fichero_paquete   =  null;
        Semaphore                           hotel_dormir               =  null;
        String                              nombre_directorio          =    "";
        String                              nombre_fichero             =    "";
        String                              password_escritura_actual  =    "";
        Integer                             lector_o_escritor          =     0;
        int                                 dormidos_actuales          =     0;
        int                                 resultado                  =     0;
        int                                 i                          =     0;
        boolean                             password_valida            = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_escritura_actual = this.w_password_escritura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_escritura_actual.equals(password_escritura);
        if (password_valida){
            this.lock_grupo_a.quieroEscribir();                                                                   //Se debe bloquear todo el proceso. En rescrituras, borrados y lecturas, el hueco entre la escritura del grupo C y la del gupo A un lector encontrará el fichero en disco pero en la estructura aun estara marcado como borrado.
                lector_o_escritor = this.a_lector_o_escritor.get(id_de_escritura);                                //Si hay controlador se debe comprobar que sea de escritura
                if ( (lector_o_escritor != null) && (lector_o_escritor == -1) ){                                  //Debe haber id y que sea de escritura.
                    this.lock_grupo_c.quieroEscribir();
                        lista_de_fichero_paquete = this.c_escrituras.get(id_de_escritura);                        //Si existe el id no devolvera null... no deberia,
                        if (lista_de_fichero_paquete != null){
                            fichero_info      = this.c_fichero_info.get            (id_de_escritura);
                            nombre_directorio = fichero_info.getDirectorioRelativo ();
                            nombre_fichero    = fichero_info.getNombreFichero      ();
                            FicherosTrabajar.escribirFichero(this.directorio_base + nombre_directorio, nombre_fichero, lista_de_fichero_paquete);
                            this.c_escrituras.remove               (id_de_escritura);                             //Una vez escrito el fichero en disco ya no se permite recibir mas paquetes.
                            this.c_controlador_de_accesos.remove   (id_de_escritura);                             //Otros hilos mantendran su referencia a controlador_de_accesos_aux pero ya no podran recibirla. Impide que dos llamadas a este metodo puedan escribir a disco, solo lo conseguira el primero.
                            this.c_fichero_info.remove             (id_de_escritura);
                            this.c_tamano_paquetes.remove          (id_de_escritura);
                            this.c_cantidad_paquetes.remove        (id_de_escritura);
                            this.c_hora_de_inicio_escritura.remove (id_de_escritura);
                        }/*if*/
                    this.lock_grupo_c.dejoDeEscribir();

                    hotel_dormir = this.a_hoteles_dormir.get (id_de_escritura);
                    if (hotel_dormir != null){                                                                //No deberia ser null
                        dormidos_actuales = this.a_cantidad_durmientes.get (id_de_escritura);                 //Y este tampoco deberia ser null y no se pregunta...
                        for (i=0; i<dormidos_actuales; i++){                                                  //Debe despertar a todos los que haya a la espera antes de eliminar la referencia del semaphore del Map.
                            hotel_dormir.release();                                                           //Si controlador_de_accesos_aux no es null entonces el Map de hoteles_dormir tampoco sera null. Se fuerno a dormir en los metodos de solicitar. Una vez despiertos se bloquearan en lock_a.
                        }/*for*/
                        this.a_lector_o_escritor.remove      (id_de_escritura);
                        this.a_hoteles_dormir.remove         (id_de_escritura);                               //Una vez que estan despierto se detruye el hotel. Si hace falta que se vuelva a crear.
                        this.a_cantidad_durmientes.remove    (id_de_escritura);
                        this.lock_grupo_z.quieroEscribir();                                                   //Una vez que el fichero ha sido escrito en el disco ya se añade a la lista definitiva de los fichero que contiene el Almacen
                            this.anotarFicheroEnMiEstructura(id_de_escritura, fichero_info);                  //Durante un tiempo el fichero estara escrito en disco sin que se haya informado a la estructura. 
                        this.lock_grupo_z.dejoDeEscribir();
                        this.log("escribiendoFicheroEnServidorEnvioFinalizado. Fin del envio para id: " + id_de_escritura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    }/*if*/
                }else{
                    resultado = INFO_ID_NO_EXISTE;
                    this.log("escribiendoFicheroEnServidorEnvioFinalizado. INFO_ID_NO_EXISTE. ( (lector_o_escritor == null) || (lector_o_escritor != -1) ). No existe o no es de escritura. "  + id_de_escritura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                }/*if*/
            this.lock_grupo_a.dejoDeEscribir();
        }else if( !password_valida ){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("escribiendoFicheroEnServidorEnvioFinalizado. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END escribiendoFicheroEnServidorEnvioFinalizado*/


    /**
     * Libera todos los recursos asociados a un fichero que se estaba enviando al servidor.           <br>
     * No escribe en disco. Es lo mismo que EnvioFinalizado pero sin escribir en disco.               <br>
     * Resultados:                                                                                    <br>
     *    ERROR_SIN_ERRORES        Operacion realizada correctamente                                  <br>
     *    INFO_ID_NO_EXISTE        No existe el id_de_escritura                                       <br>
     *    INFO_PASSWORD_INVALIDA   La contraseña de escritura suministrada no es valida               <br>
     * 
     * @param  password_escritura  Se debe enviar la contraseña de escritura para crear una sesion para escribir un fichero.
     * @param  id_de_escritura     El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return int                 Resultado de la operacion. 0 Todo correcto. INFO_ID_NO_EXISTE  No existe el id_de_escritura
     */
    @Override
    public int escribiendoFicheroEnServidorCancelar(String password_escritura, String id_de_escritura){
    /*VAR*/
        List<FicheroPaquete>                lista_de_fichero_paquete   =  null;
        Semaphore                           hotel_dormir               =  null;
        String                              password_escritura_actual  =    "";
        Integer                             lector_o_escritor          =     0;
        int                                 dormidos_actuales          =     0;
        int                                 resultado                  =     0;
        int                                 i                          =     0;
        boolean                             password_valida            = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_escritura_actual = this.w_password_escritura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_escritura_actual.equals(password_escritura);
        if (password_valida){
            this.lock_grupo_a.quieroEscribir();                                                                   //Se debe bloquear todo el proceso. En rescrituras, borrados y lecturas, el hueco entre la escritura del grupo C y la del gupo A un lector encontrará el fichero en disco pero en la estructura aun estara marcado como borrado.
                lector_o_escritor = this.a_lector_o_escritor.get(id_de_escritura);                                //Si hay controlador se debe comprobar que sea de escritura

                if ( (lector_o_escritor != null) && (lector_o_escritor == -1) ){                                  //Debe haber id y que sea de escritura.
                    this.lock_grupo_c.quieroEscribir();
                        lista_de_fichero_paquete = this.c_escrituras.get(id_de_escritura);                        //Si existe el id no devolvera null. A no ser que se hayan hecho varias llamadas a este metodo y las dos hayan pasado el grupo a_ y llegado a la vez aquí. Entonces una entra y borra y la otra se lo encuentra ya todo borrado.
                        if (lista_de_fichero_paquete != null){
                            //Aqui es donde iria la escritura a disco que no se realiza.
                            this.c_escrituras.remove               (id_de_escritura);                             //Una vez escrito el fichero en disco ya no se permite recibir mas paquetes.
                            this.c_controlador_de_accesos.remove   (id_de_escritura);                             //Otros hilos mantendran su referencia a controlador_de_accesos_aux pero ya no podran recibirla. Impide que dos llamadas a este metodo puedan escribir a disco, solo lo conseguira el primero.
                            this.c_fichero_info.remove             (id_de_escritura);
                            this.c_tamano_paquetes.remove          (id_de_escritura);
                            this.c_cantidad_paquetes.remove        (id_de_escritura);
                            this.c_hora_de_inicio_escritura.remove (id_de_escritura);
                        }/*if*/
                    this.lock_grupo_c.dejoDeEscribir();

                    hotel_dormir = this.a_hoteles_dormir.get (id_de_escritura);
                    if (hotel_dormir != null){                                                                    //Igual que en el grupo anterior, si hotel_dormir es null es porque se ha llamado dos veces a este metodo. Pero el grupo anterior podría hacerlo un hilo y este otro grupo otro hilo.
                        dormidos_actuales = this.a_cantidad_durmientes.get (id_de_escritura);
                        for (i=0; i<dormidos_actuales; i++){                                                      //Debe despertar a todos los que haya a la espera antes de eliminar la referencia del semaphore del Map.
                            hotel_dormir.release();                                                               //Si controlador_de_accesos_aux no es null entonces el Map de hoteles_dormir tampoco sera null. Se fuerno a dormir en los metodos de solicitar. Una vez despiertos se bloquearan en lock_a.
                        }/*for*/
                        this.a_lector_o_escritor.remove      (id_de_escritura);
                        this.a_hoteles_dormir.remove         (id_de_escritura);                                   //Una vez que estan despierto se detruye el hotel. Si hace falta que se vuelva a crear.
                        this.a_cantidad_durmientes.remove    (id_de_escritura);
                    }/*if*/
                    this.log("escribiendoFicheroEnServidorCancelar. Cancelado envio para id: " + id_de_escritura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                }else{
                    resultado = INFO_ID_NO_EXISTE;
                    this.log("escribiendoFicheroEnServidorCancelar. INFO_ID_NO_EXISTE. ( (lector_o_escritor == null) || (lector_o_escritor != -1) ). No existe o no es de escritura. "  + id_de_escritura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                }/*if*/
            this.lock_grupo_a.dejoDeEscribir();
        }else if( !password_valida ){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("escribiendoFicheroEnServidorCancelar. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;        
    }/*END escribiendoFicheroEnServidorCancelar*/


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
    @Override
    public String escribiendoFicheroEnServidorInfoFichero(String password_escritura, String id_de_escritura, int dir_o_file){
    /*VAR*/
        FicheroInfo                         fichero_info               =  null;
        String                              password_escritura_actual  =    "";
        String                              resultado                  =    "";
        Integer                             lector_o_escritor          =     0;
        boolean                             password_valida            = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_escritura_actual = this.w_password_escritura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_escritura_actual.equals(password_escritura);
        if (password_valida){
            this.lock_grupo_a.quieroLeer();
                lector_o_escritor = this.a_lector_o_escritor.get(id_de_escritura);                                //Si hay controlador se debe comprobar que sea de escritura            
            this.lock_grupo_a.dejoDeLeer();
            if ( (lector_o_escritor != null) && (lector_o_escritor == -1) ){                                      //Debe haber id y que sea de escritura.
                this.lock_grupo_c.quieroLeer();
                    fichero_info = this.c_fichero_info.get(id_de_escritura);                    
                this.lock_grupo_c.dejoDeLeer(); 
                if (fichero_info != null){
                    if (dir_o_file > 0){
                        resultado = fichero_info.getDirectorioRelativo ();
                    }else if (dir_o_file < 0){
                        resultado = fichero_info.getNombreFichero      ();
                    }/*if*/
                }else{
                    resultado = "" + INFO_ID_NO_EXISTE + "";
                    this.log("escribiendoFicheroEnServidorInfoFichero. INFO_ID_NO_EXISTE. (fichero_info == null). "  + id_de_escritura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                }/*if*/
            }else{
                resultado = "" + INFO_ID_NO_EXISTE + "";
                this.log("escribiendoFicheroEnServidorInfoFichero. INFO_ID_NO_EXISTE. ( (lector_o_escritor == null) || (lector_o_escritor != -1) ). No existe o no es de escritura. "  + id_de_escritura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
            
        }else if( !password_valida ){
            resultado = "" + INFO_PASSWORD_INVALIDA + "";
            this.log("escribiendoFicheroEnServidorInfoFichero. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END escribiendoFicheroEnServidorInfoFichero*/

    
//------------------------------------------------------------------------------
    
    /**
     * Para iniciar la lectura de un fichero que esta almacenado en el servidor.                                                                                                      <br>
     * Se envia el nombre del fichero junto con el directorio para que no se confunda con otros posibles ficheros con el mismo nombre.                                                <br>
     * El nombre del directorio debe empezar sin barra pero si debe terminar en barra. Aqui no se comprueba que este bien escrito. Fallara si no se envia bien.                       <br>
     * El servidor crea id para esta comunicacion que es lo que le devuelve al cliente.                                                                                               <br>
     * El cliente usara este id para solicitar trozos del fichero.                                                                                                                    <br>
     * Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema:                                                                 <br>
     *    INFO_FICHERO_NO_ENCONTRADO                            Fichero no encontrado                                                                                                 <br>
     *    INFO_LIMITE_MAX_IDS_POR_CREAR                         Se ha superado el limite de ids que se pueden crear.                                                                  <br>
     *    INFO_LIMITE_MAX_LECTORES_POR_ID                       Se ha superado el limite de lectores por ids.                                                                         <br>
     *    INFO_LIMITE_MAX_LECTORES                              Se ha superado el limite total de lectores que pueden acceder al servidor simultaneamente.                            <br>
     *    WARNING_DURANTE_LA_ESPERA                             Mientras se esperaba a que quedara libre el fichero se produjo un error.                                              <br>
     *    INFO_PASSWORD_INVALIDA                                La contraseña de lectura suministrada no es valida                                                                    <br>
     *    INFO_ACCESO_NO_PERMITIDO                              Se ha denegado el acceso, no se permite crear nuevas sesiones de lectura en este momento.                             <br>
     *    ERROR_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO  No debe estar el fichero en disco a la vez que esta marcado como borrado.                                             <br>
     * 
     * @param  password_lectura     Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.        
     * @param  nombre_directorio    Sirve para encontrar el directorio del fichero
     * @param  nombre_fichero       El nombre del fichero. 
     * @param  longitud_del_paquete El cliente establece cual debe ser la longitud de array de bytes
     * @return String               Una cadena que identifica la comunicacion. Así se puede leer el fichero a trozos y sin prisas.
     */
    @Override
    public String solicitarLeerFicheroDelServidor  (String password_lectura, String nombre_directorio, String nombre_fichero, int longitud_del_paquete){
    /*VAR*/
        FicheroInfo           fichero_info                =  null;
        List<FicheroPaquete>  lista_de_fichero_paquete    =  null;
        Semaphore             hotel_dormir                =  null;
        String                id_escritura_lectura        =    "";
        String                password_lectura_actual     =    "";
        Integer               lector_o_escritor           =     0;
        int                   cantidad_de_paquetes        =     0;
        int                   dormidos_actuales           =     0;
        int                   resultado                   =     0;
        boolean               hay_que_dormir              = false;
        boolean               existe_fichero              = false;
        boolean               fue_borrado                 = false;
        boolean               password_valida             = false;
        boolean               acceso_permitido            = false;        
    /*BEGIN*/
        id_escritura_lectura = this.idParaFichero(nombre_directorio, nombre_fichero);                                            //Es la ruta relativa donde debe ser almacenado el fichero. 
        
        this.lock_grupo_w.quieroLeer();
            password_lectura_actual = this.w_password_lectura;
            acceso_permitido        = this.w_acceso_permitido_lectura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_lectura_actual.equals(password_lectura);
        if ( acceso_permitido &&  password_valida ){                                                                             //Se permite el acceso y la contraseña es correcta.
            while ( (lector_o_escritor <= 0) && (resultado == 0) ){                                                              //Se queda en el bucle mientras haya un escritor con el fichero y no haya errores.
                hay_que_dormir = false;
                this.lock_grupo_a.quieroEscribir();                                                                              //Debe estar antes de preguntar por el fichero para que si existe ya no pueda ser borrado.
                
                    this.lock_grupo_z.quieroLeer();
                        fichero_info = this.z_ficheros_del_almacen.get(id_escritura_lectura);
                        if (fichero_info != null){
                            fue_borrado = fichero_info.getBorrado();
                        }/*if*/
                    this.lock_grupo_z.dejoDeLeer();
                    
                    existe_fichero = FicherosTrabajar.exiteFichero(this.directorio_base + nombre_directorio, nombre_fichero);
                    if ( existe_fichero && (fichero_info != null) && !fue_borrado  ){
                        lector_o_escritor = this.a_lector_o_escritor.get(id_escritura_lectura);                                  //Si hay controlador se debe comprobar que sea de escritura
                        if (lector_o_escritor == null){                                                                          //Si es null no existe y es la primera solicitud de lectua
                            this.lock_grupo_b.quieroEscribir();                                                                  //Si un segundo hilo solicita leer y va mas rapido que el primero, si el bloque del grupo a se coloca fuera ese segundo hilo que va mas rapido podria intentar realizar una lectura sin que aun esten preparadas todas las variables del grupo b.
                                resultado = this.sePermiteNuevoLector(id_escritura_lectura);
                                if (resultado == 0){                                                                             //Una vez que se tiene el visto bueno se crean todo a la vez, grupo a y grupo b.
                                    lector_o_escritor        = 1;
                                    hotel_dormir             = new Semaphore(0, true);                                           //Cero para que el primero en hacer acquiere() sea lo mismo a hacer wait().
                                    lista_de_fichero_paquete = FicherosTrabajar.leerFichero(this.directorio_base + nombre_directorio, nombre_fichero, longitud_del_paquete);
                                    if ( (lista_de_fichero_paquete != null) && (lista_de_fichero_paquete.size() > 0) ){
                                        this.a_lector_o_escritor.put   (id_escritura_lectura, lector_o_escritor         );       //Al colocar 1 este controlador es para leer el fichero enviado y es el primero.
                                        this.a_hoteles_dormir.put      (id_escritura_lectura, hotel_dormir              );       //En este objeto esperaran todos los hilos que esten asociados a un id concreto.
                                        this.a_cantidad_durmientes.put (id_escritura_lectura, 0                         );       //Ahora mismo no hay ningun hilo durmiendo.

                                        cantidad_de_paquetes = lista_de_fichero_paquete.size();
                                        this.b_lectores_por_id.put        (id_escritura_lectura, lector_o_escritor       );      //Es el primero. Variable redundante con this.a_lector_o_escritor
                                        this.b_lecturas.put               (id_escritura_lectura, lista_de_fichero_paquete);      //Asi los siguientes lectores no necesitan leer de disco.
                                        this.b_tamano_paquetes.put        (id_escritura_lectura, longitud_del_paquete    );
                                        this.b_cantidad_paquetes.put      (id_escritura_lectura, cantidad_de_paquetes    );
                                        this.b_hora_de_inicio_lectura.put (id_escritura_lectura, new Date()              );
                                        this.b_lectores_actuales_totales++;
                                        this.log("solicitarLeerFicheroDelServidor. Solicitud de lectura aceptada para id: " + id_escritura_lectura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                                    }else{
                                        resultado = INFO_FICHERO_VACIO;
                                        this.log("solicitarLeerFicheroDelServidor. INFO_FICHERO_VACIO. (lista_de_fichero_paquete). "  + nombre_directorio + "   " + nombre_fichero, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                                    }/*if*/
                                }else{
                                    this.log("solicitarLeerFicheroDelServidor. INFO_LIMITE_MAX_IDS_POR_CREAR 22 / INFO_LIMITE_MAX_LECTORES_POR_ID 23 / INFO_LIMITE_MAX_LECTORES 24: " + resultado + ". ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                                }/*if*/
                            this.lock_grupo_b.dejoDeEscribir();
                        }else if (lector_o_escritor > 0){
                            this.lock_grupo_b.quieroEscribir();                                                                  //Si un segundo hilo solicita leer y va mas rapido que el primero, si el bloque del grupo b se coloca fuera ese segundo hilo que va mas rapido podria intentar realizar una lectura sin que aun esten preparadas todas las variables del grupo b.
                                resultado = this.sePermiteNuevoLector(id_escritura_lectura);
                                if (resultado == 0){                                                                             //Una vez que se tiene el visto bueno se crean todo a la vez, grupo a y grupo b.
                                    this.a_lector_o_escritor.put      (id_escritura_lectura, lector_o_escritor + 1 );            //Se incrementa en uno los lectores para esta id. Esto es lo mismo que b_lectores_por_id
                                    this.b_lectores_por_id.put        (id_escritura_lectura, lector_o_escritor + 1 ); 
                                    this.b_hora_de_inicio_lectura.put (id_escritura_lectura, new Date()            );            //Se reinicia la fecha cada vez que llega un nuevo lector. Así solo se tiene en cuenta al ultimo lector, los demas ahora tendra un tiempo extra de lectura.
                                    this.b_lectores_actuales_totales++;
                                }/*if*/
                            this.lock_grupo_b.dejoDeEscribir();
                        }else{
                            hay_que_dormir    = true;                                                                            //Si existe un id entonces cuando se creo tambien se creo el hotel
                            hotel_dormir      = this.a_hoteles_dormir.get      (id_escritura_lectura);                           //Si controlador_de_accesos_aux no es null hoteles_dormir tampoco lo seran.                    
                            dormidos_actuales = this.a_cantidad_durmientes.get (id_escritura_lectura);
                            this.a_cantidad_durmientes.put (id_escritura_lectura, dormidos_actuales+1);                          //LecturaFinalizada debe despertar a todos los que esten antes de eliminar la referencia del Map hoteles_dormir.
                        }/*if*/
                    }else if (fichero_info == null){
                        resultado = INFO_FICHERO_NO_ENCONTRADO;                                                                  //Podria estar en el disco pero no esta en la estructura.
                        this.log("solicitarLeerFicheroDelServidor. INFO_FICHERO_NO_ENCONTRADO. (fichero_info == null). "  + nombre_directorio + "   " + nombre_fichero, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);                        
                    }else if (existe_fichero && fue_borrado){
                        resultado = ERROR_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO ;
                        this.log("solicitarLeerFicheroDelServidor. ERROR_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO . " + id_escritura_lectura, "", MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    }else{
                        resultado = INFO_FICHERO_NO_ENCONTRADO;                                                                  //El fichero no esta en el disco.
                        this.log("solicitarLeerFicheroDelServidor. INFO_FICHERO_NO_ENCONTRADO. !existe_fichero. "  + nombre_directorio + nombre_fichero, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                    }/*if*/
                this.lock_grupo_a.dejoDeEscribir();

                if (hay_que_dormir){                                                                                             //Espera fuera del synchronized para no bloquear. Se busca que el despertar solo sea para los hilos que lo necesitan, despertar a hilos de otro id es sobrecargar.
                    try {
                        hotel_dormir.acquire();                                                                                  //Puede que EnvioFinalizado, aunque ejecute su bloque lock_a despues, llegase a hacer el notify primero. Es por esto que no se puede usar wait-notify y hace falta llevar la cuenta de los notify que se hacen usando un semaphore
                    } catch (InterruptedException e) {
                        resultado = WARNING_DURANTE_LA_ESPERA;
                        this.log("solicitarLeerFicheroDelServidor. WARNING_DURANTE_LA_ESPERA. ", "", MOSTAR_SERVIDOR[2-1], MOSTAR_FECHA[2-1], 2);
                    }/*try-catch*/
                }/*if*/
            }/*while*/ 
        }else if ( !acceso_permitido ){
            resultado = INFO_ACCESO_NO_PERMITIDO;
            this.log("solicitarLeerFicheroDelServidor. INFO_ACCESO_NO_PERMITIDO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if( !password_valida ){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("solicitarLeerFicheroDelServidor. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        if (resultado < 0){
            id_escritura_lectura = "" + resultado + "";
        }/*if*/
        return id_escritura_lectura;
    }/*END solicitarLeerFicheroDelServidor*/

    
    /**
     * Para leer un paquete de informacion del fichero almacenado en el servidor  <br>
     * 
     * @param  password_lectura Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  id_de_lectura    El identificador que creo 'solicitarLeerFicheroDelServidor' antes de empezar la comunicacion  
     * @param  num_del_paquete  El numero del paquete del que se desea leer
     * @return int[]            El paquete con la informacion. null si el id_de_lectura no existe o si la contraseña es invalida.
     */
    @Override
    public int[] leyendoFicheroDelServidorLeerPaquete(String password_lectura, String id_de_lectura, int num_del_paquete){
    /*VAR*/
        List<FicheroPaquete>  lista_de_fichero_paquete   =  null;        
        FicheroPaquete        fichero_paquete_aux        =  null;
        int[]                 paquete                    =  null;
        String                password_lectura_actual    =    "";
        boolean               password_valida            = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_lectura_actual = this.w_password_lectura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_lectura_actual.equals(password_lectura);
        if (password_valida){
            this.lock_grupo_b.quieroLeer();
                lista_de_fichero_paquete = this.b_lecturas.get(id_de_lectura);                     //Si existe el id no devolvera null. Pero LecturaFinalizada podria haberle adelantado y haber borrado.                        
            this.lock_grupo_b.dejoDeLeer();
            
            if (lista_de_fichero_paquete != null){                                                 //No es necesario control de acceso, se tiene una referencia a algo que no va a ser modificado
                fichero_paquete_aux = lista_de_fichero_paquete.get(num_del_paquete);
                if (fichero_paquete_aux != null){
                    paquete = fichero_paquete_aux.getBytes();
                    this.log("leyendoFicheroDelServidorLeerPaquete. Paquete numero " + num_del_paquete + " leido para id: " + id_de_lectura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                }/*if*/
            }/*if*/
        }/*if*/
        return paquete;
    }/*END leyendoFicheroDelServidorLeerPaquete*/


    /**
     * METODO OPCIONAL: Necesita que el Cliente tenga la clase FicheroPaquete y esto es algo que no se puede exigir.      <br>
     *                                                                                                                    <br>
     * Con una única llamada se devuelven todos los paquetes.                                                             <br>
     * 
     * @param  password_lectura       Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  id_de_lectura          El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return List<FicheroPaquete>   El contenido integro del fichero en una lista de paquetes. Null si no existe el id_de_lectura o la contraseña es invalida.
     */
    @Override
    public List<FicheroPaquete> leyendoFicheroDelServidorLeerPaquetes(String password_lectura, String id_de_lectura){
    /*VAR*/
        List<FicheroPaquete> lista_de_fichero_paquete     =  null;        
        String                password_lectura_actual     =    "";
        boolean               password_valida             = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_lectura_actual = this.w_password_lectura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_lectura_actual.equals(password_lectura);
        if (password_valida){
            this.lock_grupo_b.quieroLeer();
                lista_de_fichero_paquete = this.b_lecturas.get(id_de_lectura);      //Si existe el id no devolvera null. Pero LecturaFinalizada podria haberle adelantado y haber borrado.                        
            this.lock_grupo_b.dejoDeLeer();
            this.log("leyendoFicheroDelServidorLeerPaquetes. Paquetes leidos para id: " + id_de_lectura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }/*if*/
        return lista_de_fichero_paquete;
    }/*END leyendoFicheroDelServidorLeerPaquetes*/

    
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
    @Override
    public int leyendoFicheroDelServidorTamanoDelPaquete (String password_lectura, String id_de_lectura){
    /*VAR*/
        Integer   tamano_del_paquete          =     0;
        String    password_lectura_actual     =    "";
        boolean   password_valida             = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_lectura_actual = this.w_password_lectura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_lectura_actual.equals(password_lectura);
        if (password_valida){
            this.lock_grupo_b.quieroLeer();
                tamano_del_paquete = this.b_tamano_paquetes.get(id_de_lectura);
            this.lock_grupo_b.dejoDeLeer();
            if (tamano_del_paquete == null){
                tamano_del_paquete = INFO_ID_NO_EXISTE;
                this.log("leyendoFicheroDelServidorTamanoDelPaquete. INFO_ID_NO_EXISTE. (tamano_del_paquete == null). No existe o no es de lectura. "  + id_de_lectura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }else{
                this.log("leyendoFicheroDelServidorTamanoDelPaquete. Tamano del paquete para el id " + id_de_lectura + " es: " + tamano_del_paquete, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            }/*if*/
        }else if( !password_valida ){
            tamano_del_paquete = INFO_PASSWORD_INVALIDA;
            this.log("leyendoFicheroDelServidorTamanoDelPaquete. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return tamano_del_paquete;
    }/*END leyendoFicheroDelServidorTamanoDelPaquete*/

    
    /**
     * Devuelve la cantidad de paquetes que componen el fichero.
     * Resultados:                                                                                              <br>
     *    INFO_ID_NO_EXISTE         El id_de_lectura no existe.                                                 <br>
     *    INFO_PASSWORD_INVALIDA    La contraseña de lectura suministrada no es valida                          <br> 
     * 
     * @param  password_lectura Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  id_de_lectura    El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return int              Cantidad de paquetes de los que se componen el fichero. -21 el id_de_lectura no existe.
     */
    @Override
    public int leyendoFicheroDelServidorNumeroDePaquetes (String password_lectura, String id_de_lectura){
    /*VAR*/
        Integer  cantidad_de_paquetes        =     0;
        String   password_lectura_actual     =    "";
        boolean  password_valida             = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_lectura_actual = this.w_password_lectura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_lectura_actual.equals(password_lectura);
        if (password_valida){
            this.lock_grupo_b.quieroLeer();
                cantidad_de_paquetes = this.b_cantidad_paquetes.get(id_de_lectura);
            this.lock_grupo_b.dejoDeLeer();
            if (cantidad_de_paquetes == null){
                cantidad_de_paquetes = INFO_ID_NO_EXISTE;
                this.log("leyendoFicheroDelServidorNumeroDePaquetes. INFO_ID_NO_EXISTE. (cantidad_de_paquetes == null). No existe o no es de lectura. "  + id_de_lectura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }else{
                this.log("leyendoFicheroDelServidorNumeroDePaquetes. Numero de paquetes para el id " + id_de_lectura + " es: " + cantidad_de_paquetes, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            }/*if*/
        }else if( !password_valida ){
            cantidad_de_paquetes = INFO_PASSWORD_INVALIDA;
            this.log("leyendoFicheroDelServidorNumeroDePaquetes. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return cantidad_de_paquetes;
    }/*END leyendoFicheroDelServidorNumeroDePaquetes*/
    
        
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
     */
    @Override
    public int leyendoFicheroDelServidorBytesDelPaquete(String password_lectura, String id_de_lectura, int num_del_paquete){
    /*VAR*/
        List<FicheroPaquete>  lista_de_fichero_paquete   =  null;
        FicheroPaquete        fichero_paquete_aux        =  null;
        String                password_lectura_actual    =    "";
        int                   bytes_del_paquete          =     0;
        boolean               password_valida            = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_lectura_actual = this.w_password_lectura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_lectura_actual.equals(password_lectura);
        if (password_valida){
            this.lock_grupo_b.quieroLeer();
                lista_de_fichero_paquete = this.b_lecturas.get(id_de_lectura);                     //Si existe el id no devolvera null. Pero LecturaFinalizada podria haberle adelantado y haber borrado.                        
            this.lock_grupo_b.dejoDeLeer();
            if (lista_de_fichero_paquete != null){                                                 //No es necesario control de acceso, se tiene una referencia a algo que no va a ser modificado
                fichero_paquete_aux = lista_de_fichero_paquete.get(num_del_paquete);
                bytes_del_paquete   = fichero_paquete_aux.getCantidadDeBytesValidos();
                this.log("leyendoFicheroDelServidorBytesDelPaquete. Bytes del paquete numero "  + num_del_paquete + " para el id " + id_de_lectura + " es: " + bytes_del_paquete, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            }else{
                bytes_del_paquete = INFO_ID_NO_EXISTE;
                this.log("leyendoFicheroDelServidorBytesDelPaquete. INFO_ID_NO_EXISTE. (lista_de_fichero_paquete == null). No existe o no es de lectura. "  + id_de_lectura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
        }else if( !password_valida ){
            bytes_del_paquete = INFO_PASSWORD_INVALIDA;
            this.log("leyendoFicheroDelServidorBytesDelPaquete. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return bytes_del_paquete;
    }/*END leyendoFicheroDelServidorBytesDelPaquete*/
    

    /**
     * Para informar al servidor que ya a leido todos los paquetes y que puede liberar todos los recursos asociados a la cumunicacion.                                         <br>
     * Resultados:                                                                                                                                                             <br>
     *                              Numero positivo da una idea de los lectores que quedan. Aproximado porque otros hilos pueden haber modificado el valor                     <br>
     *    ERROR_SIN_ERRORES         Cero indica que ya no quedan lectores.                                                                                                     <br>
     *    INFO_ID_NO_EXISTE         No existe el id_de_lectura                                                                                                                 <br>
     *    INFO_PASSWORD_INVALIDA    La contraseña de lectura suministrada no es valida                                                                                         <br> 
     *                                                                                                                                                                         <br>
     * Casi todo se hace dentro del bloque 'synchronized'                                                                                                                      <br>
     * Salir permitiria la entrada de nuevos lectores.                                                                                                                         <br>
     * Si se elimina el contenido del grupo a primero un nuevo hilo podria volver a crearlo.                                                                                   <br>
     * Si se deja para el final el grupo b podria borrar lo que un nuevo hilo creara pues al crear el grupo a se crea lo del b                                                 <br>
     * Este ultimo caso se da si hilo 1 primero se borra grupo a, luego hilo 2 crea grupo a, luego hilo 2 sobrescribe grupo b y por ultimo hilo 1 borra grupo b.               <br>
     * No hay riesgo de bloqueo si se hacen bien las llamadas porque 'quieroEscribir' hasta podria quitarse debido a que se accede cuando ya no hay lectores.                  <br>
     * Incluso haciendolo mal e invocando un metodo cuando ya se dijo que habia terminado de leer no hay peligro de bloqueo al no haber anidamientos inversos como el de aqui. <br>
     * 
     * @param  password_lectura  Se debe enviar la contraseña de lectura para crear una sesion para leer un fichero.
     * @param  id_de_lectura     El identificador que creo 'solicitarLeerFicheroDelServidor' antes de empezar la comunicacion 
     * @return int               Un numero que indica el resultado de la operacion.
     */
    @Override
    public int leyendoFicheroDelServidorLecturaFinalizada (String password_lectura, String id_de_lectura){
    /*VAR*/
        Semaphore  hotel_dormir               =  null;        
        Integer    lector_o_escritor          =  null;
        String     password_lectura_actual    =    "";
        int        dormidos_actuales          =     0;
        int        resultado                  =     0;
        int        i                          =     0;
        boolean    password_valida            = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_lectura_actual = this.w_password_lectura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_lectura_actual.equals(password_lectura);
        if (password_valida){
            this.lock_grupo_a.quieroEscribir();
                lector_o_escritor = this.a_lector_o_escritor.get(id_de_lectura);
                if ( (lector_o_escritor != null) && (lector_o_escritor > 0) ){                         //Si es null es que no hay id. Y si no es null debe ser de lectura.
                    lector_o_escritor--;                                                               //Ahora hay un lector menos. Si se hace cero entonces se deberan borrar las referencias.
                     if (lector_o_escritor == 0){                                                      //Si ya no quedan lectores se hace limpieza, se eliminan todas las referencias que es lo que permitira crear escritores.
                        hotel_dormir      = this.a_hoteles_dormir.get      (id_de_lectura);
                        dormidos_actuales = this.a_cantidad_durmientes.get (id_de_lectura);
                        for (i=0; i<dormidos_actuales; i++){                                           //Debe despertar a todos los que haya a la espera antes de eliminar la referencia del semaphore del Map. Solo habra escritores esperando pues a los lectores se les dejaba pasar.
                            hotel_dormir.release();                                                    //Si controlador_de_accesos_aux no es null entonces el Map de hoteles_dormir tampoco sera null. Se fuerno a dormir en los metodos de solicitar. Una vez despiertos se bloquearan en lock_a.
                        }/*for*/
                        this.a_lector_o_escritor.remove      (id_de_lectura);
                        this.a_hoteles_dormir.remove         (id_de_lectura);                          //Una vez que estan despierto se detruye el hotel. Si hace falta que se vuelva a crear.
                        this.a_cantidad_durmientes.remove    (id_de_lectura);

                        this.lock_grupo_b.quieroEscribir();
                            this.b_lecturas.remove               (id_de_lectura);
                            this.b_lectores_por_id.remove        (id_de_lectura);
                            this.b_hora_de_inicio_lectura.remove (id_de_lectura);
                            this.b_lectores_actuales_totales--;
                        this.lock_grupo_b.dejoDeEscribir();
                    }else{
                        this.a_lector_o_escritor.put(id_de_lectura, lector_o_escritor);                //Aun quedan lectores leyendo, simplemente se resta uno.
                        this.lock_grupo_b.quieroEscribir();
                            this.b_lectores_por_id.put(id_de_lectura, lector_o_escritor);              //Aun quedan lectores leyendo, simplemente se resta uno a los lectores de ese id.
                            this.b_lectores_actuales_totales--;                                        //Y tambien se resta uno a los lectores totales.
                        this.lock_grupo_b.dejoDeEscribir();
                    }/*if*/
                    resultado = lector_o_escritor;
                    this.log("leyendoFicheroDelServidorLecturaFinalizada. Lectura finalizada para el id " + id_de_lectura + ". Quedan " + resultado + " aun leyendo.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);                    
                }else{
                    resultado = INFO_ID_NO_EXISTE;                                                    //No existe el id enviado
                    this.log("leyendoFicheroDelServidorLecturaFinalizada. INFO_ID_NO_EXISTE. ( (lector_o_escritor == null) || (lector_o_escritor > 0) ). No existe o no es de lectura. "  + id_de_lectura, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                }/*if*/
            this.lock_grupo_a.dejoDeEscribir();
        }else if( !password_valida ){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("leyendoFicheroDelServidorLecturaFinalizada. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END leyendoFicheroDelServidorLecturaFinalizada*/
    
    
//------------------------------------------------------------------------------
    
    /**
     * Borra un fichero que este almacenado en el servidor.                                                                                <br>
     * Se devuelve un codigo informando del resultado.                                                                                     <br>
     *    ERROR_SIN_ERRORES                                     Operacion realizada correctamente.                                         <br>
     *    INFO_FICHERO_NO_ENCONTRADO                            Fichero no encontrado.                                                     <br>
     *    WARNING_DURANTE_LA_ESPERA                             Mientras se esperaba a que quedara libre el fichero se produjo un error.   <br>
     *    INFO_ACCESO_NO_PERMITIDO                              No se permite realizar la operacion, se han bloqueado los accesos.         <br>
     *    INFO_PASSWORD_INVALIDA                                La contraseña de escritura suministrada no es valida                       <br>
     *    ERROR_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO  No debe estar el fichero en disco a la vez que esta marcado como borrado.  <br>
     *
     * @param  password_escritura    Se debe enviar la contraseña de escritura para crear una sesion para escribir un fichero.        
     * @param  nombre_directorio     Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros. Sin barra al principio para indicar ruta relativa y con barra final
     * @param  nombre_fichero        El nombre del fichero. 
     * @return int                   Codigo para explicar el resultado de la operacion
     */
    @Override
    public int borrarFicheroDelServidor (String password_escritura, String nombre_directorio, String nombre_fichero){
    /*VAR*/
        FicheroInfo   fichero_info               =  null;
        Semaphore     hotel_dormir               =  null;        
        String        id_escritura_lectura       =    "";
        String        password_escritura_actual  =    "";
        Integer       lector_o_escritor          =     0;
        int           resultado                  =     0;
        int           dormidos_actuales          =     0;
        boolean       hay_que_dormir             = false;
        boolean       existe_fichero             = false;
        boolean       fue_borrado                = false;
        boolean       password_valida            = false;
        boolean       acceso_permitido           = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_escritura_actual = this.w_password_escritura;
            acceso_permitido          = this.w_acceso_permitido_escritura;
        this.lock_grupo_w.dejoDeLeer();
        
        password_valida = password_escritura_actual.equals(password_escritura);
        if ( acceso_permitido &&  password_valida){
            this.lock_grupo_c.quieroEscribir();
                this.c_borrando++;                                                                                               //Para indicar que hay uno mas realizando una operacion de borrado.
            this.lock_grupo_c.dejoDeEscribir();
            
            id_escritura_lectura = this.idParaFichero(nombre_directorio, nombre_fichero);
            while ( !fue_borrado && (resultado == 0) ){
                hay_que_dormir = false;
                this.lock_grupo_a.quieroEscribir();                                                                              //Deben ser de escritura para que no puedan realizarse dos llamadas a este metodo con el mismo fichero.
                    this.lock_grupo_z.quieroLeer();
                        fichero_info = this.z_ficheros_del_almacen.get(id_escritura_lectura);
                        if (fichero_info != null){
                            fue_borrado = fichero_info.getBorrado();
                        }/*if*/
                    this.lock_grupo_z.dejoDeLeer();
                
                    existe_fichero = FicherosTrabajar.exiteFichero(this.directorio_base + nombre_directorio, nombre_fichero);
                    if ( existe_fichero && !fue_borrado && (fichero_info != null) ){                                             //Existe el fichero en disco, no ha sido borrado y se confirma que fichero_borrado es false porque se cogio de fichero_info y no del valor por defecto.
                        lector_o_escritor = this.a_lector_o_escritor.get(id_escritura_lectura);                                  //Si hay controlador se debe comprobar que sea de escritura
                        if (lector_o_escritor == null){                                                                          //Si nadie esta accediendo puede borrar.
                            fue_borrado = FicherosTrabajar.borrarFichero(this.directorio_base + nombre_directorio, nombre_fichero);            //Mejor borrarlo aqui por si al salir de lock_a otro hilo decide usarlo. Aunque si se creasen las varibles para los Maps como en solicitarEscribir ya no podrían usarlo y se irían a dormir.
                            if (fue_borrado){
                                this.lock_grupo_z.quieroEscribir();
                                    fichero_info.setBorrado(true);
//                                  this.z_ficheros_del_almacen.put(id_escritura_lectura, fichero_info);                             //No hace falta hacer put debido a que se modifica la referencia.
                                this.lock_grupo_z.dejoDeEscribir();
                                this.log("borrarFicheroDelServidor. Fichero borrado: " + nombre_directorio+nombre_fichero, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                            }else{
                                this.log("borrarFicheroDelServidor. INFO_FICHERO_EN_USO. "  + nombre_directorio + nombre_fichero, "", MOSTAR_SERVIDOR[2-1], MOSTAR_FECHA[2-1], 2);
                                resultado = INFO_FICHERO_EN_USO;                                                                 //No deberia ¿?
                            }/*if*/
                        }else{                                                                                                   //No puede borrar pues esta siendo enviado o leido
                            hay_que_dormir    = true;                                                                            //Si existe un id entonces cuando se creo tambien se creo el hotel
                            hotel_dormir      = this.a_hoteles_dormir.get      (id_escritura_lectura);                           //Si controlador_de_accesos_aux no es null hoteles_dormir tampoco lo seran.                    
                            dormidos_actuales = this.a_cantidad_durmientes.get (id_escritura_lectura);
                            this.a_cantidad_durmientes.put (id_escritura_lectura, dormidos_actuales+1);                          //EnvioFinalizado debe despertar a todos los que esten antes de eliminar la referencia del Map hoteles_dormir.
                        }/*if*/
                    }else if (existe_fichero && fue_borrado){
                        resultado = ERROR_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO ;
                        this.log("borrarFicheroDelServidor. ERROR_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO . ", "", MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    }else{
                        resultado = INFO_FICHERO_NO_ENCONTRADO;
                        this.log("borrarFicheroDelServidor. INFO_FICHERO_NO_ENCONTRADO. "  + nombre_directorio + nombre_fichero, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                    }/*if*/
                this.lock_grupo_a.dejoDeEscribir();
            
                if (hay_que_dormir){                                                                                             //Espera fuera del synchronized para no bloquear. Se busca que el despertar solo sea para los hilos que lo necesitan, despertar a hilos de otro id es sobrecargar.
                    try{
                        hotel_dormir.acquire();                                                                                  //Puede que EnvioFinalizado, aunque ejecute su bloque lock_a despues, llegase a hacer el notify primero. Es por esto que no se puede usar wait-notify y hace falta llevar la cuenta de los notify que se hacen usando un semaphore
                    }catch (InterruptedException e){
                        resultado = WARNING_DURANTE_LA_ESPERA;
                        this.log("borrarFicheroDelServidor. WARNING_DURANTE_LA_ESPERA. ", "", MOSTAR_SERVIDOR[2-1], MOSTAR_FECHA[2-1], 2);
                    }/*try-catch*/
                }/*if*/
            }/*while*/
            
            this.lock_grupo_c.quieroEscribir();                                                                                  //Deja de escribir.
                this.c_borrando--;
            this.lock_grupo_c.dejoDeEscribir();
        }else if ( !acceso_permitido ){
            resultado = INFO_ACCESO_NO_PERMITIDO;
            this.log("borrarFicheroDelServidor. INFO_ACCESO_NO_PERMITIDO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if( !password_valida ){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("borrarFicheroDelServidor. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END borrarFicheroDelServidor*/
    

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
     *         INFO_PASSWORD_INVALIDA     La contraseña de escritura suministrada no es valida                                                 <br>
     * 
     * @param  password_lectura    Se debe enviar la contraseña de lectura para realizar la pregunta.
     * @param  nombre_directorio   Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros. Sin barra al principio para indicar ruta relativa y con barra final
     * @param  nombre_fichero      El nombre del fichero. 
     * @return int                 Codigo para explicar el resultado de la operacion 
     */
    @Override
    public int existeFicheroEnServidor (String password_lectura, String nombre_directorio, String nombre_fichero){
    /*VAR*/
        String        password_lectura_actual    =    "";
        int           resultado                  =     0;
        boolean       existe_fichero             = false;
        boolean       password_valida            = false;
        boolean       acceso_permitido           = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_lectura_actual = this.w_password_lectura;
            acceso_permitido        = this.w_acceso_permitido_lectura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_lectura_actual.equals(password_lectura);
        if ( acceso_permitido &&  password_valida){
            existe_fichero = FicherosTrabajar.exiteFichero(this.directorio_base + nombre_directorio, nombre_fichero);
            this.log("existeFicheroEnServidor. Existe fichero: " + nombre_directorio+nombre_fichero + " : " + existe_fichero, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            if (existe_fichero){
                resultado = 1;
            }else{
                resultado = 0;
            }/*if*/
        }else if ( !acceso_permitido ){
            resultado = INFO_ACCESO_NO_PERMITIDO;
            this.log("existeFicheroEnServidor. INFO_ACCESO_NO_PERMITIDO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if( !password_valida ){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("existeFicheroEnServidor. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END existeFicheroEnServidor*/
    
    
//--- Grupo W ------------------------------------------------------------------
    
    /**
     * Actualiza el valor de la contraseña de lectura
     * 
     * @param  password_administracion  Se debe proporcionar la contraseña de administracion para realizar esta tarea.
     * @param  nueva_password_lectura   La nueva contraseña de lectura.
     * @return int                      El resultado de la operacion. Cero si todo va bien, INFO_PASSWORD_INVALIDA  si la contraseña de administracion no es corecta.
     */
    @Override
    public int adminCambiarPasswordLectura(String password_administracion, String nueva_password_lectura){
    /*VAR*/
        int resultado = 0;
    /*BEGIN*/
        this.lock_grupo_w.quieroEscribir();
            if ( this.w_password_administracion.equals(password_administracion) ){
                this.w_password_lectura = nueva_password_lectura;
                this.log("adminCambiarPasswordLectura. Password de lectura actualizada.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            }else{
                resultado = INFO_PASSWORD_INVALIDA;
                this.log("adminCambiarPasswordLectura. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
        this.lock_grupo_w.dejoDeEscribir();
        return resultado;
    }/*END adminCambiarPasswordLectura*/
    
    
    /**
     * Actualiza el valor de la contraseña de escritura
     * 
     * @param  password_administracion   Se debe proporcionar la contraseña de administracion para realizar esta tarea.
     * @param  nueva_password_escritura  La nueva contraseña de escritura.
     * @return int                       El resultado de la operacion. Cero si todo va bien, INFO_PASSWORD_INVALIDA  si la contraseña de administracion no es corecta.
     */
    @Override
    public int adminCambiarPasswordEscritura(String password_administracion, String nueva_password_escritura){
    /*VAR*/
        int resultado = 0;
    /*BEGIN*/
        this.lock_grupo_w.quieroEscribir();
            if ( this.w_password_administracion.equals(password_administracion) ){
                this.w_password_escritura = nueva_password_escritura;
                this.log("adminCambiarPasswordEscritura. Password de escritura actualizada.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            }else{
                resultado = INFO_PASSWORD_INVALIDA;
                this.log("adminCambiarPasswordEscritura. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
        this.lock_grupo_w.dejoDeEscribir();
        return resultado;
    }/*END adminCambiarPasswordEscritura*/


    /**
     * Actualiza el valor de la contraseña de procesos
     * 
     * @param  password_administracion   Se debe proporcionar la contraseña de administracion para realizar esta tarea.
     * @param  nueva_password_procesos   La nueva contraseña de procesos.
     * @return int                       El resultado de la operacion. Cero si todo va bien, INFO_PASSWORD_INVALIDA  si la contraseña de administracion no es corecta.
     */
    @Override
    public int adminCambiarPasswordProceso(String password_administracion, String nueva_password_procesos){
    /*VAR*/
        int resultado = 0;
    /*BEGIN*/
        this.lock_grupo_w.quieroEscribir();
            if ( this.w_password_administracion.equals(password_administracion) ){
                this.w_password_procesos = nueva_password_procesos;
                this.log("adminCambiarPasswordProceso. Password de procesos actualizada.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            }else{
                resultado = INFO_PASSWORD_INVALIDA;
                this.log("adminCambiarPasswordProceso. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
        this.lock_grupo_w.dejoDeEscribir();
        return resultado;
    }/*END adminCambiarPasswordProceso*/

    
    /**
     * Actualiza el valor de la contraseña de administracion
     * 
     * @param  password_administracion_antigua  Se debe proporcionar la contraseña de administracion para realizar esta tarea.
     * @param  password_administracion_nueva    La nueva contraseña de administracion.
     * @return int                              El resultado de la operacion. Cero si todo va bien, INFO_PASSWORD_INVALIDA  si la contraseña de administracion antigua no es corecta.
     */
    @Override
    public int adminCambiarPasswordAdministracion(String password_administracion_antigua, String password_administracion_nueva){
    /*VAR*/
        int resultado = 0;
    /*BEGIN*/
        this.lock_grupo_w.quieroEscribir();
            if ( this.w_password_administracion.equals(password_administracion_antigua) ){
                this.w_password_administracion = password_administracion_nueva;
                this.log("adminCambiarPasswordAdministracion. Password de administracion actualizada.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            }else{
                resultado = INFO_PASSWORD_INVALIDA;
                this.log("adminCambiarPasswordAdministracion. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
        this.lock_grupo_w.dejoDeEscribir();
        return resultado;
    }/*END adminCambiarPasswordAdministracion*/    
    
    
    /**
     * Para permitir el acceso o no a los metodos que crean nuevas sesiones.                                <br>
     * Si no se permite el acceso no se podran crear nuevas lecturas o escrituras asi como borrar archivos. <br>
     * Su finalidad es la de apagar el servidor dejando que terminen las tareas en curso.                   <br>
     * 
     * @param  password_administracion                 Se debe proporcionar la contraseña de administracion para realizar esta tarea.
     * @param  valor_acceso_permitido_lectura          true permitira el acceso para crear sesiones de lectura,   false no permitira el acceso para crear sesiones de lectura.   Grupo B.
     * @param  valor_acceso_permitido_escritura        true permitira el acceso para crear sesiones de escritura, false no permitira el acceso para crear sesiones de escritura. Grupo C.
     * @param  valor_acceso_permitido_conexion_pasiva  true permitira aceptar conexiones pasivas,                 false no permitira aceptar conexiones pasivas.                 Grupo Y.
     * @param  valor_acceso_permitido_sincronizar      true permitira iniciar procesos de sincronizacion,         false no permitira iniciar procesos de sincronización.         Grupo Z.
     * @return int                                     El resultado de la operacion. Cero si todo va bien, INFO_PASSWORD_INVALIDA  si la contraseña de administracion antigua no es corecta.
     */
    @Override
    public int adminCambiarAccesoPermitido(String password_administracion, boolean valor_acceso_permitido_lectura, boolean valor_acceso_permitido_escritura, boolean valor_acceso_permitido_conexion_pasiva, boolean valor_acceso_permitido_sincronizar){
    /*VAR*/
        int resultado = 0;
    /*BEGIN*/
        this.lock_grupo_w.quieroEscribir();
            if ( this.w_password_administracion.equals(password_administracion) ){
                this.w_acceso_permitido_lectura         = valor_acceso_permitido_lectura;
                this.w_acceso_permitido_escritura       = valor_acceso_permitido_escritura;
                this.w_acceso_permitido_conexion_pasiva = valor_acceso_permitido_conexion_pasiva;
                this.w_acceso_permitido_sincronizar     = valor_acceso_permitido_sincronizar;
                this.log("adminCambiarAccesoPermitido. Accesos actualizados. Lectura: " + valor_acceso_permitido_lectura + ". Escritura: " + valor_acceso_permitido_escritura + ". Conexion Pasiva: " + valor_acceso_permitido_conexion_pasiva + ". Sincronizar: " + valor_acceso_permitido_sincronizar + ".", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            }else{
                resultado = INFO_PASSWORD_INVALIDA;
                this.log("adminCambiarAccesoPermitido. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            }/*if*/
        this.lock_grupo_w.dejoDeEscribir();
        return resultado;
    }/*END adminCambiarAccesoPermitido*/
    
    
//--- Grupo X ------------------------------------------------------------------

    
    /**
     * Un apagado controlado del servidor consta de las siguientes tareas:                                                                   <br>
     *    Finalizar los hilos run de las tareas periodicas.                                                                                  <br>
     *    Cerrar el acceso para que no se creen mas escrituras, lecturas, sincronizaciones, etc.                                             <br>
     *    Forzar el fin de la sincronizacion pasiva.                                                                                         <br>
     *    Esperar a que terminen las lecturas y escrituras activas.                                                                          <br>
     *    Esperar a que terminen los procesos del run()                                                                                      <br>
     *    Ejecutar activamente la sincronizacion para propagar las ultimas escrituras.                                                       <br>
     *    Informar a otros servidores. Opcional debido al procedimiento de comprobarConexiones                                               <br>
     *    Desregistrar el objeto remoto de rmiregistry.                                                                                      <br>
     *    Almacena las versiones de los ficheros, FicheroInfo.getVersion(), de todos los ficheros del servidor, this.z_ficheros_del_almacen  <br>
     * Cada procedimiento llamado debe hacer su control de acceso a las variables.                                                           <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para apagar el servidor.
     * @return int                      Cero si todo bien. INFO_PASSWORD_INVALIDA  si la contraseña enviada no es valida.
     */
    @Override
    public int apagar(String password_administracion){
    /*VAR*/
        String  password_administracion_acutal =    "";
        String  password_procesos              =    "";
        int     resultado                      =     0;
        boolean password_valida                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_acutal = this.w_password_administracion;
            password_procesos              = this.w_password_procesos;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_administracion_acutal.equals(password_administracion);
        if (password_valida){
            this.log("apagar. Inicio. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
            this.finalizarHiloRun                   (password_administracion);                                 this.log("APAGANDO: Ejecutado finalizarHiloRun. "                  , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            this.adminCambiarAccesoPermitido        (password_administracion, false, false, false, false);     this.log("APAGANDO: Ejecutado adminCambiarAccesoPermitido. "       , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            this.sincronizarInformarFinalizar       (password_administracion);                                 this.log("APAGANDO: Ejecutado sincronizarInformarFinalizar. "      , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            this.esperarFinalizarLecturasEscrituras ();                                                        this.log("APAGANDO: Ejecutado esperarFinalizarLecturasEscrituras. ", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            this.esperarFinalizarRun                ();                                                        this.log("APAGANDO: Ejecutado esperarFinalizarRun. "               , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            this.ejecutarAhoraSincronizarFicheros   (password_procesos);                                       this.log("APAGANDO: Ejecutado ejecutarAhoraSincronizarFicheros. "  , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            this.informarAlmacenes                  (password_administracion);                                 this.log("APAGANDO: Ejecutado informarAlmacenes. "                 , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            this.unbindRmiRegistry                  ();                                                        this.log("APAGANDO: Ejecutado unbindRmiRegistry. "                 , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            this.almacenarVersiones                 ();                                                        this.log("APAGANDO: Ejecutado almacenarVersiones. "                , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);            
            this.log("apagar. Fin.", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
//          System.exit(resultado);
        }else{
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("apagar. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END apagar*/

    
    /**
     * Da por finalizada la ejecucion del hilo run y no se puede volver a activar, sería necesario volver a crearlo
     * 
     * @param  password_administracion    Se debe enviar la contraseña de administracion para finalizar a run()
     * @return int                        Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     */
    @Override
    public int finalizarHiloRun(String password_administracion){
    /*VAR*/
        String  password_administracion_acutal =    "";
        int     resultado                      =     0;
        boolean password_valida                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_acutal = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_administracion_acutal.equals(password_administracion);
        if (password_valida){
            this.lock_grupo_x.quieroEscribir();
                this.x_fin = true;
            this.lock_grupo_x.dejoDeEscribir();
            this.log("finalizarHiloRun. Flags a true para indicar el fin de las tareas de mantenimiento.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else{
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("finalizarHiloRun. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END finalizarHiloRun*/
    
    
    /**
     * Usando el valor del parametro, se deja en pausa o se reativa la ejecucion del hilo.
     * 
     * @param  password_procesos   Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @param  valor_pausa         true para indicar que debe estar en pausa. false indica que se puede ejecutar el hilo
     * @return int                 Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     */
    @Override
    public int pausaHiloRun(String password_procesos, boolean valor_pausa){
    /*VAR*/
        String  password_procesos_acutal =    "";
        int     resultado                =     0;
        boolean password_valida          = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_procesos_acutal = this.w_password_procesos;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_procesos_acutal.equals(password_procesos);
        if (password_valida){
            this.lock_grupo_x.quieroEscribir();
                this.x_pausa = valor_pausa;
            this.lock_grupo_x.dejoDeEscribir();
            this.log("pausaHiloRun. Flags a true para indicar el pausa de las tareas de mantenimiento.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else{
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("pausaHiloRun. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END pausaHiloRun*/

    
    /**
     * Dice si se debe ejecutar el proceso de Caducar Sesiones
     * 
     * @param  password_procesos      Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @param  valor_caducar_sesiones true permite ejecutar el proceso, false impide que se ejecute el proceso. 
     * @return int                    Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     */
    @Override
    public int permitirEjecutarCaducarSesiones(String password_procesos, boolean valor_caducar_sesiones){
    /*VAR*/
        String  password_procesos_acutal =    "";
        int     resultado                =     0;
        boolean password_valida          = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_procesos_acutal = this.w_password_procesos;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_procesos_acutal.equals(password_procesos);
        if (password_valida){
            this.lock_grupo_x.quieroEscribir();
                this.x_ejecutar_caducar_sesiones = valor_caducar_sesiones;
            this.lock_grupo_x.dejoDeEscribir();
            this.log("permitirEjecutarCaducarSesiones. Permiso para Caducar Sesiones: " + valor_caducar_sesiones, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else{
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("permitirEjecutarCaducarSesiones. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END permitirEjecutarCaducarSesiones*/

    
    /**
     * Dice si se debe ejecutar el proceso de Comprobar Conexiones
     * 
     * @param  password_procesos          Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @param  valor_comprobar_conexiones true permite ejecutar el proceso, false impide que se ejecute el proceso. 
     * @return int                        Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     */
    @Override
    public int permitirEjecutarComprobarConexiones(String password_procesos, boolean valor_comprobar_conexiones){
    /*VAR*/
        String  password_procesos_acutal =    "";
        int     resultado                =     0;
        boolean password_valida          = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_procesos_acutal = this.w_password_procesos;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_procesos_acutal.equals(password_procesos);
        if (password_valida){
            this.lock_grupo_x.quieroEscribir();
                this.x_ejecutar_comprobar_conexiones = valor_comprobar_conexiones;
            this.lock_grupo_x.dejoDeEscribir();
            this.log("permitirEjecutarComprobarConexiones. Permiso para Comprobar Conexiones: " + valor_comprobar_conexiones, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else{
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("permitirEjecutarComprobarConexiones. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END permitirEjecutarComprobarConexiones*/

    
    /**
     * Dice si se debe ejecutar el proceso de Sincronizar Ficheros.
     * 
     * @param  password_procesos          Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @param  valor_sincronizar_ficheros true permite ejecutar el proceso, false impide que se ejecute el proceso. 
     * @return int                        Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     */
    @Override
    public int permitirEjecutarSincronizarFicheros(String password_procesos, boolean valor_sincronizar_ficheros){
    /*VAR*/
        String  password_procesos_acutal =    "";
        int     resultado                =     0;
        boolean password_valida          = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_procesos_acutal = this.w_password_procesos;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_procesos_acutal.equals(password_procesos);
        if (password_valida){
            this.lock_grupo_x.quieroEscribir();
                this.x_ejecutar_sincronizar_ficheros = valor_sincronizar_ficheros;
            this.lock_grupo_x.dejoDeEscribir();        
            this.log("permitirEjecutarSincronizarFicheros. Permiso para Sincronizar Ficheros: " + valor_sincronizar_ficheros, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else{
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("permitirEjecutarSincronizarFicheros. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END permitirEjecutarSincronizarFicheros*/
    
    
    /**
     * Una forma manual de ejecutar el proceso de Caducar Sesiones.
     * 
     * @param  password_procesos  Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @return int                Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida. INFO_ACCESO_NO_PERMITIDO  si no hay permiso para ejecutar el proceso.
     */
    @Override
    public int ejecutarAhoraCaducarSesiones(String password_procesos){
    /*VAR*/
        String  password_procesos_acutal =    "";
        int     resultado                =     0;
        boolean password_valida          = false;
        boolean hay_permiso              = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_procesos_acutal = this.w_password_procesos;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_x.quieroLeer();
            hay_permiso = this.x_ejecutar_caducar_sesiones;
        this.lock_grupo_x.dejoDeLeer();
        
        password_valida = password_procesos_acutal.equals(password_procesos);
        if (password_valida && hay_permiso){
            this.caducarSesiones();
            this.log("ejecutarAhoraCaducarSesiones. Ejecutada la tarea de Caducar Sesiones.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("ejecutarAhoraCaducarSesiones. INFO_PASSWORD_INVALIDA. "  , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!hay_permiso){
            resultado = INFO_ACCESO_NO_PERMITIDO;
            this.log("ejecutarAhoraCaducarSesiones. INFO_ACCESO_NO_PERMITIDO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END ejecutarAhoraCaducarSesiones*/


    /**
     * Una forma manual de ejecutar el proceso de Comprobar Conexiones.
     * 
     * @param  password_procesos  Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @return int                Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida. INFO_ACCESO_NO_PERMITIDO  si no hay permiso para ejecutar el proceso.
     */
    @Override
    public int ejecutarAhoraComprobarConexiones(String password_procesos){
    /*VAR*/
        String  password_procesos_acutal =    "";
        int     resultado                =     0;
        boolean password_valida          = false;
        boolean hay_permiso              = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_procesos_acutal = this.w_password_procesos;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_x.quieroLeer();
            hay_permiso = this.x_ejecutar_comprobar_conexiones;
        this.lock_grupo_x.dejoDeLeer();
        
        password_valida = password_procesos_acutal.equals(password_procesos);
        if (password_valida && hay_permiso){
            this.comprobarConexiones();
            this.log("ejecutarAhoraComprobarConexiones. Ejecutada la tarea de Comprobar Conexiones.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("ejecutarAhoraComprobarConexiones. INFO_PASSWORD_INVALIDA. "  , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!hay_permiso){
            resultado = INFO_ACCESO_NO_PERMITIDO;
            this.log("ejecutarAhoraComprobarConexiones. INFO_ACCESO_NO_PERMITIDO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END ejecutarAhoraComprobarConexiones*/

    
    /**
     * Una forma manual de ejecutar el proceso de SincronizarFicheros.
     * 
     * @param  password_procesos  Se debe enviar la contraseña de procesos para influir en la ejecucion de los procesos.
     * @return int                Cero si no hay problemas. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida. INFO_ACCESO_NO_PERMITIDO  si no hay permiso para ejecutar el proceso.
     */    
    @Override
    public int ejecutarAhoraSincronizarFicheros(String password_procesos){
    /*VAR*/
        String  password_procesos_acutal =    "";
        int     resultado                =     0;
        boolean password_valida          = false;
        boolean hay_permiso              = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_procesos_acutal = this.w_password_procesos;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_x.quieroLeer();
            hay_permiso = this.x_ejecutar_sincronizar_ficheros;
        this.lock_grupo_x.dejoDeLeer();

        password_valida = password_procesos_acutal.equals(password_procesos);
        if (password_valida && hay_permiso){
            this.sincronizarFicheros();
            this.log("ejecutarAhoraSincronizarFicheros. Ejecutada la tarea de Sincronizar Ficheros.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("ejecutarAhoraSincronizarFicheros. INFO_PASSWORD_INVALIDA. "  , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!hay_permiso){
            resultado = INFO_ACCESO_NO_PERMITIDO;
            this.log("ejecutarAhoraSincronizarFicheros. INFO_ACCESO_NO_PERMITIDO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END ejecutarAhoraSincronizarFicheros*/

    
//--- Grupo Y ------------------------------------------------------------------


    /**
     * Devuelve los almacenes conocidos, sus IDs.                                                                                                                          <br>
     * Es el resultado de unir las listas de activos, pasivos                                                                                                              <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @return List<String>             La lista de IDs de los almacenes conocidos.
     */
    @Override
    public List<String>  listaDeAlmacenesOrdenCero(String password_administracion){
    /*VAR*/
        List<String>                        lista_almacenes_conocidos_ids   =  new ArrayList<String>();
        Set<String>                         set_keys                        =  null;
        Iterator                            itr                             =  null;
        String                              password_administracion_acutal  =    "";
        int                                 i                               =     0;
        boolean                             password_valida                 = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_acutal = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_administracion_acutal.equals(password_administracion);
        if (password_valida){
            this.lock_grupo_y.quieroLeer();
                set_keys = this.y_almacenes_orden_0.keySet();
                itr      = set_keys.iterator();
                while ( itr.hasNext() ){
                    lista_almacenes_conocidos_ids.add( (String)itr.next() );
                    i++;
                }/*while*/
            this.lock_grupo_y.dejoDeLeer();
            this.log("listaDeAlmacenesOrdenCero. Enviada la lista de Orden Cero con " + i + " elementos.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else{
//          resultado = INFO_PASSWORD_INVALIDA;
            this.log("listaDeAlmacenesOrdenCero. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return lista_almacenes_conocidos_ids;
    }/*END listaDeAlmacenesOrdenCero*/

    
    /**
     * Devuelve un Almacen dado su ID. Lo busca en las listas de activos y pasivos           <br>
     * Podría no existir el ID aunque se hubiera obtenido de 'listaDeAlmacenesOrdenCero()'   <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @param  id_almacen_remoto        El id de un almacen remoto
     * @return AlmacenInterfaceRemote   El almacen remotos solicitado. null si el id no existe. Podría no existir el ID aunque se hubiera obtenido de 'listaDeAlmacenesConocidos()'
     */
    @Override
    public AlmacenInterfaceRemote almacenOrdenCero(String password_administracion, String id_almacen_remoto){
    /*VAR*/
        AlmacenInterfaceRemote almacen_aux                     =  null;
        String                 password_administracion_acutal  =    "";
        boolean                password_valida                 = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_acutal = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_administracion_acutal.equals(password_administracion);
        if (password_valida){
            this.lock_grupo_y.quieroLeer();
                if ( this.y_almacenes_comunicacion_activa.containsKey(id_almacen_remoto)){
                    almacen_aux = this.y_almacenes_comunicacion_activa.get(id_almacen_remoto);
                }else if ( this.y_almacenes_comunicacion_pasiva.containsKey(id_almacen_remoto)){
                    almacen_aux = this.y_almacenes_comunicacion_pasiva.get(id_almacen_remoto);
                }/*if*/
            this.lock_grupo_y.dejoDeLeer();
            this.log("almacenOrdenCero. Enviado un almacen de orden cero.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else{
//          resultado = INFO_PASSWORD_INVALIDA;
            this.log("almacenOrdenCero. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return almacen_aux;
    }/*END almacenOrdenCero*/

    
    /**
     * Solicita conectarse para sincronizar los fichero.                                    <br>
     * Quedara anotado como servidor pasivo. No podra ser uno que este anotado como activo. <br>
     * 
     * @param   password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @param   solicitante              El almacen que solicita conectarse. 
     * @return  boolean                  true si acepta la conexion, false si no la acepta y debe buscar a otro con quien sincronizarse.
     */
    @Override
    public boolean solicitarConexion(String password_administracion, AlmacenInterfaceRemote solicitante){
    /*VAR*/
        String  id_solicitante                  =    "";
        String  password_administracion_acutal  =    "";
        boolean conexion_aceptada               = false;
        boolean password_valida                 = false;
        boolean acceso_permitido                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_acutal = this.w_password_administracion;
            acceso_permitido               = this.w_acceso_permitido_conexion_pasiva;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_administracion_acutal.equals(password_administracion);
        if (password_valida && acceso_permitido){
             this.lock_grupo_y.quieroEscribir();
             if (this.y_num_comunicaciones_pasivas < this.y_num_max_comunicaciones_pasivas ){
                try {
                    id_solicitante = solicitante.getID();
                    if ( !this.y_almacenes_comunicacion_activa.containsKey(id_solicitante) &&      //Si esta en activa se formaria un ciclo innecesario de sincronizacion.
                         !this.y_almacenes_comunicacion_pasiva.containsKey(id_solicitante)  ){     //Si esta en pasiva es que ya esta. No se le acpeta dos veces.
                        this.anotarAlmacenPasivo       (id_solicitante, solicitante);
                        this.generarAlmacenesOrdenCero ();                                         //Al modificarse la lista de activos o pasivos se debe rehancer orden 0 y orden 1
                        conexion_aceptada = true;                        
                        this.log("solicitarConexion. Conexion pasiva aceptada a: " + id_solicitante, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    }/*if*/
                } catch (RemoteException ex) {
                    this.log("solicitarConexion. Excepcion producida por getID(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    //Se ignora porque no se ha hecho cambios que haya que deshacer. Es como si no se hubiera realizado la peticion. 
                }/*try-catch*/
            }/*if*/
            this.lock_grupo_y.dejoDeEscribir();
            if (conexion_aceptada){
                this.generarAlmacenesOrdenUno(password_administracion);                            //Tiene su propio control de bloqueos 
            }/*if*/
        }else if (!password_valida){
//          resultado = INFO_PASSWORD_INVALIDA;
            this.log("solicitarConexion. INFO_PASSWORD_INVALIDA. "  , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if( !acceso_permitido ){
//          resultado = INFO_ACCESO_NO_PERMITIDO;
            this.log("solicitarConexion. INFO_ACCESO_NO_PERMITIDO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return conexion_aceptada;
    }/*END solicitarConexion*/

    
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
    @Override
    public boolean confirmarConexionActiva(String password_administracion, String  id_solicitante){
    /*VAR*/
        String  password_administracion_acutal  =    "";
        boolean conexion_confirmada             = false;
        boolean password_valida                 = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_acutal = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_administracion_acutal.equals(password_administracion);
        if (password_valida){
            this.lock_grupo_y.quieroLeer();
                if ( this.y_almacenes_comunicacion_activa.containsKey(id_solicitante) ){
                    conexion_confirmada = true;
                    this.log("confirmarConexionActiva. Conexion activa confirmada a: " + id_solicitante, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                }/*if*/
            this.lock_grupo_y.dejoDeLeer();
        }else{
//          resultado = INFO_PASSWORD_INVALIDA;
            this.log("confirmarConexionActiva. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return conexion_confirmada;
    }/*END confirmarConexionActiva*/

    
    /**
     * Debido a posibles cortes de conexion que producen reoganizacion de las listas, un serviror puede descartar una conexion mientras otro no. <br>
     * Con este metodo, el que no descarto la conexion la vuelve a pedir y asi se reorganizan las listas en ambos lados.                         <br>
     * Se comprueba que se tenga como pasivo al solicitante. Si no se tiene se rechaza, no se le intentara poner como pasivo.                    <br>
     * Es decir, solo el llamante debera reajustar sus listas o maps, aqui solo se entra en modo lectura.                                        <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @param  id_solicitante           El id del almacen que informa que tiene con nosotros una conexion establecida
     * @return boolean                  true si confirma que acepta la conexion. false si ahora rechaza la conexion.
     */
    @Override
    public boolean confirmarConexionPasiva(String password_administracion, String  id_solicitante){
    /*VAR*/
        String  password_administracion_acutal  =    "";
        boolean conexion_confirmada             = false;
        boolean password_valida                 = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_acutal = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_administracion_acutal.equals(password_administracion);
        if (password_valida){
            this.lock_grupo_y.quieroLeer();
                if ( this.y_almacenes_comunicacion_pasiva.containsKey(id_solicitante) ){
                    conexion_confirmada = true;
                    this.log("confirmarConexionPasiva. Conexion pasiva confirmada a: " + id_solicitante, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                }/*if*/
            this.lock_grupo_y.dejoDeLeer();
        }else{
//          resultado = INFO_PASSWORD_INVALIDA;
            this.log("confirmarConexionPasiva. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return conexion_confirmada;
    }/*END confirmarConexionPasiva*/

    
    /**
     * Un almacen informa de que se va a apagar o desconectar.                                             <br>
     * De esta forma se le saca de las listas y no dara fallos de conexion al intentar contactar con el.   <br>
     * No se realiza reconexiones, ya se haran en su momento, aqui solo se elimina de los maps.            <br>
     * 
     * @param  password_administracion  Se debe enviar la contraseña de administracion para temas de conexion entre servidores, es una tarea interna que no afecta a clientes.
     * @param  almacen_baja             El almacen que informa que se va a apagar o descnoectar, para que lo saquemos de las listas
     * @return int                      Un codigo resultado de la operacion: INFO_PASSWORD_INVALIDA  si la contraseña no es valida.
     * @throws RemoteException 
     */
    @Override
    public int informandoBaja(String password_administracion, AlmacenInterfaceRemote almacen_baja){
    /*VAR*/
        String  id_solicitante                  =    "";
        String  password_administracion_acutal  =    "";
        int     resultado                       =     0;
        boolean password_valida                 = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_acutal = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        password_valida = password_administracion_acutal.equals(password_administracion);
        if (password_valida){
            this.lock_grupo_y.quieroEscribir();
                try{
                    id_solicitante = almacen_baja.getID();
                    this.borrarAlmacenActivo       (id_solicitante);
                    this.borrarAlmacenPasivo       (id_solicitante);
                    this.generarAlmacenesOrdenCero ();                          //Al modificarse la lista de activos o pasivos se debe rehancer orden 0 y orden 1
                    this.log("informandoBaja. Baja anunciada por: " + id_solicitante, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                }catch (RemoteException ex) {
                    this.log("informandoBaja. RemoteException. Producida por getID(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    //Producida por getID()
                    //Se sale sin haber ninguna tarea. Ya en comprobarConexiones se hara la limpieza por las malas (con excepciones)
                }/*try-catch*/
            this.lock_grupo_y.dejoDeEscribir();
            this.generarAlmacenesOrdenUno(password_administracion);
        }else{
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("informandoBaja. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END informandoBaja*/
    
    
//--- Grupo Z ------------------------------------------------------------------
    
    /**
     * Devuelve todos los fichero que gestiona este almacen.                                                  <br>
     * Ahora mismo devulve una referencia porque se entiende que es para usarlo con rmi que creara una copia. <br>
     * 
     * @param  password_lectura         Se debe enviar la contraseña de lectura para conocer los ficheros del almacén.
     * @return Map<String, FicheroInfo> Una coleccion de los fichero que hay en el almacen con la version y la marca de borrado. null si la contraseña no es valida.
     */
    @Override
    public Map<String, FicheroInfo> ficherosDelAlmacen(String password_lectura){
    /*VAR*/
        Map<String, FicheroInfo> ficheros_del_almacen     =  null;
        String                   password_lectura_actual  =    "";
        boolean                  password_valida          = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_lectura_actual = this.w_password_lectura;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_lectura_actual.equals(password_lectura);
        if (password_valida){
            this.lock_grupo_z.quieroLeer();
                ficheros_del_almacen = this.z_ficheros_del_almacen;
          this.lock_grupo_z.dejoDeLeer();
          this.log("ficherosDelAlmacen. Informacion dada a un solicitante. ", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if( !password_valida ){
//          resultado = INFO_PASSWORD_INVALIDA;
            this.log("ficherosDelAlmacen. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return ficheros_del_almacen;
    }/*END ficherosDelAlmacen*/

    
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
     */
    @Override
    public boolean sincronizarSolicitarIniciar(String password_administracion, String id_servidor){
    /*VAR*/
        String  password_administracion_actual =    "";
        boolean password_valida                = false;
        boolean acceso_permitido               = false;
        boolean esta_en_pasivos                = false;
        boolean se_permite_sincronizacion      = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
            acceso_permitido               = this.w_acceso_permitido_sincronizar;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_y.quieroLeer();
            esta_en_pasivos = this.y_almacenes_comunicacion_pasiva.containsKey(id_servidor);
        this.lock_grupo_y.dejoDeLeer();

        password_valida = password_administracion_actual.equals(password_administracion);
        if (password_valida && acceso_permitido && esta_en_pasivos){
                this.lock_grupo_z.quieroEscribir();
                    if ( !this.z_sincronizacion_aceptada ){
                        this.z_sincronizacion_aceptada    = true;
                        this.z_id_almacen_pasivo          = id_servidor;
                        this.z_hora_de_inicio_sincronizar = new Date();
                        se_permite_sincronizacion         = true;
                        this.log("sincronizarSolicitarIniciar. Sincronizacion de Ficheros aceptada a:  " + id_servidor              + "."  , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    }else{
                        this.log("sincronizarSolicitarIniciar. Sincronizacion de Ficheros rechazada a: " + id_servidor              + "."  , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                        this.log("sincronizarSolicitarIniciar. Sincronizacion en curso con:            " + this.z_id_almacen_pasivo + "."  , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    }/*if*/
                this.lock_grupo_z.dejoDeEscribir();
        }else if( !password_valida ){
//          resultado = INFO_PASSWORD_INVALIDA;
            this.log("sincronizarSolicitarIniciar. INFO_PASSWORD_INVALIDA. "  , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if( !acceso_permitido ){
//          resultado = INFO_ACCESO_NO_PERMITIDO;
            this.log("sincronizarSolicitarIniciar. INFO_ACCESO_NO_PERMITIDO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if( !esta_en_pasivos ){
//          resultado = ;
            this.log("sincronizarSolicitarIniciar. No esta en pasivos. "      , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return se_permite_sincronizacion;
    }/*END sincronizarSolicitarIniciar*/

    
    /**
     * Da por finalizada la sincronizacion. No se permitira el acceso a los metodos de sincronizacion.
     * 
     * @param  password_administracion   Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @return int                       Cero si todo va bien. INFO_PASSWORD_INVALIDA  si la contraseña suministrada no es valida.
     */
    @Override
    public int sincronizarInformarFinalizar(String password_administracion){
    /*VAR*/
        String   password_administracion_actual =    "";
        int      resultado                      =     0;
        boolean  password_valida                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_administracion_actual.equals(password_administracion);
        if (password_valida){
            this.lock_grupo_z.quieroEscribir(); 
                this.z_sincronizacion_aceptada    = false;
                this.z_id_almacen_pasivo          =    "";
                this.z_hora_de_inicio_sincronizar =  null;
                this.log("sincronizarInformarFinalizar. Sincronizacion de Ficheros finalizada."  , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            this.lock_grupo_z.dejoDeEscribir();
        }else{
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("sincronizarInformarFinalizar. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END sincronizarInformarFinalizar*/

    
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
     */
    @Override
    public int sincronizarEliminarDeFicherosDeLaEstructura(String password_administracion, String id_almacen, String id_de_fichero){
    /*VAR*/
        FicheroInfo fich_info                      =  null;
        String      password_administracion_actual =    "";
        int         resultado                      =     0;
        boolean     se_permite                     = false;
        boolean     password_valida                = false;        
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();

        password_valida = password_administracion_actual.equals(password_administracion);
        if (password_valida){
            this.lock_grupo_z.quieroEscribir();
                se_permite = this.z_sincronizacion_aceptada && this.z_id_almacen_pasivo.equals(id_almacen);
                if (se_permite){
                    fich_info = this.z_ficheros_del_almacen.get(id_de_fichero);
                    if ( (fich_info != null) && fich_info.getBorrado() ){
                        this.borrarFicheroDeMiEstructura(id_de_fichero);
                        this.log("sincronizarEliminarDeFicherosDeLaEstructura. Solicitud de: "+ id_almacen + ". Fichero/ID: " + id_de_fichero, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    }/*if*/
                }else{
                    resultado = INFO_SINCRONIZACION_SIN_PERMISO;
                    this.log("sincronizarEliminarDeFicherosDeLaEstructura. INFO_SINCRONIZACION_SIN_PERMISO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
                }/*if*/
            this.lock_grupo_z.dejoDeEscribir();            
        }else{
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("sincronizarEliminarDeFicherosDeLaEstructura. INFO_PASSWORD_INVALIDA. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/            
        return resultado;
    }/*END sincronizarEliminarDeFicherosDeLaEstructura*/


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
     *    WARNING_DURANTE_LA_ESPERA         Mientras se esperaba a que quedara libre el fichero se produjo un error.                                                                        <br>
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
     */
    @Override
    public String sincronizarSolicitarEscribirFicheroEnServidor (String password_administracion, String id_almacen, String nombre_directorio, String nombre_fichero, String fecha_version, int longitud_del_paquete, int cantidad_paquetes){
    /*VAR*/
        String      resultado                      =    "";
        String      password_administracion_actual =    "";
        String      password_escritura             =    "";
        boolean     se_permite                     = false;
        boolean     password_valida                = false;        
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_z.quieroLeer();
            se_permite = this.z_sincronizacion_aceptada && this.z_id_almacen_pasivo.equals(id_almacen);
        this.lock_grupo_z.dejoDeLeer();
        
        password_valida = password_administracion_actual.equals(password_administracion);
        if (password_valida && se_permite){
            this.lock_grupo_w.quieroLeer();
                password_escritura = this.w_password_escritura;
            this.lock_grupo_w.dejoDeLeer();
            resultado = this.solicitarEscribirFicheroEnServidor(password_escritura, nombre_directorio, nombre_fichero, fecha_version, longitud_del_paquete, cantidad_paquetes);
            this.log("sincronizarSolicitarEscribirFicheroEnServidor. Solicitud de: "+ id_almacen + ". Resultado/ID: " + resultado, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
            resultado = "" + INFO_PASSWORD_INVALIDA + "";
            this.log("sincronizarSolicitarEscribirFicheroEnServidor. INFO_PASSWORD_INVALIDA. "         , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!se_permite){
            resultado = "" + INFO_SINCRONIZACION_SIN_PERMISO + "";
            this.log("sincronizarSolicitarEscribirFicheroEnServidor. INFO_SINCRONIZACION_SIN_PERMISO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END sincronizarSolicitarEscribirFicheroEnServidor*/
    
    
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
     */
    @Override
    public int sincronizarEscribiendoFicheroEnServidorEnviandoPaquetes (String password_administracion, String id_almacen, String id_de_escritura, List<FicheroPaquete> lista_paquetes){
    /*VAR*/
        String      password_administracion_actual =    "";
        String      password_escritura             =    "";        
        int         resultado                      =     0;
        boolean     se_permite                     = false;
        boolean     password_valida                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_z.quieroLeer();
            se_permite = this.z_sincronizacion_aceptada && this.z_id_almacen_pasivo.equals(id_almacen);
        this.lock_grupo_z.dejoDeLeer();
        
        password_valida = password_administracion_actual.equals(password_administracion);        
        if (password_valida && se_permite){
            this.lock_grupo_w.quieroLeer();
                password_escritura = this.w_password_escritura;
            this.lock_grupo_w.dejoDeLeer();            
            resultado = this.escribiendoFicheroEnServidorEnviandoPaquetes(password_escritura, id_de_escritura, lista_paquetes);
            this.log("sincronizarEscribiendoFicheroEnServidorEnviandoPaquetes. Solicitud de: "+ id_almacen + ". Resultado: " + resultado, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("sincronizarEscribiendoFicheroEnServidorEnviandoPaquetes. INFO_PASSWORD_INVALIDA. "         , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!se_permite){
            resultado = INFO_SINCRONIZACION_SIN_PERMISO;            
            this.log("sincronizarEscribiendoFicheroEnServidorEnviandoPaquetes. INFO_SINCRONIZACION_SIN_PERMISO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END sincronizarEscribiendoFicheroEnServidorEnviandoPaquetes*/
    
    
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
     */
    @Override
    public int sincronizarEscribiendoFicheroEnServidorEnvioFinalizado(String password_administracion, String id_almacen, String id_de_escritura){
    /*VAR*/
        String      password_administracion_actual =    "";
        String      password_escritura             =    "";
        int         resultado                      =     0;
        boolean     se_permite                     = false;
        boolean     password_valida                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_z.quieroLeer();
            se_permite = this.z_sincronizacion_aceptada && this.z_id_almacen_pasivo.equals(id_almacen);
        this.lock_grupo_z.dejoDeLeer();
        
        password_valida = password_administracion_actual.equals(password_administracion);        
        if (password_valida && se_permite){
            this.lock_grupo_w.quieroLeer();
                password_escritura = this.w_password_escritura;
            this.lock_grupo_w.dejoDeLeer();
            resultado = this.escribiendoFicheroEnServidorEnvioFinalizado(password_escritura, id_de_escritura);
            this.log("sincronizarEscribiendoFicheroEnServidorEnvioFinalizado. Solicitud de: "+ id_almacen + ". Resultado: " + resultado, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("sincronizarEscribiendoFicheroEnServidorEnvioFinalizado. INFO_PASSWORD_INVALIDA. "         , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!se_permite){
            resultado = INFO_SINCRONIZACION_SIN_PERMISO;
            this.log("sincronizarEscribiendoFicheroEnServidorEnvioFinalizado. INFO_SINCRONIZACION_SIN_PERMISO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END sincronizarEscribiendoFicheroEnServidorEnvioFinalizado*/


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
     * @param  password_administracion  Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen               El id del servidor que se esta sincronizando.
     * @param  id_de_escritura          El identificador que creo 'soliciarEnviarFicheroAlServidor' antes de empezar la comunicacion
     * @return int                      Resultado de la operacion.
     */
    @Override
    public int sincronizarEscribiendoFicheroEnServidorCancelar(String password_administracion, String id_almacen, String id_de_escritura){
    /*VAR*/
        String      password_administracion_actual =    "";
        String      password_escritura             =    "";
        int         resultado                      =     0;
        boolean     se_permite                     = false;
        boolean     password_valida                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_z.quieroLeer();
            se_permite = this.z_sincronizacion_aceptada && this.z_id_almacen_pasivo.equals(id_almacen);
        this.lock_grupo_z.dejoDeLeer();
        
        password_valida = password_administracion_actual.equals(password_administracion);        
        if (password_valida && se_permite){
            this.lock_grupo_w.quieroLeer();
                password_escritura = this.w_password_escritura;
            this.lock_grupo_w.dejoDeLeer();
            resultado = this.escribiendoFicheroEnServidorCancelar(password_escritura, id_de_escritura);
            this.log("sincronizarEscribiendoFicheroEnServidorCancelar. Solicitud de: " + id_almacen + ". Resultado: " + resultado, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("sincronizarEscribiendoFicheroEnServidorCancelar. INFO_PASSWORD_INVALIDA. "         , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!se_permite){
            resultado = INFO_SINCRONIZACION_SIN_PERMISO;
            this.log("sincronizarEscribiendoFicheroEnServidorCancelar. INFO_SINCRONIZACION_SIN_PERMISO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END sincronizarEscribiendoFicheroEnServidorCancelar*/

    
    /**
     * Mismo metodo que borrarFicheroDelServidor pero para la sincronizacion.                                                              <br>
     * Es necesario que haya metodos especificos para la sincronizacion debido a que se debe proporcionar el id del servidor.              <br>
     * A los códigos de resultado que devuelve el metodo normal se añade INFO_SINCRONIZACION_SIN_PERMISO .                                 <br>
     *                                                                                                                                     <br>
     * Borra un fichero que este almacenado en el servidor.                                                                                <br>
     * Se devuelve un codigo informando del resultado.                                                                                     <br>
     *    ERROR_SIN_ERRORES                                     Operacion realizada correctamente.                                         <br>
     *    INFO_FICHERO_NO_ENCONTRADO                            Fichero no encontrado.                                                     <br>
     *    WARNING_DURANTE_LA_ESPERA                             Mientras se esperaba a que quedara libre el fichero se produjo un error.   <br>
     *    ERROR_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO  No debe estar el fichero en disco a la vez que esta marcado como borrado.  <br>
     *    INFO_SINCRONIZACION_SIN_PERMISO                       Si el servidor no tiene permiso para sincronizarse.                        <br>
     *    INFO_PASSWORD_INVALIDA                                La contraseña de administracion suministrada no es valida                  <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen                  El id del servidor que se esta sincronizando.
     * @param  nombre_directorio           Sirve para crear una estructura de directorios en el servidor para separarlos de otros ficheros. Sin barra al principio para indicar ruta relativa y con barra final
     * @param  nombre_fichero              El nombre del fichero. 
     * @return int                         Codigo para explicar el resultado de la operacion
     */   
    @Override
    public int sincronizarBorrarFicheroDelServidor(String password_administracion, String id_almacen, String nombre_directorio, String nombre_fichero){
    /*VAR*/
        String      password_administracion_actual =    "";
        String      password_escritura             =    "";
        int         resultado                      =     0;
        boolean     se_permite                     = false;
        boolean     password_valida                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_z.quieroLeer();
            se_permite = this.z_sincronizacion_aceptada && this.z_id_almacen_pasivo.equals(id_almacen);
        this.lock_grupo_z.dejoDeLeer();
        
        password_valida = password_administracion_actual.equals(password_administracion);        
        if (password_valida && se_permite){
            this.lock_grupo_w.quieroLeer();
                password_escritura = this.w_password_escritura;
            this.lock_grupo_w.dejoDeLeer();
            resultado = this.borrarFicheroDelServidor(password_escritura, nombre_directorio, nombre_fichero);
            this.log("sincronizarBorrarFicheroDelServidor. Solicitud de: "+ id_almacen + ". Directorio+Fichero: " + nombre_directorio +  nombre_fichero + ". Resultado: " + resultado, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("sincronizarBorrarFicheroDelServidor. INFO_PASSWORD_INVALIDA. "         , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!se_permite){
            resultado = INFO_SINCRONIZACION_SIN_PERMISO;
            this.log("sincronizarBorrarFicheroDelServidor. INFO_SINCRONIZACION_SIN_PERMISO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END sincronizarBorrarFicheroDelServidor*/


    /**
     * Mismo metodo que borrarFicheroDelServidor pero para la sincronizacion.                                                                                                         <br>
     * Es necesario que haya metodos especificos para la sincronizacion debido a que se debe proporcionar el id del servidor.                                                         <br>
     * A los códigos de resultado que devuelve el metodo normal se añade INFO_SINCRONIZACION_SIN_PERMISO .                                                                            <br>
     *                                                                                                                                                                                <br>
     * Para iniciar la lectura de un fichero que esta almacenado en el servidor.                                                                                                      <br>
     * Se envia el nombre del fichero junto con el directorio para que no se confunda con otros posibles ficheros con el mismo nombre.                                                <br>
     * El nombre del directorio debe empezar sin barra pero si debe terminar en barra. Aqui no se comprueba que este bien escrito. Fallara si no se envia bien.                       <br>
     * El servidor crea id para esta comunicacion que es lo que le devuelve al cliente.                                                                                               <br>
     * El cliente usara este id para solicitar trozos del fichero.                                                                                                                    <br>
     * Si el id devuelto comienza por un guion medio entonces es un numero negativo e indica que hubo algun problema:                                                                 <br>
     *    INFO_FICHERO_NO_ENCONTRADO                            Fichero no encontrado                                                                                                 <br>
     *    INFO_LIMITE_MAX_IDS_POR_CREAR                         Se ha superado el limite de ids que se pueden crear.                                                                  <br>
     *    INFO_LIMITE_MAX_LECTORES_POR_ID                       Se ha superado el limite de lectores por ids.                                                                         <br>
     *    INFO_LIMITE_MAX_LECTORES                              Se ha superado el limite total de lectores que pueden acceder al servidor simultaneamente.                            <br>
     *    WARNING_DURANTE_LA_ESPERA                             Mientras se esperaba a que quedara libre el fichero se produjo un error.                                              <br>
     *    ERROR_INTERNO_FICHERO_EXISTENTE_MARCADO_COMO_BORRADO  No debe estar el fichero en disco a la vez que esta marcado como borrado.                                             <br>
     *    INFO_SINCRONIZACION_SIN_PERMISO                       Si el servidor no tiene permiso para sincronizarse.                                                                   <br>
     *    INFO_PASSWORD_INVALIDA                                La contraseña de administracion suministrada no es valida                                                             <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen                  El id del servidor que se esta sincronizando.
     * @param  nombre_directorio           Sirve para encontrar el directorio del fichero
     * @param  nombre_fichero              El nombre del fichero. 
     * @param  longitud_del_paquete        El cliente establece cual debe ser la longitud de array de bytes
     * @return String                      Una cadena que identifica la comunicacion. Así se puede leer el fichero a trozos y sin prisas.
     */
    @Override
    public String sincronizarSolicitarLeerFicheroDelServidor  (String password_administracion, String id_almacen, String nombre_directorio, String nombre_fichero, int longitud_del_paquete){
    /*VAR*/
        String      password_administracion_actual =    "";
        String      password_lectura               =    "";
        String      resultado                      =    "";
        boolean     se_permite                     = false;
        boolean     password_valida                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_z.quieroLeer();
            se_permite = this.z_sincronizacion_aceptada && this.z_id_almacen_pasivo.equals(id_almacen);
        this.lock_grupo_z.dejoDeLeer();
        
        password_valida = password_administracion_actual.equals(password_administracion);        
        if (password_valida && se_permite){
            this.lock_grupo_w.quieroLeer();
                password_lectura = this.w_password_lectura;
            this.lock_grupo_w.dejoDeLeer();
            resultado = this.solicitarLeerFicheroDelServidor(password_lectura, nombre_directorio, nombre_fichero, longitud_del_paquete);
            this.log("sincronizarSolicitarLeerFicheroDelServidor. Solicitud de: "+ id_almacen + ". Directorio+Fichero: " + nombre_directorio +  nombre_fichero + ". Resultado: " + resultado, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
            resultado = "" + INFO_PASSWORD_INVALIDA + "";
            this.log("sincronizarSolicitarLeerFicheroDelServidor. INFO_PASSWORD_INVALIDA. "         , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!se_permite){
            resultado = "" + INFO_SINCRONIZACION_SIN_PERMISO + "";
            this.log("sincronizarSolicitarLeerFicheroDelServidor. INFO_SINCRONIZACION_SIN_PERMISO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END sincronizarSolicitarLeerFicheroDelServidor*/


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
     */
    @Override
    public List<FicheroPaquete> sincronizarLeyendoFicheroDelServidorLeerPaquetes(String password_administracion, String id_almacen, String id_de_lectura){
    /*VAR*/
        List<FicheroPaquete>      resultado                      =  null;
        String                    password_administracion_actual =    "";
        String                    password_lectura               =    "";
        boolean                   se_permite                     = false;
        boolean                   password_valida                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_z.quieroLeer();
            se_permite = this.z_sincronizacion_aceptada && this.z_id_almacen_pasivo.equals(id_almacen);
        this.lock_grupo_z.dejoDeLeer();

        password_valida = password_administracion_actual.equals(password_administracion);        
        if (password_valida && se_permite){
            this.lock_grupo_w.quieroLeer();
                password_lectura = this.w_password_lectura;
            this.lock_grupo_w.dejoDeLeer();
            resultado = this.leyendoFicheroDelServidorLeerPaquetes(password_lectura, id_de_lectura);
            this.log("sincronizarLeyendoFicheroDelServidorLeerPaquetes. Solicitud de: "+ id_almacen + ". Fichero/ID: " + id_de_lectura, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
//          resultado = "" + INFO_PASSWORD_INVALIDA + "";
            this.log("sincronizarLeyendoFicheroDelServidorLeerPaquetes. INFO_PASSWORD_INVALIDA. "         , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!se_permite){
//          resultado = "" + INFO_SINCRONIZACION_SIN_PERMISO + 
            this.log("sincronizarLeyendoFicheroDelServidorLeerPaquetes. INFO_SINCRONIZACION_SIN_PERMISO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END sincronizarLeyendoFicheroDelServidorLeerPaquetes*/

    
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
     *    INFO_PASSWORD_INVALIDA             La contraseña de administracion suministrada no es valida                                                                  <br>
     * 
     * @param  password_administracion     Se debe enviar la contraseña de administracion para usar los metodos de sincronizar.
     * @param  id_almacen                  El id del servidor que se esta sincronizando.
     * @param  id_de_lectura               El identificador que creo 'solicitarLeerFicheroDelServidor' antes de empezar la comunicacion 
     * @return int                         Un numero que indica el resultado de la operacion.
     */
    @Override
    public int sincronizarLeyendoFicheroDelServidorLecturaFinalizada (String password_administracion, String id_almacen, String id_de_lectura){
    /*VAR*/
        String      password_administracion_actual =    "";
        String      password_lectura               =    "";
        int         resultado                      =     0;
        boolean     se_permite                     = false;
        boolean     password_valida                = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion_actual = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();
        this.lock_grupo_z.quieroLeer();
            se_permite = this.z_sincronizacion_aceptada && this.z_id_almacen_pasivo.equals(id_almacen);
        this.lock_grupo_z.dejoDeLeer();
        
        password_valida = password_administracion_actual.equals(password_administracion);        
        if (password_valida && se_permite){
            this.lock_grupo_w.quieroLeer();
                password_lectura = this.w_password_lectura;
            this.lock_grupo_w.dejoDeLeer();
            resultado = this.leyendoFicheroDelServidorLecturaFinalizada(password_lectura, id_de_lectura);
            this.log("sincronizarLeyendoFicheroDelServidorLecturaFinalizada. Solicitud de: "+ id_almacen + ". Fichero/ID: " + id_de_lectura + ". Resultado: " + resultado, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
        }else if (!password_valida){
            resultado = INFO_PASSWORD_INVALIDA;
            this.log("sincronizarLeyendoFicheroDelServidorLecturaFinalizada. INFO_PASSWORD_INVALIDA. "         , "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }else if (!se_permite){
            resultado = INFO_SINCRONIZACION_SIN_PERMISO;
            this.log("sincronizarLeyendoFicheroDelServidorLecturaFinalizada. INFO_SINCRONIZACION_SIN_PERMISO. ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return resultado;
    }/*END sincronizarLeyendoFicheroDelServidorLecturaFinalizada*/

    
// METODOS PRIVADOS auxiliares de los publico ----------------------------------

    
    /**
     * Para crear un id que se usara como key en los Maps.                                                                                                      <br>
     * Tambien podra ser usado como el id que el usuario debe enviar para continuar una escritura o una lectura.                                                <br>
     * Esta la duda de si deberia ser algo mas complejo y mas criptico para que no sea modificado por el usuario pero por ahora solo se piensa en que funcione. <br>
     * Lo unico que se hace es concatener el directorio con el nombre del fichero.
     * 
     * @param  directorio_relativo El directorio del nuevo fichero.
     * @param  nombre_fichero      El nombre     del nuevo fichero.
     * @return String              Una cadena que sera unica para cada par (directorio_relativo, nombre_fichero)
     */
    private String idParaFichero(String directorio_relativo, String nombre_fichero){
    /*BEGIN*/
        return directorio_relativo + nombre_fichero;
    }/*END idParaFichero*/
    
    
    /**
     * Dice si se puede crear un nuevo lector en funcion de los limites que se le impone a la creacion de nuevos lectores.         <br>
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo b en modo lectura. <br>
     * Resultados:                                                                                                                 <br>
     * ERROR_SIN_ERRORES                Se permite crear un nuevo lector                                                           <br>
     * INFO_LIMITE_MAX_IDS_POR_CREAR    Se ha superado el numero maximo de ids que se pueden crear.                                <br>
     * INFO_LIMITE_MAX_LECTORES_POR_ID  Se ha superado el limite de lectores por ids.                                              <br>
     * INFO_LIMITE_MAX_LECTORES         Se ha superado el limite total de lectores que pueden acceder al servidor simultaneamente. <br>
     * 
     * @param  id  El identificador de lectura
     * @return int Un numero que indica el resultado de la operacion. 0 es que si se puede crear.
     */
    private int sePermiteNuevoLector(String id){
    /*VAR*/
        int          lectores_totales =  0;
        Integer      lectores_por_id  =  0;
        Integer      ids_leyendo      =  0;
        int          resultado        =  0;
    /*BEGIN*/
        lectores_totales = this.b_lectores_actuales_totales;
        lectores_por_id  = this.b_lectores_por_id.get(id);                      //Es redundante con this.a_lector_o_escritor
        ids_leyendo      = this.b_lecturas.size();
        if (lectores_por_id == null){
            lectores_por_id = 0;
        }/*if*/
        if (ids_leyendo == null){
            ids_leyendo = 0;
        }/*if*/
        if (lectores_totales >= this.b_numero_maximo_lectores_totales) {
            resultado               = INFO_LIMITE_MAX_LECTORES;
        }/*if*/
        if (lectores_por_id >= this.b_numero_maximo_lectores_por_id){
            resultado               = INFO_LIMITE_MAX_LECTORES_POR_ID;
        }/*if*/    
        if ( ids_leyendo >= this.b_numero_maximo_ids_para_leer ){
            resultado               = INFO_LIMITE_MAX_IDS_POR_CREAR;
        }/*if*/
        return resultado;
    }/*END sePermiteNuevoLector*/

    
    /**
     * Dice si se puede crear un nuevo escritor en funcion de los limites que se le impone a la creacion de nuevos escritores.     <br>
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo c en modo lectura. <br>
     * Resultados:                                                                                                                 <br>
     * Cero (0)                        Se permite crear un nuevo lector                                                            <br>
     * INFO_LIMITE_MAX_IDS_POR_CREAR   Se ha superado el numero maximo de ids que se pueden crear.                                 <br>
     * 
     * @param  id   El identificador de escritura
     * @return int  Un numero que indica el resultado de la operacion. 0 es que si se puede crear.
     */
    private int sePermiteNuevoEscritor(String id){
    /*VAR*/
        int ids_escribiendo = 0;
        int resultado       = 0;
    /*BEGIN*/
        ids_escribiendo = this.c_escrituras.size();
        if (ids_escribiendo >= this.c_numero_maximo_ids_para_escribir){        
            resultado = INFO_LIMITE_MAX_IDS_POR_CREAR;
        }/*if*/
        return resultado;
    }/*END sePermiteNuevoEscritor*/
    
    
    /**
     * Para saber si el fichero ya existe y si es mas nuevo o mas antiguo.                                                         <br>
     * Devuelve true si el fichero enviado con su version es mas nuevo que lo que haya almacenado.                                 <br>
     * No tiene encuenta el atributo borrado, solo la version. Si la version es mas nueva se deja escribir el fichero.             <br>
     * Se usa compareTo: version_almacenada.compareTo(version_enviada). Debe devolver negativo para ser aceptado y devolver true.  <br>
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo z en modo lectura. <br>
     * 
     * @param  id                El identificador con el que se almacena el fichero en todas las estructuras.
     * @param  version           La version del nuevo fichero.
     * @return boolean           true si el fichero enviado en mas nuevo que lo que haya anotado.
     */
    private boolean nuevoFicheroVersion(String id, String version){
    /*VAR*/
        FicheroInfo fich_info          =  null;
        String      version_almacenada =    "";
        boolean     nuevo_fichero      = false;
    /*BEGIN*/
        fich_info = this.z_ficheros_del_almacen.get(id);
        if (fich_info != null){
            version_almacenada = fich_info.getVersion();
            if ( version_almacenada.compareTo(version) < 0){
                nuevo_fichero = true;
            }/*if*/
        }else{
            nuevo_fichero = true;
        }/*if*/
        return nuevo_fichero;
    }/*END nuevoFicheroVersion*/

    
    /**
     * Metodo que devuelve los ficheros que tiene almacenado el Almacen dentro del directorio base.                                                                <br>
     * Devuelve la lista de ficheros que hay en el directorio. Se devuelve solo la ruta relativa junto con el fichero, el trozo del directorio enviado es omitido. <br>
     *                                                                                                                                                             <br>
     * Realmente no se si se llegara a usar este metodo pero se prepara por si acaso.                                                                                             <br>
     * Si se usa en conjunto con una bbdd el listado de ficheros estara en la bbdd y se ignoraran lo ficheros que alguien copiara manualmente sin usar los metodos de esta clase. <br>
     * Si no se usa con bbdd entonces sí será necesario que recorra el directorio que se envia y anote los fichero que encuentra.                                                 <br>
     * 
     * @param  dir_base      La ruta desde la que buscar
     * @param  dir_recursivo Para cuando se realicen llamadas recursivas. Cadena vacia al ser llamado desde fuera. Es para profundizar en otros directorios, contiene un trozo de cadena que se concatena con dir_base para crear la ruta completa manteniendo separado lo que es el directorio enviado con el directorio encontrado.
     * @return List<String>  La lista de ficheros que hay en el directorio. Se devuelve solo la ruta relativa (la de las llamadas recursivas) junto con el fichero. El trozo del directorio enviado es omitido.
     */
    private List<String> ficherosEnDirectorioA(String dir_base, String dir_recursivo){
    /*VAR*/
        List<String> lista_ficheros     = new ArrayList<String>();
        List<String> lista_ficheros_aux = new ArrayList<String>();
        File[]       lista_files        = null;
        File         f_dir              = null; 
        String       str_base_recursivo =   "";
        String       str_recursivo_aux  =   "";
        String       str_dir            =   "";
        String       str_new_file       =   "";
        String       last_char_dir      =   "";
        int          num_files          =    0;
        int          len_str_dir        =    0;
        int          num_chars_dir      =    0;
        int          i                  =    0;
    /*BEGIN*/
        str_base_recursivo = dir_base + dir_recursivo;
        f_dir = new File(str_base_recursivo);
        if ( f_dir.isDirectory() ){
            lista_files   = f_dir.listFiles();
            num_files     = lista_files.length;
            str_dir       = f_dir.getPath();
            len_str_dir   = str_dir.length() + 1;                               //+1 porque le quita la barra final al directorio.
            for (i=0; i<num_files; i++){
                if ( lista_files[i].isDirectory() ){
                    num_chars_dir      = dir_base.length();
                    last_char_dir      = dir_base.substring(num_chars_dir-1, num_chars_dir);
                    str_recursivo_aux  = lista_files[i].getPath();
                    str_recursivo_aux  = str_recursivo_aux.substring(len_str_dir) + last_char_dir;
                    lista_ficheros_aux = ficherosEnDirectorioA(dir_base, dir_recursivo + str_recursivo_aux);
                    lista_ficheros.addAll(lista_ficheros_aux);
                }else{
                    str_new_file = lista_files[i].getPath();
                    str_new_file = str_new_file.substring(len_str_dir);
                    lista_ficheros.add(dir_recursivo + str_new_file);
                }/*if*/
            }/*for*/
        }/*if*/
        return lista_ficheros;
    }/*END ficherosEnDirectorioA*/


    /**
     * Metodo que devuelve los ficheros que tiene almacenado el Almacen dentro del directorio base.                                                                               <br>
     * Devuelve un Map<String, FicheroInfo> que representan a los ficheros que hay en el directorio.                                                                              <br>
     * La key es la ruta relativa junto con el fichero, el trozo del directorio enviado es omitido.                                                                               <br>
     * El valor es un FicheroInfor que recoge:                                                                                                                                    <br>
     *    DirectorioRelativo: El directorio sin el directorio base y sin el nombre del fichero. Empieza en letra, sin barra '/'. Terminar en barra '/'.                           <br>
     *    NombreFichero     : El nombre del fichero.                                                                                                                              <br>
     *    Version           : String con fecha con el formato 'yyyy/MM/dd HH:mm:ss'                                                                                               <br>
     *    FlagBorrado       : False. Si esta en el disco duro no puede estar borrado.                                                                                             <br>
     * 
     * @param  prop_versiones            El fichero donde se asocia a cada id de fichero su version. 
     * @param  dir_base                  La ruta desde la que buscar
     * @param  dir_recursivo             Para cuando se realicen llamadas recursivas. Cadena vacia al ser llamado desde fuera. Es para profundizar en otros directorios, contiene un trozo de cadena que se concatena con dir_base para crear la ruta completa manteniendo separado lo que es el directorio enviado con el directorio encontrado.
     * @return Map<String, FicheroInfo>  Los ficheros que hay en el directorio. Se devuelve informacion solo la ruta relativa (la de las llamadas recursivas) junto con el fichero. El trozo del directorio enviado es omitido.
     */
    private Map<String, FicheroInfo> ficherosEnDirectorioB(Properties prop_versiones, String dir_base, String dir_recursivo){
    /*VAR*/
        Map<String, FicheroInfo> map_ficheros       = new HashMap<String, FicheroInfo>();
        Map<String, FicheroInfo> map_ficheros_aux   = new HashMap<String, FicheroInfo>();
        FicheroInfo              fichero_info       = null;
        File[]                   lista_files        = null;
        File                     f_dir              = null; 
//      String                   str_date           =   "";
        String                   str_base_recursivo =   "";
        String                   str_recursivo_aux  =   "";
        String                   str_dir            =   "";
        String                   str_new_file       =   "";
        String                   last_char_dir      =   "";
        String                   key                =   "";
        String                   version            =   "";
//      long                     date               =    0;
        int                      num_files          =    0;
        int                      len_str_dir        =    0;
        int                      num_chars_dir      =    0;
        int                      i                  =    0;
    /*BEGIN*/
        num_chars_dir      = dir_base.length();                                                    //Numero de caracteres del directorio base.
        last_char_dir      = dir_base.substring(num_chars_dir-1, num_chars_dir);                   //Se coge el ultimo caracter que deberia ser '/'
        str_base_recursivo = dir_base + dir_recursivo;                                             //El directorio donde se realiza la busqueda actual.
        f_dir              = new File(str_base_recursivo);
        if ( f_dir.isDirectory() ){                                                                //Por seguridad. No deberia ser un fichero.
            lista_files   = f_dir.listFiles();                                                     //Se coge la lista de ficheros
            num_files     = lista_files.length;                                                    //Se cuenta para saber cuantos ficheros hay.
            str_dir       = f_dir.getPath();                                                       //El directorio que es un File se pasa a String.
            len_str_dir   = str_dir.length() + 1;                                                  //+1 porque le quita la barra final al directorio.
            for (i=0; i<num_files; i++){                                                           //Se recorren los ficheros del directorio que pueden ser tambien directorios y ficheros.
                if ( lista_files[i].isDirectory() ){
                    str_recursivo_aux  = lista_files[i].getPath();
                    str_recursivo_aux  = str_recursivo_aux.substring(len_str_dir) + last_char_dir;
                    map_ficheros_aux   = ficherosEnDirectorioB(prop_versiones, dir_base, dir_recursivo + str_recursivo_aux);
                    map_ficheros.putAll(map_ficheros_aux);
                }else{
                    str_new_file = lista_files[i].getPath();
                    str_new_file = str_new_file.substring(len_str_dir);
                    if ( !str_new_file.equals(NOMBRE_FICHERO_VERSIONES) ){
                        key          = dir_recursivo + str_new_file;
                        //date       = lista_files[i].lastModified();                              //Forma antigua de generar la versión
                        //str_date   = this.fechaFormateada(date);
                        version      = prop_versiones.getProperty(key);
                        if (version == null){
                            version = "";
                            this.log("ficherosEnDirectorioB. No hay version para: " + key, "", MOSTAR_SERVIDOR[2-1], MOSTAR_FECHA[2-1], 2);
                        }/*if*/
                        fichero_info = new FicheroInfo(key);
                        fichero_info.setDirectorioRelativo (dir_recursivo);
                        fichero_info.setNombreFichero      (str_new_file );
                        fichero_info.setVersion            (version      );
                        fichero_info.setBorrado            (false        );
                        map_ficheros.put(key, fichero_info);
                    }/*if*/
                }/*if*/
            }/*for*/
        }/*if*/
        return map_ficheros;
    }/*END ficherosEnDirectorioB*/

    
// ZONA THREAD-RUNNABLE --------------------------------------------------------    
    
    /**
     * La versión distribuida necesita de un hilo que ejecute unas tareas cada cierto tiempo.                                                       <br>
     * Incluso una version que solo use un unico Almacen necesita ejecutar alguna de las tareas como la de caducar sesiones.                        <br>
     * Las tareas a realizar son:                                                                                                                   <br>
     *    A) Caducar las sesiones:  Evita que un fichero quede cogido indefinidamente, despues de un tiempo fuerza la finalizacion.                 <br>
     *    B) Comprobar conexiones:  Se conecta con los almacenes conocidos para ver si siguen vivos.                                                <br>
     *    C) Sincronizar ficheros:  A los almacenes del maps de activos les pide su lista de ficheros. Con las diferencias lee y escribe            <br> 
     *                                                                                                                                              <br>
     * Trabajando y Durmiendo. Son dos flags pasa saber el estado del proceso.                                                                      <br>
     * En los momentos de transicion no serán trabajando=true && durmiendo=false o al reves. Podran estar los dos a false o a true.                 <br>
     * Solo cuando trabajando=false && durmiendo=true se podrá interrumpir el proceso y se debe hacer bloqueando x_                                 <br>
     * De esta forma se garantiza que no se interrumpe el proceso mientras está ejecutandose que podría ser algo de potenciales problemas.          <br>
     *                                                                                                                                              <br>
     * Aqui se hace el control de acceso a las variables que necesita este procedimiento.                                                           <br>
     * Los procedimientos llamados deben controlar las variables a las que accedan.                                                                 <br>
     */
    @Override
    public void run(){
    /*VAR*/
        Thread   hilo                               =  null;
        String   mensaje_ciclico                    =    "";
        int      proceso_a_ejecutar                 =     0;
        int      tiempo_dormir_caducar_sesiones     =     0;
        int      tiempo_dormir_comprobar_conexiones =     0;
        int      tiempo_dormir_sincronizar_ficheros =     0;
        boolean  pausa                              = false;
        boolean  fin                                = false;
        boolean  ejecutar_caducar_sesiones          = false;
        boolean  ejecutar_comprobar_conexiones      = false;
        boolean  ejecutar_sincronizar_ficheros      = false;
    /*BEGIN*/
        //Recursivo. Crea otros hilos para que cada uno de ellos ejecute una de las tareas del switch y así cada uno tenga su tiempo de dormir.
        this.lock_grupo_x.quieroEscribir();
            proceso_a_ejecutar = this.x_proceso_a_ejecutar_cont;
            this.x_proceso_a_ejecutar_cont++;
            if (this.x_proceso_a_ejecutar_cont < this.x_proceso_a_ejecutar_max){    //Empieza en el cero. 
                hilo = new Thread(this);
                this.anotarHiloRun(hilo, this.x_proceso_a_ejecutar_cont);           //Quien lo crea lo anota. Y mejor antes de arrancarlo para evitar que haya más procesos accediendo al grupo x_
            }/*if*/
        this.lock_grupo_x.dejoDeEscribir();
        if (hilo != null){                                                          //El ultimo hilo anota lo suyo pero no crea otro. 
            hilo.start();                                                           //No se puede invocar dentro del bloqueo a x_ pues es recursivo y se volveria a llamar a quieroEscribir creando una parada infinita.
        }/*if*/
        this.log("Arrancado el hilo numero " + proceso_a_ejecutar + ". ", "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        
        while (!fin){
            this.lock_grupo_x.quieroLeer();
                tiempo_dormir_caducar_sesiones     = this.x_tiempo_dormir_caducar_sesiones;
                tiempo_dormir_comprobar_conexiones = this.x_tiempo_dormir_comprobar_conexiones;
                tiempo_dormir_sincronizar_ficheros = this.x_tiempo_dormir_sincronizar_ficheros;
                fin                                = this.x_fin;
                pausa                              = this.x_pausa;
                ejecutar_caducar_sesiones          = this.x_ejecutar_caducar_sesiones;
                ejecutar_comprobar_conexiones      = this.x_ejecutar_comprobar_conexiones;
                ejecutar_sincronizar_ficheros      = this.x_ejecutar_sincronizar_ficheros;
            this.lock_grupo_x.dejoDeLeer();
            
            try{
                switch(proceso_a_ejecutar){
                    case 0:                                                              // CASE 0 ----- CADUCAR SESIONES -----------------------------------------------
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_trabajando[proceso_a_ejecutar] = true;       //Anota que empieza a trabajar. No sera interrumpido para terminar cuando se quiera apagar()
                        this.lock_grupo_x.dejoDeEscribir();
                        if (!fin && !pausa && ejecutar_caducar_sesiones){
                            this.caducarSesiones     ();                                 //TRABAJANDO
                        }/*if*/
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_trabajando[proceso_a_ejecutar] = false;      //Anota que ya no esta trabajando. Pero aun no esta dormido. 
                        this.lock_grupo_x.dejoDeEscribir();                        
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_durmiendo[proceso_a_ejecutar] = true;        //Anota que va a dormir y es cuando puede ser inrrumpido: trabajar=false && dormido=true.
                        this.lock_grupo_x.dejoDeEscribir();
                        if (!fin){
                            Thread.sleep(tiempo_dormir_caducar_sesiones);                //DURMIENDO
                        }/*if*/
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_durmiendo[proceso_a_ejecutar] = false;       //Anota que ya no esta dormido. Pero aun no esta trabajando. 
                        this.lock_grupo_x.dejoDeEscribir();
                        this.log("run. Ciclo completado para la tarea: " + proceso_a_ejecutar + "", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    break;                                                               //------------------------------------------------------------------------------
                    case 1:                                                              // CASE 1 ----- COMPROBAR CONEXIONES -------------------------------------------
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_trabajando[proceso_a_ejecutar] = true;       //Anota que empieza a trabajar. No sera interrumpido para terminar cuando se quiera apagar()
                        this.lock_grupo_x.dejoDeEscribir();                                                
                        if (!fin && !pausa && ejecutar_comprobar_conexiones){
                            this.comprobarConexiones ();                                 //TRABAJANDO
                        }/*if*/
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_trabajando[proceso_a_ejecutar] = false;      //Anota que ya no esta trabajando. Pero aun no esta dormido. 
                        this.lock_grupo_x.dejoDeEscribir();                                                
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_durmiendo[proceso_a_ejecutar] = true;        //Anota que va a dormir y es cuando puede ser inrrumpido: trabajar=false && dormido=true.
                        this.lock_grupo_x.dejoDeEscribir();
                        if (!fin){
                            Thread.sleep(tiempo_dormir_comprobar_conexiones);            //DURMIENDO
                        }/*if*/
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_durmiendo[proceso_a_ejecutar] = false;       //Anota que ya no esta dormido. Pero aun no esta trabajando. 
                        this.lock_grupo_x.dejoDeEscribir();
                        this.log("run. Ciclo completado para la tarea: " + proceso_a_ejecutar + "", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    break;                                                               //------------------------------------------------------------------------------
                    case 2:                                                              // CASE 2 ----- SINCRONIZAR FICHEROS -------------------------------------------
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_trabajando[proceso_a_ejecutar] = true;       //Anota que empieza a trabajar. No sera interrumpido para terminar cuando se quiera apagar()
                        this.lock_grupo_x.dejoDeEscribir();                                                
                        if (!fin && !pausa && ejecutar_sincronizar_ficheros){
                            this.sincronizarFicheros ();                                 //TRABAJANDO
                        }/*if*/
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_trabajando[proceso_a_ejecutar] = false;      //Anota que ya no esta trabajando. Pero aun no esta dormido. 
                        this.lock_grupo_x.dejoDeEscribir();                                                
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_durmiendo[proceso_a_ejecutar] = true;        //Anota que va a dormir y es cuando puede ser inrrumpido: trabajar=false && dormido=true.
                        this.lock_grupo_x.dejoDeEscribir();
                        if (!fin){
                            Thread.sleep(tiempo_dormir_sincronizar_ficheros);            //DURMIENDO
                        }/*if*/
                        this.lock_grupo_x.quieroEscribir();
                            this.x_procesos_durmiendo[proceso_a_ejecutar] = false;       //Anota que ya no esta dormido. Pero aun no esta trabajando. 
                        this.lock_grupo_x.dejoDeEscribir();
                        this.log("run. Ciclo completado para la tarea: " + proceso_a_ejecutar + "", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    break;                                                               //----------------------------------------------------------
                }/*switch*/
            }catch (InterruptedException ex){
                this.log("Run().  InterruptedException. Numero del proceso: " + proceso_a_ejecutar + ". ", "", MOSTAR_SERVIDOR[2-1], MOSTAR_FECHA[2-1], 2);
                //Se ignora y se realiza la ejecucion del bucle.
            }/*try-catch*/
        }/*while*/
        
        //Para informar que ha terminado su ejecucion.
        this.lock_grupo_x.quieroEscribir();
            this.x_procesos_informar_finalizados[proceso_a_ejecutar] = true;
        this.lock_grupo_x.dejoDeEscribir();
    }/*END run*/
    
    
    /**
     * Se encarga de la tarea de finalizar las sesiones de lectura o escritura que llevan mas tiempo del permitido ejecutandose.                                                <br>
     * Con los lectores invoca el metodo de lectura finalizada y con los escritores invoca a cancelar la escritura.                                                             <br>
     * El tiempo relativo se usa multiplicando tres elementos: b_tiempo_max_rel_para_leer  *  tamaño_paquete  *  numero_de_paquetes                                             <br>
     * Si no es cero se usa b_tiempo_max_rel_para_leer                                                                                                                          <br>
     * Accede a los grupos b_, c_ y w_ en modo lectura. Los metodos de finalizarLectura y cancelarEscritura tendrán sus propios grupos.                                         <br>
     * Pero la invocacion a finalizarLectura y cancelarEscritura se hace fuera de un bloqueo, es decir, la llamada desde este procedimiento no crea anidamiento.                <br>
     */
    private void caducarSesiones (){
    /*VAR*/
        List<String> ids_lectores_caducados   =  new ArrayList<String>();
        List<String> ids_escritores_caducados =  new ArrayList<String>();
        Set          keys_ids                 =  null;
        Iterator     itr_ids                  =  null;         
        Date         date_actual              =  null;
        Date         date_del_id              =  null;
        String       id_aux                   =    "";
        String       password_lectura         =    "";
        String       password_escritura       =    "";
        String       password_administracion  =    "";
        float        ms_limite                =     0;
        long         ms_date_actual           =     0;
        long         ms_date_del_id           =     0;
        long         ms_date_de_la_sincr      =     0;
        int          tam_paquete              =     0;
        int          cantidad_paquetes        =     0;
        boolean      calcular_rel             = false;
        boolean      caducar_sincronizacion   = false;
    /*BEGIN*/
        date_actual    = new Date();
        ms_date_actual = date_actual.getTime();

        this.lock_grupo_w.quieroLeer();
            password_lectura        = this.w_password_lectura;
            password_escritura      = this.w_password_escritura;
            password_administracion = this.w_password_administracion;
        this.lock_grupo_w.dejoDeLeer();            
        
        //Se buscan los lectores caducados
        this.lock_grupo_b.quieroLeer();
            if (this.b_tiempo_max_rel_para_leer == 0){
                calcular_rel = false;
                ms_limite    = this.b_tiempo_max_abs_para_leer;
            }else{
                calcular_rel = true;
            }/*if*/
            keys_ids = this.b_hora_de_inicio_lectura.keySet();
            itr_ids  = keys_ids.iterator();
            while ( itr_ids.hasNext() ){
                id_aux         = (String)itr_ids.next();
                date_del_id    = this.b_hora_de_inicio_lectura.get(id_aux);
                ms_date_del_id = date_del_id.getTime();
                if (calcular_rel){
                    tam_paquete       = this.b_tamano_paquetes.get  (id_aux);
                    cantidad_paquetes = this.b_cantidad_paquetes.get(id_aux);
                    ms_limite         = this.b_tiempo_max_rel_para_leer * tam_paquete * cantidad_paquetes;
                }/*if*/
                if ( (ms_date_actual - ms_date_del_id) > ms_limite ){
                    ids_lectores_caducados.add(id_aux);
                }/*if*/
            }/*while*/
        this.lock_grupo_b.dejoDeLeer();

        //Se da por finalizada la lectura de los lectores caducados.
        itr_ids = ids_lectores_caducados.iterator();
        while ( itr_ids.hasNext() ){
            id_aux = (String)itr_ids.next();
            this.leyendoFicheroDelServidorLecturaFinalizada(password_lectura, id_aux);
        }/*while*/

        //Se buscan los escritores caducados
        this.lock_grupo_c.quieroLeer();
            if (this.c_tiempo_max_rel_para_escribir == 0){
                calcular_rel = false;
                ms_limite    = this.c_tiempo_max_abs_para_escribir;
            }else{
                calcular_rel = true;
            }/*if*/
            keys_ids = this.c_hora_de_inicio_escritura.keySet();
            itr_ids  = keys_ids.iterator();
            while ( itr_ids.hasNext() ){
                id_aux         = (String)itr_ids.next();
                date_del_id    = this.c_hora_de_inicio_escritura.get(id_aux);
                ms_date_del_id = date_del_id.getTime();
                if (calcular_rel){
                    tam_paquete       = this.c_tamano_paquetes.get  (id_aux);       //A las escrituras les puse una variable mientras que a las lecturas no. La gestion interna requeria un mayor control de estos valores en las escrituras y no en las lecturas.
                    cantidad_paquetes = this.c_cantidad_paquetes.get(id_aux);
                    ms_limite         = this.c_tiempo_max_rel_para_escribir * tam_paquete * cantidad_paquetes;
                }/*if*/                
                if ( (ms_date_actual - ms_date_del_id) > ms_limite ){
                    ids_escritores_caducados.add(id_aux);
                }/*if*/
            }/*while*/        
        this.lock_grupo_c.dejoDeLeer();

        //Se cancela la escritura de los escritores caducados.
        itr_ids = ids_escritores_caducados.iterator();
        while ( itr_ids.hasNext() ){
            id_aux = (String)itr_ids.next();
            this.escribiendoFicheroEnServidorCancelar(password_escritura, id_aux);
        }/*while*/
        
        //Se mira si debe cancelar la sesion de sincronización pasiva.
        this.lock_grupo_z.quieroLeer();
            if (this.z_hora_de_inicio_sincronizar != null){
                ms_date_de_la_sincr = this.z_hora_de_inicio_sincronizar.getTime();
                if ( (ms_date_actual - ms_date_de_la_sincr) > this.z_tiempo_max_abs_para_sincronizar ){
                    caducar_sincronizacion = true;
                }/*if*/
            }/*if*/
        this.lock_grupo_z.dejoDeLeer();
        
        //Se da por finalizada la sincronizacion
        if (caducar_sincronizacion){
            this.sincronizarInformarFinalizar(password_administracion);
        }/*if*/
    }/*END caducarSesiones*/
    
    
    /**
     * Se encarga de la tarea de comprobar la conectividad con los demas almacenes que estan anotados en las distintas variables del grupo y_                                                          <br>
     * Hara ping y movera a otro map cuando sea necesario                                                                                                                                              <br>
     * Si y_num_comunicaciones_activas queda por debajo de y_num_max_comunicaciones_activas se buscaran nuevos almacenes con los que realizar sincronizaciones activas.                                <br>
     * Para esta busqueda se le proporciona y_almacenes_orden_1. Despues de la busqueda se regenera y_almacenes_orden_0 e y_almacenes_orden_1                                                          <br>
     * El control de acceso a las variables compartidas se realiza aqui.                                                                                                                               <br>
     * La llamada remota no puede realizarse con el grupo y_ bloqueado pues implicaria bloqueo entre servidores si todos estan con la tarea de comprobarConexiones().                                  <br>
     * La llamada remota necesita acceder al grupo y_ para generar la respuesta que se le solicita.                                                                                                    <br>
     *                                                                                                                                                                                                 <br>
     * Se debe comprobar la conectividad de los almacenes de las siguientes variables:                                                                                                                 <br>
     *     y_almacenes_comunicacion_activa  Si no responde al ping o hay problemas se saca de y_almacenes_orden_0 y y_almacenes_comunicacion_activa. Se decrementa y_num_comunicaciones_activas.       <br>
     *     y_almacenes_comunicacion_pasiva  Si no responde al ping o hay problemas se saca de y_almacenes_orden_0 y y_almacenes_comunicacion_pasiva. Se decrementa y_num_comunicaciones_pasivas.       <br>
     *     y_almacenes_orden_1              Si no responde al ping o hay problemas se saca de y_almacenes_orden_1.                                                                                     <br>
     *                                                                                                                                                                                                 <br>
     * y_almacenes_comunicacion_activa Inter y_almacenes_comunicacion_pasiva    debe ser el conjunto    vacio.                                                                                         <br>
     * y_almacenes_orden_0                                                           es  el conjunto    y_almacenes_comunicacion_activa Union y_almacenes_comunicacion_pasiva                          <br>
     * y_almacenes_orden_1                                                           es  el conjunto    resultado de unir los y_almacenes_orden_0 de los elementos de mi conjunto y_almacenes_orden_0  <br>
     */
    private void comprobarConexiones(){
    /*CONST*/
        String  ping  = "ping";
    /*VAR*/
        Map<String, AlmacenInterfaceRemote> maps_almacenes_para_comprobar_activos =  null;
        Map<String, AlmacenInterfaceRemote> maps_almacenes_para_comprobar_pasivos =  null;
        List<AlmacenInterfaceRemote>        lista_almacenes_para_contactar        =  null;
        List<String>                        lista_almacenes_activos_borrar        =  null;     //Muchas variables similares y podria usarse la misma. Se deja para que resulte mas facil de entender el codigo.
        List<String>                        lista_almacenes_pasivos_borrar        =  null;
        List<String>                        lista_almacenes_contactados           =  null;
        AlmacenInterfaceRemote              almacen_aux                           =  null;
        Set<String>                         keys_map                              =  null;
        Iterator                            itr_keys                              =  null;
        String                              key                                   =    "";
        String                              respuesta_ping                        =    "";
        String                              password_administracion               =    "";
        int                                 control_remote_exception              =     0;
        int                                 num_almacenes                         =     0;
        int                                 i                                     =     0;
        boolean                             error_conexion                        = false;
        boolean                             confirma_conexion                     = false;
        boolean                             finalizar_sincronizacion_pasiva       = false;
        boolean                             hubo_cambios                          = false;
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion = this.w_password_administracion;                    //Necesaria para realizar las llamadas remotas.
        this.lock_grupo_w.dejoDeLeer();
        maps_almacenes_para_comprobar_activos = new HashMap<String, AlmacenInterfaceRemote>();
        maps_almacenes_para_comprobar_pasivos = new HashMap<String, AlmacenInterfaceRemote>();

        //Fase 1/3.                                                                      //Almacenes a los que preguntar. Hay que recopilarlos porque no se puede usar la variable y_ directamente pues implica tenerla bloqueada.        
        this.lock_grupo_y.quieroLeer();
            keys_map = this.y_almacenes_comunicacion_activa.keySet();
            itr_keys = keys_map.iterator();
            while ( itr_keys.hasNext() ){
                key               = (String)itr_keys.next();
                almacen_aux       = this.y_almacenes_comunicacion_activa.get(key);
                maps_almacenes_para_comprobar_activos.put(key, almacen_aux);
            }/*while*/

            keys_map = this.y_almacenes_comunicacion_pasiva.keySet();
            itr_keys = keys_map.iterator();
            while ( itr_keys.hasNext() ){
                key               = (String)itr_keys.next();
                almacen_aux       = this.y_almacenes_comunicacion_pasiva.get(key);
                maps_almacenes_para_comprobar_pasivos.put(key, almacen_aux);
            }/*while*/
        this.lock_grupo_y.dejoDeLeer();
        
        //Fase 2/3. Activos                                                              //Comprobacion de conexion. No se puede hacer con el grupo y_ bloqueado.
        lista_almacenes_activos_borrar =  new ArrayList<String>();
        keys_map                       = maps_almacenes_para_comprobar_activos.keySet();
        itr_keys                       = keys_map.iterator();
        control_remote_exception       = 0;
        while ( itr_keys.hasNext() ){
            try{
                error_conexion    = false;
                confirma_conexion = false;
                key               = (String)itr_keys.next();
                almacen_aux       = maps_almacenes_para_comprobar_activos.get(key);
                respuesta_ping    = almacen_aux.testConnection(ping);                                             control_remote_exception = 1;
                if( !respuesta_ping.equals(ping) ){
                    error_conexion = true;
                }else{
                    confirma_conexion = almacen_aux.confirmarConexionPasiva(password_administracion, this.id);    control_remote_exception = 2;
                }/*if*/
            }catch (RemoteException ex){
                switch(control_remote_exception){
                    case 0:
                        error_conexion = true;
                        this.log("comprobarConexiones. RemoteException. Producida por testConnection().    Activa. ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    break;
                    case 1:
                        error_conexion = true;
                        this.log("comprobarConexiones. RemoteException. Producida por confirmarConexion(). Activa. ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    break;
                }/*switch*/
            }/*try-catch*/
            if (error_conexion || !confirma_conexion){
                lista_almacenes_activos_borrar.add(key);                                                     //Si hubo algun tipo de problema se quita de Activo
            }/*if*/
        }/*while*/

        //Fase 2/3. Pasivos                                                                                  //Comprobacion de conexion. No se puede hacer con el grupo y_ bloqueado.
        lista_almacenes_pasivos_borrar =  new ArrayList<String>();
        keys_map                       = maps_almacenes_para_comprobar_pasivos.keySet();
        itr_keys                       = keys_map.iterator();
        control_remote_exception       = 0;
        while ( itr_keys.hasNext() ){
            try{
                error_conexion    = false;
                confirma_conexion = false;
                key               = (String)itr_keys.next();
                almacen_aux       = maps_almacenes_para_comprobar_pasivos.get(key);
                respuesta_ping    = almacen_aux.testConnection(ping);                                             control_remote_exception = 1;
                if( !respuesta_ping.equals(ping) ){
                    error_conexion = true;
                }else{
//                  confirma_conexion = almacen_aux.confirmarConexionActiva(password_administracion, this.id);    control_remote_exception = 2;
                }/*if*/
            }catch (RemoteException ex){
                switch(control_remote_exception){
                    case 0:
                        error_conexion = true;
                        this.log("comprobarConexiones. RemoteException. Producida por testConnection().    Pasiva. ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    break;
                    case 1:
                        error_conexion = true;
                        this.log("comprobarConexiones. RemoteException. Producida por confirmarConexion(). Pasiva. ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    break;
                }/*switch*/
            }/*try-catch*/
            if (error_conexion){
//          if (error_conexion || !confirma_conexion){
                lista_almacenes_pasivos_borrar.add(key);                                                     //Si es error de conexion ya no se puede usar.
            }/*if*/
        }/*while*/
        
        //Fase 3/3.                                                                                          //Borrar almacenes problematicos y actualizar otras estructuras.
        this.lock_grupo_y.quieroEscribir();
            num_almacenes = lista_almacenes_activos_borrar.size();
            hubo_cambios = hubo_cambios || (num_almacenes > 0);
            for (i=0; i<num_almacenes; i++){
                key = lista_almacenes_activos_borrar.get(i);
                this.borrarAlmacenActivo(key);
            }/*for*/
            num_almacenes = lista_almacenes_pasivos_borrar.size();
            hubo_cambios = hubo_cambios || (num_almacenes > 0);
            for (i=0; i<num_almacenes; i++){
                key = lista_almacenes_pasivos_borrar.get(i);
                this.borrarAlmacenPasivo   (key);
            }/*for*/
            this.generarAlmacenesOrdenCero ();                                                               //Una vez modificadas las listas de activos y pasivos se reconstruye orden 0.        
        this.lock_grupo_y.dejoDeEscribir();
//      if (hubo_cambios){                                                                                   //Que no haya cambio en los activos y pasivos (orden0) no quiere decir que no los haya habido para para los activos y pasivos. 
            this.generarAlmacenesOrdenUno  (password_administracion);                                        //Tiene su control del bloqueo. Se debe acutalizar siempre que se actualice orden 0 pues depende de los almacenes de orden 0.
//      }/*if*/

        //Se buscan nuevas conexiones activas si fuera necesario. Se usa orden1
        this.lock_grupo_y.quieroEscribir();
            hubo_cambios = false;
            if (this.y_num_comunicaciones_activas < this.y_num_max_comunicaciones_activas){
                lista_almacenes_para_contactar = this.mapValuesToList(this.y_almacenes_orden_1);             //Solo se le da orden1. No debe intenar contactar con los que ya son activos o pasivos. Aunque en orden1 podria haber alguno que sea para nosotros activo o pasivo. 
                this.lock_grupo_y.dejoDeEscribir();   //Se libera el bloqueo.         Cuidado, a la inversa.
                    lista_almacenes_contactados = this.conectarConServidoresA    (password_administracion, lista_almacenes_para_contactar);
                this.lock_grupo_y.quieroEscribir();   //Se vuelve a coger el bloqueo. Cuidado, a la inversa.
                hubo_cambios = (lista_almacenes_contactados.size() > 0);
                if (hubo_cambios){ 
                    this.generarAlmacenesOrdenCero ();                                                       //Se debe reconstruir orden0 si se agrego algun servidor a Activos.
                }/*if*/
            }/*if*/
        this.lock_grupo_y.dejoDeEscribir();
        if (hubo_cambios){
            this.generarAlmacenesOrdenUno  (password_administracion);
        }/*if*/

        //Se buscan nuevas conexiones activas si todavia fuera necesario. Se usa el fichero inicial.
        this.lock_grupo_y.quieroEscribir();
            hubo_cambios = false;
            if ( !this.y_ruta_fichero_distribuida.equals("")  && (this.y_num_comunicaciones_activas < this.y_num_max_comunicaciones_activas) ){
                lista_almacenes_para_contactar = this.listaAlmacenesParaContactar(this.y_ruta_fichero_distribuida);   
                this.lock_grupo_y.dejoDeEscribir();   //Se libera el bloqueo.         Cuidado, a la inversa.
                    lista_almacenes_contactados = this.conectarConServidoresA    (password_administracion, lista_almacenes_para_contactar);
                this.lock_grupo_y.quieroEscribir();   //Se vuelve a coger el bloqueo. Cuidado, a la inversa.                    
                hubo_cambios = (lista_almacenes_contactados.size() > 0);
                if (hubo_cambios){ 
                    this.generarAlmacenesOrdenCero ();                                                       //Se debe reconstruir orden0 si se agrego algun servidor a Activos.
                }/*if*/
            }/*if*/
        this.lock_grupo_y.dejoDeEscribir();
        if (hubo_cambios){
            this.generarAlmacenesOrdenUno  (password_administracion);
        }/*if*/
        
        //Se mira si alguno de los pasivos que han dado error tiene asignada la sincronizacion pasiva. En ese caso se le quita.
        this.lock_grupo_z.quieroLeer();
            num_almacenes = lista_almacenes_pasivos_borrar.size();
            i=0;
            while ( !finalizar_sincronizacion_pasiva && (i<num_almacenes) ){
                key = lista_almacenes_pasivos_borrar.get(i);
                if (this.z_id_almacen_pasivo.equals(key)){                                                   //Si el pasivo al que se le dio permiso de sincronizacion da problemas entonces se debe liberar el permiso para darselo a otro.
                    finalizar_sincronizacion_pasiva = true;
                }/*if*/
                i++;
            }/*while*/
        this.lock_grupo_z.dejoDeLeer();
        if (finalizar_sincronizacion_pasiva){
            this.sincronizarInformarFinalizar(password_administracion);
        }/*if*/
    }/*END comprobarConexiones*/
    
    
    /**
     * Haciendo peticiones a los servidores de la lista de almacenes activos (lo contrario a pasivo), le pedira su lista de ficheros para leerle y escribirle                         <br>
     * El control al acceso a las variables compartidas se realiza aqui.                                                                                                              <br>
     *                                                                                                                                                                                <br>
     * Totalmente bloqueante, mientras dura la sincronización de ficheros no se puede acceder al servidor.                                                                            <br>
     * Ni siquiera se puede dar acceso a lectura. Aqui se podria buscar la forma de bloquear el grupo a_ en modo lectura pero solicitarLeer necesita modificar variables del grupo a_ <br>
     * El grupo a_ debe ser el mas externo para evitar interbloqueos. Una vez se accede a el se accederan a los demas tarde o temprano, solo es esperar que salgan.                   <br>
     *                                                                                                                                                                                <br>
     * Si se quiere sincronizar un fichero que está siendo leido o escrito se ignorara puede que se ignore.                                                                           <br>
     * Si se quiere enviar y esta siendo leido no importa y se envia. Pero no se sobrescribira uno que este siendo accedido.                                                          <br>
     *                                                                                                                                                                                <br>
     * Aun puede suceder que dos servidores se bloqueasen mutuamente al intentar sicronizarse unos con otros si se forma un anillo. Ninguno llegara a enviar o recibir.               <br>
     *                                                                                                                                                                                <br>
     * 1º Sin anidamiento - Grupo y_ en modo lectura.                                                                                                                                 <br>
     * 2º Con anidamiento - Grupo a_ Escritura  Bloqueo general. Evita modificar ficheros durante la sincronizacion ademas de que se acceden a variables del grupo.                   <br>
     *                      Grupo z_ Escritura. Modifica la estructura que tiene los ficheros del almacen.                                                                            <br>
     *                                                                                                                                                                                <br>
     * Casos a controlar:                                                                                                                                                             <br>
     *    Caso 1: En ambos lados. Ninguno como borrado.    Gana la version más nueva.                                                                                                 <br>
     *    Caso 2: En ambos lados. Uno como borrado.        Gana la version más nueva. O se borra el otro o se copia fichero quitando la marca de borrado.                             <br>
     *    Caso 3: En un    lado.  No marcado como borrado. Se replica a donde no esta.                                                                                                <br>
     *    Caso 4: En un    lado.  Marcado como borrado.    Se quita la entrada del map que tiene la entrada de borrado.                                                               <br>
     */
    private void sincronizarFicheros(){
    /*VAR*/
        Map<String, AlmacenInterfaceRemote> almacenes_activos              = new HashMap<String, AlmacenInterfaceRemote>();        
        Map<String, FicheroInfo>            ficheros_del_otro              =  null;                                             //Se asume que la key siempre se hace igual de forma que va en funcion del nombre del directorio y el fichero de forma que para una replica de un fichero en dos servidores ambos producen la misma key.
        Set<String>                         set_ids_almacen                =  null;
        Iterator                            itr_ids_almacen                =  null;
        AlmacenInterfaceRemote              almacen_value                  =  null;
        String                              id_almacen_key                 =    "";
        String                              password_administracion        =    "";
        String                              password_lectura               =    "";
        int                                 control_remote_exception       =     0;
        boolean                             acepta_sincronizarse           = false; 
    /*BEGIN*/
        this.lock_grupo_w.quieroLeer();
            password_administracion = this.w_password_administracion;
            password_lectura        = this.w_password_lectura;
        this.lock_grupo_w.dejoDeLeer();
        
        //Se pasa a otra estructura los almacenes a usar por si son sacados del map del grupo y_. 
        //Se asume que los maps del grupo y_ pueden cambiar pero no los objetos almacenados
        this.lock_grupo_y.quieroLeer();
            set_ids_almacen = this.y_almacenes_comunicacion_activa.keySet();    
            itr_ids_almacen = set_ids_almacen.iterator();
            while ( itr_ids_almacen.hasNext() ){
                id_almacen_key = (String)itr_ids_almacen.next();
                almacen_value  = this.y_almacenes_comunicacion_activa.get(id_almacen_key);
                almacenes_activos.put(id_almacen_key, almacen_value);
            }/*while*/
        this.lock_grupo_y.dejoDeLeer();
        
        //Ahora se recorren cada uno de los almacenes de la estructura copiada para pedirles su lista de ficheros. 
        //Necesario bloquear todos los grupos que son accedidos.
        this.lock_grupo_a.quieroEscribir();                                                                                 //El grupo a_ debe ser el primero, entrando en a_ se entra en todos tarde o temprano.
        this.lock_grupo_z.quieroEscribir();                                                                                 //Para modificar la estrucutura que tiene los ficheros del almacen.
        if (!this.z_sincronizacion_aceptada){                                                                               //No se puede iniciar la sincronizacion si se le ha dicho a otro servidor que se puede sincronizar con nosotros. Puede producir interbloque entre los servidores. Y como el grupo z_ esta bloqueado no se le dara permiso a nadie mientras dura la sincronizacion.
            set_ids_almacen = almacenes_activos.keySet();    
            itr_ids_almacen = set_ids_almacen.iterator();
            while ( itr_ids_almacen.hasNext() ){
                this.z_sincronizacion_rechazada = false;                                                                    //Antes de empezar se pone a false. Si se pone a true ayuda a los procedimientos a salir para dejar de trabajar con el servidor actual y buscar el siguiente.
                control_remote_exception        =     0;
                id_almacen_key                  = (String)itr_ids_almacen.next();
                almacen_value                   = almacenes_activos.get(id_almacen_key);                                    //Almacen al que hay que pedirle su lista de ficheros.
                this.log("sincronizarFicheros. Intentando sincronizar ficheros con: " + id_almacen_key, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                
                try {
                    acepta_sincronizarse = almacen_value.sincronizarSolicitarIniciar(password_administracion, this.id);     //Se le debe pedir permiso. Debe anotarnos para que acepte nustras peticiones mas tarde.
                }catch (RemoteException ex) {
                    acepta_sincronizarse = false;                                                                           //Como si hubiera dicho que no.
                    this.log("sincronizarFicheros. RemoteException. Producida por sincronizarSolicitarIniciar(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                }/*try-catch*/
                
                if (acepta_sincronizarse){
                    try {
                        this.log("sincronizarFicheros. Inicio     sincronizar ficheros con: " + id_almacen_key, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                        ficheros_del_otro = almacen_value.ficherosDelAlmacen(password_lectura);                             control_remote_exception = 1;
                        if (ficheros_del_otro != null){
                            this.enviarFicherosQueLeFaltan (password_administracion, almacen_value, ficheros_del_otro);     //Accede al grupo_z en modo lectura.    Gestiona sus excepciones, podría terminar su ejecucion sin realizar la sincronización. 
                            this.traerFicherosQueFaltan    (password_administracion, almacen_value, ficheros_del_otro);     //Accede al grupo_z en modo escritura.  Gestiona sus excepciones, podría terminar su ejecucion sin realizar la sincronización. 
                            this.comunesEnviarTraer        (password_administracion, almacen_value, ficheros_del_otro);     //Accede al grupo_z en modo escritura.  Gestiona sus excepciones, podría terminar su ejecucion sin realizar la sincronización. 
                        }/*if*/
                        this.log("sincronizarFicheros. Fin        sincronizar ficheros con: " + id_almacen_key, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    }catch (RemoteException ex){
                        switch (control_remote_exception){
                            case 0:
                                this.log("sincronizarFicheros. RemoteException. Producida por ficherosDelAlmacen(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                                //Se ignora y se pasa al siguiente almacen de la lista.
                            break;
                        }/*switch*/
                    }/*try-catch*/
                    
                    try {
                        almacen_value.sincronizarInformarFinalizar(password_administracion);
                    }catch (RemoteException ex) {
                        this.log("sincronizarFicheros. RemoteException. Producida por sincronizarInformarFinalizar(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                        //Se ignora. Se va a coger otro servidor. La sesion se le puede quedar cogida, ya le caducara.
                    }/*try-catch*/
                }else{
                    this.log("sincronizarFicheros. Sincronizacion rechazada por:        " + id_almacen_key, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                }/*if*/
            }/*while*/
        }/*if*/
        this.lock_grupo_z.dejoDeEscribir();
        this.lock_grupo_a.dejoDeEscribir();
    }/*END sincronizarFicheros*/


        /**
         * Compara los ficheros de este servidor con los ficheros del otro servidor de forma que enviara al otro los que aqui tengamos que el no tenga.            <br>
         * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo a en modo lectura.                             <br>
         * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo z en modo lectura.                             <br>
         *                                                                                                                                                         <br>
         * Casos a controlar:                                                                                                                                      <br>
         *    Caso 1: En ambos lados. Ninguno como borrado.    Gana la version más nueva.                                                                          <br>
         *    Caso 2: En ambos lados. Uno como borrado.        Gana la version más nueva. O se borra el otro o se copia fichero quitando la marca de borrado.      <br>
         *    Caso 3: En un    lado.  No marcado como borrado. Se replica a dond no esta.                                                                          <br>
         *    Caso 4: En un    lado.  Marcado como borrado.    Se quita la entrada del map que tiene la entrada de borrado.                                        <br>
         * 
         * @param password_administracion  La contraseña de administracion que es necesaria para llamar a algunos métodos de los que se invocan.
         * @param almacen_otro             El dueño de la lista de ficheros que se envia y al que habra que enviarle ficheros
         * @param ficheros_del_otro        La lista de ficheros del otro servidor
         */
        private void enviarFicherosQueLeFaltan(String password_administracion, AlmacenInterfaceRemote almacen_otro, Map<String, FicheroInfo> ficheros_del_otro){
        /*VAR*/
            Map<String, FicheroInfo>            ficheros_que_el_otro_no_tiene  =  null;
            Set<String>                         sets_ids_fichero               =  null;
            Iterator                            itr_ids_fichero                =  null;
            String                              key_id                         =  null;
            FicheroInfo                         fichero_que_le_falta           =  null;
            boolean                             borrado                        = false;
        /*BEGIN*/
            ficheros_que_el_otro_no_tiene  = this.mapResta(this.z_ficheros_del_almacen, ficheros_del_otro);            //Devuelve referncias del primer parametro.
            sets_ids_fichero               = ficheros_que_el_otro_no_tiene.keySet();
            itr_ids_fichero                = sets_ids_fichero.iterator();
            while ( itr_ids_fichero.hasNext() && !this.z_sincronizacion_rechazada ){
                key_id               = (String)itr_ids_fichero.next();
                fichero_que_le_falta = ficheros_que_el_otro_no_tiene.get(key_id);
                borrado              = fichero_que_le_falta.getBorrado();
                if (borrado){                                                                                          //Case 4: Yo lo tengo marcado como borrado y el otro no lo tiene. Se debe borrar de la estructura.
                    this.log("enviarFicherosQueLeFaltan. Fichero: " + key_id + ". Borrado: " + borrado + ". Case 4. Se borra de mi estructura.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    this.borrarFicheroDeMiEstructura(key_id);
                }else{                                                                                                 //Caso 3: Yo lo tengo y el no. Se lo debo enviar.
                    this.log("enviarFicherosQueLeFaltan. Fichero: " + key_id + ". Borrado: " + borrado + ". Case 3. Se le envia el fichero."   , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    this.enviarFichero(password_administracion, almacen_otro, fichero_que_le_falta);
                }/*if*/
            }/*while*/
        }/*END enviarFicherosQueLeFaltan*/
        
        
        /**
         * Compara los ficheros de este servidor con los ficheros del otro servidor de forma que se traera los que el otro servidor tenga que aqui aun no esten.   <br>
         * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo a en modo lectura.                             <br>
         * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo z en modo escritura.                           <br>
         *                                                                                                                                                         <br>
         * Casos a controlar:                                                                                                                                      <br>
         *    Caso 1: En ambos lados. Ninguno como borrado.    Gana la version más nueva.                                                                          <br>
         *    Caso 2: En ambos lados. Uno como borrado.        Gana la version más nueva. O se borra el otro o se copia fichero quitando la marca de borrado.      <br>
         *    Caso 3: En un    lado.  No marcado como borrado. Se replica a dond no esta.                                                                          <br>
         *    Caso 4: En un    lado.  Marcado como borrado.    Se quita la entrada del map que tiene la entrada de borrado.                                        <br>
         * 
         * @param password_administracion  La contraseña de administracion que es necesaria para llamar a algunos métodos de los que se invocan.
         * @param almacen_otro             El dueño de la lista de ficheros que se envia y al que habra que solicitarle ficheros
         * @param ficheros_del_otro        La lista de ficheros del otro servidor
         */ 
        private void traerFicherosQueFaltan(String password_administracion, AlmacenInterfaceRemote almacen_otro, Map<String, FicheroInfo> ficheros_del_otro){
        /*VAR*/
            Map<String, FicheroInfo>            ficheros_del_otro_que_no_tengo =  null;
            Set<String>                         sets_ids_fichero               =  null;
            Iterator                            itr_ids_fichero                =  null;
            String                              key_id                         =  null;
            FicheroInfo                         fichero_que_falta              =  null;
            boolean                             borrado                        = false;
            boolean                             ok                             = false;
        /*BEGIN*/
            ficheros_del_otro_que_no_tengo = this.mapResta(ficheros_del_otro,  this.z_ficheros_del_almacen);                                    //Devuelve referncias del primer parametro.
            sets_ids_fichero               = ficheros_del_otro_que_no_tengo.keySet();
            itr_ids_fichero                = sets_ids_fichero.iterator();
            while ( itr_ids_fichero.hasNext() && !this.z_sincronizacion_rechazada ){                                                            //Casos 3 y 4 implican comunicacion con el otro. Si nos rechaza la comunicacion no hay motivos para seguir en el bucle intentandolo con otros ficheros.
                key_id            = (String)itr_ids_fichero.next();
                fichero_que_falta = ficheros_del_otro_que_no_tengo.get(key_id);
                borrado           = fichero_que_falta.getBorrado();
                if (borrado){                                                                                                                   //Case 4: Yo no lo tengo y el otro lo tiene marcado como borrado. Se le debe decir que lo borre de su estructura.
                    this.log("traerFicherosQueFaltan. Fichero: " + key_id + ". Borrado: " + borrado + ". Case 4. Se borra de su estructura.", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    ok = this.borrarFicheroDeSuEstructura(password_administracion, almacen_otro, key_id);
                }else{                                                                                                                          //Caso 3: Yo no lo tengo y el si. Me lo debo traer.
                    this.log("traerFicherosQueFaltan. Fichero: " + key_id + ". Borrado: " + borrado + ". Case 3. Se trae el fichero."       , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    ok = this.traerFichero(password_administracion, almacen_otro, fichero_que_falta);
                    if (ok){
                        this.anotarFicheroEnMiEstructura(key_id, fichero_que_falta);
                    }/*if*/
                }/*if*/
            }/*while*/
        }/*END traerFicherosQueFaltan*/
        

        /**
         * Compara los ficheros de este servidor con los ficheros del otro servidor.                                                                               <br>
         * Ahora solo se fija en los comunes, en los que estan en los dos servidores pero donde la version puede cambiar.                                          <br>
         * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo a en modo escritura.                           <br>
         * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo z en modo escritura.                           <br>
         *                                                                                                                                                         <br>
         * Casos a controlar:                                                                                                                                      <br>
         *    Caso 1: En ambos lados. Ninguno como borrado.    Gana la version más nueva.                                                                          <br>
         *    Caso 2: En ambos lados. Uno como borrado.        Gana la version más nueva. O se borra el otro o se copia fichero quitando la marca de borrado.      <br>
         *    Caso 3: En un    lado.  No marcado como borrado. Se replica a dond no esta.                                                                          <br>
         *    Caso 4: En un    lado.  Marcado como borrado.    Se quita la entrada del map que tiene la entrada de borrado.                                        <br>
         * 
         * @param password_administracion  La contraseña de administracion que es necesaria para llamar a algunos métodos de los que se invocan.
         * @param almacen_otro             El dueño de la lista de ficheros que se envia y al que habra que enviarle ficheros
         * @param ficheros_del_otro        La lista de ficheros del otro servidor
         */
        private void comunesEnviarTraer(String password_administracion, AlmacenInterfaceRemote almacen_otro, Map<String, FicheroInfo> ficheros_del_otro){
        /*VAR*/
            Map<String, FicheroInfo>            ficheros_comunes_con_el_otro   =  null;
            Set<String>                         sets_ids_fichero               =  null;
            Iterator                            itr_ids_fichero                =  null;
            String                              key_id                         =  null;
            FicheroInfo                         fichero_del_otro               =  null;
            FicheroInfo                         fichero_mio                    =  null;
            String                              version_del_otro               =    "";
            String                              version_mia                    =    "";
            boolean                             borrado_el_otro                = false;
            boolean                             borrado_el_mio                 = false;
            boolean                             ok                             = false;
            int                                 compara_version                =     0;
        /*BEGIN*/
            ficheros_comunes_con_el_otro = this.mapInterseccion(ficheros_del_otro,  this.z_ficheros_del_almacen);        //CUIDADO: Devuelve los comunes pero como referncias del primer parametro. Si se intercambia el orden de los parametros se compararan los ficheros de este almacen con sigo mismo.
            sets_ids_fichero             = ficheros_comunes_con_el_otro.keySet();
            itr_ids_fichero              = sets_ids_fichero.iterator();
            while ( itr_ids_fichero.hasNext() && !this.z_sincronizacion_rechazada ){                                     //Mientras haya ficheros para trabajar y el servidor con el que nos estamos sincronizando no nos haya quitado el psermiso
                key_id              = (String)itr_ids_fichero.next();                                                    //Se usara la interseccion para recorrer los ids. El FicheroInfo se coge de cada correspondiente map. 
                fichero_del_otro    = ficheros_del_otro.get           (key_id);
                fichero_mio         = this.z_ficheros_del_almacen.get (key_id);
                version_del_otro    = fichero_del_otro.getVersion ();
                version_mia         = fichero_mio.getVersion      ();
                borrado_el_otro     = fichero_del_otro.getBorrado ();
                borrado_el_mio      = fichero_mio.getBorrado      ();
                compara_version     = version_del_otro.compareTo(version_mia);                                           //Negativo: el otro mas antiguo. Positivo: el otro mas nuevo.
                
                if ( !borrado_el_otro && !borrado_el_mio){                                                               //Caso 1. Ninguno borrado.
                    if (compara_version < 0){                                                                            //La version del otro es menor que la mia.      Se lo debo enviar.
                        this.log("comunesEnviarTraer. Fichero: " + key_id + ". Borrado el otro: " + borrado_el_otro + ". Borrado el mio: " + borrado_el_mio + ". Version: " + compara_version + ", el otro es mas antiguo. Case 1. Se le envia el fichero."    , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                        ok = this.enviarFichero(password_administracion, almacen_otro, fichero_mio);
                    }else if (compara_version > 0){                                                                      //La version mia      es menor que la del otro. Me lo debo traer.
                        this.log("comunesEnviarTraer. Fichero: " + key_id + ". Borrado el otro: " + borrado_el_otro + ". Borrado el mio: " + borrado_el_mio + ". Version: " + compara_version + ", el otro es mas nuevo.   Case 1. Se trae el fichero."        , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                        ok = this.traerFichero(password_administracion, almacen_otro, fichero_del_otro);
                        if (ok){
                            this.anotarFicheroEnMiEstructura(key_id, fichero_del_otro);
                        }/*if*/
                    }/*if*/
                }else if ( !borrado_el_otro && borrado_el_mio){                                                          //Caso 2. No borrado el otro. Borrado el mio.
                    if (compara_version <= 0){                                                                           //La version del otro es menor que la mia.      Gano yo.     Debe borrar  su fichero.
                        this.log("comunesEnviarTraer. Fichero: " + key_id + ". Borrado el otro: " + borrado_el_otro + ". Borrado el mio: " + borrado_el_mio + ". Version: " + compara_version + ", el otro es mas antiguo. Case 2. Debe borrar su fichero."    , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                        ok = this.borrarSuFichero(password_administracion, almacen_otro, fichero_del_otro);
                    }else if (compara_version > 0){                                                                      //La version mia      es menor que la del otro. Gana el.     Me lo debo traer.
                        this.log("comunesEnviarTraer. Fichero: " + key_id + ". Borrado el otro: " + borrado_el_otro + ". Borrado el mio: " + borrado_el_mio + ". Version: " + compara_version + ", el otro es mas nuevo.   Case 2. Se trae el fichero."        , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                        ok = this.traerFichero(password_administracion, almacen_otro, fichero_del_otro);
                        if (ok){
                            this.anotarFicheroEnMiEstructura(key_id, fichero_del_otro);
                        }/*if*/
                    }/*if*/
                }else if ( borrado_el_otro && !borrado_el_mio){                                                          //Caso 2. Borrado el otro. No borrado el mio.
                    if (compara_version < 0){                                                                            //La version del otro es menor que la mia.      Gano yo. Se lo debo enviar.
                        this.log("comunesEnviarTraer. Fichero: " + key_id + ". Borrado el otro: " + borrado_el_otro + ". Borrado el mio: " + borrado_el_mio + ". Version: " + compara_version + ", el otro es mas antiguo. Case 2. Se le envia el fichero."    , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                        ok = this.enviarFichero(password_administracion, almacen_otro, fichero_mio);
                    }else if (compara_version >= 0){                                                                     //La version mia      es menor que la del otro. Gana el. Debo borrar   mi fichero.
                        this.log("comunesEnviarTraer. Fichero: " + key_id + ". Borrado el otro: " + borrado_el_otro + ". Borrado el mio: " + borrado_el_mio + ". Version: " + compara_version + ", el otro es mas nuevo.   Case 2. Debo borrar mi fichero."    , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                        ok = this.borrarMiFichero(fichero_mio);
                        if (ok){
                            this.borrarFicheroDeMiEstructura(key_id);
                        }/*if*/                        
                    }/*if*/
                }/*if*/
            }/*while*/
        }/*END comunesEnviarTraer*/

        
        /**
         * Toma el rol de Cliente.                                                                                                                  <br>
         * Escribe el fichero en disco que se debe traer del servidor que se le proporciona.                                                        <br>
         * Si la sincronizacion se realiza en cualquier momento se debe tener en cuenta que el fichero no puede estar siendo leido o escrito.       <br>
         * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede a los grupo 'a' y 'z' en modo lectura.   <br>
         * 
         * @param  password_administracion  La contraseña de administracion que es necesaria para llamar a algunos métodos de los que se invocan.
         * @param  almacen                  El servidor desde el que hay que traerse el fichero. 
         * @param  fichero_a_traer          El fichero a traer.
         * @return boolean                  true si la operacion se concluye sin problemas. Por ahora no parece necesario un nivel detallado del resultado.
         */
        private boolean traerFichero(String password_administracion, AlmacenInterfaceRemote almacen, FicheroInfo fichero_a_traer){
        /*VAR*/
            List<FicheroPaquete>  fichero_paquetes_datos     =  null;
            String                str_directorio_fichero     =    "";
            String                str_nombre_fichero         =    "";
            String                id_lectura                 =    "";
            Integer               lector_o_escritor          =     0;
            char[]                id_lectura_array_char      =  null;
            int                   tamano_paquete             =     0;
            int                   control_remote_exception   =     0;
            int                   resultado1                 =     0;
            int                   resultado2                 =     0;
            boolean               finalizar_lectura          = false;
            boolean               escribir_en_disco          = false;
            boolean               ok                         = false;
        /*BEGIN*/
            str_directorio_fichero = fichero_a_traer.getDirectorioRelativo ();
            str_nombre_fichero     = fichero_a_traer.getNombreFichero      ();
            tamano_paquete         = this.z_tamano_paquete_traer;
            id_lectura             = this.idParaFichero(str_directorio_fichero, str_nombre_fichero);
            lector_o_escritor      = this.a_lector_o_escritor.get(id_lectura);                        //El fichero no puede estar en uso.
            if (lector_o_escritor == null){                               
                try{
                    id_lectura            = almacen.sincronizarSolicitarLeerFicheroDelServidor(password_administracion, this.id, str_directorio_fichero, str_nombre_fichero, tamano_paquete);              control_remote_exception = 1;
                    id_lectura_array_char = id_lectura.toCharArray();
                    if ( (id_lectura_array_char != null) && (id_lectura_array_char.length > 0) && (id_lectura_array_char[0] != '-') ){
                        fichero_paquetes_datos = almacen.sincronizarLeyendoFicheroDelServidorLeerPaquetes      (password_administracion, this.id, id_lectura);                                              control_remote_exception = 2;
                        resultado1             = almacen.sincronizarLeyendoFicheroDelServidorLecturaFinalizada (password_administracion, this.id, id_lectura);                                              control_remote_exception = 3;
                        escribir_en_disco      = true;
                    }/*if*/
                }catch (RemoteException ex){
                    switch(control_remote_exception){
                        case 0:
                            this.log("traerFichero. RemoteException. Producida por sincronizarSolicitarLeerFicheroDelServidor(). "            , ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                            //Generado por sincronizarSolicitarLeerFicheroDelServidor.             Se ignora la lectura.
                        break;
                        case 1:
                            this.log("traerFichero. RemoteException. Producida por sincronizarLeyendoFicheroDelServidorLeerPaquetes(). "      , ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                            //Generado por sincronizarLeyendoFicheroDelServidorLeerPaquetes.       Se ignora y se da por finalizada la lectura. 
                            finalizar_lectura = true;
                        break;
                        case 2:
                            this.log("traerFichero. RemoteException. Producida por sincronizarLeyendoFicheroDelServidorLecturaFinalizada()a. ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                            //Generado por sincronizarLeyendoFicheroDelServidorLecturaFinalizada.  La lectura de datos se hizo correctamente con lo que se debe escribir a disco. Y se vuelve a intentar finalizar la lectura. 
                            finalizar_lectura = true;
                        break;
                    }/*switch*/
                }/*try-catch*/

                if ( (escribir_en_disco) && (fichero_paquetes_datos != null) ){
                    FicherosTrabajar.escribirFichero(this.directorio_base + str_directorio_fichero, str_nombre_fichero, fichero_paquetes_datos);
                    ok = true;
                }/*if*/
                if (finalizar_lectura){
                    try{
                        resultado2 = almacen.sincronizarLeyendoFicheroDelServidorLecturaFinalizada(password_administracion, this.id, id_lectura);
                    }catch (RemoteException ex){
                        this.log("traerFichero. RemoteException. Producida por sincronizarLeyendoFicheroDelServidorLecturaFinalizada()b. ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                        //Si falla la finalización puede ser porque se perdio la conexion con el servidor. Ya caducara la sesion y la cancelara sola.
                    }/*try-catch*/
                }/*if*/
                procesarSincronizacionRechazada(id_lectura, "", "", resultado1, resultado2, 0);
            }/*if*/
            return ok;
        }/*END traerFichero*/

        
        /**
         * Toma el rol de Cliente.                                                                                                                  <br>
         * Lee el fichero de disco y lo envia al servidor que se le proporciona.                                                                    <br>
         * Si la sincronizacion se realiza en cualquier momento se debe tener en cuenta que el fichero no puede estar siendo escrito.               <br>
         * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede a los grupo 'a' y 'z' en modo lectura.   <br>
         * 
         * @param  password_administracion  La contraseña de administracion que es necesaria para llamar a algunos métodos de los que se invocan.
         * @param  almacen                  El servidor al que hay que enviarle el fichero. 
         * @param  fichero_a_enviar         El fichero a enviar.
         * @return boolean                  true si la operacion se concluye sin problemas. Por ahora no parece necesario un nivel detallado del resultado.
         */
        private boolean enviarFichero(String password_administracion, AlmacenInterfaceRemote almacen, FicheroInfo fichero_a_enviar){
        /*VAR*/
            List<FicheroPaquete>                fichero_paquetes_datos     =  null;
            String                              str_directorio_fichero     =    "";
            String                              str_nombre_fichero         =    "";
            String                              str_version_fichero        =    "";
            String                              id_escritura               =    "";
            Integer                             lector_o_escritor          =     0;
            char[]                              id_escritura_array_char    =  null;
            int                                 tamano_paquete             =     0;
            int                                 cantidad_paquetes          =     0;
            int                                 control_remote_exception   =     0;
            int                                 resultado1                 =     0;
            int                                 resultado2                 =     0;
            int                                 resultado3                 =     0;
            boolean                             cancelar_envio             = false;
            boolean                             se_puede_enviar            = false;
            boolean                             ok                         = false;
        /*BEGIN*/
            str_directorio_fichero     = fichero_a_enviar.getDirectorioRelativo ();
            str_nombre_fichero         = fichero_a_enviar.getNombreFichero      ();
            str_version_fichero        = fichero_a_enviar.getVersion            ();
            tamano_paquete             = this.z_tamano_paquete_enviar;
            id_escritura               = this.idParaFichero(str_directorio_fichero, str_nombre_fichero);
            lector_o_escritor          = this.a_lector_o_escritor.get(id_escritura);

            if ( (lector_o_escritor == null) || (lector_o_escritor > 0) ){      //El fichero no puede estar siendo escrito.
                se_puede_enviar = true;
            }/*if*/
            
            if ( se_puede_enviar && FicherosTrabajar.exiteFichero(this.directorio_base + str_directorio_fichero, str_nombre_fichero) ){
                fichero_paquetes_datos = FicherosTrabajar.leerFichero(this.directorio_base + str_directorio_fichero, str_nombre_fichero, tamano_paquete);
                if (fichero_paquetes_datos != null){
                    cantidad_paquetes = fichero_paquetes_datos.size();
                    try{
                        id_escritura            = almacen.sincronizarSolicitarEscribirFicheroEnServidor(password_administracion, this.id, str_directorio_fichero, str_nombre_fichero, str_version_fichero, tamano_paquete, cantidad_paquetes);      control_remote_exception = 1;
                        id_escritura_array_char = id_escritura.toCharArray();
                        if ( (id_escritura_array_char != null) && (id_escritura_array_char.length > 0) && (id_escritura_array_char[0] != '-') ){
                            resultado1 = almacen.sincronizarEscribiendoFicheroEnServidorEnviandoPaquetes (password_administracion, this.id, id_escritura, fichero_paquetes_datos);                                                                  control_remote_exception = 2;
                            resultado2 = almacen.sincronizarEscribiendoFicheroEnServidorEnvioFinalizado  (password_administracion, this.id, id_escritura);                                                                                          control_remote_exception = 3;
                            ok         = true;                                                                                                                 //Llegar aqui no quiere decir que todo fuera bien por eso luego se ejecuta procesarSincronizacionRechazada()
                        }/*if*/
                    }catch (RemoteException ex){
                        switch(control_remote_exception){
                            case 0:
                                this.log("enviarFichero. RemoteException. Producida por sincronizarSolicitarEscribirFicheroEnServidor(). "          , ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                                //Generado por sincronizarSolicitarEscribirFicheroEnServidor.           Se da por finalizado, no se intenta enviar.
                            break;
                            case 1:
                                this.log("enviarFichero. RemoteException. Producida por sincronizarEscribiendoFicheroEnServidorEnviandoPaquetes(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                                //Generado por sincronizarEscribiendoFicheroEnServidorEnviandoPaquetes. Se da por finalizado, no se intenta enviar y se debe cancelar.
                                cancelar_envio = true;
                            break;
                            case 2:
                                this.log("enviarFichero. RemoteException. Producida por sincronizarEscribiendoFicheroEnServidorEnvioFinalizado(). " , ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                                //Generado por sincronizarEscribiendoFicheroEnServidorEnvioFinalizado.  Aunque se ha enviado todo ha habido algun problema y por evitar males mayores se intentara cancelar.
                                cancelar_envio = true;
                            break;                                
                        }/*switch*/
                    }/*try-catch*/
                    if (cancelar_envio){
                        try{
                            resultado3 = almacen.sincronizarEscribiendoFicheroEnServidorCancelar(password_administracion, this.id, id_escritura);
                        }catch (RemoteException ex){
                            this.log("enviarFichero. RemoteException. Producida por sincronizarEscribiendoFicheroEnServidorCancelar(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                            //Si cancelar envio falla puede ser porque se perdio la conexion con el servidor. Ya caducara la sesion y la cancelara el mismo.
                        }/*try-catch*/
                    }/*if*/
                    procesarSincronizacionRechazada(id_escritura, "", "", resultado1, resultado2, resultado3);
                }/*if*/
            }/*if*/
            return ok;
        }/*END enviarFichero*/

        
        /**
         * Toma el rol de Cliente.                                                                                                                   <br>
         * Le dice que debe sacar de su estrucutra de ficheros del almacen un cierto id.                                                             <br>
         * Pondra z_sincronizacion_rechazada a true si al realizar la llamada se informa de que ya no hay permiso para seguir con la sincronizacion. <br>
         * 
         * @param  password_administracion  La contraseña de administracion que es necesaria para llamar a algunos métodos de los que se invocan.
         * @param  almacen                  El servidor al que hay que borrarle el fichero. 
         * @param  fichero_a_enviar         El fichero a borrar.
         * @return boolean                  true si la operacion se concluye sin problemas. Por ahora no parece necesario un nivel detallado del resultado.
         */        
        private boolean borrarFicheroDeSuEstructura(String password_administracion, AlmacenInterfaceRemote almacen, String id_fichero_sacar){
        /*VAR*/
            int     resultado  =     0;
            boolean ok         = false;
        /*BEGIN*/
            try{
                resultado = almacen.sincronizarEliminarDeFicherosDeLaEstructura(password_administracion, this.id, id_fichero_sacar);                      //Aqui se le dice al otro almacen que debe quitar el id de z_ficheros_del_almacen. Podría rechazar lo operacion.
                ok        = (resultado == 0);
            }catch (RemoteException ex){
                this.log("borrarFicheroDeSuEstructura. RemoteException. Producida por sincronizarEliminarDeFicherosDeLaEstructura(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                //Se ignora, ya se intentara la sincronizacion de este elemento en otro momento. 
            }/*try-catch*/
            procesarSincronizacionRechazada("", "", "", resultado, 0, 0);
            return ok;
        }/*END borrarFicheroDeSuEstructura*/
        
        
        /**
         * Toma el rol de Cliente.                                                                                                                   <br>
         * Borra el fichero enviado del servidor que se proporciona.                                                                                 <br>
         * Pondra z_sincronizacion_rechazada a true si al realizar la llamada se informa de que ya no hay permiso para seguir con la sincronizacion. <br>
         * 
         * @param  password_administracion  La contraseña de administracion que es necesaria para llamar a algunos métodos de los que se invocan.
         * @param almacen                   El servidor al que hay que enviarle el fichero. 
         * @param fichero_a_enviar          El fichero a borrar.
         * @return boolean                  true si la operacion se concluye sin problemas. Por ahora no parece necesario un nivel detallado del resultado.
         */
        private boolean borrarSuFichero(String password_administracion, AlmacenInterfaceRemote almacen, FicheroInfo fichero_a_borrar){
        /*VAR*/
            String  str_directorio_fichero     =    "";
            String  str_nombre_fichero         =    "";
            int     resultado                  =     0;
            boolean ok                         = false;
        /*BEGIN*/
            str_directorio_fichero = fichero_a_borrar.getDirectorioRelativo ();
            str_nombre_fichero     = fichero_a_borrar.getNombreFichero      ();
            try{
                resultado = almacen.sincronizarBorrarFicheroDelServidor(password_administracion, this.id, str_directorio_fichero, str_nombre_fichero);
                ok        = (resultado == 0);
            }catch (RemoteException ex) {
                this.log("borrarSuFichero. RemoteException. Producida por sincronizarBorrarFicheroDelServidor(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                //Se ignora, ya se intentara en otro momento. ok sera false.
            }/*try-cathc*/
            procesarSincronizacionRechazada("", "", "", resultado, 0, 0);
            return ok;
        }/*END borrarSuFichero*/

        
        /**
         * Para borrar un fichero del disco duro.                                                                                                   <br>
         * Si la sincronizacion se realiza en cualquier momento se debe tener en cuenta que el fichero no puede estar siendo leido o escrito.       <br>
         * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo 'a' en modo lectura.            <br>
         * 
         * @param  fichero_a_borrar El fichero que se debe borrar del disco.
         * @return boolean          true si la operacion se concluye sin problemas. Por ahora no parece necesario un nivel detallado del resltado.
         */
        private boolean borrarMiFichero(FicheroInfo fichero_a_borrar){
        /*VAR*/
            String  str_directorio_fichero     =    "";
            String  str_nombre_fichero         =    "";
            String  id_lectura_escritura       =    "";
            Integer lector_o_escritor          =     0;
            boolean ok                         = false;
        /*BEGIN*/
            str_directorio_fichero     = fichero_a_borrar.getDirectorioRelativo ();
            str_nombre_fichero         = fichero_a_borrar.getNombreFichero      ();
            id_lectura_escritura       = this.idParaFichero(str_directorio_fichero, str_nombre_fichero);
            lector_o_escritor = this.a_lector_o_escritor.get(id_lectura_escritura);
            if ( (lector_o_escritor == null) && (FicherosTrabajar.exiteFichero(this.directorio_base + str_directorio_fichero, str_nombre_fichero)) ){                                                                                                      //El fichero no puede estar en uso.
                FicherosTrabajar.borrarFichero(this.directorio_base + str_directorio_fichero, str_nombre_fichero);
                ok = true;
            }/*if*/
            return ok;
        }/*END borrarFichero*/
        
        
// METODOS PRIVADOS processos --------------------------------------------------

        
    /**
     * Una manera de tener localizados a los hilos que ejecutan las tareas de mantenimiento.                                            <br>
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo x_ en modo escritura.   <br>
     * Es necesario este procedimiento porque el pseudo-constructor es 'static' y no puede acceder a la variable directamente.          <br>
     * 
     * @param hilo            El hilo a almacenar
     * @param numero_del_hilo El numero asociado al hilo.
     */
    private void anotarHiloRun(Thread hilo, int numero_del_hilo){
    /*BEGIN*/
        this.x_procesos_run.put(numero_del_hilo, hilo);
    }/*END anotarHiloRun*/
        

    /**
     * Espera a que finalicen todas las operaciones de lectura y escritura.     <br>
     * Se hace aqui control de acceso                                           <br>
     */
    private void esperarFinalizarLecturasEscrituras(){
    /*VAR*/
        int     tiempo_dormir_esperar_finalizar   =     0;
        int     num_lectores                      =     1;
        int     num_escritores                    =     1;
        int     num_borrando                      =     1;
    /*BEGIN*/
        this.lock_grupo_x.quieroLeer();
            tiempo_dormir_esperar_finalizar = this.x_tiempo_dormir_esperar_finalizar;
        this.lock_grupo_x.dejoDeLeer();
        while ( (num_lectores + num_escritores + num_borrando) > 0 ){
            try{
                this.log("esperarFinalizarLecturasEscrituras. Esperando a lectores, escritores y borradores. ", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                Thread.sleep(tiempo_dormir_esperar_finalizar);
            }catch (InterruptedException ie){
                this.log("esperarFinalizarLecturasEscrituras. InterruptedException. Producida por sleep(). ", ie.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                //No se hace nada, no se duerme y se reptie el bucle.
            }/*try-catch*/
            this.lock_grupo_b.quieroLeer();
                num_lectores = this.b_lecturas.size();
            this.lock_grupo_b.dejoDeLeer();
            this.lock_grupo_c.quieroLeer();
                num_escritores = this.c_escrituras.size();
                num_borrando   = this.c_borrando;
            this.lock_grupo_c.dejoDeLeer();
        }/*while*/
    }/*END esperarFinalizarLecturasEscrituras*/
    
        
    /**
     * Se espera a que terminen todos los hilos de las tareas de mantenimiento.                                                              <br>
     * Para esto, primero los despeirta pues los tiempo de dormir pueden ser muy largos.                                                     <br>
     * Pero para despertarlos se deben tener en cuenta los cuatro posibles casos:                                                            <br>
     *    trabajando==false   durmiendo=false   Transicion. Termino de trabajar pero aun no fue a dormir.                                    <br>
     *    trabajando==true    durmiendo=true    Transicion. Termino de dormir   pero aun no fue a trabajar.                                  <br>
     *    trabajando==true    durmiendo=false   Trabajando                                                                                   <br>
     *    trabajando==false   durmiendo=true    Durmiendo                                                                                    <br>
     * Solo se interrumpira si esta durmiendo.                                                                                               <br>
     * El resto de los casos se ignoran sin preocupaciones pues al ya estar fin=true ni ira a dormir ni ira a trabajar                       <br>
     * Simplemente si esta trabajando habra que esperar a que termine.                                                                       <br>
     * Se debe hacer las preguntas con el grupo x_ bloqueado para evitar que lo leido sea modificado y se tome una decision que ya no valga. <br>
     *                                                                                                                                       <br>
     * Se hace aqui control de acceso. Solo se bloquea el acceso a la variable, los justo inmprescindible.                                   <br>
     * Se procura bloquear lo minimo aunque implique muchos lock_ pues run() necesita acceder a x_ para avanzar.                             <br>
     * Y si run() no trabaja sería como quedarse esperando aqui indefinidamente.                                                             <br>
     */
    private void esperarFinalizarRun(){
    /*VAR*/
        Set<Integer> set_num_hilos                     =  null;
        Iterator     itr_num_hilos                     =  null;
        Integer      num_hilo                          =  null;
        Thread       hilo                              =  null;
        int          tiempo_dormir_esperar_finalizar   =     0;
        int          tiempo_dormir_esperar_interrumpir =     0;
        int          tiempo_dormir_esperar_join        =     0;
        int          num_max_hilos                     =     0;
        int          i                                 =     0;
        boolean      todos_finalizados                 =  true;
        boolean      dormido                           = false;
        boolean      trabajando                        = false;
    /*BEGIN*/
        this.lock_grupo_x.quieroLeer();                                         //Solo se bloquea el acceso a la variable. No se bloquea un while entero ni el codigo del procedimiento porque run accede a x_ y no se le dejaría trabajar.
            set_num_hilos                     = this.x_procesos_run.keySet();
            tiempo_dormir_esperar_finalizar   = this.x_tiempo_dormir_esperar_finalizar;
            tiempo_dormir_esperar_interrumpir = this.x_tiempo_dormir_esperar_interrumpir;
            tiempo_dormir_esperar_join        = this.x_tiempo_dormir_esperar_join;
        this.lock_grupo_x.dejoDeLeer();
        
        //Primero despierta a los que estan durmiendo.
        itr_num_hilos = set_num_hilos.iterator();
        while ( itr_num_hilos.hasNext() ){
            this.lock_grupo_x.quieroLeer();                                     //Mientras esta el grupo x_ bloqueado no puede cambiar el valor de durmiendo y trabajando. Despues de modificar el valor ejecuta un if(!fin) que le impedira trabajar o dormir.
            num_hilo   = (Integer)itr_num_hilos.next();
            hilo       = this.x_procesos_run.get(num_hilo);
            dormido    = this.x_procesos_durmiendo [num_hilo];
            trabajando = this.x_procesos_trabajando[num_hilo];
            if (dormido && !trabajando){                                        //Solo si esta profundamente dormido se interrumpe. Con fin==true ya ni trabajara ni dormira y se le deja que continue.
                this.log("esperarFinalizarRun. Interrumpiendo al hilo numero: " + num_hilo, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                try{
                    Thread.sleep(tiempo_dormir_esperar_interrumpir);            //Existe una minima posibilidad de que trabajando==false && durmiendo==true sin estar durmiendo. Durmiendo se pone a true justo antes de ir a dormir. Con una espera de 1 segundo se entiende que esa posibilidad desaparece casi por completo.
                }catch (InterruptedException ie){
                    this.log("esperarFinalizarRun. InterruptedException.  Producida en sleep() antes de ejecutar interrupt. ", ie.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    //Se ignora.
                }/*try-catch*/
                hilo.interrupt();
            }/*if*/
            this.lock_grupo_x.dejoDeLeer();            
        }/*while*/
        
        //Ahora se espera con join a que terminen.
        itr_num_hilos = set_num_hilos.iterator();
        while ( itr_num_hilos.hasNext() ){
            num_hilo = (Integer)itr_num_hilos.next();
            this.lock_grupo_x.quieroLeer();
                hilo = this.x_procesos_run.get(num_hilo);
            this.lock_grupo_x.dejoDeLeer();
            try{
                this.log("esperarFinalizarRun. Esperando al hilo numero: " + num_hilo + ". "                                 , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                hilo.join(tiempo_dormir_esperar_join);                          //Si no hay problemas la espera se hace aqui. Si pasa mucho tiempo se continua con el riesgo de dejar la tarea a medias.
                this.log("esperarFinalizarRun. O finalizo  el hilo numero: " + num_hilo + " o se agoto el tiempo de espera. ", "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            }catch (InterruptedException ex) {
                todos_finalizados = false;
                num_max_hilos     = this.x_proceso_a_ejecutar_max;
                this.log("esperarFinalizarRun. InterruptedException. Producida en join() en hilo numero: " + num_hilo, ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
            }/*try-catch*/
        }/*while*/
            
        //Si no se finalizo bien con join se espera de otra forma
        while ( !todos_finalizados ){                                                                        //Si hubo algún problema esperando a que finalizaran los hilos con join() entonces hay que mirar otra variable.
            this.log("esperarFinalizarRun. todos_finalizados: " + todos_finalizados, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
            try{
                Thread.sleep(tiempo_dormir_esperar_finalizar);                                               //Se duerme un tiempo antes de mirar si ya terminaron los run
            }catch (InterruptedException ie) {
                this.log("esperarFinalizarRun. InterruptedException.  Producida en sleep() analizando x_procesos_informar_finalizados[i]. ", ie.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                //Se ignora. Simplemente se ejecutara y ya volvera a dormir.
            }/*try-catch*/
            todos_finalizados = true;
            this.lock_grupo_x.quieroLeer();
                for (i=0; i<num_max_hilos; i++){
                    hilo              = this.x_procesos_run.get(i);
                    todos_finalizados = todos_finalizados && ( this.x_procesos_informar_finalizados[i] || !hilo.isAlive() || hilo.isInterrupted() );
                }/*for*/
            this.lock_grupo_x.dejoDeLeer();
        }/*while*/
    }/*END esperarFinalizarRun*/

    
    /**
     * Recorre la lista de activos, pasivos y orden_1 para avisar a todos los conocidos de que nos vamos a apagar y así no intenten contactar con nosotros. <br>
     * Se hace aqui control de acceso                                                                                                                       <br>
     * 
     * @param password_administracion Para llamar a los metodos de informarBaja de otros servidores hace falta la contraseña de administracion
     */
    private void informarAlmacenes(String password_administracion){
    /*VAR*/
        Map[]                                  maps_para_union    = new Map[2];    //Unira a orden_0 con orden_1. Puede haber repetido, la interseccion no tiene porque ser vacia.
        Map<String, AlmacenInterfaceRemote>    map_union          =  null;
        Set<String>                            set_ids            =  null;
        Iterator                               itr_ids            =  null;
        String                                 id_aux             =    "";
        AlmacenInterfaceRemote                 almacen_a_informar =  null;
    /*BEIGN*/
        this.lock_grupo_y.quieroLeer();
            maps_para_union[0] = this.y_almacenes_orden_0;
            maps_para_union[1] = this.y_almacenes_orden_1;
            map_union          = this.mapsUnion(maps_para_union);
        this.lock_grupo_y.dejoDeLeer();
        
        set_ids = map_union.keySet();                                           //El codigo puede modificar los maps y_almacenes_orden_0/1 pero no los objetos que contienen. Por eso no hay peligro de leer fuera del bloqueo.
        itr_ids = set_ids.iterator();
        while ( itr_ids.hasNext() ){
            id_aux             = (String)itr_ids.next();
            almacen_a_informar = map_union.get(id_aux);
            try{
                almacen_a_informar.informandoBaja(password_administracion, this);
            }catch (RemoteException re){
                this.log("informarAlmacenes. RemoteException.  Producida en informandoBaja(). ", re.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                //Se ignora. Se le ha intentado informar y no se ha podido. 
            }/*try-catch*/
        }/*while*/
    }/*END informarAlmacenes*/
    
    
    /**
     * Borra el servicio asociado a este objeto remoto de rmiregistry
     */
    private void unbindRmiRegistry(){
    /*VAR*/
        Registry registro                 = null;
        int      control_remote_exception = 0;
    /*BEGIN*/
        try{
            registro = LocateRegistry.getRegistry(this.rmiregistry_ip);         control_remote_exception = 1;     //Se accede sin control, no se espera que sea modificada.
            registro.unbind(this.rmiregistry_nombre_servicio);                  control_remote_exception = 2;     //Se accede sin control, no se espera que sea modificada.
        }catch (RemoteException ex) {
            switch(control_remote_exception){
                case 0:
                    this.log("unbindRmiRegistry. RemoteException. Producida por getRegistry(): ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                break;
                case 1:
                    this.log("unbindRmiRegistry. RemoteException. Producida por unbind: "       , ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                break;
            }/*switch*/
        }catch (NotBoundException nbe){
            this.log("unbindRmiRegistry. NotBoundException. Producida por unbind: ", nbe.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
        }/*try-catch*/
    }/*END unbindRmiRegistry*/
    
    
    /**
     * Las versiones de los ficheros, las que hay anotadas en cada FicheroInfo de this.z_ficheros_del_almacen deben ser almacenadas                                       <br>
     * Esto es así para que en el siguiente arranque del servidor se pueda regenerar la estrucutra this.z_ficheros_del_almacen tal y como estaba antes de que se apagase. <br>
     */
    private void almacenarVersiones (){
    /*VAR*/
        Properties                   prop_01                        =  null;
        Set<String>                  set_keys                       =  null;
        Iterator                     itr_keys                       =  null;
        FicheroInfo                  fich_info_aux                  =  null;
        FileWriter                   fw                             =  null;
        String                       key                            =    "";
        String                       version                        =    "";
        String                       ruta_nombre_directorio         =    "";
        String                       ruta_nombre_fichero            =    "";
        boolean                      directorio_creado              =  true;
        boolean                      fichero_creado                 =  true;
    /*BEGIN*/
        prop_01 = new Properties();
        this.lock_grupo_z.quieroLeer();
            set_keys = this.z_ficheros_del_almacen.keySet();
            itr_keys = set_keys.iterator();
            while ( itr_keys.hasNext() ){
                key           = (String)itr_keys.next();
                fich_info_aux = this.z_ficheros_del_almacen.get(key);
                version       = fich_info_aux.getVersion();
                prop_01.setProperty(key, version);
            }/*while*/
        this.lock_grupo_z.dejoDeLeer();

        ruta_nombre_directorio = this.directorio_base;
        ruta_nombre_fichero    = NOMBRE_FICHERO_VERSIONES;
        if ( !FicherosTrabajar.existeDirectorio(ruta_nombre_directorio) ){
            directorio_creado = FicherosTrabajar.crearDirectorio(ruta_nombre_directorio);
        }/*if*/
        if ( directorio_creado && !FicherosTrabajar.exiteFichero(ruta_nombre_directorio, ruta_nombre_fichero) ){
            fichero_creado = FicherosTrabajar.crearFichero(ruta_nombre_directorio, ruta_nombre_fichero);
        }/*if*/
        
        if (fichero_creado){
            try{
                fw = new FileWriter(ruta_nombre_directorio + ruta_nombre_fichero);
                prop_01.store(fw, "Las versiones necesarias para poder regenerar z_ficheros_del_almacen correctamente");
            }catch (IOException ex){
                this.log("almacenarVersiones. IOException. Producida por unbind: ", ex.toString()  , MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
            }/*try-catch*/
        }else{
            this.log("almacenarVersiones. No se ha creado el fichero de versiones: " + ruta_nombre_directorio + ruta_nombre_fichero, "", MOSTAR_SERVIDOR[2-1], MOSTAR_FECHA[2-1], 2);
        }/*if*/
    }/*END almacenarVersiones*/


// METODOS PRIVADOS comprobarConexiones ----------------------------------------
    
    /**
     * Agrega el servidor enviado a la lista de servidores activos (lo contrario a pasiavo)                                           <br>
     * Lo anota en: y_almacenes_comunicacion_activa                                                                                   <br>
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo y_ en modo escritura. <br>
     * 
     * @param almacen_id  El identificador del almacen
     * @param almacen_obj El almacen a agregar a conexiones activas.
     */
    private void anotarAlmacenActivo(String almacen_id, AlmacenInterfaceRemote almacen_obj){
    /*BEGIN*/
        if ( !this.y_almacenes_comunicacion_activa.containsKey(almacen_id) ){
            this.y_almacenes_comunicacion_activa.put(almacen_id, almacen_obj);
            this.y_num_comunicaciones_activas++;
        }/*if*/
    }/*END anotarAlmacenActivo*/

    
    /**
     * Agrega el servidor enviado a la lista de servidores pasivos                                                                    <br>
     * Lo anota en: y_almacenes_comunicacion_pasiva                                                                                   <br>
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo y_ en modo escritura. <br>
     * 
     * @param almacen_id  El identificador del almacen
     * @param almacen_obj El almacen a agregar a conexiones pasivas.
     */
    private void anotarAlmacenPasivo(String almacen_id, AlmacenInterfaceRemote almacen_obj){
    /*BEGIN*/
        if ( !this.y_almacenes_comunicacion_pasiva.containsKey(almacen_id) ){
            this.y_almacenes_comunicacion_pasiva.put(almacen_id, almacen_obj);
            this.y_num_comunicaciones_pasivas++;
        }/*if*/
    }/*END anotarAlmacenPasivo*/

    
    /**
     * Saca el servidor enviado de la lista de servidores activos (lo contrario a pasiavo)                                            <br>
     * Lo borra de: y_almacenes_comunicacion_activa                                                                                   <br>
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo y_ en modo escritura. <br>
     * 
     * @param almacen_id  El identificador del almacen
     */
    private void borrarAlmacenActivo(String almacen_id){
    /*BEGIN*/
        if ( this.y_almacenes_comunicacion_activa.containsKey(almacen_id) ){
            this.y_almacenes_comunicacion_activa.remove(almacen_id);
            this.y_num_comunicaciones_activas--;
        }/*if*/
    }/*END borrarAlmacenActivo*/

    
    /**
     * Saca el servidor enviado de la lista de servidores pasivos                                                                     <br>
     * Lo borra de: y_almacenes_comunicacion_pasiva                                                                                   <br>
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo y_ en modo escritura. <br>
     * 
     * @param almacen_id  El identificador del almacen
     */
    private void borrarAlmacenPasivo(String almacen_id){
    /*BEGIN*/
        if ( this.y_almacenes_comunicacion_pasiva.containsKey(almacen_id) ){
            this.y_almacenes_comunicacion_pasiva.remove(almacen_id);
            this.y_num_comunicaciones_pasivas--;
        }/*if*/
    }/*END borrarAlmacenPasivo*/
    
    
    /**
     * Agrega a la estructura que contiene los fichero que hay en el servidor el envaido por parametro                                <br>
     * Lo agrega a: z_ficheros_del_almacen                                                                                            <br>
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo z_ en modo escritura. <br>
     * 
     * @param fich_id       El identificador del fichero
     * @param fichero_info  La informacion a agregar a la estructura
     */
    private void anotarFicheroEnMiEstructura(String fich_id, FicheroInfo fichero_info){
    /*BEGIN*/
        this.z_ficheros_del_almacen.put(fich_id, fichero_info);
    }/*END anotarFicheroEnMiEstructura*/
    
    
    /**
     * Saca de la estructura que contiene los fichero que hay en el servidor el asociad al id enviado                                 <br>
     * Lo borra de: z_ficheros_del_almacen                                                                                            <br>
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo z_ en modo escritura. <br>
     * 
     * @param fich_id El identificador del fichero
     */
    private void borrarFicheroDeMiEstructura(String fich_id){
    /*BEGIN*/
        this.z_ficheros_del_almacen.remove(fich_id);
    }/*END borrarFicheroDeMiAlmacen*/
    
    
    /**
     * Carga y_almacenes_orden_0 con la unión de los maps de activos y pasivos.
     * No se hace aqui control de acceso, quien llame a este metodo debe tener en cuenta que se accede al grupo y_ en modo escritura. <br>
     */
    private void generarAlmacenesOrdenCero(){
    /*VAR*/
        Map[] maps_para_union = new Map[2];
    /*BEGIN*/
        maps_para_union[0]       = this.y_almacenes_comunicacion_activa;
        maps_para_union[1]       = this.y_almacenes_comunicacion_pasiva;
        this.y_almacenes_orden_0 = this.mapsUnion(maps_para_union);
    }/*END almacenesOrdenCero*/

    
    /**
     * Carga y_almacenes_orden_1 con la unión de los maps de los y_almacenes_orden_0 de mis y_almacenes_orden_0.                            <br>
     * Carga y_almacenes_orden_1 con la unión de las llamadas remotas a almacenresOrdenCero de los almacenes que hay en y_almacenes_orden_0 <br>
     * Asume que this.y_almacenes_orden_0 ya esta cargado, no llama a generarAlmacenesOrdenCero()                                           <br>
     * Se asegura de no insertar en la lista a si mismo ni a los que estan en activos y pasivos (orden0)                                    <br>
     *                                                                                                                                      <br>
     * Hace el control de acceso aqui debido a que realiza llamadas remotas                                                                 <br>
     */
    private void generarAlmacenesOrdenUno(String password_administracion){
    /*VAR*/
        Map<String, AlmacenInterfaceRemote> maps_almacenes_orden_0_aux =  null;
        Map[]                               maps_para_union            =  null;
        Map                                 map_aux                    =  null;
        Set                                 set_keys                   =  null;
        Iterator                            itr_keys                   =  null;
        List<String>                        ids_almacenes              =  null;
        String                              key_orden_0                =    "";
        String                              key_orden_1                =    "";
        AlmacenInterfaceRemote              almacen_remoto_0           =  null;                //Almacen de orden 0
        AlmacenInterfaceRemote              almacen_remoto_1           =  null;                //Almacen de orden 1
        int                                 control_remote_exception   =     0;
        int                                 list_size                  =     0;
        int                                 i                          =     0;
        boolean                             valida_key                 = false;
    /*BEGIN*/
        this.lock_grupo_y.quieroLeer();
            maps_para_union            = new Map[1];
            maps_para_union[0]         = this.y_almacenes_orden_0;
            maps_almacenes_orden_0_aux = this.mapsUnion(maps_para_union);                //Se usa mapsUnion() como procedimiento para copiar. Se pasan a una estructura auxiliar los almacenes de orden_0 para poder acceder sin bloquear el grupo y.
        this.lock_grupo_y.dejoDeLeer();
        
        maps_para_union    = new Map[2];
        maps_para_union[0] = new HashMap<String, AlmacenInterfaceRemote>();              //[0] Se inicializa con vacio. Luego tendra lo que poco a poco sera y_almacenes_orden_1
        set_keys           = maps_almacenes_orden_0_aux.keySet();
        itr_keys           = set_keys.iterator();
        while ( itr_keys.hasNext() ){                                                    //Recorre mis almacenes de orden 0.
            try{
                map_aux            = new HashMap<String, AlmacenInterfaceRemote>();
                key_orden_0        = (String)itr_keys.next();
                almacen_remoto_0   = maps_almacenes_orden_0_aux.get(key_orden_0);
                ids_almacenes      = almacen_remoto_0.listaDeAlmacenesOrdenCero(password_administracion);                     control_remote_exception = 1;
                list_size          = ids_almacenes.size();
                for (i=0; i<list_size; i++){                                                                 //Recorre los almacenes de orden 0 de uno mio que es de orden cero. Por tanto, uno mio de orden 1.
                    key_orden_1 = ids_almacenes.get(i);
                    this.lock_grupo_y.quieroLeer();
                        valida_key  = (!key_orden_1.equals(this.id))                                   &&    //La lista de orden1 no puede contener a uno mismo
                                      (!this.y_almacenes_comunicacion_activa.containsKey(key_orden_1)) &&    //La lista de orden1 no puede contener a uno de orden cero
                                      (!this.y_almacenes_comunicacion_pasiva.containsKey(key_orden_1));
                    this.lock_grupo_y.dejoDeLeer();
                    if ( valida_key ){
                        almacen_remoto_1 = almacen_remoto_0.almacenOrdenCero(password_administracion, key_orden_1);         control_remote_exception = 2;
                        if (almacen_remoto_1 != null){
                            map_aux.put(key_orden_1, almacen_remoto_1);
                        }/*if*/
                    }/*if*/
                }/*for*/
                maps_para_union[1] = map_aux;                                            //[1] Es el map de orden cero de uno almacen mio (que es uno mio de orden 0)
                maps_para_union[0] = this.mapsUnion(maps_para_union);                    //[0] Tendra poco a poco a y_almacenes_orden_1
            }catch (RemoteException ex){
                switch (control_remote_exception){
                    case 0:
                        this.log("generarAlmacenesOrdenUno. RemoteException. Producida por listaDeAlmacenesOrdenCero: ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                        //Generado por listaDeAlmacenesOrdenCero. Se pasa al siguiente almacen_remoto_0, no se agregan sus conocidos.
                    break;
                    case 1:
                        this.log("generarAlmacenesOrdenUno. RemoteException. Producida por almacenOrdenCero: "         , ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                        //Generado por almacenOrdenCero.          Se pasa al siguiente almacen_remoto_0, no se agregan sus conocidos.
                    break;
                }/*switch*/
            }/*try-catch*/
        }/*while*/
        this.lock_grupo_y.quieroEscribir();
            this.y_almacenes_orden_1 = maps_para_union[0];
        this.lock_grupo_y.dejoDeEscribir();
    }/*END generarAlmacenesOrdenUno*/
    
    
    /**
     * Dado un fichero con cierta informacion la usa para crear una lista de servidores con los que poder establecer comunicacion.                                         <br>
     * No agregara a la lista un almacen cuyo nombre de servicio sea igual a la ID de este servidor (this) para evitar que se use para contactar consigo mismo.            <br>
     * No puede haber ceros entre el guion bajo y el numero.                                                                                                               <br>
     * Debera haber tantas parejas (ip_registry_i, nombre_servicio_en_rmiregistry_i) como indique num_de_servidores.                                                       <br>
     * Ejemplo del fichero:                                                                                                                                                <br>
     *     num_de_servidores=2                                                                                                                                             <br>
     *     ip_rmiregistry_1=192.168.176.01                                                                                                                                 <br>
     *     nombre_servicio_en_rmiregistry_1=Almacen_001                                                                                                                    <br>
     *     ip_rmiregistry_2=192.168.176.02                                                                                                                                 <br>
     *     nombre_servicio_en_rmiregistry_2=Almacen_002                                                                                                                    <br>
     *                                                                                                                                                                     <br>
     * Hay dos grupos para las excepciones. Uno relacionado con la lectura del fichero que implica el fin de la ejecucion al no poder leer la lista de servidores.         <br>
     * El otro grupo de excepciones esta asociado a la conexion con un servidor. Se ignora si se produce excepcion y se pasa al siguiente servidor de la lista.            <br>
     * 
     * @param  fich_con                      El fichero de propiedades con la informacion sobre los servidores remotos registrados en rmiregistry
     * @return List<AlmacenInterfaceRemote>  Una lista de instancias de objetos remotos creados con la informacion del fichero que se pasa. 
     */
    private List<AlmacenInterfaceRemote> listaAlmacenesParaContactar(String fich_con){
    /*VAR*/
        List<AlmacenInterfaceRemote>          lista_almacenes                     = new ArrayList<AlmacenInterfaceRemote>();
        AlmacenInterfaceRemote                almacen_remoto                      =  null;
        Properties                            prop                                =  null;
        Registry                              registry                            =  null;
        String                                ip_rmiregistry                      =    "";
        String                                nombre_servicio_en_rmiregistry      =    "";
        int                                   num_de_servidores                   =     0;
        int                                   i                                   =     1;   //En el fichero se empieza con 1.
    /*BEGIN*/
        if ( FicherosTrabajar.exiteFichero("", fich_con) ){                     //fich_con es la ruta completa absoluta
            prop = new Properties();
            try{
                prop.load( new FileReader(fich_con) );
                num_de_servidores = Integer.parseInt( prop.getProperty(FICHERO_PROP_RMIREGISTRY_NUM_DE_SERVIDORES) );
                for (i=1; i<=num_de_servidores; i++){
                    ip_rmiregistry                 = prop.getProperty( FICHERO_PROP_RMIREGISTRY_IP              + "_" + i );
                    nombre_servicio_en_rmiregistry = prop.getProperty( FICHERO_PROP_RMIREGISTRY_NOMBRE_SERVICIO + "_" + i );
                    this.log("listaAlmacenesParaContactar. Intentando conectar con servidor.                          IP y Nombre del servicio: " + ip_rmiregistry + " / " + nombre_servicio_en_rmiregistry, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    if ( !nombre_servicio_en_rmiregistry.equals(this.id) ){         //Se obliga a que el nombre del servicio en rmiregistry sea el mismo que el del ID. Entoces así se evita que en la lista este un servidor que se pueda usar para contactar consigo mismo.
                        try{
                            registry       = LocateRegistry.getRegistry(ip_rmiregistry);
                            almacen_remoto = (AlmacenInterfaceRemote)registry.lookup(nombre_servicio_en_rmiregistry);
                            if ( (almacen_remoto != null) && FICHERO_PROP_RMIREGISTRY_CADENA_TEST.equals( almacen_remoto.testConnection(FICHERO_PROP_RMIREGISTRY_CADENA_TEST)) ){
                                this.log("listaAlmacenesParaContactar. El servidor ha respondido.                                 IP y Nombre del servicio: " + ip_rmiregistry + " / " + nombre_servicio_en_rmiregistry , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                                lista_almacenes.add(almacen_remoto);
                            }else{
                                this.log("listaAlmacenesParaContactar. Problemas con la conexion. Null o ping invalido.           IP y Nombre del servicio: " + ip_rmiregistry + " / " + nombre_servicio_en_rmiregistry , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                                this.log("listaAlmacenesParaContactar. Para comprobar si el objeto remoto es null:                                          " + almacen_remoto                                          , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                            }/*if*/
                        }catch (NotBoundException nbe){
                            this.log("listaAlmacenesParaContactar. Problemas contactando con el servidor. NotBoundException.  IP y Nombre del servicio: " + ip_rmiregistry + " / " + nombre_servicio_en_rmiregistry, nbe.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                            //Se ignora y se continua con el siguiente almacen.
                        }catch (RemoteException   re ){
                            this.log("listaAlmacenesParaContactar. Problemas contactando con el servidor. RemoteException.    IP y Nombre del servicio: " + ip_rmiregistry + " / " + nombre_servicio_en_rmiregistry, re.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                            //Se ignora y se continua con el siguiente almacen.
                        }/*try-catch*/
                    }/*if*/
                }/*for*/
            }catch (FileNotFoundException fnfe){
                this.log("listaAlmacenesParaContactar. FileNotFoundException. ", fnfe.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
            }catch (IOException ioe){
                this.log("listaAlmacenesParaContactar. IOException. ", ioe.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                //Si hay problemas leyendo el fichero en algun momento se deja y se sale pero se devuelve lo que se haya conseguido.
            }catch (NumberFormatException nfe){
                this.log("listaAlmacenesParaContactar. NumberFormatException. ", nfe.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                //Habrá problemas haciendo el parseInt si no existe la propiedad o está mal escrita. 
                //No se están haciendo otras comprobciones como asegurarse de que existen el resto de las propiedades que devuelven cadenas. 
            }/*try-catch*/
        }else{
            this.log("listaAlmacenesParaContactar. Ficerho no existe: " + fich_con, "", MOSTAR_SERVIDOR[3-1], MOSTAR_FECHA[3-1], 3);
        }/*if*/
        return lista_almacenes;
    }/*END listaAlmacenesParaContactar*/    
    
    
    /**
     * Primer metodo de dos que busca contactar con otros almacenes con los que realizar sincronizaciones activas.                                                         <br>
     * Hace aqui control de acceso debido a que conectarConServidoresB realiza llamadas remotas. Se debe evitar realizar llamadas remotas con variables bloqueadas.        <br>
     *                                                                                                                                                                     <br>
     * Dada una lista de servidores remotos, se usa para contactar con otros servidores y crear una red de almacenes distribuidos.                                         <br>
     * Se ira uno por uno y se usara el primero que de señales de vida y permita conectarse a otro usando su metodo solicitarConexion.                                     <br>
     * Se obliga a que el nombre del servicio en rmiregistry sea el mismo que el del ID.                                                                                   <br>
     *                                                                                                                                                                     <br>
     * Hay dos grupos para las excepciones. Uno relacionado con la lectura del fichero que implica el fin de la ejecucion al no poder leer la lista de servidores.         <br>
     * El otro grupo de excepciones esta asociado a la conexion con un servidor. Se ignora si se produce excepcion y se pasa al siguiente servidor de la lista.            <br>
     * 
     * @param  password_administracion  La contraseña de administracion que es necesaria para llamar a algunos métodos de los que se invocan.
     * @param  lista_almacenes_base     La lista de servidores con los que se intetara contactar. Si no se puede con ellos se usara a sus conocidos. No puede tener una referencia a this que implicaria contactarse con uno mismo.
     * @return List<String>             Una lista de string donde se agregan los ids de los servidores con los que se realiza conexiones activas
     */    
    private List<String> conectarConServidoresA(String password_administracion, List<AlmacenInterfaceRemote> lista_almacenes_base){
    /*VAR*/
        List<String>            almacenes_contactados    =  new  ArrayList<String>();
        List<String>            conexiones_realizadas    =  new  ArrayList<String>();
        AlmacenInterfaceRemote  almacen_remoto           =  null;
        int                     num_almacenes_base       =  0;
        int                     i                        =  0;
        boolean                 faltan_almacenes_activos = false;
    /*BEGIN*/
        num_almacenes_base = lista_almacenes_base.size();
        this.lock_grupo_y.quieroLeer();
            faltan_almacenes_activos = (this.y_num_comunicaciones_activas < this.y_num_max_comunicaciones_activas);
        this.lock_grupo_y.dejoDeLeer();
        while ( faltan_almacenes_activos && (i < num_almacenes_base) ){
            almacen_remoto = lista_almacenes_base.get(i);
            this.conectarConServidoresB(password_administracion, almacen_remoto, true, conexiones_realizadas, almacenes_contactados);      //Se intenta contactar con este servidor y sus conocidos antes de pasar al siguiente.
            this.lock_grupo_y.quieroLeer();
                faltan_almacenes_activos = (this.y_num_comunicaciones_activas < this.y_num_max_comunicaciones_activas);
            this.lock_grupo_y.dejoDeLeer();
            i++;
        }/*while*/
        return conexiones_realizadas;
    }/*END conectarConServidoresA*/

    
    /**
     * Segundo metodo de dos que busca contactar con otros almacenes con los que realizar sincronizaciones activas.                                                        <br>
     * Se suministra una referencia a otro servidor. Si este metodo es llamado es porque aun se puede crear nuevas  conexiones activas.                                    <br>
     * Hace aqui control de acceso debido a que realiza llamadas remotas. Se debe evitar realizar llamadas remotas con variables bloqueadas.                               <br>
     *                                                                                                                                                                     <br>
     * CONDICIONES para agregar nuevos servidores:                                                                                                                         <br>
     *     No superar el limite de conexiones activas.                                                                                                                     <br>
     *     No puede contactarse consigo mismo.                                                                                                                             <br>
     *     El servidor a analizar no puede estar ya en contactados.                                                                                                        <br>
     *     El servidor a agregar  no puede estar en conexiones pasivas.                                                                                                    <br>
     * 
     * @param password_administracion  La contraseña de administracion que es necesaria para llamar a algunos métodos de los que se invocan.
     * @param almacen_remoto           El servidor al que hay que pedirle su lista de servidores conocidos para buscar otros servidores con los que contactar. Pero primero se intenta crear una conexion con el.
     * @param probar_con_enviado       Si es false no se intentara contactar con el servidor enviado y se pasara directamente a su lista de conocidos.* 
     * @param lista_serv_que_aceptan   Una lista de string donde se agregaran el id de los servidores que acepten la conexion.
     * @param almacenes_contactados    Una lista de almacenes con los que ya se ha probado establecer una conexion y con los que no se debe volver a probar. Para las llamadas recursivas.
     */
    private void conectarConServidoresB (String password_administracion, AlmacenInterfaceRemote almacen_remoto, boolean probar_con_enviado, List<String> lista_serv_que_aceptan, List<String> almacenes_contactados){
    /*VAR*/
        List<String>                          almacenes_a_contactar      =  null;
        AlmacenInterfaceRemote                almacen_remoto_aux         =  null;
        String                                id_servidor                =    "";
        int                                   control_remote_exception_a =     0;
        int                                   control_remote_exception_b =     0;
        int                                   num_almacenes_contactar    =     0;
        int                                   i                          =     0;
        boolean                               acepta_conexion            = false;
        boolean                               candidato_valido           = false;
        boolean                               faltan_almacenes_activos   = false;
    /*BEGIN*/
        try{
            //Primero intenta conctactar con el servidor enviado
            if (probar_con_enviado){
                id_servidor = almacen_remoto.getID();                                                                                                          control_remote_exception_a = 1;                
                this.lock_grupo_y.quieroLeer();
                    candidato_valido = ( (this.y_num_comunicaciones_activas < this.y_num_max_comunicaciones_activas) &&               //No superar el limite de conexiones activas
                                         !this.id.equals(id_servidor)                                                &&               //No contactar consigo mismo
                                         !almacenes_contactados.contains(id_servidor)                                &&               //No puede haber sido ya contactado
                                         !this.y_almacenes_comunicacion_pasiva.containsKey(id_servidor)                  );           //No puede estar en pasivos
                this.lock_grupo_y.dejoDeLeer();
                if ( candidato_valido ){
                    this.log("conectarConServidoresB. 01 Solicitando conexion a:            " + id_servidor, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    acepta_conexion = almacen_remoto.solicitarConexion(password_administracion, this);                                                         control_remote_exception_a = 2;
                    if (acepta_conexion){
                        this.log("conectarConServidoresB. 01 Conexion aceptada  por:            " + id_servidor, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                        this.lock_grupo_y.quieroEscribir();
                            this.anotarAlmacenActivo(id_servidor, almacen_remoto);
                        this.lock_grupo_y.dejoDeEscribir();                            
                        lista_serv_que_aceptan.add(id_servidor);
                    }else{
                        this.log("conectarConServidoresB. 01 Conexion rechazada por:            " + id_servidor, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                    }/*if*/
                }/*if*/
                almacenes_contactados.add  (id_servidor);                                                                             //Se puede agregar a la lista de contactados a si mismo. No se si supone un problema.
            }/*if*/
            
            //Luego se intenta contactar con servidores conocidos del servidor enviado
            this.lock_grupo_y.quieroLeer();
                faltan_almacenes_activos = (this.y_num_comunicaciones_activas < this.y_num_max_comunicaciones_activas);
            this.lock_grupo_y.dejoDeLeer();            
            if (faltan_almacenes_activos ){                                                                                           //Casi absurdo porque luego se pregunta en el while. Pero puede evitar una llamada remota que además puede ser pesada si la lista es grande.
                id_servidor             = almacen_remoto.getID();                                                                                            control_remote_exception_a = 3;
                this.log("conectarConServidoresB. 02 Solicitando almacenes conocidos a: " + id_servidor                   , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                almacenes_a_contactar   = almacen_remoto.listaDeAlmacenesOrdenCero(password_administracion);                                                 control_remote_exception_a = 3;
                num_almacenes_contactar = almacenes_a_contactar.size();
                this.log("conectarConServidoresB. 02 Cantidad de almacenes conocidos:   " + num_almacenes_contactar       , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                if (num_almacenes_contactar > 0){
                    almacenes_a_contactar   = this.listResta(almacenes_a_contactar, almacenes_contactados);
                    num_almacenes_contactar = almacenes_a_contactar.size();
                    this.log("conectarConServidoresB. 02 De los cuales se usan:         :   " + num_almacenes_contactar       , "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                }/*if*/    
                
                //Primero se intenta contactar con todos los conocidos del servidor enviado
                this.lock_grupo_y.quieroLeer();
                    faltan_almacenes_activos = (this.y_num_comunicaciones_activas < this.y_num_max_comunicaciones_activas);
                this.lock_grupo_y.dejoDeLeer();            
                while ( faltan_almacenes_activos && (i < num_almacenes_contactar) ){
                    try{
                        almacen_remoto_aux = almacen_remoto.almacenOrdenCero(password_administracion, almacenes_a_contactar.get(i) );
                        if (almacen_remoto_aux != null){
                            id_servidor = almacen_remoto_aux.getID();                                                     control_remote_exception_b = 1;
                            this.lock_grupo_y.quieroLeer();
                                candidato_valido = ( !this.id.equals(id_servidor)                                   &&                //No contactar consigo mismo
                                                     !almacenes_contactados.contains(id_servidor)                   &&                //No puede haber sido ya contactado
                                                     !this.y_almacenes_comunicacion_pasiva.containsKey(id_servidor)     );            //No puede estar en pasivos
                            this.lock_grupo_y.dejoDeLeer();
                            if ( candidato_valido ){
                                this.log("conectarConServidoresB. 02 Solicitando conexion a:            " + id_servidor, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                                acepta_conexion = almacen_remoto_aux.solicitarConexion(password_administracion, this);    control_remote_exception_b = 2;
                                if (acepta_conexion){
                                    this.log("conectarConServidoresB. 02 Conexion aceptada  por:            " + id_servidor, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                                    this.lock_grupo_y.quieroEscribir();
                                        this.anotarAlmacenActivo(id_servidor, almacen_remoto_aux);
                                    this.lock_grupo_y.dejoDeEscribir();                            
                                    lista_serv_que_aceptan.add(id_servidor);
                                }else{
                                    this.log("conectarConServidoresB. 02 Conexion rechazada por:            " + id_servidor, "", MOSTAR_SERVIDOR[4-1], MOSTAR_FECHA[4-1], 4);
                                }/*if*/
                            }/*if*/ 
                            almacenes_contactados.add  (id_servidor );                                                                //Se puede agregar a la lista de contactados a si mismo. No se si supone un problema.
                        }/*if*/
                    }catch(RemoteException ex){
                        switch(control_remote_exception_b){
                            case 0:
                                this.log("conectarConServidoresB. RemoteException B. Producida por getID(). "            , ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                                //Se puede salir sin peligro, solo se había pedido el ID, no de deja ninguna tarea a medias. 
                                //Pero se podria intentar continuar... tal vez esta excepcion debe estar dentro del bucle...
                            break;
                            case 1:
                                this.log("conectarConServidoresB. RemoteException B. Producida por solicitarConexion(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                                //No se sabe si el otra habrá aceptado la conexion. Obligara a usar confirmarConexion().
                                //Pero se podria intentar continuar... tal vez esta excepcion debe estar dentro del bucle...
                        }/*switch*/
                    }/*try-catch*/
                    this.lock_grupo_y.quieroLeer();
                        faltan_almacenes_activos = (this.y_num_comunicaciones_activas < this.y_num_max_comunicaciones_activas);
                    this.lock_grupo_y.dejoDeLeer();            
                    i++;                
                }/*while*/

                //Despues se siguen agregando conexiones con los conocidos de los conocidos.
                //Se vuelve a recorrer la lista de los conocidos del servidor enviado como parametro.
                //Antes era para intentar contactar con ellos y ahora es para contactar con sus conocidos. 
                i = 0;
                this.lock_grupo_y.quieroLeer();
                    faltan_almacenes_activos = (this.y_num_comunicaciones_activas < this.y_num_max_comunicaciones_activas);
                this.lock_grupo_y.dejoDeLeer();
                while ( faltan_almacenes_activos && (i < num_almacenes_contactar) ){
                    almacen_remoto_aux = almacen_remoto.almacenOrdenCero(password_administracion, almacenes_a_contactar.get(i) );
                    if (almacen_remoto_aux != null){
                        this.conectarConServidoresB(password_administracion, almacen_remoto_aux, false, lista_serv_que_aceptan, almacenes_contactados);
                    }/*if*/
                    this.lock_grupo_y.quieroLeer();
                        faltan_almacenes_activos = (this.y_num_comunicaciones_activas < this.y_num_max_comunicaciones_activas);
                    this.lock_grupo_y.dejoDeLeer();
                    i++;
                }/*while*/
            }/*if*/
        }catch (RemoteException ex){
            switch(control_remote_exception_a){
                case 0:                                                         //Producida por getID()
                    this.log("conectarConServidoresB. RemoteException A. Producida por getID(). "                    , ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    //Se sale sin haber hecho tarea alguna.
                break;
                case 1:                                                         //Producida por solicitarConexion()
                    this.log("conectarConServidoresB. RemoteException A. Producida por solicitarConexion(). "        , ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    //El otro nos podria tener aceptado la conexion y anotado mientras nosotros aun no.
                    //El otro debera usar el metodo confirmarConexion()
                break;
                case 2:                                                         //Producida por getID()
                    this.log("conectarConServidoresB. RemoteException A. Producida por getID(). "                    , ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    //Se sale sin haber hecho tarea alguna.
                break;
                case 3:                                                         //Producida por listaDeAlmacenesConocidos()
                    this.log("conectarConServidoresB. RemoteException A. Producida por listaDeAlmacenesConocidos(). ", ex.toString(), MOSTAR_SERVIDOR[1-1], MOSTAR_FECHA[1-1], 1);
                    //Se puede salir sin peligro ya que no de deja ninguna tarea a medias. 
                break;                    
            }/*switch*/
        }/*try-catch*/
    }/*END conectarConServidoresB*/

    
// METODOS PRIVADOS sincronizarFicheros ----------------------------------------
    
    /**
     * Una procedimiento muy cutre para dar valor a z_sincronizacion_rechazada en vez de tener que ensuciar el codigo 
     * 
     * @param str1 Cadena 1
     * @param str2 Cadena 2
     * @param str3 Cadena 3
     * @param num1 Numero 1
     * @param num2 Numero 2
     * @param num3 Numero 3
     */
    private void procesarSincronizacionRechazada(String str1, String str2, String str3, int num1, int num2, int num3){
    /*VAR*/
        char[] array_char = null;
        int    num_aux    =    0;
    /*BEGIN*/
        array_char = str1.toCharArray();
        if ( (array_char != null) && (array_char.length > 0) && (array_char[0] == '-') ){
            num_aux = Integer.parseInt(str1);
            if (num_aux == INFO_SINCRONIZACION_SIN_PERMISO){
                this.z_sincronizacion_rechazada = true;
            }/*if*/
        }/*if*/
        
        array_char = str2.toCharArray();
        if ( (array_char != null) && (array_char.length > 0) && (array_char[0] == '-') ){
            num_aux = Integer.parseInt(str2);
            if (num_aux == INFO_SINCRONIZACION_SIN_PERMISO){
                this.z_sincronizacion_rechazada = true;
            }/*if*/
        }/*if*/
        
        array_char = str3.toCharArray();
        if ( (array_char != null) && (array_char.length > 0) && (array_char[0] == '-') ){
            num_aux = Integer.parseInt(str3);
            if (num_aux == INFO_SINCRONIZACION_SIN_PERMISO){
                this.z_sincronizacion_rechazada = true;
            }/*if*/
        }/*if*/
        
        if (num1 == INFO_SINCRONIZACION_SIN_PERMISO){
            this.z_sincronizacion_rechazada = true;
        }/*if*/

        if (num2 == INFO_SINCRONIZACION_SIN_PERMISO){
            this.z_sincronizacion_rechazada = true;
        }/*if*/

        if (num3 == INFO_SINCRONIZACION_SIN_PERMISO){
            this.z_sincronizacion_rechazada = true;
        }/*if*/
    }/*END procesarSincronizacionRechazada*/
    
    
// METODOS PRIVADOS auxiliares -------------------------------------------------
    
    /**
     * Realiza una resta entre dos Maps.                                                                   <br>
     * No se realiza copia, los elementos del map devuelto son referencias a elementos enviados del map_a. <br>
     * No se realiza ningun control de acceso al desconocer donde se esta accediendo.                      <br>
     * 
     * @param   map_a  El map al que hay que quitarle elementos
     * @param   map_b  El map que dice que elementos hay que quitar
     * @return  Map    Un map que tiene los elementos de map_a que no existan en map_b
     */
    private Map mapResta(Map map_a, Map map_b){
    /*VAR*/
        Map       map_resta      = new HashMap();
        Set       keys_map_a     = null;
        Iterator  itr_keys_map_a = null;
        Object    key_a          = null;
        Object    value_a        = null;
    /*BEGIN*/
        keys_map_a = map_a.keySet();
        itr_keys_map_a = keys_map_a.iterator();
        while ( itr_keys_map_a.hasNext() ){
            key_a = itr_keys_map_a.next();
            if ( !map_b.containsKey(key_a) ){
                value_a = map_a.get(key_a);
                map_resta.put(key_a, value_a);
            }/*if*/
        }/*while*/
        return map_resta;
    }/*END mapResta*/

    
    /**
     * Realiza una resta entre dos List.                                                                         <br>
     * No se realiza copia, los elementos de la list devuelta son referencias a elementos enviados de la list_a. <br>
     * No se realiza ningun control de acceso al desconocer donde se esta accediendo.                            <br>
     * 
     * @param   list_a  La list al que hay que quitarle elementos
     * @param   list_b  La list que dice que elementos hay que quitar
     * @return  List    Una list que tiene los elementos de list_a que no existan en list_b
     */
    private List listResta(List list_a, List list_b){
    /*VAR*/
        List      list_resta     = new ArrayList();
        Object    value_a        = null;
        int       size_a         =    0;
        int       i              =    0;
    /*BEGIN*/
        size_a = list_a.size();
        for (i=0; i<size_a; i++){
            value_a = list_a.get(i);
            if ( !list_b.contains(value_a) ){
                list_resta.add(value_a);
            }/*if*/
        }/*for*/
        return list_resta;
    }/*END listResta*/

    
    /**
     * Se realiza la union de todos los maps en uno solo                                                                          <br>
     * La union se hace en base a las keys.                                                                                       <br>
     * Implica que si dos maps tienen la misma key pero con valores diferentes predominara el del indice menor del array enviado. <br>
     * No se realiza copia, los elementos del map devuelto son referencias a elementos de los maps enviados                       <br>
     * No se realiza ningun control de acceso al desconocer donde se esta accediendo.                                             <br>
     * 
     * @param  maps Un array de maps con el que se hace un map resultado de la union de todos.
     * @return Map  Un map fruto de la union de todos los maps enviados.
     */
    private Map mapsUnion(Map[] maps){
    /*VAR*/
        Map       map_union      = new HashMap();
        Map       map_aux        = null;        
        Set       keys_map       = null;
        Iterator  itr_keys_map   = null;
        Object    key            = null;
        Object    value          = null;
        int       num_maps       =    0;
        int       i              =    0;
    /*BEGIN*/
        num_maps = maps.length;
        for (i=0; i<num_maps; i++){
            map_aux  = maps[i];
            keys_map = map_aux.keySet();
            itr_keys_map = keys_map.iterator();
            while ( itr_keys_map.hasNext() ){
                key   = itr_keys_map.next();
                value = map_aux.get(key);
                if ( !map_union.containsKey(key) ){
                    map_union.put(key, value);
                }/*if*/
            }/*while*/
        }/*for*/
        return map_union;
    }/*END mapsUnion*/
    
    
    /**
     * Realiza una interseccion entre dos Maps.                                                            <br>
     * No se realiza copia, los elementos del map devuelto son referencias a elementos enviados del map_a. <br>
     * No se realiza ningun control de acceso al desconocer donde se esta accediendo.                      <br>
     * 
     * @param   map_a  El map a
     * @param   map_b  El map b
     * @return  Map    Un map que tiene los elementos que se encuentran en los dos maps
     */
    private Map mapInterseccion(Map map_a, Map map_b){
    /*VAR*/
        Map       map_inter      = new HashMap();
        Set       keys_map_a     = null;
        Iterator  itr_keys_map_a = null;
        Object    key_a          = null;
        Object    value_a        = null;
    /*BEGIN*/
        keys_map_a = map_a.keySet();
        itr_keys_map_a = keys_map_a.iterator();
        while ( itr_keys_map_a.hasNext() ){
            key_a = itr_keys_map_a.next();
            if ( map_b.containsKey(key_a) ){
                value_a = map_a.get(key_a);
                map_inter.put(key_a, value_a);
            }/*if*/
        }/*while*/
        return map_inter;
    }/*END mapInterseccion*/

    
    /**
     * Dado un Map crea una List con los valores.                                                           <br>
     * No se realiza copia, los elementos de la list devuelta son referencias a elementos enviados del map. <br>
     * No se realiza ningun control de acceso al desconocer donde se esta accediendo.                       <br>
     * 
     * @param  map  El Map del que se debe extraer sus valores para crear una lista.
     * @return List La lista con los valores que contiene el Map enviado
     */
    private List mapValuesToList(Map map){
    /*VAR*/
        List      list_return  = new ArrayList();
        Set       keys_map     = null;
        Iterator  itr_keys_map = null;
        Object    key          = null;
    /*BEGIN*/
        keys_map = map.keySet();
        itr_keys_map = keys_map.iterator();
        while ( itr_keys_map.hasNext() ){
            key = itr_keys_map.next();
            list_return.add( map.get(key) );
        }/*while*/
        return list_return;
    }/*END mapValuesToList*/
    

    /**
     * Dada una fecha en milisegundos devuelve una cadena con el formato 'yyyy/MM/dd HH:mm:ss' de SimpleDateFormat
     * 
     * @param  long_date  La fecha en milisegundos desde January 1, 1970 00:00:00.000 GMT
     * @return String     La fecha formateada en una cadena de carateres.
     */
    private String fechaFormateada(long long_date){
    /*VAR*/
        Date             date      = null;
        SimpleDateFormat sdf       = null;
        String           date_text =   "";
    /*BEGIN*/
        date      = new Date(long_date);
        sdf       = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        date_text = sdf.format(date);
        return date_text;
    }/*END fechaFormateada*/
    
    
// METODOS PRIVADOS logs -------------------------------------------------------
    

    /**
     * Para mostrar un mensaje por pantalla o no de un suceso ocurrido.           <br>
     * Hay tres tipos de mensajes:                                                <br>
     *    1: Error.                                                               <br>
     *    2: Warning.                                                             <br>
     *    3: Info.                                                                <br>
     *    4: Debug.                                                               <br>
     *                                                                            <br>
     * Se mostraran todos, algunos o ningunos dependiendo del nivel seleccionado. <br>
     * Se puede mostar Error, Error y Warnings o los tres.                        <br>
     * No se puede hacer a la inversa, mostrar Infor sin mostrar los otros .      <br>
     *
     * @param mensaje_info               Cualquier comentario que se quiera agregar como documentacion.
     * @param mensaje_erro               El mensaje de error, generalmente el que devuelve la excepcion. Solo se muestra para nivel=1.
     * @param mostrar_nombre_servidor    A true agrega el nombre del servidor a los mensajes.
     * @param mostrar_fecha_hora         A true muestra la fecha y hora del mensaje.
     * @param es_por_error               Indica si se debe mostrar de forma especial por ser una excepcion o un error no esperado.
     */
    private void log (String mensaje_info, String mensaje_error, boolean mostrar_nombre_servidor, boolean mostrar_fecha_hora, int nivel){
    /*VAR*/
        String mensaje     = "";
        String mensaje_add = "";
    /*BEGIN*/
        if (mostrar_fecha_hora){
            mensaje_add = mensaje_add + new Date() + ". ";
        }/*if*/
        if (mostrar_nombre_servidor){
            mensaje_add = mensaje_add + "Almacen: " + this.id + ". ";
        }/*if*/
        if (this.log_nivel >= nivel){
            switch (nivel){
                case 1:
                    mensaje = "*** ERROR: " + mensaje_add + mensaje_info + "\n*****" + mensaje_error + "\n**********";
                break;
                case 2:
                    mensaje = "WARNING:   " + mensaje_add + mensaje_info;
                break;
                case 3:
                    mensaje = "Info:      " + mensaje_add + mensaje_info;
                break;
                case 4:
                    mensaje = "Debug:     " + mensaje_add + mensaje_info;
                break;
            }/*switch*/
            System.out.println(mensaje);
        }/*if*/
    }/*END log*/

    
}/*END CLASS AlmacenObjeto*/