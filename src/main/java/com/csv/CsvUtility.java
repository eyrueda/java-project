package com.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CsvUtility {
    private final static Logger logger = Logger.getLogger("CsvUtility");
    private String ruta;
    private File fileRoot;
    private Map<String, File> filesHM;
    private Map<String, File> cleanfilesHM;
    private Map<String, String[]> cleanCsvHM;
    private Map<String, File> pendingFilesHM;
    private FileFilter filter;
    private final String csvInputName = "expedientes.csv";
    private List<String[]> csvInputContent;
    private final String csvOutputName = "expedientes-salida.csv";
    private final String[] validExtensions = {".pdf"};
    private final String pendingFolderName = "PendingFiles";

    public CsvUtility() {
        //get the path of the executable jar file
        this.ruta = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        File f = new File(ruta);
        //point the cursor to the parent folder of the jar executable
        fileRoot = f.getParentFile();
        filesHM = new HashMap<>();
        cleanCsvHM = new HashMap<>();
        //accept only ".pdf" files
        createFilter();

    }

    private void createFilter() {
        filter = file -> {
            return Arrays.stream(validExtensions).anyMatch(entry -> file.getName().endsWith(entry));
        };
    }

    public Map<String, File> getFilesHM() {
        try {
            for (File file : Objects.requireNonNull(fileRoot.listFiles(this.filter))) {
                filesHM.put(file.getName(), file);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "There was a problem creating the HashMap: " + e.getMessage());
        }
        return filesHM;
    }

    public void readInputCSV() {
        try{
            String csvPath = fileRoot.getPath() + "/" + csvInputName;
            FileReader fileReader = new FileReader(csvPath);
            CSVReader csvReader = new CSVReader(fileReader);
            csvInputContent = csvReader.readAll();
            fileReader.close();
            csvReader.close();
        }catch (Exception e){
            logger.log(Level.SEVERE, "There was a problem reading the input CSV-- "+e.getMessage());
        }
    }

    public Map<String, String[]> sanitizeInputCSV() {
        //from 1 to avoid headers names
        for (int i = 1; i < csvInputContent.size(); i++) {
            String[] linea = csvInputContent.get(i);
            //linea[3] is the "No de Expediente" field
            if (!linea[3].isBlank()) {//
                String csvRowfilename = String.format("expediente%s.pdf", linea[3]);
                //reducing size of the csv rows
                if (filesHM.get(csvRowfilename) != null) {
                    cleanCsvHM.put(csvRowfilename, linea);
                }
            }
        }
        return cleanCsvHM;
    }

    public boolean writeNewCsv() throws Exception {
        CSVWriter writer = null;
        try {
            LinkedHashMap<String, String> f = new LinkedHashMap<>();
            f.put("ID", "filename");
            f.put("No. de caja", "dc.identifier.issn");
            f.put("Secuencia de expedientes", "dc.identifier.isbn");
            f.put("No. de expediente", "dc.identifier");
            f.put("Contenido", "dc.description");
            f.put("Fecha", "dc.date");
            f.put("No. de Ley/Resolución/Decreto", "dc.identifier.govdoc");
            f.put("Observaciones", "dc.description.tableofcontents");
            f.put("Tipo de Iniciativa", "dc.identifier.other");
            f.put("Resolución / contrato", "dc.identifier.sici");
            f.put("Titulo", "dc.title");
            f.put("Proponentes", "dc.provenance");
            f.put("Comision", "dc.publisher");
            f.put("Sesiones", "dc.description.sponsorship");
            String[] newHeaders = f.values().toArray(new String[0]);

            writer = new CSVWriter(new FileWriter(fileRoot.getPath() + "/" + csvOutputName));
            writer.writeNext(newHeaders);
            for (Map.Entry<String, String[]> entry : cleanCsvHM.entrySet()) {
                //get the old csv row
                String[] oldRow = entry.getValue();
                //initialize new row, it will have new fields, Titulo,Proponentes,Comision,Sesiones
                String[] newRow = new String[14];
                String filename = entry.getKey();
                String numeroDeCaja = oldRow[1];
                String secuenciadeExpedientes = oldRow[2];
                String numeroDeExpediente = oldRow[3];
                String contenido = oldRow[4];
                String fecha = oldRow[5];
                String numeroDeLey = oldRow[6];
                String observaciones = oldRow[7];
                String tipoDeIniciativa = oldRow[8];
                String resolucion = oldRow[9];
                //title must have "Expediente " + No. Expediente + "-" + Numero de Ley
                StringBuilder title = new StringBuilder("Expediente " + numeroDeExpediente);
                if (!numeroDeLey.isBlank() && !numeroDeLey.strip().equals("-")) {
                    title.append(" - ").append(numeroDeLey);
                }
                String titulo = title.toString();
                String proponentes = "";
                String comision = "";
                String sesiones = "";
                int index = 0;

                newRow[index++] = filename;
                newRow[index++] = numeroDeCaja;
                newRow[index++] = secuenciadeExpedientes;
                newRow[index++] = numeroDeExpediente;
                newRow[index++] = contenido;
                newRow[index++] = fecha;
                newRow[index++] = numeroDeLey;
                newRow[index++] = observaciones;
                newRow[index++] = tipoDeIniciativa;
                newRow[index++] = resolucion;
                newRow[index++] = titulo;
                newRow[index++] = proponentes;
                newRow[index++] = comision;
                newRow[index++] = sesiones;
                //add the new row to the writer buffer
                writer.writeNext(newRow);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "There was a problem creating the new CSV file " + e.getMessage());
        }
        writer.close();
        return true;
    }


    public int sanitizeDirectory() {
        pendingFilesHM = filesHM.entrySet().stream().
                filter(x -> !cleanCsvHM.containsKey(x.getKey())).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        movePendingFiles();
        return pendingFilesHM.size();
    }

    public void movePendingFiles() {
        try {
            File targetFile = new File(fileRoot.getPath() + "/" + pendingFolderName);
            if (!targetFile.exists()) {
                targetFile.mkdir();
            }
            pendingFilesHM.forEach((k, v) -> {
                        try {
                            String tempFileName = k;
                            Path targetPath = Path.of(targetFile + "/" + tempFileName);
                            Files.move(v.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "There was an error moving the file: " + k + e.getMessage());
                        }
                    }
            );
        } catch (Exception e) {
            logger.log(Level.SEVERE, "There was a problem creating the pending folder: " + e.getMessage());
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    public String getRuta() {
        return ruta;
    }

    public File getFileRoot() {
        return fileRoot;
    }

    public Map<String, File> getCleanfilesHM() {
        return cleanfilesHM;
    }

    public Map<String, String[]> getCleanCsvHM() {
        return cleanCsvHM;
    }

    public Map<String, File> getPendingFilesHM() {
        return pendingFilesHM;
    }

    public FileFilter getFilter() {
        return filter;
    }

    public String getCsvInputName() {
        return csvInputName;
    }

    public List<String[]> getCsvInputContent() {
        return csvInputContent;
    }

    public String getCsvOutputName() {
        return csvOutputName;
    }

    public String[] getValidExtensions() {
        return validExtensions;
    }

    public String getPendingFolderName() {
        return pendingFolderName;
    }
}
