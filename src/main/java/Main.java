import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.jfif.JfifDirectory;

import java.io.*;
import java.nio.file.FileSystem;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    public static final String[] CMDS = {"Quit", "Get all pictures dpi", "Get all file names"};
    public static final String[] TYPES = {"jpg", "jpeg", "tiff", "tif", "png", "bmp"};
    public static int threshold;

    private static boolean containType(String type) {
        for (String t : TYPES) {
            if (t.equals(type))
                return true;
        }
        return false;
    }

    public static void getAllFileNames(String pathName) {
        File file = new File(pathName);
        File[] files = file.listFiles();
        assert files != null : "file is empty";

        for (File f : files) {
            if (!f.isDirectory()) {
                String name = f.getName();
//                System.out.println(name);
                String namePre = name.substring(0, name.lastIndexOf('.'));
                System.out.println(namePre);
            }
        }
    }

    public static void getImgInfo(String pathName) throws ImageProcessingException, IOException, MetadataException {
        System.out.println("Directory path: " + pathName);
        File file = new File(pathName);
        File[] files = file.listFiles();
        assert files != null : "file is empty";

        List<String> lessThan = new ArrayList<>();
        List<String> greater = new ArrayList<>();
        List<String> failure = new ArrayList<>();

        for (File f : files) {
            if (!f.isDirectory()) {
                int xDpi = -1;
                int yDpi = -1;

                String fName = f.getName();
                String fPreName = fName.substring(0, fName.lastIndexOf('.'));
                String fType = fName.substring(fName.lastIndexOf('.') + 1).toLowerCase();
                if (!containType(fType)) {
                    System.out.format("%s is not a legal picture\n", fName);
                    continue;
                }
                Metadata metadata = ImageMetadataReader.readMetadata(f);
//                printMetadata(metadata);
                // 先从jfif格式获取dpi
                JfifDirectory jfif = metadata.getFirstDirectoryOfType(JfifDirectory.class);
                Collection<ExifDirectoryBase> exif = null;
                if (jfif != null) {
                    xDpi = jfif.getResX();
                    yDpi = jfif.getResY();
                }
                if (xDpi == -1) {
                    // 再从exif格式获取dpi
//                    exif = metadata.getFirstDirectoryOfType(ExifDirectoryBase.class);
                    exif = metadata.getDirectoriesOfType(ExifDirectoryBase.class);
                    for (ExifDirectoryBase e : exif) {
                        xDpi = Integer.parseInt(e.getString(ExifDirectoryBase.TAG_X_RESOLUTION));
                        yDpi = Integer.parseInt(e.getString(ExifDirectoryBase.TAG_Y_RESOLUTION));
                        if (xDpi != -1)
                            break;
                    }
                }
                if (xDpi == -1 && yDpi == -1) {
                    System.out.format("%s : get DPI failed\n", fName);
                    failure.add(fPreName);
                } else {
                    System.out.format("%s :x-DPI = %d, y-DPI = %d\n", fPreName, xDpi, yDpi);
                    if (xDpi >= threshold) {
                        greater.add(fPreName);
                    } else {
                        lessThan.add(fPreName);
                    }
                }
//                System.out.println("====");
            }
        }

        // print to a file
        String listFileName = String.format("%s\\aDpiList-%s.txt", pathName, getDate());
        System.out.println("Storage in " + listFileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(listFileName));
        writer.write(String.format("bad=%d good=%d failed=%d\n", lessThan.size(), greater.size(), failure.size()));
        writer.write("\n===========BAD===========:\n");
        for (String s : lessThan) {
            writer.write(s + "\n");
        }
        writer.write("\n===========GOOD===========:\n");
        for (String s : greater) {
            writer.write(s + "\n");
        }
        writer.write("\n===========failed===========:\n");
        for (String s : failure) {
            writer.write(s + "\n");
        }
        writer.write("\n===========END===========\n");
        writer.close();

    }

    public static void printMetadata(Metadata metadata) {
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                System.out.format("[%s] - %s = %s\n",
                        directory.getName(), tag.getTagName(), tag.getDescription());
            }
            if (directory.hasErrors()) {
                for (String error : directory.getErrors()) {
                    System.err.format("ERROR: %s\n", error);
                }
            }
        }
    }

    public static boolean copyFolder(String sourcePath, String targetPath) throws Exception {
        File sourceFile = new File(sourcePath);
        File targetFile = new File(targetPath);
        if (!sourceFile.exists()) {
            throw new Exception("文件夹不存在");
        }
        if (!sourceFile.isDirectory()) {
            throw new Exception("源文件夹不是目录");
        }
        if (!targetFile.exists()) {
            targetFile.mkdirs();
        }
        if (!targetFile.isDirectory()) {
            throw new Exception("目标文件夹不是目录");
        }

        File[] files = sourceFile.listFiles();
        if (files == null || files.length == 0) {
            return false;
        }

        for (File file : files) {
            //文件要移动的路径
            String movePath = targetFile + File.separator + file.getName();
            if (file.isDirectory()) {
                //如果是目录则递归调用
//                    copyFolder(file.getAbsolutePath(),movePath);
            } else {
                //如果是文件则复制文件
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(movePath));

                byte[] b = new byte[1024];
                int temp = 0;
                while ((temp = in.read(b)) != -1) {
                    out.write(b, 0, temp);
                }
                out.close();
                in.close();
            }
        }
        return true;
    }

    private static String getDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddHHmmss");
        String date = dateFormat.format(new Date());
        return date;
    }

    public static boolean cmd1() {
        Scanner in = new Scanner(System.in);
        System.out.println("Paste folder path(粘贴待处理图片所在文件夹的路径), eg: C:\\Users\\dengy\\Desktop\\我的图片文件夹");
//        String pathName = "D:\\Users\\Pictures\\Saved Pictures";
        String pathName = in.nextLine().trim();
        System.out.println("Input the threshold of DPI(输入DPI的阈值), eg: 96");
        threshold = in.nextInt();

//        try {
//            copyFolder(pathName, newPathName);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        app.getAllFileNames(newPathName);
        try {
            getImgInfo(pathName);
        } catch (ImageProcessingException | IOException | MetadataException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static boolean cmd2() {
        Scanner in = new Scanner(System.in);
        System.out.println("Paste folder path(粘贴待提取文件名的文件夹的路径), eg: C:\\Users\\dengy\\Desktop\\我的书架");
        String pathName = in.nextLine().trim();
        getAllFileNames(pathName);
        return true;
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int cmd;
        boolean isQuit = false;
        while (!isQuit) {
            System.out.println("Command list:");
            for (int i = 0; i < CMDS.length; i++) {
                System.out.format("%d. %s\n", i, CMDS[i]);
            }
            System.out.println("Input the command number: ");
            cmd = in.nextInt();
            if (!legal(cmd)) {
                System.out.println("Illegal input!");
                continue;
            }
            switch (cmd) {
                case 0:
                    isQuit = true;
                    break;
                case 1:
                    if (cmd1()) {
                        print();
                    }
                    break;
                case 2:
                    if (cmd2()) {
                        print();
                    }
                    break;
            }
        }
    }

    private static boolean legal(int cmd) {
        return cmd == 0 || cmd == 1 || cmd == 2;
    }

    private static void print() {
        System.out.println("=========Finished=========\n\n");
    }
}
