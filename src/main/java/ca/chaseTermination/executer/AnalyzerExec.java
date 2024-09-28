package ca.chaseTermination.executer;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AnalyzerExec {
    public static boolean exec(Class klass, List<String> options) throws Exception {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        long memorySize = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
        int mbRam = (int) (memorySize / 1024 / 1024);
        int vbRam = (int) (mbRam * 0.85);
        String className = klass.getCanonicalName();

        String vmRamParams = "-Xmx" + vbRam + "m";
        String stacksize = "-Xss256k";
        List<String> commands = new ArrayList<String>();
        commands.add(javaBin);
        commands.add("-cp");
        commands.add(classpath);
        commands.add(vmRamParams);
        commands.add(stacksize);
        commands.add(className);
        commands.addAll(options);
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();
        process.waitFor(10, TimeUnit.MINUTES);
        int exitValue = 0;
        try {
            exitValue = process.exitValue();
        } catch (IllegalThreadStateException e) {
            e.printStackTrace();
            System.out.println("Process terminates!");
            process.destroy();
            exitValue = 1;
        }
        return (exitValue != 0); //Return true if errors
    }

    public static String getOptionValue(String[] args, String option, int valueIndex) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals(option))
                return args[i + valueIndex];
        }
        return null;
    }

    public static String getOptionValueWithAssert(String[] args, String option, int valueIndex) {
        String optionValue = getOptionValue(args, option, valueIndex);
        if (optionValue == null)
            throw new RuntimeException("Missing option: " + option);
        return optionValue;
    }

    public static List<File> getFiles(String inputDirPath) {
        List<File> dirs = new ArrayList<>();
        File dir = new File(inputDirPath);
        dirs.add(dir);

        File[] subdirs = dir.listFiles(File::isDirectory);
        assert subdirs != null;
        Collections.addAll(dirs, subdirs);

        List<File> files = new ArrayList<>();
        for (File o : dirs) {
            File[] tempFiles = o.listFiles(file -> {
                String suffix = ".txt";
                return file.getName().endsWith(suffix);
            });
            assert tempFiles != null;
            System.out.println("tempFiles.length = " + tempFiles.length);
            Collections.addAll(files, tempFiles);
        }
        return files;
    }

    private static void integrateResults(String path) throws IOException {
        List<File> files = new ArrayList<>();
        File rootDir = new File(path);
        files.addAll(List.of(rootDir.listFiles((dir, name) -> name.endsWith(".res"))));
        File[] subdirs = rootDir.listFiles(file -> file.isDirectory());
        for (File subdir : subdirs) {
            files.addAll(List.of(subdir.listFiles((dir, name) -> name.endsWith(".res"))));
        }

        Set<Map<String, Object>> results = new HashSet<>();
        List<String> keys = new ArrayList<>(); keys.add("name");keys.add("t_vlog");
        for (File file : files) {
            System.out.println("Processing " + file.getName());
            Map<String, Object> result = new HashMap<>();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String ontologyname = file.getAbsolutePath().substring(path.length());
            result.put("name", ontologyname);
            String line = in.readLine();
            while (line != null) {
                String key = line.substring(0, line.indexOf(":"));
                String value = line.substring(line.indexOf(":")+1);
                result.put(key,value);
                if (!keys.contains(key)) keys.add(key);
                line = in.readLine();
            }
            results.add(result);
        }
        String header = "";
        keys.sort((o1, o2) -> {
            if (o1.equals(o2)) return 0;
            if (o1.equals("name")) return -1;
            if (o2.equals("name")) return 1;
            return o1.compareTo(o2);
        });
        for (String key : keys) {
            header += key + ",";
        }
        String fileName = path + "/results.csv";
        File file = new File(fileName); file.createNewFile();
        FileWriter out = new FileWriter(file);
        out.write(header.substring(0, header.length()-1) + "\n");

        for (Object o : results) {
            Map<String, Object> result = (Map<String, Object>) o;
            String line = "";
            for (String key : keys) {
                System.out.println("key = " + key);
                String value = result.get(key) +"";
                line += value + ",";
            }
            out.write(line.substring(0, line.length()-1) + "\n");
        }
        out.close();
    }

    public static void main(String[] args) throws IOException {
        integrateResults(args[0]);
    }
}
