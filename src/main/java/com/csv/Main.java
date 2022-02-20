package com.csv;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello world!
 */
public class Main {
    public static void main(String[] args) {
        final Logger logger = Logger.getLogger("CsvUtility");
        try {
            System.out.println("-----------------Iniciando CSV Utility----------------");
            CsvUtility csv = new CsvUtility();
//            System.out.println("listado de archivos:");
            Map<String, File> files = csv.getFilesHM();
            System.out.println(String.format("Cantidad de archivos '.pdf' en el directorio inicial: %s", files.size()));
            System.out.println(String.format("Leyendo archivo CSV: %s ", csv.getCsvInputName()));
            csv.readInputCSV();
            System.out.println(String.format("El CSV: %s fue procesado correctamente", csv.getCsvInputName()));

            Map<String, String[]> cleanCsvHM = csv.sanitizeInputCSV();
            System.out.println(String.format("Cantidad de lineas del CSV a exportar: %s", cleanCsvHM.size()));
            int pendientes = csv.sanitizeDirectory();
            System.out.println(String.format("Cantidad de archivos movidos a la carpeta 'PendingFiles' : %s", pendientes));
            System.out.println(String.format("Creando el CSV de salida:%s", csv.getCsvOutputName()));
            boolean result = csv.writeNewCsv();
            String mensaje = result == true ? "CSV generado satisfactoriamente" : "No se pudo generar el CSV";
            System.out.println(mensaje);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "There was a problem executing CSV Utility -- " + e.getMessage());
        }
    }
}
