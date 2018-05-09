import java.time.LocalDateTime;

public class JGitDemoTest {
    public static void main(String[] args){
        String date = LocalDateTime.now().toString().replaceAll("[[\\s-:punct:]]", "");
        System.out.println(date.substring(0,date.lastIndexOf(".")));
        }

}
