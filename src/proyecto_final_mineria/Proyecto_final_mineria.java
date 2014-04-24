package proyecto_final_mineria;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 *
 * @author ricardo
 */
public class Proyecto_final_mineria {

    public static void main(String[] args) {
        
        int puerto = 1085;
        int size = 111;
        int numero_de_instancias = 200;

        try {
            // La IP es la local, el puerto es en el que el servidor esté escuchando.
            DatagramSocket socket = new DatagramSocket(puerto);

            // Un DatagramPacket para recibir los mensajes.
            DatagramPacket dato = new DatagramPacket(new byte[size], size);

            float[] accelerometer_x_array = new float[numero_de_instancias];
            float[] accelerometer_y_array = new float[numero_de_instancias];
            float[] accelerometer_z_array = new float[numero_de_instancias];

            long[] tiempo_de_lecturas = new long[numero_de_instancias];

            float[] resultado_x = new float[2];
            float[] resultado_y = new float[2];
            float[] resultado_z = new float[2];

            for (int i = 0; i < numero_de_instancias; i++) {
                // Se recibe un dato y se escribe en pantalla.
                socket.receive(dato);
//                   System.out.print("Recibido dato de "+ dato.getAddress().getHostName() + " : ");

                byte[] arreglo = dato.getData();
                tiempo_de_lecturas[i] = System.currentTimeMillis();//lectura ne milisegundos

                byte[] accelerometer_x_bytes = Arrays.copyOfRange(arreglo, 4, 8);
                byte[] accelerometer_y_bytes = Arrays.copyOfRange(arreglo, 8, 12);
                byte[] accelerometer_z_bytes = Arrays.copyOfRange(arreglo, 12, 16);

                accelerometer_x_array[i] = ByteBuffer.wrap(accelerometer_x_bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                accelerometer_y_array[i] = ByteBuffer.wrap(accelerometer_y_bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                accelerometer_z_array[i] = ByteBuffer.wrap(accelerometer_z_bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                //Imprimeindo cada una de las lecturas
                System.out.println(i + 1 + " [X=" + accelerometer_x_array[i] + "]   \t\t\t" + "[Y=" + accelerometer_y_array[i] + "]   \t\t\t" + "[Z=" + accelerometer_z_array[i] + "]\n");
                System.out.println("Tiempo:\t" + tiempo_de_lecturas[i]);
            }

            resultado_x = maxmin(accelerometer_x_array);
            resultado_y = maxmin(accelerometer_y_array);
            resultado_z = maxmin(accelerometer_z_array);

            //Imprimiendo los maximos y los minimos de cada eje
            for (int i = 0; i < 2; i++) {
                System.out.println("maximo en x: " + resultado_x[i] + " maximo en y: " + resultado_y[i] + " maximo en z: " + resultado_z[i]);
            }

            float[] bins_result_x = bins(maxmin(accelerometer_x_array), accelerometer_x_array);
            float[] bins_result_y = bins(maxmin(accelerometer_y_array), accelerometer_y_array);
            float[] bins_result_z = bins(maxmin(accelerometer_z_array), accelerometer_z_array);

            System.out.println("\n\nResultados del bin en X: \n");
            for (int k = 0; k < bins_result_x.length; k++) {
                System.out.println(k + 1 + " = " + bins_result_x[k]);
            }

            System.out.println("\n\nResultados del bin en Y: \n");
            for (int k = 0; k < bins_result_y.length; k++) {
                System.out.println(k + 1 + " = " + bins_result_y[k]);
            }

            System.out.println("\n\nResultados del bin en Z: \n");
            for (int k = 0; k < bins_result_z.length; k++) {
                System.out.println(k + 1 + " = " + bins_result_z[k]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
       
    }
    
    
    public static float[] maxmin(float[] arreglo){
       
       float[] resultados = new float[2];
       
       for(int i=0;i<arreglo.length;i++){
           if(i==0){
               resultados[0] = arreglo[i];
               resultados[1] = arreglo[i];
           }
           else{
               if(arreglo[i] > resultados[0]) resultados[0] = arreglo[i];//Mayor
               if(arreglo[i] < resultados[1]) resultados[1] = arreglo[i];//Menor
           }
       }
       return resultados;
   }
   
   public static float[] bins(float[] rango_maxmin, float[] lecturas_del_eje){
       
       float[] bins = new float[10];
       for(int i=0; i<10 ;i++) bins[i]=0;//inicializarlos en 0
       
       float rango = (rango_maxmin[0]-(rango_maxmin[1]))/10;
       
       System.out.println("rango = "+rango);
       
       for(int i=0;i<lecturas_del_eje.length;i++){
           for(int j=0;j<10;j++){
               
               //preguntar por los extremos primero
               if(lecturas_del_eje[i] == rango_maxmin[1]){
                   bins[0]++;
                   break;
               }
               else if(lecturas_del_eje[i] == rango_maxmin[0]){
                   bins[9]++;
                   break;
               }
               else if(lecturas_del_eje[i] >= (rango_maxmin[1]+(j*rango)) && lecturas_del_eje[i] < (rango_maxmin[1]+((j+1)*rango))){
                   bins[j]= bins[j]+1;
                   break;
               }
           }
       }
       
       for(int i=0;i<10;i++) bins[i]= bins[i]/200;
       return bins; 
   }
   
   public static float tiempo_entre_picos(float[] lecturas_del_eje,long[] tiempos)
   {
       
       float variacion_de_umbral = 0.05f;
       
       float[] extremos = maxmin(lecturas_del_eje);
       
       
       
       List indices_de_picos_dentro_del_umbral = new LinkedList();
       for(float i =(extremos[0]-variacion_de_umbral) ; i >= extremos[1] ;i = (i-variacion_de_umbral) ){
           for(int j=0;j<lecturas_del_eje.length;j++){
               
               if(lecturas_del_eje[j] >= i /*&& !indices_de_picos_dentro_del_umbral.contains(j)*/)
                   indices_de_picos_dentro_del_umbral.add(j);
            }
           
           if(indices_de_picos_dentro_del_umbral.size() >= 3)
               break;
           else
               indices_de_picos_dentro_del_umbral.clear();//borrar los valores, para que entre de izquierda a derecha
       }
       
       long resultado = 0;
       for(int i=0;i<indices_de_picos_dentro_del_umbral.size()-1;i++)
           resultado += (tiempos[i+1]-tiempos[i]);
       
       resultado /= (indices_de_picos_dentro_del_umbral.size()-1);
          
       return 0.0f;
   }
    
}


//   //Header
//   byte[] x46 = new byte[1];//char                          0
//   byte[] x53 = new byte[1];//char                          1
//   byte[] packet_version = new byte[1];//BYTE               2
//   byte[] body_size = new byte[1];//BYTE                    3
//   
//   //Body
//   byte[] accelerometer_x = new byte[4];//float             4-7
//   byte[] accelerometer_y = new byte[4];//float             8-11
//   byte[] accelerometer_z = new byte[4];//float             12-15     
//   
//   byte[] gyroscope_x = new byte[4];//float                 16-19
//   byte[] gyroscope_y = new byte[4];//float                 20-23
//   byte[] gyroscope_z = new byte[4];//float                 24-27    
//   
//   byte[] teslameter_x = new byte[8];//double1              28-35
//   byte[] teslameter_y = new byte[8];//double               36-43
//   byte[] teslameter_z = new byte[8];//double               44-51    
//   
//   byte[] magnetic_heading = new byte[8];//double           52-59
//   byte[] true_heading = new byte[8];//double               
//   
//   byte[] latitude = new byte[8];//double
//   byte[] longitud = new byte[8];//double
//   byte[] altitud = new byte[8];//double
//   
//   byte[] proximity_sensor = new byte[1];//BOOL
//   
//   byte[] point1_touched = new byte[1];//BOOL
//   byte[] point1_x = new byte[4];//int
//   byte[] point1_y = new byte[4];//int
//   
//   byte[] point2_touched = new byte[1];//BOOL
//   byte[] point2_x = new byte[4];//int
//   byte[] point3_y = new byte[4];//int
