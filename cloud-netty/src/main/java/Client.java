import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

public class Client {
    public static void main(String[] args) throws Exception {
        CountDownLatch commonStarter = new CountDownLatch(1);  //запускает подключение к серверу по сети
        new Thread(() -> Common.getInstance().start(commonStarter)).start();
        commonStarter.await();

        FileSendler.sendFile(Paths.get("src/main/java/1.txt"), Common.getInstance().getCurrentChannel(), future -> {  //начинает передачу файла
            if (!future.isSuccess()){
                future.cause().printStackTrace(); //в случае неудачи выводит ошибку
            Common.getInstance().stop();
            }

            if (future.isSuccess()){
                System.out.println("Successfully"); //в случае удачной передачи выводит сообщение
           Common.getInstance().stop();
            }
        });
    }
}
