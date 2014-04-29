package proyecto_final_mineria;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;


/**
 *
 * @author ricardo
 */
public class Proyecto_final_mineria {
    
    static BufferedReader reader;
    static Instances data ;
    static J48 tree;

    public static void main(String[] args) {
        
        int numero_de_instancias = 200;
        
        inicializando_weka();//Inicializando API de weka construyendo el arbol J48 unpruned
        
        //En caso de querer probar con los 200 primeros valores del archivo usar la función -> probando_con_el_archivo();
         
        float[] accelerometer_x_array = new float[numero_de_instancias];
        float[] accelerometer_y_array = new float[numero_de_instancias];
        float[] accelerometer_z_array = new float[numero_de_instancias];

        long[] tiempo_de_lecturas = new long[numero_de_instancias];
        
        lectura_de_datos(accelerometer_x_array, accelerometer_y_array, accelerometer_z_array, tiempo_de_lecturas);

        String resultado_clasificasion = evaluando_la_actividad(accelerometer_x_array,accelerometer_y_array,accelerometer_z_array,tiempo_de_lecturas);
        
        System.out.println("\n\n[resultado = " + resultado_clasificasion + "]");
       
    }
    
    
    public static void lectura_de_datos(float[] arreglo_x,float[] arreglo_y,float[] arreglo_z,long[] tiempos){
        
        int puerto = 1085;
        int size = 111;
        int numero_de_instancias = arreglo_x.length;
        
        try{
            
            // La IP es la local, el puerto es en el que el servidor esté escuchando.
            DatagramSocket socket = new DatagramSocket(puerto);

            // Un DatagramPacket para recibir los mensajes.
            DatagramPacket dato = new DatagramPacket(new byte[size], size);

            for (int i = 0; i < numero_de_instancias; i++) {
                
                socket.receive(dato);
                //System.out.print("Recibido dato de "+ dato.getAddress().getHostName() + " : ");

                byte[] arreglo = dato.getData();
                tiempos[i] = System.currentTimeMillis();//lectura ne milisegundos

                byte[] accelerometer_x_bytes = Arrays.copyOfRange(arreglo, 4, 8);//4, 8
                byte[] accelerometer_y_bytes = Arrays.copyOfRange(arreglo, 8, 12);
                byte[] accelerometer_z_bytes = Arrays.copyOfRange(arreglo, 12, 16);

                arreglo_x[i] = ByteBuffer.wrap(accelerometer_x_bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat()*10f;
                arreglo_y[i] = ByteBuffer.wrap(accelerometer_y_bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat()*10f;
                arreglo_z[i] = ByteBuffer.wrap(accelerometer_z_bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat()*10f;

                //Imprimeindo cada una de las lecturas
                System.out.println(i + 1 + " [X=" + arreglo_x[i] + "]   \t\t\t" + "[Y=" + arreglo_y[i] + "]   \t\t\t" + "[Z=" + arreglo_z[i] + "]");
                //System.out.println("Tiempo:\t" + tiempo_de_lecturas[i]);
            }
            
            socket.close();//!!!Importante cerra el socket!!!
            
        }catch (Exception e){
            e.printStackTrace();
        }
        
        return;
    }
    
    public static String evaluando_la_actividad(float[] accelerometer_x_array,float[] accelerometer_y_array,float[] accelerometer_z_array,long[] tiempo_de_lecturas) {

        String resultado_clasificasion = "";
        
        float[] bins_result_x = bins(maxmin(accelerometer_x_array), accelerometer_x_array);
        float[] bins_result_y = bins(maxmin(accelerometer_y_array), accelerometer_y_array);
        float[] bins_result_z = bins(maxmin(accelerometer_z_array), accelerometer_z_array);

        try {

            int num = 1150;                                     //Un numero cualquiera
            Instance nueva_entrada = data.instance(num);      //Crear una copia

            //Modificando los valores
            //Agregar los valores de los bins
            for (int i = 0; i < 10; i++) {
                nueva_entrada.setValue(i, bins_result_x[i]);//bins_x
                nueva_entrada.setValue(i + 10, bins_result_y[i]);//bins_y
                nueva_entrada.setValue(i + 20, bins_result_z[i]);//bins_z
            }

            //AVG
            nueva_entrada.setValue(30, get_avg(accelerometer_x_array));
            nueva_entrada.setValue(31, get_avg(accelerometer_y_array));
            nueva_entrada.setValue(32, get_avg(accelerometer_z_array));

            //PEAK
            nueva_entrada.setValue(33, tiempo_entre_picos(accelerometer_x_array, tiempo_de_lecturas));
            nueva_entrada.setValue(34, tiempo_entre_picos(accelerometer_y_array, tiempo_de_lecturas));
            nueva_entrada.setValue(35, tiempo_entre_picos(accelerometer_z_array, tiempo_de_lecturas));

            //ABSOLDEV
            nueva_entrada.setValue(36, get_avg_absolute_difference(accelerometer_x_array));
            nueva_entrada.setValue(37, get_avg_absolute_difference(accelerometer_y_array));
            nueva_entrada.setValue(38, get_avg_absolute_difference(accelerometer_z_array));

            //STANDDEV
            nueva_entrada.setValue(39, get_std_deviation(accelerometer_x_array));
            nueva_entrada.setValue(40, get_std_deviation(accelerometer_y_array));
            nueva_entrada.setValue(41, get_std_deviation(accelerometer_z_array));

            //RESULTANT
            nueva_entrada.setValue(42, get_avg_resultant_acceleration(accelerometer_x_array, accelerometer_y_array, accelerometer_z_array));

            double clsLabel = tree.classifyInstance(nueva_entrada); // Clasificando una nueva instancia
            resultado_clasificasion = data.classAttribute().value((int) clsLabel);

        } catch (Exception e) {
            System.out.println("Error al clasificar");
            e.printStackTrace();
        }

        return resultado_clasificasion;
    }
    
    /**
     * En contrar los valores máximo y mínimo del arreglo a recibir
     * @param arreglo 200 lecturas del eje en cuestión  
     * @return arreglo con el valor de lectura mayor en la posicion 0 y el menor en la posición 1
     */
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
   
   /**
     * Calcula la distribucion proporcional de los valores en el eje a evaluar, dando como resultado un arreglo de 10 con las proporciones de cada segmento.
     * @param rango_maxmin
     * @param lecturas_del_eje
     * @return 
     */
   public static float[] bins(float[] rango_maxmin, float[] lecturas_del_eje){
       
       float[] bins = new float[10];
       for(int i=0; i<10 ;i++) bins[i]=0;//inicializarlos en 0
       
       float rango = (rango_maxmin[0]-(rango_maxmin[1]))/10;//Valor entre el maximo y el mínimo
       
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
   
   /**
    * Calcula el tiempo entre los picos de valores del eje.
    * @param lecturas_del_eje 200 lecturas del valor del accelerometro en el eje a considerar
    * @param tiempos Es el arreglo de tiempos en milisegundos de las 200 lecturas en el eje (sera el mismo arreglo para los tres ejes ya que las lecturas se hacen simultaneas) 
    * @return 
    */
   public static float tiempo_entre_picos(float[] lecturas_del_eje,long[] tiempos){
       
       long resultado = 0;
       List indices_de_picos_dentro_del_umbral = new LinkedList();
       float[] extremos = maxmin(lecturas_del_eje);
       float factor = 0.10f;//factor en el que comenzará a bajar el umbral
       float variacion_de_umbral = extremos[0]*factor;//El mayor * un diez porciento 
       if(variacion_de_umbral<0) variacion_de_umbral *= -1;//hace que la  cantidad a variar siempre sea positiva
       
       /**
        * Ahora comenzará a encontrar todos los indicies en los cuales los valores de lectura del eje sean mayor que el umbral.
        * El umbral comenzará con siendo igual a valor_mayor-(valor_mayor*0.10)
        * Disminullendo en cada ciclo el umbral en un factor de 0.01 en 0.01 , por lo que la segunda vez el umbral será igual a valor_mayor-(valor_mayor*0.11), y así sucesivament... 
        * Se deben encontrar por lo menos 3 indices sino pasar a la siguiente iteracion del ciclo.
        * En caso de no encontar ningún indice la funcion de be retornar 0.
        */
       for(float umbral =(extremos[0]-variacion_de_umbral) ; umbral >= extremos[1] ;  ){
           
           for(int j=0;j<lecturas_del_eje.length;j++){ // j indice de los valores de los tiempos 
               
               if(lecturas_del_eje[j] >= umbral )
                   indices_de_picos_dentro_del_umbral.add(j);//Se encontró un valor mayor al umbra, añadie el indice a la lista
            }
           
           if(indices_de_picos_dentro_del_umbral.size() >= 3)//Si la lista contiene por lo menos 3 valores mayores al umbral, pasar al siguiente paso
               break;
           else
               indices_de_picos_dentro_del_umbral.clear();//Borrar los valores, para que entre de izquierda a derecha
           
           /**
            *Actualización del umbral, teniendo en cuenta los signos para hacer las operaciones.
            */
           factor += 0.01f;
           variacion_de_umbral = extremos[0]*factor;
           
           if(extremos[0]>=0)
               umbral = (extremos[0]-variacion_de_umbral);
           else
               umbral = (extremos[0]+variacion_de_umbral);
           
       }
       
       if(indices_de_picos_dentro_del_umbral.isEmpty())
           return 0.0f;
       
       int[] indices = new int[indices_de_picos_dentro_del_umbral.size()];
       
       //Sacando los indices de la lista y convirtiendolos en un arrelgo de enteros
       for (int i = 0; i <indices_de_picos_dentro_del_umbral.size() ; i++) 
           indices[i] = Integer.parseInt(indices_de_picos_dentro_del_umbral.get(i).toString());
       
       //Sumando los tiempos entre picos
       for(int i=0;i<indices.length-1;i++)
           resultado += (tiempos[indices[i+1]]-tiempos[indices[i]]);//siguiente - actual
       
       //Dividiendo entre el número de picos
       resultado /= indices.length;
       
       return resultado;
   }

   /**
    * AVG
    * @param lecturas_del_eje 200 lecturas del valor del accelerometro en el eje a considerar
    * @return 
    */
   public static float get_avg(float[] lecturas_del_eje){
       float avg = 0;

       for(float i : lecturas_del_eje)
           avg += i;
       avg /= lecturas_del_eje.length;
       
       avg = avg<0 ? 0: avg; //si es negativo ponerlo en 0, de lo contrario dejarlo igual.
       
       return avg;
   }

   /**
    * STANDDEV
    * @param lecturas_del_eje 200 lecturas del valor del accelerometro en el eje a considerar
    * @return 
    */
   public static float get_std_deviation(float[] lecturas_del_eje){
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

   /**
    * Average Absolute Difference
    * @param lecturas_del_eje 200 lecturas del valor del accelerometro en el eje en consideración
    * @return 
    */
   public static float get_avg_absolute_difference(float[] lecturas_del_eje){
       //se calcula el average de las 200 medidas del eje a analizar
       float avg = get_avg(lecturas_del_eje);
       float abs_avg_diff = 0;
       for(float i : lecturas_del_eje)
           abs_avg_diff += Math.abs((i - avg));
       abs_avg_diff/=lecturas_del_eje.length;
       return abs_avg_diff;
   }

   /**
    * RESULTANT
    * @param eje_x 200 lecturas del valor del accelerometro en el eje X
    * @param eje_y 200 lecturas del valor del accelerometro en el eje Y
    * @param eje_z 200 lecturas del valor del accelerometro en el eje Z
    * @return 
    */
   public static float get_avg_resultant_acceleration(float[] eje_x, float[] eje_y, float[] eje_z){
       
       float avg_resultant_acceleration = 0;
       for (int i = 0; i < eje_x.length; i++)
           avg_resultant_acceleration += Math.sqrt( (Math.pow(eje_x[i],2) + Math.pow(eje_y[i],2) + Math.pow(eje_z[i],2)));
       avg_resultant_acceleration /= eje_x.length;
       return avg_resultant_acceleration;
   }
   
   
   public static void inicializando_weka() {

        //Inicializando los objetos de weka
        try {

            reader = new BufferedReader(new FileReader("WISDM_ar_v1.1_transformed.arff"));
            data = new Instances(reader);
            reader.close();
            
            // especificando el atributo de clase
            data.setClassIndex(data.numAttributes() - 1);

            String[] options = new String[1];
            options[0] = "-U";            // unpruned tree
            tree = new J48();         // new instance of tree
            tree.setOptions(options);     // set the options
            tree.buildClassifier(data);   // build classifier

        } catch (Exception e) {
            System.out.println("Error inicializando los objetos de weka");
        }
        
        System.out.println("Weka inicio bien");

    }
   
  /********************************************************************************
   Estas funciones son para leer los datos originales del archivo arff
   y poder comprobar si todos los cálculos de las funciones están bien. 
   ********************************************************************************/
   
   public static String[] leer_raw_arff(int numero_de_filas) {

        String path = "datos_crudos/WISDM_ar_v1.1_raw.txt";//Ruta relativa del archivo
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
        
        float avg_absolute_diff_X = get_avg_absolute_difference(accelerometer_x);
        float avg_absolute_diff_Y = get_avg_absolute_difference(accelerometer_y);
        float avg_absolute_diff_Z = get_avg_absolute_difference(accelerometer_z);
        
        float std_deviation_x = get_std_deviation(accelerometer_x);
        float std_deviation_y = get_std_deviation(accelerometer_y);
        float std_deviation_z = get_std_deviation(accelerometer_z);
                
        float avg_resultant = get_avg_resultant_acceleration(accelerometer_x,accelerometer_y,accelerometer_z);
        
        System.out.println("Debe funcionar!");
        
    }
       
}

/*****************************************************************************
 * 
 * 
 * Estruturad de los paquetes UDP a recivir de la aplicación --Sensor Streamer--  
 * 
 * 
   //Header
   byte[] x46 = new byte[1];//char                          0
   byte[] x53 = new byte[1];//char                          1
   byte[] packet_version = new byte[1];//BYTE               2
   byte[] body_size = new byte[1];//BYTE                    3
   
   //Body
   byte[] accelerometer_x = new byte[4];//float             4-7
   byte[] accelerometer_y = new byte[4];//float             8-11
   byte[] accelerometer_z = new byte[4];//float             12-15     
   
   byte[] gyroscope_x = new byte[4];//float                 16-19
   byte[] gyroscope_y = new byte[4];//float                 20-23
   byte[] gyroscope_z = new byte[4];//float                 24-27    
   
   byte[] teslameter_x = new byte[8];//double1              28-35
   byte[] teslameter_y = new byte[8];//double               36-43
   byte[] teslameter_z = new byte[8];//double               44-51    
   
   byte[] magnetic_heading = new byte[8];//double           52-59
   byte[] true_heading = new byte[8];//double               
   
   byte[] latitude = new byte[8];//double
   byte[] longitud = new byte[8];//double
   byte[] altitud = new byte[8];//double
   
   byte[] proximity_sensor = new byte[1];//BOOL
   
   byte[] point1_touched = new byte[1];//BOOL
   byte[] point1_x = new byte[4];//int
   byte[] point1_y = new byte[4];//int
   
   byte[] point2_touched = new byte[1];//BOOL
   byte[] point2_x = new byte[4];//int
   byte[] point3_y = new byte[4];//int 
 ************************************************************************/

