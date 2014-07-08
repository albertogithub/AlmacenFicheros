package almacenficheros;

import java.io.Serializable;

/**
 * Representa la informacion de un paquete de un fichero que se lee por partes.
 * 
 * @author Alberto Bellido de la Cruz
 */
public class FicheroPaquete implements Serializable{

// VARIABLES ----------------------------------------------------------------
    private String  id                            =    "";

//  private byte[]  array_de_bytes                =  null;
    private int[]   array_de_bytes                =  null;
    private int     tam_paquete                   =     0;
    private int     numero_de_paquete             =     0;
    private int     cantidad_de_bytes_validos     =     0;
    private boolean paquete_valido                = false;    //Para saber si hay informacions, si se ha llamado a setBytes.


// CONSTRUCTORES ------------------------------------------------------------
    public FicheroPaquete(String id){
        this.id = id;
    }

    
// METODOS ------------------------------------------------------------------
//  public void setBytes(byte[] array_de_bytes){
    public void setBytes(int[]  array_de_bytes){
        this.paquete_valido = true;
        this.array_de_bytes = array_de_bytes;
    }

    
    public void setTamPaquete(int tam_paquete){
        this.tam_paquete = tam_paquete;
    }

    
    public void setNumeroDePaquete(int numero_de_paquete){
        this.numero_de_paquete = numero_de_paquete;
    }

    public void setCantidadDeBytesValidos(int bytes_validos){
        this.cantidad_de_bytes_validos = bytes_validos;
    }

//-----
    
//  public byte[] getBytes(){
    public int[]  getBytes(){
        return this.array_de_bytes;
    }

    
    public int getTamPaquete(){
        return this.tam_paquete;
    }

    
    public int getNumeroDePaquete(){
        return this.numero_de_paquete;
    }

    public int getCantidadDeBytesValidos(){
        return this.cantidad_de_bytes_validos;
    }

    public boolean getPaqueteValido(){
        return this.paquete_valido;
    }
    
}/*END CLASS FicheroPaquete*/