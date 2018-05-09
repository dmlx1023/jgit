import java.io.*;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import jdk.internal.util.xml.impl.Input;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * @Description jgit demo
 * @Author duanmulixiang
 * @create 2018-05-09 10:26
 * @Version 1.0
 **/
public class JGitDemo {
    static final   Pattern pattern = Pattern.compile("\\s*|\t|\r|\n");
    public static void main(String[] args) throws IOException, GitAPIException, ExecutionException, InterruptedException {

        String rootPath = "C:\\Users\\duanm\\Desktop\\517上线\\properties";
        Properties props = loadProps(rootPath +File.separator+"git.properties");
        String excelPath =props.getProperty("ExcelPath");
        //列从0开始
        int clumnNum =Integer.parseInt(props.getProperty("ClumnNum"));
        //行从1开始
        int lineNum=Integer.parseInt(props.getProperty("LineNum"));
        //所有工程的父目录
        String rootProjectPath = props.getProperty("RootProjectPath");
        //版本号导出路径
        String exportPath = rootPath +File.separator+"version.txt";
        if (excelPath==null||rootProjectPath==null) {
            throw new RuntimeException("路径读取失败，请检查配置文件！");
        }





        //
        File exportFile = new File(exportPath);
        if (exportFile.exists()) {
            exportFile.delete();
        }
        exportFile.createNewFile();
        ExecutorService executorService = Executors.newCachedThreadPool();
        //待读取的excel地址,返回cqSeq的list
        File excelReadFile = new File(excelPath);
        if (!excelReadFile.exists()) {
            throw new RuntimeException("Excel文件不存在，请检查路径是否正确：ExcelPath:"+excelReadFile.getPath());
        }
        List<String> cqList = readExcel(excelReadFile, clumnNum,lineNum);
        if (cqList.size() == 0) {
            throw new RuntimeException("cq单号为空，请检查excel的cq单号的行和列是否配置正确！");
        }
        File rootFile = new File(rootProjectPath);
        if (!rootFile.exists()) {
            throw new RuntimeException("未找到工作空间，请检查路径是否正确：RootProjectPath:"+rootFile.getPath());
        }
        List<File> fileList = Arrays.asList(rootFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().endsWith(".git")) {
                    return false;
                }
                return true;
            }
        }));
        final CountDownLatch countDownLatch = new CountDownLatch(fileList.size());
        BufferedWriter buf = new BufferedWriter(new FileWriter(exportFile));
        for (File file : fileList) {
            //按照每个系统来查询所有的单号的版本，时间倒序
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                        StringBuffer sb = new StringBuffer();
                        sb.append("当前系统：" + file.getName()+"\n");
                    for (String cq : cqList) {
                        if (cq == null || "".equals(cq.trim())) {
                            continue;
                        }
                        List<String> versionList = null;
                        try {
                            versionList = getIdName(file, cq);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (GitAPIException e) {
                            e.printStackTrace();
                        }
                        int length = versionList.size() - 1;
                        for (; length >= 0; length--) {
                                sb.append(versionList.get(length)).append("\n");
                        }
                    }
                    countDownLatch.countDown();
                    try {

                        sb.append("========================");
                        buf.write(sb.toString());
                        buf.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        countDownLatch.await();
        buf.flush();
        executorService.shutdownNow();
        System.out.println("版本导出完毕");
    }

    /**
     * @Description 查找dev包含cqSeq的版本
     * @Author: duanmulixiang
     * @create: 2018/5/9 12:42
     * @Version: 1.0
     **/
    static List<String> getIdName(File file, String cqSeq) throws IOException, GitAPIException {
        DateTimeFormatter dtf= DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> result = new ArrayList();
        Git git = null;
        try {
            git = Git.open(file);
        } catch (Exception e) {
            return new ArrayList<>();
        }
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef(git.getRepository().findRef("HEAD").getName()).call();
//        git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider("duanmlx", "duanmlx")).setRebase(true).call();
        //说明当前分支时master要切换到dev
        if (git.getRepository().resolve("master")!=null){
            git.checkout().setCreateBranch(false).setName("dev").call();
        }
        Iterable<RevCommit> commits = git.log().call();
        for (RevCommit commit : commits) {
            String commitMsg = commit.getFullMessage().toLowerCase();
            if (commitMsg.indexOf(cqSeq.toLowerCase())!=-1) {
                ObjectId id = commit.getId();
                String content = "版本号：" + id.getName().substring(0, 7) + "==>提交备注：" + commit.getFullMessage() + "==>开发人员："
                        + commit.getAuthorIdent().getName() + "==>提交时间:" + LocalDateTime.ofInstant(commit.
                        getAuthorIdent().getWhen().toInstant(), ZoneId.systemDefault()) + "==>版本所在目录:" + file.getName() + "==>版本所在分支：" + git.getRepository().getBranch();

                result.add(pattern.matcher(content).replaceAll(""));
            }
        }
        return result;
    }

    public static List<String> readExcel(File file, int colum,int lineNum) throws IOException {
        List<String> list = new ArrayList<>();
        //通过名字读取会商户jvm占用excel的
        InputStream inputStream = new FileInputStream(file);
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
        XSSFSheet hssfSheet = xssfWorkbook.getSheetAt(0);
//        int firstRowNum = hssfSheet.getFirstRowNum();
        int firstRowNum = lineNum;
        int lastRowNum = hssfSheet.getLastRowNum();
        for (; firstRowNum < lastRowNum; firstRowNum++) {
            String cellValue = hssfSheet.getRow(firstRowNum).getCell(colum).getStringCellValue();
            cellValue = cellValue.replaceAll("[\\u4e00-\\u9fa5]", "");
            if (cellValue != null && !"".equals(cellValue)) {
                list.add(cellValue);
            }
        }
        if (inputStream != null) {
            inputStream.close();
        }
        return list;
    }

    static Properties loadProps(String path) throws IOException {
        Properties properties = new Properties();
        FileInputStream in = null;
        try {
         in = new FileInputStream(new File(path));
            properties.load(new InputStreamReader(in, Charset.defaultCharset()));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
             in.close();
            }
        }

        return properties;
    }

}
