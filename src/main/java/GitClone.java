import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * @Description
 * @Author duanmulixiang
 * @create 2018-05-09 15:51
 * @Version 1.0
 **/
public class GitClone {
    public static void main(String[] args) throws IOException, GitAPIException, InterruptedException {
//        common.git  "usr" "common" "ord" "cmt" "mkt" "cop" "ipf" "bth" "lvs"  "mnt" "getway" "mgt" vx-merchant-pc vx-mgt-pc vx-user-mobile vx-user-pc
            String REMOTE_URL_BASE = "ssh://duanmlx@66.6.52.130:29418/ifp/";
        String[] array = {"usr","common","ord","cmt" ,"mkt", "cop", "ipf" ,"bth", "lvs" , "mnt", "getway", "mgt", "vx-merchant-pc", "vx-mgt-pc", "vx-user-mobile",
                "vx-user-pc"};
        ExecutorService executorService = Executors.newCachedThreadPool();
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch countDownLatch = new CountDownLatch(array.length);
        File file = new File("F:\\gitCherryPick");
        for (String s : array) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    File file1 = new File(file.getPath() + File.separator + s);
                    file1.deleteOnExit();
                    if (!file1.exists()) {
                        file1.mkdir();
                    }
                    String REMOTE_URL = REMOTE_URL_BASE + s + ".git";
                    try {
                        Git.cloneRepository().setCredentialsProvider(
                                new UsernamePasswordCredentialsProvider
                                        ("duanmlx", "duanmlx")).setDirectory(file1).setURI(REMOTE_URL).setBranch("dev").call();
                        System.out.println(s+"执行完毕");
                        countDownLatch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        }

        countDownLatch.await();
        System.out.println("结束");
        executorService.shutdownNow();


    }


}
