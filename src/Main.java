import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Exception {

        File startFolder = new File("E:\\start");
        File midFolder = new File("E:\\mid");
        File endFolder = new File("E:\\end");
        String fileName = "";

        if (startFolder.exists() && startFolder.isDirectory()) {
            File[] files = startFolder.listFiles();
            Map<String, List<File>> map = Arrays.asList(files).stream().filter(a -> a.getName().contains(".mp4")).collect(Collectors.groupingBy(a -> a.getName().split("、")[0]));
            map.forEach((key,value)->{
                Map<Boolean, File> fileMap = value.stream().collect(Collectors.toMap(a -> a.getName().contains(" (1)"), Function.identity()));
                //视频
                File sp = fileMap.get(Boolean.TRUE);
                File sy = fileMap.get(Boolean.FALSE);
                //移动文件
                moveFile(sp,midFolder);
                moveFile(sy,midFolder);
                //修改文件名
                updateFile(midFolder);
                // 执行 cmd命令
                exec(sy.getName(),midFolder);
            });


        } else {
            System.out.println("文件夹不存在或不是一个文件夹");
        }

        System.out.println(fileName);
//        exec(fileName, midFolder);
    }

    private static void exec(String fileName, File midFolder) {
        try {
            // 创建进程构建器
            ProcessBuilder processBuilder = new ProcessBuilder();
            // 设置第一个命令
            processBuilder.command("cmd", "/c", "ffmpeg -i E:\\mid\\a.mp4 -vn -acodec copy E:\\mid\\a.aac");
            System.out.println("执行第一步");
            // 执行第一个命令
            Process process1 = processBuilder.start();
            if (process1.waitFor() == 0) {
                System.out.println("第一步成功");
            } else {
                System.out.println("第一步失败");
            }

            // 设置第二个命令
            System.out.println("执行第二步");
            ProcessBuilder processBuilder2 = new ProcessBuilder();
//            processBuilder2.command("cmd", "/c", "ffmpeg -i E:\\mid\\v.mp4 -i E:\\mid\\a.aac -c:v copy -c:a copy -map 0:v:0 -map 1:a:0 E:\\end\\" + fileName);
            processBuilder2.command("cmd", "/c", "ffmpeg -i E:\\mid\\v.mp4 -i E:\\mid\\a.aac -c:v copy -c:a copy -map 0:v:0 -map 1:a:0 E:\\end\\output.mp4");

            // 执行第二个命令
            Process process2 = processBuilder2.start();
            // 处理标准输出
            StreamGobbler outputGobbler = new StreamGobbler(process2.getInputStream());
            outputGobbler.start();

            // 处理错误输出
            StreamGobbler errorGobbler = new StreamGobbler(process2.getErrorStream());
            errorGobbler.start();
            process2.waitFor();
            outputGobbler.join();
            errorGobbler.join();
            if (process2.waitFor() == 0) {
                System.out.println("第二步成功");
                // 修改end中output.mp4的文件名称
                File file = new File("E:\\end\\output.mp4");
                String parentPath = file.getParent();
                String newFilePath = parentPath + File.separator + fileName;
                File newFile = new File(newFilePath);
                if (file.renameTo(newFile)) {
                    System.out.println("文件名修改成功");
                } else {
                    System.out.println("文件名修改失败");
                }
            } else {
                System.out.println("第二步失败");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 删除mid文件中的中间的视频
            deleteFilesInFolder(midFolder.getPath());
            System.out.println("——————————执行完成——————————");
        }
    }

    private static void moveFile(File file, File midFolder) {
        try {
            File destinationFile = new File(midFolder.getAbsolutePath() + File.separator + file.getName());
            Files.move(file.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("文件移动成功");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    private static void updateFile(File midFolder) {
        File[] files = midFolder.listFiles();
        if (files[0].getName().contains("(1)")) {
//            fileName = files[1].getName();
            String parentPath = files[0].getParent();
            File newFile = new File(parentPath + File.separator + "v.mp4");
            String parentPath2 = files[1].getParent();
            File newFile2 = new File(parentPath2 + File.separator + "a.mp4");
            files[0].renameTo(newFile);
            files[1].renameTo(newFile2);
        } else {
//            fileName = files[0].getName();
            String parentPath = files[1].getParent();
            File newFile = new File(parentPath + File.separator + "v.mp4");
            String parentPath2 = files[0].getParent();
            File newFile2 = new File(parentPath2 + File.separator + "a.mp4");
            files[1].renameTo(newFile);
            files[0].renameTo(newFile2);
        }
    }

    public static void deleteFilesInFolder(String folderPath) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                        System.out.println("删除文件: " + file.getAbsolutePath());
                    } else if (file.isDirectory()) {
                        deleteFilesInFolder(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    static class StreamGobbler extends Thread {
        private InputStream inputStream;

        StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}