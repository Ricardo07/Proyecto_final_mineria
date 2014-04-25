package proyecto_final_mineria;

import java.io.BufferedReader;
import java.io.FileReader;
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
        //System.out.print("PROBANDO");

        try {
            
            probando_con_el_archivo();
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
               else if(lecturas_del_eje[i] > (rango_maxmin[1]+(j*rango)) && lecturas_del_eje[i] < (rango_maxmin[1]+((j+1)*rango))){
                   bins[j] += 1;
                   break;
               }
           }
       }
       
       for(int i=0;i<10;i++) bins[i]/= 200;
       return bins; 
   }
   
   public static float tiempo_entre_picos(float[] lecturas_del_eje,long[] tiempos)
   {
       long resultado = 0;
       int iteraciones_umbral= 0;
       
       float[] extremos = maxmin(lecturas_del_eje);
       float variacion_de_umbral = extremos[0]*0.10f;
       
       List indices_de_picos_dentro_del_umbral = new LinkedList();
       
       for(float umbral =(extremos[0]-variacion_de_umbral) ; umbral >= extremos[1] ; umbral -= variacion_de_umbral ){
           
           for(int j=0;j<lecturas_del_eje.length;j++){ // j indice de los valores de los tiempos 
               
               if(lecturas_del_eje[j] >= umbral /*&& !indices_de_picos_dentro_del_umbral.contains(j)*/)
                   indices_de_picos_dentro_del_umbral.add(j);//Se encontró un valor mayor al umbra, añadie el indice a la lista
            }
           
           iteraciones_umbral++;
           
           if(indices_de_picos_dentro_del_umbral.size() >= 3)//si la lista contiene por lo menos 3 valores mayores al umbral, pasar al siguiente paso
               break;
           else
               indices_de_picos_dentro_del_umbral.clear();//borrar los valores, para que entre de izquierda a derecha
       }
       
       int[] indices = new int[indices_de_picos_dentro_del_umbral.size()];
       
       //sacando los indices de la lista y convirtiendolos en un arrelgo de enteros
       for (int i = 0; i <indices_de_picos_dentro_del_umbral.size() ; i++) {
           indices[i] = Integer.parseInt(indices_de_picos_dentro_del_umbral.get(i).toString());
           System.out.println("---->"+indices[i]);//Probando
       }
       
       for(int i=0;i<indices.length-1;i++)
           resultado += (tiempos[indices[i+1]]-tiempos[indices[i]]);//siguiente - actual
       
       resultado /= (indices.length)*1000000;//dividiendo entre el numero de picos
          
       System.out.println("Debuging");
       return resultado;
   }

   public static float get_avg(float[] lecturas_del_eje)
   {
       float avg = 0;

       //foreach en java
       for(float i : lecturas_del_eje)
           avg += i;
       avg /= lecturas_del_eje.length;
       
       avg = avg<0 ? 0: avg; //si es negativo ponerlo en 0, de lo contrario dejarlo igual.
       
       return avg;
   }

   public static float get_std_deviation(float[] lecturas_del_eje)
   {
       //primero calculamos el average
       float avg = get_avg(lecturas_del_eje);

       //luego calculamos la diferencia de cada valor con el avg y se eleva al cuadrado
       float std_dev = 0;
       for(float i : lecturas_del_eje)
           std_dev += Math.pow((i - avg), 2);

       //luego se le saca el average nuevamente y se le saca la raiz cuadrada.
       std_dev = (float)Math.sqrt((std_dev/lecturas_del_eje.length));
       return std_dev;
   }

   public static float get_avg_absolute_difference(float[] lecturas_del_eje)
   {
       //se calcula el average de las 200 medidas del eje a analizar
       float avg = get_avg(lecturas_del_eje);
       float abs_avg_diff = 0;
       for(float i : lecturas_del_eje)
           abs_avg_diff += Math.abs((i - avg));
       abs_avg_diff/=lecturas_del_eje.length;
       return abs_avg_diff;
   }

   public static float get_avg_resultant_acceleration(float[] eje_x, float[] eje_y, float[] eje_z)
   {
       float avg_resultant_acceleration = 0;
       for (int i = 0; i < eje_x.length; i++)
           avg_resultant_acceleration += Math.sqrt( (Math.pow(eje_x[i],2) + Math.pow(eje_y[i],2) + Math.pow(eje_z[i],2)));
       avg_resultant_acceleration /= eje_x.length;
       return avg_resultant_acceleration;
   }
   
  /*
   Estas funciones son para leer los datos originales del archivo arff
   y poder comprobar si todos los cálculos de las funciones están bien. 
   */
   public static String[] leer_raw_arff(int numero_de_filas) {

        String path = "/Users/ricardo/Desktop/WISDM_ar_v1.1_raw.txt";//Ruta absoluta del archivo
        String[] resultado = new String[numero_de_filas];

        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            //Leer el numero de filas deseado 
            for (int i = 0; i < numero_de_filas && (line != null); i++) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            String everything = sb.toString();//Crear el String completo
            br.close();//cerrar el archivo

            resultado = everything.split("\n");//convertir el String a un arreglo de Strings

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultado;
    }
    
    public static long[] timestamp_del_arreglo(){
        
        String[] lineas_del_archivo = leer_raw_arff(200);
        long[] resultado = new long[lineas_del_archivo.length];
        
        try{         
            for (int i = 0; i < lineas_del_archivo.length; i++) {
                String[] fila = lineas_del_archivo[i].split(",");
                resultado[i] = Long.parseLong(fila[2]);//en el archivo el indice #2 es el timestamp
            }
        } catch (NumberFormatException e) {
            System.out.println("Error de conversión");
        }
        
        return resultado;
    }
    
    public static float[] lecturas_archivo_del_eje(String eje){
        
        String[] lineas_del_archivo = leer_raw_arff(200);
        float[] valores_del_eje = new float[lineas_del_archivo.length];
        
        int indice_a_leer = 0;
        
        switch (eje) {
            case "x":
                indice_a_leer = 3;
                break;
            case "y":
                indice_a_leer = 4;
                break;
            case "z":
                indice_a_leer = 5;
                break;
            default:
                return null;
        }
        
        try {
            
            for (int i = 0; i < lineas_del_archivo.length; i++){
                String[] fila = lineas_del_archivo[i].split(",");
                String str = fila[indice_a_leer];
                
                if (str.length() > 0 && str.charAt(str.length()-1)==';') 
                    str = str.substring(0, str.length()-1);
 
                valores_del_eje[i] = Float.parseFloat(str);
            } 
            System.out.println(".");    
        } catch (NumberFormatException e) {
            System.out.println("Error de conversión");
        }
        
        return valores_del_eje;
    }
    
    public static void probando_con_el_archivo(){
        
        
        float[] accelerometer_x = lecturas_archivo_del_eje("x");
        float[] accelerometer_y = lecturas_archivo_del_eje("y");
        float[] accelerometer_z = lecturas_archivo_del_eje("z");
        
        float[] bins_x = bins(maxmin(accelerometer_x),accelerometer_x);
        float[] bins_y = bins(maxmin(accelerometer_y),accelerometer_y);
        float[] bins_z = bins(maxmin(accelerometer_z),accelerometer_z);
        
        float avg_x = get_avg(accelerometer_x);
        float avg_y = get_avg(accelerometer_y);
        float avg_z = get_avg(accelerometer_z);
        
        long[] tiempos = timestamp_del_arreglo();
        
        float pico_X = tiempo_entre_picos(accelerometer_x,tiempos);
        float pico_Y = tiempo_entre_picos(accelerometer_y,tiempos);
        float pico_Z = tiempo_entre_picos(accelerometer_z,tiempos);
        
        float std_deviation_x = get_std_deviation(accelerometer_x);
        float std_deviation_y = get_std_deviation(accelerometer_y);
        float std_deviation_z = get_std_deviation(accelerometer_z);
        
        float avg_absolute_diff_X = get_avg_absolute_difference(accelerometer_x);
        float avg_absolute_diff_Y = get_avg_absolute_difference(accelerometer_y);
        float avg_absolute_diff_Z = get_avg_absolute_difference(accelerometer_z);
                
        float avg_resultant = get_avg_resultant_acceleration(accelerometer_x,accelerometer_y,accelerometer_z);
        
        System.out.println("Debe funcionar!");
        
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
